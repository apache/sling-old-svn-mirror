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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.scripting.sightly.impl.compiler.common.DefaultPluginInvoke;
import org.apache.sling.scripting.sightly.impl.compiler.expression.Expression;
import org.apache.sling.scripting.sightly.impl.compiler.expression.ExpressionNode;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.BooleanConstant;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.StringConstant;
import org.apache.sling.scripting.sightly.impl.compiler.frontend.CompilerContext;
import org.apache.sling.scripting.sightly.impl.compiler.ris.Command;
import org.apache.sling.scripting.sightly.impl.compiler.ris.command.Conditional;
import org.apache.sling.scripting.sightly.impl.compiler.ris.command.Patterns;
import org.apache.sling.scripting.sightly.impl.compiler.ris.command.VariableBinding;
import org.apache.sling.scripting.sightly.impl.compiler.util.stream.PushStream;

/**
 * The unwrapped plugin
 */
@Component
@Service(Plugin.class)
@Properties({
        @Property(name = Plugin.SCR_PROP_NAME_BLOCK_NAME, value = "unwrap"),
        @Property(name = Plugin.SCR_PROP_NAME_PRIORITY, intValue = 125)
})
public class UnwrapPlugin extends PluginComponent {

    @Override
    public PluginInvoke invoke(final Expression expression, PluginCallInfo callInfo, final CompilerContext compilerContext) {
        return new DefaultPluginInvoke() {

            private final String variable = compilerContext.generateVariable("unwrapCondition");
            private final Command unwrapTest = new Conditional.Start(variable, false);
            private boolean isSlyTag = false;

            @Override
            public void beforeElement(PushStream stream, String tagName) {
                isSlyTag = "sly".equals(tagName.toLowerCase());
                stream.emit(new VariableBinding.Start(variable, testNode()));
            }

            @Override
            public void beforeTagOpen(PushStream stream) {
                if (isSlyTag) {
                    Patterns.endStreamIgnore(stream);
                }
                stream.emit(unwrapTest);
            }

            @Override
            public void afterTagOpen(PushStream stream) {
                stream.emit(Conditional.END);
                if (isSlyTag) {
                    Patterns.beginStreamIgnore(stream);
                }
            }

            @Override
            public void beforeTagClose(PushStream stream, boolean isSelfClosing) {
                if (isSlyTag) {
                    Patterns.endStreamIgnore(stream);
                }
                stream.emit(unwrapTest);
            }

            @Override
            public void afterTagClose(PushStream stream, boolean isSelfClosing) {
                stream.emit(Conditional.END);
                if (isSlyTag) {
                    Patterns.beginStreamIgnore(stream);
                }
            }

            @Override
            public void afterElement(PushStream stream) {
                stream.emit(VariableBinding.END);
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
