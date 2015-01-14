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
package org.apache.sling.validation.api;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.validation.api.exceptions.SlingValidationException;

/**
 * The {@code ValidationService} provides methods for finding {@link ValidationModel} services.
 */
public interface ValidationService {

    /**
     * Tries to obtain a {@link ValidationModel} that is able to validate a {@code Resource} of type {@code validatedResourceType}.
     *
     * @param validatedResourceType the type of {@code Resources} the model validates, should be either relative 
     *                              (i.e. not start with a "/") or starting with one of the resource resolver's search paths
     * @param applicablePath        the model's applicable path (the path of the validated resource)
     * @return a {@code ValidationModel} if one is found, {@code null} otherwise
     * @throws IllegalStateException in case an invalid validation model was found
     * @throws IllegalArgumentException in case validatedResourceType was blank, {@code null} or absolute but outside of the search paths.
     */
    ValidationModel getValidationModel(String validatedResourceType, String applicablePath) throws IllegalStateException, IllegalArgumentException;

    /**
     * Tries to obtain a {@link ValidationModel} that is able to validate the given {@code resource}.
     *
     * @param resource the resource for which to obtain a validation model
     * @return a {@code ValidationModel} if one is found, {@code null} otherwise
     * @throws IllegalStateException in case an invalid validation model was found
     */
    ValidationModel getValidationModel(Resource resource) throws IllegalStateException;

    /**
     * Validates a {@link Resource} using a specific {@link ValidationModel}. If the {@code model} describes a resource tree,
     * the {@link ResourceResolver} associated with the {@code resource} will be used for retrieving information about the {@code
     * resource}'s descendants.
     *
     * @param resource the resource to validate
     * @param model    the model with which to perform the validation
     * @return a {@link ValidationResult} that provides the necessary information
     * @throws org.apache.sling.validation.api.exceptions.SlingValidationException if one validator was called with invalid arguments
     */
    ValidationResult validate(Resource resource, ValidationModel model) throws SlingValidationException;

    /**
     * Validates a {@link ValueMap} or any object adaptable to a {@code ValueMap} using a specific {@link ValidationModel}. Since the
     * {@code valueMap} only contains the direct properties of the adapted object, the {@code model}'s descendants description should not
     * be queried for this validation operation.
     *
     * @param valueMap the map to validate
     * @return a {@link ValidationResult} that provides the necessary information
     * @throws org.apache.sling.validation.api.exceptions.SlingValidationException if one validator was called with invalid arguments
     */
    ValidationResult validate(ValueMap valueMap, ValidationModel model) throws SlingValidationException;
}
