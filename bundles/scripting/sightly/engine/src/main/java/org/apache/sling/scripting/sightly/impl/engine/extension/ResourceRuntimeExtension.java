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
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.scripting.sightly.SightlyException;
import org.apache.sling.scripting.sightly.compiler.RuntimeFunction;
import org.apache.sling.scripting.sightly.extension.RuntimeExtension;
import org.apache.sling.scripting.sightly.impl.utils.BindingsUtils;
import org.apache.sling.scripting.sightly.render.RenderContext;
import org.apache.sling.scripting.sightly.render.RuntimeObjectModel;
import org.osgi.service.component.annotations.Component;

/**
 * Runtime support for including resources in a HTL script through {@code data-sly-resource}.
 */
@Component(
        service = RuntimeExtension.class,
        property = {
                RuntimeExtension.NAME + "=" + RuntimeFunction.RESOURCE
        }
)
public class ResourceRuntimeExtension implements RuntimeExtension {

    private static final String OPTION_RESOURCE_TYPE = "resourceType";
    private static final String OPTION_PATH = "path";
    private static final String OPTION_PREPEND_PATH = "prependPath";
    private static final String OPTION_APPEND_PATH = "appendPath";
    private static final String OPTION_SELECTORS = "selectors";
    private static final String OPTION_REMOVE_SELECTORS = "removeSelectors";
    private static final String OPTION_ADD_SELECTORS = "addSelectors";
    private static final String OPTION_REPLACE_SELECTORS = "replaceSelectors";
    private static final String OPTION_REQUEST_ATTRIBUTES = "requestAttributes";

    @Override
    public Object call(final RenderContext renderContext, Object... arguments) {
        ExtensionUtils.checkArgumentCount(RuntimeFunction.RESOURCE, arguments, 2);
        return provideResource(renderContext, arguments[0], (Map<String, Object>) arguments[1]);
    }

    private String provideResource(final RenderContext renderContext, Object pathObj, Map<String, Object> options) {
        Map<String, Object> opts = new HashMap<>(options);
        final Bindings bindings = renderContext.getBindings();
        SlingHttpServletRequest request = BindingsUtils.getRequest(bindings);
        Map originalAttributes = ExtensionUtils.setRequestAttributes(request, (Map)options.remove(OPTION_REQUEST_ATTRIBUTES));
        RuntimeObjectModel runtimeObjectModel = renderContext.getObjectModel();
        String resourceType = runtimeObjectModel.toString(getAndRemoveOption(opts, OPTION_RESOURCE_TYPE));
        StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        if (pathObj instanceof Resource) {
            Resource includedResource = (Resource) pathObj;
            Map<String, String> dispatcherOptionsMap = handleSelectors(request, new LinkedHashSet<String>(), opts, runtimeObjectModel);
            String dispatcherOptions = createDispatcherOptions(dispatcherOptionsMap);
            includeResource(bindings, printWriter, includedResource, dispatcherOptions, resourceType);
        } else {
            String includePath = runtimeObjectModel.toString(pathObj);
            // build path completely
            includePath = buildPath(includePath, options, request.getResource());
            if (includePath != null) {
                // check if path identifies an existing resource
                Resource includedResource = request.getResourceResolver().getResource(includePath);
                PathInfo pathInfo;
                if (includedResource != null) {
                    Map<String, String> dispatcherOptionsMap =
                            handleSelectors(request, new LinkedHashSet<String>(), opts, runtimeObjectModel);
                    String dispatcherOptions = createDispatcherOptions(dispatcherOptionsMap);
                    includeResource(bindings, printWriter, includedResource, dispatcherOptions, resourceType);
                } else {
                    // analyse path and decompose potential selectors from the path
                    pathInfo = new PathInfo(includePath);
                    Map<String, String> dispatcherOptionsMap = handleSelectors(request, pathInfo.selectors, opts, runtimeObjectModel);
                    String dispatcherOptions = createDispatcherOptions(dispatcherOptionsMap);
                    includeResource(bindings, printWriter, pathInfo.path, dispatcherOptions, resourceType);
                }
            } else {
                // use the current resource
                Map<String, String> dispatcherOptionsMap = handleSelectors(request, new LinkedHashSet<String>(), opts, runtimeObjectModel);
                String dispatcherOptions = createDispatcherOptions(dispatcherOptionsMap);
                includeResource(bindings, printWriter, request.getResource(), dispatcherOptions, resourceType);
            }
        }
        ExtensionUtils.setRequestAttributes(request, originalAttributes);
        return writer.toString();
    }

