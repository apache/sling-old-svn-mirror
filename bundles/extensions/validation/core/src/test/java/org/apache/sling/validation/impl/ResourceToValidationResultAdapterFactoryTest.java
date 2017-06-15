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
package org.apache.sling.validation.impl;

import java.util.Collections;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.validation.ValidationResult;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResourceToValidationResultAdapterFactoryTest {

    @Test
    public void testResourceToValidationResultAdaption() {
        final ValidationResult result = mock(ValidationResult.class);
        when(result.isValid()).thenReturn(true);
        when(result.getFailures()).thenReturn(Collections.emptyList());
        final Resource resource = mock(Resource.class);
        final ResourceMetadata metadata = mock(ResourceMetadata.class);
        when(metadata.get("sling.validation.result")).thenReturn(result);
        when(resource.getResourceMetadata()).thenReturn(metadata);
        final ResourceToValidationResultAdapterFactory factory = new ResourceToValidationResultAdapterFactory();
        final ValidationResult adapter = factory.getAdapter(resource, ValidationResult.class);
        assertThat(adapter, is(result));
    }

}
