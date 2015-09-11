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

import static org.apache.sling.resourceresolver.impl.MockedResourceResolverImplTest.createRPHandler;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.resourceresolver.impl.ResourceAccessSecurityTracker;
import org.apache.sling.resourceresolver.impl.helper.ResourceResolverContext;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderHandler;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.junit.Test;
import org.mockito.Mockito;

public class ProviderHandlerTest {

    @Test public void testServletRegistrationAndSyntheticResources() {
        final String servletpath = "/libs/a/b/GET.servlet";
        final Map<String, String> emptyParams = Collections.emptyMap();

        final Resource servletResource = Mockito.mock(Resource.class);
        
        final RootResourceProviderEntry root = new RootResourceProviderEntry();
        final ResourceProvider leaveProvider = Mockito.mock(ResourceProvider.class);
        Mockito.when(leaveProvider.getResource(Mockito.any(ResolveContext.class), Mockito.eq(servletpath), Mockito.any(Resource.class))).thenReturn(servletResource);
        ResourceProviderHandler h = createRPHandler(leaveProvider, "my-pid", 0, servletpath);
        
        final ResourceResolverContext ctx = getResourceResolverContext(Arrays.asList(h));
        final Resource parent = root.getResource(ctx, null, ResourceUtil.getParent(servletpath), emptyParams, false);
        assertNotNull("Parent must be available", parent);
        assertTrue("Resource should be synthetic", ResourceUtil.isSyntheticResource(parent));

        final Resource servlet = root.getResource(ctx, null, servletpath, emptyParams,false);
        assertNotNull("Servlet resource must not be null", servlet);
        assertEquals(servletResource, servlet);

        assertNotNull(root.getResource(ctx, null, "/libs", emptyParams, false));

        // now check when doing a resolve()
        assertNull(root.getResource(ctx, null, "/libs", emptyParams, true));
        assertNull(root.getResource(ctx, null, ResourceUtil.getParent(servletpath), emptyParams, true));
        assertNotNull(root.getResource(ctx, null, servletpath, emptyParams, true));
    }

    private ResourceResolverContext getResourceResolverContext(List<ResourceProviderHandler> providers) {
        final ResourceResolverContext ctx = Mockito.mock(ResourceResolverContext.class);
        Mockito.when(ctx.getResourceAccessSecurityTracker()).thenReturn(new ResourceAccessSecurityTracker());
        Mockito.when(ctx.getProviders()).thenReturn(providers);
        return ctx;
    }
}
