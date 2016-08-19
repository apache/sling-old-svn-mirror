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

import org.apache.sling.scripting.sightly.compiler.RuntimeFunction;
import org.apache.sling.scripting.sightly.compiler.commands.Conditional;
import org.apache.sling.scripting.sightly.compiler.commands.OutText;
import org.apache.sling.scripting.sightly.compiler.commands.OutputVariable;
import org.apache.sling.scripting.sightly.compiler.commands.VariableBinding;
import org.apache.sling.scripting.sightly.compiler.expression.Expression;
import org.apache.sling.scripting.sightly.compiler.expression.ExpressionNode;
import org.apache.sling.scripting.sightly.compiler.expression.MarkupContext;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.RuntimeCall;
import org.apache.sling.scripting.sightly.impl.compiler.PushStream;
import org.apache.sling.scripting.sightly.impl.compiler.frontend.CompilerContext;
import org.apache.sling.scripting.sightly.impl.filter.ExpressionContext;

public class ElementPlugin extends AbstractPlugin {

    public ElementPlugin() {
        name = "element";
    }

    @Override
    public PluginInvoke invoke(final Expression expression, final PluginCallInfo callInfo, final CompilerContext compilerContext) {

        return new DefaultPluginInvoke() {

            private final ExpressionNode node = adjustContext(compilerContext, expression, MarkupContext.ELEMENT_NAME, ExpressionContext
                    .ELEMENT).getRoot();
            private String tagVar = compilerContext.generateVariable("tagVar");

            @Override
            public void beforeElement(PushStream stream, String tagName) {
                stream.write(new VariableBinding.Start(tagVar, node));
            }

            @Override
            public void beforeTagOpen(PushStream stream) {
                stream.write(new Conditional.Start(tagVar, true));
                stream.write(new OutText("<"));
                stream.write(new OutputVariable(tagVar));
                stream.write(Conditional.END);
                stream.write(new Conditional.Start(tagVar, false));
            }

            @Override
            public void beforeAttributes(PushStream stream) {
                stream.write(Conditional.END);
            }

            @Override
            public void beforeTagClose(PushStream stream, boolean isSelfClosing) {
                if (!isSelfClosing) {
                    stream.write(new Conditional.Start(tagVar, true));
                    stream.write(new OutText("</"));
                    stream.write(new OutputVariable(tagVar));
                    stream.write(new OutText(">"));
                    stream.write(Conditional.END);
                }
                stream.write(new Conditional.Start(tagVar, false));
            }

            @Override
            public void afterTagClose(PushStream stream, boolean isSelfClosing) {
                stream.write(Conditional.END);
            }

            @Override
            public void afterElement(PushStream stream) {
                stream.write(VariableBinding.END);
            }
        };

    }

    private Expression adjustContext(CompilerContext compilerContext, Expression expression, MarkupContext markupContext,
                                     ExpressionContext expressionContext) {
        ExpressionNode root = expression.getRoot();
        if (root instanceof RuntimeCall) {
            RuntimeCall runtimeCall = (RuntimeCall) root;
            if (runtimeCall.getFunctionName().equals(RuntimeFunction.XSS)) {
                return expression;
            }
        }
        return compilerContext.adjustToContext(expression, markupContext, expressionContext);
    }
}
