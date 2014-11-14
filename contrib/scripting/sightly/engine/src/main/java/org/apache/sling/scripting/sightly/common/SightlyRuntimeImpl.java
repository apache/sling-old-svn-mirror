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

package org.apache.sling.scripting.sightly.common;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.scripting.sightly.api.ExtensionInstance;
import org.apache.sling.scripting.sightly.api.RuntimeExtension;
import org.apache.sling.scripting.sightly.api.RenderContext;
import org.apache.sling.scripting.sightly.api.SightlyRenderException;
import org.apache.sling.scripting.sightly.api.SightlyRuntime;
import org.apache.sling.scripting.sightly.api.ExtensionInstance;

/**
 * Implementation for apache runtime
 */
public class SightlyRuntimeImpl implements SightlyRuntime {

    private final Map<String, RuntimeExtension> mapping;
    private final Map<String, ExtensionInstance> instanceCache = new HashMap<String, ExtensionInstance>();
    private RenderContext renderContext;

    public SightlyRuntimeImpl(Map<String, RuntimeExtension> mapping) {
        this.mapping = mapping;
    }

    @Override
    public Object call(String functionName, Object... arguments) {
        ExtensionInstance instance;
        instance = instanceCache.get(functionName);
        if (instance == null) {
            instance = createInstance(functionName);
            instanceCache.put(functionName, instance);
        }
        return instance.call(arguments);
    }

    public RenderContext getRenderContext() {
        return renderContext;
    }

    public void setRenderContext(RenderContext renderContext) {
        this.renderContext = renderContext;
    }

    private ExtensionInstance createInstance(String name) {
        RuntimeExtension extension = mapping.get(name);
        if (extension == null) {
            throw new SightlyRenderException("Runtime extension is not available: " + name);
        }
        return extension.provide(renderContext);
    }
}
