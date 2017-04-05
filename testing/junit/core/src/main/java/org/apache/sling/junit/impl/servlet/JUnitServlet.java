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
import java.util.Dictionary;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.junit.RendererSelector;
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

    private volatile ServletProcessor processor;

    protected void activate(final ComponentContext ctx) throws ServletException, NamespaceException {
        servletPath = getServletPath(ctx);
        if(servletPath == null) {
            log.info("Servlet path is null, not registering with HttpService");
        } else {
            httpService.registerServlet(servletPath, this, null, null);
            this.processor = new ServletProcessor(testsManager, rendererSelector);
            log.info("Servlet registered at {}", servletPath);
        }
    }

    /** Return the path at which to mount this servlet, or null
     *  if it must not be mounted.
     */
    private String getServletPath(ComponentContext ctx) {
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
        this.processor = null;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        this.processor.doGet(req, resp, this.servletPath);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        this.processor.doPost(req, resp);
    }


}