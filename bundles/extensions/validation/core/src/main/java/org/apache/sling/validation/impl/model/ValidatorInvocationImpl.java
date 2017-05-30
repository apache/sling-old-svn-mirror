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

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.validation.model.ValidatorInvocation;

public class ValidatorInvocationImpl implements ValidatorInvocation {
    private final @Nonnull String id; 
    private final @Nonnull Map<String, Object> parameters;
    private final Integer severity;
    
    /**
     * 
     * Only the map has proper support for equals (see https://issues.apache.org/jira/browse/SLING-4784)
     * @param id
     * @param parameters
     * @param severity
     */
    public ValidatorInvocationImpl(@Nonnull String id, @Nonnull Map<String, Object> parameters, Integer severity) {
        super();
        this.id = id;
        this.parameters = parameters;
        this.severity = severity;
    }
    

    @Override
    public String getValidatorId() {
        return id;
    }

    @Override
    public @Nonnull ValueMap getParameters() {
        return new ValueMapDecorator(parameters);
    }

    @Override
    @CheckForNull
    public Integer getSeverity() {
        return severity;
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((parameters == null) ? 0 : parameters.hashCode());
        result = prime * result + ((severity == null) ? 0 : severity.hashCode());
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
        ValidatorInvocationImpl other = (ValidatorInvocationImpl) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (parameters == null) {
            if (other.parameters != null)
                return false;
        } else if (!parameters.equals(other.parameters))
            return false;
        if (severity == null) {
            if (other.severity != null)
                return false;
        } else if (!severity.equals(other.severity))
            return false;
        return true;
    }


    @Override
    public String toString() {
        return "ParameterizedValidatorImpl [id=" + id + ", parameters=" + parameters + ", severity=" + severity + "]";
    }

}
