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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.impl.ReflectionUtil;
import org.apache.sling.models.spi.injectorspecific.StaticInjectAnnotationProcessorFactory;

public class ModelClass<ModelType> {

    private final Class<ModelType> type;
    private final Model modelAnnotation;
    final DefaultInjectionStrategy defaultInjectionStrategy;
    private volatile ModelClassConstructor[] constructors;
    private volatile InjectableField[] injectableFields;
    private volatile InjectableMethod[] injectableMethods;

    public ModelClass(Class<ModelType> type, StaticInjectAnnotationProcessorFactory[] processorFactories) {
        this.type = type;
        this.modelAnnotation = type.getAnnotation(Model.class);
        if (modelAnnotation == null) {
            defaultInjectionStrategy = DefaultInjectionStrategy.REQUIRED;
        } else {
            defaultInjectionStrategy = modelAnnotation.defaultInjectionStrategy();
        }
        updateProcessorFactories(processorFactories);
    }
    
    /**
     * Updates processor factories after the model class was instantiated.
     * @param processorFactories Static injector annotation processor factories
     */
    public void updateProcessorFactories(StaticInjectAnnotationProcessorFactory[] processorFactories) {
        this.constructors = getConstructors(type, processorFactories, defaultInjectionStrategy);
        this.injectableFields = getInjectableFields(type, processorFactories, defaultInjectionStrategy);
        this.injectableMethods = getInjectableMethods(type, processorFactories, defaultInjectionStrategy);
    }
    
    @SuppressWarnings("unchecked")
    private static ModelClassConstructor[] getConstructors(Class<?> type, StaticInjectAnnotationProcessorFactory[] processorFactories, DefaultInjectionStrategy defaultInjectionStrategy) {
        if (type.isInterface()) {
            return new ModelClassConstructor[0];
        }
        Constructor<?>[] constructors = type.getConstructors();
        
        // sort the constructor list in order from most params to least params, and constructors with @Inject annotation first
        Arrays.sort(constructors, new ParameterCountInjectComparator());

        ModelClassConstructor[] array = new ModelClassConstructor[constructors.length];
        for (int i=0; i<array.length; i++) {
            array[i] = new ModelClassConstructor(constructors[i], processorFactories, defaultInjectionStrategy);
        }
        return array;
    }

    private static InjectableField[] getInjectableFields(Class<?> type, StaticInjectAnnotationProcessorFactory[] processorFactories, DefaultInjectionStrategy defaultInjectionStrategy) {
        if (type.isInterface()) {
            return new InjectableField[0];
        }
        List<Field> injectableFields = ReflectionUtil.collectInjectableFields(type);
        InjectableField[] array = new InjectableField[injectableFields.size()];
        for (int i=0; i<array.length; i++) {
            array[i] = new InjectableField(injectableFields.get(i), processorFactories, defaultInjectionStrategy);
        }
        return array;
    }

    private static InjectableMethod[] getInjectableMethods(Class<?> type, StaticInjectAnnotationProcessorFactory[] processorFactories, DefaultInjectionStrategy defaultInjectionStrategy) {
        if (!type.isInterface()) {
            return new InjectableMethod[0];
        }
        List<Method> injectableMethods = ReflectionUtil.collectInjectableMethods(type);
        InjectableMethod[] array = new InjectableMethod[injectableMethods.size()];
        for (int i=0; i<array.length; i++) {
            array[i] = new InjectableMethod(injectableMethods.get(i), processorFactories, defaultInjectionStrategy);
        }
        return array;
    }

    public Class<ModelType> getType() {
        return this.type;
    }
    
    public Model getModelAnnotation() {
        return this.modelAnnotation;
    }
    
    public boolean hasModelAnnotation() {
        return this.modelAnnotation != null;
    }
    
    public ModelClassConstructor[] getConstructors() {
        return constructors;
    }

    public InjectableField[] getInjectableFields() {
        return this.injectableFields;
    }

    public InjectableMethod[] getInjectableMethods() {
        return this.injectableMethods;
    }

}
