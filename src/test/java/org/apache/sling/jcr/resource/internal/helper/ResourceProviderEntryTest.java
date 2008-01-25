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

import javax.servlet.http.HttpServletRequest;

import junit.framework.TestCase;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;

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
        assertNull(root.getResourceProvider("relpath"));
        assertEquals(root, root.getResourceProvider("/"));
        assertEquals(root, root.getResourceProvider("/rootel"));
        assertEquals(root, root.getResourceProvider("/rootel/child"));
        assertEquals(root,
            root.getResourceProvider("/apps/sling/sample/html.js"));
        assertEquals(root,
            root.getResourceProvider("/apps/sling/microsling/html.js"));
    }

    public void testAdd1Provider() {
        String firstPath = "/rootel";
        ResourceProvider first = new TestResourceProvider(firstPath);
        root.addResourceProvider(firstPath, first);

        assertEquals(root, root.getResourceProvider("/"));
        assertEquals(first, root.getResourceProvider("/rootel").getResourceProvider());
        assertEquals(first, root.getResourceProvider("/rootel/html.js").getResourceProvider());
        assertEquals(first, root.getResourceProvider("/rootel/child").getResourceProvider());
        assertEquals(first, root.getResourceProvider("/rootel/child/html.js").getResourceProvider());
        assertEquals(rootProvider,
            root.getResourceProvider("/apps/sling/sample/html.js").getResourceProvider());
        assertEquals(rootProvider,
            root.getResourceProvider("/apps/sling/microsling/html.js").getResourceProvider());
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

        assertEquals(rootProvider, root.getResourceProvider("/").getResourceProvider());
        assertEquals(first, root.getResourceProvider("/rootel").getResourceProvider());
        assertEquals(first, root.getResourceProvider("/rootel/html.js").getResourceProvider());
        assertEquals(second, root.getResourceProvider("/rootel/child").getResourceProvider());
        assertEquals(second, root.getResourceProvider("/rootel/child/html.js").getResourceProvider());
        assertEquals(third,
            root.getResourceProvider("/apps/sling/sample/html.js").getResourceProvider());
        assertEquals(rootProvider,
            root.getResourceProvider("/apps/sling/microsling/html.js").getResourceProvider());
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

        assertEquals(rootProvider, root.getResourceProvider("/").getResourceProvider());
        assertEquals(first, root.getResourceProvider("/rootel").getResourceProvider());
        assertEquals(first, root.getResourceProvider("/rootel/html.js").getResourceProvider());
        assertEquals(second, root.getResourceProvider("/rootel/child").getResourceProvider());
        assertEquals(second, root.getResourceProvider("/rootel/child/html.js").getResourceProvider());
        assertEquals(third,
            root.getResourceProvider("/apps/sling/sample/html.js").getResourceProvider());
        assertEquals(rootProvider,
            root.getResourceProvider("/apps/sling/microsling/html.js").getResourceProvider());
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

        assertEquals(rootProvider, root.getResourceProvider("/").getResourceProvider());
        assertEquals(first, root.getResourceProvider("/rootel/html.js").getResourceProvider());
        assertEquals(second, root.getResourceProvider("/rootel/child/html.js").getResourceProvider());

        root.removeResourceProvider(firstPath);

        assertEquals(rootProvider, root.getResourceProvider("/").getResourceProvider());
        assertEquals(rootProvider, root.getResourceProvider("/rootel/html.js").getResourceProvider());
        assertEquals(second, root.getResourceProvider("/rootel/child/html.js").getResourceProvider());

        root.addResourceProvider(firstPath, first);

        assertEquals(rootProvider, root.getResourceProvider("/").getResourceProvider());
        assertEquals(first, root.getResourceProvider("/rootel/html.js").getResourceProvider());
        assertEquals(second, root.getResourceProvider("/rootel/child/html.js").getResourceProvider());
    }

    private static class TestResourceProvider implements ResourceProvider {

        private final String[] roots;

        TestResourceProvider(String root) {
            roots = new String[] { root };
        }

        public Resource getResource(HttpServletRequest request, String path) {
            return null;
        }

        public Resource getResource(String path) {
            return null;
        }

        public String[] getRoots() {
            return roots;
        }

        public Iterator<Resource> listChildren(Resource parent) {
            return null;
        }
    }
}
