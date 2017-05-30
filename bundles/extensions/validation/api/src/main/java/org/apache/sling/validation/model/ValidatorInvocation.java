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
package org.apache.sling.validation.model;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.validation.spi.Validator;
import org.osgi.annotation.versioning.ProviderType;


/**
 * Defines a specific validator invocation for a given property (without actually exposing a reference to the underlying {@link Validator}).
 */
@ProviderType
public interface ValidatorInvocation {

    /**
     * 
     * @return the validator id of the {@link Validator} which is supposed to be called.
     */
    @Nonnull String getValidatorId();

    /**
     * 
     * @return the parameterization of the {@link Validator#validate(Object, org.apache.sling.validation.spi.ValidatorContext, ValueMap)} call (never {@code null}, but might be empty map)
     */
    @Nonnull ValueMap getParameters();

    /**
     * @return the severity of validation failures emitted for this usage of the validator (as being set in the model).
     * May be {@code null} in case it was not set on the model.
     */
    @CheckForNull Integer getSeverity();

}