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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.scripting.sightly.ResourceResolution;
import org.apache.sling.scripting.sightly.impl.compiler.CompilerException;
import org.apache.sling.scripting.sightly.impl.compiler.SightlyJavaCompilerService;
import org.apache.sling.scripting.sightly.pojo.Use;
import org.apache.sling.scripting.sightly.render.RenderContext;
import org.apache.sling.scripting.sightly.use.ProviderOutcome;
import org.apache.sling.scripting.sightly.use.UseProvider;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        metatype = true,
        label = "Apache Sling Scripting Sightly Java Use Provider",
        description = "The Java Use Provider is responsible for instantiating Java Use-API objects."
)
@Service(UseProvider.class)
@Properties({
        @Property(
                name = Constants.SERVICE_RANKING,
                label = "Service Ranking",
                description = "The Service Ranking value acts as the priority with which this Use Provider is queried to return an " +
                        "Use-object. A higher value represents a higher priority.",
                intValue = 90,
                propertyPrivate = false
        )
})
public class JavaUseProvider implements UseProvider {

    private static final Logger LOG = LoggerFactory.getLogger(JavaUseProvider.class);
    private static final Pattern JAVA_PATTERN = Pattern.compile("([[\\p{L}&&[^\\p{Lu}]]_$][\\p{L}\\p{N}_$]*\\.)*[\\p{Lu}_$][\\p{L}\\p{N}_$]*");

    @Reference
    private SightlyJavaCompilerService sightlyJavaCompilerService = null;

    @Reference
    private DynamicClassLoaderManager dynamicClassLoaderManager = null;

    @Override
    public ProviderOutcome provide(String identifier, RenderContext renderContext, Bindings arguments) {
        if (!JAVA_PATTERN.matcher(identifier).matches()) {
            LOG.debug("Identifier {} does not match a Java class name pattern.", identifier);
            return ProviderOutcome.failure();
        }

        Bindings globalBindings = renderContext.getBindings();
        Bindings bindings = UseProviderUtils.merge(globalBindings, arguments);
        SlingScriptHelper sling = UseProviderUtils.getHelper(bindings);
        Resource resource = (Resource) bindings.get(SlingBindings.RESOURCE);
        final SlingHttpServletRequest request = (SlingHttpServletRequest) bindings.get(SlingBindings.REQUEST);
        Map<String, Object> overrides = setRequestAttributes(request, arguments);

        Object result;
        try {
            Class<?> cls = dynamicClassLoaderManager.getDynamicClassLoader().loadClass(identifier);
            result = resource.adaptTo(cls);
            if (result == null) {
                result = request.adaptTo(cls);
            }
            if (result != null) {
                return ProviderOutcome.success(result);
            } else {
                /**
                 * the object was cached by the classloader but it's not adaptable from {@link Resource} or {@link
                 * SlingHttpServletRequest}; attempt to load it like a regular POJO that optionally could implement {@link Use}
                 */
                result = cls.newInstance();
                if (result instanceof Use) {
                    ((Use) result).init(bindings);
                }
                return ProviderOutcome.notNullOrFailure(result);
            }
        } catch (ClassNotFoundException e) {
            // this object might not be exported from a bundle; let's try to load it from the repository
            return getPOJOFromRepository(renderContext, sling, identifier, bindings);
        } catch (Exception e) {
            // any other exception is an error
            return ProviderOutcome.failure(e);
        } finally {
            resetRequestAttribute(request, overrides);
        }
    }

    private ProviderOutcome getPOJOFromRepository(RenderContext renderContext, SlingScriptHelper sling, String identifier, Bindings bindings) {
        try {
            ResourceResolver adminResolver = renderContext.getScriptResourceResolver();
            Resource resource = ResourceResolution.getResourceForRequest(adminResolver, sling.getRequest());
            Object result = sightlyJavaCompilerService.getInstance(adminResolver, resource, identifier);
            if (result instanceof Use) {
                ((Use) result).init(bindings);
            }
            return ProviderOutcome.success(result);
        } catch (Exception e) {
            return ProviderOutcome.failure(e);
        }
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
