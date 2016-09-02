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

import javax.script.Bindings;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScript;
import org.apache.sling.scripting.sightly.impl.engine.SightlyScriptEngineFactory;
import org.apache.sling.scripting.sightly.impl.engine.runtime.RenderContextImpl;
import org.apache.sling.scripting.sightly.impl.utils.BindingsUtils;
import org.apache.sling.scripting.sightly.render.RenderContext;
import org.apache.sling.scripting.sightly.use.ProviderOutcome;
import org.apache.sling.scripting.sightly.use.UseProvider;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use provider that interprets the identifier as a script path, and runs the respective script using a script engine that matches the
 * script extension.
 *
 * This provider returns a non-failure outcome only if the evaluated script actually returns something. For more details check the
 * implementation of the {@link SlingScript#eval(SlingBindings)} method for the available script engines from your platform.
 */
@Component(
        metatype = true,
        label = "Apache Sling Scripting HTL Script Use Provider",
        description = "The Script Use Provider is responsible for instantiating objects from scripts evaluated by other Sling Scripting " +
                "Engines."
)
@Service(UseProvider.class)
@Properties({
        @Property(
                name = Constants.SERVICE_RANKING,
                label = "Service Ranking",
                description = "The Service Ranking value acts as the priority with which this Use Provider is queried to return an " +
                        "Use-object. A higher value represents a higher priority.",
                intValue = 0,
                propertyPrivate = false
        )
})
public class ScriptUseProvider implements UseProvider {

    private static final Logger log = LoggerFactory.getLogger(ScriptUseProvider.class);

    @Override
    public ProviderOutcome provide(String scriptName, RenderContext renderContext, Bindings arguments) {
        RenderContextImpl rci = (RenderContextImpl) renderContext;
        Bindings globalBindings = rci.getBindings();
        Bindings bindings = BindingsUtils.merge(globalBindings, arguments);
        String extension = scriptExtension(scriptName);
        if (extension == null || extension.equals(SightlyScriptEngineFactory.EXTENSION)) {
            return ProviderOutcome.failure();
        }
        Resource scriptResource = rci.resolveScript(scriptName);
        if (scriptResource == null) {
            log.debug("Path does not match an existing resource: {}", scriptName);
            return ProviderOutcome.failure();
        }
        return evalScript(scriptResource, bindings);
    }

    private ProviderOutcome evalScript(Resource scriptResource, Bindings bindings) {
        SlingScript slingScript = scriptResource.adaptTo(SlingScript.class);
        if (slingScript == null) {
            return ProviderOutcome.failure();
        }
        SlingBindings slingBindings = new SlingBindings();
        slingBindings.putAll(bindings);
        Object scriptEval = slingScript.eval(slingBindings);
        return ProviderOutcome.notNullOrFailure(scriptEval);
    }

    private String scriptExtension(String path) {
        String extension = StringUtils.substringAfterLast(path, ".");
        if (StringUtils.isEmpty(extension)) {
            extension = null;
        }
        return extension;
    }

}
