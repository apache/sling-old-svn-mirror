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
package org.apache.sling.resourceresolver.impl;

import static org.apache.commons.lang3.StringUtils.defaultString;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.annotation.CheckForNull;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;

import org.apache.sling.adapter.annotations.Adaptable;
import org.apache.sling.adapter.annotations.Adapter;
import org.apache.sling.api.SlingException;
import org.apache.sling.api.adapter.SlingAdaptable;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ResourceWrapper;
import org.apache.sling.resourceresolver.impl.helper.RedirectResource;
import org.apache.sling.resourceresolver.impl.helper.ResourceIteratorDecorator;
import org.apache.sling.resourceresolver.impl.helper.ResourcePathIterator;
import org.apache.sling.resourceresolver.impl.helper.ResourceResolverContext;
import org.apache.sling.resourceresolver.impl.helper.ResourceResolverControl;
import org.apache.sling.resourceresolver.impl.helper.StarResource;
import org.apache.sling.resourceresolver.impl.helper.URI;
import org.apache.sling.resourceresolver.impl.helper.URIException;
import org.apache.sling.resourceresolver.impl.mapping.MapEntry;
import org.apache.sling.resourceresolver.impl.params.ParsedParameters;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderStorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Adaptable(adaptableClass = ResourceResolver.class, adapters = { @Adapter(Session.class) })
public class ResourceResolverImpl extends SlingAdaptable implements ResourceResolver {

    /** Default logger */
    private static final Logger logger = LoggerFactory.getLogger(ResourceResolverImpl.class);

    private static final Map<String, String> EMPTY_PARAMETERS = Collections.emptyMap();

    public static final String PROP_REDIRECT_INTERNAL = "sling:internalRedirect";

    public static final String PROP_ALIAS = "sling:alias";

    // The suffix of a resource being a content node of some parent
    // such as nt:file. The slash is included to prevent false
    // positives for the String.endsWith check for names like
    // "xyzjcr:content"
    private static final String JCR_CONTENT_LEAF = "/jcr:content";

    /** The factory which created this resource resolver. */
    private final CommonResourceResolverFactoryImpl factory;

    /** Resource resolver control. */
    private final ResourceResolverControl control;

    /** Resource resolver context. */
    private final ResourceResolverContext context;

    private volatile Exception closedResolverException;

    public ResourceResolverImpl(final CommonResourceResolverFactoryImpl factory, final boolean isAdmin, final Map<String, Object> authenticationInfo) throws LoginException {
        this(factory, isAdmin, authenticationInfo, factory.getResourceProviderTracker());
    }

    ResourceResolverImpl(final CommonResourceResolverFactoryImpl factory, final boolean isAdmin, final Map<String, Object> authenticationInfo, final ResourceProviderStorageProvider resourceProviderTracker) throws LoginException {
        this.factory = factory;
        this.context = new ResourceResolverContext(this, factory.getResourceAccessSecurityTracker());
        this.control = createControl(resourceProviderTracker, authenticationInfo, isAdmin);
        this.factory.register(this, control);
    }

    /**
     * Constructor for cloning the resource resolver
     * @param resolver The resolver to clone
     * @param authenticationInfo The auth info
     * @throws LoginException if auth to a required provider fails
     */
    private ResourceResolverImpl(final ResourceResolverImpl resolver, final Map<String, Object> authenticationInfo) throws LoginException {
        this.factory = resolver.factory;
        Map<String, Object> authInfo = new HashMap<>();
        if (resolver.control.getAuthenticationInfo() != null) {
            authInfo.putAll(resolver.control.getAuthenticationInfo());
        }
        if (authenticationInfo != null) {
            authInfo.putAll(authenticationInfo);
        }
        this.context = new ResourceResolverContext(this, factory.getResourceAccessSecurityTracker());
        this.control = createControl(factory.getResourceProviderTracker(), authInfo, resolver.control.isAdmin());
        this.factory.register(this, control);
    }

    /**
     * Create the resource resolver control
     * @param storage The provider storage
     * @param authenticationInfo Current auth info
     * @param isAdmin Is this admin?
     * @return A control
     * @throws LoginException If auth to the required providers fails.
     */
    private ResourceResolverControl createControl(final ResourceProviderStorageProvider resourceProviderTracker,
            final Map<String, Object> authenticationInfo,
            final boolean isAdmin)
    throws LoginException {
        final ResourceResolverControl control = new ResourceResolverControl(isAdmin, authenticationInfo, resourceProviderTracker);

        this.context.getProviderManager().authenticateAll(resourceProviderTracker.getResourceProviderStorage().getAuthRequiredHandlers(), control);

        return control;
    }

    /**
     * @see org.apache.sling.api.resource.ResourceResolver#clone(Map)
     */
    @Override
    public ResourceResolver clone(final Map<String, Object> authenticationInfo)
            throws LoginException {
        // ensure resolver is still live
        checkClosed();

        // create a regular resource resolver
        return new ResourceResolverImpl(this, authenticationInfo);
    }

    /**
     * @see org.apache.sling.api.resource.ResourceResolver#isLive()
     */
    @Override
    public boolean isLive() {
        return !this.control.isClosed() && this.control.isLive(this.context) && this.factory.isLive();
    }

