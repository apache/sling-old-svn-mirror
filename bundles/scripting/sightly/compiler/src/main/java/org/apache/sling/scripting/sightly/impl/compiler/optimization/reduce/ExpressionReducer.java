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
package org.apache.sling.scripting.sightly.impl.compiler.optimization.reduce;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.scripting.sightly.compiler.SightlyCompilerException;
import org.apache.sling.scripting.sightly.compiler.expression.Expression;
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
import org.apache.sling.scripting.sightly.compiler.util.VariableTracker;
import org.apache.sling.scripting.sightly.impl.compiler.CompileTimeObjectModel;

/**
 * Try to evaluate constant parts in expressions
 */
public class ExpressionReducer implements NodeVisitor<EvalResult> {

    private final VariableTracker<EvalResult> tracker;

    public static EvalResult reduce(ExpressionNode node, VariableTracker<EvalResult> tracker) {
        ExpressionReducer reducer = new ExpressionReducer(tracker);
        return reducer.eval(node);
    }

    private ExpressionReducer(VariableTracker<EvalResult> tracker) {
        this.tracker = tracker;
    }

    private EvalResult eval(ExpressionNode node) {
        try {
            return node.accept(this);
        } catch (SightlyCompilerException e) {
            if (node instanceof BinaryOperation) {
                BinaryOperation binaryOperation = (BinaryOperation) node;
                Expression parentExpression = binaryOperation.getParentExpression();
                if (parentExpression != null) {
                    throw new SightlyCompilerException(e.getMessage(), parentExpression.getRawText());
                }
            }
            throw e;
        } catch (Exception e) {
            //evaluating constant expressions may lead to errors (like division by zero)
            //in this case we leave the nodes as-is.
            return EvalResult.nonConstant(node);
        }
    }

    @Override
    public EvalResult evaluate(PropertyAccess propertyAccess) {
        EvalResult target = eval(propertyAccess.getTarget());
        EvalResult property = eval(propertyAccess.getProperty());
        if (!target.isConstant() || !property.isConstant()) {
            return EvalResult.nonConstant(new PropertyAccess(
                    target.getNode(),
                    property.getNode()));
        }

        return EvalResult.constant(CompileTimeObjectModel.resolveProperty(
                target.getValue(), property.getValue()));
    }

    @Override
    public EvalResult evaluate(Identifier identifier) {
        EvalResult result = tracker.get(identifier.getName());
        if (result != null && result.isConstant()) {
            return EvalResult.constant(result.getValue());
        }
        return EvalResult.nonConstant(identifier);
    }

    @Override
    public EvalResult evaluate(StringConstant text) {
        return EvalResult.constant(text.getText());
    }

    @Override
    public EvalResult evaluate(BinaryOperation binaryOperation) {
        EvalResult left = eval(binaryOperation.getLeftOperand());
        EvalResult right = eval(binaryOperation.getRightOperand());
        if (!(left.isConstant() && right.isConstant())) {
            return EvalResult.nonConstant(new BinaryOperation(
                    binaryOperation.getOperator(),
                    left.getNode(),
                    right.getNode()));
        }
        return EvalResult.constant(binaryOperation.getOperator().eval(left.getValue(), right.getValue()));
    }

    @Override
    public EvalResult evaluate(BooleanConstant booleanConstant) {
        return EvalResult.constant(booleanConstant.getValue());
    }

    @Override
    public EvalResult evaluate(NumericConstant numericConstant) {
        return EvalResult.constant(numericConstant.getValue());
    }

    @Override
    public EvalResult evaluate(UnaryOperation unaryOperation) {
        EvalResult target = eval(unaryOperation.getTarget());
        if (!target.isConstant()) {
            return EvalResult.nonConstant(new UnaryOperation(
                    unaryOperation.getOperator(), target.getNode()));
        }
        return EvalResult.constant(unaryOperation.getOperator().eval(target.getValue()));
    }

    @Override
    public EvalResult evaluate(TernaryOperator ternaryOperator) {
        EvalResult condition = eval(ternaryOperator.getCondition());
        if (!condition.isConstant()) {
            return EvalResult.nonConstant(new TernaryOperator(
                    condition.getNode(),
                    ternaryOperator.getThenBranch(),
                    ternaryOperator.getElseBranch()));
        }
        return (CompileTimeObjectModel.toBoolean(condition.getValue()))
                ? eval(ternaryOperator.getThenBranch())
                : eval(ternaryOperator.getElseBranch());
    }

    @Override
    public EvalResult evaluate(RuntimeCall runtimeCall) {
        List<ExpressionNode> nodes = new ArrayList<>();
        for (ExpressionNode node : runtimeCall.getArguments()) {
            EvalResult result = eval(node);
            nodes.add(result.getNode());
        }
        return EvalResult.nonConstant(new RuntimeCall(runtimeCall.getFunctionName(), nodes));
    }

    @Override
    public EvalResult evaluate(MapLiteral mapLiteral) {
        HashMap<String, EvalResult> results = new HashMap<>();
        boolean isConstant = true;
        for (Map.Entry<String, ExpressionNode> entry : mapLiteral.getMap().entrySet()) {
            EvalResult result = eval(entry.getValue());
            results.put(entry.getKey(), result);
            isConstant = isConstant && result.isConstant();
        }
        if (isConstant) {
            HashMap<String, Object> map = new HashMap<>();
            for (Map.Entry<String, EvalResult> entry : results.entrySet()) {
                map.put(entry.getKey(), entry.getValue().getValue());
            }
            return EvalResult.constant(map);
        } else {
            HashMap<String, ExpressionNode> literal = new HashMap<>();
            for (Map.Entry<String, EvalResult> entry : results.entrySet()) {
                literal.put(entry.getKey(), entry.getValue().getNode());
            }
            return EvalResult.nonConstant(new MapLiteral(literal));
        }
    }

    @Override
    public EvalResult evaluate(ArrayLiteral arrayLiteral) {
        ArrayList<EvalResult> results = new ArrayList<>();
        boolean isConstant = true;
        for (ExpressionNode node : arrayLiteral.getItems()) {
            EvalResult result = eval(node);
            results.add(result);
            isConstant = isConstant && result.isConstant();
        }
        if (isConstant) {
            ArrayList<Object> list = new ArrayList<>();
            for (EvalResult result : results) {
                list.add(result.getValue());
            }
            return EvalResult.constant(list);
        } else {
            ArrayList<ExpressionNode> literal = new ArrayList<>();
            for (EvalResult result : results) {
                literal.add(result.getNode());
            }
            return EvalResult.nonConstant(new ArrayLiteral(literal));
        }
    }

    @Override
    public EvalResult evaluate(NullLiteral nullLiteral) {
        return EvalResult.constant(null);
    }
}
