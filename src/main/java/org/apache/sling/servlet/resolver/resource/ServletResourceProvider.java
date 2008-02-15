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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.osgi.commons.OsgiUtil;
import org.apache.sling.servlet.resolver.helper.PathSupport;
import org.osgi.framework.ServiceReference;

public class ServletResourceProvider implements ResourceProvider {

    private final Servlet servlet;

    private Set<String> resourcePaths;

    public static ServletResourceProvider create(ServiceReference ref,
            Servlet servlet, String servletRoot) {

        // check whether explicit paths are set
        String[] paths = OsgiUtil.toStringArray(ref.getProperty(SLING_SERVLET_PATHS));
        if (paths != null && paths.length > 0) {
            for (int i = 0; i < paths.length; i++) {
                if (!paths[i].startsWith("/")) {
                    paths[i] = servletRoot + paths[i];
                }

            }
            return new ServletResourceProvider(paths, servlet);
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

        List<String> pathList = new ArrayList<String>();
        for (String type : types) {

            // ensure namespace prefixes are converted to slashes
            type = PathSupport.toPath(type);

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
                    selPath += selector.replace('.', '/') + "/";
                }

                boolean pathAdded = false;

                // create paths with extensions
                if (extensions != null) {
                    for (String ext : extensions) {
                        pathList.add(selPath + ext);
                        pathAdded = true;
                    }
                }

                // create paths with method names
                if (methods != null) {
                    for (String method : methods) {
                        pathList.add(selPath + method);
                        pathAdded = true;
                    }
                }

                // if neither methods nore extensions were added
                if (!pathAdded) {
                    pathList.add(selPath.substring(0, selPath.length() - 1));
                }
            }
        }

        paths = pathList.toArray(new String[pathList.size()]);
        return new ServletResourceProvider(paths, servlet);
    }

    private ServletResourceProvider(String[] paths, Servlet servlet) {
        this.servlet = servlet;
        this.resourcePaths = new HashSet<String>(Arrays.asList(paths));
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
        return new Iterator<Resource>() {

            private Iterator<String> pathIter;

            private String parentPath;

            private Resource next;

            {
                pathIter = resourcePaths.iterator();
                parentPath = parent.getPath() + "/";
                next = seek();
            }

            public boolean hasNext() {
                return next != null;
            }

            public Resource next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                Resource result = next;
                next = seek();
                return result;
            }

            public void remove() {
                throw new UnsupportedOperationException("remove");
            }

            private Resource seek() {
                while (pathIter.hasNext()) {
                    String path = pathIter.next();
                    if (path.startsWith(parentPath)
                        && path.indexOf('/', parentPath.length()) < 0) {
                        return new ServletResource(
                            parent.getResourceResolver(), servlet, path);
                    }
                }

                return null;
            }

        };
    }

    public String[] getSerlvetPaths() {
        return resourcePaths.toArray(new String[resourcePaths.size()]);
    }
}
