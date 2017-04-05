/*
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
 */
package org.apache.sling.api.request;

import java.util.HashMap;
import java.util.StringTokenizer;

/**
 * <code>RequestDispatcherOptions</code> are used in the
 * {@link org.apache.sling.api.SlingHttpServletRequest#getRequestDispatcher(org.apache.sling.api.resource.Resource, RequestDispatcherOptions)}
 * method, to give more control on some aspects of the include/forward
 * mechanism. Typical use cases include:
 * <ul>
 * <li> Forcing a resource type, to render a Resource in a specific way, like
 * for example <em>render myself in a suitable way for a navigation box</em>.
 * </li>
 * <li> Adding selectors when including a Resource, like for example <em>add
 *          a "teaser" selector to the request that I'm including here</em>.
 * </li>
 * </ul>
 * This class currently only inherits from Map, and defines some constants for
 * well-known options.
 */
public class RequestDispatcherOptions extends HashMap<String, String> {

    private static final long serialVersionUID = -9081782403304877746L;

    /**
     * When dispatching, use the value provided by this option as the resource
     * type, instead of the one defined by the
     * {@link org.apache.sling.api.resource.Resource}.
     */
    public static final String OPT_FORCE_RESOURCE_TYPE = "forceResourceType";

    /**
     * When dispatching, replace {@link RequestPathInfo} selectors by the value
     * provided by this option. If this value contains an empty string, all
     * original selectors are removed.
     */
    public static final String OPT_REPLACE_SELECTORS = "replaceSelectors";

    /**
     * When dispatching, add the value provided by this option to the
     * {@link RequestPathInfo} selectors.
     */
    public static final String OPT_ADD_SELECTORS = "addSelectors";

    /**
     * When dispatching, replace the {@link RequestPathInfo} suffix by the value
     * provided by this option
     */
    public static final String OPT_REPLACE_SUFFIX = "replaceSuffix";

    /**
     * Creates an instance with no options set.
     */
    public RequestDispatcherOptions() {
    }

    /**
     * Creates a new instances setting options by parsing the given
     * <code>options</code> string as follows:
     * <ul>
     * <li>If the string is empty or <code>null</code> no options are set.</li>
     * <li>If the string neither contains a comma nor an equals sign, the
     * string is assumed to be a resource type. Hence a
     * <code>RequestDispatcherOptions</code> object is created with the
     * {@link RequestDispatcherOptions#OPT_FORCE_RESOURCE_TYPE} field set to the
     * string.</li>
     * <li>Otherwise the string is assumed to be a comma separated list of name
     * value pairs where the equals sign is used to separate the name from its
     * value. Hence a <code>RequestDispatcherOptions</code> object is created
     * from the name value pair list.</li>
     * </ul>
     *
     * @param options The options to set.
     */
    public RequestDispatcherOptions(String options) {

        if (options != null && options.length() > 0) {
            if (options.indexOf(',') < 0 && options.indexOf('=') < 0) {
                setForceResourceType(options.trim());
            } else {
                final StringTokenizer tk = new StringTokenizer(options, ",");
                while (tk.hasMoreTokens()) {
                    String entry = tk.nextToken();
                    int equals = entry.indexOf('=');
                    if (equals > 0 && equals < entry.length() - 1) {
                        put(entry.substring(0, equals).trim(), entry.substring(
                            equals + 1).trim());
                    }
                }
            }
        }
    }

    /**
     * Sets the {@link #OPT_FORCE_RESOURCE_TYPE} option to the given
     * <code>resourceType</code> if not <code>null</code>.
     * @param resourceType the resource type
     */
    public void setForceResourceType(String resourceType) {
        if (resourceType != null) {
            put(OPT_FORCE_RESOURCE_TYPE, resourceType);
        }
    }

    /**
     * Returns the {@link #OPT_FORCE_RESOURCE_TYPE} option or <code>null</code>
     * if not set.
     * @return The resource type.
     */
    public String getForceResourceType() {
        return get(OPT_FORCE_RESOURCE_TYPE);
    }

    /**
     * Sets the {@link #OPT_ADD_SELECTORS} option to the given
     * <code>additionalSelectors</code> if not <code>null</code>.
     * @param additionalSelectors The add selectors
     */
    public void setAddSelectors(String additionalSelectors) {
        if (additionalSelectors != null) {
            put(OPT_ADD_SELECTORS, additionalSelectors);
        }
    }

    /**
     * Returns the {@link #OPT_ADD_SELECTORS} option or <code>null</code> if
     * not set.
     * @return The add selectors.
     */
    public String getAddSelectors() {
        return get(OPT_ADD_SELECTORS);
    }

    /**
     * Sets the {@link #OPT_REPLACE_SELECTORS} option to the given
     * <code>replaceSelectors</code> if not <code>null</code>.
     * If this value contains an empty string, all
     * original selectors are removed.
     * @param replaceSelectors The replace selectors.
     */
    public void setReplaceSelectors(String replaceSelectors) {
        if (replaceSelectors != null) {
            put(OPT_REPLACE_SELECTORS, replaceSelectors);
        }
    }

    /**
     * Returns the {@link #OPT_REPLACE_SELECTORS} option or <code>null</code>
     * if not set.
     * @return The replace selectors.
     */
    public String getReplaceSelectors() {
        return get(OPT_REPLACE_SELECTORS);
    }

    /**
     * Sets the {@link #OPT_REPLACE_SUFFIX} option to the given
     * <code>replaceSuffix</code> if not <code>null</code>.
     * @param replaceSuffix The replace suffix
     */
    public void setReplaceSuffix(String replaceSuffix) {
        if (replaceSuffix != null) {
            put(OPT_REPLACE_SUFFIX, replaceSuffix);
        }
    }

    /**
     * Returns the {@link #OPT_REPLACE_SUFFIX} option or <code>null</code> if
     * not set.
     * @return The replace suffix
     */
    public String getReplaceSuffix() {
        return get(OPT_REPLACE_SUFFIX);
    }
}
