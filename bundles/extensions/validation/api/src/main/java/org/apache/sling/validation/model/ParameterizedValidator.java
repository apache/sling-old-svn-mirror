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

import javax.annotation.Nonnull;

import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.validation.spi.Validator;

/**
 * Defines a validator instance with information about the type and the parameterization of the validator.
 *
 */
public interface ParameterizedValidator {

    /**
     * 
     * @return the validator. Never {@code null}.
     */
    @Nonnull Validator<?> getValidator();

    /**
     * 
     * @return the parameterization of the validator (never {@code null}, but might be empty map)
     */
    @Nonnull ValueMap getParameters();

    /**
     * 
     * @return the type of the validator (i.e. the type of the data it can handle)
     */
    @Nonnull Class<?> getType();

}