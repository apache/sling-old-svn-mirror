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

/** @author karolis.mackevicius@netcentric.biz
 * @since 05/04/17 */
public class AnnotationValidationModelBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(ValidationPackageBundleListener.class);
    private static final String[] EMPTY_PATHS = {};

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

    private List<org.apache.sling.validation.model.ChildResource> getChildResources(@Nonnull Class<?> clazz) {
        return Stream.of(clazz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(ChildResource.class))
                .filter(this::isModelClassField)
                .map(this::buildChildResource)
                .collect(Collectors.toList());
    }

    private boolean isModelClassField(Field field) {
        return getClass(field).isAnnotationPresent(Model.class);
    }

    private org.apache.sling.validation.model.ChildResource buildChildResource(@Nonnull Field field) {

        Class<?> clazz = getClass(field);

        Model model = clazz.getDeclaredAnnotation(Model.class);

        return new AnnotationChildResourceBuilder()
                .addResourceProperties(getResourceProperties(clazz, model.defaultInjectionStrategy()))
                .addChildResources(getChildResources(clazz))
                .build(field, model.defaultInjectionStrategy());

    }

    private List<ResourceProperty> getResourceProperties(@Nonnull Class<?> clazz, DefaultInjectionStrategy defaultInjectionStrategy) {
        return Stream.of(clazz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(ValueMapValue.class))
                .map(field -> new AnnotationResourcePropertyBuilder().build(defaultInjectionStrategy, field))
                .collect(Collectors.toList());
    }

    private Class<?> getClass(Field field) {
        Class<?> clazz = field.getType();
        if (Collection.class.isAssignableFrom(field.getType())) {
            ParameterizedType stringListType = (ParameterizedType) field.getGenericType();
            clazz = (Class<?>) stringListType.getActualTypeArguments()[0];
        }
        return clazz;
    }
}
