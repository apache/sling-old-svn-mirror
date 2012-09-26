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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.SlingException;
import org.apache.sling.api.adapter.SlingAdaptable;
import org.apache.sling.api.resource.AttributableResourceProvider;
import org.apache.sling.api.resource.DynamicResourceProvider;
import org.apache.sling.api.resource.ModifyingResourceProvider;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.QueriableResourceProvider;
import org.apache.sling.api.resource.QuerySyntaxException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.jcr.resource.JcrResourceUtil;
import org.apache.sling.jcr.resource.internal.JcrModifiableValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>JcrResourceProvider</code> is the main resource provider of this
 * bundle providing access to JCR resources. This resoure provider is created
 * for each <code>JcrResourceResolver</code> instance and is bound to the JCR
 * session for a single request.
 */
public class JcrResourceProvider
    extends SlingAdaptable
    implements ResourceProvider,
               DynamicResourceProvider,
               AttributableResourceProvider,
               QueriableResourceProvider,
               ModifyingResourceProvider {

    /** column name for node path */
    private static final String QUERY_COLUMN_PATH = "jcr:path";

    /** column name for score value */
    private static final String QUERY_COLUMN_SCORE = "jcr:score";

    @SuppressWarnings("deprecation")
    private static final String DEFAULT_QUERY_LANGUAGE = Query.XPATH;

    /** Default logger */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /** Flag for closing. */
    private boolean closed = false;

    private final Session session;
    private final ClassLoader dynamicClassLoader;
    private final boolean closeSession;

    public JcrResourceProvider(final Session session,
                               final ClassLoader dynamicClassLoader,
                               final boolean closeSession) {
        this.session = session;
        this.dynamicClassLoader = dynamicClassLoader;
        this.closeSession = closeSession;
    }

    // ---------- ResourceProvider interface ----------------------------------

    /**
     * @see org.apache.sling.api.resource.ResourceProvider#getResource(org.apache.sling.api.resource.ResourceResolver, javax.servlet.http.HttpServletRequest, java.lang.String)
     */
    @SuppressWarnings("javadoc")
    public Resource getResource(ResourceResolver resourceResolver,
            HttpServletRequest request, String path) throws SlingException {
        return getResource(resourceResolver, path);
    }

    /**
     * @see org.apache.sling.api.resource.ResourceProvider#getResource(org.apache.sling.api.resource.ResourceResolver, java.lang.String)
     */
    public Resource getResource(ResourceResolver resourceResolver, String path)
    throws SlingException {
        this.checkClosed();
        try {
            return createResource(resourceResolver, path);
        } catch (RepositoryException re) {
            throw new SlingException("Problem retrieving node based resource "
                + path, re);
        }

    }

    /**
     * @see org.apache.sling.api.resource.ResourceProvider#listChildren(org.apache.sling.api.resource.Resource)
     */
    public Iterator<Resource> listChildren(final Resource parent) {
        this.checkClosed();

        JcrItemResource parentItemResource;

        // short cut for known JCR resources
        if (parent instanceof JcrItemResource) {

            parentItemResource = (JcrItemResource) parent;

        } else {

            // try to get the JcrItemResource for the parent path to list
            // children
            try {
                parentItemResource = createResource(
                    parent.getResourceResolver(), parent.getPath());
            } catch (RepositoryException re) {
                parentItemResource = null;
            }

        }

        // return children if there is a parent item resource, else null
        return (parentItemResource != null)
                ? parentItemResource.listJcrChildren()
                : null;
    }

    // ---------- implementation helper ----------------------------------------

    /**
     * Creates a <code>Resource</code> instance for the item found at the
     * given path. If no item exists at that path or the item does not have
     * read-access for the session of this resolver, <code>null</code> is
     * returned.
     *
     * @param path The absolute path
     * @return The <code>Resource</code> for the item at the given path.
     * @throws RepositoryException If an error occurrs accessingor checking the
     *             item in the repository.
     */
    private JcrItemResource createResource(final ResourceResolver resourceResolver,
            final String path) throws RepositoryException {
        if (itemExists(path)) {
            Item item = session.getItem(path);
            if (item.isNode()) {
                log.debug(
                    "createResource: Found JCR Node Resource at path '{}'",
                    path);
                return new JcrNodeResource(resourceResolver, (Node) item, dynamicClassLoader);
            }

            log.debug(
                "createResource: Found JCR Property Resource at path '{}'",
                path);
            return new JcrPropertyResource(resourceResolver, path,
                (Property) item);
        }

        log.debug("createResource: No JCR Item exists at path '{}'", path);
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
     */
    private boolean itemExists(final String path) {

        try {
            return session.itemExists(path);
        } catch (RepositoryException re) {
            log.debug("itemExists: Error checking for existence of {}: {}",
                path, re.toString());
            return false;
        }
    }

    /**
     * @see org.apache.sling.api.resource.DynamicResourceProvider#isLive()
     */
    public boolean isLive() {
        return !closed && session.isLive();
    }

    /**
     * @see org.apache.sling.api.resource.DynamicResourceProvider#close()
     */
    public void close() {
        if ( this.closeSession && !closed) {
            session.logout();
        }
        this.closed = true;
    }

    /**
     * Check if the resource resolver is already closed.
     *
     * @throws IllegalStateException If the resolver is already closed
     */
    private void checkClosed() {
        if ( this.closed ) {
            throw new IllegalStateException("Resource resolver is already closed.");
        }
    }

    /**
     * @see org.apache.sling.api.resource.QueriableResourceProvider#findResources(ResourceResolver, java.lang.String, java.lang.String)
     */
    public Iterator<Resource> findResources(final ResourceResolver resolver,
                    final String query, final String language) {
        checkClosed();

        try {
            final QueryResult res = JcrResourceUtil.query(session, query, language);
            return new JcrNodeResourceIterator(resolver, res.getNodes(), this.dynamicClassLoader);
        } catch (final javax.jcr.query.InvalidQueryException iqe) {
            throw new QuerySyntaxException(iqe.getMessage(), query, language, iqe);
        } catch (final RepositoryException re) {
            throw new SlingException(re.getMessage(), re);
        }
    }

    /**
     * @see org.apache.sling.api.resource.QueriableResourceProvider#queryResources(ResourceResolver, java.lang.String, java.lang.String)
     */
    public Iterator<Map<String, Object>> queryResources(final ResourceResolver resolver, final String query, final String language) {
        checkClosed();

        final String queryLanguage = isSupportedQueryLanguage(language) ? language : DEFAULT_QUERY_LANGUAGE;

        try {
            QueryResult result = JcrResourceUtil.query(session, query,
                queryLanguage);
            final String[] colNames = result.getColumnNames();
            final RowIterator rows = result.getRows();
            return new Iterator<Map<String, Object>>() {
                public boolean hasNext() {
                    return rows.hasNext();
                };

                public Map<String, Object> next() {
                    Map<String, Object> row = new HashMap<String, Object>();
                    try {
                        Row jcrRow = rows.nextRow();
                        boolean didPath = false;
                        boolean didScore = false;
                        Value[] values = jcrRow.getValues();
                        for (int i = 0; i < values.length; i++) {
                            Value v = values[i];
                            if (v != null) {
                                String colName = colNames[i];
                                row.put(colName,
                                    JcrResourceUtil.toJavaObject(values[i]));
                                if (colName.equals(QUERY_COLUMN_PATH)) {
                                    didPath = true;
                                }
                                if (colName.equals(QUERY_COLUMN_SCORE)) {
                                    didScore = true;
                                }
                            }
                        }
                        if (!didPath) {
                            row.put(QUERY_COLUMN_PATH, jcrRow.getPath());
                        }
                        if (!didScore) {
                            row.put(QUERY_COLUMN_SCORE, jcrRow.getScore());
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
        } catch (final javax.jcr.query.InvalidQueryException iqe) {
            throw new QuerySyntaxException(iqe.getMessage(), query, language,
                iqe);
        } catch (final RepositoryException re) {
            throw new SlingException(re.getMessage(), re);
        }
    }

    private boolean isSupportedQueryLanguage(final String language) {
        try {
            String[] supportedLanguages = session.getWorkspace().
                getQueryManager().getSupportedQueryLanguages();
            for (String lang : supportedLanguages) {
                if (lang.equals(language)) {
                    return true;
                }
            }
        } catch (final RepositoryException e) {
            log.error("Unable to discover supported query languages", e);
        }
        return false;
    }

    /**
     * @see org.apache.sling.api.resource.AttributableResourceProvider#getAttributeNames(ResourceResolver)
     */
    public Collection<String> getAttributeNames(final ResourceResolver resolver) {
        this.checkClosed();

        final Set<String> names = new HashSet<String>();
        final String[] sessionNames = session.getAttributeNames();
        for(final String name : sessionNames) {
            if ( JcrResourceProviderFactory.isAttributeVisible(name) ) {
                names.add(name);
            }
        }
        return names;
    }

    /**
     * @see org.apache.sling.api.resource.AttributableResourceProvider#getAttribute(ResourceResolver, java.lang.String)
     */
    public Object getAttribute(final ResourceResolver resolver, final String name) {
        this.checkClosed();

        if ( JcrResourceProviderFactory.isAttributeVisible(name) ) {
            if ( ResourceResolverFactory.USER.equals(name) ) {
                return this.session.getUserID();
            }
            return session.getAttribute(name);
        }
        return null;
    }

    /**
     * @see org.apache.sling.api.adapter.SlingAdaptable#adaptTo(java.lang.Class)
     */
    @SuppressWarnings("unchecked")
    @Override
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        if (type == Session.class) {
            return (AdapterType) session;
        }
        return super.adaptTo(type);
    }

    /**
     * @see org.apache.sling.api.resource.ModifyingResourceProvider#create(ResourceResolver, java.lang.String, Map)
     */
    public Resource create(final ResourceResolver resolver, final String path, final Map<String, Object> properties)
    throws PersistenceException {
        // check for node type
        final Object nodeObj = (properties != null ? properties.get("jcr:primaryType") : null);
        final String nodeType = (nodeObj != null ? nodeObj.toString() : null);
        try {
            final int lastPos = path.lastIndexOf('/');
            final Node parent;
            if ( lastPos == 0 ) {
                parent = this.session.getRootNode();
            } else {
                parent = (Node)this.session.getItem(path.substring(0, lastPos));
            }
            final String name = path.substring(lastPos + 1);
            final Node node;
            if ( nodeType != null ) {
                node = parent.addNode(name, nodeType);
            } else {
                node = parent.addNode(name);
            }

            if ( properties != null ) {
                // create modifiable map
                final JcrModifiableValueMap jcrMap = new JcrModifiableValueMap(node, this.dynamicClassLoader);
                for(final Map.Entry<String, Object> entry : properties.entrySet()) {
                    if ( !"jcr:primaryType".equals(entry.getKey()) ) {
                        try {
                            jcrMap.put(entry.getKey(), entry.getValue());
                        } catch (final IllegalArgumentException iae) {
                            throw new PersistenceException(iae.getMessage(), iae, path, entry.getKey());
                        }
                    }
                }
            }

            return new JcrNodeResource(resolver, node, this.dynamicClassLoader);
        } catch (final RepositoryException e) {
            throw new PersistenceException("Unable to create node at " + path, e, path, null);
        }
    }

    /**
     * @see org.apache.sling.api.resource.ModifyingResourceProvider#delete(ResourceResolver, java.lang.String)
     */
    public void delete(final ResourceResolver resolver, final String path)
    throws PersistenceException {
        try {
            if ( session.itemExists(path) ) {
                session.getItem(path).remove();
            }
            throw new PersistenceException("Unable to delete item at " + path, null, path, null);
        } catch (final RepositoryException e) {
            throw new PersistenceException("Unable to delete item at " + path, e, path, null);
        }
    }

    /**
     * @see org.apache.sling.api.resource.ModifyingResourceProvider#revert(ResourceResolver)
     */
    public void revert(final ResourceResolver resolver) {
        try {
            this.session.refresh(false);
        } catch (final RepositoryException ignore) {
            log.warn("Unable to revert pending changes.", ignore);
        }
    }

    /**
     * @see org.apache.sling.api.resource.ModifyingResourceProvider#commit(ResourceResolver)
     */
    public void commit(final ResourceResolver resolver) throws PersistenceException {
        try {
            this.session.save();
        } catch (final RepositoryException e) {
            throw new PersistenceException("Unable to commit changes to session.", e);
        }
    }

    /**
     * @see org.apache.sling.api.resource.ModifyingResourceProvider#hasChanges(ResourceResolver)
     */
    public boolean hasChanges(final ResourceResolver resolver) {
        try {
            return this.session.hasPendingChanges();
        } catch (final RepositoryException ignore) {
            log.warn("Unable to check session for pending changes.", ignore);
        }
        return false;
    }
}
