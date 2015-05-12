/**
 * ****************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ****************************************************************************
 */
package org.apache.sling.scripting.sightly.impl.utils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.scripting.sightly.SightlyException;

/**
 * The {@code PathInfo} class provides path processing methods useful for extracting the path of a request, the selectors applied to the
 * path, the extension and query parameters.
 */
public class PathInfo {

    private URI uri;
    private String path;
    private Set<String> selectors;
    private String selectorString;
    private String extension;
    private String suffix;
    private Map<String, Collection<String>> parameters = new LinkedHashMap<String, Collection<String>>();

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
        selectors = new LinkedHashSet<String>();
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
                        values = new ArrayList<String>();
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
