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

import java.util.Map;
import java.util.Set;

import org.apache.sling.scripting.sightly.compiler.SightlyCompilerException;
import org.apache.sling.scripting.sightly.compiler.commands.Procedure;
import org.apache.sling.scripting.sightly.compiler.expression.Expression;
import org.apache.sling.scripting.sightly.compiler.expression.ExpressionNode;
import org.apache.sling.scripting.sightly.impl.compiler.Patterns;
import org.apache.sling.scripting.sightly.impl.compiler.PushStream;
import org.apache.sling.scripting.sightly.impl.compiler.frontend.CompilerContext;

/**
 * The template plugin
 */
public class TemplatePlugin extends AbstractPlugin {

    public TemplatePlugin() {
        name = "template";
        priority = Integer.MIN_VALUE;
    }

    @Override
    public PluginInvoke invoke(final Expression expressionNode, final PluginCallInfo callInfo, CompilerContext compilerContext) {
        return new DefaultPluginInvoke() {

            @Override
            public void beforeTagOpen(PushStream stream) {
                //ignoring template tags
                Patterns.beginStreamIgnore(stream);
            }

            @Override
            public void beforeElement(PushStream stream, String tagName) {
                String name = decodeName();
                Set<String> parameters = extractParameters();
                stream.write(new Procedure.Start(name, parameters));
            }

            @Override
            public void afterElement(PushStream stream) {
                stream.write(Procedure.END);
            }

            @Override
            public void afterTagOpen(PushStream stream) {
                Patterns.endStreamIgnore(stream); //resuming normal operation
            }

            @Override
            public void beforeTagClose(PushStream stream, boolean isSelfClosing) {
                Patterns.beginStreamIgnore(stream); //ignoring closing tags
            }

            @Override
            public void afterTagClose(PushStream stream, boolean isSelfClosing) {
                Patterns.endStreamIgnore(stream);
            }

            private Set<String> extractParameters() {
                Map<String, ExpressionNode> options = expressionNode.getOptions();
                return options.keySet();
            }

            private String decodeName() {
                String[] arguments = callInfo.getArguments();
                if (arguments.length == 0) {
                    throw new SightlyCompilerException("Template name was not provided");
                }
                return arguments[0];
            }
        };
    }
}
