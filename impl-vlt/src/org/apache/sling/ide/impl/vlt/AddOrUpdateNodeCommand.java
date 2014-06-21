/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.ide.impl.vlt;

import static org.apache.jackrabbit.vault.util.JcrConstants.JCR_CONTENT;
import static org.apache.jackrabbit.vault.util.JcrConstants.JCR_DATA;
import static org.apache.jackrabbit.vault.util.JcrConstants.JCR_LASTMODIFIED;
import static org.apache.jackrabbit.vault.util.JcrConstants.JCR_PRIMARYTYPE;
import static org.apache.jackrabbit.vault.util.JcrConstants.NT_RESOURCE;
import static org.apache.sling.ide.transport.Repository.NT_FILE;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.jcr.Binary;
import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.NodeType;

import org.apache.jackrabbit.vault.util.JcrConstants;
import org.apache.jackrabbit.vault.util.Text;
import org.apache.sling.ide.transport.FileInfo;
import org.apache.sling.ide.transport.ResourceProxy;
import org.apache.sling.ide.util.PathUtil;

public class AddOrUpdateNodeCommand extends JcrCommand<Void> {

    private ResourceProxy resource;
    private FileInfo fileInfo;
    private boolean primaryTypeHasChanged;

    public AddOrUpdateNodeCommand(Repository jcrRepo, Credentials credentials, FileInfo fileInfo, ResourceProxy resource) {

        super(jcrRepo, credentials, resource.getPath());

        this.fileInfo = fileInfo;
        this.resource = resource;
    }

    @Override
    protected Void execute0(Session session) throws RepositoryException, IOException {

        update(resource, session);
        return null;
    }

    private void update(ResourceProxy resource, Session session) throws RepositoryException, IOException {

        String path = resource.getPath();
        boolean nodeExists = session.nodeExists(path);

        Node node;
        if (nodeExists) {
            node = session.getNode(path);
            Activator
                    .getDefault()
                    .getPluginLogger()
                    .trace("Found existing node at {0} with primaryType {1}", path, node.getPrimaryNodeType().getName());
        } else {
            node = createNode(resource, session);
            Activator.getDefault().getPluginLogger()
                    .trace("Created node at {0} with primaryType {1}", path, node.getPrimaryNodeType().getName());
        }

        updateNode(node, resource);
        processDeletedNodes(node, resource);

        for (ResourceProxy child : resource.getCoveredChildren()) {
            update(child, session);
        }

        // save the changes so that the primary node type is propagated
        // however, we can't do that too early, since required properties might not be set
        // so we save right before checking for orderable child nodes
        if (primaryTypeHasChanged) {
            session.save();
        }

        NodeType primaryNodeType = node.getPrimaryNodeType();

        if (primaryNodeType.hasOrderableChildNodes()) {
            reorderChildNodes(node, resource);
        }
    }

    private void processDeletedNodes(Node node, ResourceProxy resource2) throws RepositoryException {

        // TODO - we probably don't support SNS here ( and in other places as well )

        List<ResourceProxy> resourceChildren = resource2.getChildren();
        if (resourceChildren.size() == 0) {
            Activator.getDefault().getPluginLogger()
                    .trace("Resource at {0} has no children, skipping deleted nodes processing",
                            resource2.getPath());
            return;
        }
        
        Map<String, ResourceProxy> resourceChildrenPaths = new HashMap<String, ResourceProxy>(
                resourceChildren.size());
        for (ResourceProxy child : resourceChildren) {
            resourceChildrenPaths.put(child.getPath(), child);
        }

        for (NodeIterator it = node.getNodes(); it.hasNext();) {

            Node child = it.nextNode();

            if (resourceChildrenPaths.containsKey(child.getPath())) {
                // only descend for reordering when the child node is covered ; otherwise we
                // don't have enough information
                if (resource2.covers(child.getPath())) {
                    processDeletedNodes(child, resourceChildrenPaths.get(child.getPath()));
                }
                continue;
            }

            Activator.getDefault().getPluginLogger()
                    .trace("Deleting node {0} as it is no longer present in the local checkout", child.getPath());
            child.remove();
        }
    }

