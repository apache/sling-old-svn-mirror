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

import org.apache.sling.validation.impl.util.ChildResourceNameRegexMatcher;
import org.apache.sling.validation.impl.util.ResourcePropertyNameRegexMatcher;
import org.apache.sling.validation.model.ValidationModel;
import org.hamcrest.Matchers;
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
        // each model needs at least one property or childresource
        modelBuilder.resourceProperty(propertyBuilder.build("nameToOverwrite"));
        childResourceBuilder = new ChildResourceBuilder();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMoreSpecificApplicationPathInModelToMerge() {
        modelBuilder.addApplicablePath("/base/path").addApplicablePath("/base/path2");
        ValidationModel baseValidationModel = modelBuilder.build("base", "some source");
        modelBuilder.setApplicablePath("/base/path3");
        new MergedValidationModel(baseValidationModel, modelBuilder.build("superType", "some source"));
    }

    @Test
    public void testLessSpecificApplicationPathInModelToMerge() {
        modelBuilder.addApplicablePath("/base/path").addApplicablePath("/base/path2");
        ValidationModel baseValidationModel = modelBuilder.build("base", "some source");
        modelBuilder.setApplicablePath("/base");
        ValidationModel mergedModel = new MergedValidationModel(baseValidationModel, modelBuilder.build("superType", "some source"));
        Assert.assertThat(mergedModel.getApplicablePaths(),
                Matchers.contains("/base/path", "/base/path2"));
    }

    @Test
    public void testOverwritingChildrenAndResourceProperties() {
        modelBuilder.resourceProperty(propertyBuilder.nameRegex("overwrittenNameToOverwrite").build("nameToOverwrite"));
        modelBuilder.childResource(childResourceBuilder.nameRegex("overwrittenNameToOverwrite")
                .build("nameToOverwrite"));
        ValidationModel baseValidationModel = modelBuilder.build("base", "some source");
        modelBuilder = new ValidationModelBuilder();
        modelBuilder.resourceProperty(propertyBuilder.nameRegex("originalNameToOverwrite").build("nameToOverwrite"));
        modelBuilder.childResource(childResourceBuilder.nameRegex("originalNameToOverwrite").build("nameToOverwrite"));
        modelBuilder.resourceProperty(propertyBuilder.nameRegex("originalNameNotOverwritten").build(
                "nameNotOverwritten"));
        modelBuilder.childResource(childResourceBuilder.nameRegex("originalNameNotOverwritten").build(
                "nameNotOverwritten"));
        ValidationModel mergedModel = new MergedValidationModel(baseValidationModel, modelBuilder.build("superType", "some source"));
        Assert.assertThat(mergedModel.getResourceProperties(), Matchers.containsInAnyOrder(
                new ResourcePropertyNameRegexMatcher("overwrittenNameToOverwrite"),
                new ResourcePropertyNameRegexMatcher("originalNameNotOverwritten")));
        Assert.assertThat(mergedModel.getChildren(), Matchers.containsInAnyOrder(new ChildResourceNameRegexMatcher(
                "overwrittenNameToOverwrite"), new ChildResourceNameRegexMatcher("originalNameNotOverwritten")));
    }

    @Test
    public void testValidatedResourceTypes() {
        ValidationModel mergedModel = new MergedValidationModel(modelBuilder.build("base", "some source"),
                modelBuilder.build("superType", "some source"));
        Assert.assertThat(mergedModel.getValidatedResourceType(), Matchers.equalTo("base"));
    }

}
