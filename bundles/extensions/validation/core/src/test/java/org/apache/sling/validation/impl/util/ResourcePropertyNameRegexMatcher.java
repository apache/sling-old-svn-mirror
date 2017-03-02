/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.validation.impl.util;

import java.util.regex.Pattern;

import org.apache.sling.validation.model.ResourceProperty;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

/**
 * Custom Hamcrest matcher which matches Resource Properties based on the equality only on their namePatterns.
 */
public class ResourcePropertyNameRegexMatcher extends TypeSafeMatcher<ResourceProperty> {

    private final String expectedNameRegex;

    public ResourcePropertyNameRegexMatcher(String nameRegex) {
        expectedNameRegex = nameRegex;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("ResourceProperty with namePattern=" + expectedNameRegex);
    }

    @Override
    protected boolean matchesSafely(ResourceProperty resourceProperty) {
        Pattern namePattern = resourceProperty.getNamePattern();
        if (namePattern == null) {
            return false;
        } else {
            return expectedNameRegex.equals(namePattern.toString());
        }
    }

}