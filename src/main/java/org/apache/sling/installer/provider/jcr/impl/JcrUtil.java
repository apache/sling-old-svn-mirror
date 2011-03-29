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
package org.apache.sling.installer.provider.jcr.impl;

import java.util.StringTokenizer;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * The <code>JcrUtil</code> class provides helper methods used
 * throughout this bundle.
 */
public abstract class JcrUtil {

    private static final String FOLDER_NODE_TYPE = "sling:Folder";

    /**
     * Creates or gets the {@link javax.jcr.Node Node} at the given Path.
     *
     * @param session The session to use for node creation
     * @param absolutePath absolute node path
     * @param nodeType to use for creation of the final node
     * @return the Node at path
     * @throws RepositoryException in case of exception accessing the Repository
     */
    public static Node createPath(final Session session,
                                  final String absolutePath,
                                  final String nodeType)
    throws RepositoryException {
        final Node parentNode = session.getRootNode();
        String relativePath = absolutePath.substring(1);
        if (!parentNode.hasNode(relativePath)) {
            Node node = parentNode;
            int pos = relativePath.lastIndexOf('/');
            if ( pos != -1 ) {
                final StringTokenizer st = new StringTokenizer(relativePath.substring(0, pos), "/");
                while ( st.hasMoreTokens() ) {
                    final String token = st.nextToken();
                    if ( !node.hasNode(token) ) {
                        try {
                            node.addNode(token, FOLDER_NODE_TYPE);
                        } catch (RepositoryException re) {
                            // we ignore this as this folder might be created from a different task
                            node.refresh(false);
                        }
                    }
                    node = node.getNode(token);
                }
                relativePath = relativePath.substring(pos + 1);
            }
            if ( !node.hasNode(relativePath) ) {
                node.addNode(relativePath, nodeType);
            }
            return node.getNode(relativePath);
        }
        return parentNode.getNode(relativePath);
    }
}
