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
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.scripting.sightly.SightlyException;
import org.apache.sling.scripting.sightly.impl.utils.BindingsUtils;
import org.apache.sling.scripting.sightly.render.RenderContext;
import org.apache.sling.scripting.sightly.use.ProviderOutcome;
import org.apache.sling.scripting.sightly.use.UseProvider;
import org.osgi.framework.Constants;

@Component(
        metatype = true,
        label = "Apache Sling Scripting Sightly Resource Use Provider",
        description = "The Java Use Provider is responsible for instantiating resource objects."
)
@Service(UseProvider.class)
@Properties({
        @Property(
                name = Constants.SERVICE_RANKING,
                label = "Service Ranking",
                description = "The Service Ranking value acts as the priority with which this Use Provider is queried to return an " +
                        "Use-object. A higher value represents a higher priority.",
                intValue = -10,
                propertyPrivate = false
        )
})
public class ResourceUseProvider implements UseProvider {

    @Override
    public ProviderOutcome provide(String identifier, RenderContext renderContext, Bindings arguments) {
        Bindings globalBindings = renderContext.getBindings();
        SlingHttpServletRequest request = BindingsUtils.getRequest(globalBindings);
        String path = normalizePath(request, identifier);
        try {
            Resource resource = request.getResourceResolver().getResource(path);
            if (resource != null && !ResourceUtil.isNonExistingResource(resource)) {
                return ProviderOutcome.success(resource);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ProviderOutcome.failure(new SightlyException(e.getMessage().toString()));
        }
        return ProviderOutcome.failure();
    }

    private String normalizePath(SlingHttpServletRequest request, String path) {
        if (!path.startsWith("/")) {
            path = request.getResource().getPath() + "/" + path;
        }
        return ResourceUtil.normalize(path);
    }
}
