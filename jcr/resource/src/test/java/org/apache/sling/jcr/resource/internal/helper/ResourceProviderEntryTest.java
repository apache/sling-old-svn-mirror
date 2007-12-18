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

import junit.framework.TestCase;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.jcr.resource.internal.JcrResourceResolver;
import org.apache.sling.jcr.resource.internal.helper.ResourceProvider;
import org.apache.sling.jcr.resource.internal.helper.ResourceProviderEntry;

public class ResourceProviderEntryTest extends TestCase {

    private ResourceProvider rootProvider;

    private ResourceProviderEntry root;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        rootProvider = new TestResourceProvider("/");
        root = new ResourceProviderEntry("/", rootProvider);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testRootProvider() {
        assertNull(root.getResourceProvider("relpath"));
        assertEquals(rootProvider, root.getResourceProvider("/"));
        assertEquals(rootProvider, root.getResourceProvider("/rootel"));
        assertEquals(rootProvider, root.getResourceProvider("/rootel/child"));
        assertEquals(rootProvider,
            root.getResourceProvider("/apps/sling/sample/html.js"));
        assertEquals(rootProvider,
            root.getResourceProvider("/apps/sling/microsling/html.js"));
    }

    public void testAdd1Provider() {
        String firstPath = "/rootel";
        ResourceProvider first = new TestResourceProvider(firstPath);
        root.addResourceProvider(first);

        assertEquals(rootProvider, root.getResourceProvider("/"));
        assertEquals(first, root.getResourceProvider("/rootel"));
        assertEquals(first, root.getResourceProvider("/rootel/html.js"));
        assertEquals(first, root.getResourceProvider("/rootel/child"));
        assertEquals(first, root.getResourceProvider("/rootel/child/html.js"));
        assertEquals(rootProvider,
            root.getResourceProvider("/apps/sling/sample/html.js"));
        assertEquals(rootProvider,
            root.getResourceProvider("/apps/sling/microsling/html.js"));
    }

    public void testAdd3Providers() {
        String firstPath = "/rootel";
        String thirdPath = "/apps/sling/sample";
        String secondPath = firstPath + "/child";

        ResourceProvider first = new TestResourceProvider(firstPath);
        ResourceProvider second = new TestResourceProvider(secondPath);
        ResourceProvider third = new TestResourceProvider(thirdPath);

        root.addResourceProvider(first);
        root.addResourceProvider(second);
        root.addResourceProvider(third);

        assertEquals(rootProvider, root.getResourceProvider("/"));
        assertEquals(first, root.getResourceProvider("/rootel"));
        assertEquals(first, root.getResourceProvider("/rootel/html.js"));
        assertEquals(second, root.getResourceProvider("/rootel/child"));
        assertEquals(second, root.getResourceProvider("/rootel/child/html.js"));
        assertEquals(third,
            root.getResourceProvider("/apps/sling/sample/html.js"));
        assertEquals(rootProvider,
            root.getResourceProvider("/apps/sling/microsling/html.js"));
    }

    public void testAdd3ProvidersReverse() {
        String firstPath = "/rootel";
        String thirdPath = "/apps/sling/sample";
        String secondPath = firstPath + "/child";

        ResourceProvider first = new TestResourceProvider(firstPath);
        ResourceProvider second = new TestResourceProvider(secondPath);
        ResourceProvider third = new TestResourceProvider(thirdPath);

        root.addResourceProvider(third);
        root.addResourceProvider(second);
        root.addResourceProvider(first);

        assertEquals(rootProvider, root.getResourceProvider("/"));
        assertEquals(first, root.getResourceProvider("/rootel"));
        assertEquals(first, root.getResourceProvider("/rootel/html.js"));
        assertEquals(second, root.getResourceProvider("/rootel/child"));
        assertEquals(second, root.getResourceProvider("/rootel/child/html.js"));
        assertEquals(third,
            root.getResourceProvider("/apps/sling/sample/html.js"));
        assertEquals(rootProvider,
            root.getResourceProvider("/apps/sling/microsling/html.js"));
    }

    public void testRemoveProviders() {
        String firstPath = "/rootel";
        String thirdPath = "/apps/sling/sample";
        String secondPath = firstPath + "/child";

        ResourceProvider first = new TestResourceProvider(firstPath);
        ResourceProvider second = new TestResourceProvider(secondPath);
        ResourceProvider third = new TestResourceProvider(thirdPath);

        root.addResourceProvider(first);
        root.addResourceProvider(second);
        root.addResourceProvider(third);

        assertEquals(rootProvider, root.getResourceProvider("/"));
        assertEquals(first, root.getResourceProvider("/rootel/html.js"));
        assertEquals(second, root.getResourceProvider("/rootel/child/html.js"));

        root.removeResourceProvider(first);

        assertEquals(rootProvider, root.getResourceProvider("/"));
        assertEquals(rootProvider, root.getResourceProvider("/rootel/html.js"));
        assertEquals(second, root.getResourceProvider("/rootel/child/html.js"));

        root.addResourceProvider(first);

        assertEquals(rootProvider, root.getResourceProvider("/"));
        assertEquals(first, root.getResourceProvider("/rootel/html.js"));
        assertEquals(second, root.getResourceProvider("/rootel/child/html.js"));
    }

    private static class TestResourceProvider implements ResourceProvider {

        private final String[] roots;

        TestResourceProvider(String root) {
            roots = new String[] { root };
        }

        public Resource getResource(JcrResourceResolver jcrResourceResolver, String path) {
            return null;
        }

        public String[] getRoots() {
            return roots;
        }

    }
}
