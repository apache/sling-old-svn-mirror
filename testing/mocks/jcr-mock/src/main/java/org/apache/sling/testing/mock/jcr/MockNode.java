/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.testing.mock.jcr;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.iterator.NodeIteratorAdapter;
import org.apache.jackrabbit.commons.iterator.PropertyIteratorAdapter;

import javax.jcr.*;
import javax.jcr.lock.Lock;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.regex.Pattern;

/**
 * Mock {@link Node} implementation
 */
class MockNode extends AbstractItem implements Node {

    public MockNode(final ItemData itemData, final Session session) {
        super(itemData, session);
    }

    @Override
    public Node addNode(final String relPath) throws RepositoryException {
        return addNode(relPath, JcrConstants.NT_UNSTRUCTURED);
    }

    @Override
    public Node addNode(final String relPath, final String primaryNodeTypeName) throws RepositoryException {
        String path = makeAbsolutePath(relPath);
        ItemData itemData = ItemData.newNode(path, new MockNodeType(primaryNodeTypeName));
        Node node = new MockNode(itemData, getSession());
        getMockedSession().addItem(itemData);
        node.setProperty(JcrConstants.JCR_PRIMARYTYPE, primaryNodeTypeName);

        // special handling for some node types
        if (StringUtils.equals(primaryNodeTypeName, JcrConstants.NT_FILE)) {
            node.setProperty(JcrConstants.JCR_CREATED, Calendar.getInstance());
            node.setProperty("jcr:createdBy", getMockedSession().getUserID());
        }

        return node;
    }

    @Override
    public Node getNode(final String relPath) throws RepositoryException {
        String path = makeAbsolutePath(relPath);
        return getSession().getNode(path);
    }

    @Override
    public NodeIterator getNodes() throws RepositoryException {
        RangeIterator items = getMockedSession().listChildren(getPath(), new ItemFilter() {
            @Override
            public boolean accept(final ItemData item) throws RepositoryException {
                return item.isNode();
            }
        });
        return new NodeIteratorAdapter(items, items.getSize());
    }

    @Override
    public NodeIterator getNodes(final String namePattern) throws RepositoryException {
        final Pattern pattern = Pattern.compile(namePattern);
        RangeIterator items = getMockedSession().listChildren(getPath(), new ItemFilter() {
            @Override
            public boolean accept(final ItemData item) throws RepositoryException {
                return item.isNode() && pattern.matcher(item.getName()).matches();
            }
        });
        return new NodeIteratorAdapter(items, items.getSize());
    }

    @Override
    public PropertyIterator getProperties() throws RepositoryException {
        RangeIterator items = getMockedSession().listChildren(getPath(), new ItemFilter() {
            @Override
            public boolean accept(final ItemData item) throws RepositoryException {
                return item.isProperty();
            }
        });
        return new PropertyIteratorAdapter(items, items.getSize());
    }

    @Override
    public PropertyIterator getProperties(final String namePattern) throws RepositoryException {
        final Pattern pattern = Pattern.compile(namePattern);
        RangeIterator items = getMockedSession().listChildren(getPath(), new ItemFilter() {
            @Override
            public boolean accept(final ItemData item) throws RepositoryException {
                return item.isProperty() && pattern.matcher(item.getName()).matches();
            }
        });
        return new PropertyIteratorAdapter(items, items.getSize());
    }

    @Override
    public Property getProperty(final String relPath) throws RepositoryException {
        String path = makeAbsolutePath(relPath);
        return getSession().getProperty(path);
    }

    @Override
    public String getIdentifier() throws RepositoryException {
        return this.itemData.getUuid();
    }

    @Override
    public String getUUID() throws RepositoryException {
        return getIdentifier();
    }

    @Override
    public boolean hasNode(final String relPath) throws RepositoryException {
        String path = makeAbsolutePath(relPath);
        return getSession().nodeExists(path);
    }

    @Override
    public boolean hasNodes() throws RepositoryException {
        return getNodes().hasNext();
    }

    @Override
    public boolean hasProperties() throws RepositoryException {
        return getProperties().hasNext();
    }

    @Override
    public boolean hasProperty(final String relPath) throws RepositoryException {
        String path = makeAbsolutePath(relPath);
        return getSession().propertyExists(path);
    }

    @Override
    public Property setProperty(final String name, final Value value) throws RepositoryException {
        ItemData itemData = ItemData.newProperty(getPath() + "/" + name);
        Property property = new MockProperty(itemData, getSession());
        property.setValue(value);
        getMockedSession().addItem(itemData);
        return property;
    }

    @Override
    public Property setProperty(final String name, final Value[] values) throws RepositoryException {
        ItemData itemData = ItemData.newProperty(getPath() + "/" + name);
        Property property = new MockProperty(itemData, getSession());
        property.setValue(values);
        getMockedSession().addItem(itemData);
        return property;
    }

