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
package org.apache.sling.scripting.sightly.impl.compiler.util.expression;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.scripting.sightly.compiler.expression.ExpressionNode;
import org.apache.sling.scripting.sightly.compiler.expression.NodeVisitor;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.ArrayLiteral;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.BinaryOperation;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.BooleanConstant;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.Identifier;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.MapLiteral;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.NullLiteral;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.NumericConstant;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.PropertyAccess;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.RuntimeCall;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.StringConstant;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.TernaryOperator;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.UnaryOperation;

public class NodeTransformer implements NodeVisitor<ExpressionNode> {

    public final ExpressionNode transform(ExpressionNode node) {
        return node.accept(this);
    }

    @Override
    public ExpressionNode evaluate(PropertyAccess propertyAccess) {
        return new PropertyAccess(transform(propertyAccess.getTarget()), transform(propertyAccess.getProperty()));
    }

    @Override
    public ExpressionNode evaluate(Identifier identifier) {
        return identifier;
    }

    @Override
    public ExpressionNode evaluate(StringConstant text) {
        return text;
    }

    @Override
    public ExpressionNode evaluate(BinaryOperation binaryOperation) {
        return new BinaryOperation(binaryOperation.getOperator(),
                transform(binaryOperation.getLeftOperand()),
                transform(binaryOperation.getRightOperand()));
    }

    @Override
    public ExpressionNode evaluate(BooleanConstant booleanConstant) {
        return booleanConstant;
    }

    @Override
    public ExpressionNode evaluate(NumericConstant numericConstant) {
        return numericConstant;
    }

    @Override
    public ExpressionNode evaluate(UnaryOperation unaryOperation) {
        return new UnaryOperation(unaryOperation.getOperator(), transform(unaryOperation.getTarget()));
    }

    @Override
    public ExpressionNode evaluate(TernaryOperator ternaryOperator) {
        return new TernaryOperator(
                transform(ternaryOperator.getCondition()),
                transform(ternaryOperator.getThenBranch()),
                transform(ternaryOperator.getElseBranch()));
    }

    @Override
    public ExpressionNode evaluate(RuntimeCall runtimeCall) {
        List<ExpressionNode> children = transformList(runtimeCall.getArguments());
        return new RuntimeCall(runtimeCall.getFunctionName(), children);
    }

    @Override
    public ExpressionNode evaluate(MapLiteral mapLiteral) {
        HashMap<String, ExpressionNode> map = new HashMap<>();
        for (Map.Entry<String, ExpressionNode> entry : mapLiteral.getMap().entrySet()) {
            map.put(entry.getKey(), transform(entry.getValue()));
        }
        return new MapLiteral(map);
    }

    @Override
    public ExpressionNode evaluate(ArrayLiteral arrayLiteral) {
        return new ArrayLiteral(transformList(arrayLiteral.getItems()));
    }

    @Override
    public ExpressionNode evaluate(NullLiteral nullLiteral) {
        return nullLiteral;
    }

    private List<ExpressionNode> transformList(List<ExpressionNode> nodes) {
        ArrayList<ExpressionNode> result = new ArrayList<>();
        for (ExpressionNode node : nodes) {
            result.add(transform(node));
        }
        return result;
    }
}
