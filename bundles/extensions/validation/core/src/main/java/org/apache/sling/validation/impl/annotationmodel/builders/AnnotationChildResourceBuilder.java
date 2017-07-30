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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.validation.impl.model.ChildResourceImpl;
import org.apache.sling.validation.model.ChildResource;
import org.apache.sling.validation.model.ResourceProperty;

/**
 * The Annotation based child resources builder.
 * It creates Child Resource and its' arguments/children according to declared field and its' annotations.
 * Child Resources are considered Injected Sling Model fields.
 */
public class AnnotationChildResourceBuilder extends AbstractAnnotationBuilder {

    private static final String ANYTHING_REGEX = ".*";

    @Nonnull
    private final List<ResourceProperty> resourceProperties = new ArrayList<>();
    @Nonnull
    private final List<ChildResource> children = new ArrayList<>();

    /**
     * Build child resource
     *
     * @param field                    the field
     * @param defaultInjectionStrategy the default injection strategy
     * @return the child resource
     */
    public @Nonnull ChildResource build(@Nonnull Field field, DefaultInjectionStrategy defaultInjectionStrategy) {
        org.apache.sling.models.annotations.injectorspecific.ChildResource child = field
                .getAnnotation(org.apache.sling.models.annotations.injectorspecific.ChildResource.class);
        setName(field, child.name());

        if(isMultiple(field)) {
            ChildResource childResource = new ChildResourceImpl(StringUtils.EMPTY, ANYTHING_REGEX, !isOptional(defaultInjectionStrategy, child.injectionStrategy()), resourceProperties, children);
            return new ChildResourceImpl(getName(), StringUtils.EMPTY, !isOptional(defaultInjectionStrategy, child.injectionStrategy()), Collections.emptyList(), Collections.singletonList(childResource));
        }
        return new ChildResourceImpl(getName(), StringUtils.EMPTY, !isOptional(defaultInjectionStrategy, child.injectionStrategy()), resourceProperties, children);
    }

    /**
     * Add resource arguments.
     *
     * @param properties the arguments
     * @return the annotation child resource builder
     */
    public AnnotationChildResourceBuilder addResourceProperties(@Nonnull Collection<ResourceProperty> properties) {
        this.resourceProperties.addAll(properties);
        return this;
    }

    /**
     * Add child resources.
     *
     * @param childResources the child resources
     * @return the annotation child resource builder
     */
    public AnnotationChildResourceBuilder addChildResources(Collection<ChildResource> childResources) {
        children.addAll(childResources);
        return this;
    }


}
