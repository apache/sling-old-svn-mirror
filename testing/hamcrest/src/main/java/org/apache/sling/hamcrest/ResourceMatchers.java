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

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.hamcrest.matchers.ResourceChildrenMatcher;
import org.apache.sling.hamcrest.matchers.ResourceMatcher;
import org.hamcrest.Matcher;

/**
 * A collection of <tt>Matcher</tt>s that work on the Resource API level
 *
 */
public final class ResourceMatchers {
    
    /**
     * Matches resources which have amongst their children the specified <tt>children</tt>.
     * 
     * Child resources not contained in the specified <tt>children</tt> are not validated.
     * 
     * <pre>
     * assertThat(resource, hasChildren('child1', 'child2'));
     * </pre>
     * 
     * @param children the expected children, not <code>null</code> or empty
     * @return a matcher instance
     */
    public static Matcher<Resource> hasChildren(String... children) {
        return new ResourceChildrenMatcher(Arrays.asList(children));
    }
    
    /**
     * Matches resources with a resource type set to the specified <tt>resourceType</tt>
     * 
     * <pre>
     * assertThat(resource, resourceOfType('my/app'));
     * </pre>
     * @param resourceType the resource type to match
     * @return a matcher instance
     */
    public static Matcher<Resource> resourceOfType(String resourceType) {
        return new ResourceMatcher(Collections.<String, Object> singletonMap(ResourceResolver.PROPERTY_RESOURCE_TYPE, resourceType));
    }

    /**
     * Matches resources which has at least the specified <tt>properties</tt> defined with matching values
     * 
     * <p>Values not declared in the the <tt>properties</tt> parameter are not validated.</p>
     * <pre>
     * Map<String, Object> expectedProperties = new HashMap<>();
     * expectedProperties.put("jcr:title", "Node title");
     * expectedProperties.put("jcr:text",  "Some long text");
     * 
     * assertThat(resource, resourceWithProps(expectedProperties));
     * </pre>
     * 
     * @param resourceType the resource type to match
     * @return a matcher instance
     */    
    public static Matcher<Resource> resourceWithProps(Map<String, Object> properties) {
        return new ResourceMatcher(properties);
    }
    
    private ResourceMatchers() {
        // prevent instantiation
    }
}
