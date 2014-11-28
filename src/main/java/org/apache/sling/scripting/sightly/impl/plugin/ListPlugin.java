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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.scripting.sightly.impl.compiler.Syntax;
import org.apache.sling.scripting.sightly.impl.compiler.expression.Expression;
import org.apache.sling.scripting.sightly.impl.compiler.expression.ExpressionNode;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.BinaryOperation;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.BinaryOperator;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.Identifier;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.MapLiteral;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.NumericConstant;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.UnaryOperation;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.UnaryOperator;
import org.apache.sling.scripting.sightly.impl.compiler.ris.command.Conditional;
import org.apache.sling.scripting.sightly.impl.compiler.ris.command.Loop;
import org.apache.sling.scripting.sightly.impl.compiler.ris.command.VariableBinding;
import org.apache.sling.scripting.sightly.impl.compiler.common.DefaultPluginInvoke;
import org.apache.sling.scripting.sightly.impl.compiler.frontend.CompilerContext;
import org.apache.sling.scripting.sightly.impl.compiler.util.stream.PushStream;

/**
 * Default implementation of the {@code data-sly-list} plugin.
 */
@Component()
@Service(Plugin.class)
@Properties({
        @Property(name = Plugin.SCR_PROP_NAME_BLOCK_NAME, value = "list"),
        @Property(name = Plugin.SCR_PROP_NAME_PRIORITY, intValue = 130)
})
public class ListPlugin extends PluginComponent {


    private static final String INDEX = "index";
    private static final String COUNT = "count";
    private static final String FIRST = "first";
    private static final String MIDDLE = "middle";
    private static final String LAST = "last";
    private static final String ODD = "odd";
    private static final String EVEN = "even";

    @Override
    public PluginInvoke invoke(final Expression expression, final PluginCallInfo callInfo, final CompilerContext compilerContext) {
        return new DefaultPluginInvoke() {

            private String listVariable = compilerContext.generateVariable("collectionVar");
            private String collectionSizeVar = compilerContext.generateVariable("size");

            @Override
            public void beforeElement(PushStream stream, String tagName) {
                stream.emit(new VariableBinding.Start(listVariable, expression.getRoot()));
                stream.emit(new VariableBinding.Start(collectionSizeVar,
                        new UnaryOperation(UnaryOperator.LENGTH, new Identifier(listVariable))));
                stream.emit(new Conditional.Start(collectionSizeVar, true));

            }

            @Override
            public void beforeChildren(PushStream stream) {
                String itemVariable = decodeItemVariable();
                String loopStatusVar = Syntax.itemLoopStatusVariable(itemVariable);
                String indexVariable = compilerContext.generateVariable("index");
                stream.emit(new Loop.Start(listVariable, itemVariable, indexVariable));
                stream.emit(new VariableBinding.Start(loopStatusVar, buildStatusObj(indexVariable, collectionSizeVar)));
            }

            @Override
            public void afterChildren(PushStream stream) {
                stream.emit(VariableBinding.END);
                stream.emit(Loop.END);
            }

            @Override
            public void afterElement(PushStream stream) {
                stream.emit(Conditional.END);
                stream.emit(VariableBinding.END);
                stream.emit(VariableBinding.END);
            }


            private String decodeItemVariable() {
                String[] args = callInfo.getArguments();
                if (args.length > 0) {
                    return args[0];
                }
                return Syntax.DEFAULT_LIST_ITEM_VAR_NAME;
            }

            private MapLiteral buildStatusObj(String indexVar, String sizeVar) {
                HashMap<String, ExpressionNode> obj = new HashMap<String, ExpressionNode>();
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
