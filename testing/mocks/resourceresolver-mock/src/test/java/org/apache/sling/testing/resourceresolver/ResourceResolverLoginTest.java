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
package org.apache.sling.testing.resourceresolver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Map;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

/**
 * Test different variants of login for resource resolver.
 */
public class ResourceResolverLoginTest {

    private static final Map<String,Object> AUTH_INFO = ImmutableMap.<String, Object>of(
                ResourceResolverFactory.USER, "myUser");
    
    private MockResourceResolverFactory factory;
    
    @Before
    public void setUp() {
        factory = new MockResourceResolverFactory();
    }

    @Test
    public void testGetResourceResolverWithoutAuthInfo() throws LoginException {
        ResourceResolver resolver = factory.getResourceResolver(null);
        assertNotNull(resolver);
        assertNull(resolver.getAttribute(ResourceResolverFactory.USER));
    }

    @Test
    public void testGetResourceResolverWithAuthInfo() throws LoginException {
        ResourceResolver resolver = factory.getResourceResolver(AUTH_INFO);
        assertNotNull(resolver);
        assertEquals("myUser", resolver.getAttribute(ResourceResolverFactory.USER));
    }

    @Test
    public void testGetAdministrativeResourceResolverWithoutAuthInfo() throws LoginException {
        ResourceResolver resolver = factory.getAdministrativeResourceResolver(null);
        assertNotNull(resolver);
        assertNull(resolver.getAttribute(ResourceResolverFactory.USER));
    }

    @Test
    public void testGetAdminstrativeResourceResolverWithAuthInfo() throws LoginException {
        ResourceResolver resolver = factory.getAdministrativeResourceResolver(AUTH_INFO);
        assertNotNull(resolver);
        assertNull(resolver.getAttribute(ResourceResolverFactory.USER));
    }

    @Test
    public void testGetServiceResourceResolverWithoutAuthInfo() throws LoginException {
        ResourceResolver resolver = factory.getServiceResourceResolver(null);
        assertNotNull(resolver);
        assertNull(resolver.getAttribute(ResourceResolverFactory.USER));
    }

    @Test
    public void testGetServiceResourceResolverWithAuthInfo() throws LoginException {
        ResourceResolver resolver = factory.getServiceResourceResolver(AUTH_INFO);
        assertNotNull(resolver);
        assertNull(resolver.getAttribute(ResourceResolverFactory.USER));
    }

}