    private void reorderChildNodes(Node nodeToReorder, ResourceProxy resourceToReorder) throws RepositoryException {

        List<ResourceProxy> children = resourceToReorder.getChildren();
        ListIterator<ResourceProxy> childrenIterator = children.listIterator();

        // do not process
        if (!childrenIterator.hasNext()) {
            Activator.getDefault().getPluginLogger()
                    .trace("Resource at {0} has no children, skipping child node reordering",
                            resourceToReorder.getPath());
            return;
        }
        List<Node> nodeChildren = new LinkedList<Node>();
        NodeIterator nodeChildrenIt = nodeToReorder.getNodes();
        while (nodeChildrenIt.hasNext()) {
            nodeChildren.add(nodeChildrenIt.nextNode());
        }
        ListIterator<Node> nodeChildrenListIt = nodeChildren.listIterator();

        // it is possible for the repository and the local workspace to have a different types of elements
        // for instance if the repository has been changed independently of the local workspace modifications
        // therefore allow for the
        boolean changed = false;

        traceResourcesAndNodes(children, nodeChildren);

        if (children.size() != nodeChildren.size()) {
            Activator.getDefault().getPluginLogger()
                    .warn("Different number of children between the local workspace and the repository for path "
                            + resourceToReorder.getPath() + ". Reordering will not be performed");
            return;
        }

        while (childrenIterator.hasNext() || nodeChildrenListIt.hasNext()) {

            ResourceProxy childResource = childrenIterator.next();
            Node childNode = nodeChildrenListIt.next();

            // order is as expected, skip reordering
            if (Text.getName(childResource.getPath()).equals(childNode.getName())) {
                // descend into covered child resources once they are properly arranged and perform reordering
                if (resourceToReorder.covers(childResource.getPath())) {
                    reorderChildNodes(childNode, childResource);
                }
                continue;
            }

            // don't perform any reordering if this particular node does not have reorderable children
            if (!nodeToReorder.getPrimaryNodeType().hasOrderableChildNodes()) {
                Activator
                        .getDefault()
                        .getPluginLogger()
                        .trace("Node at {0} does not have orderable child nodes, skipping reordering of {1}",
                                nodeToReorder.getPath(), childResource.getPath());
                continue;
            }

            String expectedParentName;
            if (childrenIterator.hasNext()) {
                expectedParentName = Text.getName(childrenIterator.next().getPath());
                childrenIterator.previous(); // move back
            } else {
                expectedParentName = null;
            }

            Activator.getDefault().getPluginLogger()
                    .trace("For node at {0} ordering {1} before {2}", nodeToReorder.getPath(),
                            Text.getName(childResource.getPath()),
                            expectedParentName);

            nodeToReorder.orderBefore(Text.getName(childResource.getPath()), expectedParentName);
            changed = true;
            break;
        }

        // re-read the data and run the ordering again
        // this makes sure that we don't have inconsistent data in the node list
        if (changed) {
            reorderChildNodes(nodeToReorder, resourceToReorder);
        }

    }

    private void traceResourcesAndNodes(List<ResourceProxy> children, List<Node> nodeChildren)
            throws RepositoryException {
        
        StringBuilder out = new StringBuilder();
        out.append("Comparison of nodes and resources before reordering \n");
        
        out.append(" === Resources === \n");
        for (int i = 0; i < children.size(); i++) {
            out.append(String.format("%3d. %s%n", i, children.get(i).getPath()));
        }

        out.append(" === Nodes === \n");
        for (int i = 0; i < nodeChildren.size(); i++) {
            out.append(String.format("%3d. %s%n", i, nodeChildren.get(i).getPath()));
        }

        Activator.getDefault().getPluginLogger().trace(out.toString());
    }

