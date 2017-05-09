/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.validation.impl.annotationmodel;

import static org.apache.sling.validation.impl.annotationmodel.ValidationPackageBundleListener.CLASSES_HEADER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.felix.framework.util.MapToDictionary;
import org.apache.sling.validation.impl.annotationmodel.testmodels.ChildModel;
import org.apache.sling.validation.impl.annotationmodel.testmodels.TextModel;
import org.apache.sling.validation.model.ChildResource;
import org.apache.sling.validation.model.ResourceProperty;
import org.apache.sling.validation.model.ValidationModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * Testing Validation Model creation from Sling Model annotations when adding a bundle.
 */
@RunWith(MockitoJUnitRunner.class)
public class ValidationPackageBundleListenerTest {

    @Mock
    private BundleContext bundleContext;

    @Mock
    private Bundle bundle;

    private ValidationModelRegister validationModelImplementation = new ValidationModelRegister();

    private ValidationPackageBundleListener listener;

    @Before
    public void setUp() throws Exception {
        listener = new ValidationPackageBundleListener(bundleContext, validationModelImplementation);
        when(bundle.getHeaders())
                .thenReturn(new MapToDictionary(Collections.singletonMap(CLASSES_HEADER, ChildModel.class.getCanonicalName())));
        doReturn(TextModel.class).when(bundle).loadClass(ChildModel.class.getCanonicalName());
    }


    @Test
    public void testAddingBundleAndCreatingValidationModel() throws Exception {
        listener.addingBundle(bundle, null);
        List<ValidationModel> validationModels = validationModelImplementation
                .getValidationModelsByResourceType("sling/validation/test/example");
        assertEquals(1, validationModels.size());
        ValidationModel result = validationModels.get(0);
        assertEquals("sling/validation/test/example", result.getValidatingResourceType());

        List<ResourceProperty> resourceProperties = new ArrayList<>(result.getResourceProperties());
        assertEquals(1, resourceProperties.size());
        ResourceProperty resourceProperty = resourceProperties.get(0);
        assertEquals("testString", resourceProperty.getName());
        assertFalse(resourceProperty.isMultiple());
        assertTrue(resourceProperty.isRequired());
        assertEquals("MinimumLengthValidator", resourceProperty.getValidatorInvocations().get(0).getValidatorId());
        assertEquals("10", resourceProperty.getValidatorInvocations().get(0).getParameters().get("minLength"));
        assertEquals(Integer.valueOf(0), resourceProperty.getValidatorInvocations().get(0).getSeverity());

        List<ChildResource> childResources = new ArrayList<>(result.getChildren());
        assertEquals(2, childResources.size());
        ChildResource model = childResources.get(0);
        assertEquals("model", model.getName());
        assertEquals(0, model.getChildren().size());
        assertEquals(2, model.getProperties().size());
        assertTrue(model.isRequired());

        List<ResourceProperty> childResourceProperties = new ArrayList<>(model.getProperties());
        assertEquals("child", childResourceProperties.get(0).getName());
        assertEquals(true, childResourceProperties.get(0).isRequired());
        assertEquals(false, childResourceProperties.get(0).isMultiple());
        assertEquals(0, childResourceProperties.get(0).getValidatorInvocations().size());

        assertEquals("multiple", childResourceProperties.get(1).getName());
        assertEquals(false, childResourceProperties.get(1).isRequired());
        assertEquals(true, childResourceProperties.get(1).isMultiple());
        assertEquals(0, childResourceProperties.get(1).getValidatorInvocations().size());

        ChildResource children = childResources.get(1);
        assertEquals("children", children.getName());
        assertEquals(1, children.getChildren().size());
        assertEquals(0, children.getProperties().size());
        assertFalse(children.isRequired());
    }

}