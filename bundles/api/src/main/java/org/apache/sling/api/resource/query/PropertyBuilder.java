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
 * Builder for creating a property part of a query.
 */
@ProviderType
public interface PropertyBuilder {

    /**
     * Create a property constraint for equality test.
     * @param value The value to check
     * @return A query builder to continue creating the query.
     */
    @Nonnull QueryBuilder eq(@Nonnull Object value);

    /**
     * Create a property constraint for is greater than test.
     * @param value The value to check
     * @return A query builder to continue creating the query.
     */
    @Nonnull QueryBuilder isGreater(@Nonnull Object value);

    /**
     * Create a property constraint for is greater or equals than test.
     * @param value The value to check
     * @return A query builder to continue creating the query.
     */
    @Nonnull QueryBuilder isGreaterOrEq(@Nonnull Object value);

    /**
     * Create a property constraint for is less than test.
     * @param value The value to check
     * @return A query builder to continue creating the query.
     */
    @Nonnull QueryBuilder isLess(@Nonnull Object value);

    /**
     * Create a property constraint for is less or equals than test.
     * @param value The value to check
     * @return A query builder to continue creating the query.
     */
    @Nonnull QueryBuilder isLessOrEq(@Nonnull Object value);

    /**
     * Create a property constraint for a contains test.
     * @param value The value to check
     * @return A query builder to continue creating the query.
     */
    @Nonnull QueryBuilder contains(@Nonnull Object value);

    /**
     * Create a property constraint for an existence test.
     * @return A query builder to continue creating the query.
     */
    @Nonnull QueryBuilder exists();
}