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
package org.apache.sling.scripting.sightly.js;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.scripting.sightly.api.ProviderOutcome;
import org.apache.sling.scripting.sightly.api.RenderContext;
import org.apache.sling.scripting.sightly.api.UseProvider;
import org.apache.sling.scripting.sightly.api.UseProviderComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.sling.scripting.sightly.js.async.AsyncContainer;
import org.apache.sling.scripting.sightly.js.async.AsyncExtractor;
import org.apache.sling.scripting.sightly.js.rhino.JsValueAdapter;

/**
 * Use provider for JS scripts. Ensures proper integration between Sightly & JS code-behind.
 */
@Component
@Service(UseProvider.class)
@Property(name = UseProviderComponent.PRIORITY, intValue = -1)
public class JsUseProvider extends UseProviderComponent {

    private static final String JS_ENGINE_NAME = "javascript";

    private static final Logger log = LoggerFactory.getLogger(JsUseProvider.class);
    private static final JsValueAdapter jsValueAdapter = new JsValueAdapter(new AsyncExtractor());

    @Reference
    private ScriptEngineManager scriptEngineManager = null;

    @Reference
    private ResourceResolverFactory rrf = null;

    @Override
    public ProviderOutcome provide(String identifier, RenderContext renderContext, Bindings arguments) {
        Bindings globalBindings = renderContext.getBindings();
        if (!Utils.isJsScript(identifier)) {
            return ProviderOutcome.failure();
        }
        ScriptEngine jsEngine = obtainEngine();
        if (jsEngine == null) {
            log.warn("No JavaScript engine defined");
            return ProviderOutcome.failure();
        }
        SlingScriptHelper scriptHelper = Utils.getHelper(globalBindings);
        JsEnvironment environment = null;
        ResourceResolver adminResolver = null;
        try {
            environment = new JsEnvironment(jsEngine);
            environment.initialize();
            String callerPath = scriptHelper.getScript().getScriptResource().getPath();
            boolean allowedExecutablePath = false;
            adminResolver = rrf.getAdministrativeResourceResolver(null);
            for (String path : adminResolver.getSearchPath()) {
                if (callerPath.startsWith(path)) {
                    allowedExecutablePath = true;
                    break;
                }
            }
            if (allowedExecutablePath) {
                Resource caller = adminResolver.getResource(callerPath);
                AsyncContainer asyncContainer = environment.run(caller, identifier, globalBindings, arguments);
                return ProviderOutcome.success(jsValueAdapter.adapt(asyncContainer));
            }
            return ProviderOutcome.failure();
        } catch (LoginException e) {
            log.error("Unable to load JS script " + identifier, e);
        } finally {
            if (environment != null) {
                environment.cleanup();
            }
            if (adminResolver != null) {
                adminResolver.close();
            }
        }
        return ProviderOutcome.failure();
    }

    private ScriptEngine obtainEngine() {
        return scriptEngineManager.getEngineByName(JS_ENGINE_NAME);
    }
}
