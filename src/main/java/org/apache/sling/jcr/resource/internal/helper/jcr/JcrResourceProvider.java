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

import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
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
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingException;
import org.apache.sling.api.adapter.SlingAdaptable;
import org.apache.sling.api.resource.AttributableResourceProvider;
import org.apache.sling.api.resource.DynamicResourceProvider;
import org.apache.sling.api.resource.ModifyingResourceProvider;
import org.apache.sling.api.resource.ParametrizableResourceProvider;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.QueriableResourceProvider;
import org.apache.sling.api.resource.QuerySyntaxException;
import org.apache.sling.api.resource.RefreshableResourceProvider;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.apache.sling.jcr.resource.JcrResourceUtil;
import org.apache.sling.jcr.resource.internal.HelperData;
import org.apache.sling.jcr.resource.internal.JcrModifiableValueMap;
import org.apache.sling.jcr.resource.internal.NodeUtil;
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
               RefreshableResourceProvider,
               ModifyingResourceProvider,
               ParametrizableResourceProvider {

    /** column name for node path */
    private static final String QUERY_COLUMN_PATH = "jcr:path";

    /** column name for score value */
    private static final String QUERY_COLUMN_SCORE = "jcr:score";

    @SuppressWarnings("deprecation")
    private static final String DEFAULT_QUERY_LANGUAGE = Query.XPATH;

    private static final Set<String> IGNORED_PROPERTIES = new HashSet<String>();
    static {
        IGNORED_PROPERTIES.add(NodeUtil.MIXIN_TYPES);
        IGNORED_PROPERTIES.add(NodeUtil.NODE_TYPE);
        IGNORED_PROPERTIES.add("jcr:created");
        IGNORED_PROPERTIES.add("jcr:createdBy");
    }

    /** Default logger */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /** Flag for closing. */
    private boolean closed = false;

    private final Session session;
    private final HelperData helper;
    private final RepositoryHolder repositoryHolder;

    public JcrResourceProvider(final Session session,
                               final ClassLoader dynamicClassLoader,
                               final RepositoryHolder repositoryHolder,
                               final PathMapper pathMapper) {
        this.session = session;
        this.helper = new HelperData(dynamicClassLoader, pathMapper);
        this.repositoryHolder = repositoryHolder;
    }

    // ---------- ResourceProvider interface ----------------------------------

    /**
     * @see org.apache.sling.api.resource.ResourceProvider#getResource(org.apache.sling.api.resource.ResourceResolver, javax.servlet.http.HttpServletRequest, java.lang.String)
     */
    @SuppressWarnings("javadoc")
    public Resource getResource(ResourceResolver resourceResolver,
            HttpServletRequest request, String path) throws SlingException {
        return getResource(resourceResolver, path, Collections.<String, String> emptyMap());
    }

    /**
     * @see org.apache.sling.api.resource.ResourceProvider#getResource(org.apache.sling.api.resource.ResourceResolver, java.lang.String)
     */
    public Resource getResource(ResourceResolver resourceResolver, String path)
    throws SlingException {
        return getResource(resourceResolver, path, Collections.<String, String> emptyMap());
    }


    /**
     * @see org.apache.sling.api.resource.ResourceProvider#getResource(org.apache.sling.api.resource.ResourceResolver, java.lang.String)
     */
    public Resource getResource(ResourceResolver resourceResolver, String path, Map<String, String> parameters)
    throws SlingException {
        this.checkClosed();
        try {
            return createResource(resourceResolver, path, parameters);
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
                    parent.getResourceResolver(), parent.getPath(), Collections.<String, String> emptyMap());
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
     * @param resourcePath The absolute path
     * @return The <code>Resource</code> for the item at the given path.
     * @throws RepositoryException If an error occurrs accessingor checking the
     *             item in the repository.
     */
    private JcrItemResource createResource(final ResourceResolver resourceResolver,
            final String resourcePath, final Map<String, String> parameters) throws RepositoryException {
        final String jcrPath = helper.pathMapper.mapResourcePathToJCRPath(resourcePath);
        if (jcrPath != null && itemExists(jcrPath)) {
            Item item = session.getItem(jcrPath);
            final String version;
            if (parameters != null && parameters.containsKey("v")) {
                version = parameters.get("v");
                item = getHistoricItem(item, version);
            } else {
                version = null;
            }
            if (item.isNode()) {
                log.debug(
                    "createResource: Found JCR Node Resource at path '{}'",
                    resourcePath);
                final JcrNodeResource resource = new JcrNodeResource(resourceResolver, resourcePath, version, (Node) item, helper);
                resource.getResourceMetadata().setParameterMap(parameters);
                return resource;
            }

            log.debug(
                "createResource: Found JCR Property Resource at path '{}'",
                resourcePath);
            final JcrPropertyResource resource = new JcrPropertyResource(resourceResolver, resourcePath, version,
                (Property) item);
            resource.getResourceMetadata().setParameterMap(parameters);
            return resource;
        }

        log.debug("createResource: No JCR Item exists at path '{}'", jcrPath);
        return null;
    }

    private Item getHistoricItem(Item item, String versionSpecifier) throws RepositoryException {
        Item currentItem = item;
        LinkedList<String> relPath = new LinkedList<String>();
        Node version = null;
        while (!"/".equals(currentItem.getPath())) {
            if (isVersionable(currentItem)) {
                version = getFrozenNode((Node) currentItem, versionSpecifier);
                break;
            } else {
                relPath.addFirst(currentItem.getName());
                currentItem = currentItem.getParent();
            }
        }
        if (version != null) {
            return getSubitem(version, StringUtils.join(relPath.iterator(), '/'));
        }
        return null;
    }

    private static Item getSubitem(Node node, String relPath) throws RepositoryException {
        if (relPath.length() == 0) { // not using isEmpty() due to 1.5 compatibility
            return node;
        } else if (node.hasNode(relPath)) {
            return node.getNode(relPath);
        } else if (node.hasProperty(relPath)) {
            return node.getProperty(relPath);
        } else {
            return null;
        }
    }

    private Node getFrozenNode(Node node, String versionSpecifier) throws RepositoryException {
        final VersionManager versionManager = session.getWorkspace().getVersionManager();
        final VersionHistory history = versionManager.getVersionHistory(node.getPath());
        if (history.hasVersionLabel(versionSpecifier)) {
            return history.getVersionByLabel(versionSpecifier).getFrozenNode();
        } else if (history.hasNode(versionSpecifier)) {
            return history.getVersion(versionSpecifier).getFrozenNode();
        } else {
            return null;
        }
    }

    private static boolean isVersionable(Item item) throws RepositoryException {
        return item.isNode() && ((Node) item).isNodeType(JcrConstants.MIX_VERSIONABLE);
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
        this.repositoryHolder.release();
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
            return new JcrNodeResourceIterator(resolver, res.getNodes(), helper);
        } catch (final javax.jcr.query.InvalidQueryException iqe) {
            throw new QuerySyntaxException(iqe.getMessage(), query, language, iqe);
        } catch (final RepositoryException re) {
            throw new SlingException(re.getMessage(), re);
        }
    }

    /**
     * @see org.apache.sling.api.resource.QueriableResourceProvider#queryResources(ResourceResolver, java.lang.String, java.lang.String)
     */
    public Iterator<ValueMap> queryResources(final ResourceResolver resolver, final String query, final String language) {
        checkClosed();

        final String queryLanguage = isSupportedQueryLanguage(language) ? language : DEFAULT_QUERY_LANGUAGE;

        try {
            final QueryResult result = JcrResourceUtil.query(session, query, queryLanguage);
            final String[] colNames = result.getColumnNames();
            final RowIterator rows = result.getRows();

            return new Iterator<ValueMap>() {

                private ValueMap next;

                {
                    next = seek();
                }

                public boolean hasNext() {
                    return next != null;
                };

                public ValueMap next() {
                    if ( next == null ) {
                        throw new NoSuchElementException();
                    }
                    final ValueMap result = next;
                    next = seek();
                    return result;
                }

                private ValueMap seek() {
                    ValueMap result = null;
                    while ( result == null && rows.hasNext() ) {
                        try {
                            final Row jcrRow = rows.nextRow();
                            final String resourcePath = helper.pathMapper.mapJCRPathToResourcePath(jcrRow.getPath());
                            if ( resourcePath != null ) {
                                final Map<String, Object> row = new HashMap<String, Object>();

                                boolean didPath = false;
                                boolean didScore = false;
                                final Value[] values = jcrRow.getValues();
                                for (int i = 0; i < values.length; i++) {
                                    Value v = values[i];
                                    if (v != null) {
                                        String colName = colNames[i];
                                        row.put(colName,
                                            JcrResourceUtil.toJavaObject(values[i]));
                                        if (colName.equals(QUERY_COLUMN_PATH)) {
                                            didPath = true;
                                            row.put(colName,
                                                    helper.pathMapper.mapJCRPathToResourcePath(JcrResourceUtil.toJavaObject(values[i]).toString()));
                                        }
                                        if (colName.equals(QUERY_COLUMN_SCORE)) {
                                            didScore = true;
                                        }
                                    }
                                }
                                if (!didPath) {
                                    row.put(QUERY_COLUMN_PATH, helper.pathMapper.mapJCRPathToResourcePath(jcrRow.getPath()));
                                }
                                if (!didScore) {
                                    row.put(QUERY_COLUMN_SCORE, jcrRow.getScore());
                                }
                                result = new ValueMapDecorator(row);
                            }
                        } catch (final RepositoryException re) {
                            log.error(
                                "queryResources$next: Problem accessing row values",
                                re);
                        }
                    }
                    return result;
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
        } else if (type == Principal.class) {
            try {
                if (this.session instanceof JackrabbitSession && session.getUserID() != null) {
                    JackrabbitSession s =((JackrabbitSession) this.session);
                    final UserManager um = s.getUserManager();
                    if (um != null) {
                        final Authorizable auth = um.getAuthorizable(s.getUserID());
                        if (auth != null) {
                            return (AdapterType) auth.getPrincipal();
                        }
                    }
                }
                log.debug("not able to adapto Resource to Principal, let the base class try to adapt");
            } catch (RepositoryException e) {
                log.warn("error while adapting Resource to Principal, let the base class try to adapt", e);
            }
        }
        return super.adaptTo(type);
    }

    /**
     * @see org.apache.sling.api.resource.ModifyingResourceProvider#create(ResourceResolver, java.lang.String, Map)
     */
    public Resource create(final ResourceResolver resolver, final String resourcePath, final Map<String, Object> properties)
    throws PersistenceException {
        // check for node type
        final Object nodeObj = (properties != null ? properties.get(NodeUtil.NODE_TYPE) : null);
        // check for sling:resourcetype
        final String nodeType;
        if ( nodeObj != null ) {
            nodeType = nodeObj.toString();
        } else {
            final Object rtObj =  (properties != null ? properties.get(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY) : null);
            boolean isNodeType = false;
            if ( rtObj != null ) {
                final String resourceType = rtObj.toString();
                if ( resourceType.indexOf(':') != -1 && resourceType.indexOf('/') == -1 ) {
                    try {
                        this.session.getWorkspace().getNodeTypeManager().getNodeType(resourceType);
                        isNodeType = true;
                    } catch (final RepositoryException ignore) {
                        // we expect this, if this isn't a valid node type, therefore ignoring
                    }
                }
            }
            if ( isNodeType ) {
                nodeType = rtObj.toString();
            } else {
                nodeType = null;
            }
        }
        final String jcrPath = helper.pathMapper.mapResourcePathToJCRPath(resourcePath);
        if ( jcrPath == null ) {
            throw new PersistenceException("Unable to create node at " + resourcePath, null, resourcePath, null);
        }
        Node node = null;
        try {
            final int lastPos = jcrPath.lastIndexOf('/');
            final Node parent;
            if ( lastPos == 0 ) {
                parent = this.session.getRootNode();
            } else {
                parent = (Node)this.session.getItem(jcrPath.substring(0, lastPos));
            }
            final String name = jcrPath.substring(lastPos + 1);
            if ( nodeType != null ) {
                node = parent.addNode(name, nodeType);
            } else {
                node = parent.addNode(name);
            }

            if ( properties != null ) {
                // create modifiable map
                final JcrModifiableValueMap jcrMap = new JcrModifiableValueMap(node, this.helper);
                // check mixin types first
                final Object value = properties.get(NodeUtil.MIXIN_TYPES);
                if ( value != null ) {
                    jcrMap.put(NodeUtil.MIXIN_TYPES, value);
                }
                for(final Map.Entry<String, Object> entry : properties.entrySet()) {
                    if ( !IGNORED_PROPERTIES.contains(entry.getKey()) ) {
                        try {
                            jcrMap.put(entry.getKey(), entry.getValue());
                        } catch (final IllegalArgumentException iae) {
                            try {
                                node.remove();
                            } catch ( final RepositoryException re) {
                                // we ignore this
                            }
                            throw new PersistenceException(iae.getMessage(), iae, resourcePath, entry.getKey());
                        }
                    }
                }
            }

            return new JcrNodeResource(resolver, resourcePath, null, node, this.helper);
        } catch (final RepositoryException e) {
            throw new PersistenceException("Unable to create node at " + jcrPath, e, resourcePath, null);
        }
    }

    /**
     * @see org.apache.sling.api.resource.ModifyingResourceProvider#delete(ResourceResolver, java.lang.String)
     */
    public void delete(final ResourceResolver resolver, final String resourcePath)
    throws PersistenceException {
        final String jcrPath = helper.pathMapper.mapResourcePathToJCRPath(resourcePath);
        if ( jcrPath == null ) {
            throw new PersistenceException("Unable to delete resource at " + resourcePath, null, resourcePath, null);
        }
        try {
            if ( session.itemExists(jcrPath) ) {
                session.getItem(jcrPath).remove();
            } else {
                throw new PersistenceException("Unable to delete resource at " + jcrPath + ". Resource does not exist.", null, resourcePath, null);
            }
        } catch (final RepositoryException e) {
            throw new PersistenceException("Unable to delete resource at " + jcrPath, e, resourcePath, null);
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

    /**
     * @see org.apache.sling.api.resource.RefreshableResourceProvider#refresh()
     */
    public void refresh() {
        try {
            this.session.refresh(true);
        } catch (final RepositoryException ignore) {
            log.warn("Unable to refresh session.", ignore);
        }
    }
}
