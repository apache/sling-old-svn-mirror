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

import javax.servlet.Servlet;

import org.apache.sling.adapter.SlingAdaptable;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceProvider;

public class ServletResource extends SlingAdaptable implements Resource {

    private final ResourceProvider resourceProvider;

    private final Servlet servlet;

    private final String path;

    private final ResourceMetadata metadata;

    public ServletResource(ResourceProvider resourceProvider, Servlet servlet,
            String path) {
        this.resourceProvider = resourceProvider;
        this.servlet = servlet;
        this.path = path;

        this.metadata = new ResourceMetadata();
        metadata.setResolutionPath(path);
    }

    public ResourceMetadata getResourceMetadata() {
        return metadata;
    }

    public ResourceProvider getResourceProvider() {
        return resourceProvider;
    }

    public String getResourceType() {
        // the resource type of a servlet is the servlet's path
        return path;
    }

    public String getPath() {
        return path;
    }

    @SuppressWarnings("unchecked")
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        if (type == Servlet.class) {
            return (AdapterType) servlet; // unchecked cast
        }

        return super.adaptTo(type);
    }

    public String toString() {
        // prepare the servlet name
        String servletName = null;
        if (servlet.getServletConfig() != null) {
            servletName = servlet.getServletConfig().getServletName();
        }
        if (servletName == null) {
            servletName = servlet.getServletInfo();
        }
        if (servletName == null) {
            servletName = servlet.getClass().getName();
        }
        
        return getClass().getSimpleName() + ", servlet=" + servletName
            + ", path=" + getPath();
    }

}
