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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.resourceresolver.impl.ResourceAccessSecurityTracker;
import org.apache.sling.resourceresolver.impl.helper.ResourceResolverContext;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.Constants;

public class ProviderHandlerTest {

    @Test public void testRoots() {
        // service id = 1, no roots
        final Map<String, Object> props1 = new HashMap<String, Object>();
        props1.put(Constants.SERVICE_ID, 1L);
        final ProviderHandler ph1 = new MyProviderHandler(props1);
        assertNull(ph1.getRoots());
        assertEquals(1, (long)ph1.getServiceId());

        // service id = 2, empty roots
        final Map<String, Object> props2 = new HashMap<String, Object>();
        props2.put(Constants.SERVICE_ID, 2L);
        props2.put(ResourceProvider.ROOTS, new String[0]);
        final ProviderHandler ph2 = new MyProviderHandler(props2);
        assertNull(ph2.getRoots());
        assertEquals(2, (long)ph2.getServiceId());

        // service id = 3, empty string
        final Map<String, Object> props3 = new HashMap<String, Object>();
        props3.put(Constants.SERVICE_ID, 3L);
        props3.put(ResourceProvider.ROOTS, new String[] {""});
        final ProviderHandler ph3 = new MyProviderHandler(props3);
        assertNull(ph3.getRoots());
        assertEquals(3, (long)ph3.getServiceId());

        // service id = 4, empty string and real string mixed
        final Map<String, Object> props4 = new HashMap<String, Object>();
        props4.put(Constants.SERVICE_ID, 4L);
        props4.put(ResourceProvider.ROOTS, new String[] {"", "/a", " ", "/a", "/b", "/c ", " /d ", ""});
        final ProviderHandler ph4 = new MyProviderHandler(props4);
        assertNotNull(ph4.getRoots());
        assertEquals(4, (long)ph4.getServiceId());
        assertEquals(new String[] {"/a", "/b", "/c", "/d"}, ph4.getRoots());

        // service id = 5, trailing slash string
        final Map<String, Object> props5 = new HashMap<String, Object>();
        props5.put(Constants.SERVICE_ID, 5L);
        props5.put(ResourceProvider.ROOTS, new String[] {"", " /", "/b/ ", " /c/", " /d/ ", ""});
        final ProviderHandler ph5 = new MyProviderHandler(props5);
        assertNotNull(ph5.getRoots());
        assertEquals(5, (long)ph5.getServiceId());
        assertEquals(new String[] {"/", "/b", "/c", "/d"}, ph5.getRoots());
    }

    @Test public void testServletRegistrationAndSyntheticResources() {
        final String servletpath = "/libs/a/b/GET.servlet";

        final Resource servletResource = Mockito.mock(Resource.class);

        final RootResourceProviderEntry root = new RootResourceProviderEntry();
        final Map<String, Object> leaveProperties = new HashMap<String, Object>();
        leaveProperties.put(ResourceProvider.ROOTS, servletpath);

        final ResourceProvider leaveProvider = Mockito.mock(ResourceProvider.class);
        Mockito.when(leaveProvider.getResource(null, servletpath)).thenReturn(servletResource);

        root.bindResourceProvider(leaveProvider, leaveProperties);

        final Resource parent = root.getResource(getResourceResolverContext(), null, ResourceUtil.getParent(servletpath), false);
        assertNotNull("Parent must be available", parent);
        assertTrue("Resource should be synthetic", ResourceUtil.isSyntheticResource(parent));

        final Resource servlet = root.getResource(getResourceResolverContext(), null, servletpath, false);
        assertNotNull("Servlet resource must not be null", servlet);
        assertEquals(servletResource, servlet);

        assertNotNull(root.getResource(getResourceResolverContext(), null, "/libs", false));

        // now check when doing a resolve()
        assertNull(root.getResource(getResourceResolverContext(), null, "/libs", true));
        assertNull(root.getResource(getResourceResolverContext(), null, ResourceUtil.getParent(servletpath), true));
        assertNotNull(root.getResource(getResourceResolverContext(), null, servletpath, true));
    }

    private ResourceResolverContext getResourceResolverContext() {
        final ResourceResolverContext ctx = Mockito.mock(ResourceResolverContext.class);
        Mockito.when(ctx.getResourceAccessSecurityTracker()).thenReturn(new ResourceAccessSecurityTracker());
        return ctx;
    }

    private static final class MyProviderHandler extends ProviderHandler {

        public MyProviderHandler(Map<String, Object> properties) {
            super(properties);
        }

        @Override
        public Resource getResource(ResourceResolverContext ctx, ResourceResolver resourceResolver, String path) {
            return null;
        }

        @Override
        public Iterator<Resource> listChildren(ResourceResolverContext ctx, Resource parent) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public ResourceProvider getResourceProvider(ResourceResolverContext ctx) {
            // TODO Auto-generated method stub
            return null;
        }

    }
}
