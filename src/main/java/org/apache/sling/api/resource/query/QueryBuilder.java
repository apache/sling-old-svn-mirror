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
package org.apache.sling.api.resource.query;

import javax.annotation.Nonnull;

import org.apache.sling.api.resource.Resource;

import aQute.bnd.annotation.ProviderType;

/**
 * The query builder can be used to build a query
 * for resources.
 */
@ProviderType
public interface QueryBuilder {

    /**
     * Build the query.
     *
     * @return The query.
     *
     * @throws IllegalArgumentException If the query is not valid.
     */
    @Nonnull Query build();

    /**
     * At a path condition to the query.
     * If several paths are added to the query, these are handled as an or.
     * If no path is specified for a query, the search paths of the resource
     * resolver are used.
     *
     * @param path An absolute resource path
     * @return The query builder to construct the query
     *
     * @throws IllegalArgumentException If the argument is not valid.
     */
    @Nonnull QueryBuilder at(@Nonnull String... path);

    /**
     * Add a condition to check whether the resource is of a specific resource type.
     * If more than one resource type is specified, the conditions are handled with
     * an or operation.
     * The operation is similar to {@link Resource#isResourceType(String)}
     * @param resourceType The resource type to check.
     * @return The query builder to construct the query.
     *
     * @throws IllegalArgumentException If the argument is not valid.
     */
    @Nonnull QueryBuilder isA(@Nonnull String resourceType);

    /**
     * Add a condition to check whether the resource has a specific name.
     * If more than one resource name is specified, the conditions are handled with
     * an or operation.
     * @param name The name to check.
     * @return The query builder to construct the query.
     *
     * @throws IllegalArgumentException If the argument is not valid.
     */
    @Nonnull QueryBuilder name(@Nonnull String resourceName);

    /**
     * Add a property condition for the resources.
     * If more than one property condition is specified, the conditions are
     * handled with an and operation.
     * The property might be a relative path pointing to a property of a child
     * resource. In this case the child resource must be provided by the same
     * resource provider. A relative path pointing to a parent resource or
     * any resource which is not a child resource is not valid.
     * The special name {@code *} can be used to mean any property of the current resource.
     * @param name The name of the property to check. The name can be a path specifying
     *        a property of a known child resource of the returned resource.
     * @return A property builder to specify the operation on the property value.
     *
     * @throws IllegalArgumentException If the argument is not valid.
     */
    @Nonnull PropertyBuilder property(@Nonnull String name);


}