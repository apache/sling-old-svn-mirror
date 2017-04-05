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

import javax.annotation.Nonnull;

import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.validation.SlingValidationException;
import org.apache.sling.validation.ValidationFailure;
import org.apache.sling.validation.ValidationResult;
import org.osgi.annotation.versioning.ConsumerType;


/**
 * A {@code Validator} is responsible for validating a single piece of information according to an internal constraint.
 */
@ConsumerType
public interface Validator <T> {

    /**
     * Validates the {@code data} and/or the {@code valueMap} according to the internal constraints of this validator.
     * 
     * The validator can enforce the type of the given data just by setting the appropriate parameter type {@code T} which can be any non-primitive class. 
     * Depending on whether this type is an array or not the {@code validate} method is called differently:
     * <table summary="">
     *  <tr>
     *    <th>T is array type</th>
     *    <th>Valuemap contains array value</th>
     *    <th>{@code validate} is called...</th>
     *  </tr>
     *  <tr>
     *    <td>yes</td>
     *    <td>yes</td>
     *    <td>once per property with {@code data} containing the full array</td>
     *  </tr>
     *  <tr>
     *    <td>yes</td>
     *    <td>no</td>
     *    <td>once per property with {@code data} containing a single element array</td>
     *  </tr>
     *  <tr>
     *    <td>no</td>
     *    <td>yes</td>
     *    <td>once per element in the property array with {@code data} containing one array element</td>
     *  </tr>
     *  <tr>
     *    <td>no</td>
     *    <td>no</td>
     *    <td>once per property with {@code data} containing the value of the property</td>
     *  </tr>
     * </table>
     * If the data cannot be converted into the type {@code T} the validator is not called, but validation fails.
     *
     * @param data the data to validate (primary property), never {@code null}.
     * @param context the validation context contains additional information about the data to be validated, never {@code null}.
     * @param arguments the parameterization of the validator. Never {@code null} but might be the empty map.
     * @return the validation result (encapsulates the validation status as well as messages).
     * @throws SlingValidationException if some expected arguments are missing from the arguments map
     */
    @Nonnull ValidationResult validate(@Nonnull T data, @Nonnull ValidatorContext context, @Nonnull ValueMap arguments) throws SlingValidationException;
    
    /**
     * Each {@link Validator} must have a service property with name {@code validator.id} of type {@link String}. The validators are only addressable via the value of this property 
     * from the validation models. If there are multiple validators registered for the same {@code validator.id}, the one with highest service ranking is used.
     * It is recommended to prefix the value of the validator with the providing bundle symbolic name to prevent any name clashes.
     */
    String PROPERTY_VALIDATOR_ID = "validator.id";
    
    /***
     * Each {@link Validator} may have a service property with name {@code validator.severity} of type {@link Integer}. This is taken as the severity of all 
     * {@link ValidationFailure}s constructed by this {@link Validator} in case the model has not overwritten the severity. 
     * If this property is not set the default severity is being used.
     */
    String PROPERTY_VALIDATOR_SEVERITY = "validator.severity";
}
