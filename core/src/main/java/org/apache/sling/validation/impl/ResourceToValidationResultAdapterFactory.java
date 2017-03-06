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
package org.apache.sling.validation.impl;

import javax.annotation.Nonnull;

import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.validation.ValidationFailure;
import org.apache.sling.validation.ValidationResult;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(property={AdapterFactory.ADAPTABLE_CLASSES+"=org.apache.sling.api.Resource", AdapterFactory.ADAPTER_CLASSES+"=org.apache.sling.validation.ValidationResult"})
public class ResourceToValidationResultAdapterFactory implements AdapterFactory {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceToValidationResultAdapterFactory.class);
    
    private static final @Nonnull String KEY_RESOURCE_METADATA = "sling.validationResult";

    @SuppressWarnings("unchecked")
    public <AdapterType> AdapterType getAdapter(Object adaptable, Class<AdapterType> type) {
        AdapterType adapter = null;
        if (adaptable instanceof Resource) {
            final Resource resource = (Resource) adaptable;
            if (type == ValidationFailure.class) {
                return (AdapterType)getValidationResultFromCache(resource);
            } else {
                LOG.warn("Cannot handle adapter {}", type.getName());
            }
        } else {
            LOG.warn("Cannot handle adaptable {}", adaptable.getClass().getName());
        }
        return adapter;
    }
    
    public static void putValidationResultToCache(ValidationResult validationResult, Resource resource) {
        resource.getResourceMetadata().put(KEY_RESOURCE_METADATA, validationResult);
    }
    
    private ValidationFailure getValidationResultFromCache(Resource resource) {
        return (ValidationFailure) resource.getResourceMetadata().get(KEY_RESOURCE_METADATA);
    }
}
