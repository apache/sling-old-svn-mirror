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
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.Identifier;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.MapLiteral;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.RuntimeCall;
import org.apache.sling.scripting.sightly.impl.compiler.ris.command.OutVariable;
import org.apache.sling.scripting.sightly.impl.compiler.ris.command.Patterns;
import org.apache.sling.scripting.sightly.impl.compiler.ris.command.VariableBinding;
import org.apache.sling.scripting.sightly.impl.compiler.common.DefaultPluginInvoke;
import org.apache.sling.scripting.sightly.impl.compiler.frontend.CompilerContext;
import org.apache.sling.scripting.sightly.impl.compiler.util.stream.PushStream;

/**
 * Implementation for the include plugin
 */
@Component
@Service(Plugin.class)
@Properties({
        @Property(name = "service.description", value = "Sightly Include Block Plugin"),
        @Property(name = Plugin.SCR_PROP_NAME_BLOCK_NAME, value = "include"),
        @Property(name = Plugin.SCR_PROP_NAME_PRIORITY, intValue = PluginComponent.DEFAULT_PRIORITY)
})
public class IncludePlugin extends PluginComponent {

    public static final String FUNCTION = "include";

    @Override
    public PluginInvoke invoke(final Expression expression, final PluginCallInfo callInfo, final CompilerContext compilerContext) {
        return new DefaultPluginInvoke() {

            @Override
            public void beforeChildren(PushStream stream) {
                String includedContentVar = compilerContext.generateVariable("includedResult");
                String pathVar = compilerContext.generateVariable("includePath");
                stream.emit(new VariableBinding.Start(pathVar, expression.getRoot()));
                stream.emit(new VariableBinding.Start(includedContentVar,
                        new RuntimeCall(FUNCTION, new Identifier(pathVar), new MapLiteral(expression.getOptions()))));
                stream.emit(new OutVariable(includedContentVar));
                stream.emit(VariableBinding.END); //end includedContentVar
                stream.emit(VariableBinding.END); //end pathVar
                Patterns.beginStreamIgnore(stream);
            }

            @Override
            public void afterChildren(PushStream stream) {
                Patterns.endStreamIgnore(stream);
            }
        };
    }
}
