/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ******************************************************************************/
package org.apache.sling.scripting.sightly.impl.engine.extension;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.scripting.sightly.SightlyException;
import org.apache.sling.scripting.sightly.compiler.RuntimeFunction;
import org.apache.sling.scripting.sightly.extension.RuntimeExtension;
import org.apache.sling.scripting.sightly.render.RenderContext;
import org.apache.sling.scripting.sightly.render.RuntimeObjectModel;

@Component
@Service(RuntimeExtension.class)
@Properties({
        @Property(name = RuntimeExtension.NAME, value = RuntimeFunction.URI_MANIPULATION)
})
public class URIManipulationFilterExtension implements RuntimeExtension {

    public static final String SCHEME = "scheme";
    public static final String DOMAIN = "domain";
    public static final String PATH = "path";
    public static final String APPEND_PATH = "appendPath";
    public static final String PREPEND_PATH = "prependPath";
    public static final String SELECTORS = "selectors";
    public static final String ADD_SELECTORS = "addSelectors";
    public static final String REMOVE_SELECTORS = "removeSelectors";
    public static final String EXTENSION = "extension";
    public static final String SUFFIX = "suffix";
    public static final String PREPEND_SUFFIX = "prependSuffix";
    public static final String APPEND_SUFFIX = "appendSuffix";
    public static final String FRAGMENT = "fragment";
    public static final String QUERY = "query";
    public static final String ADD_QUERY = "addQuery";
    public static final String REMOVE_QUERY = "removeQuery";


