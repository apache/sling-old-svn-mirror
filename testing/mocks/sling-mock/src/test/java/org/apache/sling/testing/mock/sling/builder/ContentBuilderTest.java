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
package org.apache.sling.testing.mock.sling.builder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class ContentBuilderTest {

    @Rule
    public SlingContext context = new SlingContext(ResourceResolverType.RESOURCERESOLVER_MOCK);

    @Test
    public void testResource() {
        Resource resource = context.create().resource("/content/test1/resource1");
        assertNotNull(resource);
        assertEquals("resource1", resource.getName());
        assertTrue(ResourceUtil.getValueMap(resource).isEmpty());
    }

    @Test
    public void testResourceWithProperties() {
        Resource resource = context.create().resource(
                "/content/test1/resource2",
                ImmutableMap.<String, Object> builder().put("jcr:title", "Test Title").put("stringProp", "value1")
                        .build());
        assertNotNull(resource);
        assertEquals("resource2", resource.getName());
        ValueMap props = ResourceUtil.getValueMap(resource);
        assertEquals("Test Title", props.get("jcr:title", String.class));
        assertEquals("value1", props.get("stringProp", String.class));
    }

}
