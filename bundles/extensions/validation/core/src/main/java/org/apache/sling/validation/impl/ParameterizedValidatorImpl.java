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
package org.apache.sling.validation.impl;

import javax.annotation.Nonnull;

import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.validation.api.ParameterizedValidator;
import org.apache.sling.validation.api.Validator;
import org.apache.sling.validation.impl.util.ValidatorTypeUtil;

public class ParameterizedValidatorImpl implements ParameterizedValidator {
    private final @Nonnull Validator<?> validator;
    private final @Nonnull ValueMap parameters;
    private final @Nonnull Class<?> type;
    
    public ParameterizedValidatorImpl(@Nonnull Validator<?> validator, @Nonnull ValueMap parameters) {
        super();
        this.validator = validator;
        this.parameters = parameters;
        // cache type information as this is using reflection
        this.type = ValidatorTypeUtil.getValidatorType(validator);
    }

    /* (non-Javadoc)
     * @see org.apache.sling.validation.impl.ParameterizedValidator#getValidator()
     */
    @Override
    public @Nonnull Validator<?> getValidator() {
        return validator;
    }
    
    /* (non-Javadoc)
     * @see org.apache.sling.validation.impl.ParameterizedValidator#getParameters()
     */
    @Override
    public @Nonnull ValueMap getParameters() {
        return parameters;
    }
    
    /* (non-Javadoc)
     * @see org.apache.sling.validation.impl.ParameterizedValidator#getType()
     */
    @Override
    public @Nonnull Class<?> getType() {
        return type;
    }
}
