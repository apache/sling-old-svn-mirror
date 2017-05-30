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
package org.apache.sling.validation.spi;

import java.util.ResourceBundle;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.validation.ValidationService;
import org.osgi.annotation.versioning.ProviderType;


/**
 * Used as parameter for each call of {@link Validator#validate(Object, ValidatorContext, ValueMap)}
 * Exposes additional information about the context in which the validation was called.
 */
@ProviderType
public interface ValidatorContext {

    /**
     * @return the relative location of the property which should be checked by the validator. Refers to the 'data' parameter of {@link Validator#validate(Object, ValidatorContext, ValueMap)}
     */
    @Nonnull String getLocation();

    /**
     * @return all properties of the validated resource/valuemap (only used for validations considering multiple properties), never {@code null}.
     */
    @Nonnull ValueMap getValueMap();

    /**
     * @return the resource on which the validation was triggered. {@code null} in case the validation was triggered on a {@link ValueMap} (via {@link ValidationService#validate(ValueMap, org.apache.sling.validation.model.ValidationModel)}).
     */
    @CheckForNull Resource getResource();

    /**
     * Returns the severity to be issued for validation failures in this context.
     * @return the severity of the validation failure.
     */
    int getSeverity();
    
    /**
     * @return resource bundle which should be able to give out the error message of the {@link Validator} in English.
     */
    @Nonnull ResourceBundle getDefaultResourceBundle();
}
