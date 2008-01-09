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
package org.apache.sling.jcr.resource.internal;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.AccessControlException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;
import javax.servlet.http.HttpServletRequest;

import org.apache.jackrabbit.ocm.exception.JcrMappingException;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.reflection.ReflectionUtils;
import org.apache.sling.api.SlingException;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.resource.DefaultMappedObject;
import org.apache.sling.jcr.resource.JcrResourceUtil;
import org.apache.sling.jcr.resource.PathResolver;
import org.apache.sling.jcr.resource.internal.helper.Descendable;
import org.apache.sling.jcr.resource.internal.helper.Mapping;
import org.apache.sling.jcr.resource.internal.helper.ResourcePathIterator;
import org.apache.sling.jcr.resource.internal.helper.ResourceProvider;
import org.apache.sling.jcr.resource.internal.helper.jcr.JcrNodeResource;
import org.apache.sling.jcr.resource.internal.helper.jcr.JcrNodeResourceIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>JcrResourceResolver</code> class implements the Sling
 * <code>ResourceResolver</code> and <code>ResourceResolver</code> interfaces
 * and in addition is a {@link PathResolver}. Instances of this class are
 * retrieved through the
 * {@link org.apache.sling.jcr.resource.JcrResourceResolverFactory#getResourceResolver(Session)}
 * method.
 */
public class JcrResourceResolver implements ResourceResolver, PathResolver {

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * The class used to load repository content into if a mapping cannot be
     * found for a given existing node. See {@link #getObject(String, Class)}.
     */
    public static final Class<?> DEFAULT_CONTENT_CLASS = DefaultMappedObject.class;

    protected static final String ACTION_READ = "read";

    protected static final String ACTION_CREATE = "add_node,set_property";

    protected static final String ACTION_ADD_NODE = "add_node";

    protected static final String ACTION_SET_PROPERTY = "set_property";

    protected static final String ACTION_REMOVE = "remove";

    private final JcrResourceResolverFactoryImpl factory;

    private final Session session;

    private ObjectContentManager objectContentManager;

    public JcrResourceResolver(JcrResourceResolverFactoryImpl factory,
            Session session) {
        this.factory = factory;
        this.session = session;
    }

    // ---------- ResourceResolver interface ----------------------------------

    public Resource resolve(HttpServletRequest request) throws SlingException {
        String pathInfo = request.getPathInfo();
        Resource result = resolve(pathInfo);

        if (result == null) {
            result = new NonExistingResource(pathInfo);
        }

        return result;
    }

    public Resource getResource(String path) throws SlingException {
        return getResource(path, null);
    }

    public Resource getResource(Resource base, String path)
            throws SlingException {
        // special case of absolute paths
        if (path.startsWith("/")) {
            return getResource(path);
        }

        // resolve relative path segments now
        path = JcrResourceUtil.normalize(path);
        if (path != null) {
            if (path.length() == 0) {
                // return the base resource
                return base;
            } else if (base instanceof Descendable) {
                return ((Descendable) base).getDescendent(path);
            }
        }

        // try (again) with absolute resource path
        path = base.getPath() + "/" + path;
        return getResource(path);
    }

    public Iterator<Resource> listChildren(Resource parent) {
        if (parent instanceof Descendable) {
            return ((Descendable) parent).listChildren();
        }

        try {
            parent = getResource(parent.getPath());
            if (parent instanceof Descendable) {
                return ((Descendable) parent).listChildren();
            }
        } catch (SlingException se) {
            log.warn("listChildren: Error trying to resolve parent resource "
                + parent.getPath(), se);
        }

        // return an empty iterator if parent has no node
        List<Resource> empty = Collections.emptyList();
        return empty.iterator();
    }

    public Iterator<Resource> findResources(String query, String language)
            throws SlingException {
        try {
            QueryResult res = JcrResourceUtil.query(getSession(), query,
                language);
            return new JcrNodeResourceIterator(this, res.getNodes());
        } catch (javax.jcr.query.InvalidQueryException iqe) {
            throw new SlingException(iqe);
        } catch (RepositoryException re) {
            throw new SlingException(re.getMessage(), re);
        }
    }

    public Iterator<Map<String, Object>> queryResources(String query,
            String language) throws SlingException {
        try {
            QueryResult result = JcrResourceUtil.query(getSession(), query,
                language);
            final String[] colNames = result.getColumnNames();
            final RowIterator rows = result.getRows();
            return new Iterator<Map<String, Object>>() {
                public boolean hasNext() {
                    return rows.hasNext();
                };

                public Map<String, Object> next() {
                    Map<String, Object> row = new HashMap<String, Object>();
                    try {
                        Value[] values = rows.nextRow().getValues();
                        for (int i = 0; i < values.length; i++) {
                            row.put(colNames[i],
                                JcrResourceUtil.toJavaObject(values[i]));
                        }
                    } catch (RepositoryException re) {
                        log.error(
                            "queryResources$next: Problem accessing row values",
                            re);
                    }
                    return row;
                }

                public void remove() {
                    throw new UnsupportedOperationException("remove");
                }
            };
        } catch (RepositoryException re) {
            throw new SlingException(re);
        }
    }

    @SuppressWarnings("unchecked")
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        if (type == Session.class) {
            return (AdapterType) getSession();
        } else if (type == ObjectContentManager.class) {
            return (AdapterType) objectContentManager;
        } else if (type == PathResolver.class) {
            return (AdapterType) this;
        }

        // no adapter available
        return null;
    }

    // ---------- PathResolver interface --------------------------------------

    /**
     * @throws AccessControlException If an item would exist but is not readable
     *             to this manager's session.
     */
    public Resource resolve(String uri) throws SlingException {

        // decode the request URI (required as the servlet container does not
        try {
            uri = URLDecoder.decode(uri, "UTF-8");
        } catch (UnsupportedEncodingException uee) {
            log.error("Cannot decode request URI using UTF-8", uee);
        } catch (Exception e) {
            log.error("Failed to decode request URI " + uri, e);
        }

        // resolve virtual uri
        String realUrl = factory.virtualToRealUri(uri);
        if (realUrl != null) {
            log.debug("resolve: Using real url '{}' for virtual url '{}'",
                realUrl, uri);
            uri = realUrl;
        }

        try {

            // translate url to a mapped url structure
            Resource result = transformURL(uri);
            return result;

        } catch (AccessControlException ace) {
            // rethrow AccessControlExceptions to be handled
            throw ace;

        } catch (SlingException se) {
            // rethrow SlingException as it is declared
            throw se;

        } catch (Throwable t) {
            // wrap any other issue into a SlingException
            throw new SlingException("Problem resolving " + uri, t);
        }
    }

    public String pathToURL(String path) {
        return pathToURL(null, path, null);
    }

    public String pathToURL(String prefix, String path, String suffix) {
        String href = null;

        // get first map
        Mapping[] mappings = factory.getMappings();
        for (int i = 0; i < mappings.length && href == null; i++) {
            href = mappings[i].mapHandle(path);
        }

        // if no mapping's to prefix matches the handle, use the handle itself
        if (href == null) {
            href = path;
        }

        // check virtual mappings
        String virtual = factory.realToVirtualUri(href);
        if (virtual != null) {
            log.debug("pathToURL: Using virtual URI {} for path {}", virtual,
                href);
            href = virtual;
        }

        // handle prefix and suffix
        if (prefix != null && !prefix.equals("") && !prefix.equals("/")) {
            href = prefix + href;
        }
        if (suffix != null) {
            href += suffix;
        }

        log.debug("MapHandle: {} + {} + {} -> {}", new Object[] { prefix, path,
            suffix, href });
        return href;
    }

    // ---------- former ResourceManager interface -----------------------------------

    /**
     * @throws AccessControlException If this manager has does not have enough
     *             permisssions to store the resource's object.
     */
    public void store(Resource resource) throws SlingException {
        String path = resource.getPath();
        Object data = resource.adaptTo(Object.class);
        if (data != null) {
            try {
                if (itemExists(path)) {
                    checkPermission(path, ACTION_SET_PROPERTY);
                    getObjectContentManager().update(data);
                } else {
                    checkPermission(path, ACTION_CREATE);
                    getObjectContentManager().insert(data);
                }
            } catch (RepositoryException re) {
                throw new SlingException("Problem storing object for resource "
                    + path, re);
            }
        } else {
            log.info("store: The resource {} has no object to store", path);
        }
    }

    /**
     * @throws AccessControlException if this manager has no read access
     */
    public Resource getResource(String path, Class<?> type)
            throws SlingException {
        path = JcrResourceUtil.normalize(path);
        if (path != null) {
            try {
                Resource resource = getResourceInternal(path);
                if (type != null && resource instanceof JcrNodeResource) {
                    ((JcrNodeResource) resource).setObjectType(type);
                }
                return resource;
            } catch (Exception ex) {
                throw new SlingException("Problem accessing resource" + path,
                    ex);
            }
        }

        // relative path segments cannot be resolved
        return null;
    }

    public void delete(Resource resource) throws SlingException {
        String path = resource.getPath();
        try {
            checkPermission(path, ACTION_REMOVE);
            getObjectContentManager().remove(path);
        } catch (AccessControlException ace) {
            // rethrow access control issues
            throw ace;
        } catch (Exception ex) {
            throw new SlingException("Problem deleting resource " + path, ex);
        }
    }

    public void copy(Resource resource, String destination, boolean deep)
            throws SlingException {

        String source = resource.getPath();
        try {

            checkPermission(destination, ACTION_CREATE);

            if (deep) {
                // recursively copy directly in the repository
                getSession().getWorkspace().copy(source, destination);
            } else {
                // TODO: Create node at destination:
                // - same primary node type
                // - same mixins
                // - same non-protected properties
            }

        } catch (AccessControlException ace) {
            // rethrow access control issues
            throw ace;
        } catch (Exception ex) {
            throw new SlingException("Problem copying resource " + source
                + " to " + destination, ex);
        }
    }

    public void move(Resource resource, String destination)
            throws SlingException {
        String source = resource.getPath();

        try {
            checkPermission(source, ACTION_REMOVE);
            checkPermission(destination, ACTION_CREATE);

            getSession().move(source, destination);

        } catch (AccessControlException ace) {
            // rethrow access control issues
            throw ace;
        } catch (Exception ex) {
            throw new SlingException("Problem moving resource " + source
                + " to " + destination, ex);
        }
    }

    public void orderBefore(Resource resource, String afterName)
            throws SlingException {

        String path = resource.getPath();
        Node node = resource.adaptTo(Node.class);
        if (node == null) {
            log.info("orderBefore: Resource {} is not based on a JCR", path);
            return;
        }

        try {
            Node parent = node.getParent();

            // check whether the parent node supports child node ordering
            if (!parent.getPrimaryNodeType().hasOrderableChildNodes()) {
                return;
            }

            String name = path.substring(path.lastIndexOf('/') + 1);
            parent.orderBefore(name, afterName);
        } catch (AccessControlException ace) {
            // rethrow access control issues
            throw ace;
        } catch (Exception ex) {
            throw new SlingException("Problem ordering resource " + path
                + " before " + afterName, ex);
        }
    }

    /**
     * Returns <code>true</code> if this manager has unsaved changes or if an
     * error occurrs checking for such changes.
     */
    public boolean hasChanges() {
        try {
            return getSession().hasPendingChanges();
        } catch (RepositoryException re) {
            log.error(
                "hasChanges: Problem checking for session changes, assuming true",
                re);
            return true;
        }
    }

    public void save() throws SlingException {
        try {
            getSession().save();
        } catch (RepositoryException re) {
            throw new SlingException("Problems while saving changes", re);
        }
    }

    public void rollback() {
        try {
            getSession().refresh(false);
        } catch (RepositoryException re) {
            log.error("rollback: Problem rolling back changes", re);
        }
    }

    // ---------- implementation helper ----------------------------------------

    /**
     * Loads the object to which the repository node at the given
     * <code>path</code> is mapping. If no mapping exists for an existing
     * node, the node's content is loaded into a new instance of the
     * {@link #DEFAULT_CONTENT_CLASS default content class}.
     *
     * @param type Load the node's content into an object of the given type if
     *            not <code>null</code>.
     * @return the <code>Content</code> object loaded from the node or
     *         <code>null</code> if no node exists at the given path.
     */
    public Object getObject(String path, Class<?> type) {
        try {
            if (itemExists(path)) {

                ObjectContentManager ocm = getObjectContentManager();

                // load object with explicit type, fail if not possible
                if (type != null) {
                    return ocm.getObject(type, path);
                }

                // have the mapper find a type or fall back to default type
                try {

                    return ocm.getObject(path);

                } catch (JcrMappingException jme) {

                    // fall back to default content
                    try {
                        return ocm.getObject(DEFAULT_CONTENT_CLASS, path);
                    } catch (Throwable t) {
                        // don't care for this exception, use initial one
                        throw jme;
                    }
                }
            }
        } catch (Exception ex) {
            log.error("getObject: Problem while mapping resource {}", ex);
        }

        // item does not exist or is no content or errors mapping item
        return null;
    }

    public Session getSession() {
        return session;
    }

    protected ObjectContentManager getObjectContentManager() {
        if (objectContentManager == null) {
            objectContentManager = factory.getObjectContentManager(getSession());
        }
        return objectContentManager;
    }

    private Resource transformURL(String uri) throws SlingException {
        Mapping[] mappings = factory.getMappings();
        for (int i = 0; i < mappings.length; i++) {
            // exchange the 'to'-portion with the 'from' portion and check
            String mappedUri = mappings[i].mapUri(uri);
            if (mappedUri == null) {
                log.debug("Mapping {} cannot map {}", mappings[i], uri);
                continue;
            }

            Resource resource = scanPath(mappedUri);
            if (resource != null) {

                ResourceMetadata rm = resource.getResourceMetadata();
                String path = (String) rm.get(ResourceMetadata.RESOLUTION_PATH);
                String uriPath = mappings[i].mapHandle(path);
                if (uriPath != null && !uriPath.equals(path)) {
                    resource.getResourceMetadata().put(
                        ResourceMetadata.RESOLUTION_PATH, uriPath);
                }


                return resource;
            }

            log.debug("Cannot resolve {} to resource", mappedUri);
        }

        log.info("Could not resolve URL {} to a Resource", uri);
        return null;

    }

    private Resource scanPath(String uriPath) throws SlingException {
        Resource resource = null;
        String curPath = uriPath;
        try {
            final ResourcePathIterator it = new ResourcePathIterator(uriPath);
            while (it.hasNext() && resource == null) {
                curPath = it.next();
                resource = getResourceInternal(curPath);
            }
        } catch (Exception ex) {
            throw new SlingException("Problem trying " + curPath
                + " for request path " + uriPath, ex);
        }

        return resource;
    }

    /**
     * Creates a JcrNodeResource with the given path if existing
     *
     * @throws AccessControlException If an item exists but this manager has no
     *             read access
     */
    protected Resource getResourceInternal(String path) throws Exception {

        ResourceProvider rp = factory.getResourceProvider(path);
        Resource resource = rp.getResource(this, path);
        if (resource == null && rp != factory) {
            resource = factory.getResource(this, path);
        }

        if (resource != null) {
            resource.getResourceMetadata().put(
                ResourceMetadata.RESOLUTION_PATH, path);
            return resource;
        }

        log.debug("Cannot resolve path '{}' to a resource", path);
        return null;
    }

    /**
     * Checks whether the item exists and this content manager's session has
     * read access to the item. If the item does not exist, access control is
     * ignored by this method and <code>false</code> is returned.
     *
     * @param path The path to the item to check
     * @return <code>true</code> if the item exists and this content manager's
     *         session has read access. If the item does not exist,
     *         <code>false</code> is returned ignoring access control.
     * @throws RepositoryException
     * @throws AccessControlException If the item really exists but this content
     *             manager's session has no read access to it.
     */
    public boolean itemExists(String path) throws RepositoryException {
        if (factory.itemReallyExists(getSession(), path)) {
            checkPermission(path, ACTION_READ);
            return true;
        }

        return false;
    }

    /**
     * @param path
     * @param actions
     * @throws RepositoryException
     * @throws AccessControlException if this manager does not have the
     *             permission for the listed action(s).
     */
    protected void checkPermission(String path, String actions)
            throws RepositoryException {
        getSession().checkPermission(path, actions);
    }

    protected void setPath(Object content, String path) {
        // TODO: Investigate more here !!
        ReflectionUtils.setNestedProperty(content, "path", path);
    }

}
