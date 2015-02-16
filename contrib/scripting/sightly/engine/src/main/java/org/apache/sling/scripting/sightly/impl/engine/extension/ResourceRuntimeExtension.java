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
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

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
        PathInfo pathInfo = new PathInfo(coerceString(pathObj));
        String path = pathInfo.path;
        String finalPath = buildPath(path, opts);
        String resourceType = coerceString(getAndRemoveOption(opts, OPTION_RESOURCE_TYPE));
        final Bindings bindings = renderContext.getBindings();
        Map<String, String> dispatcherOptionsMap = handleSelectors(bindings, pathInfo.selectors, opts);
        String dispatcherOptions = createDispatcherOptions(dispatcherOptionsMap);
        StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        includeResource(bindings, printWriter, finalPath, dispatcherOptions, resourceType);
        return writer.toString();
    }

    private Map<String, String> handleSelectors(Bindings bindings, Set<String> selectors, Map<String, Object> options) {
        if (selectors.isEmpty()) {
            SlingHttpServletRequest request = (SlingHttpServletRequest) bindings.get(SlingBindings.REQUEST);
            selectors.addAll(Arrays.asList(request.getRequestPathInfo().getSelectors()));
        }
        Map<String, String> dispatcherOptionsMap = new HashMap<String, String>();
        if (options.containsKey(OPTION_SELECTORS)) {
            Object selectorsObject = getAndRemoveOption(options, OPTION_SELECTORS);
            selectors.clear();
            if (selectorsObject instanceof String) {
                String selectorString = (String) selectorsObject;
                String[] parts = selectorString.split("\\.");
                selectors.addAll(Arrays.asList(parts));
            } else if (selectorsObject instanceof Object[]) {
                for (Object s : (Object[]) selectorsObject) {
                    String selector = coerceString(s);
                    if (StringUtils.isNotEmpty(selector)) {
                        selectors.add(selector);
                    }
                }
            }
            dispatcherOptionsMap.put(OPTION_ADD_SELECTORS, getSelectorString(selectors));
            dispatcherOptionsMap.put(OPTION_REPLACE_SELECTORS, " ");
        }
        if (options.containsKey(OPTION_ADD_SELECTORS)) {
            Object selectorsObject = getAndRemoveOption(options, OPTION_ADD_SELECTORS);
            if (selectorsObject instanceof String) {
                String selectorString = (String) selectorsObject;
                String[] parts = selectorString.split("\\.");
                for (String s : parts) {
                    selectors.add(s);
                }
            } else if (selectorsObject instanceof Object[]) {
                for (Object s : (Object[]) selectorsObject) {
                    String selector = coerceString(s);
                    if (StringUtils.isNotEmpty(selector)) {
                        selectors.add(selector);
                    }
                }
            }
            dispatcherOptionsMap.put(OPTION_ADD_SELECTORS, getSelectorString(selectors));
            dispatcherOptionsMap.put(OPTION_REPLACE_SELECTORS, " ");
        }
        if (options.containsKey(OPTION_REMOVE_SELECTORS)) {
            Object selectorsObject = getAndRemoveOption(options, OPTION_REMOVE_SELECTORS);
            if (selectorsObject instanceof String) {
                String selectorString = (String) selectorsObject;
                String[] parts = selectorString.split("\\.");
                for (String s : parts) {
                    selectors.remove(s);
                }
            } else if (selectorsObject instanceof Object[]) {
                for (Object s : (Object[]) selectorsObject) {
                    String selector = coerceString(s);
                    if (StringUtils.isNotEmpty(selector)) {
                        selectors.remove(selector);
                    }
                }
            } else if (selectorsObject == null) {
                selectors.clear();
            }
            String selectorString = getSelectorString(selectors);
            if (StringUtils.isEmpty(selectorString)) {
                dispatcherOptionsMap.put(OPTION_REPLACE_SELECTORS, " ");
            } else {
                dispatcherOptionsMap.put(OPTION_ADD_SELECTORS, getSelectorString(selectors));
                dispatcherOptionsMap.put(OPTION_REPLACE_SELECTORS, " ");
            }
        }
        return dispatcherOptionsMap;
    }

    private String buildPath(Object pathObj, Map<String, Object> options) {
        String prependPath = getOption(OPTION_PREPEND_PATH, options, StringUtils.EMPTY);
        if (prependPath == null) {
            prependPath = StringUtils.EMPTY;
        }
        if (StringUtils.isNotEmpty(prependPath)) {
            if (!prependPath.startsWith("/")) {
                prependPath = "/" + prependPath;
            }
            if (!prependPath.endsWith("/")) {
                prependPath += "/";
            }
        }
        String path = coerceString(pathObj);
        path = getOption(OPTION_PATH, options, StringUtils.isNotEmpty(path) ? path : StringUtils.EMPTY);
        String appendPath = getOption(OPTION_APPEND_PATH, options, StringUtils.EMPTY);
        if (appendPath == null) {
            appendPath = StringUtils.EMPTY;
        }
        if (StringUtils.isNotEmpty(appendPath)) {
            if (!appendPath.startsWith("/")) {
                appendPath = "/" + appendPath;
            }
        }
        return ResourceUtil.normalize(prependPath + path + appendPath);
    }

    private String createDispatcherOptions(Map<String, String> options) {
        if (options == null || options.isEmpty()) {
            return null;
        }
        StringBuilder buffer = new StringBuilder();
        boolean hasPreceding = false;
        for (Map.Entry<String, String> option : options.entrySet()) {
            if (hasPreceding) {
                buffer.append(", ");
            }
            String key = option.getKey();
            buffer.append(key).append("=");
            String strVal = option.getValue();
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

    private String getOption(String option, Map<String, Object> options, String defaultValue) {
        if (options.containsKey(option)) {
            return (String) options.get(option);
        }
        return defaultValue;
    }

    private Object getAndRemoveOption(Map<String, Object> options, String property) {
        return options.remove(property);
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

    private class PathInfo {
        private String path;
        private Set<String> selectors;

        PathInfo(String path) {
            selectors = getSelectorsFromPath(path);
            String selectorString = getSelectorString(selectors);
            if (StringUtils.isNotEmpty(selectorString)) {
                this.path = path.substring(0, path.length() - selectorString.length() - 1);
            } else {
                this.path = path;
            }
        }
    }

    private Set<String> getSelectorsFromPath(String path) {
        Set<String> selectors = new LinkedHashSet<String>();
        if (path != null) {
            String processingPath = path;
            int lastSlashPos = path.lastIndexOf('/');
            if (lastSlashPos > -1) {
                processingPath = path.substring(lastSlashPos + 1, path.length());
            }
            int dotPos = processingPath.indexOf('.');
            if (dotPos > -1) {
                String selectorString = processingPath.substring(dotPos + 1, processingPath.length());
                String[] selectorParts = selectorString.split("\\.");
                selectors.addAll(Arrays.asList(selectorParts));
            }
        }
        return selectors;
    }

    private String getSelectorString(Set<String> selectors) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (String s : selectors) {
            sb.append(s);
            if (i < selectors.size() - 1) {
                sb.append(".");
                i++;
            }
        }
        return sb.toString();
    }
}
