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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.stream.JsonGenerator;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.bundleresource.impl.url.ResourceURLStreamHandler;
import org.apache.sling.bundleresource.impl.url.ResourceURLStreamHandlerFactory;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceContext;
import org.junit.After;
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

    void finishContent(Bundle bundle) {
        for(final Map.Entry<String, List<String>> entry : ResourceURLStreamHandler.getParentChildRelationship().entrySet()) {
            when(bundle.getEntryPaths(entry.getKey())).thenReturn(Collections.enumeration(entry.getValue()));
        }
    }

    void addContent(Bundle bundle, String path, String content) throws IOException {
        final URL url = new URL("resource:" + path);

        ResourceURLStreamHandler.addContents(path, content);
        when(bundle.getEntry(path)).thenReturn(url);
    }

    String getContent(final Resource rsrc) throws IOException {
        final InputStream is = rsrc.adaptTo(InputStream.class);
        if ( is == null ) {
            return null;
        }
        final byte[] buffer = new byte[20];
        final int l = is.read(buffer);
        return new String(buffer, 0, l, "UTF-8");
    }

    List<String> getChildren(final Iterator<Resource> i) {
        final List<String> list = new ArrayList<>();
        if ( i != null ) {
            while ( i.hasNext() ) {
                list.add(i.next().getPath());
            }
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    void assertContent(final BundleResourceProvider provider, final String path, final String content) throws IOException {
        final Resource rsrc = provider.getResource(mock(ResolveContext.class), path, mock(ResourceContext.class), null);
        assertNotNull(rsrc);
        assertEquals(content, getContent(rsrc));
    }

    @Before
    public void setup() {
        ResourceURLStreamHandlerFactory.init();
    }

    @After
    public void finish() {
        ResourceURLStreamHandler.reset();
    }

    @SuppressWarnings("unchecked")
    @Test public void testFileResource() throws IOException {
        final Bundle bundle = getBundle();
        addContent(bundle, "/libs/foo/test.json", "HELLOWORLD");

        final PathMapping path = new PathMapping("/libs/foo", null, null);

        final BundleResourceProvider provider = new BundleResourceProvider(new BundleResourceCache(bundle), path);
        assertNotNull(provider.getResource(mock(ResolveContext.class), "/libs/foo/test.json", mock(ResourceContext.class), null));
        assertNull(provider.getResource(mock(ResolveContext.class), "/libs/foo/test", mock(ResourceContext.class), null));
    }

    @SuppressWarnings("unchecked")
    @Test public void testJSONResource() throws IOException {
        final Bundle bundle = getBundle();
        addContent(bundle, "/libs/foo/test.json", Collections.singletonMap("test", (Object)"foo"));

        final PathMapping path = new PathMapping("/libs/foo", null, "json");

        final BundleResourceProvider provider = new BundleResourceProvider(new BundleResourceCache(bundle), path);
        assertNull(provider.getResource(mock(ResolveContext.class), "/libs/foo/test.json", mock(ResourceContext.class), null));
        final Resource rsrc = provider.getResource(mock(ResolveContext.class), "/libs/foo/test", mock(ResourceContext.class), null);
        assertNotNull(rsrc);
        assertNull(rsrc.adaptTo(InputStream.class));
        assertNull(rsrc.adaptTo(URL.class));
        assertNotNull(rsrc.getValueMap());
        assertEquals("foo", rsrc.getValueMap().get("test", String.class));
    }

    @SuppressWarnings("unchecked")
    @Test public void testFileAndJSONResource() throws IOException {
        final Bundle bundle = getBundle();
        addContent(bundle, "/libs/foo/test", "HELLOWORLD");
        addContent(bundle, "/libs/foo/test.json", Collections.singletonMap("test", (Object)"foo"));

        final PathMapping path = new PathMapping("/libs/foo", null, "json");

        final BundleResourceProvider provider = new BundleResourceProvider(new BundleResourceCache(bundle), path);
        assertNull(provider.getResource(mock(ResolveContext.class), "/libs/foo/test.json", mock(ResourceContext.class), null));
        final Resource rsrc = provider.getResource(mock(ResolveContext.class), "/libs/foo/test", mock(ResourceContext.class), null);
        assertNotNull(rsrc);
        assertNotNull(rsrc.adaptTo(InputStream.class));
        assertNotNull(rsrc.adaptTo(URL.class));
        assertNotNull(rsrc.getValueMap());
        assertEquals("foo", rsrc.getValueMap().get("test", String.class));
        assertEquals("HELLOWORLD", getContent(rsrc));
    }

    @Test public void testTreeWithoutDeepJSON() throws IOException {
        testTreeWithoutDeepJSON("");
        testTreeWithoutDeepJSON("/SLING-INF");
    }

    @SuppressWarnings("unchecked")
    private void testTreeWithoutDeepJSON(final String prefix) throws IOException {
        final Bundle bundle = getBundle();
        addContent(bundle, prefix + "/libs/foo/", "DIR");
        addContent(bundle, prefix + "/libs/foo/a", "A");
        addContent(bundle, prefix + "/libs/foo/b", "B");
        addContent(bundle, prefix + "/libs/foo/test", "test");
        addContent(bundle, prefix + "/libs/foo/test/x", "X");
        addContent(bundle, prefix + "/libs/foo/test/y", "Y");
        addContent(bundle, prefix + "/libs/foo/test/z.json", Collections.singletonMap(ResourceResolver.PROPERTY_RESOURCE_TYPE, (Object)"rtz"));
        addContent(bundle, prefix + "/libs/foo/test.json", Collections.singletonMap("test", (Object)"foo"));

        finishContent(bundle);

        final PathMapping path;
        if ( prefix.length() == 0 ) {
            path = new PathMapping("/libs/foo", null, "json");
        } else {
            path = new PathMapping("/libs/foo", prefix + "/libs/foo", "json");
        }

        final BundleResourceProvider provider = new BundleResourceProvider(new BundleResourceCache(bundle), path);

        assertContent(provider, "/libs/foo/a", "A");
        assertContent(provider, "/libs/foo/b", "B");
        assertContent(provider, "/libs/foo/test", "test");
        assertContent(provider, "/libs/foo/test/x", "X");
        assertContent(provider, "/libs/foo/test/y", "Y");
        assertContent(provider, "/libs/foo/test/z", null);

        Resource rsrc = provider.getResource(mock(ResolveContext.class), "/libs/foo", mock(ResourceContext.class), null);
        assertNotNull(rsrc);

        List<String> rsrcChildren = getChildren(rsrc.listChildren());
        assertEquals(3, rsrcChildren.size());
        assertTrue(rsrcChildren.contains("/libs/foo/a"));
        assertTrue(rsrcChildren.contains("/libs/foo/b"));
        assertTrue(rsrcChildren.contains("/libs/foo/test"));

        rsrcChildren = getChildren(provider.listChildren(mock(ResolveContext.class), rsrc));
        assertEquals(3, rsrcChildren.size());
        assertTrue(rsrcChildren.contains("/libs/foo/a"));
        assertTrue(rsrcChildren.contains("/libs/foo/b"));
        assertTrue(rsrcChildren.contains("/libs/foo/test"));

        rsrc = provider.getResource(mock(ResolveContext.class), "/libs/foo/test", mock(ResourceContext.class), null);
        assertNotNull(rsrc);

        rsrcChildren = getChildren(rsrc.listChildren());
        assertEquals(3, rsrcChildren.size());
        assertTrue(rsrcChildren.contains("/libs/foo/test/x"));
        assertTrue(rsrcChildren.contains("/libs/foo/test/y"));
        assertTrue(rsrcChildren.contains("/libs/foo/test/z"));

        rsrcChildren = getChildren(provider.listChildren(mock(ResolveContext.class), rsrc));
        assertEquals(3, rsrcChildren.size());
        assertTrue(rsrcChildren.contains("/libs/foo/test/x"));
        assertTrue(rsrcChildren.contains("/libs/foo/test/y"));
        assertTrue(rsrcChildren.contains("/libs/foo/test/z"));
    }

    @Test public void testTreeWithDeepJSON() throws IOException {
        testTreeWithDeepJSON("");
        testTreeWithDeepJSON("/SLING-INF");
    }

    @SuppressWarnings("unchecked")
    private void testTreeWithDeepJSON(final String prefix) throws IOException {
        // build JSON
        final StringWriter writer = new StringWriter();
        final JsonGenerator g = Json.createGenerator(writer);
        g.writeStartObject();
        g.write("level", "1");
        g.write("name", "d");
        g.writeStartObject("g");
        g.write("level", "2");
        g.write("name", "g");
        g.writeStartObject("g1");
        g.write("level", "3");
        g.write("name", "g1");
        g.writeEnd(); // g1
        g.writeStartObject("g2");
        g.write("level", "3");
        g.write("name", "g2");
        g.writeEnd(); // g2
        g.writeEnd(); // g
        g.writeStartObject("h");
        g.write("level", "2");
        g.write("name", "h");
        g.writeStartObject("h1");
        g.write("level", "3");
        g.write("name", "h1");
        g.writeEnd(); // h1
        g.writeStartObject("h2");
        g.write("level", "3");
        g.write("name", "h2");
        g.writeStartObject("h21");
        g.write("level", "4");
        g.write("name", "h21");
        g.writeEnd(); // h21
        g.writeEnd(); // h2
        g.writeEnd(); // h

        g.writeEnd(); // root
        g.close();

        final Bundle bundle = getBundle();
        addContent(bundle, prefix + "/libs/foo/", "DIR");
        addContent(bundle, prefix + "/libs/foo/a", "A");
        addContent(bundle, prefix + "/libs/foo/b", "B");
        addContent(bundle, prefix + "/libs/foo/d.json", writer.toString());
        addContent(bundle, prefix + "/libs/foo/test", "test");
        addContent(bundle, prefix + "/libs/foo/test/x", "X");
        addContent(bundle, prefix + "/libs/foo/test/y", "Y");
        addContent(bundle, prefix + "/libs/foo/test/z.json", Collections.singletonMap(ResourceResolver.PROPERTY_RESOURCE_TYPE, (Object)"rtz"));
        addContent(bundle, prefix + "/libs/foo/test.json", Collections.singletonMap("test", (Object)"foo"));

        finishContent(bundle);

        final PathMapping path;
        if ( prefix.length() == 0 ) {
            path = new PathMapping("/libs/foo", null, "json");
        } else {
            path = new PathMapping("/libs/foo", prefix + "/libs/foo", "json");
        }

        final BundleResourceProvider provider = new BundleResourceProvider(new BundleResourceCache(bundle), path);

        assertContent(provider, "/libs/foo/a", "A");
        assertContent(provider, "/libs/foo/b", "B");
        assertContent(provider, "/libs/foo/test", "test");
        assertContent(provider, "/libs/foo/test/x", "X");
        assertContent(provider, "/libs/foo/test/y", "Y");
        assertContent(provider, "/libs/foo/test/z", null);

        Resource rsrc = provider.getResource(mock(ResolveContext.class), "/libs/foo", mock(ResourceContext.class), null);
        assertNotNull(rsrc);

        List<String> rsrcChildren = getChildren(rsrc.listChildren());
        assertEquals(4, rsrcChildren.size());
        assertTrue(rsrcChildren.contains("/libs/foo/a"));
        assertTrue(rsrcChildren.contains("/libs/foo/b"));
        assertTrue(rsrcChildren.contains("/libs/foo/d"));
        assertTrue(rsrcChildren.contains("/libs/foo/test"));

        rsrcChildren = getChildren(provider.listChildren(mock(ResolveContext.class), rsrc));
        assertEquals(4, rsrcChildren.size());
        assertTrue(rsrcChildren.contains("/libs/foo/a"));
        assertTrue(rsrcChildren.contains("/libs/foo/b"));
        assertTrue(rsrcChildren.contains("/libs/foo/d"));
        assertTrue(rsrcChildren.contains("/libs/foo/test"));

        rsrc = provider.getResource(mock(ResolveContext.class), "/libs/foo/test", mock(ResourceContext.class), null);
        assertNotNull(rsrc);

        rsrcChildren = getChildren(rsrc.listChildren());
        assertEquals(3, rsrcChildren.size());
        assertTrue(rsrcChildren.contains("/libs/foo/test/x"));
        assertTrue(rsrcChildren.contains("/libs/foo/test/y"));
        assertTrue(rsrcChildren.contains("/libs/foo/test/z"));

        rsrcChildren = getChildren(provider.listChildren(mock(ResolveContext.class), rsrc));
        assertEquals(3, rsrcChildren.size());
        assertTrue(rsrcChildren.contains("/libs/foo/test/x"));
        assertTrue(rsrcChildren.contains("/libs/foo/test/y"));
        assertTrue(rsrcChildren.contains("/libs/foo/test/z"));

        // check children of d
        rsrc = provider.getResource(mock(ResolveContext.class), "/libs/foo/d", mock(ResourceContext.class), null);
        assertNotNull(rsrc);
        rsrcChildren = getChildren(rsrc.listChildren());
        assertEquals(2, rsrcChildren.size());
        assertTrue(rsrcChildren.contains("/libs/foo/d/g"));
        assertTrue(rsrcChildren.contains("/libs/foo/d/h"));

        rsrcChildren = getChildren(provider.listChildren(mock(ResolveContext.class), rsrc));
        assertEquals(2, rsrcChildren.size());
        assertTrue(rsrcChildren.contains("/libs/foo/d/g"));
        assertTrue(rsrcChildren.contains("/libs/foo/d/h"));

        // try to get g and h directly
        rsrc = provider.getResource(mock(ResolveContext.class), "/libs/foo/d/g", mock(ResourceContext.class), null);
        assertNotNull(rsrc);
        assertEquals("g", rsrc.getValueMap().get("name", String.class));

        rsrc = provider.getResource(mock(ResolveContext.class), "/libs/foo/d/h", mock(ResourceContext.class), null);
        assertNotNull(rsrc);
        assertEquals("h", rsrc.getValueMap().get("name", String.class));

        // try to get g1 and g2 directly
        rsrc = provider.getResource(mock(ResolveContext.class), "/libs/foo/d/g/g1", mock(ResourceContext.class), null);
        assertNotNull(rsrc);
        assertEquals("g1", rsrc.getValueMap().get("name", String.class));

        rsrc = provider.getResource(mock(ResolveContext.class), "/libs/foo/d/g/g2", mock(ResourceContext.class), null);
        assertNotNull(rsrc);
        assertEquals("g2", rsrc.getValueMap().get("name", String.class));
    }
}
