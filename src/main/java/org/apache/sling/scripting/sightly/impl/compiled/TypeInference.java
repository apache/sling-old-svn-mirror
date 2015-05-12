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

package org.apache.sling.scripting.sightly.impl.compiled;

import java.util.IdentityHashMap;
import java.util.Map;

import org.apache.sling.scripting.sightly.impl.compiled.operator.BinaryOpGen;
import org.apache.sling.scripting.sightly.impl.compiled.operator.Operators;
import org.apache.sling.scripting.sightly.impl.compiled.operator.UnaryOpGen;
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

/**
 * Expression translator which uses type information
 */
public final class TypeInference implements NodeVisitor<Type> {

    private final VariableAnalyzer analyzer;
    private final Map<ExpressionNode, Type> inferMap = new IdentityHashMap<ExpressionNode, Type>();


    public static TypeInfo inferTypes(ExpressionNode node, VariableAnalyzer analyzer) {
        TypeInference typeInference = new TypeInference(analyzer);
        typeInference.infer(node);
        return new TypeInfo(typeInference.inferMap);
    }

    private TypeInference(VariableAnalyzer analyzer) {
        this.analyzer = analyzer;
    }



    private Type infer(ExpressionNode node) {
        Type type = node.accept(this);
        inferMap.put(node, type);
        return type;
    }

    @Override
    public Type evaluate(PropertyAccess propertyAccess) {
        infer(propertyAccess.getTarget());
        infer(propertyAccess.getProperty());
        return Type.UNKNOWN;
    }

    @Override
    public Type evaluate(Identifier identifier) {
        return analyzer.descriptor(identifier.getName()).getType();
    }

    @Override
    public Type evaluate(StringConstant text) {
        return Type.STRING;
    }

    @Override
    public Type evaluate(BinaryOperation binaryOperation) {
        Type leftType = infer(binaryOperation.getLeftOperand());
        Type rightType = infer(binaryOperation.getRightOperand());
        BinaryOpGen opGen = Operators.generatorFor(binaryOperation.getOperator());
        return opGen.returnType(leftType, rightType);
    }

    @Override
    public Type evaluate(BooleanConstant booleanConstant) {
        return Type.BOOLEAN;
    }

    @Override
    public Type evaluate(NumericConstant numericConstant) {
        Number number = numericConstant.getValue();
        if (number instanceof Integer || number instanceof Long) {
            return Type.LONG;
        }
        if (number instanceof Float || number instanceof Double) {
            return Type.DOUBLE;
        }
        return Type.UNKNOWN;
    }

    @Override
    public Type evaluate(UnaryOperation unaryOperation) {
        infer(unaryOperation.getTarget());
        UnaryOpGen opGen = Operators.generatorFor(unaryOperation.getOperator());
        return opGen.returnType(infer(unaryOperation.getTarget()));
    }

    @Override
    public Type evaluate(TernaryOperator ternaryOperator) {
        infer(ternaryOperator.getCondition());
        Type thenType = infer(ternaryOperator.getThenBranch());
        Type elseType = infer(ternaryOperator.getElseBranch());
        if (thenType.equals(elseType)) {
            return thenType;
        }
        return Type.UNKNOWN;
    }

    @Override
    public Type evaluate(RuntimeCall runtimeCall) {
        inferAll(runtimeCall.getArguments());
        return Type.UNKNOWN;
    }

    @Override
    public Type evaluate(MapLiteral mapLiteral) {
        inferAll(mapLiteral.getMap().values());
        return Type.MAP;
    }

    @Override
    public Type evaluate(ArrayLiteral arrayLiteral) {
        inferAll(arrayLiteral.getItems());
        return Type.UNKNOWN;
    }

    @Override
    public Type evaluate(NullLiteral nullLiteral) {
        return Type.UNKNOWN;
    }

    private void inferAll(Iterable<ExpressionNode> nodes) {
        for (ExpressionNode node : nodes) {
            infer(node);
        }
    }
}
