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

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.validation.ValidationResult;
import org.apache.sling.validation.ValidationService;
import org.apache.sling.validation.exceptions.SlingValidationException;

import aQute.bnd.annotation.ConsumerType;

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
     * <table> 
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
     * @param valueMap all properties of the validated resource/valuemap (only used for validations considering multiple properties), never {@code null}.
     * @param resource the resource on which the validation was triggered. {@code null} in case the validation was triggered on a {@link ValueMap} (via {@link ValidationService#validate(ValueMap, org.apache.sling.validation.model.ValidationModel)}).
     * @param arguments the parameterization of the validator. Never {@code null} but might be the empty map.
     * @return the validation result (encapsulates the validation status as well as messages).
     * @throws org.apache.sling.validation.exceptions.SlingValidationException if some expected arguments are missing from the arguments map
     */
    @Nonnull ValidationResult validate(@Nonnull T data, @Nonnull ValueMap valueMap, Resource resource, @Nonnull ValueMap arguments) throws SlingValidationException;
}
