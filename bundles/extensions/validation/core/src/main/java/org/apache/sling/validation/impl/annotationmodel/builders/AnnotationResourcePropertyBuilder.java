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
package org.apache.sling.validation.impl.annotationmodel.builders;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.apache.sling.validation.annotations.Validate;
import org.apache.sling.validation.impl.model.ResourcePropertyBuilder;
import org.apache.sling.validation.model.ResourceProperty;

/**
 * The Annotation based resource property builder.
 */
public class AnnotationResourcePropertyBuilder extends AbstractAnnotationBuilder {

    private ResourcePropertyBuilder builder;

    /**
     * Constructor.
     */
    public AnnotationResourcePropertyBuilder() {
        builder = new ResourcePropertyBuilder();
    }

    /**
     * Build resource property based on field annotations.
     *
     * @param defaultInjectionStrategy the default injection strategy
     * @param field                    the field
     * @return the resource property
     */
    public ResourceProperty build(DefaultInjectionStrategy defaultInjectionStrategy, Field field) {

        if (field.isAnnotationPresent(Validate.class)) {
            addValidator(field.getAnnotation(Validate.class));
        }
        setRegex(field);
        if (field.isAnnotationPresent(ValueMapValue.class)) {
            ValueMapValue valueMapValue = field.getAnnotation(ValueMapValue.class);
            if (isOptional(defaultInjectionStrategy, valueMapValue.injectionStrategy())) {
                builder.optional();
            }
            if (isMultiple(field)) {
                builder.multiple();
            }
            setName(field, valueMapValue.name());
        }

        return builder.build(getName());
    }

    private void addValidator(Validate fieldValidate) {

        if (StringUtils.isNotBlank(fieldValidate.validatorId()) && fieldValidate.properties().length > 0) {
            Map<String, Object> arguments = Arrays.asList(fieldValidate.properties())
                    .parallelStream()
                    .map(str -> str.split("="))
                    .filter(str -> str.length == 2)
                    .collect(Collectors.toMap(keyvalue -> keyvalue[0], keyvalue -> keyvalue[1]));

            builder.validator(fieldValidate.validatorId(), fieldValidate.severity(), arguments);
        }

    }
}
