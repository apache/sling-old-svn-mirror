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
package org.apache.sling.validation.api;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Defines the validation rules for a child resource, allowing {@link ValidationModel}s to be applied to {@link
 * org.apache.sling.api.resource.Resource} trees.
 */
public interface ChildResource {

    /**
     * Return this resource's name. This must match the name of the child resource which is validated through this section of the validation model.
     *
     * @return the name (if one is set) or {@code null)
     */
    String getName();
    
    /**
     * Return this resource's name pattern.
     *
     * @return the name pattern (if one is set) or {@code null)
     */
    Pattern getNamePattern();

    /**
     * Returns the properties this child resource is expected to have.
     *
     * @return the properties set
     */
    Set<ResourceProperty> getProperties();
    
    /**
     * Returns the child resources of this part of the Validation Model
     * @return child resources. Never {@code null}.
     */
    List<ChildResource> getChildren();
}
