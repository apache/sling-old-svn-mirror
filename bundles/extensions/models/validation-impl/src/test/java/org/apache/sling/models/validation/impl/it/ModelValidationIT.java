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
package org.apache.sling.models.validation.impl.it;

import java.io.IOException;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.junit.rules.TeleporterRule;
import org.apache.sling.models.factory.ModelFactory;
import org.apache.sling.models.factory.ValidationException;
import org.apache.sling.models.validation.InvalidResourceException;
import org.apache.sling.validation.ValidationService;
import org.apache.sling.validation.model.ValidationModel;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ModelValidationIT {

    @Rule
    public final TeleporterRule teleporter = TeleporterRule.forClass(getClass()).withResources("/SLING-CONTENT/");

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    private ValidationService validationService;

    private ValidationModel validationModel;

    private ModelFactory modelFactory;

    private ResourceResolverFactory resourceResolverFactory;

    private ResourceResolver resourceResolver = null;

    private static final int MODEL_AVAILABLE_TIMEOUT_SECONDS = Integer.getInteger("ValidationServiceIT.ModelAvailabelTimeoutSeconds", 10);

    @Before
    public void setUp() throws InterruptedException, LoginException {
        validationService = teleporter.getService(ValidationService.class);

        long timeoutMs = System.currentTimeMillis() + MODEL_AVAILABLE_TIMEOUT_SECONDS * 1000l;
        // wait for the model to become available (internally relies on search, is therefore asynchronous)
        do {
            validationModel = validationService.getValidationModel("validation/test/resourceType1",
                    "/validation/testing/fakeFolder1/resource", false);
            if (validationModel == null) {
                Thread.sleep(500);
            }
        } while (validationModel == null && System.currentTimeMillis() < timeoutMs);
        Assert.assertNotNull("Could not get validation model for resource type 'validation/test/resourceType1' within "
                + MODEL_AVAILABLE_TIMEOUT_SECONDS + " seconds", validationModel);
        modelFactory = teleporter.getService(ModelFactory.class);
        resourceResolverFactory = teleporter.getService(ResourceResolverFactory.class);
        resourceResolver = resourceResolverFactory.getServiceResourceResolver(null);
    }

    @After
    public void tearDown() {
        if (resourceResolver != null) {
            resourceResolver.close();
        }
    }

    @Test
    public void testValidModel() throws IOException, JSONException {
        // create a valid resource
        Resource contentResource = resourceResolver.getResource("/apps/sling/validation/content/contentValid");
        Assert.assertNotNull("Content resource must exist", contentResource);
        // generate a model
        ModelValidationRequired model = modelFactory.createModel(contentResource, ModelValidationRequired.class);
        Assert.assertNotNull("model must have been created", model);
    }

    @Test
    public void testInvalidModel() throws IOException, JSONException {
        // create a valid resource
        Resource contentResource = resourceResolver.getResource("/apps/sling/validation/content/contentInvalid");
        Assert.assertNotNull("Content resource must exist", contentResource);
        expectedEx.expect(InvalidResourceException.class);
        expectedEx.expectMessage(
                "Validation errors for '/apps/sling/validation/content/contentInvalid':\nfield1:Property does not match the pattern \"^\\p{Upper}+$\"");
        // generate a model
        modelFactory.createModel(contentResource, ModelValidationRequired.class);
    }

    @Test
    public void testModelWithoutRequiredValidationModel() {
        // create a valid resource
        Resource contentResource = resourceResolver.getResource("/apps/sling/validation/content/contentWithNoValidationModel");
        Assert.assertNotNull("Content resource must exist", contentResource);
        expectedEx.expect(ValidationException.class);
        expectedEx.expectMessage("Could not find validation model for resource '/apps/sling/validation/content/contentWithNoValidationModel' with type 'test/resourceType1'");
        // generate a model
        modelFactory.createModel(contentResource, ModelValidationRequired.class);
    }

    @Test
    public void testModelWithoutOptionalValidationModel() {
        // create a valid resource
        Resource contentResource = resourceResolver.getResource("/apps/sling/validation/content/contentWithNoValidationModel");
        Assert.assertNotNull("Content resource must exist", contentResource);
        // generate a model
        ModelValidationOptional model = modelFactory.createModel(contentResource, ModelValidationOptional.class);
        Assert.assertNotNull("model must have been created", model);
    }

    @Test
    public void testInvalidModelWithValidationDisabled() {
        // create a valid resource
        Resource contentResource = resourceResolver.getResource("/apps/sling/validation/content/contentWithNoValidationModel");
        Assert.assertNotNull("Content resource must exist", contentResource);
        // generate a model
        ModelValidationDisabled model = modelFactory.createModel(contentResource, ModelValidationDisabled.class);
        Assert.assertNotNull("model must have been created", model);
    }
}
