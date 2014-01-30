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

import java.io.UnsupportedEncodingException;
import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.sling.api.resource.AbstractResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class JcrItemResource // this should be package private, see SLING-1414
    extends AbstractResource
    implements Resource {

    /**
     * default log
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(JcrItemResource.class);

    private final ResourceResolver resourceResolver;

    private final String path;

    private final ResourceMetadata metadata;

    protected JcrItemResource(ResourceResolver resourceResolver,
                              String path) {

        this.resourceResolver = resourceResolver;
        this.path = path;

        metadata = new ResourceMetadata();
        metadata.setResolutionPath(path);
    }

    public ResourceResolver getResourceResolver() {
        return resourceResolver;
    }

    public String getPath() {
        return path;
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

        if (node.hasProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY)) {
            result = node.getProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY).getValue().getString();
        }

        if (result == null || result.length() == 0) {
            result = node.getPrimaryNodeType().getName();
        }

        return result;
    }

    protected void setContentLength(final Property property) throws RepositoryException {
        if (property.isMultiple()) {
            return;
        }

        try {
            final long length;
            if (property.getType() == PropertyType.BINARY ) {
                // we're interested in the number of bytes, not the
                // number of characters
                length = property.getLength();
            } else {
                length = property.getString().getBytes("UTF-8").length;
            }
            getResourceMetadata().setContentLength(length);
        } catch (UnsupportedEncodingException uee) {
            LOGGER.warn("getPropertyContentLength: Cannot determine length of non-binary property {}: {}",
                    toString(), uee);
        }
    }

    /**
     * Returns an iterator over the child resources or <code>null</code> if
     * there are none.
     */
    abstract Iterator<Resource> listJcrChildren();

}
