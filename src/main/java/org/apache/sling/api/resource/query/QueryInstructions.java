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

import javax.annotation.Nonnull;

import aQute.bnd.annotation.ProviderType;

/**
 * Query instructions are used to further specify runtime specific
 * instructions for a query.
 */
@ProviderType
public interface QueryInstructions {

    public interface SortCriteria {
        String getPropertyName();
        boolean isAscending();
    }
    /**
     * Unique global id.
     * The id can be used by implementations to cache the parsed query.
     * The id is ensured to be the same for two identical query instructions.
     * The only variable is the continuation key.
     * @return A unique global id.
     */
    @Nonnull String getId();

    /**
     * Get the limit of the query. By default the query is unlimited.
     * @return The limit. If a negative value is returned, there is no limit.
     */
    int getLimit();

    /**
     * Generate a continuation key to be used with {@link QueryInstructionsBuilder#continueAt(String)}.
     * The continuation can be used for paging: the last resource of a page is feed into this
     * method to get a key to be used to get the start for the next page.
     * A continuation key can only be generated if the result is sorted.
     * @param resource The last resource of a page
     * @return A continuation key for the next resource after this one, according to the sorting.
     * @throws IllegalArgumentException if the resource is {@code null}
     */
    @Nonnull String getContinuationKey();

    /**
     * Unmodifiable list with the sort criteria.
     * @return The sort criteria, the list might be empty.
     */
    @Nonnull List<SortCriteria> getSortCriteria();
}