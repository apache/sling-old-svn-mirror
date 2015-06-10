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

import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.validation.model.ChildResource;
import org.apache.sling.validation.model.ResourceProperty;
import org.apache.sling.validation.model.ValidationModel;

public class ValidationModelImpl implements ValidationModel {

    private final @Nonnull List<ResourceProperty> resourceProperties;
    private final @Nonnull String validatedResourceType;
    private final @Nonnull String[] applicablePaths;
    private final @Nonnull List<ChildResource> children;

    public ValidationModelImpl(@Nonnull List<ResourceProperty> resourceProperties, @Nonnull String validatedResourceType,
                              String[] applicablePaths, @Nonnull List<ChildResource> children) {
        this.resourceProperties = resourceProperties;
        this.validatedResourceType = validatedResourceType;
        // if this property was not set or is an empty array...
        if (applicablePaths == null || applicablePaths.length == 0) {
            // ...set this to the empty string (which matches all paths)
            this.applicablePaths = new String[] {""};
        } else {
            for (String applicablePath : applicablePaths) {
                if (StringUtils.isBlank(applicablePath)) {
                    throw new IllegalArgumentException("applicablePaths may not contain empty values!");
                }
            }
            this.applicablePaths = applicablePaths;
        }
        this.children = children;
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
    public @Nonnull String[] getApplicablePaths() {
        return applicablePaths;
    }

    @Override
    public @Nonnull List<ChildResource> getChildren() {
        return children;
    }

    @Override
    public String toString() {
        return "ResourceValidationModel [resourceProperties=" + resourceProperties + ", validatedResourceType="
                + validatedResourceType + ", applicablePaths=" + Arrays.toString(applicablePaths) + ", children=" + children + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(applicablePaths);
        result = prime * result + ((children == null) ? 0 : children.hashCode());
        result = prime * result + ((resourceProperties == null) ? 0 : resourceProperties.hashCode());
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
        if (!Arrays.equals(applicablePaths, other.applicablePaths))
            return false;
        if (!children.equals(other.children))
            return false;
        if (!resourceProperties.equals(other.resourceProperties))
            return false;
        if (!validatedResourceType.equals(other.validatedResourceType))
            return false;
        return true;
    }
}
