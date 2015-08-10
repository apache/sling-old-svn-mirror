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
package org.apache.sling.servlets.resolver.internal.resource;

import static org.apache.sling.servlets.resolver.internal.ServletResolverConstants.SLING_SERVLET_EXTENSIONS;
import static org.apache.sling.servlets.resolver.internal.ServletResolverConstants.SLING_SERVLET_METHODS;
import static org.apache.sling.servlets.resolver.internal.ServletResolverConstants.SLING_SERVLET_PATHS;
import static org.apache.sling.servlets.resolver.internal.ServletResolverConstants.SLING_SERVLET_PREFIX;
import static org.apache.sling.servlets.resolver.internal.ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES;
import static org.apache.sling.servlets.resolver.internal.ServletResolverConstants.SLING_SERVLET_SELECTORS;

import java.util.HashSet;
import java.util.Set;

import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServletResourceProviderFactory {

    /**
     * The extension appended to servlets to register into the resource tree to
     * simplify handling in the resolution process (value is ".servlet").
     */
    public static final String SERVLET_PATH_EXTENSION = ".servlet";

    private static final String[] DEFAULT_SERVLET_METHODS = {
        HttpConstants.METHOD_GET, HttpConstants.METHOD_HEAD };

    private static final String ALL_METHODS = "*";

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * The root path to use for servlets registered with relative paths.
     */
    private final String servletRoot;

    /**
     * The index of the search path to be used as servlet root path
     */
    private final int servletRootIndex;

    /**
     * The search paths
     */
    private final String[] searchPaths;

    static String ensureServletNameExtension(String servletPath) {
        if (servletPath.endsWith(SERVLET_PATH_EXTENSION)) {
            return servletPath;
        }

        return servletPath.concat(SERVLET_PATH_EXTENSION);
    }

    /**
     * Constructor
     * @param servletRoot The default value for the servlet root
     */
    public ServletResourceProviderFactory(Object servletRoot, String[] paths) {
        this.searchPaths = paths;
        String value = servletRoot.toString();
        // check if servlet root specifies a number
        boolean isNumber = false;
        int index = -1;
        if ( servletRoot instanceof Number ) {
            isNumber = true;
            index = ((Number)servletRoot).intValue();
        } else {
            if (!value.startsWith("/") ) {
                try {
                    index = Integer.valueOf(value);
                    isNumber = true;
                } catch (NumberFormatException nfe) {
                    // ignore
                }
            }
        }
        if ( !isNumber ) {
            // ensure the root starts and ends with a slash
            if (!value.startsWith("/")) {
                value = "/" + value;
            }
            if (!value.endsWith("/")) {
                value += "/";
            }

            this.servletRoot = value;
            this.servletRootIndex = -1;
        } else {
            this.servletRoot = null;
            this.servletRootIndex = index;
        }
    }

    public ServletResourceProvider create(ServiceReference ref) {

        Set<String> pathSet = new HashSet<String>();

        // check whether explicit paths are set
        addByPath(pathSet, ref);

        // now, we handle resource types, extensions and methods
        addByType(pathSet, ref);

        if (pathSet.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug(
                    "create({}): ServiceReference has no registration settings, ignoring",
                    getServiceIdentifier(ref));
            }
            return null;
        }

        if (log.isDebugEnabled()) {
            log.debug("create({}): Registering servlet for paths {}",
                getServiceIdentifier(ref), pathSet);
        }

        return new ServletResourceProvider(pathSet);
    }

    /**
     * Get the mount prefix.
     */
    private String getPrefix(final ServiceReference ref) {
        Object value = ref.getProperty(SLING_SERVLET_PREFIX);
        if ( value == null ) {
            if ( this.servletRoot != null ) {
                return this.servletRoot;
            }
            value = this.servletRootIndex;
        }
        int index = -1;
        if ( value instanceof Number ) {
            index = ((Number)value).intValue();
        } else {
            String s = value.toString();
            if ( !s.startsWith("/") ) {
                boolean isNumber = false;
                try {
                    index = Integer.valueOf(s);
                    isNumber = true;
                } catch (NumberFormatException nfe) {
                    // ignore
                }
                if ( !isNumber ) {
                    if (log.isDebugEnabled()) {
                        log.debug("getPrefix({}): Configuration property is ignored {}",
                            getServiceIdentifier(ref), value);
                    }
                    if ( this.servletRoot != null ) {
                        return this.servletRoot;
                    }
                    index = this.servletRootIndex;
                }
            } else {
                return s;
            }
        }
        if ( index == -1 || index >= this.searchPaths.length ) {
            index = this.searchPaths.length - 1;
        }
        return this.searchPaths[index];
    }

    /**
     * Add a servlet by path.
     * @param pathSet
     * @param ref
     */
    private void addByPath(Set<String> pathSet, ServiceReference ref) {
        String[] paths = OsgiUtil.toStringArray(ref.getProperty(SLING_SERVLET_PATHS));
        if (paths != null && paths.length > 0) {
            for (String path : paths) {
                if (!path.startsWith("/")) {
                    path = getPrefix(ref).concat(path);
                }

                // add the unmodified path
                pathSet.add(path);

                // ensure we have another entry which has the .servlet ext.
                pathSet.add(ensureServletNameExtension(path));
            }
        }
    }

    /**
     * Add a servlet by type
     * @param pathSet
     * @param ref
     */
    private void addByType(Set<String> pathSet, ServiceReference ref) {
        String[] types = OsgiUtil.toStringArray(ref.getProperty(SLING_SERVLET_RESOURCE_TYPES));
        if (types == null || types.length == 0) {
            if (log.isDebugEnabled()) {
                log.debug("addByType({}): no resource types declared",
                    getServiceIdentifier(ref));
            }
            return;
        }

        // check for selectors
        String[] selectors = OsgiUtil.toStringArray(ref.getProperty(SLING_SERVLET_SELECTORS));
        if (selectors == null) {
            selectors = new String[] { null };
        }

        // we have types and expect extensions and/or methods
        String[] extensions = OsgiUtil.toStringArray(ref.getProperty(SLING_SERVLET_EXTENSIONS));

        // handle the methods property specially (SLING-430)
        String[] methods = OsgiUtil.toStringArray(ref.getProperty(SLING_SERVLET_METHODS));
        if (methods == null || methods.length == 0) {

            // SLING-512 only, set default methods if no extensions are declared
            if (extensions == null || extensions.length == 0) {
                if (log.isDebugEnabled()) {
                    log.debug(
                        "addByType({}): No methods declared, assuming GET/HEAD",
                        getServiceIdentifier(ref));
                }
                methods = DEFAULT_SERVLET_METHODS;
            }

        } else if (methods.length == 1 && ALL_METHODS.equals(methods[0])) {
            if (log.isDebugEnabled()) {
                log.debug("addByType({}): Assuming all methods for '*'",
                    getServiceIdentifier(ref));
            }
            methods = null;
        }

        for (String type : types) {

            // ensure namespace prefixes are converted to slashes
            type = ResourceUtil.resourceTypeToPath(type);

            // make absolute if relative
            if (!type.startsWith("/")) {
                type = this.getPrefix(ref) + type;
            }

            // ensure trailing slash for full path building
            if (!type.endsWith("/")) {
                type += "/";
            }

            // add entries for each selector combined with each ext and method
            for (String selector : selectors) {

                String selPath = type;
                if (selector != null && selector.length() > 0) {
                    selPath += selector.replace('.', '/') + ".";
                }

                boolean pathAdded = false;
                if (extensions != null) {
                    if (methods != null) {
                        // both methods and extensions declared
                        for (String ext : extensions) {
                            for (String method : methods) {
                                pathSet.add(selPath + ext + "." + method
                                    + SERVLET_PATH_EXTENSION);
                                pathAdded = true;
                            }
                        }
                    } else {
                        // only extensions declared
                        for (String ext : extensions) {
                            pathSet.add(selPath + ext + SERVLET_PATH_EXTENSION);
                            pathAdded = true;
                        }
                    }
                } else if (methods != null) {
                    // only methods declared
                    for (String method : methods) {
                        pathSet.add(selPath + method + SERVLET_PATH_EXTENSION);
                        pathAdded = true;
                    }
                }

                // if neither methods nore extensions were added
                if (!pathAdded) {
                    pathSet.add(selPath.substring(0, selPath.length() - 1)
                        + SERVLET_PATH_EXTENSION);
                }
            }
        }
    }

    private String getServiceIdentifier(ServiceReference ref) {
        Object id = ref.getProperty(ComponentConstants.COMPONENT_NAME);
        if (id != null) {
            return id.toString();
        }

        id = ref.getProperty(Constants.SERVICE_PID);
        if (id != null) {
            return id.toString();
        }

        // service.id is guaranteed to be set by the framework
        return ref.getProperty(Constants.SERVICE_ID).toString();
    }
}
