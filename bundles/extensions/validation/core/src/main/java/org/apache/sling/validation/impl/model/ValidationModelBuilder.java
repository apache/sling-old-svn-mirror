/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.validation.impl.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.sling.validation.model.ChildResource;
import org.apache.sling.validation.model.ResourceProperty;
import org.apache.sling.validation.model.ValidationModel;

/**
 * Builder to instantiate a {@link ValidationModel}
 *
 */
public class ValidationModelBuilder {

    private final @Nonnull List<ResourceProperty> resourceProperties;
    private final @Nonnull List<ChildResource> children;
    private final Collection<String> applicablePaths;
    
    public ValidationModelBuilder() {
        resourceProperties = new ArrayList<ResourceProperty>();
        children = new ArrayList<ChildResource>();
        applicablePaths = new ArrayList<String>();
    }
    
    public @Nonnull ValidationModelBuilder resourceProperty(@Nonnull ResourceProperty resourceProperty) {
        resourceProperties.add(resourceProperty);
        return this;
    }
    
    public @Nonnull ValidationModelBuilder childResource(@Nonnull ChildResource childResource) {
        children.add(childResource);
        return this;
    }
    
    public @Nonnull ValidationModelBuilder setApplicablePath(@Nonnull String applicablePath) {
        applicablePaths.clear();
        applicablePaths.add(applicablePath);
        return this;
    }
    
    public @Nonnull ValidationModelBuilder addApplicablePath(@Nonnull String applicablePath) {
        applicablePaths.add(applicablePath);
        return this;
    }
    
    public @Nonnull ValidationModel build(@Nonnull String validatedResourceType) {
        return new ValidationModelImpl(resourceProperties, validatedResourceType, applicablePaths.toArray(new String[0]), children);
    }
}
