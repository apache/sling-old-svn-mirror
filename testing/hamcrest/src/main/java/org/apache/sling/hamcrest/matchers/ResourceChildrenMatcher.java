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
package org.apache.sling.hamcrest.matchers;

import java.util.ArrayList;
import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * A matcher which matches if the given resource has at least the child resources with the names given in the constructor.
 * Optionally it can match only if the resource's children match exactly the given child names.
 * Also you can validate the order in case of exact matching.
 *
 */
public class ResourceChildrenMatcher extends TypeSafeMatcher<Resource> {

    // this should be "Iterable<? extends Resource>" instead of "?" but cannot until https://github.com/hamcrest/JavaHamcrest/issues/107 is solved
    private final Matcher<?> iterarableMatcher;

    public ResourceChildrenMatcher(List<String> childNames, boolean exactMatch, boolean validateOrder) {
        if ( childNames == null || childNames.isEmpty() ) {
            throw new IllegalArgumentException("childNames is null or empty");
        }

        if (!exactMatch && validateOrder) {
            throw new IllegalArgumentException("Can only validate the order for exact matches");
        }

        List<Matcher<? super Resource>> resourceMatchers = new ArrayList<Matcher<? super Resource>>();
        for (String childName : childNames) {
            resourceMatchers.add(new ResourceNameMatcher(childName));
        }

        if (exactMatch) {
            if (validateOrder) {
                this.iterarableMatcher = org.hamcrest.collection.IsIterableContainingInOrder.contains(resourceMatchers);
            } else {
                this.iterarableMatcher = org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder(resourceMatchers);
            }
        } else {
            this.iterarableMatcher = org.hamcrest.core.IsCollectionContaining.hasItems(resourceMatchers.toArray(new ResourceNameMatcher[0]));
        }
    }

    @Override
    public void describeTo(Description description) {
        iterarableMatcher.describeTo(description);
    }

    @Override
    protected boolean matchesSafely(Resource item) {
        return iterarableMatcher.matches(item.getChildren());
    }

    @Override
    protected void describeMismatchSafely(Resource item, Description mismatchDescription) {
        // the default would be something like ".. but item 0 was <Resource.toString()>"
        // use the iterable matcher here instead
        iterarableMatcher.describeMismatch(item.getChildren(), mismatchDescription);
    }

}