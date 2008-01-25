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

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;
import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.SlingException;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.QuerySyntaxException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.resource.JcrResourceUtil;
import org.apache.sling.jcr.resource.PathResolver;
import org.apache.sling.jcr.resource.internal.helper.Descendable;
import org.apache.sling.jcr.resource.internal.helper.Mapping;
import org.apache.sling.jcr.resource.internal.helper.ResourcePathIterator;
import org.apache.sling.jcr.resource.internal.helper.ResourceProviderEntry;
import org.apache.sling.jcr.resource.internal.helper.jcr.JcrNodeResourceIterator;
import org.apache.sling.jcr.resource.internal.helper.jcr.JcrPropertyResource;
import org.apache.sling.jcr.resource.internal.helper.jcr.JcrResourceProviderEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>JcrResourceResolver</code> class implements the Sling
 * <code>ResourceResolver</code> and <code>ResourceResolver</code>
 * interfaces and in addition is a {@link PathResolver}. Instances of this
 * class are retrieved through the
 * {@link org.apache.sling.jcr.resource.JcrResourceResolverFactory#getResourceResolver(Session)}
 * method.
 */
public class JcrResourceResolver implements ResourceResolver, PathResolver {

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final JcrResourceProviderEntry rootProvider;
    private final JcrResourceResolverFactoryImpl factory;
    
    public JcrResourceResolver(JcrResourceProviderEntry rootProvider, JcrResourceResolverFactoryImpl factory) {
        this.rootProvider = rootProvider;
        this.factory = factory;
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
        path = JcrResourceUtil.normalize(path);
        if (path != null) {
            try {
                Resource resource = getResourceInternal(path);
                return resource;
            } catch (Exception ex) {
                throw new SlingException("Problem accessing resource" + path,
                    ex);
            }
        }

        // relative path segments cannot be resolved
        return null;
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
            return new JcrNodeResourceIterator(rootProvider.getResourceProvider(), res.getNodes());
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

    public String pathToURL(Resource resource) {
        String path = resource.getPath();

        // get first map
        String href = null;
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

        log.debug("MapHandle: {} -> {}", path, href);
        return href;
    }

    // ---------- implementation helper ----------------------------------------

    public Session getSession() {
        return rootProvider.getSession();
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

        Resource resource= null;

        ResourceProviderEntry rp = rootProvider.getResourceProvider(path);
        while (rp != null && resource == null) {
            resource = rp.getResourceProvider().getResource(path);
            rp = rp.getParentEntry();
        }
        
        if (resource != null) {
            resource.getResourceMetadata().put(
                ResourceMetadata.RESOLUTION_PATH, path);
            return resource;
        }

        log.debug("Cannot resolve path '{}' to a resource", path);
        return null;
    }

}
