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
package org.apache.sling.validation.model.spi;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.sling.validation.model.ValidationModel;
import org.osgi.annotation.versioning.ProviderType;

/** Retrieves the validation model. */
@ProviderType
public interface ValidationModelRetriever {
    /**
     * A validation model for the given resourceType at the given resourcePath
     * @param resourceType the resource type for which to retrieve the model
     * @param resourcePath may be {@code null} or empty
     * @param considerResourceSuperTypeModels {@code true} if a merged validation model considering even the models of the super resource types should be returned, otherwise {@code false}
     * @return a validation model which should be used for validation or {@code null}, if no validation model could be found
     * @throws IllegalStateException in case some error occurred during looking up models
     */
    public @CheckForNull ValidationModel getValidationModel(@Nonnull String resourceType, String resourcePath, boolean considerResourceSuperTypeModels);
}
