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
package org.apache.sling.usling.servlets;

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
import org.apache.sling.ujax.UjaxPostServlet;
import org.apache.sling.usling.renderers.DefaultHtmlRendererServlet;
import org.apache.sling.usling.renderers.JsonRendererServlet;
import org.apache.sling.usling.renderers.PlainTextRendererServlet;

/**
 * Replaces the Sling default servlet (that bundle must NOT be
 * active) for usling, by delegating to the default usling
 * renderers for GET requests, and to the ujax POST servlet 
 * for POST requests.
 *
 * @scr.service
 *  interface="javax.servlet.Servlet"
 *  
 * @scr.component 
 *  immediate="true" 
 *  metatype="false"
 *  
 * @scr.property 
 *  name="service.description"
 *  value="usling default servlet"
 *  
 * @scr.property 
 *  name="service.vendor" 
 *  value="The Apache Software Foundation"
 *
 * Use this as the default servlet for Sling 
 * @scr.property 
 *  name="sling.core.resourceTypes" 
 *  value="sling.core.servlet.default"
 *  
 */
public class UslingDefaultServlet extends SlingAllMethodsServlet {
    
    private Servlet postServlet;
    private Servlet defaultGetServlet;
    private Map<String, Servlet> getServlets;
    
    @Override
    public void init(ServletConfig config) throws ServletException {
        
        super.init(config);
        
        // setup our "internal" servlets
        postServlet = new UjaxPostServlet();
        postServlet.init(config);
        
        defaultGetServlet = new PlainTextRendererServlet("text/plain");
        
        getServlets = new HashMap<String, Servlet>();
        getServlets.put("html", new DefaultHtmlRendererServlet("text/html"));
        getServlets.put("json", new JsonRendererServlet("application/json"));
        getServlets.put("txt", defaultGetServlet);
    }

    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException, ServletException {
        final Resource resource = request.getResource();

        // cannot handle the request for missing resources
        if (resource instanceof NonExistingResource) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND,"Resource not found at path " + resource.getPath());
            return;
        }
        
        // use default renderer servlet if no extension, else lookup our getServlets 
        Servlet s = defaultGetServlet;
        final String ext = request.getRequestPathInfo().getExtension();
        if(ext!=null && ext.length() > 0) {
            s = getServlets.get(ext);
        }

        // render using s, or fail
        if(s==null) {
            response.sendError(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "No default renderer found for extension='" + ext + "'"
            );
        } else {
            s.service(request, response);
        }
    }


    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) 
    throws ServletException,IOException {
        postServlet.service(request, response);
    }
}
