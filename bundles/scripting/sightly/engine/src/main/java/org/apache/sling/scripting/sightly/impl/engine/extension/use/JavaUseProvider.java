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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import javax.script.Bindings;
import javax.servlet.ServletRequest;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.commons.classloader.ClassLoaderWriter;
import org.apache.sling.scripting.sightly.impl.engine.ResourceBackedPojoChangeMonitor;
import org.apache.sling.scripting.sightly.impl.engine.SightlyJavaCompilerService;
import org.apache.sling.scripting.sightly.impl.utils.BindingsUtils;
import org.apache.sling.scripting.sightly.pojo.Use;
import org.apache.sling.scripting.sightly.render.RenderContext;
import org.apache.sling.scripting.sightly.use.ProviderOutcome;
import org.apache.sling.scripting.sightly.use.UseProvider;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        service = UseProvider.class,
        configurationPid = "org.apache.sling.scripting.sightly.impl.engine.extension.use.JavaUseProvider",
        property = {
                Constants.SERVICE_RANKING + ":Integer=90"
        }
)
public class JavaUseProvider implements UseProvider {

    @interface Configuration {

        @AttributeDefinition(
                name = "Service Ranking",
                description = "The Service Ranking value acts as the priority with which this Use Provider is queried to return an " +
                        "Use-object. A higher value represents a higher priority."
        )
        int service_ranking() default 90;

    }

    private static final Logger LOG = LoggerFactory.getLogger(JavaUseProvider.class);
    private static final Pattern JAVA_PATTERN = Pattern.compile("([[\\p{L}&&[^\\p{Lu}]]_$][\\p{L}\\p{N}_$]*\\.)*[\\p{Lu}_$][\\p{L}\\p{N}_$]*");

    @Reference
    private SightlyJavaCompilerService sightlyJavaCompilerService = null;

    @Reference
    private ResourceBackedPojoChangeMonitor resourceBackedPojoChangeMonitor = null;

    @Reference
    private ClassLoaderWriter classLoaderWriter = null;

    @Override
    public ProviderOutcome provide(String identifier, RenderContext renderContext, Bindings arguments) {
        if (!JAVA_PATTERN.matcher(identifier).matches()) {
            LOG.debug("Identifier {} does not match a Java class name pattern.", identifier);
            return ProviderOutcome.failure();
        }
        Bindings globalBindings = renderContext.getBindings();
        SlingScriptHelper sling = BindingsUtils.getHelper(globalBindings);
        SlingHttpServletRequest request = BindingsUtils.getRequest(globalBindings);
        Map<String, Object> overrides = setRequestAttributes(request, arguments);

        Object result;
        try {
            result = sightlyJavaCompilerService.getResourceBackedUseObject(renderContext, identifier);
            if (result != null) {
                if (result instanceof Use) {
                    ((Use) result).init(BindingsUtils.merge(globalBindings, arguments));
                }
                return ProviderOutcome.success(result);
            } else {
                LOG.debug("Attempting to load class {} from the classloader cache.", identifier);
                Class<?> cls = classLoaderWriter.getClassLoader().loadClass(identifier);
                // attempt OSGi service load
                result = sling.getService(cls);
                if (result != null) {
                    return ProviderOutcome.success(result);
                }
                result = request.adaptTo(cls);
                if (result == null) {
                    Resource resource = BindingsUtils.getResource(globalBindings);
                    result = resource.adaptTo(cls);
                }
                if (result != null) {
                    return ProviderOutcome.success(result);
                } else {
                    /*
                     * the object was cached by the class loader but it's not adaptable from {@link Resource} or {@link
                     * SlingHttpServletRequest}; attempt to load it like a regular POJO that optionally could implement {@link Use}
                     */
                    result = cls.newInstance();
                    if (result instanceof Use) {
                        ((Use) result).init(BindingsUtils.merge(globalBindings, arguments));
                    }
                    return ProviderOutcome.notNullOrFailure(result);
                }
            }
        } catch (Exception e) {
            // any other exception is an error
            return ProviderOutcome.failure(e);
        } finally {
            resetRequestAttribute(request, overrides);
        }
    }

    private Map<String, Object> setRequestAttributes(ServletRequest request, Bindings arguments) {
        Map<String, Object> overrides = new HashMap<>();
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
