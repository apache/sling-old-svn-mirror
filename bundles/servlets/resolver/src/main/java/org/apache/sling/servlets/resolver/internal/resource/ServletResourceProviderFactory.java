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
import static org.apache.sling.servlets.resolver.internal.ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES;
import static org.apache.sling.servlets.resolver.internal.ServletResolverConstants.SLING_SERVLET_SELECTORS;

import java.util.HashSet;
import java.util.Set;

import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.jcr.resource.JcrResourceUtil;
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

    static String ensureServletNameExtension(String servletPath) {
        if (servletPath.endsWith(SERVLET_PATH_EXTENSION)) {
            return servletPath;
        }

        return servletPath.concat(SERVLET_PATH_EXTENSION);
    }

    public ServletResourceProviderFactory(String servletRoot) {

        // ensure the root starts and ends with a slash
        if (!servletRoot.startsWith("/")) {
            servletRoot = "/" + servletRoot;
        }
        if (!servletRoot.endsWith("/")) {
            servletRoot += "/";
        }

        this.servletRoot = servletRoot;
    }

    public ServletResourceProvider create(ServiceReference ref) {

        Set<String> pathSet = new HashSet<String>();

        // check whether explicit paths are set
        addByPath(pathSet, ref);

        // now, we handle resource types, extensions and methods
        addByType(pathSet, ref);

        if (pathSet.isEmpty()) {
            if (log.isInfoEnabled()) {
                log.info(
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

    private void addByPath(Set<String> pathSet, ServiceReference ref) {
        String[] paths = OsgiUtil.toStringArray(ref.getProperty(SLING_SERVLET_PATHS));
        if (paths != null && paths.length > 0) {
            for (String path : paths) {
                if (!path.startsWith("/")) {
                    path = servletRoot.concat(path);
                }

                // add the unmodified path
                pathSet.add(path);

                // ensure we have another entry which has the .servlet ext.
                pathSet.add(ensureServletNameExtension(path));
            }
        }
    }

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
                if (log.isInfoEnabled()) {
                    log.info(
                        "addByType({}): No methods declared, assuming GET/HEAD",
                        getServiceIdentifier(ref));
                }
                methods = DEFAULT_SERVLET_METHODS;
            }
            
        } else if (methods.length == 1 && ALL_METHODS.equals(methods[0])) {
            if (log.isInfoEnabled()) {
                log.info("addByType({}): Assuming all methods for '*'",
                    getServiceIdentifier(ref));
            }
            methods = null;
        }

        for (String type : types) {

            // ensure namespace prefixes are converted to slashes
            type = JcrResourceUtil.resourceTypeToPath(type);

            // make absolute if relative
            if (!type.startsWith("/")) {
                type = servletRoot + type;
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

                // create paths with extensions
                if (extensions != null) {
                    for (String ext : extensions) {
                        pathSet.add(selPath + ext + SERVLET_PATH_EXTENSION);
                        pathAdded = true;
                    }
                }

                // create paths with method names
                if (methods != null) {
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
