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
package org.apache.sling.servlet.resolver.mock;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

public class MockResourceResolver implements ResourceResolver {

    private String[] searchPath;

    private HashMap<String, Resource> resources = new HashMap<String, Resource>();

    public void addResource(String path, Resource resource) {
        this.resources.put(path, resource);
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
        throw new UnsupportedOperationException("Not implemented");

    }

    public String[] getSearchPath() {
        // noinspection ReturnOfCollectionOrArrayField
        return searchPath;

    }

    public Iterator<Resource> listChildren(Resource parent) {
        return new Iterator<Resource>() {

            public boolean hasNext() {
                return false;

            }

            public Resource next() {
                return null;
            }

            public void remove() {
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

    public void setSearchPath(String[] searchPath) {
        this.searchPath = searchPath;
    }
}
