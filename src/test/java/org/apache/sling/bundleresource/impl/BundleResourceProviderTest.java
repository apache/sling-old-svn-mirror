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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

import org.apache.sling.bundleresource.impl.url.ResourceURLStreamHandler;
import org.apache.sling.bundleresource.impl.url.ResourceURLStreamHandlerFactory;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceContext;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;

public class BundleResourceProviderTest {

    Bundle getBundle() {
        Bundle bundle = mock(Bundle.class);
        when(bundle.getLastModified()).thenReturn(System.currentTimeMillis());

        return bundle;
    }


    void addContent(Bundle bundle, String path, Map<String, Object> content) throws IOException {
        final URL url = new URL("resource:" + path);

        ResourceURLStreamHandler.addJSON(path, content);
        when(bundle.getEntry(path)).thenReturn(url);
    }

    @Before
    public void setup() {
        ResourceURLStreamHandlerFactory.init();
    }

    @SuppressWarnings("unchecked")
    @Test public void testFileResource() throws IOException {
        final Bundle bundle = getBundle();
        addContent(bundle, "/libs/foo/test.json", Collections.singletonMap("test", (Object)"foo"));

        final MappedPath path = new MappedPath("/libs/foo", null, null);

        final BundleResourceProvider provider = new BundleResourceProvider(bundle, path);
        assertNotNull(provider.getResource(mock(ResolveContext.class), "/libs/foo/test.json", mock(ResourceContext.class), null));
        assertNull(provider.getResource(mock(ResolveContext.class), "/libs/foo/test", mock(ResourceContext.class), null));
    }

    @SuppressWarnings("unchecked")
    @Test public void testJSONResource() throws IOException {
        final Bundle bundle = getBundle();
        addContent(bundle, "/libs/foo/test.json", Collections.singletonMap("test", (Object)"foo"));

        final MappedPath path = new MappedPath("/libs/foo", null, "json");

        final BundleResourceProvider provider = new BundleResourceProvider(bundle, path);
        assertNull(provider.getResource(mock(ResolveContext.class), "/libs/foo/test.json", mock(ResourceContext.class), null));
        assertNotNull(provider.getResource(mock(ResolveContext.class), "/libs/foo/test", mock(ResourceContext.class), null));
    }
}
