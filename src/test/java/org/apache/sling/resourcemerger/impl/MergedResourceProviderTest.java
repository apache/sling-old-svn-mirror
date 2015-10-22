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

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.resourcemerger.impl.picker.MergingResourcePicker;
import org.apache.sling.resourceresolver.impl.BasicResolveContext;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.testing.resourceresolver.MockHelper;
import org.apache.sling.testing.resourceresolver.MockResourceResolverFactory;
import org.apache.sling.testing.resourceresolver.MockResourceResolverFactoryOptions;
import org.junit.Before;
import org.junit.Test;

public class MergedResourceProviderTest {

    private ResourceResolver resolver;

    private CRUDMergingResourceProvider provider;
    private ResolveContext<Void> ctx;

    @Before public void setup() throws Exception {
        final MockResourceResolverFactoryOptions options = new MockResourceResolverFactoryOptions();
        options.setSearchPaths(new String[] {"/apps/", "/libs/"});
        final ResourceResolverFactory factory = new MockResourceResolverFactory(options);
        this.resolver = factory.getResourceResolver(null);
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
                                          .resource("/apps/b").resource("c").resource("d").resource("e").resource("f")
                                        .resource("/libs")
                                          .resource("deleteTest")
                                          .resource(".mvmTest").p("a", "1").p("b", "2")
                                          .resource(".a")
                                            .resource("1").p("a", "5").p("c", "2")
                                            .resource(".2").p(ResourceResolver.PROPERTY_RESOURCE_TYPE, "libs")
                                            .resource(".3").p("a", "1").p("b", "2").p("c", "3")
                                            .resource(".4").p("a", "1").p("b", "2").p("c", "3")
                                            .resource(".Y")
                                            .resource(".Z")
                                          .resource("/libs/a/Y/a")
                                          .resource("/libs/a/Y/b")
                                          .resource("/libs/a/Y/c")
                                          .resource("/libs/b").resource("c").resource("d").resource("e").resource("f")
                                        .commit();

        this.provider = new CRUDMergingResourceProvider("/merged", new MergingResourcePicker(), false);
        this.ctx = new BasicResolveContext(resolver, Collections.<String, String>emptyMap(), null);
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
        assertNull(this.provider.getResource(ctx, "/merged/a/Z", null));
        assertNotNull(this.provider.getResource(ctx, "/merged/a/Y", null));
        assertNotNull(this.provider.getResource(ctx, "/merged/a/X", null));
        assertNull(this.provider.getResource(ctx, "/merged/a/x", null));
        assertNull(this.provider.getResource(ctx, "/merged/a/y", null));
    }

    @Test public void testListChildren() {
        final Resource rsrcA = this.provider.getResource(ctx, "/merged/a", null);
        assertNotNull(rsrcA);
        final Iterator<Resource> i = this.provider.listChildren(ctx, rsrcA);
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

    @Test public void testListSubChildren() {
        final Resource rsrcY = this.provider.getResource(ctx, "/merged/a/Y", null);
        assertNotNull(rsrcY);
        final Iterator<Resource> i = this.provider.listChildren(ctx, rsrcY);
        assertNotNull(i);
        final List<String> names = new ArrayList<String>();
        while ( i.hasNext() ) {
            names.add(i.next().getName());
        }
        assertEquals(3, names.size());
        assertTrue(names.contains("a"));
        assertTrue(names.contains("b"));
        assertTrue(names.contains("c"));
    }

    @Test public void testProperties() {
        final Resource rsrcA1 = this.provider.getResource(ctx, "/merged/a/1", null);
        final ValueMap vm = rsrcA1.adaptTo(ValueMap.class);
        assertNotNull(vm);
        assertEquals(3, vm.size());
        assertEquals("1", vm.get("a"));
        assertEquals("2", vm.get("b"));
        assertEquals("2", vm.get("c"));
    }

    @Test public void testResourceType() {
        // a/2 defines the property and it's overlayed
        final Resource rsrcA2 = this.provider.getResource(ctx, "/merged/a/2", null);
        assertEquals("apps", rsrcA2.getResourceType());

        // a/12 doesn't define the property and it's overlayed
        final Resource rsrcA1 = this.provider.getResource(ctx, "/merged/a/1", null);
        assertEquals("a/1", rsrcA1.getResourceType());

    }

    @Test public void testClearProperties() {
        final Resource rsrcA3 = this.provider.getResource(ctx, "/merged/a/3", null);
        final ValueMap vm = rsrcA3.adaptTo(ValueMap.class);
        assertNotNull(vm);
        assertEquals(0, vm.size());
    }

    @Test public void testHideProperties() {
        final Resource rsrcA4 = this.provider.getResource(ctx, "/merged/a/4", null);
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
            final Resource rsrc = this.provider.create(ctx, path, Collections.singletonMap("foo", (Object)"bla"));
            assertNotNull(rsrc);
            assertEquals(path, rsrc.getPath());
            final ValueMap vm = ResourceUtil.getValueMap(rsrc);
            assertEquals("bla", vm.get("foo"));

            final Resource realResource = this.resolver.getResource("/apps/a/new");
            assertNotNull(realResource);
            final ValueMap vmReal = ResourceUtil.getValueMap(realResource);
            assertEquals("bla", vmReal.get("foo"));
            assertNull(this.resolver.getResource("/libs/a/new"));

            this.provider.delete(ctx, rsrc);
            assertNull(this.provider.getResource(ctx, path, null));
            assertNull(this.resolver.getResource("/libs/a/new"));
            assertNull(this.resolver.getResource("/apps/a/new"));

        } finally {
            this.resolver.revert();
        }
    }

