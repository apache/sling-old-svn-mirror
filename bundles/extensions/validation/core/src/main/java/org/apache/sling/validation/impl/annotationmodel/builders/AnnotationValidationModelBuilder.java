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
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ChildResource;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.apache.sling.validation.annotations.ValidationPaths;
import org.apache.sling.validation.impl.annotationmodel.ValidationPackageBundleListener;
import org.apache.sling.validation.impl.model.ValidationModelBuilder;
import org.apache.sling.validation.model.ResourceProperty;
import org.apache.sling.validation.model.ValidationModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Annotation based validation model builder.
 * It is building Validation Models from Sling Model classes.
 * Validation Model can have:
 *  - applicable paths, which is taken from @ValidationPaths annotation, applicable on Sling Model class level.
 *  - resource properties, which are fields injected with @ValueMapValue annotation
 *  - children resources, which are fields injected with @ChildResource annotation AND are Sling Model classes
 */
public class AnnotationValidationModelBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(ValidationPackageBundleListener.class);
    private static final String[] EMPTY_PATHS = {};

    /**
     * Build Validation Models for a given Sling Model class.
     * Assuming, that class passed as parameter has Model annotation.
     *
     * @param clazz the clazz for which validation model is built
     * @return the list of validation models for given class
     */
    public List<ValidationModel> build(@Nonnull Class<?> clazz) {
        List<ValidationModel> validationModels = new ArrayList<>();
        ValidationModelBuilder modelBuilder = new ValidationModelBuilder();

        Model model = clazz.getAnnotation(Model.class);

        String[] paths = Optional.ofNullable(clazz.getAnnotation(ValidationPaths.class))
                .map(ValidationPaths::paths)
                .orElse(EMPTY_PATHS);

        modelBuilder.addApplicablePaths(paths);

        DefaultInjectionStrategy defaultInjectionStrategy = model.defaultInjectionStrategy();
        LOG.debug("Validation Model for: {}", clazz.getName());

        List<ResourceProperty> resourceProperties = getResourceProperties(clazz, defaultInjectionStrategy);
        modelBuilder.resourceProperties(resourceProperties);

        List<org.apache.sling.validation.model.ChildResource> childResources = getChildResources(clazz);
        modelBuilder.childResources(childResources);

        for (String resourceType : model.resourceType()) {
            validationModels.add(modelBuilder.build(resourceType, StringUtils.EMPTY));
        }

        return validationModels;
    }

    /**
     * It builds Child Resources for a given class.
     * Method filters declared @ChildResource fields which are Sling Models
     * and builds child resource properties.
     * @param clazz
     * @return list of Child Resources for a given class
     */
    private List<org.apache.sling.validation.model.ChildResource> getChildResources(@Nonnull Class<?> clazz) {
        return Stream.of(clazz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(ChildResource.class))
                .filter(this::isSlingModelField)
                .map(this::buildChildResource)
                .collect(Collectors.toList());
    }

    private boolean isSlingModelField(Field field) {
        return getClass(field).isAnnotationPresent(Model.class);
    }

    /**
     * Builds Child Resource from a given Field.
     * @param field which is a Sling Model.
     * @return Child Resource
     */
    private org.apache.sling.validation.model.ChildResource buildChildResource(@Nonnull Field field) {

        Class<?> clazz = getClass(field);

        Model model = clazz.getDeclaredAnnotation(Model.class);

        return new AnnotationChildResourceBuilder()
                .addResourceProperties(getResourceProperties(clazz, model.defaultInjectionStrategy()))
                .addChildResources(getChildResources(clazz))
                .build(field, model.defaultInjectionStrategy());

    }

    /**
     * Builds Resource Properties for given class from @ValueMapValue annotated injected fields.
     * @param clazz
     * @param defaultInjectionStrategy class'es default injection strategy.
     * @return List of Resource Properties
     */
    private List<ResourceProperty> getResourceProperties(@Nonnull Class<?> clazz, DefaultInjectionStrategy defaultInjectionStrategy) {
        return Stream.of(clazz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(ValueMapValue.class))
                .map(field -> new AnnotationResourcePropertyBuilder().build(defaultInjectionStrategy, field))
                .collect(Collectors.toList());
    }

    /**
     * Get Class type from passed field. It takes into account generic types.
     * @param field
     * @return Class
     */
    private Class<?> getClass(Field field) {
        Class<?> clazz = field.getType();
        if (Collection.class.isAssignableFrom(field.getType())) {
            ParameterizedType stringListType = (ParameterizedType) field.getGenericType();
            clazz = (Class<?>) stringListType.getActualTypeArguments()[0];
        }
        return clazz;
    }
}
