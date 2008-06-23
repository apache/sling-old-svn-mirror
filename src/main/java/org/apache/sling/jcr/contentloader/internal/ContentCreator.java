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
package org.apache.sling.jcr.contentloader.internal;

import java.io.InputStream;

import javax.jcr.RepositoryException;

/**
 * The <code>ContentCreator</code>
 * is used by the {@link ContentReader} to create the actual content.
 *
 * @since 2.0.4
 */
interface ContentCreator {

    /**
     * Create a new node.
     * To add properties to this node, one of the createProperty() methods
     * should be called.
     * To add child nodes, this method should be called to create a new child node.
     * If all properties and child nodes have been added {@link #finishNode()} must be called.
     *
     * @param name The name of the node.
     * @param primaryNodeType The primary node type or null.
     * @param mixinNodeTypes The mixin node types or null.
     * @throws RepositoryException If anything goes wrong.
     */
    void createNode(String name,
                    String primaryNodeType,
                    String[] mixinNodeTypes)
    throws RepositoryException;

    /**
     * Indicates that a node is finished.
     * The parent node of the current node becomes the current node.
     * @throws RepositoryException
     */
    void finishNode()
    throws RepositoryException;

    /**
     * Create a new property to the current node.
     * @param name The property name.
     * @param propertyType The type of the property.
     * @param value The string value.
     * @throws RepositoryException
     */
    void createProperty(String name,
                        int    propertyType,
                        String value)
    throws RepositoryException;

    /**
     * Create a new multi value property to the current node.
     * @param name The property name.
     * @param propertyType The type of the property.
     * @param values The string values.
     * @throws RepositoryException
     */
    void createProperty(String name,
                        int    propertyType,
                        String[] values)
    throws RepositoryException;

    /**
     * Add a new property to the current node.
     * @param name The property name.
     * @param value The value.
     * @throws RepositoryException
     */
    void createProperty(String name,
                        Object value)
    throws RepositoryException;

    /**
     * Add a new multi value property to the current node.
     * @param name The property name.
     * @param propertyType The type of the property.
     * @param values The values.
     * @throws RepositoryException
     */
    void createProperty(String name,
                        Object[] values)
    throws RepositoryException;

    /**
     * Create a file and a resource node.
     * After the nodes have been created, the current node is the resource node.
     * So this method call should be followed by two calls to {@link #finishNode()}
     * to be on the same level as before the file creation.
     * @param name The name of the file node
     * @param data The data of the file
     * @param mimeType The mime type or null
     * @param lastModified The last modified or -1
     * @throws RepositoryException
     */
    void createFileAndResourceNode(String name,
                                   InputStream data,
                                   String      mimeType,
                                   long         lastModified)
    throws RepositoryException;

    /**
     * Switch the current node to the path (which must be relative
     * to the current node).
     * If the path does not exist and a node type is supplied,
     * the nodes are created with the given node type.
     * If the path does not exist and node type is null, false is
     * returned.
     * When the changes to the node are finished, {@link #finishNode()}
     * must be callsed.
     * @param subPath The relative path
     * @param newNodeType Node typ for newly created nodes.
     * @throws RepositoryException
     */
    boolean switchCurrentNode(String subPath, String newNodeType)
    throws RepositoryException;
}
