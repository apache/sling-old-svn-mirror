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
package org.apache.sling.core.impl.filter;

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.core.CoreConstants;
import org.apache.sling.core.impl.helper.ContentData;
import org.apache.sling.core.impl.helper.RequestData;
import org.apache.sling.core.servlets.DefaultServlet;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>ServletResolverFilter</code> TODO
 *
 * @scr.component immediate="true" label="%resolver.name"
 *                description="%resolver.description"
 * @scr.property name="service.description" value="Component Resolver Filter"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.property name="filter.scope" value="component" private="true"
 * @scr.property name="filter.order" value="-900" type="Integer" private="true"
 * @scr.service
 * @scr.reference name="Components" interface="javax.servlet.Servlet"
 *                cardinality="0..n" policy="dynamic"
 */
public class ServletResolverFilter extends ServletBindingFilter {

    /** default log */
    private static final Logger log = LoggerFactory.getLogger(ServletResolverFilter.class);

    /**
     * @scr.property values.1="/apps/components" values.2="/libs/components"
     *               label="%resolver.path.name"
     *               description="%resolver.path.description"
     */
    public static final String PROP_PATH = "path";

    private String[] path;

    private Map<String, Servlet> servlets = new HashMap<String, Servlet>();

    /**
     * @see org.apache.sling.core.component.ComponentFilter#doFilter(org.apache.sling.core.component.ComponentRequest,
     *      org.apache.sling.core.component.ComponentResponse,
     *      org.apache.sling.core.component.ComponentFilterChain)
     */
    public void doFilter(ServletRequest req, ServletResponse res,
            FilterChain filterChain) throws IOException, ServletException {

        SlingHttpServletRequest request = (SlingHttpServletRequest) req;
        SlingHttpServletResponse response = (SlingHttpServletResponse) res;

        ContentData contentData = RequestData.getRequestData(request).getContentData();

        if (contentData != null) {
            // 2.3 check Servlet
            Resource resource = contentData.getResource();
            String path = resource.getURI();
            Servlet servlet = resolveServlet(resource);
            if (servlet != null) {

                String compId = servlet.getServletConfig().getServletName();
                log.debug("Using Component {} for {}", compId, path);
                contentData.setServlet(servlet);
                filterChain.doFilter(request, response);
                return;

            }
        } else {
            log.error("ComponentResolver: No Content data in request {}",
                request.getRequestURI());
        }

        response.sendError(HttpServletResponse.SC_NOT_FOUND, "Cannot handle");
    }

    @Override
    protected void servletBound(ServiceReference reference, Servlet servlet) {
        Object typeObject = reference.getProperty(CoreConstants.SLING_RESOURCE_TYPES);
        String name = servlet.getServletConfig().getServletName();
        if (typeObject instanceof String) {
            log.debug("servletBound: Servlet {} handles type {}", name,
                typeObject);
            servlets.put((String) typeObject, servlet);
        } else if (typeObject instanceof String[]) {
            String[] types = (String[]) typeObject;
            for (String type : types) {
                log.debug("servletBound: Servlet {} handles type {}", name,
                    type);
                servlets.put(type, servlet);
            }
        }
    }

    @Override
    protected void servletUnbound(ServiceReference reference, Servlet servlet) {
        Object typeObject = reference.getProperty(CoreConstants.SLING_RESOURCE_TYPES);
        String name = servlet.getServletConfig().getServletName();
        if (typeObject instanceof String) {
            log.debug("servletBound: Servlet {} unregistered for {}", name,
                typeObject);
            servlets.remove(typeObject);
        } else if (typeObject instanceof String[]) {
            String[] types = (String[]) typeObject;
            for (String type : types) {
                log.debug("servletBound: Servlet {} unregistered for  {}", name,
                    type);
                servlets.remove(type);
            }
        }
    }

    private Servlet getServlet(String resourceType) {
        return servlets.get(resourceType);
    }

    private Servlet resolveServlet(Resource resource) {
        // get the component id
        String type = resource.getResourceType();

        // if none, try the path of the content as its component id,
        // this allows direct addressing of components
        if (type == null) {
            log.debug(
                "resolveComponent: Content {} has no componentid, trying path",
                resource.getURI());
            type = resource.getURI();
        }

        Servlet servlet = getServlet(type);
        if (servlet != null) {
            return servlet;
        }

        // if the component ID might be a realtive path name, check with path
        if (!type.startsWith("/")) {

            // apply any path prefixes
            if (this.path != null) {

                // might be a type name with namespace
                String relId = type.replace(':', '/');

                for (int i = 0; i < this.path.length; i++) {
                    String checkid = this.path[i] + relId;
                    servlet = getServlet(checkid);
                    if (servlet != null) {
                        return servlet;
                    }
                }

            }

        } else {
            // absolute path name: remove leading slash for further checks
            type = type.substring(1);
        }

        // if the path is mapped from a class name, convert the slashes
        // to dots to get a potentially fully qualified class name
        // again, this allows direct addressing of components
        type = type.replace('/', '.');
        servlet = this.getServlet(type);
        if (servlet != null) {
            return servlet;
        }

        // next we try a class name mapping convention of the content class
        type = resource.getObject().getClass().getName();
        servlet = this.getServlet(type);
        if (servlet != null) {
            return servlet;
        }

        // check whether we have Content suffix to remove
        if (type.endsWith("Content")) {
            type = type.substring(0, type.length() - "Content".length());
            servlet = this.getServlet(type);
            if (servlet != null) {
                return servlet;
            }
        }

        // add "Component" suffix and check again
        type += "Component";
        servlet = this.getServlet(type);
        if (servlet != null) {
            return servlet;
        }

        // use default component
        servlet = getServlet(DefaultServlet.class.getName());
        if (servlet != null) {
            return servlet;
        }

        // we exhausted all possibilities and finally fail
        log.error("resolveComponent: Could not resolve a component for {}",
            resource.getURI());
        return null;
    }

    // ---------- SCR Integration ----------------------------------------------

    @Override
    protected void activate(ComponentContext context) {
        super.activate(context);

        Dictionary props = context.getProperties();

        Object pathObject = props.get(PROP_PATH);
        if (pathObject instanceof String[]) {
            this.path = (String[]) pathObject;
            for (int i = 0; i < this.path.length; i++) {
                // ensure leading slash
                if (!this.path[i].startsWith("/")) {
                    this.path[i] = "/" + this.path[i];
                }
                // ensure trailing slash
                if (!this.path[i].endsWith("/")) {
                    this.path[i] += "/";
                }
            }
        } else {
            this.path = null;
        }
    }
}
