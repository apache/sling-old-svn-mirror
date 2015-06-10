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

import java.util.List;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * Describes a {@link org.apache.sling.api.resource.Resource} property.
 */
public interface ResourceProperty {

    /**
     * Returns the name of this property.
     * Either this method or {@link getNamePattern} must not return {@code null}
     *
     * @return the name or {@code null}
     */
    @CheckForNull String getName();
    
    /**
     * Returns the name pattern for this property. Either this method or {@link getName} must not return {@code null}
     *
     * @return the name pattern (if one is set) or {@code null)
     */
    @CheckForNull Pattern getNamePattern();

    /**
     * Returns {@code true} if this property is expected to be a multiple property (e.g. array of values).
     *
     * @return {@code true} if the  property is multiple, {@code false} otherwise
     */
    boolean isMultiple();

    /**
     * Returns {@code true} if at least one property matching the name/namePattern is required.
     * 
     * @return {@code true} if the property is required, {@code false} otherwise
     */
    boolean isRequired();

    /**
     * Returns a list of {@link ParameterizedValidator}s which should be applied on this property.
     *
     * @return the list of validators
     */
    @Nonnull List<ParameterizedValidator> getValidators();
}
