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

import static org.apache.sling.resourceresolver.impl.MockedResourceResolverImplTest.createRPHandler;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderHandler;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderStorage;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class ProviderHandlerTest {

    @SuppressWarnings("unchecked")
    @Test public void testServletRegistrationAndSyntheticResources() throws LoginException {
        final String servletpath = "/libs/a/b/GET.servlet";
        final Resource servletResource = Mockito.mock(Resource.class);
        Mockito.when(servletResource.getResourceMetadata()).then(new Answer<ResourceMetadata>() {
            @Override
            public ResourceMetadata answer(InvocationOnMock invocation) throws Throwable {
                return new ResourceMetadata();
            }
        });

        final ResourceProvider<?> leaveProvider = Mockito.mock(ResourceProvider.class);
        Mockito.when(leaveProvider.getResource(Mockito.any(ResolveContext.class), Mockito.eq(servletpath), Mockito.any(ResourceContext.class), Mockito.any(Resource.class))).thenReturn(servletResource);
        ResourceProviderHandler h = createRPHandler(leaveProvider, "my-pid", 0, servletpath);
        ResourceResolverFactoryActivator activator = new ResourceResolverFactoryActivator();
        activator.resourceAccessSecurityTracker = new ResourceAccessSecurityTracker();
        ResourceResolver resolver = new ResourceResolverImpl(new CommonResourceResolverFactoryImpl(activator), false, null, new ResourceProviderStorage(Arrays.asList(h)));

        final Resource parent = resolver.getResource(ResourceUtil.getParent(servletpath));
        assertNotNull("Parent must be available", parent);
        assertTrue("Resource should be synthetic", ResourceUtil.isSyntheticResource(parent));

        final Resource servlet = resolver.getResource(servletpath);
        assertNotNull("Servlet resource must not be null", servlet);
        assertEquals(servletResource, servlet);

        assertNotNull(resolver.getResource("/libs"));

        // now check when doing a resolve()
        assertTrue(resolver.resolve("/libs") instanceof NonExistingResource);
        assertTrue(resolver.resolve(ResourceUtil.getParent(servletpath)) instanceof NonExistingResource);
        assertNotNull(resolver.resolve(servletpath));
        resolver.close();
    }
}
