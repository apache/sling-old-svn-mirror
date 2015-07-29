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
package org.apache.sling.validation.impl.model;

import java.util.Arrays;
import java.util.regex.Pattern;

import org.apache.sling.validation.model.ChildResource;
import org.apache.sling.validation.model.ResourceProperty;
import org.apache.sling.validation.model.ValidationModel;
import org.hamcrest.Description;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MergedValidationModelTest {

    private ValidationModelBuilder modelBuilder;
    private ResourcePropertyBuilder propertyBuilder;
    private ChildResourceBuilder childResourceBuilder;

    @Before
    public void setup() {
        modelBuilder = new ValidationModelBuilder();
        propertyBuilder = new ResourcePropertyBuilder();
        childResourceBuilder = new ChildResourceBuilder();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMoreSpecificApplicationPathInModelToMerge() {
        modelBuilder.addApplicablePath("/base/path").addApplicablePath("/base/path2");
        ValidationModel baseValidationModel = modelBuilder.build("base");
        modelBuilder.setApplicablePath("/base/path3");
        new MergedValidationModel(baseValidationModel, modelBuilder.build("superType"));
    }

    @Test
    public void testLessSpecificApplicationPathInModelToMerge() {
        modelBuilder.addApplicablePath("/base/path").addApplicablePath("/base/path2");
        ValidationModel baseValidationModel = modelBuilder.build("base");
        modelBuilder.setApplicablePath("/base");
        ValidationModel mergedModel = new MergedValidationModel(baseValidationModel, modelBuilder.build("superType"));
        Assert.assertThat(Arrays.asList(mergedModel.getApplicablePaths()),
                Matchers.contains("/base/path", "/base/path2"));
    }

    @Test
    public void testOverwritingChildrenAndResourceProperties() {
        modelBuilder.resourceProperty(propertyBuilder.nameRegex("overwrittenNameToOverwrite").build("nameToOverwrite"));
        modelBuilder.childResource(childResourceBuilder.nameRegex("overwrittenNameToOverwrite")
                .build("nameToOverwrite"));
        ValidationModel baseValidationModel = modelBuilder.build("base");
        modelBuilder = new ValidationModelBuilder();
        modelBuilder.resourceProperty(propertyBuilder.nameRegex("originalNameToOverwrite").build("nameToOverwrite"));
        modelBuilder.childResource(childResourceBuilder.nameRegex("originalNameToOverwrite").build("nameToOverwrite"));
        modelBuilder.resourceProperty(propertyBuilder.nameRegex("originalNameNotOverwritten").build(
                "nameNotOverwritten"));
        modelBuilder.childResource(childResourceBuilder.nameRegex("originalNameNotOverwritten").build(
                "nameNotOverwritten"));
        ValidationModel mergedModel = new MergedValidationModel(baseValidationModel, modelBuilder.build("superType"));
        Assert.assertThat(mergedModel.getResourceProperties(), Matchers.containsInAnyOrder(
                new ResourcePropertyNameRegexMatcher("overwrittenNameToOverwrite"),
                new ResourcePropertyNameRegexMatcher("originalNameNotOverwritten")));
        Assert.assertThat(mergedModel.getChildren(), Matchers.containsInAnyOrder(new ChildResourceNameRegexMatcher(
                "overwrittenNameToOverwrite"), new ChildResourceNameRegexMatcher("originalNameNotOverwritten")));
    }

    @Test
    public void testValidatedResourceTypes() {
        ValidationModel mergedModel = new MergedValidationModel(modelBuilder.build("base"),
                modelBuilder.build("superType"));
        Assert.assertThat(mergedModel.getValidatedResourceType(), Matchers.equalTo("base"));
    }

    /**
     * Custom Hamcrest matcher which matches Resource Properties based on the equality only on their namePatterns.
     */
    private static final class ResourcePropertyNameRegexMatcher extends TypeSafeMatcher<ResourceProperty> {

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

    /**
     * Custom Hamcrest matcher which matches ChildResource based on the equality only on their namePatterns.
     */
    private static final class ChildResourceNameRegexMatcher extends TypeSafeMatcher<ChildResource> {

        private final String expectedNameRegex;

        public ChildResourceNameRegexMatcher(String nameRegex) {
            expectedNameRegex = nameRegex;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("ChildResource with namePattern=" + expectedNameRegex);
        }

        @Override
        protected boolean matchesSafely(ChildResource childResource) {
            Pattern namePattern = childResource.getNamePattern();
            if (namePattern == null) {
                return false;
            } else {
                return expectedNameRegex.equals(namePattern.toString());
            }
        }
    }
}
