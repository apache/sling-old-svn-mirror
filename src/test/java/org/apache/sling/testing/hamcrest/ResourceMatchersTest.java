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
package org.apache.sling.testing.hamcrest;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.hamcrest.ResourceMatchers;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.resourceresolver.MockHelper;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

public class ResourceMatchersTest {

    @Rule
    public final SlingContext context = new SlingContext();

    @Test
    public void testResourceOfType() throws PersistenceException {
        MockHelper.create(context.resourceResolver())
        .resource("/resource").p(ResourceResolver.PROPERTY_RESOURCE_TYPE, "some/type").p("some other key", "some other value").commit();
        
        Resource resource = context.resourceResolver().getResource("/resource");
        Assert.assertThat(resource, ResourceMatchers.resourceOfType("some/type"));
        Assert.assertThat(resource, Matchers.not(ResourceMatchers.resourceOfType("some/other/type")));
    }

    @Test
    public void testResourceWithName() throws PersistenceException {
        MockHelper.create(context.resourceResolver())
        .resource("/resource").commit();
        
        Resource resource = context.resourceResolver().getResource("/resource");
        Assert.assertThat(resource, ResourceMatchers.resourceWithName("resource"));
        Assert.assertThat(resource, Matchers.not(ResourceMatchers.resourceWithName("some/other/name")));
    }

    @Test
    public void testResourceWithProps() throws PersistenceException {
        MockHelper.create(context.resourceResolver())
        .resource("/resource").p("key1", "value1").p("key2", "value2").p("key3", "value3").commit();
        
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
    public void testHasChildren() throws PersistenceException {
        MockHelper.create(context.resourceResolver())
          .resource("/parent").resource("child1")
          .resource("/parent/child2").commit();
        
        Resource resource = context.resourceResolver().getResource("/parent");
        Assert.assertThat(resource, ResourceMatchers.hasChildren("child1"));
    }
    
    @Test
    public void testResourceWithNameAndProps() throws PersistenceException {
        MockHelper.create(context.resourceResolver())
        .resource("/resource").p("key1", "value1").p("key2", "value2").p("key3", "value3").commit();
        
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
    public void testContainsChildrenInAnyOrder() throws PersistenceException {
        MockHelper.create(context.resourceResolver())
          .resource("/parent").resource("child1")
          .resource("/parent/child2").commit();
        
        Resource resource = context.resourceResolver().getResource("/parent");
        Assert.assertThat(resource, ResourceMatchers.containsChildrenInAnyOrder("child2", "child1"));
        Assert.assertThat(resource, ResourceMatchers.containsChildrenInAnyOrder("child1", "child2"));
        Assert.assertThat(resource, Matchers.not(ResourceMatchers.containsChildren("child2", "child3", "child1")));
    }

    @Test
    public void testContainsChildren() throws PersistenceException {
        MockHelper.create(context.resourceResolver())
          .resource("/parent").resource("child1")
          .resource("/parent/child2").commit();
        
        Resource resource = context.resourceResolver().getResource("/parent");
        Assert.assertThat(resource, ResourceMatchers.containsChildren("child1", "child2"));
        Assert.assertThat(resource, Matchers.not(ResourceMatchers.containsChildren("child2", "child1")));
        Assert.assertThat(resource, Matchers.not(ResourceMatchers.containsChildren("child1", "child2", "child3")));
    }
}
