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

import java.util.HashMap;
import java.util.Map;
import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
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
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use provider for JavaScript Use-API objects.
 */
@Component(
        service = UseProvider.class,
        configurationPid = "org.apache.sling.scripting.sightly.js.impl.JsUseProvider",
        property = {
                Constants.SERVICE_RANKING + ":Integer=80"
        }
)
@Designate(
        ocd = JsUseProvider.Configuration.class
)
public class JsUseProvider implements UseProvider {

    @ObjectClassDefinition(
            name = "Apache Sling Scripting HTL JavaScript Use Provider Configuration",
            description = "HTL JavaScript Use Provider configuration options"
    )
    @interface Configuration {

        @AttributeDefinition(
                name = "Service Ranking",
                description = "The Service Ranking value acts as the priority with which this Use Provider is queried to return an " +
                        "Use-object. A higher value represents a higher priority."
        )
        int service_ranking() default 80;

    }

    private static final Logger LOGGER = LoggerFactory.getLogger(JsUseProvider.class);
    private static final String JS_ENGINE_NAME = "javascript";
    private static final JsValueAdapter jsValueAdapter = new JsValueAdapter(new AsyncExtractor());
    private static final String SLING_SCRIPTING_USER = "sling-scripting";

    @Reference
    private ScriptEngineManager scriptEngineManager = null;

    @Reference
    private ProxyAsyncScriptableFactory proxyAsyncScriptableFactory = null;

    @Reference
    private ResourceResolverFactory rrf = null;

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
        ResourceResolver slingScriptingResolver = null;
        try {
            environment = new JsEnvironment(jsEngine);
            environment.initialize();
            Map<String, Object> authenticationInfo = new HashMap<String, Object>() {{
                put(ResourceResolverFactory.SUBSERVICE, SLING_SCRIPTING_USER);
            }};
            slingScriptingResolver = rrf.getServiceResourceResolver(authenticationInfo);
            Resource callerScript = slingScriptingResolver.getResource(scriptHelper.getScript().getScriptResource().getPath());
            Resource scriptResource = Utils.getScriptResource(callerScript, identifier, globalBindings);
            globalBindings.put(ScriptEngine.FILENAME, scriptResource.getPath());
            proxyAsyncScriptableFactory.registerProxies(globalBindings);
            AsyncContainer asyncContainer = environment.runResource(scriptResource, globalBindings, arguments);
            return ProviderOutcome.success(jsValueAdapter.adapt(asyncContainer));
        } catch (LoginException e) {
            LOGGER.error("Cannot obtain a resource resolver backed by the " + SLING_SCRIPTING_USER + " service user.", e);
            return ProviderOutcome.failure(e);
        } finally {
            if (environment != null) {
                environment.cleanup();
            }
            if (slingScriptingResolver != null) {
                slingScriptingResolver.close();
            }
        }
    }
}
