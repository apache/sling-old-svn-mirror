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
package org.apache.sling.content.jcr.internal.mapping;

import java.security.AccessControlException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

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
import org.apache.sling.component.Content;
import org.apache.sling.content.jcr.DefaultContent;
import org.apache.sling.content.jcr.InvalidQueryException;
import org.apache.sling.content.jcr.JcrContentManager;


/**
 * The <code>ContentManagerImpl</code> TODO
 */
public class ContentManagerImpl extends ObjectContentManagerImpl implements
        JcrContentManager {

    /**
     * The class used to load repository content into if a mapping cannot be
     * found for a given existing node. See {@link #load(String)}.
     */
    public static final Class DEFAULT_CONTENT_CLASS = DefaultContent.class;

    protected static final String ACTION_READ = "read";
    protected static final String ACTION_CREATE = "add_node,set_property";
    protected static final String ACTION_ADD_NODE = "add_node";
    protected static final String ACTION_SET_PROPERTY = "set_property";
    protected static final String ACTION_REMOVE = "remove";

    private PersistenceManagerProviderImpl pmProvider;

    private boolean autoSave;

    /**
     *
     * @param mapper
     * @param typeConverterProvider
     * @param session
     */
    public ContentManagerImpl(PersistenceManagerProviderImpl pmProvider,
            Mapper mapper, AtomicTypeConverterProvider typeConverterProvider,
            Session session) throws RepositoryException {
        super(mapper, null, null, null, session);

        ObjectCache objectCache = new ObservingObjectCache(session); // new RequestObjectCacheImpl();
        QueryManager queryManager = new QueryManagerImpl(mapper,
            typeConverterProvider.getAtomicTypeConverters(),
            session.getValueFactory());
        ObjectConverter objectConverter = new ObjectConverterImpl(mapper,
            typeConverterProvider, new ProxyManagerImpl(), objectCache);

        this.setObjectConverter(objectConverter);
        this.setQueryManager(queryManager);
        this.setRequestObjectCache(objectCache);

        this.pmProvider = pmProvider;
    }

    public void logout() {
        // does nothing according to the ContentManager API spec
    }

    // ---------- Persistence Support ------------------------------------------

    /**
     * @see org.apache.sling.core.content.ContentManager#setAutoSave(boolean)
     */
    public void setAutoSave(boolean autoSave) {
        this.autoSave = autoSave;
    }

    /**
     * @see org.apache.sling.core.content.ContentManager#isAutoSave()
     */
    public boolean isAutoSave() {
        return this.autoSave;
    }

    /**
     * @return
     */
    public boolean hasPendingChanges() {
        try {
            return this.getSession().hasPendingChanges();
        } catch (RepositoryException re) {
            throw new ObjectContentManagerException("Problem checking for pending changes", re);
        }
    }

    /**
     * @see org.apache.jackrabbit.ocm.manager.impl.ObjectContentManagerImpl#refresh(boolean)
     */
    public void refresh(boolean keepChanges) {
        try {
            this.getSession().refresh(keepChanges);
        } catch (RepositoryException re) {
            throw new ObjectContentManagerException("Cannot rollback changes", re);
        }
    }

    /**
     * @see org.apache.sling.core.content.ContentManager#copy(org.apache.sling.core.component.Content, java.lang.String, boolean)
     */
    public void copy(Content content, String destination, boolean deep) {
        this.checkPermission(destination, ACTION_CREATE);

        if (deep) {
            // recursively copy directly in the repository
            try {
                this.getSession().getWorkspace().copy(content.getPath(), destination);
            } catch (RepositoryException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        } else {
            Content copied = this.load(content.getPath(), content.getClass());
            this.setPath(copied, destination);
            this.insert(copied);
        }
        this.conditionalSave();
    }

    /**
     * @see org.apache.sling.core.content.ContentManager#create(java.lang.String, java.lang.Class, java.util.Map)
     */
    public Content create(String path, Class objectClass, Map properties) {
        this.checkPermission(path, ACTION_CREATE);

        try {
            // create content
            Content content = (Content) objectClass.newInstance();

            // set the path on the content object
            this.setPath(content, path);

            // set initial properties
            if (properties != null) {
                for (Iterator pi = properties.entrySet().iterator(); pi.hasNext();) {
                    Map.Entry prop = (Map.Entry) pi.next();
                    if (prop.getKey() instanceof String) {
                        ReflectionUtils.setNestedProperty(content,
                            (String) prop.getKey(), prop.getValue());
                    }
                }
            }

            this.insert(content);
            this.conditionalSave();
            return content;
        } catch (IllegalAccessException iae) {
        } catch (InstantiationException ie) {
        } catch (ExceptionInInitializerError eiie) {
            // TODO: handle
        }
        return null;
    }

    /**
     * @see org.apache.sling.core.content.ContentManager#delete(org.apache.sling.core.component.Content)
     */
    public void delete(Content content) {
        this.checkPermission(content.getPath(), ACTION_REMOVE);
        this.remove(content);
        this.conditionalSave();
    }

    /**
     * @see org.apache.sling.core.content.ContentManager#delete(java.lang.String)
     */
    public void delete(String path) {
        this.checkPermission(path, ACTION_REMOVE);
        this.remove(path);
        this.conditionalSave();
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
    public Content load(String path) {
        if (this.itemExists(path)) {
            // load and return (only if content)
            try {
                Object loaded = this.getObject(path);
                if (loaded instanceof Content) {
                    return (Content) loaded;
                }
            } catch (JcrMappingException jme) {

                // fall back to default content
                try {
                    Object loaded = this.getObject(DEFAULT_CONTENT_CLASS, path);
                    if (loaded instanceof Content) {
                        return (Content) loaded;
                    }
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
    public Content load(String path, Class type) {
        if (this.itemExists(path)) {
            // load and return (only if content)
            Object loaded = this.getObject(type, path);
            if (loaded instanceof Content) {
                return (Content) loaded;
            }
        }

        // item does not exist or is no content
        return null;
    }

    /**
     * @see org.apache.sling.core.content.ContentManager#move(org.apache.sling.core.component.Content, java.lang.String)
     */
    public void move(Content content, String destination) {
        this.move(content.getPath(), destination);
    }

    /**
     * @see org.apache.jackrabbit.ocm.manager.impl.ObjectContentManagerImpl#move(java.lang.String, java.lang.String)
     */
    public void move(String source, String destination) {
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
            throw new ObjectContentManagerException("Cannot move " + source + " to "
                + destination, re);
        }

        this.conditionalSave();
    }

    /**
     * @see org.apache.sling.core.content.ContentManager#orderBefore(org.apache.sling.core.component.Content, java.lang.String)
     */
    public void orderBefore(Content content, String afterName) {
        Node parent;
        try {
            parent = this.getSession().getItem(content.getPath()).getParent();
        } catch (RepositoryException re) {
            throw new ObjectContentManagerException("Cannot get parent of "
                + content.getPath() + " to order content");
        }

        // check whether the parent node supports child node ordering
        try {
            if (!parent.getPrimaryNodeType().hasOrderableChildNodes()) {
                return;
            }
        } catch (RepositoryException re) {
            throw new ObjectContentManagerException("Cannot check whether "
                + content.getPath() + " can be ordered", re);
        }

        int ls = content.getPath().lastIndexOf('/');
        String name = content.getPath().substring(ls + 1);

        try {
            parent.orderBefore(name, afterName);
        } catch (RepositoryException re) {
            throw new ObjectContentManagerException("Cannot order " + content.getPath(),
                re);
        }

        this.conditionalSave();
    }

    /**
     * @see org.apache.sling.core.content.ContentManager#store(org.apache.sling.core.component.Content)
     */
    public void store(Content content) {
        this.update(content);
        this.conditionalSave();
    }

    //---------- JcrContentManager interface ----------------------------------

    public Iterator getObjects(String queryExpression, String language) {
        try {
            Session session = this.getSession();
            javax.jcr.query.QueryManager queryManager = session.getWorkspace().getQueryManager();
            Query query = queryManager.createQuery(queryExpression, language);
            final QueryResult queryResult = query.execute();

            return new Iterator() {
                private final NodeIterator results;
                private Object nextResult;

                {
                    this.results = queryResult.getNodes();
                    this.seek();
                }

                public boolean hasNext() {
                    return this.nextResult != null;
                }

                public Object next() {
                    if (this.nextResult == null) {
                        throw new NoSuchElementException();
                    }

                    Object result = this.nextResult;
                    this.seek();
                    return result;
                }

                public void remove() {
                    throw new UnsupportedOperationException();
                }

                private void seek() {
                    while (this.results.hasNext()) {
                        try {
                            Node node = this.results.nextNode();
                            Object value = ContentManagerImpl.this.getObject(node.getPath());
                            if (value != null) {
                                this.nextResult = value;
                                return;
                            }
                        } catch (RepositoryException re) {
                            // TODO: log this situation and continue mapping
                        } catch (ObjectContentManagerException ocme) {
                            // TODO: log this situation and continue mapping
                        } catch (Throwable t) {
                            // TODO: log this situation and continue mapping
                        }
                    }

                    // no more results
                    this.nextResult = null;
                }
            };

        } catch (javax.jcr.query.InvalidQueryException iqe) {
            throw new InvalidQueryException(iqe);
        } catch (RepositoryException re) {
            throw new ObjectContentManagerException(re.getMessage(), re);
        }
    }

    //---------- implementation helper ----------------------------------------

    protected void conditionalSave() {
        if (this.isAutoSave()) {
            this.save();
        }
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

    protected void setPath(Content content, String path) {
        ReflectionUtils.setNestedProperty(content, "path", path);
    }

}
