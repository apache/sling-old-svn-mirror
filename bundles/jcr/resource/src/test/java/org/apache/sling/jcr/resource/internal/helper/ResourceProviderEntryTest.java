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
package org.apache.sling.jcr.resource.internal.helper;

import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import junit.framework.TestCase;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;

public class ResourceProviderEntryTest extends TestCase {

    private ResourceProvider rootProvider;

    private ResourceProviderEntry root;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        rootProvider = new TestResourceProvider("/");
        root = new ResourceProviderEntry("/", rootProvider, null);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testRootProvider() {
        assertNull(root.getResource(null, "relpath"));
        assertEquals(root, root.getResource(null, "/"));
        assertEquals(root, root.getResource(null, "/rootel"));
        assertEquals(root, root.getResource(null, "/rootel/child"));
        assertEquals(root, root.getResource(null, "/apps/sling/sample/html.js"));
        assertEquals(root, root.getResource(null,
            "/apps/sling/microsling/html.js"));
    }

    public void testAdd1Provider() {
        String firstPath = "/rootel";
        ResourceProvider first = new TestResourceProvider(firstPath);
        root.addResourceProvider(firstPath, first);

        assertEquals(root, root.getResource(null, "/"));
        assertEquals(first, root.getResource(null, "/rootel"));
        assertEquals(first, root.getResource(null, "/rootel/html.js"));
        assertEquals(first, root.getResource(null, "/rootel/child"));
        assertEquals(first, root.getResource(null, "/rootel/child/html.js"));
        assertEquals(rootProvider, root.getResource(null,
            "/apps/sling/sample/html.js"));
        assertEquals(rootProvider, root.getResource(null,
            "/apps/sling/microsling/html.js"));
    }

    public void testAdd3Providers() {
        String firstPath = "/rootel";
        String thirdPath = "/apps/sling/sample";
        String secondPath = firstPath + "/child";

        ResourceProvider first = new TestResourceProvider(firstPath);
        ResourceProvider second = new TestResourceProvider(secondPath);
        ResourceProvider third = new TestResourceProvider(thirdPath);

        root.addResourceProvider(firstPath, first);
        root.addResourceProvider(secondPath, second);
        root.addResourceProvider(thirdPath, third);

        assertEquals(rootProvider, root.getResource(null, "/"));
        assertEquals(first, root.getResource(null, "/rootel"));
        assertEquals(first, root.getResource(null, "/rootel/html.js"));
        assertEquals(second, root.getResource(null, "/rootel/child"));
        assertEquals(second, root.getResource(null, "/rootel/child/html.js"));
        assertEquals(third,
            root.getResource(null, "/apps/sling/sample/html.js"));
        assertEquals(rootProvider, root.getResource(null,
            "/apps/sling/microsling/html.js"));
    }

    public void testAdd3ProvidersReverse() {
        String firstPath = "/rootel";
        String thirdPath = "/apps/sling/sample";
        String secondPath = firstPath + "/child";

        ResourceProvider first = new TestResourceProvider(firstPath);
        ResourceProvider second = new TestResourceProvider(secondPath);
        ResourceProvider third = new TestResourceProvider(thirdPath);

        root.addResourceProvider(thirdPath, third);
        root.addResourceProvider(secondPath, second);
        root.addResourceProvider(firstPath, first);

        assertEquals(rootProvider, root.getResource(null, "/"));
        assertEquals(first, root.getResource(null, "/rootel"));
        assertEquals(first, root.getResource(null, "/rootel/html.js"));
        assertEquals(second, root.getResource(null, "/rootel/child"));
        assertEquals(second, root.getResource(null, "/rootel/child/html.js"));
        assertEquals(third,
            root.getResource(null, "/apps/sling/sample/html.js"));
        assertEquals(rootProvider, root.getResource(null,
            "/apps/sling/microsling/html.js"));
    }

    public void testRemoveProviders() {
        String firstPath = "/rootel";
        String thirdPath = "/apps/sling/sample";
        String secondPath = firstPath + "/child";

        ResourceProvider first = new TestResourceProvider(firstPath);
        ResourceProvider second = new TestResourceProvider(secondPath);
        ResourceProvider third = new TestResourceProvider(thirdPath);

        root.addResourceProvider(firstPath, first);
        root.addResourceProvider(secondPath, second);
        root.addResourceProvider(thirdPath, third);

        assertEquals(rootProvider, root.getResource(null, "/"));
        assertEquals(first, root.getResource(null, "/rootel/html.js"));
        assertEquals(second, root.getResource(null, "/rootel/child/html.js"));

        root.removeResourceProvider(firstPath);

        assertEquals(rootProvider, root.getResource(null, "/"));
        assertEquals(rootProvider, root.getResource(null, "/rootel/html.js"));
        assertEquals(second, root.getResource(null, "/rootel/child/html.js"));

        root.addResourceProvider(firstPath, first);

        assertEquals(rootProvider, root.getResource(null, "/"));
        assertEquals(first, root.getResource(null, "/rootel/html.js"));
        assertEquals(second, root.getResource(null, "/rootel/child/html.js"));
    }

    protected void assertEquals(ResourceProvider resProvider, Resource res) {
        assertEquals(resProvider, res.getResourceResolver());
    }

    protected void assertEquals(ResourceProviderEntry resProviderEntry,
            Resource res) {
        assertEquals(resProviderEntry.getResourceProvider(),
            res.getResourceResolver());
    }

    // The test provider implements the ResourceResolver interface and sets
    // itself on the returned resource. This way the assertEquals methods above
    // may identify whether a resource has been returned from the expected
    // ResourceProvider
    private static class TestResourceProvider implements ResourceProvider, ResourceResolver {

        private final String[] roots;

        TestResourceProvider(String root) {
            roots = new String[] { root };
        }

        public Resource getResource(ResourceResolver resolver,
                HttpServletRequest request, String path) {
            return getResource(resolver, path);
        }

        public Resource getResource(ResourceResolver resolver, String path) {
            return new TestResource(path, this);
        }

        public String[] getRoots() {
            return roots;
        }

        public Iterator<Resource> listChildren(Resource parent) {
            return null;
        }

        // just dummy implementation to mark our resources for the tests
        public Iterator<Resource> findResources(String query, String language) {
            return null;
        }

        public Resource getResource(String path) {
            return null;
        }

        public Resource getResource(Resource base, String path) {
            return null;
        }

        public String[] getSearchPath() {
            return null;
        }

        public String map(HttpServletRequest request, String resourcePath) {
            return null;
        }
        
        public String map(String resourcePath) {
            return null;
        }

        public Iterator<Map<String, Object>> queryResources(String query,
                String language) {
            return null;
        }

        public Resource resolve(HttpServletRequest request, String absPath) {
            return null;
        }
        
        public Resource resolve(HttpServletRequest request) {
            return null;
        }

        public Resource resolve(String absPath) {
            return null;
        }

        public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
            return null;
        }
    }

    private static class TestResource implements Resource {

        private final String path;

        private final ResourceResolver resourceResolver;

        public TestResource(String path, ResourceResolver resourceResolver) {
            this.path = path;
            this.resourceResolver = resourceResolver;
        }

        public String getPath() {
            return path;
        }

        public ResourceMetadata getResourceMetadata() {
            return null;
        }

        public ResourceResolver getResourceResolver() {
            return resourceResolver;
        }

        public String getResourceType() {
            return null;
        }

        public String getResourceSuperType() {
            return null;
        }
        
        public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
            return null;
        }

    }
}
