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
package org.apache.sling.resourcemerger.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.testing.resourceresolver.MockResourceResolverFactory;
import org.junit.Before;
import org.junit.Test;

public class MergedResourceProviderTest {

    private ResourceResolver resolver;

    private MergedResourceProvider provider;

    @Before public void setup() throws Exception {
        final ResourceResolverFactory factory = new MockResourceResolverFactory();
        this.resolver = factory.getAdministrativeResourceResolver(null);
        final Resource root = this.resolver.getResource("/");
        final Resource apps = this.resolver.create(root, "apps", null);
        final Resource libs = this.resolver.create(root, "libs", null);

        final Resource appsA = this.resolver.create(apps, "a", null);
        final Resource libsA = this.resolver.create(libs, "a", null);

        this.resolver.create(appsA, "1", null);
        this.resolver.create(libsA, "1", null);
        this.resolver.create(appsA, "2", null);
        this.resolver.create(libsA, "2", null);
        this.resolver.create(appsA, "X", null);
        this.resolver.create(libsA, "Y", null);

        this.resolver.commit();

        this.provider = new MergedResourceProvider("/merged");
    }

    @Test public void testListChildren() {
        final Resource rsrcA = this.provider.getResource(this.resolver, "/merged/a");
        assertNotNull(rsrcA);
        final Iterator<Resource> i = this.provider.listChildren(rsrcA);
        assertNotNull(i);
        final List<String> names = new ArrayList<String>();
        while ( i.hasNext() ) {
            names.add(i.next().getName());
        }
        assertEquals(4, names.size());
        assertTrue(names.contains("1"));
        assertTrue(names.contains("2"));
        assertTrue(names.contains("Y"));
        assertTrue(names.contains("X"));
    }
}
