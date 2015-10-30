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

import org.apache.sling.api.resource.query.QueryInstructions.SortCriteria;

/**
 * Implementation of the sort criteria
 */
public class BasicSortCriteria implements SortCriteria {

    private final String name;

    private final boolean isAscending;

    public BasicSortCriteria(final String name, final boolean isAscending) {
        this.name = name;
        this.isAscending = isAscending;
    }

    @Override
    public String getPropertyName() {
        return this.name;
    }

    @Override
    public boolean isAscending() {
        return this.isAscending;
    }
}