/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.apache.sling.scripting.sightly.java.compiler.impl;

import java.util.Map;

import org.apache.sling.scripting.sightly.compiler.expression.ExpressionNode;
import org.apache.sling.scripting.sightly.compiler.expression.SideEffectVisitor;
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
import org.apache.sling.scripting.sightly.compiler.expression.nodes.UnaryOperator;
import org.apache.sling.scripting.sightly.java.compiler.impl.operator.BinaryOpGen;
import org.apache.sling.scripting.sightly.java.compiler.impl.operator.Operators;
import org.apache.sling.scripting.sightly.java.compiler.impl.operator.UnaryOpGen;

/**
 * Builds expressions within a sling source file.
 */
public final class ExpressionTranslator extends SideEffectVisitor {

    private final JavaSource source;
    private final VariableAnalyzer analyzer;
    private final TypeInfo typeInfo;

    private ExpressionTranslator(JavaSource source, VariableAnalyzer analyzer, TypeInfo typeInfo) {
        this.source = source;
        this.analyzer = analyzer;
        this.typeInfo= typeInfo;
    }

    public static void buildExpression(ExpressionNode node,
                                       JavaSource source,
                                       VariableAnalyzer analyzer,
                                       TypeInfo typeInfo) {
        ExpressionTranslator builder = new ExpressionTranslator(source, analyzer, typeInfo);
        builder.traverse(node);
    }

    public void traverse(ExpressionNode node) {
        visit(node);
    }

    private void visit(ExpressionNode node) {
        node.accept(this);
    }

    @Override
    public void visit(PropertyAccess propertyAccess) {
        if (typeInfo.typeOf(propertyAccess.getTarget()) == Type.MAP) {
            //Special optimization for maps
            visit(propertyAccess.getTarget());
            source.startCall(SourceGenConstants.MAP_GET, true);
            visit(propertyAccess.getProperty());
            source.endCall();
        } else {
            source.objectModel().startCall(SourceGenConstants.ROM_RESOLVE_PROPERTY, true);
            visit(propertyAccess.getTarget());
            source.separateArgument();
            visit(propertyAccess.getProperty());
            source.endCall();
        }
    }

    @Override
    public void visit(Identifier identifier) {
        String safeName = analyzer.assignedName(identifier.getName());
        source.append(safeName);
    }

    @Override
    public void visit(StringConstant text) {
        source.stringLiteral(text.getText());
    }

    @Override
    public void visit(BinaryOperation binaryOperation) {
        BinaryOpGen opGen = Operators.generatorFor(binaryOperation.getOperator());
        source.startExpression();
        opGen.generate(source, this,
                typeInfo.getTyped(binaryOperation.getLeftOperand()),
                typeInfo.getTyped(binaryOperation.getRightOperand()));
        source.endExpression();
    }

    @Override
    public void visit(BooleanConstant booleanConstant) {
        source.append(Boolean.toString(booleanConstant.getValue()));
    }

    @Override
    public void visit(NumericConstant numericConstant) {
        source.append(numericConstant.getValue().toString()); //todo: check correctness
    }

    @Override
    public void visit(UnaryOperation unaryOperation) {
        UnaryOperator operator = unaryOperation.getOperator();
        ExpressionNode operand = unaryOperation.getTarget();
        UnaryOpGen unaryOpGen = Operators.generatorFor(operator);
        source.startExpression();
        unaryOpGen.generate(source, this, typeInfo.getTyped(operand));
        source.endExpression();
    }

    @Override
    public void visit(TernaryOperator ternaryOperator) {
        GenHelper.generateTernary(source, this,
                typeInfo.getTyped(ternaryOperator.getCondition()),
                typeInfo.getTyped(ternaryOperator.getThenBranch()),
                typeInfo.getTyped(ternaryOperator.getElseBranch()));
    }

    @Override
    public void visit(RuntimeCall runtimeCall) {
        source.startMethodCall(SourceGenConstants.RENDER_CONTEXT_INSTANCE, SourceGenConstants.RUNTIME_CALL_METHOD)
                .stringLiteral(runtimeCall.getFunctionName());
        for (ExpressionNode arg : runtimeCall.getArguments()) {
            source.separateArgument();
            visit(arg);
        }
        source.endCall();
    }

    @Override
    public void visit(MapLiteral mapLiteral) {
        source.startCall(SourceGenConstants.START_MAP_METHOD).endCall();
        for (Map.Entry<String, ExpressionNode> entry : mapLiteral.getMap().entrySet()) {
            source.startCall(SourceGenConstants.MAP_TYPE_ADD, true)
                    .stringLiteral(entry.getKey())
                    .separateArgument();
            visit(entry.getValue());
            source.endCall();
        }
    }

    @Override
    public void visit(ArrayLiteral arrayLiteral) {
        source.startExpression().startArray();
        boolean needsComma = false;
        for (ExpressionNode node : arrayLiteral.getItems()) {
            if (needsComma) {
                source.separateArgument();
            }
            visit(node);
            needsComma = true;
        }
        source.endArray().endExpression();
    }

    @Override
    public void visit(NullLiteral nullLiteral) {
        source.nullLiteral();
    }

    public VariableAnalyzer getAnalyzer() {
        return analyzer;
    }
}
