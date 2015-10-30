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
import java.util.Set;

import org.apache.sling.api.resource.query.Query;

/**
 * Implementation of the query.
 */
public class BasicQuery implements Query {

    private final Set<String> paths;

    private final Set<String> resourceTypes;

    private final Set<String> names;

    private final Set<PropertyConstraint> propertyConstraints;

    private final QueryType queryType;

    private final List<Query> subQueries;

    public BasicQuery(final Set<String> paths,
            final Set<String> resourceTypes,
            final Set<String> names,
            final Set<PropertyConstraint> propertyConstraints) {
        this.paths = Collections.unmodifiableSet(paths);
        this.resourceTypes = Collections.unmodifiableSet(resourceTypes);
        this.names = Collections.unmodifiableSet(names);
        this.propertyConstraints = Collections.unmodifiableSet(propertyConstraints);
        this.subQueries = null;
        this.queryType = QueryType.SINGLE;
    }

    public BasicQuery(final QueryType qt,
            final List<Query> subQueries) {
        this.queryType = qt;
        this.subQueries = Collections.unmodifiableList(subQueries);
        this.paths = null;
        this.resourceTypes = null;
        this.names = null;
        this.propertyConstraints = null;
    }

    @Override
    public String getId() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<String> getPaths() {
        return this.paths;
    }

    @Override
    public Set<String> getResourceNames() {
        return this.names;
    }

    @Override
    public Set<String> getIsA() {
        return this.resourceTypes;
    }

    @Override
    public Set<PropertyConstraint> getPropertyConstraints() {
        return propertyConstraints;
    }

    @Override
    public List<Query> getParts() {
        return this.subQueries;
    }

    @Override
    public QueryType getQueryType() {
        return this.queryType;
    }
}