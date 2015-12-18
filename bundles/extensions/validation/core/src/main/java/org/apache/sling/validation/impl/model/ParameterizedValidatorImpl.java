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
package org.apache.sling.validation.impl.model;

import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.validation.impl.util.ValidatorTypeUtil;
import org.apache.sling.validation.model.ParameterizedValidator;
import org.apache.sling.validation.spi.Validator;

public class ParameterizedValidatorImpl implements ParameterizedValidator {
    private final @Nonnull Validator<?> validator;
    private final @Nonnull Map<String, Object> parameters;
    private final @Nonnull Class<?> type;
    
    /**
     * 
     * Only the map has proper support for equals (see https://issues.apache.org/jira/browse/SLING-4784)
     * @param validator
     * @param parameters
     */
    public ParameterizedValidatorImpl(@Nonnull Validator<?> validator, @Nonnull Map<String, Object> parameters) {
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
        return new ValueMapDecorator(parameters);
    }
    
    /* (non-Javadoc)
     * @see org.apache.sling.validation.impl.ParameterizedValidator#getType()
     */
    @Override
    public @Nonnull Class<?> getType() {
        return type;
    }

    @Override
    public String toString() {
        return "ParameterizedValidatorImpl [validator=" + validator + ", parameters=" + parameters + ", type=" + type
                + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((parameters == null) ? 0 : parameters.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((validator == null) ? 0 : validator.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ParameterizedValidatorImpl other = (ParameterizedValidatorImpl) obj;
        if (!parameters.equals(other.parameters))
            return false;
        if (!type.equals(other.type))
            return false;
        if (!validator.getClass().getName().equals(other.validator.getClass().getName()))
            return false;
        return true;
    }
    
}