    @Override
    @SuppressWarnings("unchecked")
    public Object call(RenderContext renderContext, Object... arguments) {
        ExtensionUtils.checkArgumentCount(RuntimeFunction.URI_MANIPULATION, arguments, 2);
        RuntimeObjectModel runtimeObjectModel = renderContext.getObjectModel();
        String uriString = runtimeObjectModel.toString(arguments[0]);
        Map<String, Object> options = runtimeObjectModel.toMap(arguments[1]);
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
        handleSelectors(runtimeObjectModel, selectors, options);
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
        String newSuffix = FilenameUtils.normalize(prependSuffix + suffix + appendSuffix, true);
        if (StringUtils.isNotEmpty(newSuffix)) {
            if (!newSuffix.startsWith("/")) {
                sb.append("/");
            }
            sb.append(newSuffix);
        }
        Map<String, Collection<String>> parameters = pathInfo.getParameters();
        handleParameters(runtimeObjectModel, parameters, options);
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

    private void handleSelectors(RuntimeObjectModel runtimeObjectModel, Set<String> selectors, Map<String, Object> options) {
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
                    selectorsArray[index++] = runtimeObjectModel.toString(selector);
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
                selectorsArray[index++] = runtimeObjectModel.toString(selector);
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
                selectorsArray[index++] = runtimeObjectModel.toString(selector);
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
    private void handleParameters(RuntimeObjectModel runtimeObjectModel, Map<String, Collection<String>> parameters, Map<String, Object>
            options) {
        if (options.containsKey(QUERY)) {
            Object queryOption = options.get(QUERY);
            parameters.clear();
            Map<String, Object> queryParameters = runtimeObjectModel.toMap(queryOption);
            addQueryParameters(runtimeObjectModel, parameters, queryParameters);
        }
        Object addQueryOption = options.get(ADD_QUERY);
        if (addQueryOption != null) {
            Map<String, Object> addParams = runtimeObjectModel.toMap(addQueryOption);
            addQueryParameters(runtimeObjectModel, parameters, addParams);
        }
        Object removeQueryOption = options.get(REMOVE_QUERY);
        if (removeQueryOption != null) {
            if (removeQueryOption instanceof String) {
                parameters.remove(removeQueryOption);
            } else if (removeQueryOption instanceof Object[]) {
                Object[] removeQueryParamArray = (Object[]) removeQueryOption;
                for (Object param : removeQueryParamArray) {
                    String paramString = runtimeObjectModel.toString(param);
                    if (paramString != null) {
                        parameters.remove(paramString);
                    }
                }
            }
        }
    }

    private void addQueryParameters(RuntimeObjectModel runtimeObjectModel, Map<String, Collection<String>> parameters, Map<String, Object>
            queryParameters) {
        for (Map.Entry<String, Object> entry : queryParameters.entrySet()) {
            Object entryValue = entry.getValue();
            if (runtimeObjectModel.isCollection(entryValue)) {
                Collection<Object> collection = runtimeObjectModel.toCollection(entryValue);
                Collection<String> values = new ArrayList<>(collection.size());
                for (Object o : collection) {
                    values.add(runtimeObjectModel.toString(o));
                }
                parameters.put(entry.getKey(), values);
            } else {
                Collection<String> values = new ArrayList<>(1);
                values.add(runtimeObjectModel.toString(entryValue));
                parameters.put(entry.getKey(), values);
            }
        }
    }

    public static class PathInfo {

        private URI uri;
        private String path;
        private Set<String> selectors;
        private String selectorString;
        private String extension;
        private String suffix;
        private Map<String, Collection<String>> parameters = new LinkedHashMap<>();

        /**
         * Creates a {@code PathInfo} object based on a request path.
         *
         * @param path the full normalized path (no '.', '..', or double slashes8) of the request, including the query parameters
         * @throws NullPointerException if the supplied {@code path} is null
         */
        public PathInfo(String path) {
            if (path == null) {
                throw new NullPointerException("The path parameter cannot be null.");
            }
            try {
                uri = new URI(path);
            } catch (URISyntaxException e) {
                throw new SightlyException("The provided path does not represent a valid URI: " + path);
            }
            selectors = new LinkedHashSet<>();
            String processingPath = path;
            if (uri.getPath() != null) {
                processingPath = uri.getPath();
            }
            int lastDot = processingPath.lastIndexOf('.');
            if (lastDot > -1) {
                String afterLastDot = processingPath.substring(lastDot + 1);
                String[] parts = afterLastDot.split("/");
                extension = parts[0];
                if (parts.length > 1) {
                    // we have a suffix
                    StringBuilder suffixSB = new StringBuilder();
                    for (int i = 1; i < parts.length; i++) {
                        suffixSB.append("/").append(parts[i]);
                    }
                    int hashIndex = suffixSB.indexOf("#");
                    if (hashIndex > -1) {
                        suffix = suffixSB.substring(0, hashIndex);
                    } else {
                        suffix = suffixSB.toString();
                    }
                }
            }
            int firstDot = processingPath.indexOf('.');
            if (firstDot < lastDot) {
                selectorString = processingPath.substring(firstDot + 1, lastDot);
                String[] selectorsArray = selectorString.split("\\.");
                selectors.addAll(Arrays.asList(selectorsArray));
            }
            int pathLength = processingPath.length() - (selectorString == null ? 0 : selectorString.length() + 1) - (extension == null ? 0:
                    extension.length() + 1) - (suffix == null ? 0 : suffix.length());
            if (pathLength == processingPath.length()) {
                this.path = processingPath;
            } else {
                this.path = processingPath.substring(0, pathLength);
            }
            String query = uri.getQuery();
            if (StringUtils.isNotEmpty(query)) {
                String[] keyValuePairs = query.split("&");
                for (int i = 0; i < keyValuePairs.length; i++) {
                    String[] pair = keyValuePairs[i].split("=");
                    if (pair.length == 2) {
                        String param = pair[0];
                        String value = pair[1];
                        Collection<String> values = parameters.get(param);
                        if (values == null) {
                            values = new ArrayList<>();
                            parameters.put(param, values);
                        }
                        values.add(value);
                    }
                }
            }
        }

        /**
         * Returns the scheme of this path if the path corresponds to a URI and if the URI provides scheme information.
         *
         * @return the scheme or {@code null} if the path does not contain a scheme
         */
        public String getScheme() {
            return uri.getScheme();
        }

        /**
         * Returns the path separator ("//") if the path defines an absolute URI.
         *
         * @return the path separator if the path is an absolute URI, {@code null} otherwise
         */
        public String getBeginPathSeparator() {
            if (uri.isAbsolute()) {
                return "//";
            }
            return null;
        }

        /**
         * Returns the host part of the path, if the path defines a URI.
         *
         * @return the host if the path defines a URI, {@code null} otherwise
         */
        public String getHost() {
            return uri.getHost();
        }

        /**
         * Returns the port if the path defines a URI and if it contains port information.
         *
         * @return the port or -1 if no port is defined
         */
        public int getPort() {
            return uri.getPort();
        }

        /**
         * Returns the path from which <i>{@code this}</i> object was built.
         *
         * @return the original path
         */
        public String getFullPath() {
            return uri.toString();
        }

        /**
         * Returns the path identifying the resource, without any selectors, extension or query parameters.
         *
         * @return the path of the resource
         */
        public String getPath() {
            return path;
        }

        /**
         * Returns the selectors set.
         *
         * @return the selectors set; if there are no selectors the set will be empty
         */
        public Set<String> getSelectors() {
            return selectors;
        }

        /**
         * Returns the extension.
         *
         * @return the extension, if one exists, otherwise {@code null}
         */
        public String getExtension() {
            return extension;
        }

        /**
         * Returns the selector string.
         *
         * @return the selector string, if the path has selectors, otherwise {@code null}
         */
        public String getSelectorString() {
            return selectorString;
        }

        /**
         * Returns the suffix appended to the path. The suffix represents the path segment between the path's extension and the path's fragment.
         *
         * @return the suffix if the path contains one, {@code null} otherwise
         */
        public String getSuffix() {
            return suffix;
        }

        /**
         * Returns the fragment is this path defines a URI and it contains a fragment.
         *
         * @return the fragment, or {@code null} if one doesn't exist
         */
        public String getFragment() {
            return uri.getFragment();
        }

        /**
         * Returns the URI parameters if the provided path defines a URI.
         * @return the parameters map; can be empty if there are no parameters of if the path doesn't identify a URI
         */
        public Map<String, Collection<String>> getParameters() {
            return parameters;
        }
    }

}
