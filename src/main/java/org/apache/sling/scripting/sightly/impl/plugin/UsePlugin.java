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
import org.apache.sling.scripting.sightly.impl.compiler.expression.Expression;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.MapLiteral;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.RuntimeCall;
import org.apache.sling.scripting.sightly.impl.compiler.ris.command.VariableBinding;
import org.apache.sling.scripting.sightly.impl.compiler.common.DefaultPluginInvoke;
import org.apache.sling.scripting.sightly.impl.compiler.frontend.CompilerContext;
import org.apache.sling.scripting.sightly.impl.compiler.util.stream.PushStream;

@Component
@Service
@Properties({
        @Property(name = "service.description", value = "Sightly Use Block Plugin"),
        @Property(name = Plugin.SCR_PROP_NAME_BLOCK_NAME, value = "use"),
        @Property(name = Plugin.SCR_PROP_NAME_PRIORITY, intValue = 1)
})
public class UsePlugin extends PluginComponent {

    public static final String FUNCTION_NAME = "use";

    private static final String DEFAULT_VARIABLE_NAME = "useBean";

    @Override
    public PluginInvoke invoke(final Expression expression,
                               final PluginCallInfo callInfo,
                               final CompilerContext compilerContext) {
        return new DefaultPluginInvoke() {

            @Override
            public void beforeElement(PushStream stream, String tagName) {
                String variableName = decodeVariableName();
                stream.emit(new VariableBinding.Global(variableName,
                        new RuntimeCall(FUNCTION_NAME, expression.getRoot(), new MapLiteral(expression.getOptions()))));
            }

            private String decodeVariableName() {
                String[] arguments = callInfo.getArguments();
                if (arguments.length > 0) {
                    return arguments[0];
                }
                return DEFAULT_VARIABLE_NAME;
            }
        };
    }
}
