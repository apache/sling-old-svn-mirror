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
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.scripting.SlingScriptHelper;

import org.apache.sling.scripting.sightly.api.ProviderOutcome;
import org.apache.sling.scripting.sightly.api.RenderContext;
import org.apache.sling.scripting.sightly.api.ResourceResolution;
import org.apache.sling.scripting.sightly.api.Use;
import org.apache.sling.scripting.sightly.api.UseProvider;
import org.apache.sling.scripting.sightly.api.UseProviderComponent;
import org.apache.sling.scripting.sightly.compiler.SightlyCompileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provider which instantiates POJOs.
 */
@Component
@Service(UseProvider.class)
@Property(name = UseProviderComponent.PRIORITY, intValue = 10)
public class PojoUseProvider extends UseProviderComponent {

    private final Logger LOG = LoggerFactory.getLogger(PojoUseProvider.class);

    @Reference
    private SightlyCompileService sightlyCompileService = null;

    @Override
    public ProviderOutcome provide(String identifier, RenderContext renderContext, Bindings arguments) {
        Bindings globalBindings = renderContext.getBindings();
        Bindings bindings = merge(globalBindings, arguments);
        SlingScriptHelper sling = ScriptEvalUtils.getHelper(bindings);
        ResourceResolverFactory rrf = sling.getService(ResourceResolverFactory.class);
        ResourceResolver adminResolver = null;
        try {
            adminResolver = rrf.getAdministrativeResourceResolver(null);
            Resource resource = ResourceResolution.resolveComponentForRequest(adminResolver, sling.getRequest());
            Object result = sightlyCompileService.getInstance(resource, identifier, false);
            if (result instanceof Use) {
                ((Use) result).init(bindings);
            }
            return ProviderOutcome.notNullOrFailure(result);
        } catch (Exception e) {
            LOG.error(String.format("Can't instantiate %s POJO.", identifier), e);
            return ProviderOutcome.failure();
        } finally {
            if (adminResolver != null) {
                adminResolver.close();
            }
        }
    }
}
