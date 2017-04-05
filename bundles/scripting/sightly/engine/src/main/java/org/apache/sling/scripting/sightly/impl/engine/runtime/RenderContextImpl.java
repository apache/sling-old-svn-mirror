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
package org.apache.sling.scripting.sightly.impl.engine.runtime;

import java.util.Map;
import javax.script.Bindings;
import javax.script.ScriptContext;

import org.apache.sling.scripting.sightly.SightlyException;
import org.apache.sling.scripting.sightly.extension.RuntimeExtension;
import org.apache.sling.scripting.sightly.impl.engine.ExtensionRegistryService;
import org.apache.sling.scripting.sightly.impl.utils.BindingsUtils;
import org.apache.sling.scripting.sightly.render.AbstractRuntimeObjectModel;
import org.apache.sling.scripting.sightly.render.RenderContext;
import org.apache.sling.scripting.sightly.render.RuntimeObjectModel;

/**
 * Rendering context for HTL rendering units.
 */
public class RenderContextImpl implements RenderContext {

    private static final AbstractRuntimeObjectModel OBJECT_MODEL = new SlingRuntimeObjectModel();

    private final Bindings bindings;
    private final ExtensionRegistryService extensionRegistryService;

    public RenderContextImpl(ScriptContext scriptContext) {
        bindings = scriptContext.getBindings(ScriptContext.ENGINE_SCOPE);
        extensionRegistryService = BindingsUtils.getHelper(bindings).getService(ExtensionRegistryService.class);
    }

    @Override
    public RuntimeObjectModel getObjectModel() {
        return OBJECT_MODEL;
    }

    /**
     * Provide the bindings for this script
     * @return - the list of global bindings available to the script
     */
    @Override
    public Bindings getBindings() {
        return bindings;
    }

    @Override
    public Object call(String functionName, Object... arguments) {
        Map<String, RuntimeExtension> extensions = extensionRegistryService.extensions();
        RuntimeExtension extension = extensions.get(functionName);
        if (extension == null) {
            throw new SightlyException("Runtime extension is not available: " + functionName);
        }
        return extension.call(this, arguments);
    }

}
