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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.resourcemerger.impl.picker.OverridingResourcePicker;
import org.apache.sling.testing.resourceresolver.MockHelper;
import org.apache.sling.testing.resourceresolver.MockResourceResolverFactory;
import org.apache.sling.testing.resourceresolver.MockResourceResolverFactoryOptions;
import org.junit.Before;
import org.junit.Test;

public class OverridingResourceProviderTest {

    private static final String SUPER_TYPE = "sling:resourceSuperType";

    private ResourceResolver resolver;

    private MergingResourceProvider provider;

    /*
     * Tree is:
     * /apps/a/1/a
     * /apps/a/1/b
     * /apps/a/1/b/1
     * /apps/a/1/d
     * /apps/a/1/d/1
     * /apps/a/1/d/1/a
     * /apps/a/1/d/1/b/1
     * /apps/a/1/c
     * /apps/a/2/c
     * /apps/a/3
     * 
     * /apps/a/2 has the super type of /apps/a/1
     * /apps/a/3 has the super type of /apps/a/2
     */
    @Before
    public void setup() throws Exception {
        final MockResourceResolverFactoryOptions options = new MockResourceResolverFactoryOptions();
        options.setSearchPaths(new String[] {"/apps", "/libs"});
        final ResourceResolverFactory factory = new MockResourceResolverFactory(options);
        this.resolver = factory.getAdministrativeResourceResolver(null);
        MockHelper.create(this.resolver)
                    .resource("/apps")
                    .resource("a")
                    .resource("1").p("a", "1").p("b", "1")
                    .resource("a").p("1", "a").p("2", "b")
                    .resource(".b").p("1", "a").p("2", "b")
                    .resource("1")
                    .resource("/apps/a/1/d").p("a", "1").p("b", "2")
                    .resource("1").p("1", "a").p("2", "b")
                    .resource("a")
                    .resource("/apps/a/1/d/1/b")
                    .resource("1")
                    .resource("/apps/a/1/c").p("1", "a").p("2", "b")
                    .resource("/apps/a/2").p(SUPER_TYPE, "a/1").p("b", "2").p(MergedResourceConstants.PN_HIDE_CHILDREN, new String[] {"b"})
                    .resource("c").p("1", "c")
                    .resource("/apps/a/3").p(SUPER_TYPE, "a/2")
                    .commit();

        this.provider = new MergingResourceProvider("/override", new OverridingResourcePicker(), true);
    }

    @Test
    public void testOverridingOnTarget() {
        final Resource rsrcA2 = this.provider.getResource(this.resolver, "/override/apps/a/2");
        final ValueMap vm = rsrcA2.adaptTo(ValueMap.class);
        assertNotNull(vm);
        assertEquals(3, vm.size()); //3rd is resource:superType
        assertEquals("1", vm.get("a"));
        assertEquals("2", vm.get("b"));
    }

    @Test
    public void testOverridingViaParent() {
        final Resource rsrcA2 = this.provider.getResource(this.resolver, "/override/apps/a/2/c");
        final ValueMap vm = rsrcA2.adaptTo(ValueMap.class);
        assertNotNull(vm);
        assertEquals(2, vm.size());
        assertEquals("c", vm.get("1"));
        assertEquals("b", vm.get("2"));
    }

    @Test
    public void testInheritingFromGrandParent() {
        assertNotNull(this.provider.getResource(this.resolver, "/override/apps/a/3/a"));
        assertNull(this.provider.getResource(this.resolver, "/override/apps/a/3/b"));
        assertNotNull(this.provider.getResource(this.resolver, "/override/apps/a/3/c"));
        assertNotNull(this.provider.getResource(this.resolver, "/override/apps/a/3/d"));
    }

    @Test
    public void testHideChildrenFromList() {
        final Resource rsrcA2 = this.provider.getResource(this.resolver, "/override/apps/a/2");
        final Iterator<Resource> children = this.provider.listChildren(rsrcA2);
        final List<String> names = new ArrayList<String>();
        while (children.hasNext()) {
            names.add(children.next().getName());
        }
        assertTrue(names.contains("a"));
        assertFalse(names.contains("b"));
        assertTrue(names.contains("c"));
    }

    @Test
    public void testHideChildrenFromGet() {
        assertNotNull(this.provider.getResource(this.resolver, "/override/apps/a/1/b/1"));
        assertNull(this.provider.getResource(this.resolver, "/override/apps/a/2/b"));
        assertNull(this.provider.getResource(this.resolver, "/override/apps/a/2/b/1"));
        assertNotNull(this.provider.getResource(this.resolver, "/override/apps/a/2/d/1/a"));
        assertNotNull(this.provider.getResource(this.resolver, "/override/apps/a/2/d/1/b"));
        assertNotNull(this.provider.getResource(this.resolver, "/override/apps/a/2/d/1/b/1"));
    }

    // doing it this way because the mock resource resolver doesn't
    // access the resource provider
    private Resource getChildResource(Resource parent, String name) {
        final Iterator<Resource> children = this.provider.listChildren(parent);
        while (children.hasNext()) {
            final Resource candidate = children.next();
            if (candidate.getName().equals(name)) {
               return candidate;
            }
        }
        return null;
    }

    @Test
    public void testOverriddenIncludesChildFromSuper() {
        final Resource rsrcA2 = this.provider.getResource(this.resolver, "/override/apps/a/2");

        Resource d = getChildResource(rsrcA2, "d");
        assertNotNull(d);

        Resource d1 = getChildResource(d, "1");
        assertNotNull(d1);

        Resource d1a = getChildResource(d1, "a");
        assertNotNull(d1a);
    }

}
