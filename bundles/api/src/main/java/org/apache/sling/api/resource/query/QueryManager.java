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

import java.util.Iterator;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import aQute.bnd.annotation.ProviderType;

/**
 * The query manager allows to run a query in the context
 * of a resource resolver.
 */
@ProviderType
public interface QueryManager {

    /**
     * Execute the given query in the context of the resource resolver.
     *
     * In general a search is performed across all resource providers supporting
     * search. However, sorting and pagination across providers can't be done in
     * a general but fast way, therefore as soon as a query instructions object
     * is provided, the search is only performed against a single provider. If
     * the query is restricted by path, the provider handling that path is used.
     * If potentially more than one provider is targeted and a query instructions
     * is provided, an {@code IllegalArgumentException} is thrown.
     *
     * @param resolver The resource resolver
     * @param q The query
     * @param qi The optional query instructions
     * @return An iterator of resources - the result might be empty.
     * @throws IllegalArgumentException If the query is invalid.
     */
    @Nonnull Iterator<Resource> find(@Nonnull ResourceResolver resolver,
                                     @Nonnull Query q,
                                     @CheckForNull QueryInstructions qi);

    /**
     * Create a new query.
     * @return A query builder to build the query.
     */
    @Nonnull QueryBuilder query();

    /**
     * Create a new query based on combining two or more queries in an and fashion.
     *
     * @param q1 First query
     * @param q2 Additional queries
     * @return The new query
     */
    @Nonnull Query andQuery(@Nonnull Query q1, @Nonnull Query... q2);

    /**
     * Create a new query based on combining two or more queries in an or fashion.
     *
     * @param q1 First query
     * @param q2 Additional queries
     * @return The new query
     *
     * @throws IllegalArgumentException If the argument is not valid.
     */
    @Nonnull Query orQuery(@Nonnull Query q1, @Nonnull Query... q2);

    /**
     * Create query instructions.
     * @return A new query instruction builder.
     */
    @Nonnull QueryInstructionsBuilder instructions();
}
