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

import java.util.Collection;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import aQute.bnd.annotation.ProviderType;

/**
 * Defines the validation rules for a child resource, allowing {@link ValidationModel}s to be applied to {@link
 * org.apache.sling.api.resource.Resource} trees.
 */
@ProviderType
public interface ChildResource {

    /**
     * Return this resource's name. This must match the name of the child resource which is validated through this section of the validation model.
     * In case {@link getNamePattern} is not {@code null} this name is not relevant for matching the content resource.
     *
     * @return the name
     */
     String getName();
    
    /**
     * Returns this resource's name pattern. In case this is not returning {@code null}, this pattern is used for finding the child resources which should be validated.
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
    @Nonnull Collection<ResourceProperty> getProperties();
    
    /**
     * Returns the child resources of this part of the Validation Model
     * @return child resources. Never {@code null}.
     */
    @Nonnull Collection<ChildResource> getChildren();
}
