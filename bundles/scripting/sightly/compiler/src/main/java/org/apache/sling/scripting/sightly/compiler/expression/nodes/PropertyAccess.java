/*******************************************************************************
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
 ******************************************************************************/
package org.apache.sling.scripting.sightly.compiler.expression.nodes;

import org.apache.sling.scripting.sightly.compiler.expression.ExpressionNode;
import org.apache.sling.scripting.sightly.compiler.expression.NodeVisitor;

/**
 * Defines an expression in which an object is queried for a specific property (e.g. {@code obj.prop}).
 */
public final class PropertyAccess implements ExpressionNode {

    private final ExpressionNode target;
    private final ExpressionNode property;

    /**
     * Creates a property access node.
     *
     * @param target   the expression for the object being accessed
     * @param property the expression identifying the object's accessed property
     */
    public PropertyAccess(ExpressionNode target, ExpressionNode property) {
        this.target = target;
        this.property = property;
    }

    /**
     * Creates a property access node.
     *
     * @param target   the expression for the object being accessed
     * @param property the name of the accessed property
     */
    public PropertyAccess(ExpressionNode target, String property) {
        this.target = target;
        this.property = new StringConstant(property);
    }

    /**
     * Builds a chained property access node with the given target and the specified properties.
     *
     * @param target     the target node
     * @param properties a non-empty list of property names
     * @throws IllegalArgumentException if the list of properties is empty
     */
    public PropertyAccess(ExpressionNode target, Iterable<String> properties) {
        String lastProp = null;
        ExpressionNode result = target;
        for (String property : properties) {
            if (lastProp != null) {
                result = new PropertyAccess(result, new StringConstant(lastProp));
            }
            lastProp = property;
        }
        if (lastProp == null) {
            throw new IllegalArgumentException("The list of properties must be non-empty");
        }
        this.target = result;
        this.property = new StringConstant(lastProp);
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.evaluate(this);
    }

    /**
     * The object being accessed.
     *
     * @return a node representing the object being accessed
     */
    public ExpressionNode getTarget() {
        return target;
    }

    /**
     * Returns the expression node identifying the accessed property.
     *
     * @return the expression node identifying the accessed property
     */
    public ExpressionNode getProperty() {
        return property;
    }

    @Override
    public String toString() {
        return "PropertyAccess{" +
                "target=" + target +
                ", property='" + property + '\'' +
                '}';
    }

}