    /**
     * @see org.apache.sling.api.resource.ResourceResolver#close()
     */
    @Override
    public void close() {
        if (factory.shouldLogResourceResolverClosing()) {
            closedResolverException = new Exception("Stack Trace");
        }
        this.factory.unregister(this, this.control);
    }

    /**
     * Check if the resource resolver is already closed or the factory which created this resolver is no longer live.
     *
     * @throws IllegalStateException
     *             If the resolver is already closed or the factory is no longer live.
     */
    private void checkClosed() {
        if (this.control.isClosed()) {
            if (closedResolverException != null) {
                logger.error("The ResourceResolver has already been closed.", closedResolverException);
            }
            throw new IllegalStateException("Resource resolver is already closed.");
        }
        if (!this.factory.isLive()) {
            throw new IllegalStateException("Resource resolver factory which created this resolver is no longer active.");
        }
    }

    // ---------- attributes

    /**
     * @see org.apache.sling.api.resource.ResourceResolver#getAttributeNames()
     */
    @Override
    public Iterator<String> getAttributeNames() {
        checkClosed();
        return this.control.getAttributeNames(this.context).iterator();
    }

    /**
     * @see org.apache.sling.api.resource.ResourceResolver#getAttribute(String)
     */
    @Override
    public Object getAttribute(final String name) {
        checkClosed();
        if (name == null) {
            throw new NullPointerException("name");
        }

        return this.control.getAttribute(this.context, name);
    }

    // ---------- resolving resources

    /**
     * @see org.apache.sling.api.resource.ResourceResolver#resolve(java.lang.String)
     */
    @Override
    public Resource resolve(final String path) {
        checkClosed();

        final Resource rsrc = this.resolveInternal(null, path);
        return rsrc;
    }

    /**
     * @see org.apache.sling.api.resource.ResourceResolver#resolve(javax.servlet.http.HttpServletRequest)
     */
    @Override
    public Resource resolve(final HttpServletRequest request) {
        checkClosed();

        // throws NPE if request is null as required
        final Resource rsrc = this.resolveInternal(request, request.getPathInfo());
        return rsrc;
    }

    /**
     * @see org.apache.sling.api.resource.ResourceResolver#resolve(javax.servlet.http.HttpServletRequest,
     *      java.lang.String)
     */
    @Override
    public Resource resolve(final HttpServletRequest request, String path) {
        checkClosed();

        final Resource rsrc = this.resolveInternal(request, path);
        return rsrc;
    }

    private Resource resolveInternal(final HttpServletRequest request, String absPath) {
        // make sure abspath is not null and is absolute
        if (absPath == null) {
            absPath = "/";
        } else if (!absPath.startsWith("/")) {
            absPath = "/" + absPath;
        }

        // check for special namespace prefix treatment
        absPath = unmangleNamespaces(absPath);

        // Assume http://localhost:80 if request is null
        String[] realPathList = { absPath };
        String requestPath;
        if (request != null) {
            requestPath = getMapPath(request.getScheme(), request.getServerName(), request.getServerPort(), absPath);
        } else {
            requestPath = getMapPath("http", "localhost", 80, absPath);
        }

        logger.debug("resolve: Resolving request path {}", requestPath);

        // loop while finding internal or external redirect into the
        // content out of the virtual host mapping tree
        // the counter is to ensure we are not caught in an endless loop here
        // TODO: might do better to be able to log the loop and help the user
        for (int i = 0; i < 100; i++) {

            String[] mappedPath = null;

            final Iterator<MapEntry> mapEntriesIterator = this.factory.getMapEntries().getResolveMapsIterator(requestPath);
            while (mapEntriesIterator.hasNext()) {
                final MapEntry mapEntry = mapEntriesIterator.next();
                mappedPath = mapEntry.replace(requestPath);
                if (mappedPath != null) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("resolve: MapEntry {} matches, mapped path is {}", mapEntry, Arrays.toString(mappedPath));
                    }
                    if (mapEntry.isInternal()) {
                        // internal redirect
                        logger.debug("resolve: Redirecting internally");
                        break;
                    }

                    // external redirect
                    logger.debug("resolve: Returning external redirect");
                    return this.factory.getResourceDecoratorTracker().decorate(
                            new RedirectResource(this, absPath, mappedPath[0], mapEntry.getStatus()));
                }
            }

            // if there is no virtual host based path mapping, abort
            // and use the original realPath
            if (mappedPath == null) {
                logger.debug("resolve: Request path {} does not match any MapEntry", requestPath);
                break;
            }

            // if the mapped path is not an URL, use this path to continue
            if (!mappedPath[0].contains("://")) {
                logger.debug("resolve: Mapped path is for resource tree");
                realPathList = mappedPath;
                break;
            }

