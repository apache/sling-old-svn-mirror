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
 * It creates Resource Property according to declared field and its' annotation.
 * It is re-using existing ResourcePropertyBuilder functionality to create the actual Resource Property
 */
public class AnnotationResourcePropertyBuilder extends AbstractAnnotationBuilder {

    private static final int SPLIT_LENGTH = 2;
    private ResourcePropertyBuilder builder;

    /**
     * Build resource property based on field annotations.
     *
     * @param defaultInjectionStrategy the default injection strategy
     * @param field                    the field
     * @return the resource property
     */
    public ResourceProperty build(DefaultInjectionStrategy defaultInjectionStrategy, Field field) {
        builder = new ResourcePropertyBuilder();

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

    /**
     * Adds Validator and it's arguments for Resource Property.
     * @param validate annotation
     */
    private void addValidator(Validate validate) {

        if (StringUtils.isNotBlank(validate.validatorId()) && validate.properties().length > 0) {
            Map<String, Object> arguments = Arrays.asList(validate.properties())
                    .parallelStream()
                    .map(str -> str.split("="))
                    .filter(str -> str.length == SPLIT_LENGTH)
                    .collect(Collectors.toMap(keyvalue -> keyvalue[0], keyvalue -> keyvalue[1]));

            builder.validator(validate.validatorId(), validate.severity(), arguments);
        }

    }
}
