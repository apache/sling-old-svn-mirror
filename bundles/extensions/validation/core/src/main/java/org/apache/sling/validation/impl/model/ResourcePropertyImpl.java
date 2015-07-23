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
package org.apache.sling.validation.impl.model;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.annotation.Nonnull;

import org.apache.sling.validation.model.ParameterizedValidator;
import org.apache.sling.validation.model.ResourceProperty;

public class ResourcePropertyImpl implements ResourceProperty {

    private final String name;
    private final boolean isMultiple;
    private final boolean isRequired;
    private final @Nonnull List<ParameterizedValidator> validators;
    private final Pattern namePattern;

    public ResourcePropertyImpl(@Nonnull String name, String nameRegex, boolean isMultiple, boolean isRequired,
            @Nonnull List<ParameterizedValidator> validators) throws IllegalArgumentException {
        if (nameRegex != null) {
            try {
                this.namePattern = Pattern.compile(nameRegex);
            } catch (PatternSyntaxException e) {
                throw new IllegalArgumentException("Invalid regex given", e);
            }
        } else {
            this.namePattern = null;
        }
        this.name = name;
        this.isMultiple = isMultiple;
        this.isRequired = isRequired;
        this.validators = validators;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Pattern getNamePattern() {
        return namePattern;
    }

    @Override
    public boolean isMultiple() {
        return isMultiple;
    }

    @Override
    public boolean isRequired() {
        return isRequired;
    }

    @Override
    public @Nonnull List<ParameterizedValidator> getValidators() {
        return validators;
    }

    @Override
    public String toString() {
        return "ResourcePropertyImpl [name=" + name + ", isMultiple=" + isMultiple + ", isRequired=" + isRequired
                + ", validators=" + validators + ", namePattern=" + namePattern + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (isMultiple ? 1231 : 1237);
        result = prime * result + (isRequired ? 1231 : 1237);
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((namePattern == null) ? 0 : namePattern.hashCode());
        result = prime * result + ((validators == null) ? 0 : validators.hashCode());
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
        ResourcePropertyImpl other = (ResourcePropertyImpl) obj;
        if (isMultiple != other.isMultiple)
            return false;
        if (isRequired != other.isRequired)
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (namePattern == null) {
            if (other.namePattern != null)
                return false;
        } else if (!namePattern.pattern().equals(other.namePattern.pattern()))
            return false;
        if (!validators.equals(other.validators))
            return false;
        return true;
    }

}
