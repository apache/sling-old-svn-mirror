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
import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.validation.annotations.Validate;

/** The Abstract annotation builder, providing common functionality for Annotation Based Builders. */
abstract class AbstractAnnotationBuilder {

    private String nameRegex;
    private String name;

    void setRegex(Field field) {
        if (field.isAnnotationPresent(Validate.class)) {
            Validate validate = field.getAnnotation(Validate.class);
            if (StringUtils.isNotBlank(validate.regex())) {
                this.nameRegex = validate.regex();
            }
        }
    }

    boolean isOptional(DefaultInjectionStrategy defaultInjectionStrategy, InjectionStrategy injectionStrategy) {
        return injectionStrategy.equals(InjectionStrategy.OPTIONAL) || (injectionStrategy.equals(InjectionStrategy.DEFAULT)
                && defaultInjectionStrategy.equals(DefaultInjectionStrategy.OPTIONAL));
    }

    boolean isMultiple(Field field) {
        return (field.getType().isArray() || Collection.class.isAssignableFrom(field.getType()));
    }

    void setName(Field field, String annotationName) {
        if (annotationName.isEmpty()) {
            name = field.getName();
        } else {
            name = annotationName;
        }
    }

    String getNameRegex() {
        return nameRegex;
    }

    String getName() {
        return name;
    }
}