            // otherwise the mapped path is an URI and we have to try to
            // resolve that URI now, using the URI's path as the real path
            try {
                final URI uri = new URI(mappedPath[0], false);
                requestPath = getMapPath(uri.getScheme(), uri.getHost(), uri.getPort(), uri.getPath());
                realPathList = new String[] { uri.getPath() };

                logger.debug("resolve: Mapped path is an URL, using new request path {}", requestPath);
            } catch (final URIException use) {
                // TODO: log and fail
                throw new ResourceNotFoundException(absPath);
            }
        }

        // now we have the real path resolved from virtual host mapping
        // this path may be absolute or relative, in which case we try
        // to resolve it against the search path

        Resource res = null;
        for (int i = 0; res == null && i < realPathList.length; i++) {
            final ParsedParameters parsedPath = new ParsedParameters(realPathList[i]);
            final String realPath = parsedPath.getRawPath();


            // first check whether the requested resource is a StarResource
            if (StarResource.appliesTo(realPath)) {
                logger.debug("resolve: Mapped path {} is a Star Resource", realPath);
                res = new StarResource(this, ensureAbsPath(realPath));

            } else {

                if (realPath.startsWith("/")) {

                    // let's check it with a direct access first
                    logger.debug("resolve: Try absolute mapped path {}", realPath);
                    res = resolveInternal(realPath, parsedPath.getParameters());

                } else {

                    final String[] searchPath = getSearchPath();
                    for (int spi = 0; res == null && spi < searchPath.length; spi++) {
                        logger.debug("resolve: Try relative mapped path with search path entry {}", searchPath[spi]);
                        res = resolveInternal(searchPath[spi] + realPath, parsedPath.getParameters());
                    }

                }
            }
        }

        // if no resource has been found, use a NonExistingResource
        if (res == null) {
            final ParsedParameters parsedPath = new ParsedParameters(realPathList[0]);
            final String resourcePath = ensureAbsPath(parsedPath.getRawPath());
            logger.debug("resolve: Path {} does not resolve, returning NonExistingResource at {}", absPath, resourcePath);

            res = new NonExistingResource(this, resourcePath);
            // SLING-864: if the path contains a dot we assume this to be
            // the start for any selectors, extension, suffix, which may be
            // used for further request processing.
            // the resolution path must be the full path and is already set within
            // the non existing resource
            final int index = resourcePath.indexOf('.');
            if (index != -1) {
                res.getResourceMetadata().setResolutionPathInfo(resourcePath.substring(index));
            }
            res.getResourceMetadata().setParameterMap(parsedPath.getParameters());
        } else {
            logger.debug("resolve: Path {} resolves to Resource {}", absPath, res);
        }

        return this.factory.getResourceDecoratorTracker().decorate(res);
    }

    /**
     * calls map(HttpServletRequest, String) as map(null, resourcePath)
     *
     * @see org.apache.sling.api.resource.ResourceResolver#map(java.lang.String)
     */
    @Override
    public String map(final String resourcePath) {
        checkClosed();
        return map(null, resourcePath);
    }

    /**
     * full implementation - apply sling:alias from the resource path - apply
     * /etc/map mappings (inkl. config backwards compat) - return absolute uri
     * if possible
     *
     * @see org.apache.sling.api.resource.ResourceResolver#map(javax.servlet.http.HttpServletRequest,
     *      java.lang.String)
     */
    @Override
    public String map(final HttpServletRequest request, final String resourcePath) {
        checkClosed();

        // find a fragment or query
        int fragmentQueryMark = resourcePath.indexOf('#');
        if (fragmentQueryMark < 0) {
            fragmentQueryMark = resourcePath.indexOf('?');
        }

        // cut fragment or query off the resource path
        String mappedPath;
        final String fragmentQuery;
        if (fragmentQueryMark >= 0) {
            fragmentQuery = resourcePath.substring(fragmentQueryMark);
            mappedPath = resourcePath.substring(0, fragmentQueryMark);
            logger.debug("map: Splitting resource path '{}' into '{}' and '{}'", new Object[] { resourcePath, mappedPath,
                    fragmentQuery });
        } else {
            fragmentQuery = null;
            mappedPath = resourcePath;
        }

        // cut off scheme and host, if the same as requested
        final String schemehostport;
        final String schemePrefix;
        if (request != null) {
            schemehostport = MapEntry.getURI(request.getScheme(), request.getServerName(), request.getServerPort(), "/");
            schemePrefix = request.getScheme().concat("://");
            logger.debug("map: Mapping path {} for {} (at least with scheme prefix {})", new Object[] { resourcePath,
                    schemehostport, schemePrefix });

        } else {

            schemehostport = null;
            schemePrefix = null;
            logger.debug("map: Mapping path {} for default", resourcePath);

        }

        ParsedParameters parsed = new ParsedParameters(mappedPath);
        final Resource res = resolveInternal(parsed.getRawPath(), parsed.getParameters());

        if (res != null) {

            // keep, what we might have cut off in internal resolution
            final String resolutionPathInfo = res.getResourceMetadata().getResolutionPathInfo();

            logger.debug("map: Path maps to resource {} with path info {}", res, resolutionPathInfo);

            // find aliases for segments. we can't walk the parent chain
            // since the request session might not have permissions to
            // read all parents SLING-2093
            final LinkedList<String> names = new LinkedList<>();

            Resource current = res;
            String path = res.getPath();
            while (path != null) {
                String alias = null;
                if (current != null && !path.endsWith(JCR_CONTENT_LEAF)) {
                    if (factory.isOptimizeAliasResolutionEnabled()) {
                        logger.debug("map: Optimize Alias Resolution is Enabled");
                        String parentPath = ResourceUtil.getParent(path);
                        if (parentPath != null) {
                            final Map<String, String> aliases = factory.getMapEntries().getAliasMap(parentPath);
                            if (aliases!= null && aliases.containsValue(current.getName())) {
                                for (String key:aliases.keySet()) {
                                    if (current.getName().equals(aliases.get(key))) {
                                        alias = key;
                                        break;
                                    }
                                }
                            }
                        }
                    } else {
                        logger.debug("map: Optimize Alias Resolution is Disabled");
                        alias = ResourceResolverControl.getProperty(current, PROP_ALIAS);
                    }
                }
                if (alias == null || alias.length() == 0) {
                    alias = ResourceUtil.getName(path);
                }
                names.add(alias);
                path = ResourceUtil.getParent(path);
                if ("/".equals(path)) {
                    path = null;
                } else if (path != null) {
                    current = res.getResourceResolver().resolve(path);
                }
            }

            // build path from segment names
            final StringBuilder buf = new StringBuilder();

            // construct the path from the segments (or root if none)
            if (names.isEmpty()) {
                buf.append('/');
            } else {
                while (!names.isEmpty()) {
                    buf.append('/');
                    buf.append(names.removeLast());
                }
            }

            // reappend the resolutionPathInfo
            if (resolutionPathInfo != null) {
                buf.append(resolutionPathInfo);
            }

            // and then we have the mapped path to work on
            mappedPath = buf.toString();

            logger.debug("map: Alias mapping resolves to path {}", mappedPath);

        }

        boolean mappedPathIsUrl = false;
        for (final MapEntry mapEntry : this.factory.getMapEntries().getMapMaps()) {
            final String[] mappedPaths = mapEntry.replace(mappedPath);
            if (mappedPaths != null) {

                logger.debug("map: Match for Entry {}", mapEntry);

                mappedPathIsUrl = !mapEntry.isInternal();

                if (mappedPathIsUrl && schemehostport != null) {

                    mappedPath = null;

                    for (final String candidate : mappedPaths) {
                        if (candidate.startsWith(schemehostport)) {
                            mappedPath = candidate.substring(schemehostport.length() - 1);
                            mappedPathIsUrl = false;
                            logger.debug("map: Found host specific mapping {} resolving to {}", candidate, mappedPath);
                            break;
                        } else if (candidate.startsWith(schemePrefix) && mappedPath == null) {
                            mappedPath = candidate;
                        }
                    }

                    if (mappedPath == null) {
                        mappedPath = mappedPaths[0];
                    }

                } else {

                    // we can only go with assumptions selecting the first entry
                    mappedPath = mappedPaths[0];

                }

                logger.debug("resolve: MapEntry {} matches, mapped path is {}", mapEntry, mappedPath);

                break;
            }
        }

        // this should not be the case, since mappedPath is primed
        if (mappedPath == null) {
            mappedPath = resourcePath;
        }

        // [scheme:][//authority][path][?query][#fragment]
        try {
            // use commons-httpclient's URI instead of java.net.URI, as it can
            // actually accept *unescaped* URIs, such as the "mappedPath" and
            // return them in proper escaped form, including the path, via
            // toString()
            final URI uri = new URI(mappedPath, false);

            // 1. mangle the namespaces in the path
            String path = mangleNamespaces(uri.getPath());

            // 2. prepend servlet context path if we have a request
            if (request != null && request.getContextPath() != null && request.getContextPath().length() > 0) {
                path = request.getContextPath().concat(path);
            }
            // update the path part of the URI
            uri.setPath(path);

            mappedPath = uri.toString();
        } catch (final URIException e) {
            logger.warn("map: Unable to mangle namespaces for " + mappedPath + " returning unmangled", e);
        }

        logger.debug("map: Returning URL {} as mapping for path {}", mappedPath, resourcePath);

        // reappend fragment and/or query
        if (fragmentQuery != null) {
            mappedPath = mappedPath.concat(fragmentQuery);
        }

        return mappedPath;
    }

    // ---------- search path for relative resoures

    /**
     * @see org.apache.sling.api.resource.ResourceResolver#getSearchPath()
     */
    @Override
    public String[] getSearchPath() {
        checkClosed();
        return factory.getSearchPath().clone();
    }

    // ---------- direct resource access without resolution

    /**
     * @see org.apache.sling.api.resource.ResourceResolver#getResource(java.lang.String)
     */
    @Override
    public Resource getResource(String path) {
        checkClosed();
        final Resource result = this.getResourceInternal(null, path);
        return result;
    }

    /**
     * @see org.apache.sling.api.resource.ResourceResolver#getResource(org.apache.sling.api.resource.Resource,
     *      java.lang.String)
     */
    @Override
    public Resource getResource(final Resource base, final String path) {
        checkClosed();

        String absolutePath = path;
        if (absolutePath != null && !absolutePath.startsWith("/") && base != null && base.getPath() != null ) {
            absolutePath = appendToPath(base.getPath(), absolutePath);
        }

        final Resource result = getResourceInternal(base, absolutePath);
        return result;
    }

    /**
     * Methods concatenates two paths. If the first path contains parameters separated semicolon, they are
     * moved at the end of the result.
     *
     * @param pathWithParameters
     * @param segmentToAppend
     * @return
     */
    private static String appendToPath(final String pathWithParameters, final String segmentToAppend) {
        final ParsedParameters parsed = new ParsedParameters(pathWithParameters);
        if (parsed.getParametersString() == null) {
            return new StringBuilder(parsed.getRawPath()).append('/').append(segmentToAppend).toString();
        } else {
            return new StringBuilder(parsed.getRawPath()).append('/').append(segmentToAppend).append(parsed.getParametersString()).toString();
        }
    }

    private Resource getResourceInternal(Resource parent, String path) {

        Resource result = null;
        if ( path != null ) {
            // if the path is absolute, normalize . and .. segments and get res
            if (path.startsWith("/")) {
                final ParsedParameters parsedPath = new ParsedParameters(path);
                path = ResourceUtil.normalize(parsedPath.getRawPath());
                result = (path != null) ? getAbsoluteResourceInternal(parent, path, parsedPath.getParameters(), false) : null;
                if (result != null) {
                    result = this.factory.getResourceDecoratorTracker().decorate(result);
                }
            } else {

                // otherwise we have to apply the search path
                // (don't use this.getSearchPath() to save a few cycle for not cloning)
                final String[] paths = factory.getSearchPath();
                if (paths != null) {
                    for (final String prefix : factory.getSearchPath()) {
                        result = getResource(prefix + path);
                        if (result != null) {
                            break;
                        }
                    }
                }
            }
        }

        return result;
    }

    /**
     * @see org.apache.sling.api.resource.ResourceResolver#listChildren(org.apache.sling.api.resource.Resource)
     */
    @Override
    public Iterator<Resource> listChildren(final Resource parent) {
        checkClosed();

        if (parent instanceof ResourceWrapper) {
            return listChildren(((ResourceWrapper) parent).getResource());
        }
        return new ResourceIteratorDecorator(this.factory.getResourceDecoratorTracker(), this.control.listChildren(this.context, parent));
    }

    /**
     * @see org.apache.sling.api.resource.Resource#getChildren()
     */
    @Override
    public Iterable<Resource> getChildren(final Resource parent) {
        return new Iterable<Resource>() {

            @Override
            public Iterator<Resource> iterator() {
                return listChildren(parent);
            }
        };
    }

    // ---------- Querying resources

    private static final String DEFAULT_QUERY_LANGUAGE = "xpath";

    /**
     * @see org.apache.sling.api.resource.ResourceResolver#findResources(java.lang.String,
     *      java.lang.String)
     */
    @Override
    public Iterator<Resource> findResources(final String query, final String language) throws SlingException {
        checkClosed();

        return new ResourceIteratorDecorator(this.factory.getResourceDecoratorTracker(),
                control.findResources(this.context, query, defaultString(language, DEFAULT_QUERY_LANGUAGE)));
    }

    /**
     * @see org.apache.sling.api.resource.ResourceResolver#queryResources(java.lang.String,
     *      java.lang.String)
     */
    @Override
    public Iterator<Map<String, Object>> queryResources(final String query, final String language)
            throws SlingException {
        checkClosed();

        return control.queryResources(this.context, query, defaultString(language, DEFAULT_QUERY_LANGUAGE));
    }

    /**
     * @see org.apache.sling.api.resource.ResourceResolver#getUserID()
     */
    @Override
    public String getUserID() {
        checkClosed();

        // Try auth info first
        if ( this.control.getAuthenticationInfo() != null ) {
            final Object impUser = this.control.getAuthenticationInfo().get(ResourceResolverFactory.USER_IMPERSONATION);
            if ( impUser != null ) {
                return impUser.toString();
            }
            final Object user = this.control.getAuthenticationInfo().get(ResourceResolverFactory.USER);
            if ( user != null ) {
                return user.toString();
            }
        }
        // Try attributes
        final Object impUser = this.getAttribute(ResourceResolverFactory.USER_IMPERSONATION);
        if ( impUser != null ) {
            return impUser.toString();
        }
        final Object user = this.getAttribute(ResourceResolverFactory.USER);
        if ( user != null ) {
            return user.toString();
        }

        return null;
    }

    /** Cached session object, fetched on demand. */
    private Object cachedSession;
    /** Flag indicating if a searching has already been searched. */
    private boolean searchedSession = false;

    /**
     * Try to get a session from one of the resource providers.
     */
    @SuppressWarnings("unchecked")
    private <AdapterType> AdapterType getSession(final Class<AdapterType> type) {
        if ( !this.searchedSession ) {
            this.searchedSession = true;
            this.cachedSession = this.control.adaptTo(this.context, type);
        }
        return (AdapterType) this.cachedSession;
    }

    // ---------- Adaptable interface

    /**
     * @see org.apache.sling.api.adapter.SlingAdaptable#adaptTo(java.lang.Class)
     */
    @Override
    public <AdapterType> AdapterType adaptTo(final Class<AdapterType> type) {
        checkClosed();

        if (type.getName().equals("javax.jcr.Session")) {
            return getSession(type);
        }
        final AdapterType result = this.control.adaptTo(this.context, type);
        if ( result != null ) {
            return result;
        }

        // fall back to default behaviour
        return super.adaptTo(type);
    }

    // ---------- internal

    /**
     * Returns a string used for matching map entries against the given request
     * or URI parts.
     *
     * @param scheme
     *            The URI scheme
     * @param host
     *            The host name
     * @param port
     *            The port number. If this is negative, the default value used
     *            is 80 unless the scheme is "https" in which case the default
     *            value is 443.
     * @param path
     *            The (absolute) path
     * @return The request path string {scheme}/{host}.{port}{path}.
     */
    private static String getMapPath(final String scheme, final String host, int port, final String path) {
        if (port < 0) {
            port = ("https".equals(scheme)) ? 443 : 80;
        }

        return scheme + "/" + host + "." + port + path;
    }

    /**
     * Internally resolves the absolute path. The will almost always contain
     * request selectors and an extension. Therefore this method uses the
     * {@link ResourcePathIterator} to cut off parts of the path to find the
     * actual resource.
     * <p>
     * This method operates in two steps:
     * <ol>
     * <li>Check the path directly
     * <li>Drill down the resource tree from the root down to the resource trying to get the child
     * as per the respective path segment or finding a child whose <code>sling:alias</code> property
     * is set to the respective name.
     * </ol>
     * <p>
     * If neither mechanism (direct access and drill down) resolves to a resource this method
     * returns <code>null</code>.
     *
     * @param absPath
     *            The absolute path of the resource to return.
     * @return The resource found or <code>null</code> if the resource could not
     *         be found. The
     *         {@link org.apache.sling.api.resource.ResourceMetadata#getResolutionPathInfo()
     *         resolution path info} field of the resource returned is set to
     *         the part of the <code>absPath</code> which has been cut off by
     *         the {@link ResourcePathIterator} to resolve the resource.
     */
    private Resource resolveInternal(final String absPath, final Map<String, String> parameters) {
        Resource resource = null;
        String curPath = absPath;
        try {
            final ResourcePathIterator it = new ResourcePathIterator(absPath);
            while (it.hasNext() && resource == null) {
                curPath = it.next();
                resource = getAbsoluteResourceInternal(null, curPath, parameters, true);
            }
        } catch (final Exception ex) {
            throw new SlingException("Problem trying " + curPath + " for request path " + absPath, ex);
        }

        // SLING-627: set the part cut off from the uriPath as
        // sling.resolutionPathInfo property such that
        // uriPath = curPath + sling.resolutionPathInfo
        if (resource != null) {

            final String rpi = absPath.substring(curPath.length());
            resource.getResourceMetadata().setResolutionPath(absPath.substring(0, curPath.length()));
            resource.getResourceMetadata().setResolutionPathInfo(rpi);
            resource.getResourceMetadata().setParameterMap(parameters);

            logger.debug("resolveInternal: Found resource {} with path info {} for {}", new Object[] { resource, rpi, absPath });

        } else {

            String tokenizedPath = absPath;

            // no direct resource found, so we have to drill down into the
            // resource tree to find a match
            resource = getAbsoluteResourceInternal(null, "/", parameters, true);

            //no read access on / drilling further down
            //SLING-5638
            if (resource == null) {
                resource = getAbsoluteResourceInternal(absPath, parameters, true);
                if (resource != null) {
                    tokenizedPath = tokenizedPath.substring(resource.getPath().length());
                }
            }

            final StringBuilder resolutionPath = new StringBuilder();
            final StringTokenizer tokener = new StringTokenizer(tokenizedPath, "/");
            while (resource != null && tokener.hasMoreTokens()) {
                final String childNameRaw = tokener.nextToken();

                Resource nextResource = getChildInternal(resource, childNameRaw);
                if (nextResource != null) {

                    resource = nextResource;
                    resolutionPath.append("/").append(childNameRaw);

                } else {

                    String childName = null;
                    final ResourcePathIterator rpi = new ResourcePathIterator(childNameRaw);
                    while (rpi.hasNext() && nextResource == null) {
                        childName = rpi.next();
                        nextResource = getChildInternal(resource, childName);
                    }

                    // switch the currentResource to the nextResource (may be
                    // null)
                    resource = nextResource;
                    resolutionPath.append("/").append(childName);

                    // terminate the search if a resource has been found
                    // with the extension cut off
                    if (nextResource != null) {
                        break;
                    }
                }
            }

            // SLING-627: set the part cut off from the uriPath as
            // sling.resolutionPathInfo property such that
            // uriPath = curPath + sling.resolutionPathInfo
            if (resource != null) {
                final String path = resolutionPath.toString();
                final String pathInfo = absPath.substring(path.length());

                resource.getResourceMetadata().setResolutionPath(path);
                resource.getResourceMetadata().setResolutionPathInfo(pathInfo);
                resource.getResourceMetadata().setParameterMap(parameters);

                logger.debug("resolveInternal: Found resource {} with path info {} for {}", new Object[] { resource, pathInfo,
                        absPath });
            }
        }

        return resource;
    }

    private Resource getChildInternal(final Resource parent, final String childName) {
        final String path;
        if ( childName.startsWith("/") ) {
            path = childName;
        } else {
            path = parent.getPath() + '/' + childName;
        }
        Resource child = getAbsoluteResourceInternal(parent, ResourceUtil.normalize(path), EMPTY_PARAMETERS, true );
        if (child != null) {
            final String alias = ResourceResolverControl.getProperty(child, PROP_REDIRECT_INTERNAL);
            if (alias != null) {
                // TODO: might be a redirect ??
                logger.warn("getChildInternal: Internal redirect to {} for Resource {} is not supported yet, ignoring", alias,
                        child);
            }

            // we have the resource name, continue with the next level
            return child;
        }

        // we do not have a child with the exact name, so we look for
        // a child, whose alias matches the childName
        if (factory.isOptimizeAliasResolutionEnabled()){
            logger.debug("getChildInternal: Optimize Alias Resolution is Enabled");
            //optimization made in SLING-2521
            final Map<String, String> aliases = factory.getMapEntries().getAliasMap(parent.getPath());
            if (aliases != null) {
                final String aliasName = aliases.get(childName);
                if (aliasName != null ) {
                    final String aliasPath;
                    if ( aliasName.startsWith("/") ) {
                        aliasPath = aliasName;
                    } else {
                        aliasPath = parent.getPath() + '/' + aliasName;
                    }
                    final Resource aliasedChild = getAbsoluteResourceInternal(parent, ResourceUtil.normalize(aliasPath), EMPTY_PARAMETERS, true );
                    logger.debug("getChildInternal: Found Resource {} with alias {} to use", aliasedChild, childName);
                    return aliasedChild;
                }
            }
        } else {
            logger.debug("getChildInternal: Optimize Alias Resolution is Disabled");
            final Iterator<Resource> children = listChildren(parent);
            while (children.hasNext()) {
                child = children.next();
                if (!child.getPath().endsWith(JCR_CONTENT_LEAF)) {
                    final String[] aliases = ResourceResolverControl.getProperty(child, PROP_ALIAS, String[].class);
                    if (aliases != null) {
                        for (final String alias : aliases) {
                            if (childName.equals(alias)) {
                                logger.debug("getChildInternal: Found Resource {} with alias {} to use", child, childName);
                                final Resource aliasedChild = getAbsoluteResourceInternal(parent, ResourceUtil.normalize(child.getPath()) , EMPTY_PARAMETERS, true);
                                return aliasedChild;
                            }
                        }
                    }
                }
            }
        }

        // no match for the childName found
        logger.debug("getChildInternal: Resource {} has no child {}", parent, childName);
        return null;
    }

    /**
     * Creates a resource with the given path if existing
     */
    private Resource getAbsoluteResourceInternal(@CheckForNull final Resource parent, @CheckForNull final String path, final Map<String, String> parameters, final boolean isResolve) {
        if (path == null || path.length() == 0 || path.charAt(0) != '/') {
            logger.debug("getResourceInternal: Path must be absolute {}", path);
            return null; // path must be absolute
        }

        final Resource parentToUse;
        if (parent != null && path.startsWith(parent.getPath())) {
            parentToUse = parent;
        } else {
            parentToUse = null;
        }

        final Resource resource = this.control.getResource(this.context, path, parentToUse, parameters, isResolve);
        if (resource != null) {
            resource.getResourceMetadata().setResolutionPath(path);
            resource.getResourceMetadata().setParameterMap(parameters);
            return resource;
        }

        logger.debug("getResourceInternal: Cannot resolve path '{}' to a resource", path);
        return null;
    }

    /**
     * Creates a resource, traversing bottom up, to the highest readable resource.
     *
     */
    private Resource getAbsoluteResourceInternal(String absPath, final Map<String, String> parameters, final boolean isResolved) {

        if (!absPath.contains("/") || "/".equals(absPath)) {
            return null;
        }

        absPath = absPath.substring(absPath.indexOf("/"));
        Resource resource = getAbsoluteResourceInternal(null, absPath, parameters, isResolved);

        absPath = absPath.substring(0, absPath.lastIndexOf("/"));

        while (!absPath.equals("")) {
            Resource r = getAbsoluteResourceInternal(null, absPath, parameters, true);

            if (r != null) {
                resource = r;
            }
            absPath = absPath.substring(0, absPath.lastIndexOf("/"));
        }
        return resource;
    }

    /**
     * Returns the <code>path</code> as an absolute path. If the path is already
     * absolute it is returned unmodified (the same instance actually). If the
     * path is relative it is made absolute by prepending the first entry of the
     * {@link #getSearchPath() search path}.
     *
     * @param path
     *            The path to ensure absolute
     * @return The absolute path as explained above
     */
    private String ensureAbsPath(String path) {
        if (!path.startsWith("/")) {
            path = getSearchPath()[0] + path;
        }
        return path;
    }

    private String mangleNamespaces(String absPath) {
        if ( absPath != null && factory.getNamespaceMangler() != null ) {
            absPath = ((JcrNamespaceMangler)factory.getNamespaceMangler()).mangleNamespaces(this, logger, absPath);
        }

        return absPath;
    }

    private String unmangleNamespaces(String absPath) {
        if (absPath != null && factory.getNamespaceMangler() != null ) {
            absPath = ((JcrNamespaceMangler)factory.getNamespaceMangler()).unmangleNamespaces(this, logger, absPath);
        }

        return absPath;
    }

    /**
     * @see org.apache.sling.api.resource.ResourceResolver#delete(org.apache.sling.api.resource.Resource)
     */
    @Override
    public void delete(final Resource resource)
            throws PersistenceException {
        // check if the resource is non existing - throws NPE if resource is null as stated in the API
        if ( ResourceUtil.isNonExistingResource(resource) ) {
            // nothing to do
            return;
        }
        // if resource is null, we get an NPE as stated in the API
        this.control.delete(this.context, resource);
    }

    /**
     * @see org.apache.sling.api.resource.ResourceResolver#create(org.apache.sling.api.resource.Resource, java.lang.String, Map)
     */
    @Override
    public Resource create(final Resource parent, final String name, final Map<String, Object> properties)
            throws PersistenceException {
        // if parent or name is null, we get an NPE as stated in the API
        if ( name == null ) {
            throw new NullPointerException("name");
        }
        // name should be a name not a path
        if ( name.indexOf("/") != -1 ) {
            throw new IllegalArgumentException("Name should not contain a slash: " + name);
        }
        final String path;
        if ( parent.getPath().equals("/") ) {
            path = parent.getPath() + name;
        } else {
            path = parent.getPath() + "/" + name;
        }
        // experimental code for handling synthetic resources
        if ( ResourceUtil.isSyntheticResource(parent) ) {
            Resource grandParent = parent.getParent();
            if (grandParent != null) {
                this.create(grandParent, parent.getName(), null);
            } else {
                throw new IllegalArgumentException("Can't create child on a synthetic root");
            }
        }
        final Resource rsrc = this.control.create(this.context, path, properties);
        rsrc.getResourceMetadata().setResolutionPath(rsrc.getPath());
        return this.factory.getResourceDecoratorTracker().decorate(rsrc);
    }

    /**
     * @see org.apache.sling.api.resource.ResourceResolver#revert()
     */
    @Override
    public void revert() {
        this.control.revert(this.context);
    }

    /**
     * @see org.apache.sling.api.resource.ResourceResolver#commit()
     */
    @Override
    public void commit() throws PersistenceException {
        this.control.commit(this.context);
    }

    /**
     * @see org.apache.sling.api.resource.ResourceResolver#hasChanges()
     */
    @Override
    public boolean hasChanges() {
        return this.control.hasChanges(this.context);
    }

    /**
     * @see org.apache.sling.api.resource.ResourceResolver#hasChildren()
     */
	@Override
    public boolean hasChildren(Resource resource) {
		return listChildren(resource).hasNext();
	}

    /**
     * @see org.apache.sling.api.resource.ResourceResolver#getParentResourceType(org.apache.sling.api.resource.Resource)
     */
    @Override
    public String getParentResourceType(final Resource resource) {
        String resourceSuperType = null;
        if ( resource != null ) {
            resourceSuperType = resource.getResourceSuperType();
            if (resourceSuperType == null) {
                resourceSuperType = this.getParentResourceType(resource.getResourceType());
            }
        }
        return resourceSuperType;
    }

    /**
     * @see org.apache.sling.api.resource.ResourceResolver#getParentResourceType(java.lang.String)
     */
    @Override
    public String getParentResourceType(final String resourceType) {
        return this.control.getParentResourceType(this.factory, this, resourceType);
    }

    /**
     * @see org.apache.sling.api.resource.ResourceResolver#isResourceType(org.apache.sling.api.resource.Resource, java.lang.String)
     */
    @Override
    public boolean isResourceType(final Resource resource, final String resourceType) {
        boolean result = false;
        if ( resource != null && resourceType != null ) {
             // Check if the resource is of the given type. This method first checks the
             // resource type of the resource, then its super resource type and continues
             //  to go up the resource super type hierarchy.
             if (ResourceTypeUtil.areResourceTypesEqual(resourceType, resource.getResourceType(), getSearchPath())) {
                 result = true;
             } else {
                 Set<String> superTypesChecked = new HashSet<>();
                 String superType = this.getParentResourceType(resource);
                 while (!result && superType != null) {
                     if (ResourceTypeUtil.areResourceTypesEqual(resourceType, superType, getSearchPath())) {
                         result = true;
                     } else {
                         superTypesChecked.add(superType);
                         superType = this.getParentResourceType(superType);
                         if (superType != null && superTypesChecked.contains(superType)) {
                             throw new SlingException("Cyclic dependency for resourceSuperType hierarchy detected on resource " + resource.getPath(), null);
                         }
                     }
                 }
             }

        }
        return result;
    }

    /**
     * @see org.apache.sling.api.resource.ResourceResolver#refresh()
     */
    @Override
    public void refresh() {
        this.control.refresh(this.context);
    }

    @Override
    public Resource getParent(final Resource child) {
        Resource rsrc = null;
        final String parentPath = ResourceUtil.getParent(child.getPath());
        if ( parentPath != null ) {
            // if the parent path is relative, resolve using search paths.
            if ( !parentPath.startsWith("/") ) {
                rsrc = context.getResourceResolver().getResource(parentPath);
            } else {
                rsrc = this.control.getParent(this.context, parentPath, child);
                if (rsrc != null ) {
                    rsrc.getResourceMetadata().setResolutionPath(rsrc.getPath());
                    rsrc = this.factory.getResourceDecoratorTracker().decorate(rsrc);
                }
            }
        }
        return rsrc;
    }

    @Override
    public Resource copy(final String srcAbsPath, final String destAbsPath) throws PersistenceException {
        Resource rsrc = this.control.copy(this.context, srcAbsPath, destAbsPath);
        if (rsrc != null ) {
            rsrc.getResourceMetadata().setResolutionPath(rsrc.getPath());
            rsrc = this.factory.getResourceDecoratorTracker().decorate(rsrc);
        }
        return rsrc;
    }

    @Override
    public Resource move(final String srcAbsPath, final String destAbsPath) throws PersistenceException {
        Resource rsrc = this.control.move(this.context, srcAbsPath, destAbsPath);
        if (rsrc != null ) {
            rsrc.getResourceMetadata().setResolutionPath(rsrc.getPath());
            rsrc = this.factory.getResourceDecoratorTracker().decorate(rsrc);
        }
        return rsrc;
    }
}
