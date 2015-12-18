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

import java.util.Collection;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.validation.model.ValidationModel;
import org.apache.sling.validation.spi.Validator;

/**
 * All providers of {@link ValidationModel}s must implement this interface. In addition if the model might become
 * invalid after some time it is also the obligatation of the provider implementation to invalidate the cache via the
 * {@link ValidationModelCache} OSGi service.
 */
public interface ValidationModelProvider {

    /**
     * Retrieves the models responsible for validating the given resourceType.
     * 
     * @param relativeResourceType
     * @param validatorsMap
     *            all known validators in a map (key=name of validator). Only one of those should be used in the
     *            returned validation models.
     * @param resourceResolver
     *            the resource resolver which should be used by the provider (if one is necessary).
     * @return a Collection of {@link ValidationModel}s. Never {@code null}, but might be empty collection in case no
     *         model for the given resource type could be found.
     * @throws IllegalStateException
     *             in case a validation model was found but it is invalid
     */
    public @Nonnull Collection<ValidationModel> getModel(@Nonnull String relativeResourceType,
            @Nonnull Map<String, Validator<?>> validatorsMap, @Nonnull ResourceResolver resourceResolver)
            throws IllegalStateException;
}
