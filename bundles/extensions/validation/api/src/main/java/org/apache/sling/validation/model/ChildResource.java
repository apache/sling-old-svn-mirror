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
package org.apache.sling.validation.model;

import java.util.List;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * Defines the validation rules for a child resource, allowing {@link ValidationModel}s to be applied to {@link
 * org.apache.sling.api.resource.Resource} trees.
 */
public interface ChildResource {

    /**
     * Return this resource's name. This must match the name of the child resource which is validated through this section of the validation model.
     * Either this method or {@link getNamePattern} must not return {@code null}
     *
     * @return the name (if one is set) or {@code null)
     */
    @CheckForNull String getName();
    
    /**
     * Returns this resource's name pattern. Either this method or {@link getName} must not return {@code null}
     *
     * @return the name pattern (if one is set) or {@code null)
     */
    @CheckForNull Pattern getNamePattern();

    /**
     * Returns {@code true} if at least one resource matching the name/namePattern is required.
     * 
     * @return {@code true} if the resource is required, {@code false} otherwise
     */
    boolean isRequired();

    /**
     * Returns the properties this child resource is expected to have.
     *
     * @return the properties list. Never {@code null}.
     */
    @Nonnull List<ResourceProperty> getProperties();
    
    /**
     * Returns the child resources of this part of the Validation Model
     * @return child resources. Never {@code null}.
     */
    @Nonnull List<ChildResource> getChildren();
}
