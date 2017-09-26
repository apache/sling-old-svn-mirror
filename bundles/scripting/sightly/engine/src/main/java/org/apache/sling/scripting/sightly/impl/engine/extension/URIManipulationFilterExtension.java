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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.scripting.sightly.SightlyException;
import org.apache.sling.scripting.sightly.compiler.RuntimeFunction;
import org.apache.sling.scripting.sightly.extension.RuntimeExtension;
import org.apache.sling.scripting.sightly.render.RenderContext;
import org.apache.sling.scripting.sightly.render.RuntimeObjectModel;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        service = RuntimeExtension.class,
        property = {
                RuntimeExtension.NAME + "=" + RuntimeFunction.URI_MANIPULATION
        }
)
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

    private static final Logger LOG = LoggerFactory.getLogger(URIManipulationFilterExtension.class);

    @Override
    @SuppressWarnings("unchecked")
    public Object call(RenderContext renderContext, Object... arguments) {
        ExtensionUtils.checkArgumentCount(RuntimeFunction.URI_MANIPULATION, arguments, 2);
        RuntimeObjectModel runtimeObjectModel = renderContext.getObjectModel();
        String uriString = runtimeObjectModel.toString(arguments[0]);
        Map<String, Object> options = runtimeObjectModel.toMap(arguments[1]);

        try {
            URI originalUri = new URI(uriString);
            // read in https://docs.oracle.com/javase/7/docs/api/java/net/URI.html in section "Identities"
            // which constructors to use for which use case
            final URI transformedUri;
            final String scheme = getOption(SCHEME, options, originalUri.getScheme(), true);
            final String fragment = getOption(FRAGMENT, options, originalUri.getFragment(), false);

            // first check which type of URI
            if (originalUri.isOpaque()) {
                // only scheme and fragment is relevant
                transformedUri = new URI(scheme, originalUri.getSchemeSpecificPart(), fragment);
                return transformedUri.toString();
            } else {
                // only server-based authorities are supported
                try {
                    originalUri = originalUri.parseServerAuthority();
                } catch (URISyntaxException e) {
                    LOG.warn("Only server-based authorities are supported for non-opaque URLs");
                    throw e;
                }
                final String host = getOption(DOMAIN, options, originalUri.getHost(), true);
                final String path = getPath(runtimeObjectModel, originalUri.getPath(), options, scheme != null || host != null);
                final String escapedQuery = getEscapedQuery(runtimeObjectModel, originalUri.getRawQuery(), options);

                // the URI constructor will escape the % in the query part again, we must revert that
                transformedUri = new URI(scheme, originalUri.getUserInfo(), host, originalUri.getPort(), path, escapedQuery,
                        fragment);
                return unescapePercentInQuery(transformedUri.toString());
            }
        } catch (URISyntaxException e) {
            LOG.warn("Cannot manipulate invalid URI '{}'", uriString, e);
        }
        return uriString;
    }

    /**
     * Decodes the encoding of the percent character itself within the given uri's query i.e. reverts the conversion of {@code %} to
     * {@code %25}.
     *
     * @return the uri with the query partly decoded ({@code %25} decoded to {@code %})
     * @see <a href="https://blog.stackhunter.com/2014/03/31/encode-special-characters-java-net-uri/">How to Encode Special Characters in
     * Java’s URI Class</a>
     * @see <a href="https://stackoverflow.com/q/19917079/5155923">java.net.URI and percent in query parameter value</a>
     */
    static String unescapePercentInQuery(String uri) {
        String[] parts = uri.split("\\?", 2);
        if (parts.length != 2) {
            return uri;
        }
        // separate fragment
        String[] suffixParts = parts[1].split("#", 2);
        final String suffix;
        final String query;
        if (suffixParts.length == 2) {
            query = suffixParts[0];
            suffix = "#" + suffixParts[1];
        } else {
            query = parts[1];
            suffix = "";
        }
        return parts[0] + "?" + query.replaceAll("%25", "%") + suffix;
    }

    /**
     * Returns a value from a map.
     *
     * @param key               the option name
     * @param options           the options map
     * @param defaultValue      the default value to return
     * @param useDefaultIfEmpty if set to {@code true}, the {@code defaultValue} will be returned when the map contains the required
     *                          option but its value is the empty string or null
     * @return either the value from the map for entry with name or {@code defaultValue}; in case {@code useDefaultIfEmpty} is set to
     * {@code false}, returns null in case the entry present in the map but has an empty string or null value
     */
    private String getOption(String key, Map<String, Object> options, String defaultValue, boolean useDefaultIfEmpty) {
        String value = (String) options.get(key);
        if (StringUtils.isNotEmpty(value)) {
            return value;
        }
        if (options.containsKey(key)) {
            if (useDefaultIfEmpty) {
                return defaultValue;
            } else {
                return null;
            }
        }
        return defaultValue;
    }

    static String concatenateWithSlashes(String... pathParts) {
        StringBuilder sb = new StringBuilder();
        for (String pathPart : pathParts) {
            if (StringUtils.isNotBlank(pathPart)) {
                if (sb.length() > 0 && !pathPart.startsWith("/") && !sb.toString().endsWith("/")) {
                    sb.append("/");
                }
                if (sb.toString().endsWith("/") && pathPart.startsWith("/")) {
                    sb.append(pathPart.substring(1));
                } else {
                    sb.append(pathPart);
                }
            }
        }
        return sb.toString();
    }

    private String getPath(RuntimeObjectModel runtimeObjectModel, String originalPath, Map<String, Object> options, boolean isAbsolute) {
        ModifiableRequestPathInfo requestPathInfo = new ModifiableRequestPathInfo(originalPath);
        final String prependPath = getOption(PREPEND_PATH, options, StringUtils.EMPTY, true);
        final String path =
                getOption(PATH, options, requestPathInfo.getResourcePath(), true); // empty path option should not remove existing path!
        final String appendPath = getOption(APPEND_PATH, options, StringUtils.EMPTY, true);
        if (!options.containsKey(PATH) && StringUtils.isEmpty(path)) {
            // no not prepend/append if path is neither set initially nor through option
            LOG.debug("Do not modify path because original path was empty and not set through an option either!");
            // dealing with selectors, extension or suffix is not allowed then either
            return requestPathInfo.toString();
        } else {
            String newPath = concatenateWithSlashes(prependPath, path, appendPath);

            // do we need to make the path absolute?
            if (isAbsolute && !newPath.startsWith("/")) {
                newPath = '/' + newPath;
            }
            // modify resource path
            requestPathInfo.setResourcePath(newPath);
        }

        handleSelectors(runtimeObjectModel, requestPathInfo, options);
        // handle extension
        String extension = getOption(EXTENSION, options, requestPathInfo.getExtension(), false);
        requestPathInfo.setExtension(extension);

        // modify suffix!
        String prependSuffix = getOption(PREPEND_SUFFIX, options, StringUtils.EMPTY, true);
        // remove suffix if option is empty
        String suffix = getOption(SUFFIX, options, requestPathInfo.getSuffix(), false);
        String appendSuffix = getOption(APPEND_SUFFIX, options, StringUtils.EMPTY, true);

        String newSuffix = concatenateWithSlashes(prependSuffix, suffix, appendSuffix);
        if (StringUtils.isNotEmpty(newSuffix)) {
            // make sure it starts with a slash
            if (!newSuffix.startsWith("/")) {
                newSuffix = '/' + newSuffix;
            }
        }
        requestPathInfo.setSuffix(newSuffix);
        return requestPathInfo.toString();
    }

    private String getEscapedQuery(RuntimeObjectModel runtimeObjectModel, String originalQuery, Map<String, Object> options) {
        // parse parameters
        Map<String, Collection<String>> parameters = new LinkedHashMap<>();
        if (StringUtils.isNotEmpty(originalQuery)) {
            String[] keyValuePairs = originalQuery.split("&");
            for (String keyValuePair : keyValuePairs) {
                String[] pair = keyValuePair.split("=");
                if (pair.length == 2) {
                    String param;
                    try {
                        param = URLDecoder.decode(pair[0], StandardCharsets.UTF_8.name());
                    } catch (UnsupportedEncodingException e) {
                        LOG.warn("Could not decode parameter key '{}'", pair[0], e);
                        continue;
                    }
                    String value;
                    try {
                        value = URLDecoder.decode(pair[1], StandardCharsets.UTF_8.name());
                    } catch (UnsupportedEncodingException e) {
                        LOG.warn("Could not decode parameter value of parameter '{}': '{}'", param, pair[1], e);
                        continue;
                    }
                    Collection<String> values = parameters.get(param);
                    if (values == null) {
                        values = new ArrayList<>();
                        parameters.put(param, values);
                    }
                    values.add(value);
                }
            }
        }

        if (handleParameters(runtimeObjectModel, parameters, options)) {
            if (!parameters.isEmpty()) {
                try {
                    StringBuilder sb = new StringBuilder();
                    for (Map.Entry<String, Collection<String>> entry : parameters.entrySet()) {
                        for (String value : entry.getValue()) {
                            sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.name())).append("=")
                                    .append(URLEncoder.encode(value, StandardCharsets.UTF_8.name())).append("&");
                        }
                    }
                    // delete the last &
                    sb.deleteCharAt(sb.length() - 1);
                    return sb.toString();
                } catch (UnsupportedEncodingException e) {
                    throw new SightlyException("Could not encode the parameter values/keys", e);
                }
            } else {
                return null;
            }
        }
        return originalQuery;
    }

    private void handleSelectors(RuntimeObjectModel runtimeObjectModel, ModifiableRequestPathInfo requestPathInfo,
                                 Map<String, Object> options) {
        if (options.containsKey(SELECTORS)) {
            Object selectorsOption = options.get(SELECTORS);
            if (selectorsOption == null) {
                // we want to remove all selectors
                requestPathInfo.clearSelectors();
            } else if (selectorsOption instanceof String) {
                String selectorString = (String) selectorsOption;
                String[] selectorsArray = selectorString.split("\\.");
                requestPathInfo.replaceSelectors(selectorsArray);
            } else if (selectorsOption instanceof Object[]) {
                Object[] selectorsURIArray = (Object[]) selectorsOption;
                String[] selectorsArray = new String[selectorsURIArray.length];
                int index = 0;
                for (Object selector : selectorsURIArray) {
                    selectorsArray[index++] = runtimeObjectModel.toString(selector);
                }
                requestPathInfo.replaceSelectors(selectorsArray);
            }
        }
        Object addSelectorsOption = options.get(ADD_SELECTORS);
        if (addSelectorsOption instanceof String) {
            String selectorString = (String) addSelectorsOption;
            String[] selectorsArray = selectorString.split("\\.");
            requestPathInfo.addSelectors(selectorsArray);
        } else if (addSelectorsOption instanceof Object[]) {
            Object[] selectorsURIArray = (Object[]) addSelectorsOption;
            String[] selectorsArray = new String[selectorsURIArray.length];
            int index = 0;
            for (Object selector : selectorsURIArray) {
                selectorsArray[index++] = runtimeObjectModel.toString(selector);
            }
            requestPathInfo.addSelectors(selectorsArray);
        }
        Object removeSelectorsOption = options.get(REMOVE_SELECTORS);
        if (removeSelectorsOption instanceof String) {
            String selectorString = (String) removeSelectorsOption;
            String[] selectorsArray = selectorString.split("\\.");
            requestPathInfo.removeSelectors(selectorsArray);
        } else if (removeSelectorsOption instanceof Object[]) {
            Object[] selectorsURIArray = (Object[]) removeSelectorsOption;
            String[] selectorsArray = new String[selectorsURIArray.length];
            int index = 0;
            for (Object selector : selectorsURIArray) {
                selectorsArray[index++] = runtimeObjectModel.toString(selector);
            }
            requestPathInfo.removeSelectors(selectorsArray);
        }
    }

    @SuppressWarnings("unchecked")
    private boolean handleParameters(RuntimeObjectModel runtimeObjectModel, Map<String, Collection<String>> parameters,
                                     Map<String, Object> options) {
        boolean hasModifiedParameters = false;
        if (options.containsKey(QUERY)) {
            Object queryOption = options.get(QUERY);
            parameters.clear();
            Map<String, Object> queryParameters = runtimeObjectModel.toMap(queryOption);
            addQueryParameters(runtimeObjectModel, parameters, queryParameters);
            hasModifiedParameters = true;
        }
        Object addQueryOption = options.get(ADD_QUERY);
        if (addQueryOption != null) {
            Map<String, Object> addParams = runtimeObjectModel.toMap(addQueryOption);
            addQueryParameters(runtimeObjectModel, parameters, addParams);
            hasModifiedParameters = true;
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
            hasModifiedParameters = true;
        }
        return hasModifiedParameters;
    }

    private void addQueryParameters(RuntimeObjectModel runtimeObjectModel, Map<String, Collection<String>> parameters,
                                    Map<String, Object> queryParameters) {
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

    static class ModifiableRequestPathInfo implements RequestPathInfo {

        private String resourcePath;
        private List<String> selectors;
        private String extension;
        private String suffix;

        /**
         * Creates the {@link RequestPathInfo} object based on a request path only.
         * This utility does not take this into account any underlying repository structure and
         * just uses the first dot to split resource path from the rest!
         * {@code org.apache.sling.servlets.resolver.internal.DecomposedUrl} uses the same logic!
         *
         * @param path the full normalized path (no '.', '..', or double slashes8) of the request, including the query parameters
         * @throws NullPointerException if the supplied {@code path} is null
         */
        ModifiableRequestPathInfo(String path) {
            if (path == null) {
                throw new NullPointerException("The path parameter cannot be null.");
            }
            selectors = new LinkedList<>();
            // get relevant parts
            int firstDot = path.indexOf('.');

            // the extra path in the request URI
            final String pathToParse;
            if (firstDot >= 0) {
                pathToParse = path.substring(firstDot);
                resourcePath = path.substring(0, firstDot);
            } else {
                pathToParse = "";
                resourcePath = path;
            }

            // use logic from org.apache.sling.engine.impl.request.SlingRequestPathInfo
            // separate selectors/ext from the suffix
            int firstSlash = pathToParse.indexOf('/');
            String pathToSplit;
            if (firstSlash < 0) {
                pathToSplit = pathToParse;
                suffix = null;
            } else {
                pathToSplit = pathToParse.substring(0, firstSlash);
                suffix = pathToParse.substring(firstSlash);
            }

            int lastDot = pathToSplit.lastIndexOf('.');

            if (lastDot > 1) {
                // no selectors if splitting would give an empty array
                String tmpSel = pathToSplit.substring(1, lastDot);
                selectors.addAll(Arrays.asList(tmpSel.split("\\.")));

            }

            // extension only if lastDot is not trailing
            extension = (lastDot + 1 < pathToSplit.length())
                    ? pathToSplit.substring(lastDot + 1) : null;
        }

        void setExtension(String extension) {
            this.extension = extension;
        }

        void setSuffix(String suffix) {
            this.suffix = suffix;
        }

        @Override
        @Nonnull
        public String getResourcePath() {
            return resourcePath;
        }

        void setResourcePath(String path) {
            this.resourcePath = path;
        }

        /**
         * Returns the extension.
         *
         * @return the extension, if one exists, otherwise {@code null}
         */
        @Override
        public String getExtension() {
            return extension;
        }

        /**
         * Returns the selector string.
         *
         * @return the selector string, if the path has selectors, otherwise {@code null}
         */
        @Override
        public String getSelectorString() {
            throw new UnsupportedOperationException();
        }

        /**
         * Returns the suffix appended to the path. The suffix represents the path segment between the path's extension and the path's
         * fragment.
         *
         * @return the suffix if the path contains one, {@code null} otherwise
         */
        @Override
        public String getSuffix() {
            return suffix;
        }

        @Override
        @Nonnull
        public String[] getSelectors() {
            return selectors.toArray(new String[]{});
        }

        @Override
        public Resource getSuffixResource() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
            // resourcePath + selectors + extension + suffix
            StringBuilder sb = new StringBuilder(getResourcePath());
            if (getSelectors().length > 0) {
                for (String selector : selectors) {
                    if (StringUtils.isNotBlank(selector) && !selector.contains(" ")) {
                        // make sure not to append empty or invalid selectors
                        sb.append(".").append(selector);
                    }
                }
            }
            if (StringUtils.isNotEmpty(getExtension())) {
                sb.append('.').append(getExtension());
            }
            if (StringUtils.isNotEmpty(getSuffix())) {
                sb.append(getSuffix());
            }
            return sb.toString();
        }

        void replaceSelectors(String[] selectorsArray) {
            selectors.clear();
            selectors.addAll(Arrays.asList(selectorsArray));
        }

        void addSelectors(String[] selectorsArray) {
            selectors.addAll(Arrays.asList(selectorsArray));
        }

        void removeSelectors(String[] selectorsArray) {
            selectors.removeAll(Arrays.asList(selectorsArray));
        }

        void clearSelectors() {
            selectors.clear();
        }
    }

}