    @Override
    public Property setProperty(final String name, final String[] values) throws RepositoryException {
        ItemData itemData = ItemData.newProperty(getPath() + "/" + name);
        Property property = new MockProperty(itemData, getSession());
        property.setValue(values);
        getMockedSession().addItem(itemData);
        return property;
    }

    @Override
    public Property setProperty(final String name, final String value) throws RepositoryException {
        ItemData itemData = ItemData.newProperty(getPath() + "/" + name);
        Property property = new MockProperty(itemData, getSession());
        property.setValue(value);
        getMockedSession().addItem(itemData);
        return property;
    }

    @Override
    @SuppressWarnings("deprecation")
    public Property setProperty(final String name, final InputStream value) throws RepositoryException {
        ItemData itemData = ItemData.newProperty(getPath() + "/" + name);
        Property property = new MockProperty(itemData, getSession());
        property.setValue(value);
        getMockedSession().addItem(itemData);
        return property;
    }

    @Override
    public Property setProperty(final String name, final boolean value) throws RepositoryException {
        ItemData itemData = ItemData.newProperty(getPath() + "/" + name);
        Property property = new MockProperty(itemData, getSession());
        property.setValue(value);
        getMockedSession().addItem(itemData);
        return property;
    }

    @Override
    public Property setProperty(final String name, final double value) throws RepositoryException {
        ItemData itemData = ItemData.newProperty(getPath() + "/" + name);
        Property property = new MockProperty(itemData, getSession());
        property.setValue(value);
        getMockedSession().addItem(itemData);
        return property;
    }

    @Override
    public Property setProperty(final String name, final long value) throws RepositoryException {
        ItemData itemData = ItemData.newProperty(getPath() + "/" + name);
        Property property = new MockProperty(itemData, getSession());
        property.setValue(value);
        getMockedSession().addItem(itemData);
        return property;
    }

    @Override
    public Property setProperty(final String name, final Calendar value) throws RepositoryException {
        ItemData itemData = ItemData.newProperty(getPath() + "/" + name);
        Property property = new MockProperty(itemData, getSession());
        property.setValue(value);
        getMockedSession().addItem(itemData);
        return property;
    }

    @Override
    public Property setProperty(final String name, final Node value) throws RepositoryException {
        ItemData itemData = ItemData.newProperty(getPath() + "/" + name);
        Property property = new MockProperty(itemData, getSession());
        property.setValue(value);
        getMockedSession().addItem(itemData);
        return property;
    }

    @Override
    public Property setProperty(final String name, final Binary value) throws RepositoryException {
        ItemData itemData = ItemData.newProperty(getPath() + "/" + name);
        Property property = new MockProperty(itemData, getSession());
        property.setValue(value);
        getMockedSession().addItem(itemData);
        return property;
    }

    @Override
    public Property setProperty(final String name, final BigDecimal value) throws RepositoryException {
        ItemData itemData = ItemData.newProperty(getPath() + "/" + name);
        Property property = new MockProperty(itemData, getSession());
        property.setValue(value);
        getMockedSession().addItem(itemData);
        return property;
    }

    @Override
    public boolean isNode() {
        return true;
    }

    @Override
    public boolean isNodeType(final String nodeTypeName) throws RepositoryException {
        return this.itemData.getNodeType().isNodeType(nodeTypeName);
    }

    @Override
    public NodeType getPrimaryNodeType() throws RepositoryException {
        return this.itemData.getNodeType();
    }

    @Override
    public Item getPrimaryItem() throws RepositoryException {
        // support "jcr:content" node and "jcr:data" property as primary items
        if (hasProperty(JcrConstants.JCR_DATA)) {
            return getProperty(JcrConstants.JCR_DATA);
        } else if (hasNode(JcrConstants.JCR_CONTENT)) {
            return getNode(JcrConstants.JCR_CONTENT);
        } else {
            throw new ItemNotFoundException();
        }
    }