    @Test public void testDeleteByHiding() throws PersistenceException {
        final String path = "/merged/deleteTest";
        try {
            assertNotNull(this.resolver.getResource("/libs/deleteTest"));
            assertNull(this.resolver.getResource("/apps/deleteTest"));

            final Resource rsrc = this.provider.getResource(ctx, path, null);
            assertNotNull(rsrc);
            assertEquals(path, rsrc.getPath());

            this.provider.delete(ctx, rsrc);

            assertNull(this.provider.getResource(ctx, path, null));
            assertNotNull(this.resolver.getResource("/libs/deleteTest"));
            final Resource hidingRsrc = this.resolver.getResource("/apps/deleteTest");
            assertNotNull(hidingRsrc);
            final ValueMap vm = hidingRsrc.getValueMap();
            assertEquals(Boolean.TRUE, vm.get(MergedResourceConstants.PN_HIDE_RESOURCE));

        } finally {
            this.resolver.revert();
        }
    }

    @Test public void testDeleteByHidingAndCreate() throws PersistenceException {
        final String path = "/merged/deleteTest";
        try {
            assertNotNull(this.resolver.getResource("/libs/deleteTest"));
            assertNull(this.resolver.getResource("/apps/deleteTest"));

            final Resource rsrc = this.provider.getResource(ctx, path, null);
            assertNotNull(rsrc);
            assertEquals(path, rsrc.getPath());

            this.provider.delete(ctx, rsrc);
            this.provider.create(ctx, path, Collections.singletonMap("foo", (Object)"bla"));

            assertNotNull(this.provider.getResource(ctx, path, null));
            assertNotNull(this.resolver.getResource("/libs/deleteTest"));
            final Resource hidingRsrc = this.resolver.getResource("/apps/deleteTest");
            assertNotNull(hidingRsrc);
            final ValueMap vm = hidingRsrc.getValueMap();
            assertEquals("bla", vm.get("foo"));

        } finally {
            this.resolver.revert();
        }
    }

    @Test public void testModifiableValueMap() throws PersistenceException {
        final String path = "/merged/mvmTest";
        try {
            assertNotNull(this.resolver.getResource("/libs/mvmTest"));
            assertNull(this.resolver.getResource("/apps/mvmTest"));

            final Resource rsrc = this.provider.getResource(ctx, path, null);
            assertNotNull(rsrc);
            final ValueMap beforeVM = rsrc.getValueMap();
            assertEquals("1", beforeVM.get("a"));
            assertEquals("2", beforeVM.get("b"));

            final ModifiableValueMap mvm = rsrc.adaptTo(ModifiableValueMap.class);
            assertNotNull(mvm);
            assertEquals("1", mvm.get("a"));
            assertEquals("2", mvm.get("b"));

            mvm.put("c", "3");
            mvm.remove("a");

            assertNotNull(this.resolver.getResource("/libs/mvmTest"));
            assertNotNull(this.resolver.getResource("/apps/mvmTest"));

            final Resource rsrc2 = this.provider.getResource(ctx, path, null);
            assertNotNull(rsrc2);
            final ValueMap afterVM = rsrc2.getValueMap();
            assertNull(afterVM.get("a"));
            assertEquals("2", afterVM.get("b"));
            assertEquals("3", afterVM.get("c"));

            final Resource rsrcL = this.resolver.getResource("/libs/mvmTest");
            assertEquals("1", rsrcL.getValueMap().get("a"));
            assertEquals("2", rsrcL.getValueMap().get("b"));
            assertNull(rsrcL.getValueMap().get("c"));

            final Resource rsrcA = this.resolver.getResource("/apps/mvmTest");
            assertNull(rsrcA.getValueMap().get("a"));
            assertNull(rsrcA.getValueMap().get("b"));
            assertEquals("3", rsrcA.getValueMap().get("c"));
            final String[] hidden = rsrcA.getValueMap().get(MergedResourceConstants.PN_HIDE_PROPERTIES, String[].class);
            assertNotNull(hidden);
            assertEquals(1, hidden.length);
            assertEquals("a", hidden[0]);

        } finally {
            this.resolver.revert();
        }

    }

    @Test public void testGetWithRelatedResource() {
        final String path = "/merged/b/c/d";
        String[] relatedPaths = new String[] {
                null, // no related resource
                "/merged/a", // not related
                "/merged/b", // parent of parent
                "/merged/b/c", // parent
                "/merged/b/c/d", // itself
                "/merged/b/c/d/e", // child
                "/merged/b/c/d/e/f" // deep child
        };
        for (String relatedPath : relatedPaths) {
            final Resource relatedResource;
            if (relatedPath != null) {
                relatedResource = provider.getResource(ctx, relatedPath, null);
                assertNotNull("Not found: " + relatedPath, relatedResource);
            } else {
                relatedResource = null;
            }
            Resource resource = provider.getResource(ctx, path, relatedResource);
            assertNotNull(resource);
            assertEquals(path, resource.getPath());
            assertTrue(resource instanceof MergedResource);

            MergedResource mergedResource = (MergedResource) resource;
            List<Resource> mappedResources = mergedResource.getMappedResources();
            assertEquals(2, mappedResources.size());
            assertEquals(mappedResources.get(0).getPath(), "/libs/b/c/d");
            assertEquals(mappedResources.get(1).getPath(), "/apps/b/c/d");
        }
    }

}
