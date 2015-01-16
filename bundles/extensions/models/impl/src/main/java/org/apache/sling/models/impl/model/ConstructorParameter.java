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

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.util.Arrays;

import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.spi.injectorspecific.StaticInjectAnnotationProcessorFactory;

/**
 * Constructor parameters aren't normally accessible using the
 * AnnotatedElement. This class acts as a facade to ease
 * compatibility with field and method injection.
 */
public class ConstructorParameter extends AbstractInjectableElement {

    private final Type parameterType;
    private final Type genericType;
    private final boolean isPrimitive;
    private final int parameterIndex;

    public ConstructorParameter(Annotation[] annotations, Type parameterType, Type genericType, boolean isPrimitive,
            int parameterIndex, StaticInjectAnnotationProcessorFactory[] processorFactories, DefaultInjectionStrategy defaultInjectionStrategy) {
        super(new FakeAnnotatedElement(annotations, parameterIndex), genericType, null, processorFactories, defaultInjectionStrategy);
        this.parameterType = parameterType;
        this.genericType = genericType;
        this.isPrimitive = isPrimitive;
        this.parameterIndex = parameterIndex;
    }

    public Type getGenericType() {
        return this.genericType;
    }

    public Type getParameterType() {
        return this.parameterType;
    }

    public boolean isPrimitive() {
        return isPrimitive;
    }
    
    public int getParameterIndex() {
        return this.parameterIndex;
    }
    
    @Override
    public String toString() {
        return "Parameter" + this.parameterIndex + "[" + this.genericType.toString() + "]";
    }

    public static class FakeAnnotatedElement implements AnnotatedElement {
        
        private final Annotation[] annotations;
        private final int parameterIndex;
        
        public FakeAnnotatedElement(Annotation[] annotations, int parameterIndex) {
            this.annotations = annotations;
            this.parameterIndex = parameterIndex;
        }

        @Override
        public boolean isAnnotationPresent(Class<? extends Annotation> paramClass) {
            return getAnnotation(paramClass) != null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T extends Annotation> T getAnnotation(Class<T> paramClass) {
            for (Annotation annotation : this.annotations) {
                if (paramClass.isInstance(annotation)) {
                    return (T)annotation;
                }
            }
            return null;
        }

        @Override
        public Annotation[] getAnnotations() {
            return annotations;
        }

        @Override
        public Annotation[] getDeclaredAnnotations() {
            return annotations;
        }
        
        public int getParameterIndex() {
            return this.parameterIndex;
        }

        @Override
        public String toString() {
            return "FakeAnnotatedElement [annotations=" + Arrays.toString(annotations) + ", parameterIndex="
                    + parameterIndex + "]";
        }

    }
   
}
