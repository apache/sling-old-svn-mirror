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

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.AbstractResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class JcrItemResource<T extends Item> // this should be package private, see SLING-1414
    extends AbstractResource
    implements Resource {

    /**
     * default log
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(JcrItemResource.class);

    private final ResourceResolver resourceResolver;

    protected final String path;

    protected final String version;

    private final T item;

    private final ResourceMetadata metadata;

    protected JcrItemResource(final ResourceResolver resourceResolver,
                              final String path,
                              final String version,
                              final T item,
                              final ResourceMetadata metadata) {

        this.resourceResolver = resourceResolver;
        this.path = path;
        this.version = version;
        this.item = item;
        this.metadata = metadata;
    }

    /**
     * @see org.apache.sling.api.resource.Resource#getResourceResolver()
     */
    public ResourceResolver getResourceResolver() {
        return resourceResolver;
    }

    /**
     * @see org.apache.sling.api.resource.Resource#getPath()
     */
    public String getPath() {
        if (version == null) {
            return path;
        } else if (version.contains(".")) {
            return String.format("%s;v='%s'", path, version);
        } else {
            return String.format("%s;v=%s", path, version);
        }
    }

    /**
     * @see org.apache.sling.api.resource.Resource#getResourceMetadata()
     */
    public ResourceMetadata getResourceMetadata() {
        return metadata;
    }

    /**
     * Get the underlying item. Depending on the concrete implementation either
     * a {@link javax.jcr.Node} or a {@link javax.jcr.Property}.
     *
     * @return a {@link javax.jcr.Node} or a {@link javax.jcr.Property}, depending
     *         on the implementation
     */
    protected T getItem() {
        return item;
    }

    /**
     * Compute the resource type of the given node, using either the
     * SLING_RESOURCE_TYPE_PROPERTY, or the node's primary node type, if the
     * property is not set
     */
    protected String getResourceTypeForNode(final Node node)
            throws RepositoryException {
        String result = null;

        if (node.hasProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY)) {
            result = node.getProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY).getString();
        }

        if (result == null || result.length() == 0) {
            // Do not load the relatively expensive NodeType object unless necessary. See OAK-2441 for the reason why it
            // cannot only use getProperty here.
            if (node.hasProperty(JcrConstants.JCR_PRIMARYTYPE)) {
                result = node.getProperty(JcrConstants.JCR_PRIMARYTYPE).getString();
            } else {
                result = node.getPrimaryNodeType().getName();
            }
        }

        return result;
    }

    public static long getContentLength(final Property property) throws RepositoryException {
        if (property.isMultiple()) {
            return -1;
        }

        try {
            long length = -1;
            if (property.getType() == PropertyType.BINARY ) {
                // we're interested in the number of bytes, not the
                // number of characters
                try {
                    length =  property.getLength();
                } catch (final ValueFormatException vfe) {
                    LOGGER.debug(
                        "Length of Property {} cannot be retrieved, ignored ({})",
                        property.getPath(), vfe);
                }
            } else {
                length = property.getString().getBytes("UTF-8").length;
            }
            return length;
        } catch (UnsupportedEncodingException uee) {
            LOGGER.warn("getPropertyContentLength: Cannot determine length of non-binary property {}: {}",
                    property, uee);
        }
        return -1;
    }

    /**
     * Returns an iterator over the child resources or <code>null</code> if
     * there are none.
     */
    abstract Iterator<Resource> listJcrChildren();

}
