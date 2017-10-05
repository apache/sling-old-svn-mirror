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
package org.apache.sling.starter.startup.impl;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

public class HttpStartupSetup {

    private static final String CONTEXT_NAME = "internal.sling.setup";

    private volatile ServiceRegistration<ServletContextHelper> httpContextRegistration;

    private volatile ServiceRegistration<Servlet> defaultServletRegistration;

    private volatile ServiceRegistration<Filter> startupFilterRegistration;

    private final BundleContext context;

    public HttpStartupSetup(final BundleContext context) {
        this.context = context;
    }

    public void start() {
        this.registerHttpContext();
        this.registerStartupFilter();
    }

    public void stop() {
        this.unregisterStartupFilter();
        this.unregisterHttpContext();
    }

    /**
     * Register the http context
     * @param context The bundle context
     */
    private void registerHttpContext() {
        final Dictionary<String, Object> properties = new Hashtable<>();
        properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, CONTEXT_NAME);
        properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/");
        properties.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);

        this.httpContextRegistration = context.registerService(ServletContextHelper.class,
            new ServletContextHelper() {
            }, properties);

        final Dictionary<String, Object> servletProps = new Hashtable<>();
        servletProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
                "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=" + CONTEXT_NAME + ")");
        servletProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/");

        this.defaultServletRegistration = context.registerService(Servlet.class, new HttpServlet() {

            private static final long serialVersionUID = 1L;

        }, servletProps);
    }

    /**
     * Unregister the http context
     */
    private void unregisterHttpContext() {
        if ( this.defaultServletRegistration != null ) {
            this.defaultServletRegistration.unregister();
            this.defaultServletRegistration = null;
        }
        if ( this.httpContextRegistration != null ) {
            this.httpContextRegistration.unregister();
            this.httpContextRegistration = null;
        }
    }

    /**
     * Register the startup filter
     * @param context The bundle context
     */
    private void registerStartupFilter() {
        final Dictionary<String, Object> properties = new Hashtable<>();
        properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
                "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=" + CONTEXT_NAME + ")");
        properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/");
        properties.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);

        this.startupFilterRegistration = context.registerService(Filter.class,
            new StartupFilter(), properties);
    }

    /**
     * Unregister the startup filter
     */
    private void unregisterStartupFilter() {
        if ( this.startupFilterRegistration != null ) {
            this.startupFilterRegistration.unregister();
            this.startupFilterRegistration = null;
        }
    }
}
