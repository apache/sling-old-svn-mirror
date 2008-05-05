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
package org.apache.sling.servlet.resolver.resource;

import static org.apache.sling.servlet.resolver.ServletResolverConstants.SLING_SERVLET_EXTENSIONS;
import static org.apache.sling.servlet.resolver.ServletResolverConstants.SLING_SERVLET_METHODS;
import static org.apache.sling.servlet.resolver.ServletResolverConstants.SLING_SERVLET_PATHS;
import static org.apache.sling.servlet.resolver.ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES;
import static org.apache.sling.servlet.resolver.ServletResolverConstants.SLING_SERVLET_SELECTORS;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.jcr.resource.JcrResourceUtil;
import org.osgi.framework.ServiceReference;

public class ServletResourceProvider implements ResourceProvider {

    public static final String SERVLET_PATH_EXTENSION = ".servlet";
    
    private final Servlet servlet;

    private Set<String> resourcePaths;

    public static ServletResourceProvider create(ServiceReference ref,
            Servlet servlet, String servletRoot) {

        // check whether explicit paths are set
        String[] paths = OsgiUtil.toStringArray(ref.getProperty(SLING_SERVLET_PATHS));
        if (paths != null && paths.length > 0) {
          Set<String> pathSet = new HashSet<String>();
          for (String path : paths) {
                if (!path.startsWith("/")) {
                    path = servletRoot.concat(path);
                }

                // add the unmodified path
                pathSet.add(path);

                // ensure we have another entry which has the .servlet ext.
                pathSet.add(ensureServletNameExtension(path));
            }
            return new ServletResourceProvider(pathSet, servlet);
        }

        // now, we fall back to resource types, extensions and methods
        String[] types = OsgiUtil.toStringArray(ref.getProperty(SLING_SERVLET_RESOURCE_TYPES));
        if (types == null || types.length == 0) {
            // TODO: should log, why we ignore this servlet
            return null;
        }

        // check for selectors
        String[] selectors = OsgiUtil.toStringArray(ref.getProperty(SLING_SERVLET_SELECTORS));
        if (selectors == null) {
            selectors = new String[] { null };
        }

        // we have types and expect extensions and/or methods
        String[] extensions = OsgiUtil.toStringArray(ref.getProperty(SLING_SERVLET_EXTENSIONS));
        String[] methods = OsgiUtil.toStringArray(ref.getProperty(SLING_SERVLET_METHODS));
        // if ((extensions == null || extensions.length == 0)
        // && (methods == null || methods.length == 0)) {
        // // TODO: should log, why we ignore this servlet
        // return null;
        // }

        Set<String> pathSet = new HashSet<String>();
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

        return new ServletResourceProvider(pathSet, servlet);
    }

    static String ensureServletNameExtension(String servletPath) {
        if (servletPath.endsWith(SERVLET_PATH_EXTENSION)) {
            return servletPath;
        }
        
        return servletPath.concat(SERVLET_PATH_EXTENSION);
    }
    
    private ServletResourceProvider(Set<String> resourcePaths, Servlet servlet) {
        this.servlet = servlet;
        this.resourcePaths = resourcePaths;
    }

    public Resource getResource(ResourceResolver resourceResolver,
            HttpServletRequest request, String path) {
        return getResource(resourceResolver, path);
    }

    public Resource getResource(ResourceResolver resourceResolver, String path) {
        if (resourcePaths.contains(path)) {
            return new ServletResource(resourceResolver, servlet, path);
        }

        return null;
    }

    public Iterator<Resource> listChildren(final Resource parent) {
        return new ServletResourceIterator(this, parent);
    }

    Servlet getServlet() {
        return servlet;
    }

    Iterator<String> getServletPathIterator() {
        return resourcePaths.iterator();
    }

    public String[] getSerlvetPaths() {
        return resourcePaths.toArray(new String[resourcePaths.size()]);
    }

}
