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
    public void testOfType() {
        context.build().resource("/resource", 
                ResourceResolver.PROPERTY_RESOURCE_TYPE, "some/type",
                "some other key", "some other value");
        
        Resource resource = context.resourceResolver().getResource("/resource");
        Assert.assertThat(resource, ResourceMatchers.ofType("some/type"));
        Assert.assertThat(resource, Matchers.not(ResourceMatchers.ofType("some/other/type")));
    }

    @Test
    public void testWithPath() {
        context.build().resource("/resource");
        
        Resource resource = context.resourceResolver().getResource("/resource");
        Assert.assertThat(resource, ResourceMatchers.withPath("/resource"));
        Assert.assertThat(resource, Matchers.not(ResourceMatchers.withPath("some/other/name")));
    }

    @Test
    public void testWithName() {
        context.build().resource("/resource");
        
        Resource resource = context.resourceResolver().getResource("/resource");
        Assert.assertThat(resource, ResourceMatchers.withName("resource"));
        Assert.assertThat(resource, Matchers.not(ResourceMatchers.withName("some/other/name")));
    }

    @Test
    public void testWithProps() {
        context.build().resource("/resource",
                "key1", "value1",
                "key2", "value2",
                "key3", "value3");
        
        Map<String, Object> expectedProperties = ImmutableMap.<String, Object>builder()
                .put("key1", "value1")
                .put("key2", "value2")
                .build();
        
        Resource resource = context.resourceResolver().getResource("/resource");
        Assert.assertThat(resource, ResourceMatchers.withProps(expectedProperties));
        
        // test existing key with not matching value
        expectedProperties = ImmutableMap.<String, Object>builder()
                .put("key1", "value1")
                .put("key2", "value3")
                .build();
        Assert.assertThat(resource, Matchers.not(ResourceMatchers.withProps(expectedProperties)));
        
        // test non-existing key
        expectedProperties = ImmutableMap.<String, Object>builder()
                .put("key4", "value4")
                .build();
        Assert.assertThat(resource, Matchers.not(ResourceMatchers.withProps(expectedProperties)));
    }

    @Test
    public void testWithPropsVarargs() {
        context.build().resource("/resource",
                "key1", "value1",
                "key2", "value2",
                "key3", "value3");
        
        Object[] expectedProperties = new Object[] {
                "key1", "value1",
                "key2", "value2"
        };
        
        Resource resource = context.resourceResolver().getResource("/resource");
        Assert.assertThat(resource, ResourceMatchers.withProps(expectedProperties));

        // test existing key with not matching value
        expectedProperties = new Object[] {
                "key1", "value1",
                "key2", "value3"
        };
        Assert.assertThat(resource, Matchers.not(ResourceMatchers.withProps(expectedProperties)));
        
        // test non-existing key
        expectedProperties = new Object[] {
                "key4", "value4"
        };
        Assert.assertThat(resource, Matchers.not(ResourceMatchers.withProps(expectedProperties)));
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
    public void testWithNameAndProps() {
        context.build().resource("/resource",
                "key1", "value1",
                "key2", "value2",
                "key3", "value3");
        
        Map<String, Object> expectedProperties = ImmutableMap.<String, Object>builder()
                .put("key1", "value1")
                .put("key2", "value2")
                .build();
        
        Resource resource = context.resourceResolver().getResource("/resource");
        Assert.assertThat(resource, ResourceMatchers.withNameAndProps("resource", expectedProperties));
        
        // test not matching name
        Assert.assertThat(resource, Matchers.not(ResourceMatchers.withNameAndProps("resource1", expectedProperties)));
        
        // test existing key with not matching value
        expectedProperties = ImmutableMap.<String, Object>builder()
                .put("key1", "value1")
                .put("key2", "value3")
                .build();
        Assert.assertThat(resource, Matchers.not(ResourceMatchers.withNameAndProps("resource", expectedProperties)));
    }

    @Test
    public void testWithNameAndPropsVarargs() {
        context.build().resource("/resource",
                "key1", "value1",
                "key2", "value2",
                "key3", "value3");
        
        Object[] expectedProperties = new Object[] {
                "key1", "value1",
                "key2", "value2"
        };
        
        Resource resource = context.resourceResolver().getResource("/resource");
        Assert.assertThat(resource, ResourceMatchers.withNameAndProps("resource", expectedProperties));
        
        // test not matching name
        Assert.assertThat(resource, Matchers.not(ResourceMatchers.withNameAndProps("resource1", expectedProperties)));
        
        // test existing key with not matching value
        expectedProperties = new Object[] {
                "key1", "value1",
                "key2", "value3"
        };
        Assert.assertThat(resource, Matchers.not(ResourceMatchers.withNameAndProps("resource", expectedProperties)));
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
