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
package org.apache.sling.jcr.resource.internal.mapping;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import org.apache.jackrabbit.ocm.exception.JcrMappingException;
import org.apache.jackrabbit.ocm.exception.ObjectContentManagerException;
import org.apache.jackrabbit.ocm.manager.atomictypeconverter.AtomicTypeConverterProvider;
import org.apache.jackrabbit.ocm.manager.cache.ObjectCache;
import org.apache.jackrabbit.ocm.manager.impl.ObjectContentManagerImpl;
import org.apache.jackrabbit.ocm.manager.objectconverter.ObjectConverter;
import org.apache.jackrabbit.ocm.manager.objectconverter.impl.ObjectConverterImpl;
import org.apache.jackrabbit.ocm.manager.objectconverter.impl.ProxyManagerImpl;
import org.apache.jackrabbit.ocm.mapper.Mapper;
import org.apache.jackrabbit.ocm.query.QueryManager;
import org.apache.jackrabbit.ocm.query.impl.QueryManagerImpl;
import org.apache.jackrabbit.ocm.reflection.ReflectionUtils;
import org.apache.sling.api.SlingException;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceManager;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.jcr.resource.DefaultMappedObject;
import org.apache.sling.jcr.resource.internal.util.JcrHelper;
import org.apache.sling.jcr.resource.internal.util.JcrNodeResource;
import org.apache.sling.jcr.resource.internal.util.ResourcePathIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The <code>ContentManagerImpl</code> TODO
 */
