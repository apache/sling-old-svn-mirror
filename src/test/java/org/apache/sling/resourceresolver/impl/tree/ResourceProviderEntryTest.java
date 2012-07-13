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
package org.apache.sling.resourceresolver.impl.tree;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.resource.AbstractResource;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Constants;

public class ResourceProviderEntryTest {

    private ResourceProvider rootProvider;

    private ResourceProviderEntry root;

    @Before public void setUp() throws Exception {
        rootProvider = new TestResourceProvider("/");
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(Constants.SERVICE_ID, (long)0);
        root = new ResourceProviderEntry("/", new ResourceProviderHandler[]{ new ResourceProviderHandler(rootProvider, props)});
    }

    @Test public void testRootProvider() {
        assertNull(root.getResource(null, null, "relpath"));
        assertEquals(root, root.getResource(null, null, "/"));
        assertEquals(root, root.getResource(null, null, "/rootel"));
        assertEquals(root, root.getResource(null, null, "/rootel/child"));
        assertEquals(root, root.getResource(null, null, "/apps/sling/sample/html.js"));
        assertEquals(root, root.getResource(null, null,
            "/apps/sling/microsling/html.js"));
    }

    @Test public void testAdd1Provider() {
        String firstPath = "/rootel";
        ResourceProvider first = new TestResourceProvider(firstPath);
        final Map<String, Object> firstProps = new HashMap<String, Object>();
        firstProps.put(Constants.SERVICE_ID, (long)1);
        root.addResourceProvider(firstPath, new ResourceProviderHandler(first, firstProps));


        assertEquals(root, root.getResource(null, null, "/"));
        assertEquals(first, root.getResource(null, null, "/rootel"));
        assertEquals(first, root.getResource(null, null, "/rootel/html.js"));
        assertEquals(first, root.getResource(null, null, "/rootel/child"));
        assertEquals(first, root.getResource(null, null, "/rootel/child/html.js"));
        assertEquals(rootProvider, root.getResource(null, null,
            "/apps/sling/sample/html.js"));
        assertEquals(rootProvider, root.getResource(null, null,
            "/apps/sling/microsling/html.js"));
    }

    @Test public void testAdd3Providers() {
        String firstPath = "/rootel";
        String thirdPath = "/apps/sling/sample";
        String secondPath = firstPath + "/child";

        ResourceProvider first = new TestResourceProvider(firstPath);
        ResourceProvider second = new TestResourceProvider(secondPath);
        ResourceProvider third = new TestResourceProvider(thirdPath);
        final Map<String, Object> firstProps = new HashMap<String, Object>();
        firstProps.put(Constants.SERVICE_ID, (long)1);
        final Map<String, Object> secondProps = new HashMap<String, Object>();
        secondProps.put(Constants.SERVICE_ID, (long)2);
        final Map<String, Object> thirdProps = new HashMap<String, Object>();
        thirdProps.put(Constants.SERVICE_ID, (long)3);

        root.addResourceProvider(firstPath, new ResourceProviderHandler(first, firstProps));
        root.addResourceProvider(secondPath, new ResourceProviderHandler(second, secondProps));
        root.addResourceProvider(thirdPath, new ResourceProviderHandler(third, thirdProps));


        assertEquals(rootProvider, root.getResource(null, null, "/"));
        assertEquals(first, root.getResource(null, null, "/rootel"));
        assertEquals(first, root.getResource(null, null, "/rootel/html.js"));
        assertEquals(second, root.getResource(null, null, "/rootel/child"));
        assertEquals(second, root.getResource(null, null, "/rootel/child/html.js"));
        assertEquals(third,
            root.getResource(null, null, "/apps/sling/sample/html.js"));
        Resource resource = root.getResource(null, null,
            "/apps/sling/microsling/html.js");
            assertEquals(rootProvider, resource);
    }

    @Test public void testAdd3ProvidersReverse() {
        String firstPath = "/rootel";
        String thirdPath = "/apps/sling/sample";
        String secondPath = firstPath + "/child";

        ResourceProvider first = new TestResourceProvider(firstPath);
        ResourceProvider second = new TestResourceProvider(secondPath);
        ResourceProvider third = new TestResourceProvider(thirdPath);
        final Map<String, Object> firstProps = new HashMap<String, Object>();
        firstProps.put(Constants.SERVICE_ID, (long)1);
        final Map<String, Object> secondProps = new HashMap<String, Object>();
        secondProps.put(Constants.SERVICE_ID, (long)2);
        final Map<String, Object> thirdProps = new HashMap<String, Object>();
        thirdProps.put(Constants.SERVICE_ID, (long)3);

        root.addResourceProvider(firstPath, new ResourceProviderHandler(first, firstProps));
        root.addResourceProvider(secondPath, new ResourceProviderHandler(second, secondProps));
        root.addResourceProvider(thirdPath, new ResourceProviderHandler(third, thirdProps));

        assertEquals(rootProvider, root.getResource(null, null, "/"));
        assertEquals(first, root.getResource(null, null, "/rootel"));
        assertEquals(first, root.getResource(null, null, "/rootel/html.js"));
        assertEquals(second, root.getResource(null, null, "/rootel/child"));
        assertEquals(second, root.getResource(null, null, "/rootel/child/html.js"));
        assertEquals(third,
           root.getResource(null, null, "/apps/sling/sample/html.js"));
        Resource resource = root.getResource(null, null,
              "/apps/sling/microsling/html.js");
        assertEquals(rootProvider, resource);
    }

