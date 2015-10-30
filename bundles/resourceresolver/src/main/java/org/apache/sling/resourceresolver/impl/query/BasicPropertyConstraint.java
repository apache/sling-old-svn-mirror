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

import org.apache.sling.api.resource.query.Query.PropertyConstraint;
import org.apache.sling.api.resource.query.Query.PropertyOperator;

/**
 * Implementation of the property constraint.
 */
public class BasicPropertyConstraint implements PropertyConstraint {

    private final PropertyOperator operator;

    private final String name;

    private final Object value;

    public BasicPropertyConstraint(final PropertyOperator operator,
            final String name,
            final Object value) {
        this.operator = operator;
        this.name = name;
        this.value = value;
    }

    @Override
    public PropertyOperator getOperator() {
        return this.operator;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Object getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        return "BasicPropertyConstraint [operator=" + operator + ", name=" + name + ", value=" + value + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((operator == null) ? 0 : operator.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        BasicPropertyConstraint other = (BasicPropertyConstraint) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (operator != other.operator)
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }
}