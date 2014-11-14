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

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;

import org.apache.sling.scripting.sightly.api.ResourceResolution;
import org.apache.sling.scripting.sightly.api.SightlyUseException;

/**
 * Utilities for script evaluation
 */
public class ScriptEvalUtils {

    public static SlingScriptHelper getHelper(Bindings bindings) {
        return (SlingScriptHelper) bindings.get(SlingBindings.SLING);
    }

    public static Resource locateScriptResource(ResourceResolver resourceResolver, SlingScriptHelper sling, String script) {
        Resource result = null;
        if (script.startsWith("/")) {
            result = resourceResolver.getResource(script);
        }
        if (result == null) {
            Resource componentResource = ResourceResolution.resolveComponentForRequest(resourceResolver, sling.getRequest());
            result = ResourceResolution.resolveComponentRelative(resourceResolver, componentResource, script);
        }
        if (result != null) {
            checkSearchPath(result, resourceResolver);
        }
        return result;
    }

    private static void checkSearchPath(Resource resource, ResourceResolver resourceResolver) {
        String resourcePath = resource.getPath();
        for (String path : resourceResolver.getSearchPath()) {
            if (resourcePath.startsWith(path)) {
                return;
            }
        }
        throw new SightlyUseException("Use plugin cannot access path: " + resource.getPath());
    }
}
