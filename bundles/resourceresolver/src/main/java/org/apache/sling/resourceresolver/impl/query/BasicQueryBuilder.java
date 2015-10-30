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

import java.util.HashSet;
import java.util.Set;

import org.apache.sling.api.resource.query.PropertyBuilder;
import org.apache.sling.api.resource.query.Query;
import org.apache.sling.api.resource.query.Query.PropertyConstraint;
import org.apache.sling.api.resource.query.Query.PropertyOperator;
import org.apache.sling.api.resource.query.QueryBuilder;

/**
 * Implementation of the query builder.
 */
public class BasicQueryBuilder implements QueryBuilder {

    private final Set<String> paths = new HashSet<String>();

    private final Set<String> resourceTypes = new HashSet<String>();

    private final Set<String> names = new HashSet<String>();

    private final Set<PropertyConstraint> propertyConstraints = new HashSet<PropertyConstraint>();

    @Override
    public Query build() {
        return new BasicQuery(paths, resourceTypes, names, propertyConstraints);
    }

    @Override
    public QueryBuilder at(final String... path) {
        for(final String p : path) {
            this.paths.add(p);
        }
        return this;
    }

    @Override
    public QueryBuilder isA(final String resourceType) {
        this.resourceTypes.add(resourceType);
        return this;
    }

    @Override
    public QueryBuilder name(final String resourceName) {
        this.names.add(resourceName);
        return this;
    }

    @Override
    public PropertyBuilder property(final String name) {
        return new PropertyBuilder() {

            @Override
            public QueryBuilder isLessOrEq(final Object value) {
                propertyConstraints.add(new BasicPropertyConstraint(PropertyOperator.LT_OR_EQ, name, value));
                return BasicQueryBuilder.this;
            }

            @Override
            public QueryBuilder isLess(final Object value) {
                propertyConstraints.add(new BasicPropertyConstraint(PropertyOperator.LT, name, value));
                return BasicQueryBuilder.this;
            }

            @Override
            public QueryBuilder isGreaterOrEq(final Object value) {
                propertyConstraints.add(new BasicPropertyConstraint(PropertyOperator.GT_OR_EQ, name, value));
                return BasicQueryBuilder.this;
            }

            @Override
            public QueryBuilder isGreater(final Object value) {
                propertyConstraints.add(new BasicPropertyConstraint(PropertyOperator.GT, name, value));
                return BasicQueryBuilder.this;
            }

            @Override
            public QueryBuilder exists() {
                propertyConstraints.add(new BasicPropertyConstraint(PropertyOperator.EXISTS, name, null));
                return BasicQueryBuilder.this;
            }

            @Override
            public QueryBuilder eq(final Object value) {
                propertyConstraints.add(new BasicPropertyConstraint(PropertyOperator.EQ, name, value));
                return BasicQueryBuilder.this;
            }

            @Override
            public QueryBuilder contains(final Object value) {
                propertyConstraints.add(new BasicPropertyConstraint(PropertyOperator.CONTAINS, name, value));
                return BasicQueryBuilder.this;
            }
        };
    }

}