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

import java.util.Iterator;
import java.util.Set;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;

public class ServletResourceProvider implements ResourceProvider {

    private Servlet servlet;

    private Set<String> resourcePaths;

    ServletResourceProvider(Set<String> resourcePaths) {
        this.resourcePaths = resourcePaths;
    }

    public void setServlet(Servlet servlet) {
        this.servlet = servlet;
    }
    
    public Resource getResource(ResourceResolver resourceResolver,
            HttpServletRequest request, String path) {
        return getResource(resourceResolver, path);
    }

    public Resource getResource(ResourceResolver resourceResolver, String path) {
        // only return a resource if the servlet has been assigned
        if (servlet != null && resourcePaths.contains(path)) {
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
