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

package org.apache.sling.scripting.sightly.impl.engine.extension.use;

import java.io.InputStream;
import java.io.InputStreamReader;
import javax.script.Bindings;
import javax.script.CompiledScript;
import javax.script.ScriptEngineManager;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.scripting.api.CachedScript;
import org.apache.sling.scripting.api.ScriptCache;
import org.apache.sling.scripting.api.resource.ScriptingResourceResolverProvider;
import org.apache.sling.scripting.core.ScriptNameAwareReader;
import org.apache.sling.scripting.sightly.ResourceResolution;
import org.apache.sling.scripting.sightly.SightlyException;
import org.apache.sling.scripting.sightly.impl.engine.SightlyCompiledScript;
import org.apache.sling.scripting.sightly.impl.engine.SightlyScriptEngine;
import org.apache.sling.scripting.sightly.impl.engine.SightlyScriptEngineFactory;
import org.apache.sling.scripting.sightly.impl.utils.BindingsUtils;
import org.apache.sling.scripting.sightly.impl.utils.ScriptUtils;
import org.apache.sling.scripting.sightly.java.compiler.RenderUnit;
import org.apache.sling.scripting.sightly.render.RenderContext;
import org.apache.sling.scripting.sightly.use.ProviderOutcome;
import org.apache.sling.scripting.sightly.use.UseProvider;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;

/**
 * Interprets identifiers as paths to other HTL templates
 */
@Component(
        service = UseProvider.class,
        configurationPid = "org.apache.sling.scripting.sightly.impl.engine.extension.use.RenderUnitProvider",
        property = {
                Constants.SERVICE_RANKING + ":Integer=100"
        }
)
public class RenderUnitProvider implements UseProvider {

    @interface Configuration {

        @AttributeDefinition(
                name = "Service Ranking",
                description = "The Service Ranking value acts as the priority with which this Use Provider is queried to return an " +
                        "Use-object. A higher value represents a higher priority."
        )
        int service_ranking() default 100;
    }

    @Reference
    private ScriptCache scriptCache;

    @Reference
    private ScriptEngineManager scriptEngineManager;

    @Reference
    private ScriptingResourceResolverProvider scriptingResourceResolverProvider;

    @Override
    public ProviderOutcome provide(String identifier, RenderContext renderContext, Bindings arguments) {
        if (identifier.endsWith("." + SightlyScriptEngineFactory.EXTENSION)) {
            Bindings globalBindings = renderContext.getBindings();
            SlingScriptHelper sling = BindingsUtils.getHelper(globalBindings);
            SlingHttpServletRequest request = BindingsUtils.getRequest(globalBindings);
            final Resource renderUnitResource = ScriptUtils.resolveScript(scriptingResourceResolverProvider
                    .getRequestScopedResourceResolver(), renderContext, identifier);
            if (renderUnitResource == null) {
                Resource caller = ResourceResolution.getResourceForRequest(request.getResourceResolver(), request);
                if (caller != null) {
                    String resourceSuperType = caller.getResourceSuperType();
                    StringBuilder errorMessage = new StringBuilder("Cannot find resource ");
                    errorMessage.append(identifier).append(" for base path ").append(caller.getPath());
                    if (StringUtils.isNotEmpty(resourceSuperType)) {
                        errorMessage.append(" with resource super type ").append(resourceSuperType);
                    }
                    errorMessage.append(".");
                    return ProviderOutcome.failure(new SightlyException(errorMessage.toString()));
                } else {
                    return ProviderOutcome.failure(new SightlyException("Cannot resolve template " + identifier + " for script " + sling
                            .getScript().getScriptResource().getPath()));
                }
            }
            RenderUnit renderUnit;
            try {
                CachedScript cachedScript = scriptCache.getScript(renderUnitResource.getPath());
                final SightlyCompiledScript compiledScript;
                if (cachedScript != null) {
                    compiledScript = (SightlyCompiledScript) cachedScript.getCompiledScript();
                } else {
                    SightlyScriptEngine sightlyScriptEngine =
                            (SightlyScriptEngine) scriptEngineManager.getEngineByName(SightlyScriptEngineFactory.SHORT_NAME);
                    String encoding = renderUnitResource.getResourceMetadata().getCharacterEncoding();
                    if (StringUtils.isEmpty(encoding)) {
                        encoding = "UTF-8";
                    }
                    InputStream inputStream = renderUnitResource.adaptTo(InputStream.class);
                    if (inputStream == null) {
                        return ProviderOutcome.failure();
                    }
                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream, encoding);
                    ScriptNameAwareReader reader = new ScriptNameAwareReader(inputStreamReader, renderUnitResource.getPath());
                    compiledScript = (SightlyCompiledScript) sightlyScriptEngine.compile(reader);
                    scriptCache.putScript(new CachedScript() {
                        @Override
                        public String getScriptPath() {
                            return renderUnitResource.getPath();
                        }

                        @Override
                        public CompiledScript getCompiledScript() {
                            return compiledScript;
                        }
                    });
                }
                renderUnit = compiledScript.getRenderUnit();
                return ProviderOutcome.success(renderUnit);
            } catch (Exception e) {
                return ProviderOutcome.failure(e);
            }
        }
        return ProviderOutcome.failure();
    }
}
