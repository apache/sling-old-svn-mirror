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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.testing.resourceresolver.MockResource;
import org.junit.Test;

public class MergedResourceTest {

    @Test public void testResourceTypeByMethod() throws Exception {

        final MockResource r1 = new MockResource("/a", null, null) {

            @Override
            public String getResourceType() {
                return "a";
            }
        };
        final MockResource r2 = new MockResource("/b", null, null) {

            @Override
            public String getResourceType() {
                return "b";
            }
        };
        final MockResource r3 = new MockResource("/c", null, null) {

            @Override
            public String getResourceType() {
                return "c";
            }
        };

        final List<Resource> resources = new ArrayList<Resource>();
        resources.add(r1);
        resources.add(r2);
        resources.add(r3);

        final MergedResource mr = new MergedResource(null, "/a", "a", resources, Collections.EMPTY_LIST);

        assertEquals("c", mr.getResourceType());
        assertNull(mr.getResourceSuperType());
    }

    @Test public void testResourceTypeByValueMap() throws Exception {
        final ValueMap vm1 = new ValueMapDecorator(Collections.singletonMap(ResourceResolver.PROPERTY_RESOURCE_TYPE, (Object)"vma"));
        final MockResource r1 = new MockResource("/a", vm1, null);
        final ValueMap vm2 = new ValueMapDecorator(Collections.singletonMap(ResourceResolver.PROPERTY_RESOURCE_TYPE, (Object)"vmb"));
        final MockResource r2 = new MockResource("/b", vm2, null);
        final ValueMap vm3 = new ValueMapDecorator(Collections.singletonMap(ResourceResolver.PROPERTY_RESOURCE_TYPE, (Object)"vmc"));
        final MockResource r3 = new MockResource("/c", vm3, null);

        final List<Resource> resources = new ArrayList<Resource>();
        resources.add(r1);
        resources.add(r2);
        resources.add(r3);

        final List<ValueMap> valueMaps = new ArrayList<ValueMap>();
        valueMaps.add(vm1);
        valueMaps.add(vm2);
        valueMaps.add(vm3);

        final MergedResource mr = new MergedResource(null, "/a", "a", resources, valueMaps);

        assertEquals("vmc", mr.getResourceType());
        assertNull(mr.getResourceSuperType());
    }

    @Test public void testResourceTypeMixed() throws Exception {
        final ValueMap vm1 = new ValueMapDecorator(Collections.singletonMap(ResourceResolver.PROPERTY_RESOURCE_TYPE, (Object)"vma"));
        final MockResource r1 = new MockResource("/a", vm1, null) {

            @Override
            public String getResourceType() {
                return "a";
            }
        };
        final ValueMap vm2 = new ValueMapDecorator(Collections.singletonMap(ResourceResolver.PROPERTY_RESOURCE_TYPE, (Object)"vmb"));
        final MockResource r2 = new MockResource("/b", vm2, null) {

            @Override
            public String getResourceType() {
                return "b";
            }
        };
        final ValueMap vm3 = new ValueMapDecorator(Collections.singletonMap(ResourceResolver.PROPERTY_RESOURCE_TYPE, (Object)"vmc"));
        final MockResource r3 = new MockResource("/c", vm3, null) {

            @Override
            public String getResourceType() {
                return "c";
            }
        };

        final List<Resource> resources = new ArrayList<Resource>();
        resources.add(r1);
        resources.add(r2);
        resources.add(r3);

        final List<ValueMap> valueMaps = new ArrayList<ValueMap>();
        valueMaps.add(vm1);
        valueMaps.add(vm2);
        valueMaps.add(vm3);

        final MergedResource mr1 = new MergedResource(null, "/a", "a", resources, valueMaps);

        assertEquals("c", mr1.getResourceType());
        assertEquals("vmc", mr1.getResourceSuperType());

        final MergedResource mr2 = new MergedResource(null, "/a", "a", resources, Collections.singletonList(vm2));

        assertEquals("c", mr2.getResourceType());
        assertEquals("vmb", mr2.getResourceSuperType());
    }
    
    @Test public void testToString() throws Exception {
        final ValueMap vm = new ValueMapDecorator(
                Collections.singletonMap(ResourceResolver.PROPERTY_RESOURCE_TYPE, (Object) "vm"));
        final Resource r = new MockResource("/innerResourcePath", vm, null) {

            @Override
            public String getResourceType() {
                return "innerResource";
            }
        };

        final Resource mr = new MergedResource(null, "/merged", "merged", Collections.singletonList(r),
                Collections.singletonList(vm));

        assertTrue(mr.toString().contains("/innerResourcePath"));
    }
}
