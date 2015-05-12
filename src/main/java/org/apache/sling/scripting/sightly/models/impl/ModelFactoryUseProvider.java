/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.scripting.sightly.models.impl;


import java.util.HashMap;
import java.util.Map;

import javax.script.Bindings;
import javax.script.SimpleBindings;
import javax.servlet.ServletRequest;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.models.factory.ModelFactory;
import org.apache.sling.scripting.sightly.render.RenderContext;
import org.apache.sling.scripting.sightly.use.ProviderOutcome;
import org.apache.sling.scripting.sightly.use.UseProvider;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sightly {@link UseProvider} which will instantiate a referenced Sling Model.
 * For that it tries to use the {@link ModelFactory#createModel(Object, Class)} first with the adaptable {@link Resource} 
 * then with the adaptable {@link SlingHttpServletRequest}.
 * It will always fail with an exception (i.e. no other {@code UseProvider} is asked afterwards and the exception is being rethrown)
 * in case the following two preconditions are fulfilled:
 * <ol>
 * <li>the given identifier specifies a class which can be loaded by the DynamicClassLoader</li>
 * <li>the loaded class has a Model annotation</li>
 * </ol>
 * In case any of those preconditions are not fulfilled the other registered UseProviders are used!
 */
@Component
@Service
/*
 * must have a higher priority than org.apache.sling.scripting.sightly.impl.engine.extension.use.JavaUseProvider but lower than 
 * org.apache.sling.scripting.sightly.impl.engine.extension.use.RenderUnitProvider to kick in 
 * before the JavaUseProvider but after the RenderUnitProvider
 */
@Property(name = Constants.SERVICE_RANKING, intValue = { 95 }) 
public class ModelFactoryUseProvider implements UseProvider {

    private static final Logger log = LoggerFactory.getLogger(ModelFactoryUseProvider.class);
    
    @Reference
    ModelFactory modelFactory;
    
    @Reference
    private DynamicClassLoaderManager dynamicClassLoaderManager = null;
    
    @Override
    public ProviderOutcome provide(final String identifier, final RenderContext renderContext, final Bindings arguments) {
        final Class<?> cls;
        try {
            cls = dynamicClassLoaderManager.getDynamicClassLoader().loadClass(identifier);
        } catch(ClassNotFoundException e) {
            log.debug("Could not find class with the given name {}: {}", identifier, e.getMessage());
            // next use provider will be queried
            return ProviderOutcome.failure();
        }
        Bindings globalBindings = renderContext.getBindings();
        Bindings bindings = merge(globalBindings, arguments);
        Resource resource = (Resource) bindings.get(SlingBindings.RESOURCE);
        if (resource == null) {
            return ProviderOutcome.failure(new IllegalStateException("Could not get resource from bindings"));
        }
        SlingHttpServletRequest request = (SlingHttpServletRequest) bindings.get(SlingBindings.REQUEST);
        if (request == null) {
            return ProviderOutcome.failure(new IllegalStateException("Could not get request from bindings"));
        }

        // pass parameters as request attributes
        Map<String, Object> overrides = setRequestAttributes(request, arguments);
        Object obj = null;
        try {
            if (!modelFactory.isModelClass(resource, cls)) {
                log.debug("{} is no Sling Model (because it lacks the according Model annotation)!");
                // next use provider will be queried
                return ProviderOutcome.failure();
            }
            // try to instantiate class via Sling Models (first via resource, then via request)
            if (modelFactory.canCreateFromAdaptable(resource, cls)) {
                obj = modelFactory.createModel(resource, cls);
            } else if (modelFactory.canCreateFromAdaptable(request, cls)) {
                obj = modelFactory.createModel(request, cls);
            } else {
                return ProviderOutcome.failure(new IllegalStateException("Could not adapt the given Sling Model from neither resource nor request: " + cls));
            }
        } catch (Throwable e) {
            return ProviderOutcome.failure(e);
        } finally {
            resetRequestAttribute(request, overrides);
        }
        return ProviderOutcome.notNullOrFailure(obj);
    }

    private Map<String, Object> setRequestAttributes(final ServletRequest request,
            final Bindings arguments) {
        Map<String, Object> overrides = new HashMap<String, Object>();
        for (Map.Entry<String, Object> entry : arguments.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            Object oldValue = request.getAttribute(key);
            if (oldValue != null) {
                overrides.put(key, oldValue);
            }
            else {
                overrides.put(key, null);
            }
            request.setAttribute(key, value);
        }
        return overrides;
    }

    private void resetRequestAttribute(final ServletRequest request,
            final Map<String, Object> overrides) {
        for (Map.Entry<String, Object> entry : overrides.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value == null) {
                request.removeAttribute(key);
            }
            else {
                request.setAttribute(key, value);
            }
        }
    }

    private SimpleBindings merge(Bindings former, Bindings latter) {
        SimpleBindings bindings = new SimpleBindings();
        bindings.putAll(former);
        bindings.putAll(latter);
        return bindings;
    }
}
