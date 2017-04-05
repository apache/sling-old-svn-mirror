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

import org.apache.sling.scripting.sightly.compiler.expression.Expression;
import org.apache.sling.scripting.sightly.compiler.expression.ExpressionNode;
import org.apache.sling.scripting.sightly.compiler.expression.NodeVisitor;

/**
 * A {code BinaryOperation} defines an expression where a binary operator is applied (e.g. "a AND b").
 */
public final class BinaryOperation implements ExpressionNode {

    private BinaryOperator operator;
    private ExpressionNode leftOperand;
    private ExpressionNode rightOperand;
    private Expression parentExpression;

    /**
     * Creates a {@code BinaryOperation}.
     *
     * @param operator     the operator
     * @param leftOperand  the left operand
     * @param rightOperand the right operand
     */
    public BinaryOperation(BinaryOperator operator, ExpressionNode leftOperand, ExpressionNode rightOperand) {
        this.operator = operator;
        this.leftOperand = leftOperand;
        this.rightOperand = rightOperand;
    }

    /**
     * Creates a {@code BinaryOperation}.
     *
     * @param operator         the operator
     * @param leftOperand      the left operand
     * @param rightOperand     the right operand
     * @param parentExpression the parent expression of this operation
     */
    public BinaryOperation(BinaryOperator operator, ExpressionNode leftOperand, ExpressionNode rightOperand, Expression parentExpression) {
        this(operator, leftOperand, rightOperand);
        this.parentExpression = parentExpression;
    }

    /**
     * Returns the operator of the operation.
     *
     * @return the operator
     */
    public BinaryOperator getOperator() {
        return operator;
    }

    /**
     * Returns the left operand.
     *
     * @return the left operand
     */
    public ExpressionNode getLeftOperand() {
        return leftOperand;
    }

    /**
     * Returns the right operand.
     *
     * @return the right operand.
     */
    public ExpressionNode getRightOperand() {
        return rightOperand;
    }

    /**
     * Returns the parent expression, if any.
     *
     * @return the parent expression or {@code null}
     */
    public Expression getParentExpression() {
        return parentExpression;
    }

    /**
     * Returns a copy of this {@code BinaryOperation} that contains information about the node's parent expression.
     *
     * @param parentExpression the parent expression
     * @return a copy of the original {@code BinaryOperation}
     */
    public BinaryOperation withParentExpression(Expression parentExpression) {
        return new BinaryOperation(operator, leftOperand, rightOperand, parentExpression);
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.evaluate(this);
    }

    @Override
    public String toString() {
        return "BinaryOperation{" +
                "operator=" + operator +
                ", leftOperand=" + leftOperand +
                ", rightOperand=" + rightOperand +
                '}';
    }

}
