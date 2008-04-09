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
package org.apache.sling.jcr.resource.internal.helper.jcr;

import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_SUPER_TYPE_PROPERTY;
import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;

import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.sling.adapter.SlingAdaptable;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.resource.JcrResourceTypeProvider;
import org.apache.sling.jcr.resource.JcrResourceUtil;

abstract class JcrItemResource extends SlingAdaptable implements Resource {

    /** marker value for the resourceSupertType before trying to evaluate */
    private static final String UNSET_RESOURCE_SUPER_TYPE = "<unset>";

    private final ResourceResolver resourceResolver;

    private final String path;

    private final ResourceMetadata metadata;

    private String resourceSuperType;
    
    protected final JcrResourceTypeProvider defaultResourceTypeProvider;
    
    protected JcrItemResource(ResourceResolver resourceResolver, 
            String path, JcrResourceTypeProvider defaultResourceTypeProvider) {

        this.resourceResolver = resourceResolver;
        this.path = path;
        this.defaultResourceTypeProvider = defaultResourceTypeProvider;

        metadata = new ResourceMetadata();
        metadata.setResolutionPath(path);

        resourceSuperType = UNSET_RESOURCE_SUPER_TYPE;
    }

    public ResourceResolver getResourceResolver() {
        return resourceResolver;
    }

    public String getPath() {
        return path;
    }

    public String getResourceSuperType() {
        if (resourceSuperType == UNSET_RESOURCE_SUPER_TYPE) {

            String rst = null;

            // try local resourceSuperType "property"
            Resource typeResource = getResourceResolver().getResource(this,
                SLING_RESOURCE_SUPER_TYPE_PROPERTY);
            if (typeResource != null) {
                rst = typeResource.adaptTo(String.class);
            }

            // try explicit resourceSuperType resource
            if (rst == null) {
                String typePath = JcrResourceUtil.resourceTypeToPath(getResourceType());
                typePath += "/" + SLING_RESOURCE_SUPER_TYPE_PROPERTY;
                typeResource = getResourceResolver().getResource(typePath);
                if (typeResource != null) {
                    rst = typeResource.adaptTo(String.class);
                }
            }

            // now set the field to whatever we have, even null
            resourceSuperType = rst;

        }
        return resourceSuperType;
    }

    public ResourceMetadata getResourceMetadata() {
        return metadata;
    }

    /**
     * Compute the resource type of the given node, using either the
     * SLING_RESOURCE_TYPE_PROPERTY, or the node's primary node type, if the
     * property is not set
     */
    protected String getResourceTypeForNode(Node node)
            throws RepositoryException {
        String result = null;

        if (node.hasProperty(SLING_RESOURCE_TYPE_PROPERTY)) {
            result = node.getProperty(SLING_RESOURCE_TYPE_PROPERTY).getValue().getString();
        }
        
        if (result == null && defaultResourceTypeProvider != null) {
            result = defaultResourceTypeProvider.getResourceTypeForNode(node);
        }

        if (result == null || result.length() == 0) {
            result = node.getPrimaryNodeType().getName();
        }

        return result;
    }
    
    /**
     * Returns an iterator over the child resources or <code>null</code> if
     * there are none.
     */
    abstract Iterator<Resource> listChildren();

}
