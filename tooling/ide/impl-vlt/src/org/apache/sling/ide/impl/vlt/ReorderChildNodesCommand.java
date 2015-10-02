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

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.apache.jackrabbit.vault.util.Text;
import org.apache.sling.ide.log.Logger;
import org.apache.sling.ide.transport.ResourceProxy;

/**
 * The <tt>ReorderChildNodesCommand</tt> reorders the child nodes of the node matching the specified resource
 *
 */
public class ReorderChildNodesCommand extends JcrCommand<Void> {

    private final ResourceProxy resource;

    public ReorderChildNodesCommand(Repository repository, Credentials credentials, ResourceProxy resource,
            Logger logger) {
        super(repository, credentials, resource.getPath(), logger);

        this.resource = resource;
    }

    @Override
    protected Void execute0(Session session) throws RepositoryException, IOException {

        boolean nodeExists = session.nodeExists(getPath());
        if (!nodeExists) {
            return null;
        }

        Node node = session.getNode(getPath());

        NodeType primaryNodeType = node.getPrimaryNodeType();

        if (primaryNodeType.hasOrderableChildNodes()) {
            reorderChildNodes(node, resource);
        }
        
        return null;
    }

    private void reorderChildNodes(Node nodeToReorder, ResourceProxy resourceToReorder) throws RepositoryException {

        List<ResourceProxy> children = resourceToReorder.getChildren();
        ListIterator<ResourceProxy> childrenIterator = children.listIterator();

        // do not process
        if (!childrenIterator.hasNext()) {
            getLogger().trace("Resource at {0} has no children, skipping child node reordering",
                            resourceToReorder.getPath());
            return;
        }

        Set<String> resourceChildNames = new HashSet<String>(children.size());
        Set<String> nodeChildNames = new HashSet<String>(children.size());

        List<Node> nodeChildren = new LinkedList<Node>();
        NodeIterator nodeChildrenIt = nodeToReorder.getNodes();
        while (nodeChildrenIt.hasNext()) {
            Node node = nodeChildrenIt.nextNode();
            nodeChildren.add(node);
            nodeChildNames.add(node.getName());
        }

        for (ResourceProxy childResources : children) {
            resourceChildNames.add(Text.getName(childResources.getPath()));
        }
        ListIterator<Node> nodeChildrenListIt = nodeChildren.listIterator();

        // the sorting is conditioned on the local workspace and the repository having the same child names
        // if that precondition is not met abort processing since there can be no meaningful result

        traceResourcesAndNodes(children, nodeChildren);

        if (! resourceChildNames.equals(nodeChildNames)) {
            getLogger().warn(
                    "Different child names between the local workspace ( " + resourceChildNames
                            + ") and the repository (" + nodeChildNames + ") for path " + resource.getPath()
                            + ". Reordering will not be performed");
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
                getLogger().trace("Node at {0} does not have orderable child nodes, skipping reordering of {1}",
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

            getLogger().trace("For node at {0} ordering {1} before {2}", nodeToReorder.getPath(),
                    Text.getName(childResource.getPath()), expectedParentName);

            nodeToReorder.orderBefore(Text.getName(childResource.getPath()), expectedParentName);
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

        getLogger().trace(out.toString());
    }
    
    @Override
    public Kind getKind() {
        return Kind.REORDER_CHILDREN;
    }
}