    @Test public void testRemoveProviders() {
        String firstPath = "/rootel";
        String thirdPath = "/apps/sling/sample";
        String secondPath = firstPath + "/child";

        ResourceProvider first = new TestResourceProvider(firstPath);
        ResourceProvider second = new TestResourceProvider(secondPath);
        ResourceProvider third = new TestResourceProvider(thirdPath);
        final Map<String, Object> firstProps = new HashMap<String, Object>();
        firstProps.put(Constants.SERVICE_ID, (long)1);
        final Map<String, Object> secondProps = new HashMap<String, Object>();
        secondProps.put(Constants.SERVICE_ID, (long)2);
        final Map<String, Object> thirdProps = new HashMap<String, Object>();
        thirdProps.put(Constants.SERVICE_ID, (long)3);

        root.addResourceProvider(firstPath, new ResourceProviderHandler(first, firstProps));
        root.addResourceProvider(secondPath, new ResourceProviderHandler(second, secondProps));
        root.addResourceProvider(thirdPath, new ResourceProviderHandler(third, thirdProps));

        assertEquals(rootProvider, root.getResource(null, null, "/"));
        assertEquals(first, root.getResource(null, null, "/rootel/html.js"));
        assertEquals(second, root.getResource(null, null, "/rootel/child/html.js"));

        root.removeResourceProvider(firstPath, new ResourceProviderHandler(first, firstProps));

        assertEquals(rootProvider, root.getResource(null, null, "/"));
        assertEquals(rootProvider, root.getResource(null, null, "/rootel/sddsf/sdfsdf/html.js"));
        assertEquals(rootProvider, root.getResource(null, null, "/rootel/html.js"));
        assertEquals(second, root.getResource(null, null, "/rootel/child/html.js"));

        root.addResourceProvider(firstPath, new ResourceProviderHandler(first, firstProps));

        assertEquals(rootProvider, root.getResource(null, null, "/"));
        assertEquals(first, root.getResource(null, null, "/rootel/html.js"));
        assertEquals(second, root.getResource(null, null, "/rootel/child/html.js"));
    }

    protected void assertEquals(ResourceProvider resProvider, Resource res) {
        org.junit.Assert.assertEquals(resProvider, res.getResourceResolver());
    }

    protected void assertEquals(ResourceProviderEntry resProviderEntry,
            Resource res) {
        ProviderHandler[] resourceProviders = resProviderEntry.getResourceProviders();
        for ( ProviderHandler rp : resourceProviders ) {
            if ( rp.equals(res.getResourceResolver())) {
                return;
            }
        }
        fail();
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

        public ResourceResolver clone(Map<String, Object> authenticationInfo) {
            throw new UnsupportedOperationException("copy");
        }

        public Resource getResource(ResourceResolver resolver,
                HttpServletRequest request, String path) {
            return getResource(resolver, path);
        }

        public Resource getResource(ResourceResolver resolver, String path) {
            return new TestResource(path, this);
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

        /**
         * {@inheritDoc}
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return Arrays.toString(roots);
        }

        public boolean isLive() {
            return true;
        }

        public void close() {
            // nothing to do
        }

        public String getUserID() {
            return null;
        }

        public Object getAttribute(String name) {
            return null;
        }

        public Iterator<String> getAttributeNames() {
            return Collections.<String> emptyList().iterator();
        }

        public void delete(Resource resource) {
            // TODO Auto-generated method stub
        }

        public Resource addChild(Resource parent, String name, ValueMap properties) {
            // TODO Auto-generated method stub
            return null;
        }

        public void update(Resource resource, ModifiableValueMap properties) {
            // TODO Auto-generated method stub
        }

        public void revert() {
            // TODO Auto-generated method stub

        }

        public void commit() {
            // TODO Auto-generated method stub

        }

        public boolean hasChanges() {
            // TODO Auto-generated method stub
            return false;
        }
    }

    private static class TestResource extends AbstractResource {

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
