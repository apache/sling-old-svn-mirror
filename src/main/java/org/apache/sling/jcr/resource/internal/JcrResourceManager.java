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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.apache.jackrabbit.ocm.exception.JcrMappingException;
import org.apache.jackrabbit.ocm.exception.ObjectContentManagerException;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.reflection.ReflectionUtils;
import org.apache.sling.api.SlingException;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceManager;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.jcr.resource.DefaultMappedObject;
import org.apache.sling.jcr.resource.internal.helper.JcrHelper;
import org.apache.sling.jcr.resource.internal.helper.JcrNodeResource;
import org.apache.sling.jcr.resource.internal.helper.JcrNodeResourceIterator;
import org.apache.sling.jcr.resource.internal.helper.ResourcePathIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>JcrResourceManager</code> TODO
 */
public class JcrResourceManager implements ResourceManager {

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

    private final JcrResourceManagerFactoryImpl factory;

    private final Session session;

    private ObjectContentManager objectContentManager;

    public JcrResourceManager(JcrResourceManagerFactoryImpl factory,
            Session session) {
        this.factory = factory;
        this.session = session;
    }

    protected Session getSession() {
        return session;
    }

    protected ObjectContentManager getObjectContentManager() {
        if (objectContentManager == null) {
            objectContentManager = factory.getObjectContentManager(getSession());
        }
        return objectContentManager;
    }

    // ---------- ResourceResolver interface ----------------------------------

    public Resource resolve(ServletRequest request) throws SlingException {
        Resource result = null;
        String path = null;
        String pathInfo = ((HttpServletRequest) request).getPathInfo();
        try {
            final ResourcePathIterator it = new ResourcePathIterator(pathInfo);
            while (it.hasNext() && result == null) {
                result = getResourceInternal(it.next(), null);
            }
        } catch (RepositoryException re) {
            throw new SlingException("RepositoryException for path=" + path, re);
        }

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
        path = JcrHelper.resolveRelativeSegments(path);
        if (path != null) {
            if (path.length() == 0) {
                // return the base resource
                return base;
            } else if (base.getRawData() instanceof Node) {
                try {
                    Node baseNode = (Node) base.getRawData();
                    if (baseNode.hasNode(path)) {
                        return new JcrNodeResource(this, baseNode.getNode(path));
                    }

                    log.error("getResource: There is no node at {} below {}",
                        path, base.getURI());
                    return null;
                } catch (RepositoryException re) {
                    log.error(
                        "getResource: Problem accessing relative resource at "
                            + path, re);
                    return null;
                }
            }
        }

        // try (again) with absolute resource path
        path = base.getURI() + "/" + path;
        return getResource(path);
    }

    public Iterator<Resource> listChildren(final Resource parent)
            throws SlingException {
        if (parent.getRawData() instanceof Node) {

            try {
                return new JcrNodeResourceIterator(this,
                    ((Node) parent.getRawData()).getNodes());
            } catch (RepositoryException re) {
                throw new SlingException("Cannot get children of Resource "
                    + parent, re);
            }
        }

        // return an empty iterator if parent has no node
        List<Resource> empty = Collections.emptyList();
        return empty.iterator();
    }

