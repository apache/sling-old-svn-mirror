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

import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;

/**
 * The Abstract annotation builder, providing common functionality for Annotation Based Resource Property and Child Resource Builders.
 * Common properties:
 *
 *  - name is taken either from @ChildResource, @ValueMapValue annotations' name property or from actual field name.
 *  i.e. @ChildResource(name="childName") or @ValueMapValue(name="propertyName")
 *
 *
 */
public abstract class AbstractAnnotationBuilder {

    private String name;

    /**
     * Checks if property should be Required or Optional.
     *
     * @param defaultInjectionStrategy the default injection strategy
     * @param injectionStrategy        the injection strategy
     * @return the boolean
     */
    public boolean isOptional(DefaultInjectionStrategy defaultInjectionStrategy, InjectionStrategy injectionStrategy) {
        return injectionStrategy.equals(InjectionStrategy.OPTIONAL) || (injectionStrategy.equals(InjectionStrategy.DEFAULT)
                && defaultInjectionStrategy.equals(DefaultInjectionStrategy.OPTIONAL));
    }

    /**
     * If field is of Array | Collection type,
     * it is considered as Multiple
     * @param field the field
     * @return the boolean
     */
    public boolean isMultiple(Field field) {
        return (field.getType().isArray() || Collection.class.isAssignableFrom(field.getType()));
    }

    /**
     * Extracts name from Annotation name, if available,
     * otherwise it takes it from Field.
     *
     * @param field          the field
     * @param annotationName the annotation name
     */
    public void setName(Field field, String annotationName) {
        if (annotationName.isEmpty()) {
            name = field.getName();
        } else {
            name = annotationName;
        }
    }

    /**
     * Getter for name property.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }
}
