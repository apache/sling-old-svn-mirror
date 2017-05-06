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

/**
 * The type Abstract annotation builder.
 *
 * @author karolis.mackevicius @netcentric.biz
 * @since 01 /05/17
 */
abstract class AbstractAnnotationBuilder {

    private boolean optional;
    private boolean multiple;
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

    void setOptional(DefaultInjectionStrategy defaultInjectionStrategy, InjectionStrategy injectionStrategy) {
        optional = injectionStrategy.equals(InjectionStrategy.OPTIONAL) || (injectionStrategy.equals(InjectionStrategy.DEFAULT)
                && defaultInjectionStrategy.equals(DefaultInjectionStrategy.OPTIONAL));
    }

    void setMultiple(Field field) {
        multiple = (field.getType().isArray() || Collection.class.isAssignableFrom(field.getType()));
    }

    void setName(Field field, String annotationName) {
        if (annotationName.isEmpty()) {
            name = field.getName();
        } else {
            name = annotationName;
        }
    }

    boolean isOptional() {
        return optional;
    }

    boolean isMultiple() {
        return multiple;
    }

    String getNameRegex() {
        return nameRegex;
    }

    String getName() {
        return name;
    }
}
