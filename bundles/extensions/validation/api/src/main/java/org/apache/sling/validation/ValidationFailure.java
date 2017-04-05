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
package org.apache.sling.validation;

import java.util.ResourceBundle;

import javax.annotation.Nonnull;

import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface ValidationFailure {

    /**
     * @param resourceBundle ResourceBundle in which to look up the according message (used for i18n), if {@code null} is given, the default resource bundle is used.
     * @return the failure message
     */
    @Nonnull String getMessage(ResourceBundle resourceBundle);

    /**
     * Returns the relative location of the property/resource/value which triggered this validation failure.
     * The location 
     * <ul>
     * <li>is relative to the resource given in the first parameter in case it was returned by {@link ValidationService#validate(org.apache.sling.api.resource.Resource, org.apache.sling.validation.model.ValidationModel)} or {@link ValidationService#validateResourceRecursively(org.apache.sling.api.resource.Resource, boolean, java.util.function.Predicate, boolean)} or</li>
     * <li>contains just the value name in case it was returned by {@link ValidationService#validate(org.apache.sling.api.resource.ValueMap, org.apache.sling.validation.model.ValidationModel)}</li>
     * </ul>
     * @return the location (usually the validated resource's property path).
     */
    @Nonnull String getLocation();

    /**
     * @return the severity of this validation failure. If no explicit severity was set either in the validation model or in the validator, this returns {@code 0}.
     */
    int getSeverity();

}
