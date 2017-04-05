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
 * Defines a unary operation (e.g. {@code !variableName}).
 */
public final class UnaryOperation implements ExpressionNode {

    private UnaryOperator operator;
    private ExpressionNode target;

    /**
     * Creates a {@code UnaryOperation}.
     *
     * @param operator the operator
     * @param target   the target to which the operator is applied
     */
    public UnaryOperation(UnaryOperator operator, ExpressionNode target) {
        this.operator = operator;
        this.target = target;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.evaluate(this);
    }

    /**
     * Returns the operator applied in this operation.
     *
     * @return the operator applied in this operation
     */
    public UnaryOperator getOperator() {
        return operator;
    }

    /**
     * Returns the target to which the operation is applied.
     *
     * @return the target to which the operation is applied
     */
    public ExpressionNode getTarget() {
        return target;
    }

    @Override
    public String toString() {
        return "UnaryOperation{" +
                "operator=" + operator +
                ", operand=" + target +
                '}';
    }

}
