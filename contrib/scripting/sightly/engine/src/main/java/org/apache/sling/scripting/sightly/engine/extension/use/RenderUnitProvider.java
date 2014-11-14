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

import javax.script.Bindings;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;

import org.apache.sling.scripting.sightly.api.ProviderOutcome;
import org.apache.sling.scripting.sightly.api.RenderContext;
import org.apache.sling.scripting.sightly.api.RenderUnit;
import org.apache.sling.scripting.sightly.api.UnitLocator;
import org.apache.sling.scripting.sightly.api.UseProvider;
import org.apache.sling.scripting.sightly.api.UseProviderComponent;
import org.apache.sling.scripting.sightly.engine.SightlyScriptEngineFactory;
import org.apache.sling.scripting.sightly.api.ProviderOutcome;
import org.apache.sling.scripting.sightly.api.RenderUnit;
import org.apache.sling.scripting.sightly.api.UnitLocator;
import org.apache.sling.scripting.sightly.api.UseProvider;

/**
 * Interprets identifiers as paths to other Sightly templates
 */
@Component
@Service(UseProvider.class)
public class RenderUnitProvider extends UseProviderComponent {

    @Override
    public ProviderOutcome provide(String identifier, RenderContext renderContext, Bindings arguments) {
        if (identifier.endsWith("." + SightlyScriptEngineFactory.EXTENSION)) {
            UnitLocator unitLocator = renderContext.getUnitLocator();
            RenderUnit renderUnit = unitLocator.locate(identifier);
            return ProviderOutcome.notNullOrFailure(renderUnit);
        }
        return ProviderOutcome.failure();
    }
}