    private Node createNode(ResourceProxy resource, Session session) throws RepositoryException, FileNotFoundException {

        String parentLocation = Text.getRelativeParent(resource.getPath(), 1);
        if (parentLocation.isEmpty()) {
            parentLocation = "/";
        }

        if (!session.nodeExists(parentLocation)) {
            throw new RepositoryException("No parent found at " + parentLocation + " ; it's needed to create node at "
                    + resource.getPath());
        }

        String primaryType = (String) resource.getProperties().get(JCR_PRIMARYTYPE);

        if (primaryType == null) {
            throw new IllegalArgumentException("Missing " + JCR_PRIMARYTYPE + " for ResourceProxy at path "
                    + resource.getPath());
        }

        return session.getNode(parentLocation).addNode(PathUtil.getName(resource.getPath()), primaryType);
    }

    private void updateNode(Node node, ResourceProxy resource) throws RepositoryException, IOException {

        if (node.getPath().equals(getPath())) {
            updateFileLikeNodeTypes(node);
        }

        Set<String> propertiesToRemove = new HashSet<String>();
        PropertyIterator properties = node.getProperties();
        while (properties.hasNext()) {
            Property property = properties.nextProperty();
            if (property.getDefinition().isProtected()
                    || property.getDefinition().getRequiredType() == PropertyType.BINARY) {
                continue;
            }
            propertiesToRemove.add(property.getName());
        }

        propertiesToRemove.removeAll(resource.getProperties().keySet());

        Session session = node.getSession();

        // update the mixin types ahead of type as contraints are enforced before
        // the session is committed
        Object mixinTypes = resource.getProperties().get(JcrConstants.JCR_MIXINTYPES);
        if (mixinTypes != null) {
            updateMixins(node, mixinTypes);
        }

        String primaryType = (String) resource.getProperties().get(JcrConstants.JCR_PRIMARYTYPE);
        if (!node.getPrimaryNodeType().getName().equals(primaryType)) {
            node.setPrimaryType(primaryType);
            primaryTypeHasChanged = true;
            Activator.getDefault().getPluginLogger()
                    .trace("Set new primary type {0} for node at {1}", primaryType, node.getPath());
        }

        // TODO - review for completeness and filevault compatibility
        for (Map.Entry<String, Object> entry : resource.getProperties().entrySet()) {

            String propertyName = entry.getKey();
            Object propertyValue = entry.getValue();
            Property property = null;

            if (node.hasProperty(propertyName)) {
                property = node.getProperty(propertyName);
            }

            if (property != null && property.getDefinition().isProtected()) {
                continue;
            }

            // TODO - we don't handle the case where the input no longer matches the property definition, e.g. type
            // change or multiplicity change

            boolean isMultiple = property != null && property.getDefinition().isMultiple();

            ValueFactory valueFactory = session.getValueFactory();
            Value value = null;
            Value[] values = null;

            if (propertyValue instanceof String) {
                if (isMultiple) {
                    values = toValueArray(new String[] { (String) propertyValue }, session);
                } else {
                    value = valueFactory.createValue((String) propertyValue);
                }
            } else if (propertyValue instanceof String[]) {
                values = toValueArray((String[]) propertyValue, session);
            } else if (propertyValue instanceof Boolean) {
                if (isMultiple) {
                    values = toValueArray(new Boolean[] { (Boolean) propertyValue }, session);
                } else {
                    value = valueFactory.createValue((Boolean) propertyValue);
                }
            } else if (propertyValue instanceof Boolean[]) {
                values = toValueArray((Boolean[]) propertyValue, session);
            } else if (propertyValue instanceof Calendar) {
                if (isMultiple) {
                    values = toValueArray(new Calendar[] { (Calendar) propertyValue }, session);
                } else {
                    value = valueFactory.createValue((Calendar) propertyValue);
                }
            } else if (propertyValue instanceof Calendar[]) {
                values = toValueArray((Calendar[]) propertyValue, session);
            } else if (propertyValue instanceof Double) {
                if (isMultiple) {
                    values = toValueArray(new Double[] { (Double) propertyValue }, session);
                } else {
                    value = valueFactory.createValue((Double) propertyValue);
                }
            } else if (propertyValue instanceof Double[]) {
                values = toValueArray((Double[]) propertyValue, session);
            } else if (propertyValue instanceof BigDecimal) {
                if (isMultiple) {
                    values = toValueArray(new BigDecimal[] { (BigDecimal) propertyValue }, session);
                } else {
                    value = valueFactory.createValue((BigDecimal) propertyValue);
                }
            } else if (propertyValue instanceof BigDecimal[]) {
                values = toValueArray((BigDecimal[]) propertyValue, session);
            } else if (propertyValue instanceof Long) {
                if (isMultiple) {
                    values = toValueArray(new Long[] { (Long) propertyValue }, session);
                } else {
                    value = valueFactory.createValue((Long) propertyValue);
                }
            } else if (propertyValue instanceof Long[]) {
                values = toValueArray((Long[]) propertyValue, session);
                // TODO - distinguish between weak vs strong references
            } else if (propertyValue instanceof UUID) {
                Node reference = session.getNodeByIdentifier(((UUID) propertyValue).toString());
                if (isMultiple) {
                    values = toValueArray(new UUID[] { (UUID) propertyValue }, session);
                } else {
                    value = valueFactory.createValue(reference);
                }

            } else if (propertyValue instanceof UUID[]) {
                values = toValueArray((UUID[]) propertyValue, session);
            } else {
                throw new IllegalArgumentException("Unable to handle value '" + propertyValue + "' for property '"
                        + propertyName + "'");
            }

            if (value != null) {
                Object[] arguments = { propertyName, value, propertyValue, node.getPath() };
                Activator.getDefault().getPluginLogger()
                        .trace("Setting property {0} with value {1} (raw =  {2}) on node at {3}", arguments);
                node.setProperty(propertyName, value);
                Activator.getDefault().getPluginLogger()
                        .trace("Set property {0} with value {1} (raw =  {2}) on node at {3}", arguments);
            } else if (values != null) {
                Object[] arguments = { propertyName, values, propertyValue, node.getPath() };
                Activator.getDefault().getPluginLogger()
                        .trace("Setting property {0} with values {1} (raw =  {2}) on node at {3}", arguments);
                node.setProperty(propertyName, values);
                Activator.getDefault().getPluginLogger()
                        .trace("Set property {0} with values {1} (raw =  {2}) on node at {3}", arguments);
            } else {
                throw new IllegalArgumentException("Unable to extract a value or a value array for property '"
                        + propertyName + "' with value '" + propertyValue + "'");
            }
        }

        for (String propertyToRemove : propertiesToRemove) {
            node.getProperty(propertyToRemove).remove();
            Activator.getDefault().getPluginLogger()
                    .trace("Removed property {0} from node at {1}", propertyToRemove, node.getPath());
        }

    }

