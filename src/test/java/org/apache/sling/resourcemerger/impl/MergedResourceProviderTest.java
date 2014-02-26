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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.testing.resourceresolver.MockHelper;
import org.apache.sling.testing.resourceresolver.MockResourceResolverFactory;
import org.apache.sling.testing.resourceresolver.MockResourceResolverFactoryOptions;
import org.junit.Before;
import org.junit.Test;

public class MergedResourceProviderTest {

    private ResourceResolver resolver;

    private MergedResourceProvider provider;

    @Before public void setup() throws Exception {
        final MockResourceResolverFactoryOptions options = new MockResourceResolverFactoryOptions();
        options.setSearchPaths(new String[] {"/apps/", "/libs/"});
        final ResourceResolverFactory factory = new MockResourceResolverFactory(options);
        this.resolver = factory.getAdministrativeResourceResolver(null);
        MockHelper.create(this.resolver).resource("/apps")
                                          .resource("a").p(MergedResourceConstants.PN_HIDE_CHILDREN, new String[] {"Z", "x", "y"})
                                            .resource("1").p("a", "1").p("b", "2")
                                            .resource(".2").p(ResourceResolver.PROPERTY_RESOURCE_TYPE, "apps")
                                            .resource(".3").p("e", "2")
                                                           .p(MergedResourceConstants.PN_HIDE_PROPERTIES, "*")
                                                           .p("b", "x")
                                                           .p("d", "1")
                                            .resource(".4").p("e", "2")
                                                           .p(MergedResourceConstants.PN_HIDE_PROPERTIES, new String[] {"a", "c"})
                                                           .p("b", "x")
                                                           .p("d", "1")
                                            .resource(".X")
                                        .resource("/libs")
                                          .resource("a")
                                            .resource("1").p("a", "5").p("c", "2")
                                            .resource(".2").p(ResourceResolver.PROPERTY_RESOURCE_TYPE, "libs")
                                            .resource(".3").p("a", "1").p("b", "2").p("c", "3")
                                            .resource(".4").p("a", "1").p("b", "2").p("c", "3")
                                            .resource(".Y")
                                            .resource(".Z")
                                        .commit();

        this.provider = new MergedResourceProvider("/merged");
    }

    @Test public void testHideChildren() {
        // check preconditions in libs and apps
        assertNotNull(this.resolver.getResource("/libs/a/Z"));
        assertNull(this.resolver.getResource("/libs/a/X"));
        assertNotNull(this.resolver.getResource("/libs/a/Y"));
        assertNull(this.resolver.getResource("/libs/a/x"));
        assertNull(this.resolver.getResource("/libs/a/y"));
        assertNull(this.resolver.getResource("/apps/a/Z"));
        assertNotNull(this.resolver.getResource("/apps/a/X"));
        assertNull(this.resolver.getResource("/apps/a/Y"));
        assertNull(this.resolver.getResource("/apps/a/x"));
        assertNull(this.resolver.getResource("/apps/a/y"));

        // now do the real checks
        assertNull(this.provider.getResource(this.resolver, "/merged/a/Z"));
        assertNotNull(this.provider.getResource(this.resolver, "/merged/a/Y"));
        assertNotNull(this.provider.getResource(this.resolver, "/merged/a/X"));
        assertNull(this.provider.getResource(this.resolver, "/merged/a/x"));
        assertNull(this.provider.getResource(this.resolver, "/merged/a/y"));
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
        assertEquals(6, names.size());
        assertTrue(names.contains("1"));
        assertTrue(names.contains("2"));
        assertTrue(names.contains("3"));
        assertTrue(names.contains("4"));
        assertTrue(names.contains("Y"));
        assertTrue(names.contains("X"));
    }

    @Test public void testProperties() {
        final Resource rsrcA1 = this.provider.getResource(this.resolver, "/merged/a/1");
        final ValueMap vm = rsrcA1.adaptTo(ValueMap.class);
        assertNotNull(vm);
        assertEquals(3, vm.size());
        assertEquals("1", vm.get("a"));
        assertEquals("2", vm.get("b"));
        assertEquals("2", vm.get("c"));
    }

    @Test public void testResourceType() {
        // a/2 defines the property and it's overlayed
        final Resource rsrcA2 = this.provider.getResource(this.resolver, "/merged/a/2");
        assertEquals("apps", rsrcA2.getResourceType());

        // a/12 doesn't define the property and it's overlayed
        final Resource rsrcA1 = this.provider.getResource(this.resolver, "/merged/a/1");
        assertEquals("a/1", rsrcA1.getResourceType());

    }

    @Test public void testClearProperties() {
        final Resource rsrcA3 = this.provider.getResource(this.resolver, "/merged/a/3");
        final ValueMap vm = rsrcA3.adaptTo(ValueMap.class);
        assertNotNull(vm);
        assertEquals(0, vm.size());
    }

    @Test public void testHideProperties() {
        final Resource rsrcA4 = this.provider.getResource(this.resolver, "/merged/a/4");
        final ValueMap vm = rsrcA4.adaptTo(ValueMap.class);
        assertNotNull(vm);
        assertEquals(3, vm.size());
        assertEquals("1", vm.get("d"));
        assertEquals("2", vm.get("e"));
        assertEquals("x", vm.get("b"));
    }

    @Test public void testSimpleCreateAndDelete() throws PersistenceException {
        final String path = "/merged/a/new";
        try {
            final Resource rsrc = this.provider.create(this.resolver, path, Collections.singletonMap("foo", (Object)"bla"));
            assertNotNull(rsrc);
            assertEquals(path, rsrc.getPath());
            final ValueMap vm = ResourceUtil.getValueMap(rsrc);
            assertEquals("bla", vm.get("foo"));

            final Resource realResource = this.resolver.getResource("/apps/a/new");
            assertNotNull(realResource);
            final ValueMap vmReal = ResourceUtil.getValueMap(realResource);
            assertEquals("bla", vmReal.get("foo"));
            assertNull(this.resolver.getResource("/libs/a/new"));

            this.provider.delete(this.resolver, path);
            assertNull(this.provider.getResource(this.resolver, path));
            assertNull(this.resolver.getResource("/libs/a/new"));
            assertNull(this.resolver.getResource("/apps/a/new"));

        } finally {
            this.resolver.revert();
        }
    }
}
