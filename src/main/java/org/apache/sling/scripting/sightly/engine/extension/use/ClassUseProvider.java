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

import java.util.HashMap;
import java.util.Map;

import javax.script.Bindings;
import javax.servlet.ServletRequest;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;

import org.apache.sling.scripting.sightly.api.ProviderOutcome;
import org.apache.sling.scripting.sightly.api.RenderContext;
import org.apache.sling.scripting.sightly.api.UseProvider;
import org.apache.sling.scripting.sightly.api.UseProviderComponent;
import org.apache.sling.scripting.sightly.api.ProviderOutcome;
import org.apache.sling.scripting.sightly.api.UseProvider;

/**
 * Interprets the identifier as a class name and tries to load that class
 * using a dynamic class loader
 */
@Component
@Service(UseProvider.class)
@Property(name = UseProviderComponent.PRIORITY, intValue = 0)
public class ClassUseProvider extends UseProviderComponent {

    @Override
    public ProviderOutcome provide(String identifier, RenderContext renderContext, Bindings arguments) {
        Bindings globalBindings = renderContext.getBindings();
        Bindings bindings = merge(globalBindings, arguments);
        SlingScriptHelper sling = (SlingScriptHelper) bindings.get(SlingBindings.SLING);
        Resource resource = (Resource) bindings.get(SlingBindings.RESOURCE);
        final SlingHttpServletRequest request = (SlingHttpServletRequest) bindings.get(SlingBindings.REQUEST);
        Map<String, Object> overrides = setRequestAttributes(request, arguments);
        
        DynamicClassLoaderManager classLoaderManager = sling.getService(DynamicClassLoaderManager.class);
        Object obj;
        try {
            Class<?> cls = classLoaderManager.getDynamicClassLoader().loadClass(identifier);
            obj = resource.adaptTo(cls);
            if (obj == null) {
                obj = request.adaptTo(cls);
            }
        } catch (ClassNotFoundException e) {
            obj = null;
        } finally {
            resetRequestAttribute(request, overrides);
        }
        return ProviderOutcome.notNullOrFailure(obj);
    }

    private Map<String, Object> setRequestAttributes(ServletRequest request, Bindings arguments) {
        Map<String, Object> overrides = new HashMap<String, Object>();
        for (Map.Entry<String, Object> entry : arguments.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            Object oldValue = request.getAttribute(key);
            if (oldValue != null) {
                overrides.put(key, oldValue);
            } else {
                overrides.put(key, NULL);
            }
            request.setAttribute(key, value);
        }
        return overrides;
    }

    private void resetRequestAttribute(ServletRequest request, Map<String, Object> overrides) {
        for (Map.Entry<String, Object> entry : overrides.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value == NULL) {
                request.removeAttribute(key);
            } else {
                request.setAttribute(key, value);
            }
        }
    }

    private static final Object NULL = new Object();

}