    private Map<String, String> handleSelectors(SlingHttpServletRequest request, Set<String> selectors, Map<String, Object> options,
                                                RuntimeObjectModel runtimeObjectModel) {
        if (selectors.isEmpty()) {
            selectors.addAll(Arrays.asList(request.getRequestPathInfo().getSelectors()));
        }
        Map<String, String> dispatcherOptionsMap = new HashMap<>();
        dispatcherOptionsMap.put(OPTION_ADD_SELECTORS, getSelectorString(selectors));
        dispatcherOptionsMap.put(OPTION_REPLACE_SELECTORS, " ");
        if (options.containsKey(OPTION_SELECTORS)) {
            Object selectorsObject = getAndRemoveOption(options, OPTION_SELECTORS);
            selectors.clear();
            addSelectors(selectors, selectorsObject, runtimeObjectModel);
            dispatcherOptionsMap.put(OPTION_ADD_SELECTORS, getSelectorString(selectors));
            dispatcherOptionsMap.put(OPTION_REPLACE_SELECTORS, " ");
        }
        if (options.containsKey(OPTION_ADD_SELECTORS)) {
            Object selectorsObject = getAndRemoveOption(options, OPTION_ADD_SELECTORS);
            addSelectors(selectors, selectorsObject, runtimeObjectModel);
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
                    String selector = runtimeObjectModel.toString(s);
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

    private void addSelectors(Set<String> selectors, Object selectorsObject, RuntimeObjectModel runtimeObjectModel) {
        if (selectorsObject instanceof String) {
            String selectorString = (String) selectorsObject;
            String[] parts = selectorString.split("\\.");
            selectors.addAll(Arrays.asList(parts));
        } else if (selectorsObject instanceof Object[]) {
            for (Object s : (Object[]) selectorsObject) {
                String selector = runtimeObjectModel.toString(s);
                if (StringUtils.isNotEmpty(selector)) {
                    selectors.add(selector);
                }
            }
        }
    }

    private String buildPath(String path, Map<String, Object> options, Resource currentResource) {
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
        String finalPath = prependPath + path + appendPath;
        if (!finalPath.startsWith("/")) {
            finalPath = currentResource.getPath() + "/" + finalPath;
        }
        return ResourceUtil.normalize(finalPath);
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

    private String getOption(String option, Map<String, Object> options, String defaultValue) {
        if (options.containsKey(option)) {
            return (String) options.get(option);
        }
        return defaultValue;
    }

    private Object getAndRemoveOption(Map<String, Object> options, String property) {
        return options.remove(property);
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

    private void includeResource(final Bindings bindings, PrintWriter out, String path, String dispatcherOptions, String resourceType) {
        if (StringUtils.isEmpty(path)) {
            throw new SightlyException("Resource path cannot be empty");
        } else {
            SlingHttpServletRequest request = BindingsUtils.getRequest(bindings);
            Resource includeRes = request.getResourceResolver().resolve(path);
            if (ResourceUtil.isNonExistingResource(includeRes)) {
                includeRes = new SyntheticResource(request.getResourceResolver(), path, resourceType);
            }
            includeResource(bindings, out, includeRes, dispatcherOptions, resourceType);
        }
    }

    private void includeResource(final Bindings bindings, PrintWriter out, Resource includeRes, String dispatcherOptions, String resourceType) {
        if (includeRes == null) {
            throw new SightlyException("Resource cannot be null");
        } else {
            SlingHttpServletResponse customResponse = new PrintWriterResponseWrapper(out, BindingsUtils.getResponse(bindings));
            SlingHttpServletRequest request = BindingsUtils.getRequest(bindings);
            RequestDispatcherOptions opts = new RequestDispatcherOptions(dispatcherOptions);
            if (StringUtils.isNotEmpty(resourceType)) {
                opts.setForceResourceType(resourceType);
            }
            RequestDispatcher dispatcher = request.getRequestDispatcher(includeRes, opts);
            try {
                if (dispatcher != null) {
                    dispatcher.include(request, customResponse);
                } else {
                    throw new SightlyException("Failed to include resource " + includeRes.getPath());
                }
            } catch (Exception e) {
                throw new SightlyException("Failed to include resource " + includeRes.getPath(), e);
            }
        }
    }

    private class PathInfo {
        private String path;
        private Set<String> selectors;

        PathInfo(String path) {
            selectors = getSelectorsFromPath(path);
            if (selectors.isEmpty()) {
                this.path = path;
            } else {
                String selectorString = getSelectorString(selectors);
                this.path = path.replace("." + selectorString, "");
            }
        }

        private Set<String> getSelectorsFromPath(String path) {
            Set<String> selectors = new LinkedHashSet<>();
            if (path != null) {
                String processingPath = path;
                int lastSlashPos = path.lastIndexOf('/');
                if (lastSlashPos > -1) {
                    processingPath = path.substring(lastSlashPos + 1, path.length());
                }
                int dotPos = processingPath.indexOf('.');
                if (dotPos > -1) {
                    int lastDotPos = processingPath.lastIndexOf('.');
                    // We're expecting selectors only when an extension is also present. If there's
                    // one dot it means we only have the extension
                    if (lastDotPos > dotPos) {
                        String selectorString = processingPath.substring(dotPos + 1, lastDotPos);
                        String[] selectorParts = selectorString.split("\\.");
                        selectors.addAll(Arrays.asList(selectorParts));
                    }
                }
            }
            return selectors;
        }
    }
}
