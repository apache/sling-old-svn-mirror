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
package org.apache.sling.scripting.sightly.impl.plugin;

import java.util.HashMap;

import org.apache.sling.scripting.sightly.impl.compiler.Syntax;
import org.apache.sling.scripting.sightly.compiler.expression.Expression;
import org.apache.sling.scripting.sightly.compiler.expression.ExpressionNode;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.BinaryOperation;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.BinaryOperator;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.Identifier;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.MapLiteral;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.NumericConstant;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.UnaryOperation;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.UnaryOperator;
import org.apache.sling.scripting.sightly.compiler.commands.Conditional;
import org.apache.sling.scripting.sightly.compiler.commands.Loop;
import org.apache.sling.scripting.sightly.compiler.commands.VariableBinding;
import org.apache.sling.scripting.sightly.impl.compiler.frontend.CompilerContext;
import org.apache.sling.scripting.sightly.impl.compiler.PushStream;

public class ListPlugin extends AbstractPlugin {

    private static final String INDEX = "index";
    private static final String COUNT = "count";
    private static final String FIRST = "first";
    private static final String MIDDLE = "middle";
    private static final String LAST = "last";
    private static final String ODD = "odd";
    private static final String EVEN = "even";

    public ListPlugin() {
        name = "list";
        priority = 130;
    }

    @Override
    public PluginInvoke invoke(final Expression expression, final PluginCallInfo callInfo, final CompilerContext compilerContext) {
        return new DefaultPluginInvoke() {

            private String listVariable = compilerContext.generateVariable("collectionVar");
            private String collectionSizeVar = compilerContext.generateVariable("size");

            @Override
            public void beforeElement(PushStream stream, String tagName) {
                stream.write(new VariableBinding.Start(listVariable, expression.getRoot()));
                stream.write(new VariableBinding.Start(collectionSizeVar,
                        new UnaryOperation(UnaryOperator.LENGTH, new Identifier(listVariable))));
                stream.write(new Conditional.Start(collectionSizeVar, true));

            }

            @Override
            public void beforeChildren(PushStream stream) {
                String itemVariable = decodeItemVariable();
                String loopStatusVar = Syntax.itemLoopStatusVariable(itemVariable);
                String indexVariable = compilerContext.generateVariable("index");
                stream.write(new Loop.Start(listVariable, itemVariable, indexVariable));
                stream.write(new VariableBinding.Start(loopStatusVar, buildStatusObj(indexVariable, collectionSizeVar)));
            }

            @Override
            public void afterChildren(PushStream stream) {
                stream.write(VariableBinding.END);
                stream.write(Loop.END);
            }

            @Override
            public void afterElement(PushStream stream) {
                stream.write(Conditional.END);
                stream.write(VariableBinding.END);
                stream.write(VariableBinding.END);
            }


            private String decodeItemVariable() {
                String[] args = callInfo.getArguments();
                if (args.length > 0) {
                    return args[0];
                }
                return Syntax.DEFAULT_LIST_ITEM_VAR_NAME;
            }

            private MapLiteral buildStatusObj(String indexVar, String sizeVar) {
                HashMap<String, ExpressionNode> obj = new HashMap<>();
                Identifier indexId = new Identifier(indexVar);
                BinaryOperation firstExpr = new BinaryOperation(BinaryOperator.EQ, indexId, NumericConstant.ZERO);
                BinaryOperation lastExpr = new BinaryOperation(
                        BinaryOperator.EQ,
                        indexId,
                        new BinaryOperation(BinaryOperator.SUB, new Identifier(sizeVar), NumericConstant.ONE));
                obj.put(INDEX, indexId);
                obj.put(COUNT, new BinaryOperation(BinaryOperator.ADD, indexId, NumericConstant.ONE));
                obj.put(FIRST, firstExpr);
                obj.put(MIDDLE, new UnaryOperation(
                        UnaryOperator.NOT,
                        new BinaryOperation(BinaryOperator.OR, firstExpr, lastExpr)));
                obj.put(LAST, lastExpr);
                obj.put(ODD, parityCheck(indexId, NumericConstant.ZERO));
                obj.put(EVEN, parityCheck(indexId, NumericConstant.ONE));
                return new MapLiteral(obj);
            }

            private ExpressionNode parityCheck(ExpressionNode numericExpression, NumericConstant expected) {
                return new BinaryOperation(
                        BinaryOperator.EQ,
                        new BinaryOperation(BinaryOperator.REM, numericExpression, NumericConstant.TWO),
                        expected);
            }
        };
    }
}