    private void updateMixins(Node node, Object mixinValue) throws RepositoryException {

        List<String> newMixins = new ArrayList<String>();

        if (mixinValue instanceof String) {
            newMixins.add((String) mixinValue);
        } else {
            newMixins.addAll(Arrays.asList((String[]) mixinValue));
        }

        List<String> oldMixins = new ArrayList<String>();
        for (NodeType mixinNT : node.getMixinNodeTypes()) {
            oldMixins.add(mixinNT.getName());
        }

        List<String> mixinsToAdd = new ArrayList<String>(newMixins);
        mixinsToAdd.removeAll(oldMixins);
        List<String> mixinsToRemove = new ArrayList<String>(oldMixins);
        mixinsToRemove.removeAll(newMixins);

        for (String mixinToAdd : mixinsToAdd) {
            node.addMixin(mixinToAdd);
            Activator.getDefault().getPluginLogger()
                    .trace("Added new mixin {0} to node at path {1}", mixinToAdd, node.getPath());
        }

        for (String mixinToRemove : mixinsToRemove) {
            node.removeMixin(mixinToRemove);
            Activator.getDefault().getPluginLogger()
                    .trace("Removed mixin {0} from node at path {1}", mixinToRemove, node.getPath());
        }
    }

    private void updateFileLikeNodeTypes(Node node) throws RepositoryException, IOException {
        // TODO - better handling of file-like nodes - perhaps we need to know the SerializationKind here
        // TODO - avoid IO
        File file = new File(fileInfo.getLocation());

        if (!hasFileLikePrimaryNodeType(node)) {
            return;
        }

        Node contentNode;

        if (node.hasNode(JCR_CONTENT)) {
            contentNode = node.getNode(JCR_CONTENT);
        } else {
            if (node.getProperty(JCR_PRIMARYTYPE).getString().equals(NT_RESOURCE)) {
                contentNode = node;
            } else {
                contentNode = node.addNode(JCR_CONTENT, NT_RESOURCE);
            }
        }

        Activator.getDefault().getPluginLogger()
                .trace("Updating {0} property on node at {1} ", JCR_DATA, contentNode.getPath());

        FileInputStream inputStream = new FileInputStream(file);
        try {
            Binary binary = node.getSession().getValueFactory().createBinary(inputStream);
            contentNode.setProperty(JCR_DATA, binary);
            // TODO: might have to be done differently since the client and server's clocks can differ
            // and the last_modified should maybe be taken from the server's time..
            contentNode.setProperty(JCR_LASTMODIFIED, Calendar.getInstance());

        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                // don't care
            }
        }
    }

    private boolean hasFileLikePrimaryNodeType(Node node) throws RepositoryException {
        return hasPrimaryNodeType(node, NT_FILE, NT_RESOURCE);
    }

    private boolean hasPrimaryNodeType(Node node, String... nodeTypeNames) throws RepositoryException {

        String primaryNodeTypeName = node.getPrimaryNodeType().getName();
        for (String nodeTypeName : nodeTypeNames) {
            if (primaryNodeTypeName.equals(nodeTypeName)) {
                return true;
            }

        }
        for (NodeType supertype : node.getPrimaryNodeType().getSupertypes()) {
            String superTypeName = supertype.getName();
            for (String nodeTypeName : nodeTypeNames) {
                if (superTypeName.equals(nodeTypeName)) {
                    return true;
                }
            }
        }

        return false;

    }

    private Value[] toValueArray(String[] strings, Session session) throws RepositoryException {

        Value[] values = new Value[strings.length];

        for (int i = 0; i < strings.length; i++) {
            values[i] = session.getValueFactory().createValue(strings[i]);
        }

        return values;
    }

    private Value[] toValueArray(Boolean[] booleans, Session session) throws RepositoryException {

        Value[] values = new Value[booleans.length];

        for (int i = 0; i < booleans.length; i++) {
            values[i] = session.getValueFactory().createValue(booleans[i]);
        }

        return values;
    }

    private Value[] toValueArray(Calendar[] calendars, Session session) throws RepositoryException {

        Value[] values = new Value[calendars.length];

        for (int i = 0; i < calendars.length; i++) {
            values[i] = session.getValueFactory().createValue(calendars[i]);
        }

        return values;
    }

    private Value[] toValueArray(Double[] doubles, Session session) throws RepositoryException {

        Value[] values = new Value[doubles.length];

        for (int i = 0; i < doubles.length; i++) {
            values[i] = session.getValueFactory().createValue(doubles[i]);
        }

        return values;
    }

    private Value[] toValueArray(BigDecimal[] bigDecimals, Session session) throws RepositoryException {

        Value[] values = new Value[bigDecimals.length];

        for (int i = 0; i < bigDecimals.length; i++) {
            values[i] = session.getValueFactory().createValue(bigDecimals[i]);
        }

        return values;
    }

    private Value[] toValueArray(Long[] longs, Session session) throws RepositoryException {

        Value[] values = new Value[longs.length];

        for (int i = 0; i < longs.length; i++) {
            values[i] = session.getValueFactory().createValue(longs[i]);
        }

        return values;
    }

    private Value[] toValueArray(UUID[] uuids, Session session) throws RepositoryException {

        Value[] values = new Value[uuids.length];

        for (int i = 0; i < uuids.length; i++) {

            Node reference = session.getNodeByIdentifier(uuids[i].toString());

            values[i] = session.getValueFactory().createValue(reference);
        }

        return values;
    }

}
