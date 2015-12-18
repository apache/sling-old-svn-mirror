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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.sling.validation.model.ParameterizedValidator;
import org.apache.sling.validation.model.ResourceProperty;
import org.apache.sling.validation.spi.Validator;

public class ResourcePropertyBuilder {

    public boolean optional;
    public boolean multiple;
    String nameRegex;
    final List<ParameterizedValidator> validators;

    public ResourcePropertyBuilder() {
        validators = new ArrayList<ParameterizedValidator>();
        this.nameRegex = null;
        this.optional = false;
        this.multiple = false;
    }

    public @Nonnull ResourcePropertyBuilder nameRegex(String nameRegex) {
        this.nameRegex = nameRegex;
        return this;
    }
    
    public @Nonnull ResourcePropertyBuilder validator(@Nonnull Validator<?> validator) {
        validators.add(new ParameterizedValidatorImpl(validator, new HashMap<String, Object>()));
        return this;
    }
    
    
    public @Nonnull ResourcePropertyBuilder validator(@Nonnull Validator<?> validator, String... parametersNamesAndValues) {
        if (parametersNamesAndValues.length % 2 != 0) {
            throw new IllegalArgumentException("array parametersNamesAndValues must be even! (first specify name then value, separated by comma)");
        }
        // convert to map
        Map<String, Object> parameterMap = new HashMap<String, Object>();
        for (int i=0; i<parametersNamesAndValues.length; i=i+2) {
            parameterMap.put(parametersNamesAndValues[i], parametersNamesAndValues[i+1]);
        }
        validators.add(new ParameterizedValidatorImpl(validator, parameterMap));
        return this;
    }

    public @Nonnull ResourcePropertyBuilder optional() {
        this.optional = true;
        return this;
    }
    
    public @Nonnull ResourcePropertyBuilder multiple() {
        this.multiple = true;
        return this;
    }
    
    public @Nonnull ResourceProperty build(String name) {
        return new ResourcePropertyImpl(name, nameRegex, multiple, !optional, validators);
    }
}
