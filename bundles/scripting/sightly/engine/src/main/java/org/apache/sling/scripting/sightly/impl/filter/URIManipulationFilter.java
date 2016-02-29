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
package org.apache.sling.scripting.sightly.impl.filter;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.scripting.sightly.extension.RuntimeExtension;
import org.apache.sling.scripting.sightly.impl.compiler.expression.Expression;
import org.apache.sling.scripting.sightly.impl.compiler.expression.ExpressionNode;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.MapLiteral;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.RuntimeCall;
import org.apache.sling.scripting.sightly.impl.engine.extension.ExtensionUtils;
import org.apache.sling.scripting.sightly.impl.utils.PathInfo;
import org.apache.sling.scripting.sightly.impl.utils.RenderUtils;
import org.apache.sling.scripting.sightly.render.RenderContext;

/**
 * The {@code URIManipulationFilter} provides support for Sightly's URI Manipulation options according to the
 * <a href="https://github.com/Adobe-Marketing-Cloud/sightly-spec/blob/1.1/SPECIFICATION.md">language specification</a>
 */
@Component
@Service({Filter.class, RuntimeExtension.class})
@Properties({
        @Property(name = RuntimeExtension.NAME, value = URIManipulationFilter.URI_MANIPULATION_FUNCTION)
})
public class URIManipulationFilter extends FilterComponent implements RuntimeExtension {

    public static final String URI_MANIPULATION_FUNCTION = "uriManipulation";

    private static final String SCHEME = "scheme";
    private static final String DOMAIN = "domain";
    private static final String PATH = "path";
    private static final String APPEND_PATH = "appendPath";
    private static final String PREPEND_PATH = "prependPath";
    private static final String SELECTORS = "selectors";
    private static final String ADD_SELECTORS = "addSelectors";
    private static final String REMOVE_SELECTORS = "removeSelectors";
    private static final String EXTENSION = "extension";
    private static final String SUFFIX = "suffix";
    private static final String PREPEND_SUFFIX = "prependSuffix";
    private static final String APPEND_SUFFIX = "appendSuffix";
    private static final String FRAGMENT = "fragment";
    private static final String QUERY = "query";
    private static final String ADD_QUERY = "addQuery";
    private static final String REMOVE_QUERY = "removeQuery";

