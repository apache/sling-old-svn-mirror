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

import java.util.ArrayList;
import java.util.List;

import org.apache.sling.api.resource.query.QueryInstructions;
import org.apache.sling.api.resource.query.QueryInstructions.SortCriteria;
import org.apache.sling.api.resource.query.QueryInstructionsBuilder;

/**
 * Implementation of the query instructions builder.
 */
public class BasicQueryInstructionsBuilder implements QueryInstructionsBuilder {

    private final List<SortCriteria> sortCriteria = new ArrayList<SortCriteria>();

    private String continuationKey;

    private int limit = -1;

    @Override
    public QueryInstructionsBuilder limit(final int limit) {
        this.limit = limit;
        return this;
    }

    @Override
    public QueryInstructionsBuilder continueAt(String continuationKey) {
        this.continuationKey = continuationKey;
        return this;
    }

    @Override
    public QueryInstructionsBuilder sortAscendingBy(final String propName) {
        this.sortCriteria.add(new BasicSortCriteria(propName, true));
        return this;
    }

    @Override
    public QueryInstructionsBuilder sortDescendingBy(final String propName) {
        this.sortCriteria.add(new BasicSortCriteria(propName, false));
        return this;
    }

    @Override
    public QueryInstructions build() {
        return new BasicQueryInstructions(sortCriteria, continuationKey, limit);
    }
}