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

import org.apache.sling.validation.api.ParameterizedValidator;
import org.apache.sling.validation.api.ResourceProperty;
import org.apache.sling.validation.api.Type;

public class ResourcePropertyImpl implements ResourceProperty {

    private String name;
    private Type type;
    private boolean isMultiple;
    private List<ParameterizedValidator> validators;

    public ResourcePropertyImpl(String name, Type type, List<ParameterizedValidator> validators) {
        this(name, type, false, validators);
    }

    public ResourcePropertyImpl(String name, Type type, boolean isMultiple, List<ParameterizedValidator> validators) {
        this.name = name;
        this.type = type;
        this.isMultiple = isMultiple;
        this.validators = validators;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public boolean isMultiple() {
        return isMultiple;
    }

    @Override
    public List<ParameterizedValidator> getValidators() {
        return validators;
    }
}
