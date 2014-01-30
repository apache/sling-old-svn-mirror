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
package org.apache.sling.resourcemerger.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.testing.resourceresolver.MockResourceResolverFactory;
import org.junit.Before;
import org.junit.Test;

public class MergedResourceProviderTest {

    private ResourceResolver resolver;

    private MergedResourceProvider provider;

    @Before public void setup() throws Exception {
        final ResourceResolverFactory factory = new MockResourceResolverFactory();
        this.resolver = new ResourceResolverWrapper(factory.getAdministrativeResourceResolver(null));
        final Resource root = this.resolver.getResource("/");
        final Resource apps = this.resolver.create(root, "apps", null);
        final Resource libs = this.resolver.create(root, "libs", null);

        final Resource appsA = this.resolver.create(apps, "a", null);
        final Resource libsA = this.resolver.create(libs, "a", null);

        this.resolver.create(appsA, "1", new map().p("a", "1").p("b", "2"));
        this.resolver.create(libsA, "1", new map().p("a", "5").p("c", "2"));
        this.resolver.create(appsA, "2", null);
        this.resolver.create(libsA, "2", null);
        this.resolver.create(appsA, "X", null);
        this.resolver.create(libsA, "Y", null);

        this.resolver.commit();

        this.provider = new MergedResourceProvider("/merged");
    }

    @Test public void testListChildren() {
        final Resource rsrcA = this.provider.getResource(this.resolver, "/merged/a");
        assertNotNull(rsrcA);
        final Iterator<Resource> i = this.provider.listChildren(rsrcA);
        assertNotNull(i);
        final List<String> names = new ArrayList<String>();
        while ( i.hasNext() ) {
            names.add(i.next().getName());
        }
        assertEquals(4, names.size());
        assertTrue(names.contains("1"));
        assertTrue(names.contains("2"));
        assertTrue(names.contains("Y"));
        assertTrue(names.contains("X"));
    }

    @Test public void testProperties() {
        final Resource rsrcA1 = this.provider.getResource(this.resolver, "/merged/a/1");
        final ValueMap vm = rsrcA1.adaptTo(ValueMap.class);
        assertNotNull(vm);
        assertEquals(3, vm.size());
        assertEquals("1", vm.get("a"));
        assertEquals("2", vm.get("b"));
        assertEquals("2", vm.get("c"));
    }

    protected static final class map extends HashMap<String, Object> {

        private static final long serialVersionUID = 1L;

        public map p(final String name, final String value) {
            this.put(name, value);
            return this;
        }
    }

    /**
     *  We have to use a wrapper until Sling testing resource resolver mock 0.2.0 is out
     *  Fixing SLING-3354
     */
    public static class ResourceResolverWrapper implements ResourceResolver {

        private final ResourceResolver resolver;

        public ResourceResolverWrapper(final ResourceResolver r) {
            this.resolver = r;
        }

        public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
            return resolver.adaptTo(type);
        }

        public Resource resolve(HttpServletRequest request, String absPath) {
            return resolver.resolve(request, absPath);
        }

        public Resource resolve(String absPath) {
            return resolver.resolve(absPath);
        }

        public Resource resolve(HttpServletRequest request) {
            return resolver.resolve(request);
        }

        public String map(String resourcePath) {
            return resolver.map(resourcePath);
        }

        public String map(HttpServletRequest request, String resourcePath) {
            return resolver.map(request, resourcePath);
        }

        public Resource getResource(String path) {
            return resolver.getResource(path);
        }

        public Resource getResource(Resource base, String path) {
            return resolver.getResource(base, path);
        }

        public String[] getSearchPath() {
            return new String[] {"/apps", "/libs"};
        }

        public Iterator<Resource> listChildren(Resource parent) {
            return resolver.listChildren(parent);
        }

        public Iterable<Resource> getChildren(Resource parent) {
            return resolver.getChildren(parent);
        }

        public Iterator<Resource> findResources(String query, String language) {
            return resolver.findResources(query, language);
        }

        public Iterator<Map<String, Object>> queryResources(String query,
                String language) {
            return resolver.queryResources(query, language);
        }

        public boolean hasChildren(Resource resource) {
            return resolver.hasChildren(resource);
        }

        public ResourceResolver clone(Map<String, Object> authenticationInfo)
                throws LoginException {
            return resolver.clone(authenticationInfo);
        }

        public boolean isLive() {
            return resolver.isLive();
        }

        public void close() {
            resolver.close();
        }

        public String getUserID() {
            return resolver.getUserID();
        }

        public Iterator<String> getAttributeNames() {
            return resolver.getAttributeNames();
        }

        public Object getAttribute(String name) {
            return resolver.getAttribute(name);
        }

        public void delete(Resource resource) throws PersistenceException {
            resolver.delete(resource);
        }

        public Resource create(Resource parent, String name,
                Map<String, Object> properties) throws PersistenceException {
            return resolver.create(parent, name, properties);
        }

        public void revert() {
            resolver.revert();
        }

        public void commit() throws PersistenceException {
            resolver.commit();
        }

        public boolean hasChanges() {
            return resolver.hasChanges();
        }

        public String getParentResourceType(Resource resource) {
            return resolver.getParentResourceType(resource);
        }

        public String getParentResourceType(String resourceType) {
            return resolver.getParentResourceType(resourceType);
        }

        public boolean isResourceType(Resource resource, String resourceType) {
            return resolver.isResourceType(resource, resourceType);
        }

        public void refresh() {
            resolver.refresh();
        }

    }
}
