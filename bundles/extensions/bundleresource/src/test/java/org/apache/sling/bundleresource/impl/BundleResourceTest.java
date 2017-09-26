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
package org.apache.sling.bundleresource.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.bundleresource.impl.url.ResourceURLStreamHandler;
import org.apache.sling.bundleresource.impl.url.ResourceURLStreamHandlerFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;

public class BundleResourceTest {

    BundleResourceCache getBundleResourceCache() {
        Bundle bundle = mock(Bundle.class);
        when(bundle.getLastModified()).thenReturn(System.currentTimeMillis());

        BundleResourceCache cache = mock(BundleResourceCache.class);
        when(cache.getBundle()).thenReturn(bundle);

        return cache;
    }

    @Before
    public void setup() {
        ResourceURLStreamHandlerFactory.init();
    }

    @After
    public void finish() {
        ResourceURLStreamHandler.reset();
    }

    void addContent(BundleResourceCache cache, String path, Map<String, Object> content) throws IOException {
        final URL url = new URL("resource:" + path);

        ResourceURLStreamHandler.addJSON(path, content);
        when(cache.getEntry(path)).thenReturn(url);
    }

    @Test public void testFileResource() throws MalformedURLException {
        final BundleResourceCache cache = getBundleResourceCache();
        when(cache.getEntry("/libs/foo/test.json")).thenReturn(new URL("file:/libs/foo/test.json"));
        final BundleResource rsrc = new BundleResource(null, cache,
                new PathMapping("/libs/foo", null, null), "/libs/foo/test.json", null, false);
        assertEquals(JcrConstants.NT_FILE, rsrc.getResourceType());
        assertNull(rsrc.getResourceSuperType());
        final ValueMap vm = rsrc.getValueMap();
        assertEquals(JcrConstants.NT_FILE, vm.get(ResourceResolver.PROPERTY_RESOURCE_TYPE, String.class));
    }

    @Test public void testJSONResource() throws IOException {
        final BundleResourceCache cache = getBundleResourceCache();
        addContent(cache, "/libs/foo/test.json", Collections.singletonMap("test", (Object)"foo"));
        final BundleResource rsrc = new BundleResource(null, cache,
                new PathMapping("/libs/foo", null, "json"), "/libs/foo/test", null, false);
        assertEquals(JcrConstants.NT_FILE, rsrc.getResourceType());
        assertNull(rsrc.getResourceSuperType());
        final ValueMap vm = rsrc.getValueMap();
        assertEquals(JcrConstants.NT_FILE, vm.get(ResourceResolver.PROPERTY_RESOURCE_TYPE, String.class));
        assertEquals("foo", vm.get("test", String.class));
    }
}
