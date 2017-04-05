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
package org.apache.sling.models.impl.model;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.factory.ModelClassException;
import org.apache.sling.models.impl.ReflectionUtil;
import org.apache.sling.models.impl.Result;
import org.apache.sling.models.spi.injectorspecific.StaticInjectAnnotationProcessorFactory;

public class InjectableField extends AbstractInjectableElement {
    
    private final Field field;
    
    public InjectableField(Field field, StaticInjectAnnotationProcessorFactory[] processorFactories, DefaultInjectionStrategy defaultInjectionStrategy) {
        super(field, ReflectionUtil.mapPrimitiveClasses(field.getGenericType()), field.getName(), processorFactories, defaultInjectionStrategy);
        this.field = field;
    }

    public RuntimeException set(Object createdObject, Result<Object> result) {
        synchronized (field) {
            boolean accessible = field.isAccessible();
            try {
                if (!accessible) {
                    field.setAccessible(true);
                }
                field.set(createdObject, result.getValue());
            } catch (Exception e) {
                return new ModelClassException("Could not inject field due to reflection issues", e);
            } finally {
                if (!accessible) {
                    field.setAccessible(false);
                }
            }
        }
        return null;
    }

    public boolean isPrimitive() {
        return false;
    }

    public Class<?> getFieldType() {
        return field.getType();
    }

    public Type getFieldGenericType() {
        return field.getGenericType();
    }
}