    @Override
    public int hashCode() {
        return itemData.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MockNode) {
            return itemData.equals(((MockNode) obj).itemData);
        }
        return false;
    }

    // --- unsupported operations ---
    @Override
    public Property setProperty(final String name, final Value value, final int type) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Property setProperty(final String name, final Value[] values, final int type) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Property setProperty(final String name, final String[] values, final int type) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Property setProperty(final String name, final String value, final int type) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addMixin(final String pMixinName) throws RepositoryException {
        if (hasProperty("jcr:mixinTypes")) {
            Property property = getProperty("jcr:mixinTypes");
            Value[] values = property.getValues();
            boolean hasMixin = false;
            for (Value value : values) {
                if (value.getString().equals(pMixinName)) {
                    hasMixin = true;
                    break;
                }
            }
            if (!hasMixin) {
                String[] mixins = new String[values.length + 1];
                int index = 0;
                for (Value value : values) {
                    mixins[index] = value.getString();
                    index++;
                }
                mixins[index] = pMixinName;
                setProperty("jcr:mixinTypes", mixins);
            }
        } else {
            setProperty("jcr:mixinTypes", new String[]{pMixinName});
        }
    }

    @Override
    public boolean canAddMixin(final String pMixinName) throws RepositoryException {
        if (hasProperty("jcr:mixinTypes")) {
            Property property = getProperty("jcr:mixinTypes");
            Value[] values = property.getValues();
            for (Value value : values) {
                if (value.getString().equals(pMixinName)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void cancelMerge(final Version pVersion) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Version checkin() throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void checkout() throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void doneMerge(final Version pVersion) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Version getBaseVersion() throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getCorrespondingNodePath(final String workspaceName) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public NodeDefinition getDefinition() throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getIndex() throws RepositoryException {
        NodeIterator siblings = getParent().getNodes();
        int index = 0;
        while (siblings.hasNext()) {
            Node nextNode = siblings.nextNode();
            if (nextNode.equals(this)) {
                return index;
            }
            index++;
        }
        return index;
    }

    @Override
    public Lock getLock() throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public NodeType[] getMixinNodeTypes() throws RepositoryException {
        NodeTypeManager e = getSession().getWorkspace().getNodeTypeManager();
        if (hasProperty("jcr:mixinTypes")) {
            Property property = getProperty("jcr:mixinTypes");
            Value[] values = property.getValues();
            NodeType[] types = new NodeType[values.length];

            for (int i = 0; i < values.length; ++i) {
                types[i] = e.getNodeType(values[i].getString());
            }

            return types;
        } else {
            return new NodeType[]{};
        }
    }

    @Override
    public PropertyIterator getReferences() throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public VersionHistory getVersionHistory() throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean holdsLock() throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCheckedOut() throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isLocked() throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Lock lock(final boolean isDeep, final boolean isSessionScoped) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public NodeIterator merge(final String srcWorkspace, final boolean bestEffort) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void orderBefore(final String srcChildRelPath, final String destChildRelPath) throws RepositoryException {
        NodeIterator nodes = getNodes();
        int count = 0;
        Integer srcNodeIndex = null;
        Integer destNodeIndex = null;
        while (nodes.hasNext()) {
            Node nextNode = nodes.nextNode();
            if (nextNode.getName().equals(srcChildRelPath)) {
                srcNodeIndex = count;
            } else if (nextNode.getPath().equals(destChildRelPath)) {
                destNodeIndex = count;
            }
            count++;
        }
        if (srcNodeIndex != null && destNodeIndex != null &&
                destNodeIndex != srcNodeIndex + 1) {
            if (srcNodeIndex < destNodeIndex) {
                getSession().move(srcChildRelPath, srcChildRelPath);
            }
            nodes = getNodes();
            boolean move = false;
            count = 0;
            while (nodes.hasNext()) {
                Node nextNode = nodes.nextNode();
                if (count == destNodeIndex || nextNode.getName().equals(srcChildRelPath)) {
                    move = !move;
                }
                if (move) {
                    getSession().move(nextNode.getPath(), nextNode.getPath());
                }
                count++;
            }

        }
    }

    @Override
    public void removeMixin(final String mixinName) throws RepositoryException {
        Property property = getProperty("jcr:mixinTypes");
        Value[] values = property.getValues();

        boolean hasMixin = false;
        for (Value value : values) {
            if (value.getString().equals(mixinName)) {
                hasMixin = true;
                break;
            }
        }

        if (hasMixin) {
            String[] mixins = new String[values.length - 1];
            int index = 0;
            for (Value value : values) {
                if (!value.getString().equals(mixinName)) {
                    mixins[index] = value.getString();
                    index++;
                }
            }
            setProperty("jcr:mixinTypes", mixins);
        }
    }

    @Override
    public void restore(final String versionName, final boolean removeExisting) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void restore(final Version version, final boolean removeExisting) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void restore(final Version version, final String relPath, final boolean removeExisting) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void restoreByLabel(final String versionLabel, final boolean removeExisting) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unlock() throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void update(final String srcWorkspaceName) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void followLifecycleTransition(final String transition) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] getAllowedLifecycleTransistions() throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public NodeIterator getNodes(final String[] nameGlobs) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public PropertyIterator getProperties(final String[] nameGlobs) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public PropertyIterator getReferences(final String name) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public NodeIterator getSharedSet() throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public PropertyIterator getWeakReferences() throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public PropertyIterator getWeakReferences(final String name) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeShare() throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeSharedSet() throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setPrimaryType(final String pNodeTypeName) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

}
