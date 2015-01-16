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
package org.apache.sling.scripting.sightly.impl.engine.extension;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.script.Bindings;
import javax.servlet.RequestDispatcher;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.scripting.sightly.extension.RuntimeExtension;
import org.apache.sling.scripting.sightly.impl.plugin.ResourcePlugin;
import org.apache.sling.scripting.sightly.render.RenderContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runtime support for including resources in a Sightly script through {@code data-sly-resource}. For more details check the implementation
 * of the {@link org.apache.sling.scripting.sightly.impl.plugin.ResourcePlugin}.
 */
@Component
@Service(RuntimeExtension.class)
@Properties(
        @Property(name = RuntimeExtension.NAME, value = ResourcePlugin.FUNCTION)
)
@SuppressWarnings("unused")
public class ResourceRuntimeExtension implements RuntimeExtension {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceRuntimeExtension.class);
    private static final String OPTION_RESOURCE_TYPE = "resourceType";
    private static final String OPTION_PATH = "path";
    private static final String OPTION_PREPEND_PATH = "prependPath";
    private static final String OPTION_APPEND_PATH = "appendPath";
    private static final String OPTION_SELECTORS = "selectors";
    private static final String OPTION_REMOVE_SELECTORS = "removeSelectors";
    private static final String OPTION_ADD_SELECTORS = "addSelectors";
    private static final String OPTION_REPLACE_SELECTORS = "replaceSelectors";

    @Override
    @SuppressWarnings("unchecked")
    public Object call(final RenderContext renderContext, Object... arguments) {
        ExtensionUtils.checkArgumentCount(ResourcePlugin.FUNCTION, arguments, 2);
        return provideResource(renderContext, arguments[0], (Map<String, Object>) arguments[1]);
    }

    private String provideResource(final RenderContext renderContext, Object pathObj, Map<String, Object> options) {
        Map<String, Object> opts = new HashMap<String, Object>(options);
        String path = buildPath(pathObj, opts);
        String resourceType = getAndRemoveOption(opts, OPTION_RESOURCE_TYPE);
        final Bindings bindings = renderContext.getBindings();
        handleSelectors(bindings, path, opts);
        String dispatcherOptions = createDispatcherOptions(opts);
        StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        includeResource(bindings, printWriter, path, dispatcherOptions, resourceType);
        return writer.toString();
    }

    private void handleSelectors(final Bindings bindings, String path, Map<String, Object> options) {
        String selectors = getAndRemoveOption(options, OPTION_SELECTORS);
        if (StringUtils.isNotEmpty(selectors)) {
            // handle the selectors option
            options.put(OPTION_ADD_SELECTORS, selectors);
            options.put(OPTION_REPLACE_SELECTORS, " ");
        } else {
            if (options.containsKey(OPTION_REMOVE_SELECTORS)) {
                String removeSelectors = getAndRemoveOption(options, OPTION_REMOVE_SELECTORS);
                if (StringUtils.isEmpty(removeSelectors)) {
                    options.put(OPTION_REPLACE_SELECTORS, " ");
                } else {
                    String currentSelectors = getSelectorsFromPath(path);
                    if (StringUtils.isEmpty(currentSelectors)) {
                        currentSelectors = ((SlingHttpServletRequest) bindings.get(SlingBindings.REQUEST)).getRequestPathInfo()
                                .getSelectorString();
                    }
                    if (StringUtils.isNotEmpty(currentSelectors)) {
                        options.put(OPTION_REPLACE_SELECTORS, " ");
                        String addSelectors = currentSelectors.replace(removeSelectors, "").replaceAll("\\.\\.", "\\.");
                        if (addSelectors.startsWith(".")) {
                            addSelectors = addSelectors.substring(1);
                        }
                        if (addSelectors.endsWith(".")) {
                            addSelectors = addSelectors.substring(0, addSelectors.length() - 1);
                        }
                        options.put(OPTION_ADD_SELECTORS, addSelectors);
                    }
                }
            }
        }
    }

    private String buildPath(Object pathObj, Map<String, Object> options) {
        String path = coerceString(pathObj);
        String prependPath = getAndRemoveOption(options, OPTION_PREPEND_PATH);
        String appendPath = getAndRemoveOption(options, OPTION_APPEND_PATH);
        if (StringUtils.isEmpty(path)) {
            path = getOption(options, OPTION_PATH);
        }
        if (StringUtils.isNotEmpty(prependPath)) {
            path = prependPath + "/" + path;
        }
        if (StringUtils.isNotEmpty(appendPath)) {
            path = path + "/" + appendPath;
        }

        return path;
    }

    private String createDispatcherOptions(Map<String, Object> options) {
        if (options == null || options.isEmpty()) {
            return null;
        }
        StringBuilder buffer = new StringBuilder();
        boolean hasPreceding = false;
        for (Map.Entry<String, Object> option : options.entrySet()) {
            if (hasPreceding) {
                buffer.append(", ");
            }
            String key = option.getKey();
            buffer.append(key).append("=");
            String strVal = coerceString(option.getValue());
            if (strVal == null) {
                strVal = "";
            }
            buffer.append(strVal);
            hasPreceding = true;
        }
        return buffer.toString();
    }

    private String coerceString(Object obj) {
        if (obj instanceof String) {
            return (String) obj;
        }
        return null;
    }

    private String getOption(Map<String, Object> options, String property) {
        return (String) options.get(property);
    }

    private String getAndRemoveOption(Map<String, Object> options, String property) {
        return (String) options.remove(property);
    }

    private String getSelectorsFromPath(String path) {
        String filePath;
        if (path.contains("/")) {
            filePath = path.substring(path.lastIndexOf('/') + 1, path.length());
        } else {
            filePath = path;
        }
        String[] parts = filePath.split("\\.");
        if (parts.length > 2) {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < parts.length - 1; i++) {
                sb.append(parts[i]);
                if (i != parts.length - 2) {
                    sb.append(".");
                }
            }
            if (sb.length() > 0) {
                return sb.toString();
            }
        }
        return null;
    }

    private void includeResource(final Bindings bindings, PrintWriter out, String script, String dispatcherOptions, String resourceType) {
        if (StringUtils.isEmpty(script)) {
            LOG.error("Script path cannot be empty");
        } else {
            SlingHttpServletResponse customResponse = new PrintWriterResponseWrapper(out,
                    (SlingHttpServletResponse) bindings.get(SlingBindings.RESPONSE));
            SlingHttpServletRequest request = (SlingHttpServletRequest) bindings.get(SlingBindings.REQUEST);
            script = normalizePath(request, script);

            Resource includeRes = request.getResourceResolver().resolve(script);
            if (includeRes instanceof NonExistingResource || includeRes.isResourceType(Resource.RESOURCE_TYPE_NON_EXISTING)) {
                includeRes = new SyntheticResource(request.getResourceResolver(), script, resourceType);
            }
            try {
                RequestDispatcherOptions opts = new RequestDispatcherOptions(dispatcherOptions);
                if (StringUtils.isNotEmpty(resourceType)) {
                    opts.setForceResourceType(resourceType);
                }
                RequestDispatcher dispatcher = request.getRequestDispatcher(includeRes, opts);
                dispatcher.include(request, customResponse);
            } catch (Exception e) {
                LOG.error("Failed to include resource {}", script, e);
            }
        }
    }

    private String normalizePath(SlingHttpServletRequest request, String path) {
        if (!path.startsWith("/")) {
            path = request.getResource().getPath() + "/" + path;
        }
        return ResourceUtil.normalize(path);
    }
}