    public Iterator<Resource> findResources(String query, String language)
            throws SlingException {
        try {
            QueryResult res = JcrHelper.query(getSession(), query, language);
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
            QueryResult result = JcrHelper.query(getSession(), query, language);
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
                                JcrHelper.toJavaObject(values[i]));
                        }
                    } catch (RepositoryException re) {
                        // TODO:log
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

    // ---------- ResourceManager interface -----------------------------------

    public Resource getResource(String path, Class<?> type)
            throws SlingException {
        path = JcrHelper.resolveRelativeSegments(path);
        if (path != null) {
            try {
                return getResourceInternal(path, type);
            } catch (RepositoryException re) {
                throw new SlingException("Cannot get resource " + path, re);
            }
        }

        // relative path segments cannot be resolved
        return null;
    }

    public void store(Resource resource) throws SlingException {
        if (resource.getObject() != null) {
            String path = resource.getURI();

            if (itemExists(path)) {
                checkPermission(path, ACTION_SET_PROPERTY);
                getObjectContentManager().update(resource.getObject());
            } else {
                this.checkPermission(path, ACTION_CREATE);
                getObjectContentManager().insert(resource.getObject());
            }
        }
    }

    public void delete(Resource resource) throws SlingException {
        String path = resource.getURI();
        this.checkPermission(path, ACTION_REMOVE);
        getObjectContentManager().remove(path);
    }

    public void copy(Resource resource, String destination, boolean deep)
            throws SlingException {
        this.checkPermission(destination, ACTION_CREATE);

        if (deep) {
            // recursively copy directly in the repository
            try {
                getSession().getWorkspace().copy(resource.getURI(), destination);
            } catch (RepositoryException e) {
                e.printStackTrace();
            }
        } else {
            Object copied = getObjectContentManager().getObject(
                resource.getObject().getClass(), resource.getURI());
            setPath(copied, destination);
            getObjectContentManager().insert(copied);
        }
    }

    public void move(Resource resource, String destination)
            throws SlingException {
        String source = resource.getURI();

        this.checkPermission(source, ACTION_REMOVE);
        this.checkPermission(destination, ACTION_CREATE);

        try {
            this.getSession().move(source, destination);
            // } catch (ItemExistsException iee) {
            // } catch (PathNotFoundException pnfe) {
            // } catch (VersionException ve) {
            // } catch (ConstraintViolationException cve) {
            // } catch (LockException le) {
        } catch (RepositoryException re) {
            throw new SlingException("Cannot move " + source + " to "
                + destination, re);
        }
    }

    public void orderBefore(Resource resource, String afterName)
            throws SlingException {
        Node parent;
        try {
            parent = ((Item) resource.getRawData()).getParent();
        } catch (RepositoryException re) {
            throw new SlingException("Cannot get parent of "
                + resource.getURI() + " to order content");
        }

        // check whether the parent node supports child node ordering
        try {
            if (!parent.getPrimaryNodeType().hasOrderableChildNodes()) {
                return;
            }
        } catch (RepositoryException re) {
            throw new SlingException("Cannot check whether "
                + resource.getURI() + " can be ordered", re);
        }

        int ls = resource.getURI().lastIndexOf('/');
        String name = resource.getURI().substring(ls + 1);

        try {
            parent.orderBefore(name, afterName);
        } catch (RepositoryException re) {
            throw new SlingException("Cannot order " + resource.getURI(), re);
        }
    }

    /**
     * @return
     */
    public boolean hasChanges() {
        try {
            return this.getSession().hasPendingChanges();
        } catch (RepositoryException re) {
            throw new ObjectContentManagerException(
                "Problem checking for pending changes", re);
        }
    }

    public void save() throws SlingException {
        try {
            getSession().save();
        } catch (RepositoryException re) {
            throw new SlingException("Cannot save changes", re);
        }
    }

    public void rollback() {
        try {
            getSession().refresh(false);
        } catch (RepositoryException re) {
            throw new ObjectContentManagerException("Cannot rollback changes",
                re);
        }
    }

    // ---------- Persistence Support -----------------------------------------

    /**
     * Loads the content of the repository node at the given <code>path</code>
     * into a <code>Content</code> object. If no mapping exists for an
     * existing node, the node's content is loaded into a new instance of the
     * {@link #DEFAULT_CONTENT_CLASS default content class}.
     *
     * @return the <code>Content</code> object loaded from the node or
     *         <code>null</code> if no node exists at the given path.
     * @throws JcrMappingException If an error occurrs loading the node into a
     *             <code>Content</code> object.
     * @throws AccessControlException If the item really exists but this content
     *             manager's session has no read access to it.
     */
    public Object getObject(String path, Class<?> type) {
        if (this.itemExists(path)) {

            ObjectContentManager ocm = getObjectContentManager();

            // load object with explicite type, fail completely if not possible
            if (type != null) {
                return ocm.getObject(type, path);
            }

            // have the mapper find a type or fall back to default type
            try {
                Object loaded = ocm.getObject(path);
                if (loaded != null) {
                    return loaded;
                }
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

        // item does not exist or is no content
        return null;
    }

    // ---------- implementation helper ----------------------------------------

    /** Creates a JcrNodeResource with the given path if existing */
    protected Resource getResourceInternal(String path, Class<?> type)
            throws RepositoryException {
        Session session = getSession();
        if (session.itemExists(path)) {
            Resource result = new JcrNodeResource(this, session, path, type);
            result.getResourceMetadata().put(ResourceMetadata.RESOLUTION_PATH,
                path);
            log.info("Found Resource at path '{}'", path);
            return result;
        }

        log.info("Path '{}' does not resolve to an Item", path);
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
     * @throws AccessControlException If the item really exists but this content
     *             manager's session has no read access to it.
     */
    protected boolean itemExists(String path) {
        try {
            if (factory.itemReallyExists(this.getSession(), path)) {
                this.checkPermission(path, ACTION_READ);
                return true;
            }

            return false;
        } catch (RepositoryException re) {
            throw new org.apache.jackrabbit.ocm.exception.RepositoryException(
                re);
        }
    }

    protected void checkPermission(String path, String actions) {
        try {
            this.getSession().checkPermission(path, actions);
        } catch (RepositoryException re) {
            throw new org.apache.jackrabbit.ocm.exception.RepositoryException(
                re);
        }
    }

    protected void setPath(Object content, String path) {
        // TODO: Investigate more here !!
        ReflectionUtils.setNestedProperty(content, "path", path);
    }

}