public class ContentManagerImpl implements ResourceManager {

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * The class used to load repository content into if a mapping cannot be
     * found for a given existing node. See {@link #load(String)}.
     */
    public static final Class DEFAULT_CONTENT_CLASS = DefaultMappedObject.class;

    protected static final String ACTION_READ = "read";
    protected static final String ACTION_CREATE = "add_node,set_property";
    protected static final String ACTION_ADD_NODE = "add_node";
    protected static final String ACTION_SET_PROPERTY = "set_property";
    protected static final String ACTION_REMOVE = "remove";

    private PersistenceManagerProviderImpl pmProvider;

    private ObjectContentManagerImpl ocm;

    /**
     *
     * @param mapper
     * @param typeConverterProvider
     * @param session
     */
    public ContentManagerImpl(PersistenceManagerProviderImpl pmProvider,
            Mapper mapper, AtomicTypeConverterProvider typeConverterProvider,
            Session session) throws RepositoryException {
        ocm = new ObjectContentManagerImpl(mapper, null, null, null, session);

        ObjectCache objectCache = new ObservingObjectCache(session); // new RequestObjectCacheImpl();
        QueryManager queryManager = new QueryManagerImpl(mapper,
            typeConverterProvider.getAtomicTypeConverters(),
            session.getValueFactory());
        ObjectConverter objectConverter = new ObjectConverterImpl(mapper,
            typeConverterProvider, new ProxyManagerImpl(), objectCache);

        ocm.setObjectConverter(objectConverter);
        ocm.setQueryManager(queryManager);
        ocm.setRequestObjectCache(objectCache);

        this.pmProvider = pmProvider;
    }

    protected Session getSession() {
        return ocm.getSession();
    }

    // ---------- Persistence Support ------------------------------------------

    /**
     * @return
     */
    public boolean hasChanges() {
        try {
            return this.getSession().hasPendingChanges();
        } catch (RepositoryException re) {
            throw new ObjectContentManagerException("Problem checking for pending changes", re);
        }
    }

    /**
     * @see org.apache.jackrabbit.ocm.manager.impl.ObjectContentManagerImpl#refresh(boolean)
     */
    public void rollback() {
        try {
            getSession().refresh(false);
        } catch (RepositoryException re) {
            throw new ObjectContentManagerException("Cannot rollback changes", re);
        }
    }

    public void save() throws SlingException {
        try {
            getSession().save();
        } catch (RepositoryException re) {
            throw new SlingException("Cannot save changes", re);
        }
    }

    public void copy(Resource resource, String destination, boolean deep)
            throws SlingException {
        this.checkPermission(destination, ACTION_CREATE);

        if (deep) {
            // recursively copy directly in the repository
            try {
                getSession().getWorkspace().copy(resource.getURI(), destination);
            } catch (RepositoryException e) {
                e.printStackTrace(); // To change body of catch statement use
                                        // File | Settings | File Templates.
            }
        } else {
            Object copied = load(resource.getURI(),
                resource.getObject().getClass());
            setPath(copied, destination);
            ocm.insert(copied);
        }
    }

    /**
     * @see org.apache.sling.content.ContentManager#create(org.apache.sling.component.Content)
     */
    public void create(Resource resource) {
        if (resource.getObject() != null) {
            String path = resource.getURI();

            this.checkPermission(path, ACTION_CREATE);

            // TODO handle exceptions correctly
            ocm.insert(resource.getObject());
        }
    }

    /**
     * @see org.apache.sling.core.content.ContentManager#delete(org.apache.sling.core.component.Content)
     */
    public void delete(Resource resource) throws SlingException {
        this.checkPermission(resource.getURI(), ACTION_REMOVE);
        ocm.remove(resource.getURI());
    }

    /**
     * @see org.apache.sling.core.content.ContentManager#delete(java.lang.String)
     */
    public void delete(String path) {
        this.checkPermission(path, ACTION_REMOVE);
        ocm.remove(path);
    }

    /**
     * Loads the content of the repository node at the given <code>path</code>
     * into a <code>Content</code> object. If no mapping exists for an existing
     * node, the node's content is loaded into a new instance of the
     * {@link #DEFAULT_CONTENT_CLASS default content class}.
     *
     * @return the <code>Content</code> object loaded from the node or
     *      <code>null</code> if no node exists at the given path.
     *
     * @throws JcrMappingException If an error occurrs loading the node into
     *      a <code>Content</code> object.
     * @throws AccessControlException If the item really exists but this content
     *             manager's session has no read access to it.
     */
    public Object load(String path) {
        if (this.itemExists(path)) {
            // load and return (only if content)
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

    /**
     * @see org.apache.sling.core.content.ContentManager#load(java.lang.String, java.lang.Class)
     */
    public Object load(String path, Class<?> type) {
        if (this.itemExists(path)) {
            // load and return (only if content)
            return ocm.getObject(type, path);
        }

        // item does not exist or is no content
        return null;
    }

    /**
     * @see org.apache.sling.core.content.ContentManager#move(org.apache.sling.core.component.Content, java.lang.String)
     */
    public void move(Resource resource, String destination)
            throws SlingException {
        this.move(resource.getURI(), destination);
    }

    /**
     * @see org.apache.jackrabbit.ocm.manager.impl.ObjectContentManagerImpl#move(java.lang.String, java.lang.String)
     */
    public void move(String source, String destination) throws SlingException {
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

    /**
     * @see org.apache.sling.core.content.ContentManager#orderBefore(org.apache.sling.core.component.Content, java.lang.String)
     */
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
     * @see org.apache.sling.core.content.ContentManager#store(org.apache.sling.core.component.Content)
     */
    public void store(Resource resource) throws SlingException {
        if (resource.getObject() != null) {
            ocm.update(resource.getObject());
        }
    }

    //---------- JcrContentManager interface ----------------------------------

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

    public Iterator<Resource> findResources(String query, String language)
    throws SlingException {
        try {
            QueryResult res = JcrHelper.query(getSession(), query, language);
            final NodeIterator nodes = res.getNodes();

            return new Iterator<Resource>() {

                private Resource nextResult = seek();

                public boolean hasNext() {
                    return nextResult != null;
                }

                public Resource next() {
                    if (nextResult == null) {
                        throw new NoSuchElementException();
                    }

                    Resource result = nextResult;
                    nextResult = seek();
                    return result;
                }

                public void remove() {
                    throw new UnsupportedOperationException();
                }

                private Resource seek() {
                    while (nodes.hasNext()) {
                        try {
                            return new JcrNodeResource(ContentManagerImpl.this,
                                nodes.nextNode());
                        } catch (RepositoryException re) {
                            // TODO: log this situation and continue mapping
                        } catch (ObjectContentManagerException ocme) {
                            // TODO: log this situation and continue mapping
                        } catch (Throwable t) {
                            // TODO: log this situation and continue mapping
                        }
                    }

                    // no more results
                    return null;
                }
            };

        } catch (javax.jcr.query.InvalidQueryException iqe) {
            throw new SlingException(iqe);
        } catch (RepositoryException re) {
            throw new SlingException(re.getMessage(), re);
        }
    }

    //---------- implementation helper ----------------------------------------

    /** Creates a JcrNodeResource with the given path if existing */
    protected Resource getResourceInternal(String path, Class<?> type)
            throws RepositoryException {
        Session session = getSession();
        if (session.itemExists(path)) {
            Resource result;

            if (type != null) {
                Object mapped = load(path, type);
                result = new JcrNodeResource(session, path, mapped);
            } else {
                result = new JcrNodeResource(this, session, path);
            }
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
     *
     * @throws AccessControlException If the item really exists but this content
     *             manager's session has no read access to it.
     */
    protected boolean itemExists(String path) {
        try {
            if (this.pmProvider.itemReallyExists(this.getSession(), path)) {
                this.checkPermission(path, ACTION_READ);
                return true;
            }

            return false;
        } catch (RepositoryException re) {
            throw new org.apache.jackrabbit.ocm.exception.RepositoryException(re);
        }
    }

    protected void checkPermission(String path, String actions) {
        try {
            this.getSession().checkPermission(path, actions);
        } catch (RepositoryException re) {
            throw new org.apache.jackrabbit.ocm.exception.RepositoryException(re);
        }
    }

    protected void setPath(Object content, String path) {
        // TODO: Investigate more here !!
        ReflectionUtils.setNestedProperty(content, "path", path);
    }

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
                final NodeIterator children = ((Node) parent.getRawData()).getNodes();
                return new Iterator<Resource>() {

                    public boolean hasNext() {
                        return children.hasNext();
                    }

                    public Resource next() {
                        try {
                            return new JcrNodeResource(ContentManagerImpl.this,
                                children.nextNode());
                        } catch (RepositoryException re) {
                            log.warn(
                                "Problem while trying to create a resource", re);
                            return new NonExistingResource(parent.getURI()
                                + "/?");
                        }
                    }

                    public void remove() {
                        throw new UnsupportedOperationException("remove");
                    }

                };
            } catch (RepositoryException re) {
                throw new SlingException("Cannot get children of Resource "
                    + parent, re);
            }
        }

        // return an empty iterator if parent has no node
        List<Resource> empty = Collections.emptyList();
        return empty.iterator();
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
                            row.put(colNames[i], JcrHelper.toJavaObject(values[i]));
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

}
