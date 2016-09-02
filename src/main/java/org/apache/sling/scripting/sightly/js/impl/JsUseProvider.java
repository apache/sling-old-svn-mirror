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
package org.apache.sling.scripting.sightly.js.impl;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.scripting.sightly.SightlyException;
import org.apache.sling.scripting.sightly.js.impl.async.AsyncContainer;
import org.apache.sling.scripting.sightly.js.impl.async.AsyncExtractor;
import org.apache.sling.scripting.sightly.js.impl.jsapi.ProxyAsyncScriptableFactory;
import org.apache.sling.scripting.sightly.js.impl.rhino.JsValueAdapter;
import org.apache.sling.scripting.sightly.render.RenderContext;
import org.apache.sling.scripting.sightly.use.ProviderOutcome;
import org.apache.sling.scripting.sightly.use.UseProvider;
import org.osgi.framework.Constants;

/**
 * Use provider for JavaScript Use-API objects.
 */
@Component(
    metatype = true,
    label = "Apache Sling Scripting HTL JavaScript Use Provider",
    description = "The JavaScript Use Provider is responsible for instantiating JavaScript Use-API objects."
)
@Service(UseProvider.class)
@Properties({
                @Property(
                    name = Constants.SERVICE_RANKING,
                    label = "Service Ranking",
                    description = "The Service Ranking value acts as the priority with which this Use Provider is queried to return an " +
                        "Use-object. A higher value represents a higher priority.",
                    intValue = 80,
                    propertyPrivate = false
                )
            })
public class JsUseProvider implements UseProvider {

    private static final String JS_ENGINE_NAME = "javascript";
    private static final JsValueAdapter jsValueAdapter = new JsValueAdapter(new AsyncExtractor());

    @Reference
    private ScriptEngineManager scriptEngineManager = null;

    @Reference
    private ProxyAsyncScriptableFactory proxyAsyncScriptableFactory = null;

    @Override
    public ProviderOutcome provide(String identifier, RenderContext renderContext, Bindings arguments) {
        Bindings globalBindings = renderContext.getBindings();
        if (!Utils.isJsScript(identifier)) {
            return ProviderOutcome.failure();
        }
        ScriptEngine jsEngine = scriptEngineManager.getEngineByName(JS_ENGINE_NAME);
        if (jsEngine == null) {
            return ProviderOutcome.failure(new SightlyException("No JavaScript engine was defined."));
        }
        SlingScriptHelper scriptHelper = Utils.getHelper(globalBindings);
        JsEnvironment environment = null;
        try {
            environment = new JsEnvironment(jsEngine);
            environment.initialize();
            Resource scriptResource = Utils.getScriptResource(scriptHelper.getScript().getScriptResource(), identifier, globalBindings);
            globalBindings.put(ScriptEngine.FILENAME, scriptResource.getPath());
            proxyAsyncScriptableFactory.registerProxies(globalBindings);
            AsyncContainer asyncContainer = environment.runResource(scriptResource, globalBindings, arguments);
            return ProviderOutcome.success(jsValueAdapter.adapt(asyncContainer));
        } finally {
            if (environment != null) {
                environment.cleanup();
            }
        }
    }
}
