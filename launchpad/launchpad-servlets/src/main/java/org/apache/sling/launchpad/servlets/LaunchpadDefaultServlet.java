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
package org.apache.sling.launchpad.servlets;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.launchpad.renderers.DefaultHtmlRendererServlet;
import org.apache.sling.launchpad.renderers.JsonRendererServlet;
import org.apache.sling.launchpad.renderers.PlainTextRendererServlet;
import org.apache.sling.launchpad.renderers.StreamRendererServlet;
import org.apache.sling.ujax.UjaxPostServlet;
import org.osgi.service.component.ComponentContext;

/**
 * Replaces the Sling default servlet (that bundle must NOT be
 * active) for the Launchpad, by delegating to our default 
 * renderers for GET requests, and to the ujax POST servlet 
 * for POST requests.
 *
 * @scr.service
 *  interface="javax.servlet.Servlet"
 *  
 * @scr.component 
 *  immediate="true" 
 *  
 * @scr.property 
 *  name="service.description"
 *  value="Launchpad Default Servlet"
 *  
 * @scr.property 
 *  name="service.vendor" 
 *  value="The Apache Software Foundation"
 *
 * Use this as the default servlet for Sling 
 * @scr.property 
 *  name="sling.servlet.resourceTypes" 
 *  value="sling/servlet/default"
 *  
 */
public class LaunchpadDefaultServlet extends SlingAllMethodsServlet {

    private Servlet postServlet;

    private Servlet defaultGetServlet;
    
    private Servlet streamServlet;

    private Servlet ujaxInfoServlet;

    private Map<String, Servlet> getServlets;

    /** @scr.property value="index.html" */
    private static final String PROP_ROOT_REDIRECT = "root.redirect";
    private String rootRedirect;
    
    @Override
    public void init(ServletConfig config) throws ServletException {

        super.init(config);

        // setup our "internal" servlets
        postServlet = new UjaxPostServlet();
        postServlet.init(config);

        ujaxInfoServlet = new UjaxInfoServlet();
        ujaxInfoServlet.init(config);

        defaultGetServlet = new PlainTextRendererServlet("text/plain");
        streamServlet = new StreamRendererServlet("text/plain", config);

        getServlets = new HashMap<String, Servlet>();
        getServlets.put("html", new DefaultHtmlRendererServlet("text/html"));
        getServlets.put("json", new JsonRendererServlet("application/json"));
        getServlets.put("txt", defaultGetServlet);
        getServlets.put("res", streamServlet);
    }

    protected void doGet(SlingHttpServletRequest request,
            SlingHttpServletResponse response) throws IOException,
            ServletException {
        final Resource resource = request.getResource();

        if(rootRedirect!= null && rootRedirect.length() > 0 && "/".equals(request.getPathInfo())) {
            response.sendRedirect(rootRedirect);
            return;
        }
        
        if (request.getPathInfo().startsWith(UjaxInfoServlet.PATH_PREFIX)) {
            ujaxInfoServlet.service(request, response);
            return;
        }

        // cannot handle the request for missing resources
        if (resource instanceof NonExistingResource) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND,
                "Resource not found at path " + resource.getPath());
            return;
        }

        // render using a servlet or binary streaming
        Servlet s = defaultGetServlet;
        final String ext = request.getRequestPathInfo().getExtension();
        if (ext != null && ext.length() > 0) {
            // if there is an extension, lookup our getServlets
            s = getServlets.get(ext);
        } else {
            // no extension means we're addressing a static file directly
            // check whether the resource adapts to a stream, spool then
            s = streamServlet;
        }

        // render using stream, s, or fail
        if (s != null) {
            s.service(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "No default renderer found for extension='" + ext + "'");
        }
    }

    protected void doPost(SlingHttpServletRequest request,
            SlingHttpServletResponse response) throws ServletException,
            IOException {
        postServlet.service(request, response);
    }
    
    protected void activate(ComponentContext componentContext) throws Exception {
        rootRedirect = (String)componentContext.getProperties().get(PROP_ROOT_REDIRECT);
    }
}
