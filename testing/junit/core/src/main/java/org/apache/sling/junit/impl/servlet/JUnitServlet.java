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
package org.apache.sling.junit.impl.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Dictionary;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.junit.Renderer;
import org.apache.sling.junit.RendererSelector;
import org.apache.sling.junit.RequestParser;
import org.apache.sling.junit.TestSelector;
import org.apache.sling.junit.TestsManager;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Simple test runner servlet */
@SuppressWarnings("serial")
@Component(immediate=true, metatype=true)
public class JUnitServlet extends HttpServlet {
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    public static final String CSS = "junit.css";
    public static final String FORCE_RELOAD_PARAM = "forceReload";
    
    @Property(value="/system/sling/junit")
    static final String SERVLET_PATH_NAME = "servlet.path";
    
    /** Non-null if we are registered with HttpService */ 
    private String servletPath;
    
    @Reference
    private TestsManager testsManager;
    
    @Reference
    private HttpService httpService;
    
    @Reference
    private RendererSelector rendererSelector;
    
    protected void activate(ComponentContext ctx) throws ServletException, NamespaceException {
        servletPath = getServletPath(ctx);
        if(servletPath == null) {
            log.info("Servlet path is null, not registering with HttpService");
        } else {
            httpService.registerServlet(servletPath, this, null, null);
            log.info("Servlet registered at {}", servletPath);
        }
    }

    /** Return the path at which to mount this servlet, or null
     *  if it must not be mounted.
     */
    protected String getServletPath(ComponentContext ctx) {
        final Dictionary<?, ?> config = ctx.getProperties();
        String result = (String)config.get(SERVLET_PATH_NAME);
        if(result != null && result.trim().length() == 0) {
            result = null;
        }
        return result;
    }
    
    protected void deactivate(ComponentContext ctx) throws ServletException, NamespaceException {
        if(servletPath != null) {
            httpService.unregister(servletPath);
            log.info("Servlet unregistered from path {}", servletPath);
        }
        servletPath = null;
    }
    
    /** Return sorted list of available tests
     * @param prefix optionally select only names that match this prefix
     */
    private List<String> getTestNames(TestSelector selector, boolean forceReload) {
        final List<String> result = new LinkedList<String>();
        if(forceReload) {
            log.debug("{} is true, clearing TestsManager caches", FORCE_RELOAD_PARAM);
        }
        result.addAll(testsManager.getTestNames(selector));
        Collections.sort(result);
        return result;
    }
    
    private void sendCss(HttpServletResponse response) throws IOException {
        final InputStream str = getClass().getResourceAsStream("/" + CSS);
        if(str == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, CSS);
        } else {
            response.setContentType("text/css");
            final OutputStream out = response.getOutputStream();
            final byte[] buffer = new byte[16384];
            int count = 0;
            while( (count = str.read(buffer)) > 0) {
                out.write(buffer, 0, count);
            }
            out.flush();
        }
    }
    
    private boolean getForceReloadOption(HttpServletRequest request) {
        final boolean forceReload = "true".equalsIgnoreCase(request.getParameter(FORCE_RELOAD_PARAM));
        log.debug("{} option is set to {}", FORCE_RELOAD_PARAM, forceReload);
        return forceReload;
    }
    
    /** GET request lists available tests */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
    throws ServletException, IOException {
        final boolean forceReload = getForceReloadOption(request);
        
        // Redirect to / if called without it, and serve CSS if requested 
        {
            final String pi = request.getPathInfo();
            if(pi == null) {
                response.sendRedirect(request.getContextPath() + servletPath + "/");
            } else if(pi.endsWith(CSS)) {
                sendCss(response);
                return;
            }
        }

        final TestSelector selector = getTestSelector(request);
        final Renderer renderer = rendererSelector.getRenderer(selector);
        if(renderer == null) {
            throw new ServletException("No Renderer found for " + selector);
        }
        log.debug("GET request: {}", selector);

        renderer.setup(response, getClass().getSimpleName());
        renderer.info("info", "Test selector: " + selector); 
        
        // Any test classes?
        final List<String> testNames = getTestNames(selector, forceReload); 
        if(testNames.isEmpty()) {
            renderer.info(
                    "warning",
                    "No test classes found for selector " + selector
                    + ", check the requirements of the active " +
                    "TestsProvider services for how to supply tests." 
                    );
        } else {
            try {
                testsManager.listTests(testNames, renderer);
                final String postPath = getTestExecutionPath(request, selector, renderer.getExtension()); 
                renderer.link("Execute these tests", postPath, "POST");
            } catch(Exception e) {
                throw new ServletException(e);
            }
        }
        renderer.cleanup();
    }
    
    /** POST request executes tests */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
    throws ServletException, IOException {
        final TestSelector selector = getTestSelector(request);
        final boolean forceReload = getForceReloadOption(request);
        log.info("POST request, executing tests: {}, {}={}", 
                new Object[] { selector, FORCE_RELOAD_PARAM, forceReload});
        
        final Renderer renderer = rendererSelector.getRenderer(selector);
        if(renderer == null) {
            throw new ServletException("No Renderer found for " + selector);
        }
        renderer.setup(response, getClass().getSimpleName());
        
        final List<String> testNames = getTestNames(selector, forceReload);
        if(testNames.isEmpty()) {
            response.sendError(
                    HttpServletResponse.SC_NOT_FOUND, 
                    "No tests found for " + selector);
        }
        try {
            testsManager.executeTests(testNames, renderer, selector);
        } catch(Exception e) {
            throw new ServletException(e);
        }
        
        renderer.cleanup();
    }
    
    /** Return a TestSelector for supplied request */
    protected TestSelector getTestSelector(HttpServletRequest request) {
        return new RequestParser(getTestSelectionPath(request));
    }
    
    /** Return subpath to use for selecting tests */
    protected String getTestSelectionPath(HttpServletRequest request) {
        return request.getPathInfo();
    }
    
    /** Return path to which to POST to execute specified test */
    protected String getTestExecutionPath(HttpServletRequest request, TestSelector selector, String extension) {
        return request.getContextPath() 
        + servletPath
        + "/"
        + selector.getTestSelectorString()
        + "."
        + extension
        ;
    }
}