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
import java.util.Iterator;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.hamcrest.matchers.ResourceIteratorPathMatcher;
import org.hamcrest.Matcher;

/**
 * A collection of <tt>Matcher</tt>s for resource iterators.
 */
public final class ResourceIteratorMatchers {
    
    private ResourceIteratorMatchers() {
        // static methods only
    }

    /**
     * Asserts that the given resource collection has resources with exactly the given paths in the given order.
     * @param paths the expected resource paths
     * @return a matcher instance
     */
    public static Matcher<Iterator<Resource>> paths(String... paths) {
        return new ResourceIteratorPathMatcher(Arrays.asList(paths));
    }
    
}
