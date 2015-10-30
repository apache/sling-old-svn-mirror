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
package org.apache.sling.resourceresolver.impl.query;

import java.util.Collections;
import java.util.List;

import org.apache.sling.api.resource.query.QueryInstructions;

/**
 * Implementation of the query instructions.
 */
public class BasicQueryInstructions implements QueryInstructions {

    private final List<SortCriteria> sortCriteria;

    private final String continuationKey;

    private final int limit;

    public BasicQueryInstructions(final List<SortCriteria> sortCriteria,
            final String continuationKey,
            final int limit) {
        this.sortCriteria = Collections.unmodifiableList(sortCriteria);
        this.continuationKey = continuationKey;
        this.limit = limit;
    }

    @Override
    public String getId() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getLimit() {
        return this.limit;
    }

    @Override
    public String getContinuationKey() {
        return this.continuationKey;
    }

    @Override
    public List<SortCriteria> getSortCriteria() {
        return sortCriteria;
    }
}