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
package org.apache.sling.tenant.internal;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;

import junit.framework.TestCase;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.tenant.Tenant;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;

public class TenantProviderImplTest {

    @Test
    public void testListTenantsWithoutTenantRoot() throws Exception {
        TenantProviderImpl provider = new TenantProviderImpl();
        final ResourceResolverFactory rrf = Mockito.mock(ResourceResolverFactory.class);
        final BundleContext context = Mockito.mock(BundleContext.class);
        final ResourceResolver rr = Mockito.mock(ResourceResolver.class);
        Mockito.when(rrf.getAdministrativeResourceResolver(
                Mockito.anyMapOf(String.class, Object.class))).thenReturn(rr);
        set(provider, "factory", rrf);
        provider.activate(context, new HashMap<String, Object>());
        Iterator<Tenant> tenants = provider.getTenants();
        TestCase.assertNotNull(tenants);
        TestCase.assertFalse(tenants.hasNext());
    }

    private static void set(Object o, String name, Object value) throws Exception {
        final Field f = o.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(o, value);
    }

}
