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

import org.apache.sling.scripting.sightly.compiler.commands.Command;
import org.apache.sling.scripting.sightly.compiler.commands.Conditional;
import org.apache.sling.scripting.sightly.impl.compiler.Patterns;
import org.apache.sling.scripting.sightly.compiler.commands.VariableBinding;
import org.apache.sling.scripting.sightly.impl.compiler.PushStream;
import org.apache.sling.scripting.sightly.compiler.expression.Expression;
import org.apache.sling.scripting.sightly.compiler.expression.ExpressionNode;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.BooleanConstant;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.StringConstant;
import org.apache.sling.scripting.sightly.impl.compiler.frontend.CompilerContext;

/**
 * The unwrapped plugin
 */
public class UnwrapPlugin extends AbstractPlugin {

    public UnwrapPlugin() {
        name = "unwrap";
        priority = 125;
    }

    @Override
    public PluginInvoke invoke(final Expression expression, PluginCallInfo callInfo, final CompilerContext compilerContext) {
        return new DefaultPluginInvoke() {

            private final String variable = compilerContext.generateVariable("unwrapCondition");
            private final Command unwrapTest = new Conditional.Start(variable, false);
            private boolean isSlyTag = false;

            @Override
            public void beforeElement(PushStream stream, String tagName) {
                isSlyTag = "sly".equals(tagName.toLowerCase());
                stream.write(new VariableBinding.Start(variable, testNode()));
            }

            @Override
            public void beforeTagOpen(PushStream stream) {
                if (isSlyTag) {
                    Patterns.endStreamIgnore(stream);
                }
                stream.write(unwrapTest);
            }

            @Override
            public void afterTagOpen(PushStream stream) {
                stream.write(Conditional.END);
                if (isSlyTag) {
                    Patterns.beginStreamIgnore(stream);
                }
            }

            @Override
            public void beforeTagClose(PushStream stream, boolean isSelfClosing) {
                if (isSlyTag) {
                    Patterns.endStreamIgnore(stream);
                }
                stream.write(unwrapTest);
            }

            @Override
            public void afterTagClose(PushStream stream, boolean isSelfClosing) {
                stream.write(Conditional.END);
                if (isSlyTag) {
                    Patterns.beginStreamIgnore(stream);
                }
            }

            @Override
            public void afterElement(PushStream stream) {
                stream.write(VariableBinding.END);
            }

            private ExpressionNode testNode() {
                return (isEmptyExpression(expression.getRoot())) ? BooleanConstant.TRUE : expression.getRoot();
            }

            private boolean isEmptyExpression(ExpressionNode node) {
                return node instanceof StringConstant && ((StringConstant) node).getText().isEmpty();
            }
        };
    }
}
