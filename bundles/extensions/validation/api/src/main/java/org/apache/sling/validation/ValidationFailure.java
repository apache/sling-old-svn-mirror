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

import javax.annotation.Nonnull;

public interface ValidationFailure {

    /**
     * @return the failure message
     */
    public @Nonnull String getMessage();
    
    /**
     * Returns the relative location of the property/resource/value which triggered this validation failure.
     * The location 
     * <ul>
     * <li>is relative to the resource given in the first parameter in case it was returned by {@link ValidationService#validate(org.apache.sling.api.resource.Resource, org.apache.sling.validation.model.ValidationModel)} or {@link ValidationService#validateResourceRecursively(org.apache.sling.api.resource.Resource, boolean, org.apache.commons.collections.Predicate, boolean)} or</li>
     * <li>contains just the value name in case it was returned by {@link ValidationService#validate(org.apache.sling.api.resource.ValueMap, org.apache.sling.validation.model.ValidationModel)}</li>
     * </ul>
     * @return the location (usually the validated resource's property path).
     */
    public @Nonnull String getLocation();
    
}
