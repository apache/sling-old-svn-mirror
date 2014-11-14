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
package org.apache.sling.scripting.sightly.engine.extension.use;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.script.Bindings;
import javax.script.SimpleBindings;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;

import org.apache.sling.scripting.sightly.api.RuntimeExtensionComponent;
import org.apache.sling.scripting.sightly.api.ExtensionInstance;
import org.apache.sling.scripting.sightly.api.ProviderOutcome;
import org.apache.sling.scripting.sightly.api.RenderContext;
import org.apache.sling.scripting.sightly.api.RuntimeExtension;
import org.apache.sling.scripting.sightly.api.RuntimeExtensionException;
import org.apache.sling.scripting.sightly.api.SightlyUseException;
import org.apache.sling.scripting.sightly.api.UseProvider;
import org.apache.sling.scripting.sightly.plugin.UsePlugin;

/**
 * Runtime extension for the USE plugin
 */
@Component
@Service(RuntimeExtension.class)
@Properties(
        @Property(name = RuntimeExtensionComponent.SCR_PROP_NAME, value = UsePlugin.FUNCTION_NAME)
)
@Reference(
        policy = ReferencePolicy.DYNAMIC,
        referenceInterface = UseProvider.class,
        name = "useProvider",
        cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE
)
public class UseRuntimeExtension extends RuntimeExtensionComponent {

    private volatile List<UseProvider> providers = Collections.emptyList();

    @Override
    @SuppressWarnings("unchecked")
    public ExtensionInstance provide(final RenderContext renderContext) {
        return new ExtensionInstance() {

            @Override
            public Object call(Object... arguments) {
                if (arguments.length != 2) {
                    throw new RuntimeExtensionException("Use extension requires two arguments");
                }
                String identifier = renderContext.getObjectModel().coerceToString(arguments[0]);
                if (StringUtils.isEmpty(identifier)) {
                    return null;
                }
                Map<String, Object> useArgumentsMap = renderContext.getObjectModel().coerceToMap(arguments[1]);
                Bindings useArguments = new SimpleBindings(Collections.unmodifiableMap(useArgumentsMap));
                for (UseProvider provider : providers) {
                    ProviderOutcome outcome = provider.provide(identifier, renderContext, useArguments);
                    if (outcome.isSuccess()) {
                        return outcome.getResult();
                    }
                }
                throw new SightlyUseException("No use provider could resolve identifier: " + identifier);
            }
        };
    }

    @SuppressWarnings("UnusedDeclaration")
    private void bindUseProvider(UseProvider provider) {
        ArrayList<UseProvider> newProviders = new ArrayList<UseProvider>(providers);
        newProviders.add(provider);
        Collections.sort(newProviders);
        providers = newProviders;
    }

    @SuppressWarnings("UnusedDeclaration")
    private void unbindUseProvider(UseProvider provider) {
        ArrayList<UseProvider> newProviders = new ArrayList<UseProvider>(providers);
        newProviders.remove(provider);
        Collections.sort(newProviders);
        providers = newProviders;
    }
}
