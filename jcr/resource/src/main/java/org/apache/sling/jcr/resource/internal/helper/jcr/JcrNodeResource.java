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

import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.net.URLFactory;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.jcr.resource.JcrPropertyMap;
import org.apache.sling.jcr.resource.JcrResourceTypeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A Resource that wraps a JCR Node */
public class JcrNodeResource extends JcrItemResource {

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Node node;

    private final String resourceType;

    JcrNodeResource(ResourceResolver resourceResolver, Node node,
            JcrResourceTypeProvider[] resourceTypeProviders)
            throws RepositoryException {
        super(resourceResolver, node.getPath(), resourceTypeProviders);
        this.node = node;
        resourceType = getResourceTypeForNode(node);

        // check for nt:file metadata
        setMetaData(node, getResourceMetadata());
    }

    public String getResourceType() {
        return resourceType;
    }

    @SuppressWarnings("unchecked")
    public <Type> Type adaptTo(Class<Type> type) {
        if (type == Node.class || type == Item.class) {
            return (Type) getNode(); // unchecked cast
        } else if (type == InputStream.class) {
            return (Type) getInputStream(); // unchecked cast
        } else if (type == URL.class) {
            return (Type) getURL(); // unchecked cast
        } else if (type == Map.class || type == ValueMap.class) {
            return (Type) new JcrPropertyMap(getNode()); // unchecked cast
        }

        // fall back to default implementation
        return super.adaptTo(type);
    }

    public String toString() {
        return getClass().getSimpleName() + ", type=" + getResourceType()
            + ", path=" + getPath();
    }

    // ---------- internal -----------------------------------------------------

    private Node getNode() {
        return node;
    }

    /**
     * Returns a stream to the <em>jcr:data</em> property if the
     * {@link #getNode() node} is an <em>nt:file</em> or <em>nt:resource</em>
     * node. Otherwise returns <code>null</code>. If a valid stream can be
     * returned, this method also sets the content length resource metadata.
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

                Property data;

                // if the node has a jcr:data property, use that property
                if (content.hasProperty(JCR_DATA)) {
                    data = content.getProperty(JCR_DATA);

                } else {

                    // otherwise try to follow default item trail
                    try {
                        Item item = content.getPrimaryItem();
                        while (item.isNode()) {
                            item = ((Node) item).getPrimaryItem();
                        }
                        data = ((Property) item);
                    } catch (ItemNotFoundException infe) {
                        // we don't actually care, but log for completeness
                        log.debug("getInputStream: No primary items for "
                            + toString(), infe);
                        data = null;
                    }
                }

                if (data != null) {
                    // we set the content length only if the input stream is
                    // fetched. otherwise the repository needs to load the
                    // binary property which could cause performance loss
                    // for all resources that do need to provide the stream
                    long length = data.getLength();
                    InputStream stream = data.getStream();

                    getResourceMetadata().setContentLength(length);
                    return stream;
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

    @Override
    Iterator<Resource> listChildren() {
        try {
            if (getNode().hasNodes()) {
                return new JcrNodeResourceIterator(getResourceResolver(),
                    getNode().getNodes(), this.resourceTypeProviders);
            }
        } catch (RepositoryException re) {
            log.error("listChildren: Cannot get children of " + this, re);
        }

        return Collections.<Resource> emptyList().iterator();
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
