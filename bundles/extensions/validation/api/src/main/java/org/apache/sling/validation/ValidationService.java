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
package org.apache.sling.validation;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.commons.collections.Predicate;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.validation.exceptions.SlingValidationException;
import org.apache.sling.validation.model.ValidationModel;

import aQute.bnd.annotation.ProviderType;

/**
 * The {@code ValidationService} provides methods for finding {@link ValidationModel} services.
 */
@ProviderType
public interface ValidationService {

    /**
     * Tries to obtain a {@link ValidationModel} that is able to validate a {@code Resource} of type {@code validatedResourceType}.
     *
     * @param validatedResourceType the type of {@code Resources} the model validates, should be either relative 
     *                              (i.e. not start with a "/") or starting with one of the resource resolver's search paths
     * @param resourcePath        the path of the validated resource, may be {@code null} or empty. Must match the applicablePath property of the model
     * @param considerResourceSuperTypeModels if {@code true} will also consider the validation model of the resource super type (recursively), otherwise not.
     * @return a {@code ValidationModel} if one is found, {@code null} otherwise
     * @throws IllegalStateException in case an invalid validation model was found
     * @throws IllegalArgumentException in case validatedResourceType was blank, {@code null} or absolute but outside of the search paths or some other error occurred while retrieving the models.
     */
    @CheckForNull ValidationModel getValidationModel(@Nonnull String validatedResourceType, String resourcePath, boolean considerResourceSuperTypeModels) throws IllegalStateException, IllegalArgumentException;

    /**
     * Tries to obtain a {@link ValidationModel} that is able to validate the given {@code resource}.
     *
     * @param resource the resource for which to obtain a validation model
     * @return a {@code ValidationModel} if one is found, {@code null} otherwise
     * @throws IllegalStateException in case an invalid validation model was found
     * @throws IllegalArgumentException in case resourceType being set on the given resource is blank, not set or absolute but outside of the search paths or some other error occurred while retrieving the models.
     */
    @CheckForNull ValidationModel getValidationModel(@Nonnull Resource resource, boolean considerResourceSuperTypeModels) throws IllegalStateException, IllegalArgumentException;

    /**
     * Validates a {@link Resource} using a specific {@link ValidationModel}. If the {@code model} describes a resource tree,
     * the {@link ResourceResolver} associated with the {@code resource} will be used for retrieving information about the {@code
     * resource}'s descendants.
     *
     * @param resource the resource to validate
     * @param model    the model with which to perform the validation
     * @param considerResourceSuperTypeModels if {@code true} will also consider the validation model of the resource super type (recursively), otherwise not.
     * @return a {@link ValidationResult} that provides the necessary information
     * @throws SlingValidationException if one validator was called with invalid arguments
     */
    @Nonnull ValidationResult validate(@Nonnull Resource resource, @Nonnull ValidationModel model) throws SlingValidationException;

    /**
     * Validates a {@link ValueMap} or any object adaptable to a {@code ValueMap} using a specific {@link ValidationModel}. Since the
     * {@code valueMap} only contains the direct properties of the adapted object, the {@code model}'s descendants description should not
     * be queried for this validation operation.
     *
     * @param valueMap the map to validate
     * @return a {@link ValidationResult} that provides the necessary information
     * @throws SlingValidationException if one validator was called with invalid arguments
     */
    @Nonnull ValidationResult validate(@Nonnull ValueMap valueMap, @Nonnull ValidationModel model) throws SlingValidationException;

    /**
     * Validates a {@link Resource} and all child resources recursively by traversing through the resource tree starting from the given resource.
     * For all resources which are included in the given {@code filter} the according {@link ValidationModel} is retrieved and validation is called on those resources.
     * @param resource the root resource which is validated
     * @param enforceValidation if {@code true} will throw an {@link IllegalArgumentException} in case a validation model could not be found for a (not-ignored) resource 
     * @param filter a {@link Predicate} on a resource which is evaluated to determine whether a given resource should be validated. May be {@code null} in which case all resources are validated. 
     * Children of ignored resources are still validated (if this predicate applies to them).
     * @param considerResourceSuperTypeModels if {@code true} will also consider the validation model of the resource super type (recursively), otherwise not.
     * @return the aggregated {@link ValidationResult} over all child resource validations
     * @throws IllegalStateException in case an invalid validation model was found
     * @throws IllegalArgumentException in case resourceType is absolute but outside of the search paths or if no validation model could be found (and enforceValidation is {@code true}).
     * @throws SlingValidationException if one validator was called with invalid arguments
     */
    @Nonnull ValidationResult validateResourceRecursively(@Nonnull Resource resource, boolean enforceValidation, Predicate filter, boolean considerResourceSuperTypeModels) throws IllegalStateException, IllegalArgumentException, SlingValidationException;

}
