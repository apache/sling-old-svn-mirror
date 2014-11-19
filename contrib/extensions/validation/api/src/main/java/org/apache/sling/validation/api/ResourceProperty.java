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

import java.util.Map;

/**
 * Describes a {@link org.apache.sling.api.resource.Resource} property.
 */
public interface ResourceProperty {

    /**
     * Returns the name of this property.
     *
     * @return the name
     */
    String getName();

    /**
     * Returns the type of data this property should contain.
     *
     * @return the type
     */
    Type getType();

    /**
     * Returns {@code true} if this property is expected to be a multiple property (e.g. array of values).
     *
     * @return {@code true} if the  property is multiple, {@code false} otherwise
     */
    boolean isMultiple();

    /**
     * Returns a map containing the validators that should be applied to this property together with the arguments for each validator, also
     * stored in a key-value map.
     *
     * @return the validators
     */
    Map<Validator, Map<String, String>> getValidators();
}
