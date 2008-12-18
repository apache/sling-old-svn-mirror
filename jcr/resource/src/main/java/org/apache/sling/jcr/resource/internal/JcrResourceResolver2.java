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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;
import javax.servlet.http.HttpServletRequest;

import org.apache.sling.adapter.SlingAdaptable;
import org.apache.sling.api.SlingException;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.QuerySyntaxException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.jcr.resource.JcrResourceUtil;
import org.apache.sling.jcr.resource.internal.helper.MapEntries;
import org.apache.sling.jcr.resource.internal.helper.MapEntry;
import org.apache.sling.jcr.resource.internal.helper.RedirectResource;
import org.apache.sling.jcr.resource.internal.helper.ResourcePathIterator;
import org.apache.sling.jcr.resource.internal.helper.jcr.JcrNodeResourceIterator;
import org.apache.sling.jcr.resource.internal.helper.jcr.JcrResourceProviderEntry;
import org.apache.sling.jcr.resource.internal.helper.starresource.StarResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrResourceResolver2 extends SlingAdaptable implements
        ResourceResolver {

    private static final String MANGLE_NAMESPACE_IN_SUFFIX = "_";

    private static final String MANGLE_NAMESPACE_IN_PREFIX = "/_";

    private static final String MANGLE_NAMESPACE_IN = "/_([^_]+)_";

    private static final String MANGLE_NAMESPACE_OUT_SUFFIX = ":";

    private static final String MANGLE_NAMESPACE_OUT_PREFIX = "/";

    private static final String MANGLE_NAMESPACE_OUT = "/([^:]+):";

    public static final String PROP_REG_EXP = "sling:match";

    public static final String PROP_REDIRECT_INTERNAL = "sling:internalRedirect";

    public static final String PROP_ALIAS = "sling:alias";

    public static final String PROP_REDIRECT_EXTERNAL = "sling:redirect";
    
    public static final String PROP_REDIRECT_EXTERNAL_STATUS = "sling:status";

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final JcrResourceProviderEntry rootProvider;

    private final JcrResourceResolverFactoryImpl factory;

    private final MapEntries resourceMapper;

    public JcrResourceResolver2(JcrResourceProviderEntry rootProvider,
            JcrResourceResolverFactoryImpl factory, MapEntries resourceMapper) {
        this.rootProvider = rootProvider;
        this.factory = factory;
        this.resourceMapper = resourceMapper;
    }

    // ---------- resolving resources

    public Resource resolve(HttpServletRequest request) {
        // throws NPE if request is null as required
        return resolve(request, request.getPathInfo());
    }

    public Resource resolve(HttpServletRequest request, String absPath) {
        // make sure abspath is not null and is absolute
        if (absPath == null) {
            absPath = "/";
        } else if (!absPath.startsWith("/")) {
            absPath = "/" + absPath;
        }

        return resolveInternal(request, absPath, true);
    }
    
    public Resource resolve(String absPath) {
        if (absPath == null) {
            throw new NullPointerException("absPath");
        }
        
        return resolveInternal(null, absPath, false);
    }

    // calls map(HttpServletRequest, String) as map(null, resourcePath)
    public String map(String resourcePath) {
        return map(null, resourcePath);
    }

    // full implementation
    //   - apply sling:alias from the resource path
    //   - apply /etc/map mappings (inkl. config backwards compat)
    //   - return absolute uri if possible
    public String map(final HttpServletRequest request, final String resourcePath) {
        
        String mappedPath = resourcePath;
        boolean mappedPathIsUrl = false;
        String resolutionPathInfo;

        // cut off scheme and host, if the same as requested
        String schemehostport;
        if (request != null) {
            schemehostport = MapEntry.getURI(request.getScheme(),
                request.getServerName(), request.getServerPort(), "/");

            log.debug("map: Mapping path {} for {}", resourcePath,
                schemehostport);

        } else {

            schemehostport = null;
            log.debug("map: Mapping path {} for default", resourcePath);

        }

        Resource res = resolveInternal(mappedPath);
        if (res != null) {

            // keep, what we might have cut off in internal resolution
            resolutionPathInfo = res.getResourceMetadata().getResolutionPathInfo();
            
            log.debug("map: Path maps to resource {} with path info {}", res,
                resolutionPathInfo);
            
            // find aliases for segments
            LinkedList<String> names = new LinkedList<String>();
            while (res != null) {
                String alias = getProperty(res, PROP_ALIAS);
                if (alias == null) {
                    alias = ResourceUtil.getName(res);
                }
                if (alias != null && alias.length() > 0) {
                    names.add(alias);
                }
                res = ResourceUtil.getParent(res);
            }
            
            // build path from segment names
            StringBuilder buf = new StringBuilder();
            while (!names.isEmpty()) {
                buf.append('/');
                buf.append(names.removeLast());
            }
            mappedPath = buf.toString();
            
            log.debug("map: Alias mapping resolves to path {}", mappedPath);
            
        } else {
            
            // we have no resource, hence no resolution path info
            resolutionPathInfo = null;
            
        }
        
        for (MapEntry mapEntry : resourceMapper.getMapMaps()) {
            String[] mappedPaths = mapEntry.replace(mappedPath);
            if (mappedPaths != null) {

                log.debug("map: Match for Entry {}", mapEntry);
                
                mappedPath = mappedPaths[0];
                mappedPathIsUrl = !mapEntry.isInternal();

                if (mappedPathIsUrl && schemehostport != null) {
                    for (String candidate : mappedPaths) {
                        if (candidate.startsWith(schemehostport)) {
                            mappedPath = candidate.substring(schemehostport.length() - 1);
                            log.debug(
                                "map: Found host specific mapping {} resolving to {}",
                                candidate, mappedPath);
                            break;
                        }
                    }
                }

                log.debug(
                    "resolve: MapEntry {} matches, mapped path is {}",
                    mapEntry, mappedPath);

                break;
            }
        }

        // this should not be the case, since mappedPath is primed
        if (mappedPath == null) {
            mappedPath = resourcePath;
        }
        
        // append resolution path info, which might have been cut off above
        if (resolutionPathInfo != null) {
            mappedPath = mappedPath.concat(resolutionPathInfo);
        }
        
        if (mappedPathIsUrl) {
            // TODO: Consider mangling the path but not the scheme and
            // esp. the host:port part
            
            log.debug("map: Returning URL {} as mapping for path {}",
                mappedPath, resourcePath);
            
            return mappedPath;
        }

        // mangle the namespaces
        mappedPath = mangleNamespaces(mappedPath);

        // prepend servlet context path if we have a request
        if (request != null && request.getContextPath() != null
            && request.getContextPath().length() > 0) {
            mappedPath = request.getContextPath().concat(mappedPath);
        }

        log.debug(
            "map: Returning path {} (after mangling, inlc. context) for {}",
            mappedPath, resourcePath);
        
        return mappedPath;
    }

    // ---------- search path for relative resoures

    public String[] getSearchPath() {
        return factory.getSearchPath().clone();
    }

    // ---------- direct resource access without resolution

    public Resource getResource(String path) {

        // if the path is absolute, normalize . and .. segements and get res
        if (path.startsWith("/")) {
            path = ResourceUtil.normalize(path);
            return (path != null) ? getResourceInternal(path) : null;
        }

        // otherwise we have to apply the search path
        // (don't use this.getSearchPath() to save a few cycle for not cloning)
        for (String prefix : factory.getSearchPath()) {
            Resource res = getResource(prefix + path);
            if (res != null) {
                return res;
            }
        }

        // no resource found, if we get here
        return null;
    }

    public Resource getResource(Resource base, String path) {

        if (!path.startsWith("/") && base != null) {
            path = base.getPath() + "/" + path;
        }

        return getResource(path);
    }

    public Iterator<Resource> listChildren(Resource parent) {
        return rootProvider.listChildren(parent);
    }

    // ---------- Querying resources

    public Iterator<Resource> findResources(String query, String language)
            throws SlingException {
        try {
            QueryResult res = JcrResourceUtil.query(getSession(), query,
                language);
            return new JcrNodeResourceIterator(this, res.getNodes(),
                rootProvider.getResourceTypeProviders());
        } catch (javax.jcr.query.InvalidQueryException iqe) {
            throw new QuerySyntaxException(iqe.getMessage(), query, language,
                iqe);
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
                            Value v = values[i];
                            if (v != null) {
                                row.put(colNames[i],
                                    JcrResourceUtil.toJavaObject(values[i]));
                            }
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
        } catch (javax.jcr.query.InvalidQueryException iqe) {
            throw new QuerySyntaxException(iqe.getMessage(), query, language,
                iqe);
        } catch (RepositoryException re) {
            throw new SlingException(re.getMessage(), re);
        }
    }

    // ---------- Adaptable interface

    @SuppressWarnings("unchecked")
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        if (type == Session.class) {
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
    
    // expect absPath to be non-null and absolute
    public Resource resolveInternal(HttpServletRequest request, String absPath,
            boolean requireResource) {

        // check for special namespace prefix treatment
        absPath = unmangleNamespaces(absPath);
        
        // Assume http://localhost:80 if request is null
        String[] realPathList = { absPath };
        String requestPath;
        if (request != null) {
            requestPath = getMapPath(request.getScheme(),
                request.getServerName(), request.getServerPort(), absPath);
        } else {
            requestPath = getMapPath("http", "localhost", 80, absPath);
        }

        log.debug("resolve: Resolving request path {}", requestPath);

        // loop while finding internal or external redirect into the
        // content out of the virtual host mapping tree
        // the counter is to ensure we are not caught in an endless loop here
        // TODO: might do better to be able to log the loop and help the user
        for (int i = 0; i < 100; i++) {

            String[] mappedPath = null;
            for (MapEntry mapEntry : resourceMapper.getResolveMaps()) {
                mappedPath = mapEntry.replace(requestPath);
                if (mappedPath != null) {
                    log.debug(
                        "resolve: MapEntry {} matches, mapped path is {}",
                        mapEntry, mappedPath);

                    if (mapEntry.isInternal()) {
                        // internal redirect
                        log.debug("resolve: Redirecting internally");
                        break;
                    }

                    // external redirect
                    log.debug("resolve: Returning external redirect");
                    return new RedirectResource(this, absPath, mappedPath[0]);
                }
            }

            // if there is no virtual host based path mapping, abort
            // and use the original realPath
            if (mappedPath == null) {
                log.debug(
                    "resolve: Request path {} does not match any MapEntry",
                    requestPath);
                break;
            }

            // if the mapped path is not an URL, use this path to continue
            if (!mappedPath[0].contains("://")) {
                log.debug("resolve: Mapped path is for resource tree");
                realPathList = mappedPath;
                break;
            }

            // otherwise the mapped path is an URI and we have to try to
            // resolve that URI now, using the URI's path as the real path
            try {
                URI uri = new URI(mappedPath[0]);
                requestPath = getMapPath(uri.getScheme(), uri.getHost(),
                    uri.getPort(), uri.getPath());
                realPathList = new String[] { uri.getPath() };

                log.debug(
                    "resolve: Mapped path is an URL, using new request path {}",
                    requestPath);
            } catch (URISyntaxException use) {
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

                log.debug("resolve: Mapped path {} is a Star Resource",
                    realPath);
                res = new StarResource(this, ensureAbsPath(realPath),
                    factory.getJcrResourceTypeProvider());

            } else

            if (realPath.startsWith("/")) {

                // let's check it with a direct access first
                log.debug("resolve: Try absolute mapped path {}", realPath);
                res = resolveInternal(realPath);

            } else {

                String[] searchPath = getSearchPath();
                for (int spi = 0; res == null && spi < searchPath.length; spi++) {
                    log.debug(
                        "resolve: Try relative mapped path with search path entry {}",
                        searchPath[spi]);
                    res = resolveInternal(searchPath[spi] + realPath);
                }

            }

        }
        
        if (res == null) {
            if (requireResource) {
                log.debug(
                    "resolve: Path {} does not resolve, returning NonExistingResource at {}",
                    absPath, realPathList[0]);
                res = new NonExistingResource(this,
                    ensureAbsPath(realPathList[0]));
            } else {
                log.debug("resolve: No Resource for {}", absPath);
            }
        } else {
            log.debug("resolve: Path {} resolves to Resource {}", absPath, res);
        }

        return res;
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
            
            log.debug(
                "resolveInternal: Found resource {} with path info {} for {}",
                new Object[] { resource, rpi, absPath });

        } else {

            // no direct resource found, so we have to drill down into the
            // resource tree to find a match
            resource = getResourceInternal("/");
            StringTokenizer tokener = new StringTokenizer(absPath, "/");
            while (resource != null && tokener.hasMoreTokens()) {
                String childNameRaw = tokener.nextToken();

                Resource nextResource = getChildInternal(resource, childNameRaw);
                if (nextResource != null) {

                    resource = nextResource;

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

                    // SLING-627: set the part cut off from the uriPath as
                    // sling.resolutionPathInfo property such that
                    // uriPath = curPath + sling.resolutionPathInfo
                    if (nextResource != null) {
                        String path = ResourceUtil.normalize(ResourceUtil.getParent(
                            nextResource).getPath()
                            + "/" + childName);
                        String pathInfo = absPath.substring(path.length());
                        nextResource.getResourceMetadata().setResolutionPathInfo(
                            pathInfo);
                        break;
                    }
                }
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
                log.warn(
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
            String alias = getProperty(child, PROP_ALIAS);
            if (childName.equals(alias)) {
                log.debug(
                    "getChildInternal: Found Resource {} with alias {} to use",
                    child, childName);
                return child;
            }
        }

        // no match for the childName found
        log.debug("getChildInternal: Resource {} has no child {}", parent,
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

        log.debug(
            "getResourceInternal: Cannot resolve path '{}' to a resource", path);
        return null;
    }

    public String getProperty(Resource res, String propName) {
        
        // check the property in the resource itself
        ValueMap props = res.adaptTo(ValueMap.class);
        if (props != null) {
            String prop = props.get(propName, String.class);
            if (prop != null) {
                log.debug("getProperty: Resource {} has property {}={}",
                    new Object[] { res, propName, prop });
                return prop;
            }
        }
        
        // otherwise, check it in the jcr:content child resource
        res = getResource(res, "jcr:content");
        if (res != null) {
            return getProperty(res, propName);
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
            Set<String> namespaces = resourceMapper.getNamespacePrefixes();
            Pattern p = Pattern.compile(MANGLE_NAMESPACE_IN);
            Matcher m = p.matcher(absPath);
            StringBuffer buf = new StringBuffer();
            while (m.find()) {
                String namespace = m.group(1);
                if (namespaces.contains(namespace)) {
                    String replacement = MANGLE_NAMESPACE_OUT_PREFIX + namespace + MANGLE_NAMESPACE_OUT_SUFFIX;
                    m.appendReplacement(buf, replacement);
                }
            }
            m.appendTail(buf);
            absPath = buf.toString();
        }
        
        return absPath;
    }
}
