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
package org.apache.sling.validation.impl;

import java.util.List;
import java.util.regex.Pattern;

import org.apache.sling.validation.api.ParameterizedValidator;
import org.apache.sling.validation.api.ResourceProperty;

public class ResourcePropertyImpl implements ResourceProperty {

    private String name;
    private boolean isMultiple;
    private boolean isRequired;
    private List<ParameterizedValidator> validators;
    private Pattern namePattern;
    
    public ResourcePropertyImpl(String name, String nameRegex, boolean isMultiple, boolean isRequired, List<ParameterizedValidator> validators) {
        if (nameRegex != null) {
            this.name = null;
            this.namePattern = Pattern.compile(nameRegex);
        } else {
            this.name = name;
            this.namePattern = null;
        }
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
    public List<ParameterizedValidator> getValidators() {
        return validators;
    }

    
}
