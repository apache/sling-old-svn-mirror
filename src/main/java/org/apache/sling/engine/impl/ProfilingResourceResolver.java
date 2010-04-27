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
package org.apache.sling.engine.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceWrapper;

public class ProfilingResourceResolver implements ResourceResolver {

    private final ResourceResolver delegatee;

//    private final Logger logger = LoggerFactory.getLogger(this.getClass());

//    int getResource;

//    int adaptToCount;

//    int adaptToCountCacheHit;

    private final Map<String, Object> adaptToCache = new HashMap<String, Object>();

    public ProfilingResourceResolver(final ResourceResolver delegatee) {
        this.delegatee = delegatee;
    }

    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        final String key = type.getName();
//        adaptToCount++;
        Object obj = adaptToCache.get(key);
        if ( obj == null ) {
//            logger.info("Adapting to {}", type);
            obj = delegatee.adaptTo(type);
            adaptToCache.put(key, obj);
//        } else {
//            adaptToCountCacheHit++;
        }
        return (AdapterType) obj;
    }

    public void close() {
//        logger.info("* #getResource({})", getResource);
//        logger.info("* #adaptTo({})", adaptToCount);
//        logger.info("* #adaptToCacheHit({})", adaptToCountCacheHit);
        delegatee.close();
    }

    public Iterator<Resource> findResources(String query, String language) {
        return new IteratorDecorator(delegatee.findResources(query, language), this);
    }

    public Resource getResource(Resource base, String path) {
//        getResource++;
//        logger.info("* getResource({}, {})", base.getPath(), path);
        final Resource rsrc = delegatee.getResource(base, path);
        return rsrc == null ? null : new InternalResource(rsrc, this);
    }

    public Resource getResource(String path) {
//        getResource++;
//        if ( path.startsWith("/apps") ) {
//            logger.info("Who is calling this: " + path, new Exception());
//        }
//        logger.info("* getResource({})", path);
        final Resource rsrc = delegatee.getResource(path);
        return rsrc == null ? null : new InternalResource(rsrc, this);
    }

    public String[] getSearchPath() {
        return delegatee.getSearchPath();
    }

    public Iterator<Resource> listChildren(Resource parent) {
//        logger.info("* listChildren({})", parent.getPath());
        return  new IteratorDecorator(delegatee.listChildren(parent), this);
    }

    public String map(HttpServletRequest request, String resourcePath) {
        return delegatee.map(request, resourcePath);
    }

    public String map(String resourcePath) {
        return delegatee.map(resourcePath);
    }

    public Iterator<Map<String, Object>> queryResources(String query,
            String language) {
        return delegatee.queryResources(query, language);
    }

    public Resource resolve(HttpServletRequest request, String absPath) {
        final Resource rsrc = delegatee.resolve(request, absPath);
        return rsrc == null ? null : new InternalResource(rsrc, this);
    }

    public Resource resolve(HttpServletRequest request) {
        final Resource rsrc = delegatee.resolve(request);
        return rsrc == null ? null : new InternalResource(rsrc, this);
    }

    public Resource resolve(String path) {
//        logger.info("* resolve({})", path);
        final Resource rsrc = delegatee.resolve(path);
        return rsrc == null ? null : new InternalResource(rsrc, this);
    }

    public static final class InternalResource extends ResourceWrapper {

        private final ResourceResolver resolver;

        private final Map<String, Object> adaptToCache = new HashMap<String, Object>();

        public InternalResource(Resource resource, final ResourceResolver resolver) {
            super(resource);
            this.resolver = resolver;
        }

        @Override
        public ResourceResolver getResourceResolver() {
            return this.resolver;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
            final String key = type.getName();
            Object obj = adaptToCache.get(key);
            if ( obj == null ) {
                obj = super.adaptTo(type);
                if ( obj != null ) {
                    adaptToCache.put(key, obj);
                }
            }
            return (AdapterType) obj;
        }

    }

    public static final class IteratorDecorator implements Iterator<Resource> {

        private final Iterator<Resource> delegatee;
        private final ResourceResolver resolver;

        public IteratorDecorator(final Iterator<Resource> delegatee, final ResourceResolver resolver) {
            this.delegatee = delegatee;
            this.resolver = resolver;
        }

        public boolean hasNext() {
            return this.delegatee.hasNext();
        }

        public Resource next() {
            final Resource rsrc = delegatee.next();
            return rsrc == null ? null : new InternalResource(rsrc, resolver);
        }

        public void remove() {
            delegatee.remove();
        }

    }
}
