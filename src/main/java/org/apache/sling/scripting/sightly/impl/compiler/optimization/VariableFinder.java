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
package org.apache.sling.scripting.sightly.impl.compiler.optimization;

import java.util.HashSet;
import java.util.Set;

import org.apache.sling.scripting.sightly.impl.compiler.expression.ExpressionNode;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.ArrayLiteral;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.BinaryOperation;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.BooleanConstant;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.Identifier;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.MapLiteral;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.NullLiteral;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.NumericConstant;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.PropertyAccess;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.RuntimeCall;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.StringConstant;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.TernaryOperator;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.UnaryOperation;
import org.apache.sling.scripting.sightly.impl.compiler.util.expression.SideEffectVisitor;

/**
 * SideEffectVisitor which extracts all the variables from an expression
 */
public class VariableFinder extends SideEffectVisitor {

    private final Set<String> variables;

    public VariableFinder(Set<String> variables) {
        this.variables = variables;
    }

    private void traverse(ExpressionNode node) {
        if (node != null) {
            node.accept(this);
        }
    }

    public static Set<String> findVariables(ExpressionNode node) {
        HashSet<String> result = new HashSet<String>();
        VariableFinder finder = new VariableFinder(result);
        finder.traverse(node);
        return result;
    }

    @Override
    public void visit(PropertyAccess propertyAccess) {
        traverse(propertyAccess.getTarget());
        traverse(propertyAccess.getProperty());
    }

    @Override
    public void visit(Identifier identifier) {
        variables.add(identifier.getName());
    }

    @Override
    public void visit(StringConstant text) {
    }

    @Override
    public void visit(BinaryOperation binaryOperation) {
        traverse(binaryOperation.getLeftOperand());
        traverse(binaryOperation.getRightOperand());
    }

    @Override
    public void visit(BooleanConstant booleanConstant) {
    }

    @Override
    public void visit(NumericConstant numericConstant) {
    }

    @Override
    public void visit(UnaryOperation unaryOperation) {
        traverse(unaryOperation.getTarget());
    }

    @Override
    public void visit(TernaryOperator ternaryOperator) {
        traverse(ternaryOperator.getCondition());
        traverse(ternaryOperator.getThenBranch());
        traverse(ternaryOperator.getElseBranch());
    }

    @Override
    public void visit(RuntimeCall runtimeCall) {
        for (ExpressionNode node : runtimeCall.getArguments()) {
            traverse(node);
        }
    }

    @Override
    public void visit(MapLiteral mapLiteral) {
        for (ExpressionNode value : mapLiteral.getMap().values()) {
            traverse(value);
        }
    }

    @Override
    public void visit(ArrayLiteral arrayLiteral) {
        for (ExpressionNode item : arrayLiteral.getItems()) {
            traverse(item);
        }
    }

    @Override
    public void visit(NullLiteral nullLiteral) {
    }
}
