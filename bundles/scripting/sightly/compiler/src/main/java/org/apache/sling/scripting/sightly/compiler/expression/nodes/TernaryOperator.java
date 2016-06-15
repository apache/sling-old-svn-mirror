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
 * Defines the Sightly ternary operator (e.g. {@code condition ? then : else}).
 */
public final class TernaryOperator implements ExpressionNode {

    private ExpressionNode condition;
    private ExpressionNode thenBranch;
    private ExpressionNode elseBranch;

    /**
     * Creates the operator.
     *
     * @param condition  the operator's condition
     * @param thenBranch the "then" branch
     * @param elseBranch the "else" branch
     */
    public TernaryOperator(ExpressionNode condition, ExpressionNode thenBranch, ExpressionNode elseBranch) {
        this.condition = condition;
        this.thenBranch = thenBranch;
        this.elseBranch = elseBranch;
    }

    /**
     * Returns the condition of this operator.
     *
     * @return the condition of this operator
     */
    public ExpressionNode getCondition() {
        return condition;
    }

    /**
     * Returns the "then" branch.
     *
     * @return the "then" branch
     */
    public ExpressionNode getThenBranch() {
        return thenBranch;
    }

    /**
     * Returns the "else" branch.
     *
     * @return the "else" branch
     */
    public ExpressionNode getElseBranch() {
        return elseBranch;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.evaluate(this);
    }

    @Override
    public String toString() {
        return "TernaryOperator{" +
                "condition=" + condition +
                ", thenBranch=" + thenBranch +
                ", elseBranch=" + elseBranch +
                '}';
    }
}
