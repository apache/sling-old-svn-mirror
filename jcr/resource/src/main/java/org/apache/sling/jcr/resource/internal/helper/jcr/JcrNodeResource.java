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
package org.apache.sling.jcr.resource.internal.helper.jcr;

import static org.apache.jackrabbit.JcrConstants.JCR_CONTENT;
import static org.apache.jackrabbit.JcrConstants.JCR_CREATED;
import static org.apache.jackrabbit.JcrConstants.JCR_DATA;
import static org.apache.jackrabbit.JcrConstants.JCR_ENCODING;
import static org.apache.jackrabbit.JcrConstants.JCR_LASTMODIFIED;
import static org.apache.jackrabbit.JcrConstants.JCR_MIMETYPE;
import static org.apache.jackrabbit.JcrConstants.NT_FILE;
import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;

import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.net.URLFactory;
import org.apache.sling.adapter.SlingAdaptable;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.jcr.resource.internal.helper.Descendable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A Resource that wraps a JCR Node */
public class JcrNodeResource extends SlingAdaptable implements Resource,
        Descendable {

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ResourceProvider resourceProvider;

    private final Node node;

    private final String path;

    private final String resourceType;

    private final ResourceMetadata metadata;

    JcrNodeResource(ResourceProvider resourceProvider, Node node)
            throws RepositoryException {
        this.resourceProvider = resourceProvider;
        this.node = node;
        this.path = node.getPath();
        metadata = new ResourceMetadata();
        metadata.setResolutionPath(path);
        resourceType = getResourceTypeForNode(node);

        // check for nt:file metadata
        setMetaData(node, metadata);
    }

    public String getPath() {
        return path;
    }

    public String getResourceType() {
        return resourceType;
    }

    public ResourceMetadata getResourceMetadata() {
        return metadata;
    }

    @SuppressWarnings("unchecked")
    public <Type> Type adaptTo(Class<Type> type) {
        if (type == Node.class || type == Item.class) {
            return (Type) getNode(); // unchecked cast
        } else if (type == InputStream.class) {
            return (Type) getInputStream(); // unchecked cast
        } else if (type == URL.class) {
            return (Type) getURL(); // unchecked cast
        }

        // fall back to default implementation
        return super.adaptTo(type);
    }

    public String toString() {
        return getClass().getSimpleName() + ", type=" + getResourceType()
            + ", path=" + getPath();
    }

    public ResourceProvider getResourceProvider() {
        return resourceProvider;
    }

    Node getNode() {
        return node;
    }

    // ---------- internal -----------------------------------------------------

    /**
     * Returns a stream to the <em>jcr:content/jcr:data</em> property if the
     * {@link #getRawData() raw data} is an <em>nt:file</em> node. Otherwise
     * returns <code>null</code>.
     */
    private InputStream getInputStream() {
        // implement this for nt:file only
        if (node != null) {
            try {
                // find the content node: for nt:file it is jcr:content
                // otherwise it is the node of this resource
                Node content = node.isNodeType(NT_FILE)
                        ? node.getNode(JCR_CONTENT)
                        : node;

                // if the node has a jcr:data property, use that property
                if (content.hasProperty(JCR_DATA)) {
                    return content.getProperty(JCR_DATA).getStream();
                }

                // otherwise try to follow default item trail
                try {
                    Item item = content.getPrimaryItem();
                    while (item.isNode()) {
                        item = ((Node) item).getPrimaryItem();
                    }
                    return ((Property) item).getStream();
                } catch (ItemNotFoundException infe) {
                    // we don't actually care, but log for completeness
                    log.debug("getInputStream: No primary items for "
                        + toString(), infe);
                }

            } catch (RepositoryException re) {
                log.error("getInputStream: Cannot get InputStream for " + this,
                    re);
            }
        }

        // fallback to non-streamable resource
        return null;
    }

    private URL getURL() {
        try {
            return URLFactory.createURL(node.getSession(), node.getPath());
        } catch (Exception ex) {
            log.error("getURL: Cannot create URL for " + this, ex);
        }

        return null;
    }

    // ---------- Descendable interface ----------------------------------------

    public Iterator<Resource> listChildren() {
        return new JcrNodeResourceIterator(this);
    }

    public Resource getDescendent(String relPath) {
        try {
            if (node.hasNode(relPath)) {
                return new JcrNodeResource(resourceProvider,
                    node.getNode(relPath));
            } else if (node.hasProperty(relPath)) {
                return new JcrPropertyResource(resourceProvider, getPath()
                    + "/" + relPath, node.getProperty(relPath));
            }

            log.error("getResource: There is no node at {} below {}", path,
                getPath());
            return null;
        } catch (RepositoryException re) {
            log.error("getResource: Problem accessing relative resource at "
                + path, re);
            return null;
        }
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

    private void setMetaData(Node node, ResourceMetadata metadata) {
        try {

            // check stuff for nt:file nodes
            if (node.isNodeType(NT_FILE)) {
                metadata.setCreationTime(node.getProperty(JCR_CREATED).getLong());

                // continue our stuff with the jcr:content node
                // which might be nt:resource, which we support below
                node = node.getNode(JCR_CONTENT);
            }

            // check stuff for nt:resource (or similar) nodes
            if (node.hasProperty(JCR_MIMETYPE)) {
                metadata.setContentType(node.getProperty(JCR_MIMETYPE).getString());
            }

            if (node.hasProperty(JCR_ENCODING)) {
                metadata.setCharacterEncoding(node.getProperty(JCR_ENCODING).getString());
            }

            if (node.hasProperty(JCR_LASTMODIFIED)) {
                metadata.setModificationTime(node.getProperty(JCR_LASTMODIFIED).getLong());
            }
        } catch (RepositoryException re) {
            log.info(
                "setMetaData: Problem extracting metadata information for "
                    + getPath(), re);
        }
    }

}
