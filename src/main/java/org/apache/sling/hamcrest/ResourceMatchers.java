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
    
    public static Matcher<Resource> hasChildren(String... children) {
        return new ResourceChildrenMatcher(Arrays.asList(children));
    }
    
    public static Matcher<Resource> resourceOfType(String resourceType) {
        return new ResourceMatcher(Collections.<String, Object> singletonMap(ResourceResolver.PROPERTY_RESOURCE_TYPE, resourceType));
    }

    public static Matcher<Resource> resourceWithProps(Map<String, Object> properties) {
        return new ResourceMatcher(properties);
    }
    
    private ResourceMatchers() {
        // prevent instantiation
    }
}
