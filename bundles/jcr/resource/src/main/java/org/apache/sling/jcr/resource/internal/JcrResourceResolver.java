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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.Credentials;
import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import javax.servlet.http.HttpServletRequest;

import org.apache.jackrabbit.util.Text;
import org.apache.sling.adapter.SlingAdaptable;
import org.apache.sling.api.SlingException;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.QuerySyntaxException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.apache.sling.jcr.resource.JcrResourceUtil;
import org.apache.sling.jcr.resource.internal.helper.MapEntry;
import org.apache.sling.jcr.resource.internal.helper.RedirectResource;
import org.apache.sling.jcr.resource.internal.helper.ResourceIterator;
import org.apache.sling.jcr.resource.internal.helper.ResourcePathIterator;
import org.apache.sling.jcr.resource.internal.helper.URI;
import org.apache.sling.jcr.resource.internal.helper.URIException;
import org.apache.sling.jcr.resource.internal.helper.jcr.JcrNodeResourceIterator;
import org.apache.sling.jcr.resource.internal.helper.jcr.JcrResourceProviderEntry;
import org.apache.sling.jcr.resource.internal.helper.starresource.StarResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrResourceResolver
    extends SlingAdaptable implements ResourceResolver {

    /** default logger */
    private final Logger LOGGER = LoggerFactory.getLogger(JcrResourceResolver.class);

    private static final String MANGLE_NAMESPACE_IN_SUFFIX = "_";

    private static final String MANGLE_NAMESPACE_IN_PREFIX = "/_";

    private static final String MANGLE_NAMESPACE_IN = "/_([^_/]+)_";

    private static final String MANGLE_NAMESPACE_OUT_SUFFIX = ":";

    private static final String MANGLE_NAMESPACE_OUT_PREFIX = "/";

    private static final String MANGLE_NAMESPACE_OUT = "/([^:/]+):";

    public static final String PROP_REG_EXP = "sling:match";

    public static final String PROP_REDIRECT_INTERNAL = "sling:internalRedirect";

    public static final String PROP_ALIAS = "sling:alias";

    public static final String PROP_REDIRECT_EXTERNAL = "sling:redirect";

    public static final String PROP_REDIRECT_EXTERNAL_STATUS = "sling:status";

    @SuppressWarnings("deprecation")
    private static final String DEFAULT_QUERY_LANGUAGE = Query.XPATH;

    /** column name for node path */
    private static final String QUERY_COLUMN_PATH = "jcr:path";

    /** column name for score value */
    private static final String QUERY_COLUMN_SCORE = "jcr:score";

    /** The root provider for the resource tree. */
    private final JcrResourceProviderEntry rootProvider;

    /** The factory which created this resource resolver. */
    private final JcrResourceResolverFactoryImpl factory;

    /** Is this a resource resolver for an admin? */
    private final boolean isAdmin;

    /** The original authentication information - this is used for further resource resolver creations. */
    private final Map<String, Object> originalAuthInfo;

    /** Resolvers for different workspaces. */
    private Map<String, JcrResourceResolver> createdResolvers;

    /** Closed marker. */
    private volatile boolean closed = false;

    /** a resolver with the workspace which was specifically requested via a request attribute. */
    private ResourceResolver requestBoundResolver;

    private final boolean useMultiWorkspaces;

    public JcrResourceResolver(final JcrResourceProviderEntry rootProvider,
                               final JcrResourceResolverFactoryImpl factory,
                               final boolean isAdmin,
                               final Map<String, Object> originalAuthInfo,
                               boolean useMultiWorkspaces) {
        this.rootProvider = rootProvider;
        this.factory = factory;
        this.isAdmin = isAdmin;
        this.originalAuthInfo = originalAuthInfo;
        this.useMultiWorkspaces = useMultiWorkspaces;
    }

    /**
     * @see org.apache.sling.api.resource.ResourceResolver#clone(Map)
     */
    public ResourceResolver clone(Map<String, Object> authenticationInfo)
            throws LoginException {

        // ensure resolver is still live
        checkClosed();

        // create the merged map
        Map<String, Object> newAuthenticationInfo = new HashMap<String, Object>();
        if (originalAuthInfo != null) {
            newAuthenticationInfo.putAll(originalAuthInfo);
        }
        if (authenticationInfo != null) {
            newAuthenticationInfo.putAll(authenticationInfo);
        }

        // get an administrative resolver if this resolver isAdmin unless
        // credentials and/or user name are present in the credentials and/or
        // a session is present
        if (isAdmin
            && !(newAuthenticationInfo.get(JcrResourceConstants.AUTHENTICATION_INFO_CREDENTIALS) instanceof Credentials)
            && !(newAuthenticationInfo.get(JcrResourceConstants.AUTHENTICATION_INFO_SESSION) instanceof Session)
            && !(newAuthenticationInfo.get(ResourceResolverFactory.USER) instanceof String)) {
            return factory.getAdministrativeResourceResolver(newAuthenticationInfo);
        }

        // create a regular resource resolver
        return factory.getResourceResolver(newAuthenticationInfo);
    }

    /**
     * @see org.apache.sling.api.resource.ResourceResolver#isLive()
     */
    public boolean isLive() {
        return !this.closed && getSession().isLive();
    }

    /**
     * @see org.apache.sling.api.resource.ResourceResolver#close()
     */
    public void close() {
        if (!this.closed) {
            this.closed = true;
            closeCreatedResolvers();
            closeSession();
        }
    }

    /**
     * Closes the session underlying this resource resolver. This method is
     * called by the {@link #close()} method.
     * <p>
     * Extensions can overwrite this method to do other work (or not close the
     * session at all). Handle with care !
     */
    protected void closeSession() {
        try {
            getSession().logout();
        } catch (Throwable t) {
            LOGGER.debug(
                "closeSession: Unexpected problem closing the session; ignoring",
                t);
        }
    }

    /**
     * Closes any helper resource resolver created while this resource resolver
     * was used.
     * <p>
     * Extensions can overwrite this method to do other work (or not close the
     * created resource resovlers at all). Handle with care !
     */
    protected void closeCreatedResolvers() {
        if (this.createdResolvers != null) {
            for (final ResourceResolver resolver : createdResolvers.values()) {
                try {
                    resolver.close();
                } catch (Throwable t) {
                    LOGGER.debug(
                        "closeCreatedResolvers: Unexpected problem closing the created resovler "
                            + resolver + "; ignoring", t);
                }
            }
        }
    }

    /**
     * Calls the {@link #close()} method to ensure the resolver is properly
     * cleaned up before it is being collected by the garbage collector because
     * it is not referred to any more.
     */
    protected void finalize() {
        close();
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

    // ---------- attributes

    /**
     * @see org.apache.sling.api.resource.ResourceResolver#getAttributeNames()
     */
    public Iterator<String> getAttributeNames() {
        checkClosed();
        final Set<String> names = new HashSet<String>();
        names.addAll(Arrays.asList(getSession().getAttributeNames()));
        if (originalAuthInfo != null) {
            names.addAll(originalAuthInfo.keySet());
        }
        return new Iterator<String>() {
            final Iterator<String> keys = names.iterator();

            String nextKey = seek();

            private String seek() {
                while (keys.hasNext()) {
                    final String key = keys.next();
                    if (JcrResourceResolverFactoryImpl.isAttributeVisible(key)) {
                        return key;
                    }
                }
                return null;
            }

            public boolean hasNext() {
                return nextKey != null;
            }

            public String next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                String toReturn = nextKey;
                nextKey = seek();
                return toReturn;
            }

            public void remove() {
                throw new UnsupportedOperationException("remove");
            }
        };
    }

    /**
     * @see org.apache.sling.api.resource.ResourceResolver#getAttribute(String)
     */
    public Object getAttribute(String name) {
        if (name == null) {
            throw new NullPointerException("name");
        }

        if (JcrResourceResolverFactoryImpl.isAttributeVisible(name)) {
            final Object sessionAttr = getSession().getAttribute(name);
            if (sessionAttr != null) {
                return sessionAttr;
            }
            if (originalAuthInfo != null) {
                return originalAuthInfo.get(name);
            }
        }

        // not a visible attribute
        return null;
    }

    // ---------- resolving resources

    /**
     * @see org.apache.sling.api.resource.ResourceResolver#resolve(java.lang.String)
     */
    public Resource resolve(String absPath) {
        checkClosed();
        return resolve(null, absPath);
    }

    /**
     * @see org.apache.sling.api.resource.ResourceResolver#resolve(javax.servlet.http.HttpServletRequest)
     */
    public Resource resolve(HttpServletRequest request) {
        checkClosed();
        // throws NPE if request is null as required
        return resolve(request, request.getPathInfo());
    }

    /**
     * @see org.apache.sling.api.resource.ResourceResolver#resolve(javax.servlet.http.HttpServletRequest, java.lang.String)
     */
    public Resource resolve(HttpServletRequest request, String absPath) {
        checkClosed();

        String workspaceName = null;

        // make sure abspath is not null and is absolute
        if (absPath == null) {
            absPath = "/";
        } else if (!absPath.startsWith("/")) {
            if (useMultiWorkspaces) {
                final int wsSepPos = absPath.indexOf(":/");
                if (wsSepPos != -1) {
                    workspaceName = absPath.substring(0, wsSepPos);
                    absPath = absPath.substring(wsSepPos + 1);
                } else {
                    absPath = "/" + absPath;
                }
            } else {
                absPath = "/" + absPath;
            }
        }

        // check for special namespace prefix treatment
        absPath = unmangleNamespaces(absPath);
        if (useMultiWorkspaces) {
            if (workspaceName == null) {
                // check for workspace info from request
                workspaceName = (request == null ? null :
                    (String)request.getAttribute(ResourceResolver.REQUEST_ATTR_WORKSPACE_INFO));
            }
            if (workspaceName != null && !workspaceName.equals(getSession().getWorkspace().getName())) {
                LOGGER.debug("Delegating resolving to resolver for workspace {}", workspaceName);
                try {
                    final ResourceResolver wsResolver = getResolverForWorkspace(workspaceName);
                    requestBoundResolver = wsResolver;
                    return wsResolver.resolve(request, absPath);
                } catch (LoginException e) {
                    // requested a resource in a workspace I don't have access to.
                    // we treat this as a not found resource
                    LOGGER.debug(
                        "resolve: Path {} does not resolve, returning NonExistingResource",
                           absPath);

                    final Resource res = new NonExistingResource(this, absPath);
                    // SLING-864: if the path contains a dot we assume this to be
                    // the start for any selectors, extension, suffix, which may be
                    // used for further request processing.
                    int index = absPath.indexOf('.');
                    if (index != -1) {
                        res.getResourceMetadata().setResolutionPathInfo(absPath.substring(index));
                    }
                    return this.factory.getResourceDecoratorTracker().decorate(res, workspaceName, request);
                }

            }
        }
        // Assume http://localhost:80 if request is null
        String[] realPathList = { absPath };
        String requestPath;
        if (request != null) {
            requestPath = getMapPath(request.getScheme(),
                request.getServerName(), request.getServerPort(), absPath);
        } else {
            requestPath = getMapPath("http", "localhost", 80, absPath);
        }

        LOGGER.debug("resolve: Resolving request path {}", requestPath);

        // loop while finding internal or external redirect into the
        // content out of the virtual host mapping tree
        // the counter is to ensure we are not caught in an endless loop here
        // TODO: might do better to be able to log the loop and help the user
        for (int i = 0; i < 100; i++) {

            String[] mappedPath = null;
            for (MapEntry mapEntry : this.factory.getMapEntries().getResolveMaps()) {
                mappedPath = mapEntry.replace(requestPath);
                if (mappedPath != null) {
                    if ( LOGGER.isDebugEnabled() ) {
                        LOGGER.debug(
                            "resolve: MapEntry {} matches, mapped path is {}",
                            mapEntry, Arrays.toString(mappedPath));
                    }
                    if (mapEntry.isInternal()) {
                        // internal redirect
                        LOGGER.debug("resolve: Redirecting internally");
                        break;
                    }

                    // external redirect
                    LOGGER.debug("resolve: Returning external redirect");
                    return this.factory.getResourceDecoratorTracker().decorate(
                            new RedirectResource(this, absPath, mappedPath[0],
                                   mapEntry.getStatus()), workspaceName,
                             request);
                }
            }

            // if there is no virtual host based path mapping, abort
            // and use the original realPath
            if (mappedPath == null) {
                LOGGER.debug(
                    "resolve: Request path {} does not match any MapEntry",
                    requestPath);
                break;
            }

            // if the mapped path is not an URL, use this path to continue
            if (!mappedPath[0].contains("://")) {
                LOGGER.debug("resolve: Mapped path is for resource tree");
                realPathList = mappedPath;
                break;
            }

            // otherwise the mapped path is an URI and we have to try to
            // resolve that URI now, using the URI's path as the real path
            try {
                URI uri = new URI(mappedPath[0], false);
                requestPath = getMapPath(uri.getScheme(), uri.getHost(),
                    uri.getPort(), uri.getPath());
                realPathList = new String[] { uri.getPath() };

                LOGGER.debug(
                    "resolve: Mapped path is an URL, using new request path {}",
                    requestPath);
            } catch (URIException use) {
                // TODO: log and fail
                throw new ResourceNotFoundException(absPath);
            }
        }

        // now we have the real path resolved from virtual host mapping
        // this path may be absolute or relative, in which case we try
        // to resolve it against the search path

        Resource res = null;
        for (int i = 0; res == null && i < realPathList.length; i++) {
            String realPath = realPathList[i];

            // first check whether the requested resource is a StarResource
            if (StarResource.appliesTo(realPath)) {
                LOGGER.debug("resolve: Mapped path {} is a Star Resource",
                    realPath);
                res = new StarResource(this, ensureAbsPath(realPath));

            } else {

                if (realPath.startsWith("/")) {

                    // let's check it with a direct access first
                    LOGGER.debug("resolve: Try absolute mapped path {}", realPath);
                    res = resolveInternal(realPath);

                } else {

                    String[] searchPath = getSearchPath();
                    for (int spi = 0; res == null && spi < searchPath.length; spi++) {
                        LOGGER.debug(
                            "resolve: Try relative mapped path with search path entry {}",
                            searchPath[spi]);
                        res = resolveInternal(searchPath[spi] + realPath);
                    }

                }
            }

        }

        // if no resource has been found, use a NonExistingResource
        if (res == null) {
            String resourcePath = ensureAbsPath(realPathList[0]);
            LOGGER.debug(
                "resolve: Path {} does not resolve, returning NonExistingResource at {}",
                   absPath, resourcePath);

            res = new NonExistingResource(this, resourcePath);
            // SLING-864: if the path contains a dot we assume this to be
            // the start for any selectors, extension, suffix, which may be
            // used for further request processing.
            int index = resourcePath.indexOf('.');
            if (index != -1) {
                res.getResourceMetadata().setResolutionPathInfo(resourcePath.substring(index));
            }
        } else {
            LOGGER.debug("resolve: Path {} resolves to Resource {}", absPath, res);
        }

        return this.factory.getResourceDecoratorTracker().decorate(res, workspaceName, request);
    }

    /**
     * calls map(HttpServletRequest, String) as map(null, resourcePath)
     * @see org.apache.sling.api.resource.ResourceResolver#map(java.lang.String)
     */
    public String map(String resourcePath) {
        checkClosed();
        return map(null, resourcePath);
    }

    /**
     * full implementation
     *   - apply sling:alias from the resource path
     *   - apply /etc/map mappings (inkl. config backwards compat)
     *   - return absolute uri if possible
     * @see org.apache.sling.api.resource.ResourceResolver#map(javax.servlet.http.HttpServletRequest, java.lang.String)
     */
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
            LOGGER.debug("map: Splitting resource path '{}' into '{}' and '{}'",
                new Object[] { resourcePath, mappedPath, fragmentQuery });
        } else {
            fragmentQuery = null;
            mappedPath = resourcePath;
        }


        // cut off scheme and host, if the same as requested
        final String schemehostport;
        final String schemePrefix;
        if (request != null) {
            schemehostport = MapEntry.getURI(request.getScheme(),
                request.getServerName(), request.getServerPort(), "/");
            schemePrefix = request.getScheme().concat("://");
            LOGGER.debug(
                "map: Mapping path {} for {} (at least with scheme prefix {})",
                new Object[] { resourcePath, schemehostport, schemePrefix });

        } else {

            schemehostport = null;
            schemePrefix = null;
            LOGGER.debug("map: Mapping path {} for default", resourcePath);

        }

        Resource res = null;
        String workspaceName = null;

        if (useMultiWorkspaces) {
            final int wsSepPos = mappedPath.indexOf(":/");
            if (wsSepPos != -1) {
                workspaceName = mappedPath.substring(0, wsSepPos);
                if (workspaceName.equals(getSession().getWorkspace().getName())) {
                    mappedPath = mappedPath.substring(wsSepPos + 1);
                } else {
                    try {
                        JcrResourceResolver wsResolver = getResolverForWorkspace(workspaceName);
                        mappedPath = mappedPath.substring(wsSepPos + 1);
                        res = wsResolver.resolveInternal(mappedPath);
                    } catch (LoginException e) {
                        // requested a resource in a workspace I don't have access to.
                        // we treat this as a not found resource
                        return null;
                    }
                }
            } else {
                // check for workspace info in request
                workspaceName = (request == null ? null :
                    (String)request.getAttribute(ResourceResolver.REQUEST_ATTR_WORKSPACE_INFO));
                if ( workspaceName != null && !workspaceName.equals(getSession().getWorkspace().getName())) {
                    LOGGER.debug("Delegating resolving to resolver for workspace {}", workspaceName);
                    try {
                        JcrResourceResolver wsResolver = getResolverForWorkspace(workspaceName);
                        res = wsResolver.resolveInternal(mappedPath);
                    } catch (LoginException e) {
                        // requested a resource in a workspace I don't have access to.
                        // we treat this as a not found resource
                        return null;
                    }

                }
            }
        }

        if (res == null) {
            res = resolveInternal(mappedPath);
        }

        if (res != null) {

            // keep, what we might have cut off in internal resolution
            final String resolutionPathInfo = res.getResourceMetadata().getResolutionPathInfo();

            LOGGER.debug("map: Path maps to resource {} with path info {}", res,
                resolutionPathInfo);

            // find aliases for segments. we can't walk the parent chain
            // since the request session might not have permissions to
            // read all parents SLING-2093
            String[] segments = Text.explode(res.getPath(), '/');
            if (segments.length > 0) {
                StringBuilder buf = new StringBuilder();
                Resource current = res.getResourceResolver().getResource("/");
                for (String name: segments) {
                    Resource child = current.getChild(name);
                    if (child == null) {
                        LOGGER.warn("map: could not load child resource {}/{} for alias mapping.", buf, name);
                        current = new NonExistingResource(res.getResourceResolver(), current.getPath() + "/" + name);
                        buf.append('/').append(name);
                    } else {
                        String alias = getProperty(child, PROP_ALIAS);
                        if (alias == null || alias.length() == 0) {
                            alias = name;
                        }
                        buf.append('/').append(alias);
                        current = child;
                    }
                }

                // reappend the resolutionPathInfo
                if (resolutionPathInfo != null) {
                    buf.append(resolutionPathInfo);
                }

                // and then we have the mapped path to work on
                mappedPath = buf.toString();
            } else {
                // root if no segments
            	mappedPath = "/";            		            	
            }

            LOGGER.debug("map: Alias mapping resolves to path {}", mappedPath);

        }

        boolean mappedPathIsUrl = false;
        for (final MapEntry mapEntry : this.factory.getMapEntries().getMapMaps()) {
            final String[] mappedPaths = mapEntry.replace(mappedPath);
            if (mappedPaths != null) {

                LOGGER.debug("map: Match for Entry {}", mapEntry);

                mappedPathIsUrl = !mapEntry.isInternal();

                if (mappedPathIsUrl && schemehostport != null) {

                    mappedPath = null;

                    for (final String candidate : mappedPaths) {
                        if (candidate.startsWith(schemehostport)) {
                            mappedPath = candidate.substring(schemehostport.length() - 1);
                            mappedPathIsUrl = false;
                            LOGGER.debug(
                                "map: Found host specific mapping {} resolving to {}",
                                candidate, mappedPath);
                            break;
                        } else if (candidate.startsWith(schemePrefix)
                            && mappedPath == null) {
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

                LOGGER.debug(
                    "resolve: MapEntry {} matches, mapped path is {}",
                    mapEntry, mappedPath);

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
            // return them in proper escaped form, including the path, via toString()
            URI uri = new URI(mappedPath, false);

            // 1. mangle the namespaces in the path
            String path = mangleNamespaces(uri.getPath());

            // 2. prepend servlet context path if we have a request
            if (request != null && request.getContextPath() != null
                && request.getContextPath().length() > 0) {
                path = request.getContextPath().concat(path);
            }
            // update the path part of the URI
            uri.setPath(path);

            mappedPath = uri.toString();
        } catch (URIException e) {
            LOGGER.warn("map: Unable to mangle namespaces for " + mappedPath
                    + " returning unmangled", e);
        }

        LOGGER.debug("map: Returning URL {} as mapping for path {}",
            mappedPath, resourcePath);

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
    public String[] getSearchPath() {
        checkClosed();
        return factory.getSearchPath().clone();
    }

    // ---------- direct resource access without resolution

    /**
     * @see org.apache.sling.api.resource.ResourceResolver#getResource(java.lang.String)
     */
    public Resource getResource(String path) {
        checkClosed();

        if (useMultiWorkspaces) {
            final int wsSepPos = path.indexOf(":/");
            if (wsSepPos != -1) {
                final String workspaceName = path.substring(0, wsSepPos);
                if (workspaceName.equals(getSession().getWorkspace().getName())) {
                    path = path.substring(wsSepPos + 1);
                } else {
                    try {
                        ResourceResolver wsResolver = getResolverForWorkspace(workspaceName);
                        return wsResolver.getResource(path.substring(wsSepPos + 1));
                    } catch (LoginException e) {
                        // requested a resource in a workspace I don't have access to.
                        // we treat this as a not found resource
                        return null;
                    }
                }
            }
        }

        // if the path is absolute, normalize . and .. segements and get res
        if (path.startsWith("/")) {
            path = ResourceUtil.normalize(path);
            Resource result = (path != null) ? getResourceInternal(path) : null;
            if ( result != null ) {
                String workspacePrefix = null;
                if ( !getSession().getWorkspace().getName().equals(this.factory.getDefaultWorkspaceName()) ) {
                    workspacePrefix = getSession().getWorkspace().getName();
                }

                result = this.factory.getResourceDecoratorTracker().decorate(result, workspacePrefix, null);
                return result;
            }
            return null;
        }

        // otherwise we have to apply the search path
        // (don't use this.getSearchPath() to save a few cycle for not cloning)
        String[] paths = factory.getSearchPath();
        if (paths != null) {
            for (String prefix : factory.getSearchPath()) {
                Resource res = getResource(prefix + path);
                if (res != null) {
                    return res;
                }
            }
        }

        // no resource found, if we get here
        return null;
    }

    /**
     * @see org.apache.sling.api.resource.ResourceResolver#getResource(org.apache.sling.api.resource.Resource, java.lang.String)
     */
    public Resource getResource(Resource base, String path) {
        checkClosed();

        if (!path.startsWith("/") && base != null) {
            path = base.getPath() + "/" + path;
        }

        return getResource(path);
    }

    /**
     * @see org.apache.sling.api.resource.ResourceResolver#listChildren(org.apache.sling.api.resource.Resource)
     */
    @SuppressWarnings("unchecked")
    public Iterator<Resource> listChildren(Resource parent) {
        checkClosed();
        final String path = parent.getPath();
        final int wsSepPos = path.indexOf(":/");
        if (wsSepPos != -1) {
            final String workspaceName = path.substring(0, wsSepPos);
            if (!workspaceName.equals(getSession().getWorkspace().getName())) {
                if (useMultiWorkspaces) {
                    try {
                        ResourceResolver wsResolver = getResolverForWorkspace(workspaceName);
                        return wsResolver.listChildren(parent);
                    } catch (LoginException e) {
                        // requested a resource in a workspace I don't have access to.
                        // we treat this as a not found resource
                        return Collections.EMPTY_LIST.iterator();
                    }
                }
                // this is illegal
                return Collections.EMPTY_LIST.iterator();
            } else if (parent instanceof WorkspaceDecoratedResource) {
                parent = ((WorkspaceDecoratedResource) parent).getResource();
            } else {
                LOGGER.warn("looking for children of workspace path {}, but with an undecorated resource.",
                        parent.getPath());
            }
        }

        String workspacePrefix = null;
        if ( !getSession().getWorkspace().getName().equals(this.factory.getDefaultWorkspaceName()) ) {
            workspacePrefix = getSession().getWorkspace().getName();
        }

        return new ResourceIteratorDecorator(
            this.factory.getResourceDecoratorTracker(), workspacePrefix,
            new ResourceIterator(parent, rootProvider));
    }

    // ---------- Querying resources

    /**
     * @see org.apache.sling.api.resource.ResourceResolver#findResources(java.lang.String, java.lang.String)
     */
    public Iterator<Resource> findResources(final String query, final String language)
    throws SlingException {
        checkClosed();
        try {
            Session session = null;
            String workspaceName = null;
            if (requestBoundResolver != null) {
                session = requestBoundResolver.adaptTo(Session.class);
                workspaceName = session.getWorkspace().getName();
            } else {
                session = getSession();
            }
            final QueryResult res = JcrResourceUtil.query(session, query, language);
            return new ResourceIteratorDecorator(this.factory.getResourceDecoratorTracker(), workspaceName,
                    new JcrNodeResourceIterator(this, res.getNodes(), factory.getDynamicClassLoader()));
        } catch (javax.jcr.query.InvalidQueryException iqe) {
            throw new QuerySyntaxException(iqe.getMessage(), query, language, iqe);
        } catch (RepositoryException re) {
            throw new SlingException(re.getMessage(), re);
        }
    }

    /**
     * @see org.apache.sling.api.resource.ResourceResolver#queryResources(java.lang.String, java.lang.String)
     */
    public Iterator<Map<String, Object>> queryResources(final String query,
                                                        final String language)
    throws SlingException {
        checkClosed();

        final String queryLanguage = isSupportedQueryLanguage(language) ? language : DEFAULT_QUERY_LANGUAGE;

        try {
            QueryResult result = JcrResourceUtil.query(adaptTo(Session.class), query,
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
                        LOGGER.error(
                            "queryResources$next: Problem accessing row values",
                            re);
                    }
                    return row;
                }

                public void remove() {
                    throw new UnsupportedOperationException("remove");
                }
            };
        } catch (javax.jcr.query.InvalidQueryException iqe) {
            throw new QuerySyntaxException(iqe.getMessage(), query, language,
                iqe);
        } catch (RepositoryException re) {
            throw new SlingException(re.getMessage(), re);
        }
    }

    /**
     * @see org.apache.sling.api.resource.ResourceResolver#getUserID()
     */
    public String getUserID() {
        checkClosed();
        return getSession().getUserID();
    }

    // ---------- Adaptable interface

    /**
     * @see org.apache.sling.adapter.SlingAdaptable#adaptTo(java.lang.Class)
     */
    @SuppressWarnings("unchecked")
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        checkClosed();
        if (type == Session.class) {
            if (requestBoundResolver != null) {
                return (AdapterType) requestBoundResolver.adaptTo(Session.class);
            }
            return (AdapterType) getSession();
        }

        // fall back to default behaviour
        return super.adaptTo(type);
    }

    // ---------- internal

    /**
     * Returns the JCR Session of the root resource provider which provides
     * access to the repository.
     */
    private Session getSession() {
        return rootProvider.getSession();
    }

    /**
     * Get a resolver for the workspace.
     */
    private synchronized JcrResourceResolver getResolverForWorkspace(
            final String workspaceName) throws LoginException {
        if (createdResolvers == null) {
            createdResolvers = new HashMap<String, JcrResourceResolver>();
        }
        JcrResourceResolver wsResolver = createdResolvers.get(workspaceName);
        if (wsResolver == null) {
            final Map<String, Object> newAuthInfo = new HashMap<String, Object>();
            newAuthInfo.put(JcrResourceConstants.AUTHENTICATION_INFO_WORKSPACE,
                workspaceName);
            wsResolver = (JcrResourceResolver) clone(newAuthInfo);
            createdResolvers.put(workspaceName, wsResolver);
        }
        return wsResolver;
    }

    /**
     * Returns a string used for matching map entries against the given request
     * or URI parts.
     *
     * @param scheme The URI scheme
     * @param host The host name
     * @param port The port number. If this is negative, the default value used
     *            is 80 unless the scheme is "https" in which case the default
     *            value is 443.
     * @param path The (absolute) path
     * @return The request path string {scheme}/{host}.{port}{path}.
     */
    public static String getMapPath(String scheme, String host, int port, String path) {
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
     * <li>Drill down the resource tree from the root down to the resource
     * trying to get the child as per the respective path segment or finding a
     * child whose <code>sling:alias</code> property is set to the respective
     * name.
     * </ol>
     * <p>
     * If neither mechanism (direct access and drill down) resolves to a
     * resource this method returns <code>null</code>.
     *
     * @param absPath The absolute path of the resource to return.
     * @return The resource found or <code>null</code> if the resource could
     *         not be found. The
     *         {@link org.apache.sling.api.resource.ResourceMetadata#getResolutionPathInfo() resolution path info}
     *         field of the resource returned is set to the part of the
     *         <code>absPath</code> which has been cut off by the
     *         {@link ResourcePathIterator} to resolve the resource.
     */
    private Resource resolveInternal(String absPath) {
        Resource resource = null;
        String curPath = absPath;
        try {
            final ResourcePathIterator it = new ResourcePathIterator(absPath);
            while (it.hasNext() && resource == null) {
                curPath = it.next();
                resource = getResourceInternal(curPath);
            }
        } catch (Exception ex) {
            throw new SlingException("Problem trying " + curPath
                + " for request path " + absPath, ex);
        }

        // SLING-627: set the part cut off from the uriPath as
        // sling.resolutionPathInfo property such that
        // uriPath = curPath + sling.resolutionPathInfo
        if (resource != null) {

            String rpi = absPath.substring(curPath.length());
            resource.getResourceMetadata().setResolutionPathInfo(rpi);

            LOGGER.debug(
                "resolveInternal: Found resource {} with path info {} for {}",
                new Object[] { resource, rpi, absPath });

        } else {

            // no direct resource found, so we have to drill down into the
            // resource tree to find a match
            resource = getResourceInternal("/");
            StringBuilder resolutionPath = new StringBuilder();
            StringTokenizer tokener = new StringTokenizer(absPath, "/");
            while (resource != null && tokener.hasMoreTokens()) {
                String childNameRaw = tokener.nextToken();

                Resource nextResource = getChildInternal(resource, childNameRaw);
                if (nextResource != null) {

                    resource = nextResource;
                    resolutionPath.append("/").append(childNameRaw);

                } else {

                    String childName = null;
                    ResourcePathIterator rpi = new ResourcePathIterator(
                        childNameRaw);
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

                LOGGER.debug(
                    "resolveInternal: Found resource {} with path info {} for {}",
                    new Object[] { resource, pathInfo, absPath });
            }
        }

        return resource;
    }

    private Resource getChildInternal(Resource parent, String childName) {
        Resource child = getResource(parent, childName);
        if (child != null) {
            String alias = getProperty(child, PROP_REDIRECT_INTERNAL);
            if (alias != null) {
                // TODO: might be a redirect ??
                LOGGER.warn(
                    "getChildInternal: Internal redirect to {} for Resource {} is not supported yet, ignoring",
                    alias, child);
            }

            // we have the resource name, continue with the next level
            return child;
        }

        // we do not have a child with the exact name, so we look for
        // a child, whose alias matches the childName
        Iterator<Resource> children = listChildren(parent);
        while (children.hasNext()) {
            child = children.next();
            String[] aliases = getProperty(child, PROP_ALIAS, String[].class);
            if (aliases != null) {
                for (String alias : aliases) {
                    if (childName.equals(alias)) {
                        LOGGER.debug(
                            "getChildInternal: Found Resource {} with alias {} to use",
                            child, childName);
                        return child;
                    }
                }
            }
        }

        // no match for the childName found
        LOGGER.debug("getChildInternal: Resource {} has no child {}", parent,
            childName);
        return null;
    }

    /**
     * Creates a JcrNodeResource with the given path if existing
     */
    protected Resource getResourceInternal(String path) {

        Resource resource = rootProvider.getResource(this, path);
        if (resource != null) {
            resource.getResourceMetadata().setResolutionPath(path);
            return resource;
        }

        LOGGER.debug(
            "getResourceInternal: Cannot resolve path '{}' to a resource", path);
        return null;
    }

    public String getProperty(Resource res, String propName) {
        return getProperty(res, propName, String.class);
    }

    public <Type> Type getProperty(Resource res, String propName,
            Class<Type> type) {

        // check the property in the resource itself
        ValueMap props = res.adaptTo(ValueMap.class);
        if (props != null) {
            Type prop = props.get(propName, type);
            if (prop != null) {
                LOGGER.debug("getProperty: Resource {} has property {}={}",
                    new Object[] { res, propName, prop });
                return prop;
            }
            // otherwise, check it in the jcr:content child resource
            // This is a special case checking for JCR based resources
            // we directly use the deep resolution of properties of the
            // JCR value map implementation - this does not work
            // in non JCR environments, however in non JCR envs there
            // is usually no "jcr:content" child node anyway
            prop = props.get("jcr:content/" + propName, type);
            return prop;
        }

        return null;
    }

    /**
     * Returns the <code>path</code> as an absolute path. If the path is
     * already absolute it is returned unmodified (the same instance actually).
     * If the path is relative it is made absolute by prepending the first entry
     * of the {@link #getSearchPath() search path}.
     *
     * @param path The path to ensure absolute
     * @return The absolute path as explained above
     */
    private String ensureAbsPath(String path) {
        if (!path.startsWith("/")) {
            path = getSearchPath()[0] + path;
        }
        return path;
    }

    private String mangleNamespaces(String absPath) {
        if (factory.isMangleNamespacePrefixes() && absPath.contains(MANGLE_NAMESPACE_OUT_SUFFIX)) {
            Pattern p = Pattern.compile(MANGLE_NAMESPACE_OUT);
            Matcher m = p.matcher(absPath);

            StringBuffer buf = new StringBuffer();
            while (m.find()) {
                String replacement = MANGLE_NAMESPACE_IN_PREFIX + m.group(1) + MANGLE_NAMESPACE_IN_SUFFIX;
                m.appendReplacement(buf, replacement);
            }

            m.appendTail(buf);

            absPath = buf.toString();
        }

        return absPath;
    }

    private String unmangleNamespaces(String absPath) {
        if (factory.isMangleNamespacePrefixes() && absPath.contains(MANGLE_NAMESPACE_IN_PREFIX)) {
            Pattern p = Pattern.compile(MANGLE_NAMESPACE_IN);
            Matcher m = p.matcher(absPath);
            StringBuffer buf = new StringBuffer();
            while (m.find()) {
                String namespace = m.group(1);
                try {

                    // throws if "namespace" is not a registered
                    // namespace prefix
                    getSession().getNamespaceURI(namespace);

                    String replacement = MANGLE_NAMESPACE_OUT_PREFIX
                        + namespace + MANGLE_NAMESPACE_OUT_SUFFIX;
                    m.appendReplacement(buf, replacement);

                } catch (NamespaceException ne) {

                    // not a valid prefix
                    LOGGER.debug(
                        "unmangleNamespaces: '{}' is not a prefix, not unmangling",
                        namespace);

                } catch (RepositoryException re) {

                    LOGGER.warn(
                        "unmangleNamespaces: Problem checking namespace '{}'",
                        namespace, re);

                }
            }
            m.appendTail(buf);
            absPath = buf.toString();
        }

        return absPath;
    }

    private boolean isSupportedQueryLanguage(String language) {
        try {
            String[] supportedLanguages = adaptTo(Session.class).getWorkspace().
                getQueryManager().getSupportedQueryLanguages();
            for (String lang : supportedLanguages) {
                if (lang.equals(language)) {
                    return true;
                }
            }
        } catch (RepositoryException e) {
            LOGGER.error("Unable to discover supported query languages", e);
        }
        return false;
    }
}
