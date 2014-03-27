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
package org.apache.sling.resourceresolver.impl;

import static org.junit.Assert.assertEquals;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.resourceresolver.impl.helper.ResourceResolverContext;
import org.apache.sling.resourceresolver.impl.tree.RootResourceProviderEntry;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

/** Test ResourceResolverImpl.mangleNamespaces methods */
public class ResourceResolverMangleNamespacesTest {
    private ResourceResolverImpl rr;

    @Mock
    private Session mockedSession;

    private Session activeSession;

    public static final String NS_PREFIX = "testNS";
    public static final String NS_URL = "http://example.com/namespaces/testNS";

    @Before
    public void setup() throws RepositoryException {
        MockitoAnnotations.initMocks(this);
        activeSession = mockedSession;

        // Setup a ResourceResolverImpl with namespace mangling and unmangling
        final ResourceResolverFactoryActivator act = new ResourceResolverFactoryActivator() {
            @Override
            public boolean isMangleNamespacePrefixes() {
                return true;
            }
        };

        Mockito.when(mockedSession.getNamespacePrefix(NS_PREFIX)).thenReturn(NS_URL);

        final RootResourceProviderEntry rrpe = new RootResourceProviderEntry() {
            @Override
            @SuppressWarnings("unchecked")
            public <AdapterType> AdapterType adaptTo(ResourceResolverContext ctx, Class<AdapterType> type) {
                if(type == Session.class) {
                    return (AdapterType)activeSession;
                }
                return super.adaptTo(ctx, type);
            }
        };

        final CommonResourceResolverFactoryImpl fac = new CommonResourceResolverFactoryImpl(act) {
            @Override
            public RootResourceProviderEntry getRootProviderEntry() {
                return rrpe;
            }
        };

        rr = new ResourceResolverImpl(fac, new ResourceResolverContext(false, null, new ResourceAccessSecurityTracker()));
    }

    @Test
    public void testUrlWithPath() {
        assertEquals("http://example.com/some/path", rr.map("http://example.com/some/path"));
    }

    @Test
    public void testMangleHttp() {
        assertEquals("http://example.com/path/_with_colon", rr.map("http://example.com/path/with:colon"));
    }

    @Test
    public void testUnmangleHttp() {
        final Resource r = rr.resolve(null, "http://example.com/path/_with_mangling");
        assertEquals("/http://example.com/path/with:mangling", r.getPath());
    }

    @Test
    public void testUnmangleNoSession() {
        activeSession = null;
        final Resource r = rr.resolve(null, "http://example.com/path/_with_mangling");
        assertEquals("/http://example.com/path/_with_mangling", r.getPath());
    }

    @Test
    public void testManglePath() {
        assertEquals("/example.com/path/_with_colon", rr.map("/example.com/path/with:colon"));
    }

    @Test
    public void testUnmanglePath() {
        final Resource r = rr.resolve(null, "/example.com/path/_with_mangling");
        assertEquals("/example.com/path/with:mangling", r.getPath());
    }

    @Test
    public void testUrlNoPath() {
        assertEquals("http://withSlash.com/", rr.map("http://withSlash.com/"));
        assertEquals("http://noSlash.com", rr.map("http://noSlash.com"));
        assertEquals("http://nosuffix", rr.map("http://nosuffix"));
    }

    @Test
    public void testWeirdCases() {
        assertEquals("http://foo", rr.map("http://foo"));
        assertEquals("http://", rr.map("http://"));
        assertEquals("http:/", rr.map("http:/"));
        assertEquals("http:", rr.map("http:"));
        assertEquals("http", rr.map("http"));

        assertEquals("gopher://foo", rr.map("gopher://foo"));
        assertEquals("gopher://", rr.map("gopher://"));
        assertEquals("gopher:/", rr.map("gopher:/"));
        assertEquals("gopher:", rr.map("gopher:"));
        assertEquals("gopher", rr.map("gopher"));
    }
}
