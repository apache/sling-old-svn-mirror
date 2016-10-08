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

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

public class ResourceMatchersTest {

    @Rule
    public final SlingContext context = new SlingContext();

    @Test
    public void testResourceOfType() {
        context.build().resource("/resource", 
                ResourceResolver.PROPERTY_RESOURCE_TYPE, "some/type",
                "some other key", "some other value");
        
        Resource resource = context.resourceResolver().getResource("/resource");
        Assert.assertThat(resource, ResourceMatchers.resourceOfType("some/type"));
        Assert.assertThat(resource, Matchers.not(ResourceMatchers.resourceOfType("some/other/type")));
    }

    @Test
    public void testResourceWithPath() {
        context.build().resource("/resource");
        
        Resource resource = context.resourceResolver().getResource("/resource");
        Assert.assertThat(resource, ResourceMatchers.resourceWithPath("/resource"));
        Assert.assertThat(resource, Matchers.not(ResourceMatchers.resourceWithPath("some/other/name")));
    }

    @Test
    public void testResourceWithName() {
        context.build().resource("/resource");
        
        Resource resource = context.resourceResolver().getResource("/resource");
        Assert.assertThat(resource, ResourceMatchers.resourceWithName("resource"));
        Assert.assertThat(resource, Matchers.not(ResourceMatchers.resourceWithName("some/other/name")));
    }

    @Test
    public void testResourceWithProps() {
        context.build().resource("/resource",
                "key1", "value1",
                "key2", "value2",
                "key3", "value3");
        
        Map<String, Object> expectedProperties = new HashMap<String, Object>();
        expectedProperties.put("key1", "value1");
        expectedProperties.put("key2", "value2");
        
        Resource resource = context.resourceResolver().getResource("/resource");
        Assert.assertThat(resource, ResourceMatchers.resourceWithProps(expectedProperties));
        // test existing key with not matching value
        expectedProperties.put("key2", "value3"); 
        Assert.assertThat(resource, Matchers.not(ResourceMatchers.resourceWithProps(expectedProperties)));
        
        // test non-existing key
        expectedProperties.clear();
        expectedProperties.put("key4", "value4");
        Assert.assertThat(resource, Matchers.not(ResourceMatchers.resourceWithProps(expectedProperties)));
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
    public void testResourceWithNameAndProps() {
        context.build().resource("/resource",
                "key1", "value1",
                "key2", "value2",
                "key3", "value3");
        
        Map<String, Object> expectedProperties = new HashMap<String, Object>();
        expectedProperties.put("key1", "value1");
        expectedProperties.put("key2", "value2");
        
        Resource resource = context.resourceResolver().getResource("/resource");
        Assert.assertThat(resource, ResourceMatchers.resourceWithNameAndProps("resource", expectedProperties));
        
        // test not matching name
        Assert.assertThat(resource, Matchers.not(ResourceMatchers.resourceWithNameAndProps("resource1", expectedProperties)));
        
        // test existing key with not matching value
        expectedProperties.put("key2", "value3"); 
        Assert.assertThat(resource, Matchers.not(ResourceMatchers.resourceWithNameAndProps("resource", expectedProperties)));
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
