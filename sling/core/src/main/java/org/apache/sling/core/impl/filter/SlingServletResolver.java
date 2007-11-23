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

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.ServletResolver;
import org.apache.sling.core.CoreConstants;
import org.apache.sling.core.impl.helper.SlingServletConfig;
import org.apache.sling.core.servlets.DefaultServlet;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>SlingServletResolver</code> TODO
 */
public class SlingServletResolver extends ServletBinder implements
        ServletResolver, ManagedService {

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * @scr.property values.1="/apps/components" values.2="/libs/components"
     *               label="%resolver.path.name"
     *               description="%resolver.path.description"
     */
    public static final String PROP_PATH = "path";

    public static final String DEFAULT_SERVLET_NAME = "sling.core.servlet.default";

    private String[] path;

    private Map<String, Servlet> servlets = new HashMap<String, Servlet>();

    private ServiceRegistration registration;

    public SlingServletResolver(BundleContext bundleContext,
            ServletContext servletContext) {
        super(bundleContext, servletContext, Servlet.class.getName());

        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_PID, getClass().getName());
        props.put(Constants.SERVICE_DESCRIPTION, "Sling Servlet Resolver");
        props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");

        registration = bundleContext.registerService(
            ManagedService.class.getName(), this, props);
    }

    @Override
    public void dispose() {
        if (registration != null) {
            registration.unregister();
        }

        super.dispose();
    }
    // ---------- ServletResolver interface -----------------------------------

    public Servlet resolveServlet(SlingHttpServletRequest request) {

        // 2.3 check Servlet
        Resource resource = request.getResource();
        String path = resource.getURI();
        Servlet servlet = resolveServlet(resource);

        if (servlet != null) {
            String name = servlet.getServletConfig().getServletName();
            log.debug("Using Component {} for {}", name, path);
        }

        return servlet;
    }

    @Override
    public Object addingService(ServiceReference reference) {
        Object typeObject = reference.getProperty(CoreConstants.SLING_RESOURCE_TYPES);
        if (typeObject == null) {
            log.info(
                "addingService: Ignoring Servlet service {} without resource types",
                reference.getProperty(Constants.SERVICE_ID));
            return null;
        }

        Servlet servlet = (Servlet) super.addingService(reference);
        if (servlet != null) {
            String name = servlet.getServletConfig().getServletName();
            if (typeObject instanceof String[]) {
                String[] types = (String[]) typeObject;
                for (String type : types) {
                    log.debug("servletBound: Servlet {} handles type {}", name,
                        type);
                    servlets.put(type, servlet);
                }
            } else {
                log.debug("servletBound: Servlet {} handles type {}", name,
                    typeObject);
                servlets.put(typeObject.toString(), servlet);
            }
        }

        return servlet;
    }

    @Override
    public void modifiedService(ServiceReference reference, Object service) {
        // might decide to fix registration resource types
        super.modifiedService(reference, service);
    }

    @Override
    public void removedService(ServiceReference reference, Object service) {
        Object typeObject = reference.getProperty(CoreConstants.SLING_RESOURCE_TYPES);

        if (typeObject != null) {
            String name = ((Servlet) service).getServletConfig().getServletName();
            if (typeObject instanceof String[]) {
                String[] types = (String[]) typeObject;
                for (String type : types) {
                    log.debug("servletBound: Servlet {} unregistered for  {}",
                        name, type);
                    servlets.remove(type);
                }
            } else {
                log.debug("servletBound: Servlet {} unregistered for {}", name,
                    typeObject);
                servlets.remove(typeObject);
            }
        }

        super.removedService(reference, service);
    }

    // ---------- internal helper ---------------------------------------------

    private Servlet getServlet(String resourceType) {
        return servlets.get(resourceType);
    }

    private Servlet resolveServlet(Resource resource) {
        // get the servlet by resource type
        String type = resource.getResourceType();
        Servlet servlet = getServlet(type);
        if (servlet != null) {
            return servlet;
        }

        // TODO: implement script resolver here

        // use default servlet, create one if missing
        servlet = getServlet(DEFAULT_SERVLET_NAME);
        if (servlet == null) {
            try {
                servlet = new DefaultServlet();
                servlet.init(new SlingServletConfig(null, null, DEFAULT_SERVLET_NAME));
                servlets.put(DEFAULT_SERVLET_NAME, servlet);
            } catch (ServletException se) {
                log.error("Failed to initiliaze Servlet", se);
            }
        }

        return servlet;
    }

    // ---------- SCR Integration ----------------------------------------------

    public void updated(Dictionary properties) {

        // empty path to begin with
        path = null;

        // from configuration if available
        if (properties != null) {
            Object pathObject = properties.get(PROP_PATH);
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
            }
        }
    }

}
