/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.servlets.get;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.get.helpers.HtmlRendererServlet;
import org.apache.sling.servlets.get.helpers.JsonRendererServlet;
import org.apache.sling.servlets.get.helpers.PlainTextRendererServlet;
import org.apache.sling.servlets.get.helpers.StreamRendererServlet;

/**
 * A SlingSafeMethodsServlet that renders the current Resource as simple HTML
 * 
 * @scr.component immediate="true" metatype="no"
 * @scr.service interface="javax.servlet.Servlet"
 * 
 * @scr.property name="service.description" value="Default GET Servlet"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * 
 * Use this as a default servlet for Sling
 * @scr.property name="sling.servlet.resourceTypes"
 *               value="sling/servlet/default"
 *               
 * Generic handler for all get requests
 * @scr.property name="sling.servlet.methods" value="GET"
 */
public class DefaultGetServlet extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = -5815904221043005085L;

    private Map<String, Servlet> rendererMap = new HashMap<String, Servlet>();

    private Servlet streamerServlet;

    @Override
    public void init() throws ServletException {
        super.init();

        // Register renderer servlets
        setupServlet(rendererMap, HtmlRendererServlet.EXT_HTML,
            new HtmlRendererServlet());
        setupServlet(rendererMap, PlainTextRendererServlet.EXT_TXT,
            new PlainTextRendererServlet());
        setupServlet(rendererMap, JsonRendererServlet.EXT_JSON,
            new JsonRendererServlet());
        setupServlet(rendererMap, StreamRendererServlet.EXT_RES,
            new StreamRendererServlet());

        // use the servlet for rendering StreamRendererServlet.EXT_RES as the
        // streamer servlet
        streamerServlet = rendererMap.get(StreamRendererServlet.EXT_RES);
    }

    /**
     * @throws ResourceNotFoundException if the resource of the request is a non
     *             existing resource.
     */
    @Override
    protected void doGet(SlingHttpServletRequest request,
            SlingHttpServletResponse response) throws ServletException,
            IOException {

        // cannot handle the request for missing resources
        if (ResourceUtil.isNonExistingResource(request.getResource())) {
            throw new ResourceNotFoundException(
                request.getResource().getPath(), "No Resource found");
        }

        Servlet rendererServlet;
        String ext = request.getRequestPathInfo().getExtension();
        if (ext == null) {
            rendererServlet = streamerServlet;
        } else {
            rendererServlet = rendererMap.get(ext);
        }

        // fail if we should not just stream or we cannot support the ext.
        if (rendererServlet == null) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "No renderer for extension='" + ext + "'");
            return;
        }

        request.getRequestProgressTracker().log("Using " 
                + rendererServlet.getClass().getName()
                + " to render for extension=" + ext);
        rendererServlet.service(request, response);
    }

    @Override
    public void destroy() {

        for (Servlet servlet : rendererMap.values()) {
            try {
                servlet.destroy();
            } catch (Throwable t) {
                // TODO: log
            }
        }

        streamerServlet = null;
        rendererMap.clear();

        super.destroy();
    }

    private void setupServlet(Map<String, Servlet> rendererMap, String key,
            Servlet servlet) {
        try {
            servlet.init(getServletConfig());
            rendererMap.put(key, servlet);
        } catch (Throwable t) {
            // TODO: log
        }
    }
}
