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
import static org.apache.sling.api.resource.ResourceMetadata.CHARACTER_ENCODING;
import static org.apache.sling.api.resource.ResourceMetadata.CONTENT_TYPE;
import static org.apache.sling.api.resource.ResourceMetadata.CREATION_TIME;
import static org.apache.sling.api.resource.ResourceMetadata.MODIFICATION_TIME;
import static org.apache.sling.api.resource.ResourceMetadata.RESOLUTION_PATH;
import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;

import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;

import javax.jcr.Item;
import javax.jcr.Node;
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

    /** The relative path name of the data property of an nt:file node */
    private static final String FILE_DATA_PROP = JCR_CONTENT + "/" + JCR_DATA;

    private final ResourceProvider resourceProvider;

    private final Node node;

    private final String path;

    private final String resourceType;

    private final ResourceMetadata metadata;

//    JcrNodeResource(JcrResourceProvider resourceProvider, String path)
//            throws RepositoryException {
//        this.resourceProvider = resourceProvider;
//        node = (Node) resourceProvider.getSession().getItem(path);
//        this.path = node.getPath();
//        metadata = new ResourceMetadata();
//        resourceType = getResourceTypeForNode(node);
//
//        // check for nt:file metadata
//        setMetaData(node, metadata);
//    }

    JcrNodeResource(ResourceProvider resourceProvider, Node node)
            throws RepositoryException {
        this.resourceProvider = resourceProvider;
        this.node = node;
        this.path = node.getPath();
        metadata = new ResourceMetadata();
        metadata.put(RESOLUTION_PATH, path);
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
        return "JcrNodeResource, type=" + resourceType + ", path=" + path;
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
                if (node.isNodeType(NT_FILE)
                    && node.hasProperty(FILE_DATA_PROP)) {
                    return node.getProperty(FILE_DATA_PROP).getStream();
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

    private static void setMetaData(Node node, ResourceMetadata metadata) {
        try {
            if (node.isNodeType(NT_FILE)) {
                metadata.put(CREATION_TIME,
                    node.getProperty(JCR_CREATED).getLong());

                if (node.hasNode(JCR_CONTENT)) {
                    Node content = node.getNode(JCR_CONTENT);
                    if (content.hasProperty(JCR_MIMETYPE)) {
                        metadata.put(CONTENT_TYPE, content.getProperty(
                            JCR_MIMETYPE).getString());
                    }

                    if (content.hasProperty(JCR_ENCODING)) {
                        metadata.put(CHARACTER_ENCODING, content.getProperty(
                            JCR_ENCODING).getString());
                    }

                    if (content.hasProperty(JCR_LASTMODIFIED)) {
                        metadata.put(MODIFICATION_TIME, content.getProperty(
                            JCR_LASTMODIFIED).getLong());
                    }
                }
            }
        } catch (RepositoryException re) {
            // TODO: should log
        }
    }

}
