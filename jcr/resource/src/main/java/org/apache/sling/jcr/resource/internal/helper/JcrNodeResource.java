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
package org.apache.sling.jcr.resource.internal.helper;

import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.jcr.resource.internal.JcrResourceManager;

/** A Resource that wraps a JCR Node */
public class JcrNodeResource implements Resource {

    private static final Object UNDEFINED = "undefined";

    private final JcrResourceManager cMgr;

    private final Node node;

    private final String path;

    private final String resourceType;

    private Object object = UNDEFINED;

    private Class<?> objectType;

    private final ResourceMetadata metadata;

    public JcrNodeResource(JcrResourceManager cMgr, Session s, String path)
            throws RepositoryException {
        this.cMgr = cMgr;
        node = (Node) s.getItem(path);
        this.path = node.getPath();
        metadata = new ResourceMetadata();
        resourceType = getResourceTypeForNode(node);
    }

    public JcrNodeResource(JcrResourceManager cMgr, Session s, String path,
            Class<?> type) throws RepositoryException {
        this.cMgr = cMgr;
        node = (Node) s.getItem(path);
        this.path = node.getPath();
        metadata = new ResourceMetadata();
        resourceType = getResourceTypeForNode(node);
        objectType = type;
    }

    public JcrNodeResource(JcrResourceManager cMgr, Node node)
            throws RepositoryException {
        this.cMgr = cMgr;
        this.node = node;
        this.path = node.getPath();
        metadata = new ResourceMetadata();
        metadata.put(ResourceMetadata.RESOLUTION_PATH, path);
        resourceType = getResourceTypeForNode(node);
    }

    public String toString() {
        return "JcrNodeResource, type=" + resourceType + ", path=" + path;
    }

    public Object getRawData() {
        return node;
    }

    public String getURI() {
        return path;
    }

    public String getResourceType() {
        return resourceType;
    }

    public Object getObject() {
        if (object == UNDEFINED) {
            // lazy loaded object
            object = cMgr.getObject(getURI(), objectType);
        }

        return object;
    }

    public ResourceMetadata getResourceMetadata() {
        return metadata;
    }

    /**
     * Compute the resource type of the given node, using either the
     * SLING_RESOURCE_TYPE_PROPERTY, or the node's primary type, if the property
     * is not set
     */
    public static String getResourceTypeForNode(Node node)
            throws RepositoryException {
        String result = null;

        if (node.hasProperty(SLING_RESOURCE_TYPE_PROPERTY)) {
            result = node.getProperty(SLING_RESOURCE_TYPE_PROPERTY).getValue().getString();
        }

        if (result == null || result.length() == 0) {
            result = node.getPrimaryNodeType().getName();
        }

        return result;
    }
}
