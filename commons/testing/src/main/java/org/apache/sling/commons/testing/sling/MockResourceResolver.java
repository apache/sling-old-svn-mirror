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
package org.apache.sling.commons.testing.sling;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

public class MockResourceResolver implements ResourceResolver {

    private String[] searchPath;

    private Map<String, Resource> resources = new LinkedHashMap<String, Resource>();

    private Map<String, Collection<Resource>> children = new LinkedHashMap<String, Collection<Resource>>();

    public void addResource(Resource resource) {
        this.resources.put(resource.getPath(), resource);
    }

    public void addChildren(Resource parent, Collection<Resource> children) {
        this.children.put(parent.getPath(), children);
    }

    public Resource resolve(HttpServletRequest request) {
        throw new UnsupportedOperationException("Not implemented");

    }

    public Resource resolve(String absPath) {
        throw new UnsupportedOperationException("Not implemented");

    }

    public String map(String resourcePath) {
        return null;

    }

    public Resource getResource(String path) {
        return resources.get(path);
    }

    public Resource getResource(Resource base, String path) {
        if (!path.startsWith("/")) {
            path = base.getPath() + "/" + path;
        }
        return getResource(path);
    }

    public String[] getSearchPath() {
        return searchPath.clone();

    }

    public Iterator<Resource> listChildren(final Resource parent) {
        Collection<Resource> childCollection = children.get(parent.getPath());
        if (childCollection != null) {
            return childCollection.iterator();
        }

        return new Iterator<Resource>() {
            final String parentPath = parent.getPath() + "/";

            final Iterator<Resource> elements = resources.values().iterator();

            Resource nextResource = seek();

            public boolean hasNext() {
                return nextResource != null;
            }

            public Resource next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                Resource result = nextResource;
                nextResource = seek();
                return result;
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }

            private Resource seek() {
                while (elements.hasNext()) {
                    Resource next = elements.next();
                    String path = next.getPath();
                    if (path.startsWith(parentPath)
                        && path.indexOf('/', parentPath.length()) < 0) {
                        return next;
                    }
                }
                return null;
            }
        };
    }

    public Iterator<Resource> findResources(String query, String language) {
        throw new UnsupportedOperationException("Not implemented");

    }

    public Iterator<Map<String, Object>> queryResources(String query,
            String language) {
        throw new UnsupportedOperationException("Not implemented");

    }

    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        throw new UnsupportedOperationException("Not implemented");

    }

    public void setSearchPath(String... searchPath) {
        this.searchPath = new String[searchPath.length];
        for (int i=0; i < searchPath.length; i++) {
            String entry = searchPath[i];
            if (!entry.endsWith("/")) {
                entry = entry.concat("/");
            }
            this.searchPath[i] = entry; 
        }
    }
}
