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

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

@RunWith(MockitoJUnitRunner.class)
public class AnnotationValidationModelProviderImplTest {

    private AnnotationValidationModelProviderImpl validationModelProvider = new AnnotationValidationModelProviderImpl();

    @Mock
    private BundleContext bundleContext;

    @Mock
    private ComponentContext ctx;

    @Before
    public void setUp() throws Exception {
        when(ctx.getBundleContext()).thenReturn(bundleContext);
        validationModelProvider.activate(ctx);

    }

    @Test
    public void noValidationModelsForResourceType() throws Exception {
        assertTrue(validationModelProvider.getValidationModels("sling/validation/test/example").isEmpty());
    }

}