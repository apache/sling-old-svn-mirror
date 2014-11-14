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

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScript;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.scripting.sightly.api.ProviderOutcome;
import org.apache.sling.scripting.sightly.api.UseProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.sling.scripting.sightly.api.ProviderOutcome;
import org.apache.sling.scripting.sightly.api.RenderContext;
import org.apache.sling.scripting.sightly.api.SightlyUseException;
import org.apache.sling.scripting.sightly.api.UseProvider;
import org.apache.sling.scripting.sightly.api.UseProviderComponent;

/**
 * Use provider that interprets the identifier as a script path, and runs the respective script using a script engine that matches the
 * script extension.
 *
 * This provider returns a non-failure outcome only if the evaluated script actually returns something. For more details check the
 * implementation of the {@link SlingScript#eval(SlingBindings)} method for the available script engines from your platform.
 */
@Component
@Service(UseProvider.class)
@Property(name = UseProviderComponent.PRIORITY, intValue = 15)
public class ScriptUseProvider extends UseProviderComponent {

    private static final Logger log = LoggerFactory.getLogger(ScriptUseProvider.class);

    @Reference
    private ResourceResolverFactory rrf = null;

    @Override
    public ProviderOutcome provide(String scriptName, RenderContext renderContext, Bindings arguments) {
        Bindings globalBindings = renderContext.getBindings();
        Bindings bindings = merge(globalBindings, arguments);
        String extension = scriptExtension(scriptName);
        if (extension == null) {
            return ProviderOutcome.failure();
        }
        SlingScriptHelper sling = (SlingScriptHelper) bindings.get(SlingBindings.SLING);
        ResourceResolver adminResolver = null;
        try {
            adminResolver = rrf.getAdministrativeResourceResolver(null);
            if (adminResolver == null) {
                log.warn("Cannot obtain administrative resource resolver for " + scriptName);
                return ProviderOutcome.failure();
            }
            Resource scriptResource = ScriptEvalUtils.locateScriptResource(adminResolver, sling, scriptName);
            if (scriptResource == null) {
                log.debug("Path does not match an existing resource: {}", scriptName);
                return ProviderOutcome.failure();
            }
            return evalScript(scriptResource, bindings);
        } catch (LoginException e) {
            throw new SightlyUseException(e);
        } finally {
            if (adminResolver != null) {
                adminResolver.close();
            }
        }

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
