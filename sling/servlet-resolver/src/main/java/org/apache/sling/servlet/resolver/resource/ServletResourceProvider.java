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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.osgi.commons.OsgiUtil;
import org.apache.sling.servlet.resolver.helper.PathSupport;
import org.osgi.framework.ServiceReference;

public class ServletResourceProvider implements ResourceProvider {

    private Map<String, ServletResource> servletMap;

    public static ServletResourceProvider create(ServiceReference ref,
            Servlet servlet, String servletRoot) {

        // check whether explicit paths are set
        String[] paths = OsgiUtil.toStringArray(ref.getProperty(SLING_SERVLET_PATHS));
        if (paths != null && paths.length > 0) {
            for (int i=0; i < paths.length; i++) {
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
            selectors = new String[]{ null };
        }
        
        // we have types and expect extensions and/or methods
        String[] extensions = OsgiUtil.toStringArray(ref.getProperty(SLING_SERVLET_EXTENSIONS));
        String[] methods = OsgiUtil.toStringArray(ref.getProperty(SLING_SERVLET_METHODS));
//        if ((extensions == null || extensions.length == 0)
//            && (methods == null || methods.length == 0)) {
//            // TODO: should log, why we ignore this servlet
//            return null;
//        }

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
                    pathList.add(selPath.substring(0, selPath.length()-1));
                }
            }
        }

        paths = pathList.toArray(new String[pathList.size()]);
        return new ServletResourceProvider(paths, servlet);
    }

    private ServletResourceProvider(String[] paths, Servlet servlet) {
        servletMap = new HashMap<String, ServletResource>();
        for (String path : paths) {
            ServletResource res = new ServletResource(this, servlet, path);
            servletMap.put(path, res);
        }
    }

    public Resource getResource(HttpServletRequest request, String path) {
        return getResource(path);
    }

    public Resource getResource(String path) {
        return servletMap.get(path);
    }

    public Iterator<Resource> listChildren(Resource parent) {
        // the resource provider only has exact paths no children
        return null;
    }

    public String[] getSerlvetPaths() {
        return servletMap.keySet().toArray(
            new String[servletMap.keySet().size()]);
    }
}
