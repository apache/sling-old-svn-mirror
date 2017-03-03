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
package org.apache.sling.validation.impl.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.sling.validation.model.ChildResource;
import org.apache.sling.validation.model.ResourceProperty;
import org.apache.sling.validation.model.ValidationModel;

public class ValidationModelImpl implements ValidationModel {

    private final @Nonnull List<ResourceProperty> resourceProperties;
    private final @Nonnull String validatedResourceType;
    private final @Nonnull Collection<String> applicablePaths;
    private final @Nonnull List<ChildResource> children;
    private final @Nonnull String source;

    /**
     * Only used from {@link ValidationModelBuilder}
     * Copy all modifiable properties.
     * @param resourceProperties
     * @param validatedResourceType
     * @param applicablePaths
     * @param children
     * @param source a string identifying the model's source (e.g. a resource path)
     */
    ValidationModelImpl(@Nonnull List<ResourceProperty> resourceProperties, @Nonnull String validatedResourceType,
                              Collection<String> applicablePaths, @Nonnull List<ChildResource> children, @Nonnull String source) {
        this.resourceProperties = new ArrayList<>(resourceProperties);
        this.validatedResourceType = validatedResourceType;
        
        if (resourceProperties.isEmpty() && children.isEmpty()) {
            throw new IllegalArgumentException("Neither children nor properties set in validation model for " + validatedResourceType + "'");
        }
        // if this collection is empty, make it contain on entry with the emty string (means no restriction)
        if (applicablePaths.isEmpty()) {
            this.applicablePaths = Collections.singletonList("");
        } else {
            this.applicablePaths = new ArrayList<>(applicablePaths);
        }
        this.children = new ArrayList<>(children);
        this.source = source;
    }

    @Override
    public @Nonnull List<ResourceProperty> getResourceProperties() {
        return resourceProperties;
    }

    @Override
    public @Nonnull String getValidatedResourceType() {
        return validatedResourceType;
    }

    @Override
    public @Nonnull Collection<String> getApplicablePaths() {
        return applicablePaths;
    }

    @Override
    public @Nonnull List<ChildResource> getChildren() {
        return children;
    }

    @Override
    public @Nonnull String getSource() {
        return source;
    }

    @Override
    public String toString() {
        return "ValidationModelImpl [resourceProperties=" + resourceProperties + ", validatedResourceType=" + validatedResourceType
                + ", applicablePaths=" + applicablePaths + ", children=" + children + ", source="+ source +"]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((applicablePaths == null) ? 0 : applicablePaths.hashCode());
        result = prime * result + ((children == null) ? 0 : children.hashCode());
        result = prime * result + ((resourceProperties == null) ? 0 : resourceProperties.hashCode());
        result = prime * result + ((source == null) ? 0 : source.hashCode());
        result = prime * result + ((validatedResourceType == null) ? 0 : validatedResourceType.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ValidationModelImpl other = (ValidationModelImpl) obj;
        if (!applicablePaths.equals(other.applicablePaths))
            return false;
        if (!children.equals(other.children))
            return false;
        if (!resourceProperties.equals(other.resourceProperties))
            return false;
        if (!source.equals(other.source))
            return false;
        if (!validatedResourceType.equals(other.validatedResourceType))
            return false;
        return true;
    }

    
}
