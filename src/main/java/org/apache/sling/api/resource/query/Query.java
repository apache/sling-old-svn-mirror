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

import java.util.List;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import aQute.bnd.annotation.ProviderType;

/**
 * A query object describes a query to be executed. Each query has a unique
 * and stable id which can be used by implementations to cache the parsing
 * of the query object.
 *
 * A query can either be a compound query, combining other queries or
 * it can be an actual query over resource paths, names, types and
 * properties. The method {@link #getQueryType()} can be used
 * to identify the type of the query.
 */
@ProviderType
public interface Query {

    /**
     * Enumeration defining whether the query is a compound or
     * single query.
     */
    public enum QueryType {
        SINGLE,
        COMPOUND_AND,
        COMPOUND_OR
    };

    /**
     * The different property operations.
     */
    public enum PropertyOperator {
        EXISTS,   // property exists
        EQ,       // property has an equal value
        GT,       // property is greater than the value
        GT_OR_EQ, // property is greater or equal to the value
        LT,       // property is less than the value
        LT_OR_EQ, // property is less or equal to the value
        CONTAINS  // property contains the value
    };

    /**
     * A property constraint describes a constraint on a property.
     * The operator defines the comparison operation and the name
     * the name of the property to check. The property name might
     * be a relative path pointing to a property of a child
     * resource or it might consist of the special name {code *}
     * denoting any property.
     */
    public interface PropertyConstraint {

        /**
         * The operator to use to compare the property value.
         * @return The operator
         */
        @Nonnull PropertyOperator getOperator();

        /**
         * The property name
         * @return The name of the property, relative path or {@code *}.
         */
        @Nonnull String getName();

        /**
         * The value to compare against.
         * @return The value to compare or {@code null}.
         */
        @CheckForNull Object getValue();
    }

    /**
     * Unique global id.
     * The id can be used by implementations to cache the parsed query.
     * The id is ensured to be the same for two identical queries.
     * @return A unique global id.
     */
    @Nonnull String getId();

    /**
     * Returns constraints on the resource paths.
     * This method returns an empty list if it's a compound query.
     * @return The list of paths, might be empty.
     */
    @CheckForNull List<String> getPaths();

    /**
     * Returns constraints on the resource names.
     * This method returns an empty list if it's a compound query.
     * @return The list of names, might be empty.
     */
    @CheckForNull List<String> getResourceNames();

    /**
     * Returns constraints on the resource types.
     * This method returns an empty list if it's a compound query.
     * @return The list of types, might be empty.
     */
    @CheckForNull List<String> getIsA();

    /**
     * Return the list of property constraints.
     * This method only returns constraints, if the query type
     * is {@link QueryType#SINGLE}.
     * @return The list of property constraints or {@code null}.
     */
    @CheckForNull List<PropertyConstraint> getPropertyConstraints();

    /**
     * Returns the queries if {@link #getPartOperatorType()} does not
     * return {@link PartOperatorType#NONE}.
     * @return The parts or {@code null} if this query is not an operation
     *         on other queries.
     */
    @CheckForNull List<Query> getParts();

    /**
     * Return the type of the query.
     * @return The query type.
     */
    @Nonnull QueryType getQueryType();

}