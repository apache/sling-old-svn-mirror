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

import java.util.Set;

/**
 * Defines the validation rules for a child resource, allowing {@link ValidationModel}s to be applied to {@link
 * org.apache.sling.api.resource.Resource} trees.
 */
public interface ChildResource {

    /**
     * Return this resource's name.
     *
     * @return the name
     */
    String getName();

    /**
     * Returns the properties this child resource is expected to have.
     *
     * @return the properties set
     */
    Set<ResourceProperty> getProperties();
}
