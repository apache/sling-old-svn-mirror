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
package org.apache.sling.microsling.resource;


import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;

/** A Resource that wraps a JCR Node */
public class JcrNodeResource implements Resource {
    private final Node node;
    private final String path;
    private final String resourceType;
    private Object object;
    private final ResourceMetadata metadata;

    /** JCR Property that defines the resource type of this node
     *  (TODO: use a sling:namespaced property name)
     */
    public static final String SLING_RESOURCE_TYPE_PROPERTY = "slingResourceType";

    public static final String NODE_TYPE_RT_PREFIX = "NODETYPES/";

    JcrNodeResource(javax.jcr.Session s,String path) throws RepositoryException {
        node = (Node)s.getItem(path);
        this.path = node.getPath();
        metadata = new ResourceMetadata();
        resourceType = getResourceTypeForNode(node);
    }

    public JcrNodeResource(Node node) throws RepositoryException {
        this.node = node;
        this.path = node.getPath();
        metadata = new ResourceMetadata();
        metadata.put(ResourceMetadata.RESOLUTION_PATH, path);
        resourceType = getResourceTypeForNode(node);
    }

    public void setObject(Object object) {
        this.object = object;
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

    // no object mapping yet
    public Object getObject() {
        return object;
    }

    public ResourceMetadata getResourceMetadata() {
        return metadata;
    }

    /** Compute the resource type of the given node, using either the SLING_RESOURCE_TYPE_PROPERTY,
     *  or the node's primary type, if the property is not set
     */
    public static String getResourceTypeForNode(Node node) throws RepositoryException {
        String result = null;

        if(node.hasProperty(SLING_RESOURCE_TYPE_PROPERTY)) {
            result = node.getProperty(SLING_RESOURCE_TYPE_PROPERTY).getValue().getString().toLowerCase().trim();
        }

        if(result==null || result.length() == 0) {
            result = NODE_TYPE_RT_PREFIX + filterName(node.getPrimaryNodeType().getName());
        }

        return result;
    }

    /** Filter a node type name so that it can be used in a resource type value */
    public static String filterName(String name) {
        return name.toLowerCase().replaceAll("\\:","/");
    }
}