    @Override
    public Expression apply(Expression expression, ExpressionContext expressionContext) {
        if ((expression.containsOption(SCHEME) || expression.containsOption(DOMAIN) || expression.containsOption(PATH) || expression
                .containsOption(APPEND_PATH) || expression.containsOption(PREPEND_PATH) || expression.containsOption(SELECTORS) ||
                expression.containsOption(ADD_SELECTORS) || expression.containsOption(REMOVE_SELECTORS) || expression.containsOption
                (EXTENSION) || expression.containsOption(SUFFIX) || expression.containsOption(PREPEND_SUFFIX) || expression
                .containsOption(APPEND_SUFFIX) || expression.containsOption(FRAGMENT) || expression.containsOption(QUERY) || expression
                .containsOption(ADD_QUERY) || expression.containsOption(REMOVE_QUERY)) && expressionContext != ExpressionContext
                .PLUGIN_DATA_SLY_USE && expressionContext
                != ExpressionContext.PLUGIN_DATA_SLY_TEMPLATE && expressionContext != ExpressionContext.PLUGIN_DATA_SLY_CALL &&
                expressionContext != ExpressionContext.PLUGIN_DATA_SLY_RESOURCE) {
            Map<String, ExpressionNode> uriOptions = getFilterOptions(expression, SCHEME, DOMAIN, PATH, APPEND_PATH, PREPEND_PATH,
                    SELECTORS, ADD_SELECTORS, REMOVE_SELECTORS, EXTENSION, SUFFIX, PREPEND_SUFFIX, APPEND_SUFFIX, FRAGMENT, QUERY,
                    ADD_QUERY, REMOVE_QUERY);
            if (uriOptions.size() > 0) {
                ExpressionNode translation = new RuntimeCall(URI_MANIPULATION_FUNCTION, expression.getRoot(), new MapLiteral(uriOptions));
                return expression.withNode(translation);
            }
        }
        return expression;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object call(RenderContext renderContext, Object... arguments) {
        ExtensionUtils.checkArgumentCount(URI_MANIPULATION_FUNCTION, arguments, 2);
        String uriString = RenderUtils.toString(arguments[0]);
        Map<String, Object> options = RenderUtils.toMap(arguments[1]);
        StringBuilder sb = new StringBuilder();
        PathInfo pathInfo = new PathInfo(uriString);
        uriAppender(sb, SCHEME, options, pathInfo.getScheme());
        if (sb.length() > 0) {
            sb.append(":");
            sb.append(StringUtils.defaultIfEmpty(pathInfo.getBeginPathSeparator(), "//"));
        }
        if (sb.length() > 0) {
            uriAppender(sb, DOMAIN, options, pathInfo.getHost());
        } else {
            String domain = getOption(DOMAIN, options, pathInfo.getHost());
            if (StringUtils.isNotEmpty(domain)) {
                sb.append("//").append(domain);
            }
        }
        if (pathInfo.getPort() > -1) {
            sb.append(":").append(pathInfo.getPort());
        }
        String prependPath = getOption(PREPEND_PATH, options, StringUtils.EMPTY);
        if (prependPath == null) {
            prependPath = StringUtils.EMPTY;
        }
        if (StringUtils.isNotEmpty(prependPath)) {
            if (sb.length() > 0 && !prependPath.startsWith("/")) {
                prependPath = "/" + prependPath;
            }
            if (!prependPath.endsWith("/")) {
                prependPath += "/";
            }
        }
        String path = getOption(PATH, options, pathInfo.getPath());
        if (StringUtils.isEmpty(path)) {
            // if the path is forced to be empty don't remove the path
            path = pathInfo.getPath();
        }
        String appendPath = getOption(APPEND_PATH, options, StringUtils.EMPTY);
        if (appendPath == null) {
            appendPath = StringUtils.EMPTY;
        }
        if (StringUtils.isNotEmpty(appendPath)) {
            if (!appendPath.startsWith("/")) {
                appendPath = "/" + appendPath;
            }
        }
        String newPath;
        try {
            newPath = new URI(prependPath + path + appendPath).normalize().getPath();
        } catch (URISyntaxException e) {
            newPath = prependPath + path + appendPath;
        }
        if (sb.length() > 0 && sb.lastIndexOf("/") != sb.length() - 1 && StringUtils.isNotEmpty(newPath) && !newPath.startsWith("/")) {
            sb.append("/");
        }
        sb.append(newPath);
        Set<String> selectors = pathInfo.getSelectors();
        handleSelectors(selectors, options);
        for (String selector : selectors) {
            if (StringUtils.isNotBlank(selector) && !selector.contains(" ")) {
                // make sure not to append empty or invalid selectors
                sb.append(".").append(selector);
            }
        }
        String extension = getOption(EXTENSION, options, pathInfo.getExtension());
        if (StringUtils.isNotEmpty(extension)) {
            sb.append(".").append(extension);
        }

        String prependSuffix = getOption(PREPEND_SUFFIX, options, StringUtils.EMPTY);
        if (StringUtils.isNotEmpty(prependSuffix)) {
            if (!prependSuffix.startsWith("/")) {
                prependSuffix = "/" + prependSuffix;
            }
            if (!prependSuffix.endsWith("/")) {
                prependSuffix += "/";
            }
        }
        String pathInfoSuffix = pathInfo.getSuffix();
        String suffix = getOption(SUFFIX, options, pathInfoSuffix == null ? StringUtils.EMPTY : pathInfoSuffix);
        if (suffix == null) {
            suffix = StringUtils.EMPTY;
        }
        String appendSuffix = getOption(APPEND_SUFFIX, options, StringUtils.EMPTY);
        if (StringUtils.isNotEmpty(appendSuffix)) {
            appendSuffix = "/" + appendSuffix;
        }
        String newSuffix = ResourceUtil.normalize(prependSuffix + suffix + appendSuffix);
        if (StringUtils.isNotEmpty(newSuffix)) {
            if (!newSuffix.startsWith("/")) {
                sb.append("/");
            }
            sb.append(newSuffix);
        }
        Map<String, Collection<String>> parameters = pathInfo.getParameters();
        handleParameters(parameters, options);
        if (!parameters.isEmpty()) {
            sb.append("?");
            for (Map.Entry<String, Collection<String>> entry : parameters.entrySet()) {
                for (String value : entry.getValue()) {
                    sb.append(entry.getKey()).append("=").append(value).append("&");
                }
            }
            // delete the last &
            sb.deleteCharAt(sb.length() - 1);
        }
        String fragment = getOption(FRAGMENT, options, pathInfo.getFragment());
        if (StringUtils.isNotEmpty(fragment)) {
            sb.append("#").append(fragment);
        }
        return sb.toString();
    }

    private void uriAppender(StringBuilder stringBuilder, String option, Map<String, Object> options, String defaultValue) {
        String value = (String) options.get(option);
        if (StringUtils.isNotEmpty(value)) {
            stringBuilder.append(value);
        } else {
            if (StringUtils.isNotEmpty(defaultValue)) {
                stringBuilder.append(defaultValue);
            }
        }
    }

    private String getOption(String option, Map<String, Object> options, String defaultValue) {
        if (options.containsKey(option)) {
            return (String) options.get(option);

        }
        return defaultValue;
    }

    private void handleSelectors(Set<String> selectors, Map<String, Object> options) {
        if (options.containsKey(SELECTORS)) {
            Object selectorsOption = options.get(SELECTORS);
            if (selectorsOption == null) {
                // we want to remove all selectors
                selectors.clear();
            } else if (selectorsOption instanceof String) {
                String selectorString = (String) selectorsOption;
                String[] selectorsArray = selectorString.split("\\.");
                replaceSelectors(selectors, selectorsArray);
            } else if (selectorsOption instanceof Object[]) {
                Object[] selectorsURIArray = (Object[]) selectorsOption;
                String[] selectorsArray = new String[selectorsURIArray.length];
                int index = 0;
                for (Object selector : selectorsURIArray) {
                    selectorsArray[index++] = RenderUtils.toString(selector);
                }
                replaceSelectors(selectors, selectorsArray);
            }
        }
        Object addSelectorsOption = options.get(ADD_SELECTORS);
        if (addSelectorsOption instanceof String) {
            String selectorString = (String) addSelectorsOption;
            String[] selectorsArray = selectorString.split("\\.");
            addSelectors(selectors, selectorsArray);
        } else if (addSelectorsOption instanceof Object[]) {
            Object[] selectorsURIArray = (Object[]) addSelectorsOption;
            String[] selectorsArray = new String[selectorsURIArray.length];
            int index = 0;
            for (Object selector : selectorsURIArray) {
                selectorsArray[index++] = RenderUtils.toString(selector);
            }
            addSelectors(selectors, selectorsArray);
        }
        Object removeSelectorsOption = options.get(REMOVE_SELECTORS);
        if (removeSelectorsOption instanceof String) {
            String selectorString = (String) removeSelectorsOption;
            String[] selectorsArray = selectorString.split("\\.");
            removeSelectors(selectors, selectorsArray);
        } else if (removeSelectorsOption instanceof Object[]) {
            Object[] selectorsURIArray = (Object[]) removeSelectorsOption;
            String[] selectorsArray = new String[selectorsURIArray.length];
            int index = 0;
            for (Object selector : selectorsURIArray) {
                selectorsArray[index++] = RenderUtils.toString(selector);
            }
            removeSelectors(selectors, selectorsArray);
        }

    }

    private void replaceSelectors(Set<String> selectors, String[] selectorsArray) {
        selectors.clear();
        selectors.addAll(Arrays.asList(selectorsArray));
    }

    private void addSelectors(Set<String> selectors, String[] selectorsArray) {
        selectors.addAll(Arrays.asList(selectorsArray));
    }

    private void removeSelectors(Set<String> selectors, String[] selectorsArray) {
        selectors.removeAll(Arrays.asList(selectorsArray));
    }

    @SuppressWarnings("unchecked")
    private void handleParameters(Map<String, Collection<String>> parameters, Map<String, Object> options) {
        if (options.containsKey(QUERY)) {
            Object queryOption = options.get(QUERY);
            parameters.clear();
            Map<String, Object> queryParameters = RenderUtils.toMap(queryOption);
            for (Map.Entry<String, Object> entry : queryParameters.entrySet()) {
                Object entryValue = entry.getValue();
                if (RenderUtils.isCollection(entryValue)) {
                    Collection<Object> collection = RenderUtils.toCollection(entryValue);
                    Collection<String> values = new ArrayList<String>(collection.size());
                    for (Object o : collection) {
                        values.add(RenderUtils.toString(o));
                    }
                    parameters.put(entry.getKey(), values);
                } else {
                    Collection<String> values = new ArrayList<String>(1);
                    values.add(RenderUtils.toString(entryValue));
                    parameters.put(entry.getKey(), values);
                }
            }
        }
        Object addQueryOption = options.get(ADD_QUERY);
        if (addQueryOption != null) {
            Map<String, Object> addParams = RenderUtils.toMap(addQueryOption);
            for (Map.Entry<String, Object> entry : addParams.entrySet()) {
                Object entryValue = entry.getValue();
                if (RenderUtils.isCollection(entryValue)) {
                    Collection<Object> collection = RenderUtils.toCollection(entryValue);
                    Collection<String> values = new ArrayList<String>(collection.size());
                    for (Object o : collection) {
                        values.add(RenderUtils.toString(o));
                    }
                    parameters.put(entry.getKey(), values);
                } else {
                    Collection<String> values = new ArrayList<String>(1);
                    values.add(RenderUtils.toString(entryValue));
                    parameters.put(entry.getKey(), values);
                }
            }
        }
        Object removeQueryOption = options.get(REMOVE_QUERY);
        if (removeQueryOption != null) {
            if (removeQueryOption instanceof String) {
                parameters.remove(removeQueryOption);
            } else if (removeQueryOption instanceof Object[]) {
                Object[] removeQueryParamArray = (Object[]) removeQueryOption;
                for (Object param : removeQueryParamArray) {
                    String paramString = RenderUtils.toString(param);
                    if (paramString != null) {
                        parameters.remove(paramString);
                    }
                }
            }
        }
    }
}
