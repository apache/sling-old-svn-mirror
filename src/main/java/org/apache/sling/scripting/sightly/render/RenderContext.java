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
package org.apache.sling.scripting.sightly.render;

import javax.script.Bindings;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.ServletResolver;
import org.apache.sling.scripting.sightly.extension.RuntimeExtension;
import org.apache.sling.scripting.sightly.impl.engine.runtime.RenderUnit;
import org.apache.sling.scripting.sightly.use.UseProvider;

import aQute.bnd.annotation.ProviderType;

/**
 * The {@code RenderContext} defines the context for executing Sightly scripts (see {@link RenderUnit}).
 */
@ProviderType
public interface RenderContext {

    /**
     * Returns the map of script bindings available to Sightly scripts.
     *
     * @return the global bindings for a script
     */
    Bindings getBindings();

    /**
     * Call one of the registered {@link RuntimeExtension}s.
     *
     * @param functionName the name under which the extension is registered
     * @param arguments    the extension's arguments
     * @return the {@link RuntimeExtension}'s result
     */
    Object call(String functionName, Object... arguments);

    /**
     * Returns the {@link ResourceResolver} that was used for resolving the
     * currently evaluated script. This resolver is the same resolver used by
     * the {@link ServletResolver} and thus should be used <i>only</i> for
     * script resolution.
     * <p>
     * This {@code ResourceResolver} must not be closed.
     * <p>
     * If a {@link RuntimeExtension} or a {@link UseProvider} need to resolve
     * content, then the {@link ResourceResolver} of the current request should
     * be used. This can be retrieved by using the following call:
     *
     * <pre>
     * ResourceResolver resolver = (ResourceResolver) renderContext.getBindings().get(SlingBindings.REQUEST).getResourceResolver();
     * </pre>
     *
     * @return the resource resolver used to resolve the currently evaluated
     *         script
     */
    ResourceResolver getScriptResourceResolver();

}
