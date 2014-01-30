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
import java.security.AccessControlException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import org.apache.jackrabbit.net.URLFactory;
import org.apache.sling.adapter.annotations.Adaptable;
import org.apache.sling.adapter.annotations.Adapter;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.jcr.resource.JcrModifiablePropertyMap;
import org.apache.sling.jcr.resource.JcrPropertyMap;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.apache.sling.jcr.resource.internal.JcrModifiableValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A Resource that wraps a JCR Node */
@Adaptable(adaptableClass=Resource.class, adapters={
        @Adapter({Node.class, Map.class, Item.class, ValueMap.class, URL.class}),
        @Adapter(value=PersistableValueMap.class, condition="If the resource is a JcrNodeResource and the user has set property privileges on the node."),
        @Adapter(value=InputStream.class, condition="If the resource is a JcrNodeResource and has a jcr:data property or is an nt:file node.")
})
class JcrNodeResource extends JcrItemResource { // this should be package private, see SLING-1414

    /** marker value for the resourceSupertType before trying to evaluate */
    private static final String UNSET_RESOURCE_SUPER_TYPE = "<unset>";

    /** default log */
    private static final Logger LOGGER = LoggerFactory.getLogger(JcrNodeResource.class);

    private final Node node;

    private final String resourceType;

    protected String resourceSuperType;

    private final ClassLoader dynamicClassLoader;

    /**
     * Constructor
     * @param resourceResolver
     * @param node
     * @param dynamicClassLoader Dynamic class loader for loading serialized objects.
     * @throws RepositoryException
     */
    public JcrNodeResource(final ResourceResolver resourceResolver,
                           final Node node,
                           final ClassLoader dynamicClassLoader)
    throws RepositoryException {
        super(resourceResolver, node.getPath());
        this.dynamicClassLoader = dynamicClassLoader;
        this.node = node;
        resourceType = getResourceTypeForNode(node);
        resourceSuperType = UNSET_RESOURCE_SUPER_TYPE;

        // check for nt:file metadata
        setMetaData(node, getResourceMetadata());
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getResourceSuperType() {
        // Yes, this isn't how you're supposed to compare Strings, but this is intentional.
        if ( resourceSuperType == UNSET_RESOURCE_SUPER_TYPE ) {
            try {
                if (node.hasProperty(JcrResourceConstants.SLING_RESOURCE_SUPER_TYPE_PROPERTY)) {
                    resourceSuperType = node.getProperty(JcrResourceConstants.SLING_RESOURCE_SUPER_TYPE_PROPERTY).getValue().getString();
                }
            } catch (RepositoryException re) {
                // we ignore this
            }
            if ( resourceSuperType == UNSET_RESOURCE_SUPER_TYPE ) {
                resourceSuperType = null;
            }
        }
        return resourceSuperType;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <Type> Type adaptTo(Class<Type> type) {
        if (type == Node.class || type == Item.class) {
            return (Type) getNode(); // unchecked cast
        } else if (type == InputStream.class) {
            return (Type) getInputStream(); // unchecked cast
        } else if (type == URL.class) {
            return (Type) getURL(); // unchecked cast
        } else if (type == Map.class || type == ValueMap.class) {
            return (Type) new JcrPropertyMap(getNode(), this.dynamicClassLoader); // unchecked cast
        } else if (type == PersistableValueMap.class ) {
            // check write
            try {
                getNode().getSession().checkPermission(getNode().getPath(),
                    "set_property");
                return (Type) new JcrModifiablePropertyMap(getNode(), this.dynamicClassLoader);
            } catch (AccessControlException ace) {
                // the user has no write permission, cannot adapt
                LOGGER.debug(
                    "adaptTo(PersistableValueMap): Cannot set properties on {}",
                    this);
            } catch (RepositoryException e) {
                // some other problem, cannot adapt
                LOGGER.debug(
                    "adaptTo(PersistableValueMap): Unexpected problem for {}",
                    this);
            }
        } else if (type == ModifiableValueMap.class ) {
            // check write
            try {
                getNode().getSession().checkPermission(getNode().getPath(),
                    "set_property");
                return (Type) new JcrModifiableValueMap(getNode(), this.dynamicClassLoader);
            } catch (AccessControlException ace) {
                // the user has no write permission, cannot adapt
                LOGGER.debug(
                    "adaptTo(ModifiableValueMap): Cannot set properties on {}",
                    this);
            } catch (RepositoryException e) {
                // some other problem, cannot adapt
                LOGGER.debug(
                    "adaptTo(ModifiableValueMap): Unexpected problem for {}",
                    this);
            }
        }

        // fall back to default implementation
        return super.adaptTo(type);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
        	+ ", type=" + getResourceType()
        	+ ", superType=" + getResourceSuperType()
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

                        // set the content length property as a side effect
                        // for resources which are not nt:file based and whose
                        // data is not in jcr:content/jcr:data this will lazily
                        // set the correct content length
                        this.setContentLength(data);

                    } catch (ItemNotFoundException infe) {
                        // we don't actually care, but log for completeness
                        LOGGER.debug("getInputStream: No primary items for {}", toString(), infe);
                        data = null;
                    }
                }

                if (data != null) {
                    return data.getBinary().getStream();
                }

            } catch (RepositoryException re) {
                LOGGER.error("getInputStream: Cannot get InputStream for " + this,
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
            LOGGER.error("getURL: Cannot create URL for " + this, ex);
        }

        return null;
    }

    // ---------- Descendable interface ----------------------------------------

    @Override
    Iterator<Resource> listJcrChildren() {
        try {
            if (getNode().hasNodes()) {
                return new JcrNodeResourceIterator(getResourceResolver(),
                    getNode().getNodes(), this.dynamicClassLoader);
            }
        } catch (RepositoryException re) {
            LOGGER.error("listChildren: Cannot get children of " + this, re);
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
                // if the node is new, the content node might not exist yet
                if ( !node.isNew() || node.hasNode(JCR_CONTENT) ) {
                    node = node.getNode(JCR_CONTENT);
                }
            }

            // check stuff for nt:resource (or similar) nodes
            if (node.hasProperty(JCR_MIMETYPE)) {
                metadata.setContentType(node.getProperty(JCR_MIMETYPE).getString());
            }

            if (node.hasProperty(JCR_ENCODING)) {
                metadata.setCharacterEncoding(node.getProperty(JCR_ENCODING).getString());
            }

            if (node.hasProperty(JCR_LASTMODIFIED)) {
                // We don't check node type, so JCR_LASTMODIFIED might not be a long
                final Property prop = node.getProperty(JCR_LASTMODIFIED);
                try {
                    metadata.setModificationTime(prop.getLong());
                } catch(ValueFormatException vfe) {
                    LOGGER.debug("Property {} cannot be converted to a long, ignored ({})",
                            prop.getPath(), vfe);
                }
            }

            if (node.hasProperty(JCR_DATA)) {
                final Property prop = node.getProperty(JCR_DATA);
                try {
                    metadata.setContentLength(prop.getLength());
                } catch (ValueFormatException vfe) {
                    LOGGER.debug(
                        "Length of Property {} cannot be retrieved, ignored ({})",
                        prop.getPath(), vfe);
                }
            }
        } catch (RepositoryException re) {
            LOGGER.info(
                "setMetaData: Problem extracting metadata information for "
                    + getPath(), re);
        }
    }
}
