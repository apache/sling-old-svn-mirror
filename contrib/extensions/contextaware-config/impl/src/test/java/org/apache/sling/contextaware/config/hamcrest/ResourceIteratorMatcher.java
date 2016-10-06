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
package org.apache.sling.contextaware.config.hamcrest;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.hamcrest.matchers.ResourcePathMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import com.google.common.collect.ImmutableList;

class ResourceIteratorMatcher extends TypeSafeMatcher<Iterator<Resource>> {

    // this should be "Iterable<? extends Resource>" instead of "?" but cannot until https://github.com/hamcrest/JavaHamcrest/issues/107 is solved
    private final Matcher<?> iterarableMatcher;

    public ResourceIteratorMatcher(List<String> paths) {
        if ( paths == null || paths.isEmpty() ) {
            throw new IllegalArgumentException("names is null or empty");
        }

        List<Matcher<? super Resource>> resourceMatchers = new ArrayList<>();
        for (String path : paths) {
            resourceMatchers.add(new ResourcePathMatcher(path));
        }

        this.iterarableMatcher = org.hamcrest.collection.IsIterableContainingInOrder.contains(resourceMatchers);
    }

    @Override
    public void describeTo(Description description) {
        iterarableMatcher.describeTo(description);
    }

    @Override
    protected boolean matchesSafely(Iterator<Resource> items) {
        return iterarableMatcher.matches(ImmutableList.copyOf(items));
    }

    @Override
    protected void describeMismatchSafely(Iterator<Resource> items, Description mismatchDescription) {
        iterarableMatcher.describeMismatch(ImmutableList.copyOf(items), mismatchDescription);
    }

}
