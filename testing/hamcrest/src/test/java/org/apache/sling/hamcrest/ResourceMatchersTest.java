/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.hamcrest;

import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class ResourceMatchersTest {

    @Rule
    public final SlingContext context = new SlingContext();

    @Test
    public void testResourceType() {
        context.build().resource("/resource", 
                ResourceResolver.PROPERTY_RESOURCE_TYPE, "some/type",
                "some other key", "some other value");
        
        Resource resource = context.resourceResolver().getResource("/resource");
        Assert.assertThat(resource, ResourceMatchers.resourceType("some/type"));
        Assert.assertThat(resource, Matchers.not(ResourceMatchers.resourceType("some/other/type")));
    }

    @Test
    public void testPath() {
        context.build().resource("/resource");
        
        Resource resource = context.resourceResolver().getResource("/resource");
        Assert.assertThat(resource, ResourceMatchers.path("/resource"));
        Assert.assertThat(resource, Matchers.not(ResourceMatchers.path("some/other/name")));
    }

    @Test
    public void testName() {
        context.build().resource("/resource");
        
        Resource resource = context.resourceResolver().getResource("/resource");
        Assert.assertThat(resource, ResourceMatchers.name("resource"));
        Assert.assertThat(resource, Matchers.not(ResourceMatchers.name("some/other/name")));
    }

    @Test
    public void testProps() {
        context.build().resource("/resource",
                "key1", "value1",
                "key2", 123,
                "key3", new String[] { "item1", "item2" },
                "key4", "otherValue");
        
        Map<String, Object> expectedProperties = ImmutableMap.<String, Object>builder()
                .put("key1", "value1")
                .put("key2", 123)
                .put("key3", new String[] { "item1", "item2" })
                .build();
        
        Resource resource = context.resourceResolver().getResource("/resource");
        Assert.assertThat(resource, ResourceMatchers.props(expectedProperties));
        
        // test existing key with not matching value
        expectedProperties = ImmutableMap.<String, Object>builder()
                .put("key1", "value1")
                .put("key3", "value3")
                .build();
        Assert.assertThat(resource, Matchers.not(ResourceMatchers.props(expectedProperties)));
        
        // test non-existing key
        expectedProperties = ImmutableMap.<String, Object>builder()
                .put("key5", "value5")
                .build();
        Assert.assertThat(resource, Matchers.not(ResourceMatchers.props(expectedProperties)));
    }

    @Test
    public void testPropsVarargs() {
        context.build().resource("/resource",
                "key1", "value1",
                "key2", true,
                "key3", new int[] { 1, 2, 3 },
                "key4", "otherValue");
        
        Object[] expectedProperties = new Object[] {
                "key1", "value1",
                "key2", true,
                "key3", new int[] { 1, 2, 3 }
        };
        
        Resource resource = context.resourceResolver().getResource("/resource");
        Assert.assertThat(resource, ResourceMatchers.props(expectedProperties));

        // test existing key with not matching value
        expectedProperties = new Object[] {
                "key1", "value1",
                "key3", new int[] { 1, 2, 5 }
        };
        Assert.assertThat(resource, Matchers.not(ResourceMatchers.props(expectedProperties)));

        expectedProperties = new Object[] {
                "key1", "value1",
                "key3", new int[] { 1, 2 }
        };
        Assert.assertThat(resource, Matchers.not(ResourceMatchers.props(expectedProperties)));
        
        expectedProperties = new Object[] {
                "key1", "value1",
                "key3", 1
        };
        Assert.assertThat(resource, Matchers.not(ResourceMatchers.props(expectedProperties)));
        
        expectedProperties = new Object[] {
                "key1", "value1",
                "key3", null
        };
        Assert.assertThat(resource, Matchers.not(ResourceMatchers.props(expectedProperties)));
        
        // test non-existing key
        expectedProperties = new Object[] {
                "key5", "value5"
        };
        Assert.assertThat(resource, Matchers.not(ResourceMatchers.props(expectedProperties)));
    }

    @Test
    public void testHasChildren() {
        context.build()
            .resource("/parent").resource("child1")
            .resource("/parent/child2");
        
        Resource resource = context.resourceResolver().getResource("/parent");
        Assert.assertThat(resource, ResourceMatchers.hasChildren("child1"));
    }
    
    @Test
    public void testNameAndProps() {
        context.build().resource("/resource",
                "key1", "value1",
                "key2", new String[] { "item1" },
                "key3", "value3");
        
        Map<String, Object> expectedProperties = ImmutableMap.<String, Object>builder()
                .put("key1", "value1")
                .put("key2", new String[] { "item1" })
                .build();
        
        Resource resource = context.resourceResolver().getResource("/resource");
        Assert.assertThat(resource, ResourceMatchers.nameAndProps("resource", expectedProperties));
        
        // test not matching name
        Assert.assertThat(resource, Matchers.not(ResourceMatchers.nameAndProps("resource1", expectedProperties)));
        
        // test existing key with not matching value
        expectedProperties = ImmutableMap.<String, Object>builder()
                .put("key1", "value1")
                .put("key2", "value3")
                .build();
        Assert.assertThat(resource, Matchers.not(ResourceMatchers.nameAndProps("resource", expectedProperties)));
    }

    @Test
    public void testNameAndPropsVarargs() {
        context.build().resource("/resource",
                "key1", "value1",
                "key2", "value2",
                "key3", "value3");
        
        Object[] expectedProperties = new Object[] {
                "key1", "value1",
                "key2", "value2"
        };
        
        Resource resource = context.resourceResolver().getResource("/resource");
        Assert.assertThat(resource, ResourceMatchers.nameAndProps("resource", expectedProperties));
        
        // test not matching name
        Assert.assertThat(resource, Matchers.not(ResourceMatchers.nameAndProps("resource1", expectedProperties)));
        
        // test existing key with not matching value
        expectedProperties = new Object[] {
                "key1", "value1",
                "key2", "value3"
        };
        Assert.assertThat(resource, Matchers.not(ResourceMatchers.nameAndProps("resource", expectedProperties)));
    }

    @Test
    public void testContainsChildrenInAnyOrder() {
        context.build()
            .resource("/parent").resource("child1")
            .resource("/parent/child2");
        
        Resource resource = context.resourceResolver().getResource("/parent");
        Assert.assertThat(resource, ResourceMatchers.containsChildrenInAnyOrder("child2", "child1"));
        Assert.assertThat(resource, ResourceMatchers.containsChildrenInAnyOrder("child1", "child2"));
        Assert.assertThat(resource, Matchers.not(ResourceMatchers.containsChildren("child2", "child3", "child1")));
    }

    @Test
    public void testContainsChildren() {
        context.build()
            .resource("/parent").resource("child1")
            .resource("/parent/child2");
        
        Resource resource = context.resourceResolver().getResource("/parent");
        Assert.assertThat(resource, ResourceMatchers.containsChildren("child1", "child2"));
        Assert.assertThat(resource, Matchers.not(ResourceMatchers.containsChildren("child2", "child1")));
        Assert.assertThat(resource, Matchers.not(ResourceMatchers.containsChildren("child1", "child2", "child3")));
    }

}
