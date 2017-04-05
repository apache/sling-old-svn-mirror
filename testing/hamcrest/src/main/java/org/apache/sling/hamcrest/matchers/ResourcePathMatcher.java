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

import org.apache.sling.api.resource.Resource;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

/**
 * Matcher which matches whenever the path of the given resource is equal to the path given in the constructor.
 */
public class ResourcePathMatcher extends TypeSafeMatcher<Resource> {

    private final String path;

    public ResourcePathMatcher(String path) {
        this.path = path;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("Resource with path ").appendValue(path);
    }

    @Override
    protected boolean matchesSafely(Resource resource) {
        return path.equals(resource.getPath());
    }

    @Override
    protected void describeMismatchSafely(Resource resource, Description mismatchDescription) {
        mismatchDescription.appendText("was Resource with path ").appendValue(resource.getPath()).appendText(" (resource: ").appendValue(resource).appendText(")");
    }

}
