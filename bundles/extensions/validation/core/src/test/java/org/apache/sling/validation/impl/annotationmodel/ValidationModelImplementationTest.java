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
package org.apache.sling.validation.impl.annotationmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.apache.sling.validation.model.ValidationModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Bundle;


@RunWith(MockitoJUnitRunner.class)
public class ValidationModelImplementationTest {

    private ValidationModelImplementation modelImplementation = new ValidationModelImplementation();

    @Mock
    private Bundle bundle;

    @Mock
    private ValidationModel model;

    @Mock
    private ValidationModel model2;

    @Before
    public void setUp() throws Exception {
        when(model.getValidatingResourceType()).thenReturn("validating/resource/type");
        when(model2.getValidatingResourceType()).thenReturn("second/type");
    }

    @Test
    public void testValidationModelsRegistration() throws Exception {
        modelImplementation.registerValidationModelsByBundle(bundle, Arrays.asList(model, model2, model));
        assertNotNull(modelImplementation.getValidationModelsByResourceType("validating/resource/type"));
        assertEquals(2, modelImplementation.getValidationModelsByResourceType("validating/resource/type").size());
        assertEquals(1, modelImplementation.getValidationModelsByResourceType("second/type").size());
    }

    @Test
    public void testEmpty() throws Exception {
        assertNotNull(modelImplementation.getValidationModelsByResourceType("validating/resource/type"));
    }

}