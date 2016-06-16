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
package org.apache.sling.scripting.sightly.compiler.commands;

import org.apache.sling.scripting.sightly.compiler.expression.ExpressionNode;

/**
 * This {@link Command} marks the binding of a variable. The command must have a corresponding binding end later in the stream.
 */
public final class VariableBinding {

    public static class Start implements Command {
        private String variableName;
        private ExpressionNode expression;

        public Start(String variableName, ExpressionNode expression) {
            this.variableName = variableName;
            this.expression = expression;
        }

        public String getVariableName() {
            return variableName;
        }

        public ExpressionNode getExpression() {
            return expression;
        }

        @Override
        public void accept(CommandVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public String toString() {
            return "VariableBinding.Start{" +
                    "variableName='" + variableName + '\'' +
                    ", expression=" + expression +
                    '}';
        }
    }

    public static final End END = new End();

    public static final class End implements Command {

        private End() {
        }

        @Override
        public void accept(CommandVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public String toString() {
            return "VariableBinding.End{}";
        }
    }

    public static final class Global implements Command {

        private final String variableName;
        private final ExpressionNode expressionNode;

        public Global(String variableName, ExpressionNode expressionNode) {
            this.variableName = variableName;
            this.expressionNode = expressionNode;
        }

        public String getVariableName() {
            return variableName;
        }

        public ExpressionNode getExpression() {
            return expressionNode;
        }


        @Override
        public void accept(CommandVisitor visitor) {
            visitor.visit(this);
        }
    }
}
