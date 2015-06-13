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

import aQute.bnd.annotation.ProviderType;

/**
 * Query instructions are used to further specify runtime specific
 * instructions for a query.
 */
@ProviderType
public interface QueryInstructionsBuilder {

    /**
     * Specify the limit of the query. By default the query is unlimited.
     * @param limit The new limit. If a value less than 1 is used, there is no limit.
     * @return The query instructions.
     */
    @Nonnull QueryInstructionsBuilder limit(int limit);

    /**
     * Specify the continuation key.
     */
    @Nonnull QueryInstructionsBuilder continueAt(@Nonnull String continuationKey);

    /**
     * Sort the result in ascending order by this property.
     * If more than one sort criteria is specified, the result is sorted first by the
     * first criteria, within that sorting the second criteria is applied and so on.
     * @param propName The name of the property
     * @return The query builder to construct the query.
     *
     * @throws IllegalArgumentException If the argument is {@code null}.
     */
    @Nonnull QueryInstructionsBuilder sortAscendingBy(@Nonnull String propName);

    /**
     * Sort the result in descending order by this property.
     * If more than one sort criteria is specified, the result is sorted first by the
     * first criteria, within that sorting the second criteria is applied and so on.
     * @param propName The name of the property
     * @return The query builder to construct the query.
     *
     * @throws IllegalArgumentException If the argument is {@code null}.
     */
    @Nonnull QueryInstructionsBuilder sortDescendingBy(@Nonnull String propName);

    /**
     * Build the query instructions object.
     * @return The query instructions.
     */
    @Nonnull QueryInstructions build();
}