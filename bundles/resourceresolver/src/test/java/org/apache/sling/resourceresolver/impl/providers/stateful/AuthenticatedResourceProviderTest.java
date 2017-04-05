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
package org.apache.sling.resourceresolver.impl.providers.stateful;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.security.AccessSecurityException;
import org.apache.sling.api.security.ResourceAccessSecurity;
import org.apache.sling.resourceresolver.impl.ResourceAccessSecurityTracker;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderHandler;
import org.apache.sling.spi.resource.provider.QueryLanguageProvider;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.junit.Before;
import org.junit.Test;

public class AuthenticatedResourceProviderTest {

    private AuthenticatedResourceProvider src;

    private ResourceAccessSecurity security;
    private ResourceResolver resourceResolver;
    private ResolveContext<Object> resolveContext;
    private ResourceProvider<Object> resourceProvider;
    private QueryLanguageProvider<Object> queryLanguageProvider;

    private boolean useRAS;

    @Before
    public void prepare() throws PersistenceException, AccessSecurityException {
        this.resourceResolver = mock(ResourceResolver.class);
        this.resolveContext = mock(ResolveContext.class);
        when(this.resolveContext.getResourceResolver()).thenReturn(this.resourceResolver);

        this.security = mock(ResourceAccessSecurity.class);

        this.queryLanguageProvider = mock(QueryLanguageProvider.class);

        this.resourceProvider = mock(ResourceProvider.class);
        when(resourceProvider.getQueryLanguageProvider()).thenReturn(this.queryLanguageProvider);

        final ResourceProviderHandler handler = mock(ResourceProviderHandler.class);
        when(handler.getResourceProvider()).thenReturn(this.resourceProvider);

        useRAS = false;

        final ResourceAccessSecurityTracker securityTracker = new ResourceAccessSecurityTracker() {
            @Override
            public ResourceAccessSecurity getApplicationResourceAccessSecurity() {
                if ( useRAS) {
                    return security;
                }
                return null;
            }
        };

        this.src = new AuthenticatedResourceProvider(handler, false, this.resolveContext, securityTracker);

    }

    @Test public void testBasics() throws Exception {
        assertEquals(this.resolveContext, this.src.getResolveContext());

        this.src.refresh();
        verify(this.resourceProvider).refresh(this.resolveContext);

        when(this.resourceProvider.isLive(this.resolveContext)).thenReturn(true);
        assertTrue(this.src.isLive());
        when(this.resourceProvider.isLive(this.resolveContext)).thenReturn(false);
        assertFalse(this.src.isLive());

        this.src.commit();
        verify(this.resourceProvider).commit(this.resolveContext);
    }

    @Test public void testGetParent() {
        final Resource child = mock(Resource.class);
        when(child.getPath()).thenReturn("/parent/child");
        final Resource parent = mock(Resource.class);
        when(parent.getPath()).thenReturn("/parent");
        when(this.resourceProvider.getParent(this.resolveContext, child)).thenReturn(parent);

        assertEquals("/parent", this.src.getParent(child).getPath());
    }
}
