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
import java.util.Map;
import java.util.regex.Matcher;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;
import javax.servlet.http.HttpServletRequest;

import org.apache.sling.adapter.SlingAdaptable;
import org.apache.sling.api.SlingException;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.QuerySyntaxException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.jcr.resource.JcrResourceUtil;
import org.apache.sling.jcr.resource.internal.JcrResourceResolverFactoryImpl.ResourcePattern;
import org.apache.sling.jcr.resource.internal.helper.Mapping;
import org.apache.sling.jcr.resource.internal.helper.ResourcePathIterator;
import org.apache.sling.jcr.resource.internal.helper.jcr.JcrNodeResourceIterator;
import org.apache.sling.jcr.resource.internal.helper.jcr.JcrResourceProviderEntry;
import org.apache.sling.jcr.resource.internal.helper.starresource.StarResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>JcrResourceResolver</code> class implements the Sling
 * <code>ResourceResolver</code> interface. Instances of this class
 * are retrieved through the
 * {@link org.apache.sling.jcr.resource.JcrResourceResolverFactory#getResourceResolver(Session)}
 * method.
 */
public class JcrResourceResolver extends SlingAdaptable implements
        ResourceResolver {

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final JcrResourceProviderEntry rootProvider;

    private final JcrResourceResolverFactoryImpl factory;

    public JcrResourceResolver(JcrResourceProviderEntry rootProvider,
            JcrResourceResolverFactoryImpl factory) {
        this.rootProvider = rootProvider;
        this.factory = factory;
    }

    // ---------- ResourceResolver interface ----------------------------------

    public Resource resolve(HttpServletRequest request) throws SlingException {
        String pathInfo = request.getPathInfo();

        // servlet directly address, so there is no path info, use "/" then
        if (pathInfo == null) {
            pathInfo = "/";
        }

        Resource result = resolve(pathInfo);

        if (result == null) {
            if(StarResource.appliesTo(request)) {
                result = new StarResource(this, pathInfo, rootProvider.getResourceTypeProviders());
            } else {
                result = new NonExistingResource(this, pathInfo);
            }
        }

        return result;
    }

    public Resource resolve(String uri) throws SlingException {

        // resolve virtual uri
        String realUrl = factory.virtualToRealUri(uri);
        if (realUrl != null) {
            log.debug("resolve: Using real url '{}' for virtual url '{}'",
                realUrl, uri);
            uri = realUrl;
        }

        try {

            // translate url to a mapped url structure
            Resource result = urlToResource(uri);
            return result;

        } catch (SlingException se) {
            // rethrow SlingException as it is declared
            throw se;

        } catch (Throwable t) {
            // wrap any other issue into a SlingException
            throw new SlingException("Problem resolving " + uri, t);
        }

    }

    public String map(String resourcePath) {

        // get first map
        String href = null;
        Mapping[] mappings = factory.getMappings();
        for (int i = 0; i < mappings.length && href == null; i++) {
            href = mappings[i].mapHandle(resourcePath);
        }

        // if no mapping's to prefix matches the handle, use the handle itself
        if (href == null) {
            href = resourcePath;
        }

        // apply regexp
        href = applyPattern(href, factory.getBackPatterns());

        // check virtual mappings
        String virtual = factory.realToVirtualUri(href);
        if (virtual != null) {
            log.debug("map: Using virtual URI {} for path {}", virtual, href);
            href = virtual;
        }

        log.debug("map: {} -> {}", resourcePath, href);
        return href;
    }

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

    public String[] getSearchPath() {
        return factory.getSearchPath().clone();
    }

    public Iterator<Resource> listChildren(Resource parent) {
        return rootProvider.listChildren(parent);
    }

    public Iterator<Resource> findResources(String query, String language)
            throws SlingException {
        try {
            QueryResult res = JcrResourceUtil.query(getSession(), query,
                language);
            return new JcrNodeResourceIterator(this, res.getNodes(), rootProvider.getResourceTypeProviders());
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

    @SuppressWarnings("unchecked")
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        if (type == Session.class) {
            return (AdapterType) getSession();
        }

        // fall back to default behaviour
        return super.adaptTo(type);
    }

    // ---------- implementation helper ----------------------------------------

    public Session getSession() {
        return rootProvider.getSession();
    }

    /**
     * Try to get a resource for the given url
     * @param uri The url
     * @return The resource or null
     * @throws SlingException
     */
    private Resource urlToResource(String uri)
    throws SlingException {
        // apply regexp
        uri = applyPattern(uri, factory.getPatterns());

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
                String path = rm.getResolutionPath();
                String uriPath = mappings[i].mapHandle(path);
                if (uriPath != null && !uriPath.equals(path)) {
                    rm.setResolutionPath(uriPath);
                }

                return resource;
            }

            log.debug("Cannot resolve {} to resource", mappedUri);
        }

        // handle vanity paths
        final ResourcePathIterator it = new ResourcePathIterator(uri);
        while (it.hasNext() ) {
            final String curPath = it.next();
            final Resource resource = searchVanityPath(curPath);
            if ( resource != null ) {
                final ResourceMetadata rm = resource.getResourceMetadata();
                final String rpi = uri.substring(curPath.length());
                // append html if no extension available
                if ( rpi.indexOf('.') == -1 ) {
                    rm.setResolutionPathInfo(rpi + ".html");
                } else {
                    rm.setResolutionPathInfo(rpi);
                }

                final String path = rm.getResolutionPath();
                if (!uri.equals(path)) {
                    rm.setResolutionPath(uri);
                }

                return resource;
            }
        }

        log.debug("Could not resolve URL {} to a Resource", uri);
        return null;

    }

    /**
     * Apply the regexp patterns
     * @param uri
     * @param patterns
     * @return
     */
    private String applyPattern(String uri, final ResourcePattern[] patterns) {
        for(final ResourcePattern pattern : patterns) {
            final Matcher matcher = pattern.pattern.matcher(uri);

            final StringBuffer myStringBuffer = new StringBuffer();
            while (matcher.find()) {
                String repl = pattern.replacement;
                for(int i=1; i<=matcher.groupCount(); i++) {
                    int pos = repl.indexOf("$"+i);
                    if ( pos != -1 ) {
                       repl = repl.substring(0, pos) + matcher.group(i) + repl.substring(pos +2);
                    }
                }
                matcher.appendReplacement(myStringBuffer, repl);
            }
            matcher.appendTail(myStringBuffer);
            uri = myStringBuffer.toString();
        }
        return uri;
    }

    /**
     * Search for a vanity path for this
     * @param path
     * @return
     */
    private Resource searchVanityPath(String path) {
        // sling:VanityPath (uppercase V) is the mixin name
        // sling:vanityPath (lowercase) is the property name 
        final String queryString = "SELECT * FROM sling:VanityPath where sling:vanityPath ='" + path +
            "' ORDER BY sling:vanityOrder DESC";
        final Iterator<Resource> i = this.findResources(queryString, Query.SQL);
        if ( i.hasNext() ) {
            Resource rsrc = i.next();
            boolean needsWrapper = false;
            final ValueMap properties = rsrc.adaptTo(ValueMap.class);
            if ( properties != null ) {
                needsWrapper = properties.get("sling:redirect", false);
            }
            // check if this is a content node, then use the parent
            if ( ResourceUtil.getName(rsrc).equals("jcr:content") ) {
                rsrc = ResourceUtil.getParent(rsrc);
            }
            if ( needsWrapper ) {
                rsrc = new RedirectResource(rsrc, path, rsrc.getPath());
            }
            return rsrc;
        }
        return null;
    }

    private Resource scanPath(final String uriPath)
            throws SlingException {
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

        // SLING-627: set the part cut off from the uriPath as
        // sling.resolutionPathInfo property such that
        // uriPath = curPath + sling.resolutionPathInfo
        if (resource != null) {
            String rpi = uriPath.substring(curPath.length());
            resource.getResourceMetadata().setResolutionPathInfo(rpi);
        }

        return resource;
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

        log.debug("Cannot resolve path '{}' to a resource", path);
        return null;
    }

    private static final class RedirectResource implements Resource {

        final Resource resource;

        final String target;

        final String path;

        public RedirectResource(final Resource rsrc,
                                final String path,
                                final String target) {
            this.resource = rsrc;
            this.path = path;
            this.target = target;
        }

        /**
         * @see org.apache.sling.api.resource.Resource#getPath()
         */
        public String getPath() {
            return this.path;
        }

        /**
         * @see org.apache.sling.api.resource.Resource#getResourceMetadata()
         */
        public ResourceMetadata getResourceMetadata() {
            return this.resource.getResourceMetadata();
        }

        /**
         * @see org.apache.sling.api.resource.Resource#getResourceResolver()
         */
        public ResourceResolver getResourceResolver() {
            return this.resource.getResourceResolver();
        }

        /**
         * @see org.apache.sling.api.resource.Resource#getResourceSuperType()
         */
        public String getResourceSuperType() {
            return null;
        }

        /**
         * @see org.apache.sling.api.resource.Resource#getResourceType()
         */
        public String getResourceType() {
            return "sling:redirect";
        }

        /**
         * @see org.apache.sling.api.adapter.Adaptable#adaptTo(java.lang.Class)
         */
        @SuppressWarnings("unchecked")
        public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
            if ( type == ValueMap.class ) {
                return (AdapterType) new ValueMapDecorator(Collections.singletonMap("sling:target", (Object)this.target));
            }
            return null;
        }

    }
}
