/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.apache.sling.scripting.sightly.models.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import javax.script.Bindings;
import javax.servlet.ServletRequest;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.models.factory.ModelFactory;
import org.apache.sling.scripting.sightly.render.RenderContext;
import org.apache.sling.scripting.sightly.use.ProviderOutcome;
import org.apache.sling.scripting.sightly.use.UseProvider;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * HTL {@link UseProvider} which will instantiate a referenced Sling Model.
 * </p>
 * <p>
 * For that it tries to use the {@link ModelFactory#createModel(Object, Class)} first with the adaptable {@link SlingHttpServletRequest}
 * then with the adaptable {@link Resource}. It will always fail with an exception (i.e. no other {@code UseProvider} is
 * asked afterwards and the exception is being rethrown) in case the following two preconditions are fulfilled:
 * </p>
 * <ol>
 * <li>the given identifier specifies a class which can be loaded by a {@link org.apache.sling.commons.classloader.DynamicClassLoader}</li>
 * <li>the loaded class has a {@link org.apache.sling.models.annotations.Model} annotation</li>
 * </ol>
 *
 * <p>
 * In case any of those preconditions are not fulfilled the other registered {@link UseProvider}s will be queried.
 * </p>
 */
@Component(
        service = UseProvider.class,
        configurationPid = "org.apache.sling.scripting.sightly.models.impl.SlingModelsUseProvider",
        property = {
                /**
                 * Must have a higher priority than {@link org.apache.sling.scripting.sightly.impl.engine.extension.use.JavaUseProvider} but lower
                 * than {@link org.apache.sling.scripting.sightly.impl.engine.extension.use.RenderUnitProvider} to kick in before the
                 * JavaUseProvider but after the RenderUnitProvider.
                 */
                Constants.SERVICE_RANKING + ":Integer=95"
        }
)
public class SlingModelsUseProvider implements UseProvider {

    @interface Configuration {

        @AttributeDefinition(
                name = "Service Ranking",
                description = "The Service Ranking value acts as the priority with which this Use Provider is queried to return an " +
                        "Use-object. A higher value represents a higher priority."
        )
        int service_ranking() default 95;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(SlingModelsUseProvider.class);
    private static final Pattern JAVA_PATTERN = Pattern.compile(
        "([[\\p{L}&&[^\\p{Lu}]]_$][\\p{L}\\p{N}_$]*\\.)*[\\p{Lu}_$][\\p{L}\\p{N}_$]*");


    @Reference
    private ModelFactory modelFactory = null;

    @Reference
    private DynamicClassLoaderManager dynamicClassLoaderManager = null;

    @Override
    public ProviderOutcome provide(final String identifier, final RenderContext renderContext, final Bindings arguments) {
        if (!JAVA_PATTERN.matcher(identifier).matches()) {
            LOGGER.debug("Identifier {} does not match a Java class name pattern.", identifier);
            return ProviderOutcome.failure();
        }
        final Class<?> cls;
        try {
            cls = dynamicClassLoaderManager.getDynamicClassLoader().loadClass(identifier);
        } catch (ClassNotFoundException e) {
            LOGGER.debug("Could not find class with the given name {}.", identifier);
            // next use provider will be queried
            return ProviderOutcome.failure();
        }
        if (!modelFactory.isModelClass(cls)) {
            LOGGER.debug("{} is not a Sling Model.");
            // next use provider will be queried
            return ProviderOutcome.failure();
        }
        Bindings globalBindings = renderContext.getBindings();
        SlingHttpServletRequest request = (SlingHttpServletRequest) globalBindings.get(SlingBindings.REQUEST);
        if (request == null) {
            return ProviderOutcome.failure(new IllegalStateException("Could not get request from bindings."));
        }
        // pass parameters as request attributes
        Map<String, Object> overrides = setRequestAttributes(request, arguments);

        try {
            // try to instantiate class via Sling Models (first via request, then via resource)
            if (modelFactory.canCreateFromAdaptable(request, cls)) {
                LOGGER.debug("Trying to instantiate class {} as Sling Model from request.", cls);
                return ProviderOutcome.notNullOrFailure(modelFactory.createModel(request, cls));
            }
            Resource resource = (Resource) globalBindings.get(SlingBindings.RESOURCE);
            if (resource == null) {
                LOGGER.debug("Could not get resource from bindings.");
                return ProviderOutcome.failure(new IllegalStateException("Could not get resource from bindings."));
            }
            if (modelFactory.canCreateFromAdaptable(resource, cls)) {
                LOGGER.debug("Trying to instantiate class {} as Sling Model from resource.", cls);
                return ProviderOutcome.notNullOrFailure(modelFactory.createModel(resource, cls));
            }
            return ProviderOutcome.failure(
                new IllegalStateException("Could not adapt the given Sling Model from neither request nor resource: " + cls));
        } catch (Throwable e) {
            return ProviderOutcome.failure(e);
        } finally {
            resetRequestAttribute(request, overrides);

        }
    }

    private Map<String, Object> setRequestAttributes(final ServletRequest request, final Bindings arguments) {
        Map<String, Object> overrides = new HashMap<>();
        for (Map.Entry<String, Object> entry : arguments.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            Object oldValue = request.getAttribute(key);
            if (oldValue != null) {
                overrides.put(key, oldValue);
            } else {
                overrides.put(key, null);
            }
            request.setAttribute(key, value);
        }
        return overrides;
    }

    private void resetRequestAttribute(final ServletRequest request, final Map<String, Object> overrides) {
        for (Map.Entry<String, Object> entry : overrides.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value == null) {
                request.removeAttribute(key);
            } else {
                request.setAttribute(key, value);
            }
        }
    }
}
