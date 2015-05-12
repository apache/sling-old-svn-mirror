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
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.scripting.sightly.impl.compiler.expression.Expression;
import org.apache.sling.scripting.sightly.impl.compiler.expression.ExpressionNode;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.MapLiteral;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.RuntimeCall;
import org.apache.sling.scripting.sightly.impl.compiler.ris.command.OutVariable;
import org.apache.sling.scripting.sightly.impl.compiler.ris.command.Patterns;
import org.apache.sling.scripting.sightly.impl.compiler.ris.command.VariableBinding;
import org.apache.sling.scripting.sightly.impl.compiler.common.DefaultPluginInvoke;
import org.apache.sling.scripting.sightly.impl.compiler.frontend.CompilerContext;
import org.apache.sling.scripting.sightly.impl.compiler.util.stream.PushStream;

/**
 * The resource plugin
 */
@Component
@Service(Plugin.class)
@Properties({
        @Property(name = "service.description", value = "Sightly Resource Block Plugin"),
        @Property(name = Plugin.SCR_PROP_NAME_BLOCK_NAME, value = "resource"),
        @Property(name = Plugin.SCR_PROP_NAME_PRIORITY, intValue = PluginComponent.DEFAULT_PRIORITY)
})
public class ResourcePlugin extends PluginComponent {

    public static final String FUNCTION = "includeResource";

    @Override
    public PluginInvoke invoke(final Expression expression, final PluginCallInfo callInfo, final CompilerContext compilerContext) {

        return new DefaultPluginInvoke() {

            private Map<String, ExpressionNode> expressionOptions = new HashMap<String, ExpressionNode>(expression.getOptions());

            @Override
            public void beforeChildren(PushStream stream) {
                String resourceVar = compilerContext.generateVariable("resourceContent");
                stream.emit(new VariableBinding.Start(resourceVar,
                        new RuntimeCall(FUNCTION,
                                expression.getRoot(), new MapLiteral(expressionOptions))));
                stream.emit(new OutVariable(resourceVar));
                stream.emit(VariableBinding.END);
                Patterns.beginStreamIgnore(stream);
            }

            @Override
            public void afterChildren(PushStream stream) {
                Patterns.endStreamIgnore(stream);
            }

        };
    }
}
