/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.apache.sling.scripting.sightly.impl.engine.extension.use;

import javax.script.Bindings;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.scripting.sightly.ResourceResolution;
import org.apache.sling.scripting.sightly.impl.utils.BindingsUtils;
import org.apache.sling.scripting.sightly.render.RenderContext;
import org.apache.sling.scripting.sightly.use.UseProvider;

/**
 * Helper class for {@link UseProvider} implementations.
 */
public class UseProviderUtils {

    /**
     * Locates a script resource identified by the {@code script} path. This path can be absolute or relative. If the path is relative then
     * it will be used to locate a script for the current request, provided by the {@link SlingScriptHelper} parameter.
     *
     * @param renderContext the render context
     * @param script        the path to the script
     * @return the script resource if found, {@code null} otherwise
     */
    public static Resource locateScriptResource(RenderContext renderContext, String script) {
        Bindings bindings = renderContext.getBindings();
        SlingScriptHelper sling = BindingsUtils.getHelper(bindings);
        SlingHttpServletRequest request = BindingsUtils.getRequest(bindings);
        Resource caller = ResourceResolution.getResourceForRequest(renderContext.getScriptResourceResolver(), request);
        Resource result = ResourceResolution.getResourceFromSearchPath(caller, script);
        if (result == null) {
            caller = sling.getScript().getScriptResource();
            result = ResourceResolution.getResourceFromSearchPath(caller, script);
        }
        return result;
    }

}
