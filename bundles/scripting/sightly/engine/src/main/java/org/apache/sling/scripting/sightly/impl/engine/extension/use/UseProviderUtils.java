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
import javax.script.SimpleBindings;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.scripting.sightly.ResourceResolution;
import org.apache.sling.scripting.sightly.SightlyException;
import org.apache.sling.scripting.sightly.use.UseProvider;

/**
 * Helper class for {@link UseProvider} implementations.
 */
public class UseProviderUtils {

    /**
     * Combine two bindings objects. Priority goes to latter bindings.
     *
     * @param former first map of bindings
     * @param latter second map of bindings, which can override the fist one
     * @return the merging of the two maps
     */
    public static Bindings merge(Bindings former, Bindings latter) {
        Bindings bindings = new SimpleBindings();
        bindings.putAll(former);
        bindings.putAll(latter);
        return bindings;
    }

    /**
     * Retrieves the {@link SlingScriptHelper} from a {@link Bindings} map.
     *
     * @param bindings the bindings map
     * @return the {@link SlingScriptHelper} if found, {@code null} otherwise
     */
    public static SlingScriptHelper getHelper(Bindings bindings) {
        return (SlingScriptHelper) bindings.get(SlingBindings.SLING);
    }

    /**
     * Locates a script resource identified by the {@code script} path. This path can be absolute or relative. If the path is relative
     * then it will be used to locate a script for the current request, provided by the {@link SlingScriptHelper} parameter.
     *
     * @param resourceResolver a resource resolver used for searching the script
     * @param sling            the {@link SlingScriptHelper}
     * @param script           the path to the script
     * @return the script resource if found, {@code null} otherwise
     */
    public static Resource locateScriptResource(ResourceResolver resourceResolver, SlingScriptHelper sling, String script) {
        Resource componentResource = ResourceResolution.getResourceForRequest(resourceResolver, sling.getRequest());
        Resource result = ResourceResolution.getResourceFromSearchPath(componentResource, script);
        return result;
    }

}
