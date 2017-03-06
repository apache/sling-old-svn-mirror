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
package org.apache.sling.validation.model.spi;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.sling.validation.model.ValidationModel;
import org.apache.sling.validation.model.ValidatorAndSeverity;
import org.osgi.annotation.versioning.ProviderType;


/**
 * All providers of {@link ValidationModel}s must implement this interface. Caching of validation models should be implemented in the provider itself, 
 * because the providers are asked potentially multiple times for each {@link ValidationService#getValidationModel(org.apache.sling.api.resource.Resource, boolean)} or 
 *  {@link ValidationService#getValidationModel(String, String, boolean)} call.
 * 
 */
@ProviderType
public interface ValidationModelProvider {

    /**
     * Retrieves the models responsible for validating the given resourceType.
     * 
     * @param relativeResourceType the relative resource (relative to one of the resource resolver's search paths)
     * @param validatorsMap
     *            all known validators in a map (key=id of validator). Only one of those should be used in the
     *            returned validation models.
     * @return a List of {@link ValidationModel}s. Never {@code null}, but might be empty collection in case no
     *         model for the given resource type could be found. The order which model gets active is mostly determined by {@link ValidationModel#getApplicablePaths()} (longest path match wins) 
     *         but in case there are multiple models having the same applicable path, the order being returned here is considered (i.e. the first one is taken).
     * @throws IllegalStateException
     *             in case a validation model was found but it is invalid
     */
    @Nonnull List<ValidationModel> getModels(@Nonnull String relativeResourceType,
            @Nonnull Map<String, ValidatorAndSeverity<?>> validatorsMap) throws IllegalStateException;

}
