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

import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

import javax.servlet.Servlet;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;

public class ServletResourceProvider extends ResourceProvider<Object> {

    private Servlet servlet;

    private Set<String> resourcePaths;

    ServletResourceProvider(Set<String> resourcePaths) {
        this.resourcePaths = resourcePaths;
    }

    public void setServlet(Servlet servlet) {
        this.servlet = servlet;
    }

    @Override
    public Resource getResource(final ResolveContext<Object> ctx, String path, ResourceContext resourceContext, Resource parent) {
        // only return a resource if the servlet has been assigned
        if (servlet != null && resourcePaths.contains(path)) {
            return new ServletResource(ctx.getResourceResolver(), servlet, path);
        }

        final ResourceProvider parentProvider = ctx.getParentResourceProvider();
        if ( parentProvider != null ) {
            final Resource useParent = (parent instanceof ServletResource ? null : parent);
            return parentProvider.getResource(ctx.getParentResolveContext(), path, resourceContext, useParent);
        }
        return null;
    }

    @Override
    public Iterator<Resource> listChildren(ResolveContext<Object> ctx, Resource parent) {
        final ResourceProvider parentProvider = ctx.getParentResourceProvider();
        if ( parentProvider != null ) {
            return parentProvider.listChildren(ctx.getParentResolveContext(), parent);
        }
        return null;
    }

    Servlet getServlet() {
        return servlet;
    }

    Iterator<String> getServletPathIterator() {
        return resourcePaths.iterator();
    }

    public String[] getServletPaths() {
        return resourcePaths.toArray(new String[resourcePaths.size()]);
    }

    /** Return suitable info for logging */
    @Override
    public String toString() {
        return getClass().getSimpleName() + ": servlet="
            + servlet.getClass().getName() + ", paths="
            + Arrays.toString(getServletPaths());
    }
}
