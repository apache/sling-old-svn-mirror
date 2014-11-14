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

package org.apache.sling.scripting.sightly.engine.runtime;

import javax.script.Bindings;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.scripting.sightly.api.RenderUnit;
import org.apache.sling.scripting.sightly.api.ResourceResolution;
import org.apache.sling.scripting.sightly.api.UnitLocator;
import org.apache.sling.scripting.sightly.engine.UnitLoader;

/**
 * Implementation for unit locator
 */
public class UnitLocatorImpl implements UnitLocator {

    private final UnitLoader unitLoader;
    private final ResourceResolver resolver;
    private final Bindings bindings;
    private final Resource currentScriptResource;

    public UnitLocatorImpl(UnitLoader unitLoader, ResourceResolver resourceResolver,
                           Bindings bindings, Resource currentScriptResource) {
        this.unitLoader = unitLoader;
        this.resolver = resourceResolver;
        this.bindings = bindings;
        this.currentScriptResource = currentScriptResource;
    }

    @Override
    public RenderUnit locate(String path) {
        Resource resource = locateResource(path);
        if (resource == null) {
            return null;
        }
        return unitLoader.createUnit(resource, bindings);
    }

    private Resource locateResource(String script) {
        SlingHttpServletRequest request = (SlingHttpServletRequest) bindings.get(SlingBindings.REQUEST);
        Resource resource = ResourceResolution.resolveComponentForRequest(resolver, request);
        if (resource != null) {
            resource = ResourceResolution.resolveComponentRelative(resolver, resource, script);
        } else {
            resource = ResourceResolution.resolveComponentRelative(resolver, currentScriptResource, script);
        }
        return resource;
    }

}
