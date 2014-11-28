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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.scripting.sightly.ResourceResolution;
import org.apache.sling.scripting.sightly.impl.compiler.SightlyJavaCompilerService;
import org.apache.sling.scripting.sightly.pojo.Use;
import org.apache.sling.scripting.sightly.render.RenderContext;
import org.apache.sling.scripting.sightly.use.ProviderOutcome;
import org.apache.sling.scripting.sightly.use.UseProvider;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provider which instantiates POJOs.
 */
@Component(
        metatype = true,
        label = "Apache Sling Scripting Sightly POJO Use Provider",
        description = "The POJO Use Provider is responsible for instantiating Use-API objects that optionally can implement the org" +
                ".apache.sling.scripting.sightly.use.Use interface."
)
@Service(UseProvider.class)
@Properties({
        @Property(
                name = Constants.SERVICE_RANKING,
                label = "Service Ranking",
                description = "The Service Ranking value acts as the priority with which this Use Provider is queried to return an " +
                        "Use-object. A higher value represents a higher priority.",
                intValue = 90,
                propertyPrivate = false
        )
})
public class PojoUseProvider implements UseProvider {

    private final Logger LOG = LoggerFactory.getLogger(PojoUseProvider.class);

    @Reference
    private SightlyJavaCompilerService sightlyJavaCompilerService = null;

    @Override
    public ProviderOutcome provide(String identifier, RenderContext renderContext, Bindings arguments) {
        Bindings globalBindings = renderContext.getBindings();
        Bindings bindings = UseProviderUtils.merge(globalBindings, arguments);
        SlingScriptHelper sling = UseProviderUtils.getHelper(bindings);
        ResourceResolverFactory rrf = sling.getService(ResourceResolverFactory.class);
        ResourceResolver adminResolver = null;
        try {
            adminResolver = rrf.getAdministrativeResourceResolver(null);
            Resource resource = ResourceResolution.resolveComponentForRequest(adminResolver, sling.getRequest());
            Object result = sightlyJavaCompilerService.getInstance(resource, identifier);
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
