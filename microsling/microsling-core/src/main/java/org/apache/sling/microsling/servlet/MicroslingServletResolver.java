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
package org.apache.sling.microsling.servlet;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingScript;
import org.apache.sling.api.scripting.SlingScriptResolver;
import org.apache.sling.api.servlets.ServletResolver;
import org.apache.sling.microsling.scripting.MicroslingScriptServlet;
import org.apache.sling.microsling.slingservlets.DefaultSlingServlet;
import org.apache.sling.microsling.slingservlets.StreamServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MicroslingServletResolver implements ServletResolver {

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    private ServletContext servletContext;

    private Map<String, Servlet> servlets;

    private DefaultSlingServlet defaultSlingServlet;

    MicroslingServletResolver(ServletContext servletContext)
            throws ServletException {
        this.servletContext = servletContext;
        this.servlets = new HashMap<String, Servlet>();

        addServlet("nt:file", new StreamServlet());

        defaultSlingServlet = new DefaultSlingServlet();
        ServletConfig config = new MicroslingServletConfig(
            "Default microsling Servlet", getServletContext());
        defaultSlingServlet.init(config);
    }

    protected void destroy() {
        if (servlets != null) {
            Servlet[] servletList = servlets.values().toArray(
                new Servlet[servlets.size()]);
            for (Servlet servlet : servletList) {
                try {
                    servlet.destroy();
                } catch (Throwable t) {
                    getServletContext().log(
                        "Unexpected problem destroying servlet " + servlet, t);
                }
            }
            servlets.clear();
        }

        if (defaultSlingServlet != null) {
            defaultSlingServlet.destroy();
        }
    }

    public Servlet resolveServlet(SlingHttpServletRequest request)
            throws ServletException {
        // Select a SlingServlet and delegate the actual request processing
        // to it
        final Servlet selectedServlet = selectSlingServlet(request);
        if (selectedServlet != null) {
            return selectedServlet;
        }

        // no typed servlet, so lets try scripting
        SlingScript script = getScriptResolver(request).resolveScript(request);
        if (script != null) {
            return new MicroslingScriptServlet(script);
        }

        if (log.isDebugEnabled()) {
            final Resource r = request.getResource();
            log.debug("No specific Servlet or script found for Resource " + r
                + ", using default Servlet");
        }

        return defaultSlingServlet;
    }

    protected SlingScriptResolver getScriptResolver(
            SlingHttpServletRequest request) {
        return request.getServiceLocator().getService(SlingScriptResolver.class);
    }

    /** Select a SlingServlet to process the given request */
    protected Servlet selectSlingServlet(SlingHttpServletRequest req)
            throws SlingException {

        // use the resource type to select a servlet
        final Resource r = req.getResource();
        String type = (r == null ? null : r.getResourceType());
        final Servlet result = (type != null) ? servlets.get(type) : null;

        if (log.isDebugEnabled()) {
            if (result == null) {
                log.debug("No Servlet found for resource type " + type);
            } else {
                log.debug("Using Servlet class "
                    + result.getClass().getSimpleName() + " for resource type "
                    + type);
            }
        }

        return result;
    }

    /** Add servlets by resource type */
    protected void addServlet(final String resourceType, Servlet servlet) {

        try {
            servlet.init(new MicroslingServletConfig(resourceType, getServletContext()));

            // only register if initialization succeeds
            servlets.put(resourceType, servlet);
        } catch (Throwable t) {
            getServletContext().log(
                "Failed initializing servlet " + servlet + " for type "
                    + resourceType, t);
        }

    }

    protected ServletContext getServletContext() {
        return servletContext;
    }
}
