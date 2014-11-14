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

package org.apache.sling.scripting.sightly.plugin;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;

import org.apache.sling.scripting.sightly.compiler.api.expression.Expression;
import org.apache.sling.scripting.sightly.compiler.api.expression.ExpressionNode;
import org.apache.sling.scripting.sightly.compiler.api.plugin.CompilerContext;
import org.apache.sling.scripting.sightly.compiler.api.plugin.MarkupContext;
import org.apache.sling.scripting.sightly.compiler.api.plugin.Plugin;
import org.apache.sling.scripting.sightly.compiler.api.plugin.PluginCallInfo;
import org.apache.sling.scripting.sightly.compiler.api.plugin.PluginInvoke;
import org.apache.sling.scripting.sightly.compiler.api.ris.command.Conditional;
import org.apache.sling.scripting.sightly.compiler.api.ris.command.OutText;
import org.apache.sling.scripting.sightly.compiler.api.ris.command.OutVariable;
import org.apache.sling.scripting.sightly.compiler.api.ris.command.VariableBinding;
import org.apache.sling.scripting.sightly.compiler.common.DefaultPluginInvoke;
import org.apache.sling.scripting.sightly.compiler.util.stream.PushStream;
import org.apache.sling.scripting.sightly.compiler.api.expression.Expression;
import org.apache.sling.scripting.sightly.compiler.api.plugin.CompilerContext;
import org.apache.sling.scripting.sightly.compiler.api.plugin.MarkupContext;
import org.apache.sling.scripting.sightly.compiler.api.plugin.Plugin;
import org.apache.sling.scripting.sightly.compiler.api.plugin.PluginCallInfo;
import org.apache.sling.scripting.sightly.compiler.api.ris.command.Conditional;
import org.apache.sling.scripting.sightly.compiler.api.ris.command.OutText;
import org.apache.sling.scripting.sightly.compiler.api.ris.command.VariableBinding;
import org.apache.sling.scripting.sightly.compiler.util.stream.PushStream;

/**
 * Implementation for the {@code data-sly-element} plugin.
 */
@Component
@Service
@Properties({
        @Property(name = "service.description", value = "Sightly Resource Block Plugin"),
        @Property(name = Plugin.SCR_PROP_NAME_BLOCK_NAME, value = "element"),
        @Property(name = Plugin.SCR_PROP_NAME_PRIORITY, intValue = PluginComponent.DEFAULT_PRIORITY)
})
public class ElementPlugin extends PluginComponent {

    @Override
    public PluginInvoke invoke(final Expression expression, final PluginCallInfo callInfo, final CompilerContext compilerContext) {

        return new DefaultPluginInvoke() {

            private final ExpressionNode node = compilerContext.adjustToContext(expression, MarkupContext.ELEMENT_NAME).getRoot();
            private String tagVar = compilerContext.generateVariable("tagVar");

            @Override
            public void beforeElement(PushStream stream, String tagName) {
                stream.emit(new VariableBinding.Start(tagVar, node));
            }

            @Override
            public void beforeTagOpen(PushStream stream) {
                stream.emit(new Conditional.Start(tagVar, true));
                stream.emit(new OutText("<"));
                stream.emit(new OutVariable(tagVar));
                stream.emit(Conditional.END);
                stream.emit(new Conditional.Start(tagVar, false));
            }

            @Override
            public void beforeAttributes(PushStream stream) {
                stream.emit(Conditional.END);
            }

            @Override
            public void beforeTagClose(PushStream stream, boolean isSelfClosing) {
                if (!isSelfClosing) {
                    stream.emit(new Conditional.Start(tagVar, true));
                    stream.emit(new OutText("</"));
                    stream.emit(new OutVariable(tagVar));
                    stream.emit(new OutText(">"));
                    stream.emit(Conditional.END);
                }
                stream.emit(new Conditional.Start(tagVar, false));
            }

            @Override
            public void afterTagClose(PushStream stream, boolean isSelfClosing) {
                stream.emit(Conditional.END);
            }

            @Override
            public void afterElement(PushStream stream) {
                stream.emit(VariableBinding.END);
            }
        };

    }
}
