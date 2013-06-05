/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.hc.sling.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Webconsole plugin to execute health check rules */ 
@SuppressWarnings("serial")
public class SlingHealthCheckWebconsolePlugin extends HttpServlet {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final SlingHealthCheckServlet servlet;
    private final ServiceRegistration service;
    private final ResourceResolverFactory resourceResolverFactory;  
    public static final String TITLE = "Sling Health Check";
    public static final String LABEL = "healthcheck";
    public static final String PARAM_PATH = "rulesPath";
    public static final String PARAM_TAGS = "tags";
    
    SlingHealthCheckWebconsolePlugin(BundleContext ctx, ResourceResolverFactory rrf, SlingHealthCheckServlet s){
        servlet = s;
        resourceResolverFactory = rrf;
        
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_DESCRIPTION, "Sling Health Check Web Console Plugin");
        props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        props.put(Constants.SERVICE_PID, getClass().getName());
        props.put("felix.webconsole.label", LABEL);
        props.put("felix.webconsole.title", TITLE);
        props.put("felix.webconsole.css", "/" + LABEL + "/res/ui/healthcheck.css");

        service = ctx.registerService(new String[] { "javax.servlet.Servlet" }, this, props);
        log.info("{} registered as a Webconsole plugin", this);
    }
    
    public void deactivate() {
        log.info("Unregistering {}", this);
        service.unregister();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Static resource?
        final String pathInfo = req.getPathInfo();
        if(pathInfo!= null && pathInfo.contains("res/ui")) {
            final String prefix = "/" + LABEL;
            final InputStream is = getClass().getResourceAsStream(pathInfo.substring(prefix.length()));
            if(is == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, pathInfo);
            }
            final byte [] buffer = new byte[16384];
            int n=0;
            while( (n = is.read(buffer, 0, buffer.length)) > 0) {
                resp.getOutputStream().write(buffer, 0, n); 
            }
            resp.getOutputStream().flush();
            return;
        }
        
        // Send parameters form
        doForm(req, resp);
        
        // And execute rules if we got a path
        final String path = getParam(req, PARAM_PATH, null);
        final String [] tags = getParam(req, PARAM_TAGS, "").split(",");
        
        if(path == null || path.trim().length() == 0) {
            return;
        }
        
        ResourceResolver resolver = null;
        try {
            resolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
            final Resource r = resolver.getResource(path);
            if(r == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, path);
            }
            servlet.executeRules(r, tags, "html", resp);
        } catch (LoginException e) {
            throw new ServletException("Unable to get a ResourceResolver", e);
        } finally {
            if(resolver != null) {
                resolver.close();
            }
        }
    }
    
    private void doForm(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        final PrintWriter pw = resp.getWriter();
        final WebConsoleHelper c = new WebConsoleHelper(pw);
        pw.print("<form method='get'>");
        pw.println("<table class='content' cellpadding='0' cellspacing='0' width='100%'>");
        c.titleHtml(TITLE, "To execute health check rules, enter the path of a rules definition "
                + "( or the root of a subtree of definitions) "
                + " and an optional list of tags if you want to select a tagged subset of the rules.");
        
        final String path = getParam(req, PARAM_PATH, "");
        
        c.tr(); 
        c.tdLabel("Rules definition path");
        c.tdContent();
        pw.println("<input type='text' name='" + PARAM_PATH + "' value='" + path + "' class='input' size='80'>");
        c.closeTd(); 
        c.closeTr();
        
        final String tags = getParam(req, PARAM_TAGS, "");
        
        c.tr(); 
        c.tdLabel("Rule tags (comma-separated)");
        c.tdContent();
        pw.println("<input type='text' name='" + PARAM_TAGS + "' value='" + tags + "' class='input' size='80'>");
        c.closeTd(); 
        c.closeTr();
        
        c.tr(); 
        c.tdContent();
        pw.println("<input type='submit' value='Execute selected rules'/>");
        c.closeTd(); 
        c.closeTr();
        
        pw.println("</table></form>");
    }

    private String getParam(HttpServletRequest req, String name, String defaultValue) {
        String result = req.getParameter(name);
        if(result == null) {
            result = defaultValue;
        }
        return result;
    }
}
