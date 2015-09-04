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

package org.apache.sling.bgservlets.impl;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Deep-creation of nodes: parent nodes are created if needed.
 * TOO replace with the version of JCR-2687 once that's released.
 */
public class DeepNodeCreator {
    
    /** Create a node, also creating parent nodes as needed
     * @param path Path of the node to create
     * @param session Used to create nodes
     * @param nodeType Node type of created nodes, can be
     *  overridden via {@link #getNodeType}
     * @return The created node
     * @throws RepositoryException In case of problems
     */
    public Node deepCreateNode(String path, Session session, String nodeType) 
    throws RepositoryException {
        Node result = null;
        if (session.itemExists(path)) {
            final Item it = session.getItem(path);
            if (it.isNode()) {
                result = (Node) it;
            }
        } else {
            final int slashPos = path.lastIndexOf("/");
            String parentPath = path.substring(0, slashPos);
            Node parent = null;
            if(parentPath.length() == 0) {
                // reached the root
                parent = session.getRootNode();
            } else {
                parent = deepCreateNode(parentPath, session, nodeType);
            }
            final String childPath = path.substring(slashPos + 1);
            result = parent.addNode(childPath, getNodeType(parent, childPath, nodeType));
            nodeCreated(result);
            session.save();
        }
        return result;
    }
    
    /** Can be overridden to return a specific nodetype to use at a given path.
     *  @param parent the parent of the node that is being created
     *  @param childPath the path of the child that is being created
     *  @param suggestedNodeType the nodeType value passed to {@link deepCreateNode}
     *  @return suggestedNodeType by default
     */
    protected String getNodeType(Node parent, String childPath, String suggestedNodeType) 
    throws RepositoryException {
        return suggestedNodeType;
    }
    
    /** Can be overridden to customize the created nodes, add mixins etc. */
    protected void nodeCreated(Node n) throws RepositoryException {
    }
}
