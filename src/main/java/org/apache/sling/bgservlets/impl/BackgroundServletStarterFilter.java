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
package org.apache.sling.bgservlets.impl;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.bgservlets.ExecutionEngine;
import org.apache.sling.bgservlets.JobStorage;
import org.apache.sling.engine.SlingRequestProcessor;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filter that runs the current request in the background if specific request
 * parameters are set. Must be placed early in the filter chain.
 */
@Component(
        metatype=true,
        label="%BackgroundServletStarterFilter.label",
        description="%BackgroundServletStarterFilter.description")
@Service
@Properties( {
        @Property(name = "filter.scope", value = "request", propertyPrivate=true),
        @Property(name = "filter.order", intValue = -1000000000, propertyPrivate=true )})
public class BackgroundServletStarterFilter implements Filter {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    private ExecutionEngine executionEngine;

    @Reference
    private SlingRequestProcessor slingRequestProcessor;

    @Reference
    private JobStorage jobStorage;

    /** Name of the property that defines the request parameter name to
     *  use to start a servlet in the background.
     */
    @Property(value="sling:bg")
    public static final String PROP_BG_PARAM = "background.parameter.name";

    /**
     * Request runs in the background if this request parameter is present
     */
    private String bgParamName;

    protected void activate(ComponentContext ctx) {
        bgParamName = (String)ctx.getProperties().get(PROP_BG_PARAM);
        if(bgParamName == null || bgParamName.length() == 0) {
            throw new IllegalStateException("Missing " + PROP_BG_PARAM + " in ComponentContext");
        }
        log.info("Request parameter {} will run servlets in the background", bgParamName);
    }

    public void doFilter(final ServletRequest sreq,
            final ServletResponse sresp, final FilterChain chain)
            throws IOException, ServletException {
        if (!(sreq instanceof HttpServletRequest)) {
            throw new ServletException("request is not an HttpServletRequest: "
                    + sresp.getClass().getName());
        }
        if (!(sresp instanceof HttpServletResponse)) {
            throw new ServletException(
                    "response is not an HttpServletResponse: "
                            + sresp.getClass().getName());
        }
        final HttpServletRequest request = (HttpServletRequest) sreq;
        final SlingHttpServletRequest slingRequest =
            (request instanceof SlingHttpServletRequest ? (SlingHttpServletRequest) request : null);
        final HttpServletResponse response = (HttpServletResponse) sresp;
        final String bgParam = sreq.getParameter(bgParamName);
        if (Boolean.valueOf(bgParam)) {
            try {
                final BackgroundRequestExecutionJob job = new BackgroundRequestExecutionJob(
                    slingRequestProcessor, jobStorage, slingRequest, response,
                    new String[] { bgParamName });
                log.debug("{} parameter true, running request in the background ({})",
                        bgParamName, job);
                if (slingRequest != null) {
                    slingRequest.getRequestProgressTracker().log(
                            bgParamName
                            + " parameter true, running request in background ("
                            + job + ")");
                }
                executionEngine.queueForExecution(job);

                // Redirect to the job's status page, using same extension
                // as this request
                String ext = slingRequest.getRequestPathInfo().getExtension();
                if(ext == null) {
                    ext = "";
                } else if(ext.length() > 0) {
                    ext = "." + ext;
                }
                final String path = request.getContextPath() + job.getPath() + ext;
                response.sendRedirect(path);
            } catch (org.apache.sling.api.resource.LoginException e) {
                throw new ServletException("LoginException in doFilter", e);
            }
        } else {
            chain.doFilter(sreq, sresp);
        }
    }

    public void destroy() {
    }

    public void init(FilterConfig cfg) throws ServletException {
    }
}