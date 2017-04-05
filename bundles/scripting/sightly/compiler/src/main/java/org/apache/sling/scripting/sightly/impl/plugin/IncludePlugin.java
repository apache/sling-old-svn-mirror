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
import org.apache.sling.scripting.sightly.compiler.commands.OutputVariable;
import org.apache.sling.scripting.sightly.impl.compiler.Patterns;
import org.apache.sling.scripting.sightly.compiler.commands.VariableBinding;
import org.apache.sling.scripting.sightly.impl.compiler.PushStream;
import org.apache.sling.scripting.sightly.compiler.expression.Expression;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.Identifier;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.MapLiteral;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.RuntimeCall;
import org.apache.sling.scripting.sightly.impl.compiler.frontend.CompilerContext;

/**
 * Implementation for the include plugin
 */
public class IncludePlugin extends AbstractPlugin {

    public IncludePlugin() {
        name = "include";
    }

    @Override
    public PluginInvoke invoke(final Expression expression, final PluginCallInfo callInfo, final CompilerContext compilerContext) {
        return new DefaultPluginInvoke() {

            @Override
            public void beforeChildren(PushStream stream) {
                String includedContentVar = compilerContext.generateVariable("includedResult");
                String pathVar = compilerContext.generateVariable("includePath");
                stream.write(new VariableBinding.Start(pathVar, expression.getRoot()));
                stream.write(new VariableBinding.Start(includedContentVar,
                        new RuntimeCall(RuntimeFunction.INCLUDE, new Identifier(pathVar), new MapLiteral(expression.getOptions()))));
                stream.write(new OutputVariable(includedContentVar));
                stream.write(VariableBinding.END); //end includedContentVar
                stream.write(VariableBinding.END); //end pathVar
                Patterns.beginStreamIgnore(stream);
            }

            @Override
            public void afterChildren(PushStream stream) {
                Patterns.endStreamIgnore(stream);
            }
        };
    }
}
