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

import org.apache.sling.scripting.sightly.impl.compiler.expression.ExpressionNode;
import org.apache.sling.scripting.sightly.impl.compiler.expression.NodeVisitor;
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
        HashMap<String, ExpressionNode> map = new HashMap<String, ExpressionNode>();
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
        ArrayList<ExpressionNode> result = new ArrayList<ExpressionNode>();
        for (ExpressionNode node : nodes) {
            result.add(transform(node));
        }
        return result;
    }
}
