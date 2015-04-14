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
package org.apache.sling.validation.impl;

import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.validation.api.ChildResource;
import org.apache.sling.validation.api.ResourceProperty;
import org.apache.sling.validation.api.ValidationModel;

/**
 * 
 * @author kwindszus
 *
 */
public class ResourceValidationModel implements ValidationModel {

    private final @Nonnull Set<ResourceProperty> resourceProperties;
    private final @Nonnull String validatedResourceType;
    private final @Nonnull String[] applicablePaths;
    private final String path;
    private final @Nonnull List<ChildResource> children;

    public ResourceValidationModel(String path, @Nonnull Set<ResourceProperty> resourceProperties, @Nonnull String validatedResourceType,
                              String[] applicablePaths, @Nonnull List<ChildResource> children) {
        this.path = path;
        this.resourceProperties = resourceProperties;
        this.validatedResourceType = validatedResourceType;
        // if this property was not set
        if (applicablePaths == null) {
            // set this to the empty string (which matches all paths)
            this.applicablePaths = new String[] {""};
        } else {
            if (applicablePaths.length == 0) {
                throw new IllegalArgumentException("applicablePaths cannot be an empty array!");
            }
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
    public @Nonnull Set<ResourceProperty> getResourceProperties() {
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

    public String getPath() {
        return path;
    }
}
