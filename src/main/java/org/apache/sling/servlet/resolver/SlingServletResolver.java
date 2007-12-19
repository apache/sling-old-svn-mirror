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
package org.apache.sling.servlet.resolver;

import static org.apache.sling.api.SlingConstants.*;
import static org.apache.sling.core.CoreConstants.*;
import static org.apache.sling.servlet.resolver.ServletResolverConstants.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.SlingException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.scripting.SlingScript;
import org.apache.sling.api.scripting.SlingScriptResolver;
import org.apache.sling.api.servlets.ServletResolver;
import org.apache.sling.core.servlets.AbstractServiceReferenceConfig;
import org.apache.sling.core.servlets.ErrorHandler;
import org.apache.sling.servlet.resolver.defaults.DefaultErrorHandlerServlet;
import org.apache.sling.servlet.resolver.defaults.DefaultServlet;
import org.apache.sling.servlet.resolver.helper.ErrorSlingHttpServletRequest;
import org.apache.sling.servlet.resolver.helper.SlingScriptServlet;
import org.apache.sling.servlet.resolver.helper.SlingServletConfig;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>SlingServletResolver</code> TODO
 *
 * @scr.component label="%servletresolver.name"
 *                description="%servletresolver.description"
 * @scr.property name="service.description" value="Sling Servlet Resolver and
 *               Error Handler"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.service
 * @scr.reference name="Servlet" interface="javax.servlet.Servlet"
 *                cardinality="0..n" policy="dynamic"
 */
public class SlingServletResolver implements ServletResolver, ErrorHandler {

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * @scr.property values.1="/apps/components" values.2="/libs/components"
     *               label="%resolver.path.name"
     *               description="%resolver.path.description"
     */
    public static final String PROP_PATH = "path";

    private static final String REF_SERVLET = "Servlet";

    /** @scr.reference cardinality="0..1" policy="dynamic" */
    private SlingScriptResolver scriptResolver;

    /** @scr.reference */
    private ServletContext servletContext;

    private Map<String, Servlet> servlets = new HashMap<String, Servlet>();

    private Map<ServiceReference, Servlet> servletsByReference = new HashMap<ServiceReference, Servlet>();

    private List<ServiceReference> pendingServlets = new ArrayList<ServiceReference>();

    private ComponentContext context;

    private String[] path;

    private Servlet coreDefaultServlet;

    private Servlet defaultErrorServlet;

    // ---------- ServletResolver interface -----------------------------------

    public Servlet resolveServlet(SlingHttpServletRequest request) {

        // resolve a servlet or script based on the request
        Servlet servlet = resolveServletOrScript(request);

        // use default servlet, create one if missing
        if (servlet == null) {
            servlet = getServlet(DEFAULT_SERVLET_NAME);
        }

        // last resort, use the core bundle default servlet
        if (servlet == null) {
            servlet = getCoreDefaultServlet();
        }

        if (servlet != null && log.isDebugEnabled()) {
            String name;
            if (servlet.getServletConfig() != null) {
                name = servlet.getServletConfig().getServletName();
            } else {
                name = servlet.getServletInfo();
            }
            if (name == null) {
                name = servlet.getClass().getName();
            }
            log.debug("Using Servlet {} for {}", name, path);
        }

        return servlet;
    }

    // ---------- ErrorHandler interface --------------------------------------

    public void handleError(int status, String message,
            HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        // do not handle, if already handling ....
        if (request.getAttribute(ERROR_REQUEST_URI) == null) {

            // find the error handler component
            ErrorSlingHttpServletRequest eRequest = new ErrorSlingHttpServletRequest(
                request, String.valueOf(status));
            Servlet servlet = resolveServletOrScript(eRequest);

            if (servlet == null) {
                servlet = getDefaultErrorServlet();
            }

            // set the message properties
            request.setAttribute(ERROR_STATUS, new Integer(
                status));
            request.setAttribute(ERROR_MESSAGE, message);

            // the servlet name for a sendError handling is still stored
            // as the request attribute
            Object servletName = request.getAttribute(SLING_CURRENT_SERVLET_NAME);
            if (servletName instanceof String) {
                request.setAttribute(ERROR_SERVLET_NAME, servletName);
            }

            if (handleError(servlet, request, response)) {
                return;
            }
        }

        // get here, if we have no handler, let the status go up the chain
        // and if this causes and exception, so what ...
        response.sendError(status, message);
    }

    public void handleError(Throwable throwable, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {

        // do not handle, if already handling ....
        if (request.getAttribute(SlingConstants.ERROR_REQUEST_URI) == null) {

            // find the error handler component
            Servlet servlet = null;
            Class<?> tClass = throwable.getClass();
            ErrorSlingHttpServletRequest eRequest = new ErrorSlingHttpServletRequest(
                request, "");
            while (servlet == null && tClass != Object.class) {
                eRequest.setMethod(tClass.getSimpleName());
                servlet = resolveServletOrScript(eRequest);

                // go to the base class
                tClass = tClass.getSuperclass();
            }

            if (servlet == null) {
                servlet = getDefaultErrorServlet();
            }

            // set the message properties
            request.setAttribute(SlingConstants.ERROR_EXCEPTION, throwable);
            request.setAttribute(SlingConstants.ERROR_EXCEPTION_TYPE,
                throwable.getClass());
            request.setAttribute(SlingConstants.ERROR_MESSAGE,
                throwable.getMessage());

            if (handleError(servlet, request, response)) {
                return;
            }
        }

        // get here, if we have no handler, let the throwable go up the chain
        if (throwable instanceof IOException) {
            throw (IOException) throwable;
        } else if (throwable instanceof ServletException) {
            throw (ServletException) throwable;
        } else if (throwable instanceof RuntimeException) {
            throw (RuntimeException) throwable;
        } else {
            throw new ServletException(throwable);
        }
    }

    // ---------- internal helper ---------------------------------------------

    Servlet resolveServletOrScript(SlingHttpServletRequest request) {

        // get the servlet by resource type
        Servlet servlet = getServlet(request.getResource().getResourceType());
        if (servlet != null) {
            return servlet;
        }

        // no typed servlet, so lets try scripting
        try {
            servlet = getScriptServlet(request);
            if (servlet != null) {
                return servlet;
            }
        } catch (SlingException se) {
            log.error("resolveServlet: Error trying to find script", se);
        }

        // no script either
        return null;
    }

    private Servlet getScriptServlet(SlingHttpServletRequest request)
            throws SlingException {
        SlingScriptResolver ssr = scriptResolver;
        if (ssr != null) {
            SlingScript script = ssr.resolveScript(request);
            if (script != null) {
                return new SlingScriptServlet(script);
            }
        }

        return null;
    }

    private Servlet getCoreDefaultServlet() {
        if (coreDefaultServlet == null) {
            try {
                Servlet servlet = new DefaultServlet();
                servlet.init(new SlingServletConfig(servletContext, null,
                    "Sling Core Default Servlet"));
                coreDefaultServlet = servlet;
            } catch (ServletException se) {
                log.error("Failed to initiliaze Servlet", se);
            }
        }

        return coreDefaultServlet;
    }

    private Servlet getDefaultErrorServlet() {
        if (defaultErrorServlet == null) {
            try {
                Servlet servlet = new DefaultErrorHandlerServlet();
                servlet.init(new SlingServletConfig(servletContext, null,
                    "Sling Default Error Handler Servlet"));
                defaultErrorServlet = servlet;
            } catch (ServletException se) {
                log.error("Failed to initiliaze Servlet", se);
            }
        }

        return defaultErrorServlet;
    }

    private boolean handleError(Servlet errorHandler,
            HttpServletRequest request, HttpServletResponse response) {

        request.setAttribute(SlingConstants.ERROR_REQUEST_URI,
            request.getRequestURI());

        // if there is no explicitly known error causing servlet, use
        // the name of the error handler servlet
        if (request.getAttribute(SlingConstants.ERROR_SERVLET_NAME) == null) {
            request.setAttribute(SlingConstants.ERROR_SERVLET_NAME,
                errorHandler.getServletConfig().getServletName());
        }

        // find a component by
        try {
            errorHandler.service(request, response);
            return true;
        } catch (Throwable t) {
            log.error("Cannot handle error", t);
        }

        return false;
    }

    // ---------- SCR Integration ----------------------------------------------

    protected void activate(ComponentContext context) {

        this.context = context;

        // empty path to begin with
        path = null;

        // from configuration if available
        Dictionary<?, ?> properties = context.getProperties();
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

    protected synchronized void bindServlet(ServiceReference reference) {
        if (servletContext == null) {
            pendingServlets.add(reference);
        } else {
            createServlet(servletContext, reference);
        }
    }

    protected synchronized void unbindServlet(ServiceReference reference) {
        if (servletContext == null) {
            pendingServlets.remove(reference);
        } else {
            destroyServlet(reference);
        }
    }

    protected void bindServletContext(ServletContext newServletContext) {

        boolean destroy;
        Collection<ServiceReference> refs;

        synchronized (this) {

            if (this.servletContext == null) {

                refs = pendingServlets;
                pendingServlets = new ArrayList<ServiceReference>();
                destroy = false;

            } else {

                refs = new ArrayList<ServiceReference>(servletsByReference.keySet());
                destroy = true;

            }

            this.servletContext = newServletContext;
        }

        if (destroy) {
            destroyAllServlets(refs);
        }

        createAllServlets(this.servletContext, refs);
    }

    protected void unbindServletContext(ServletContext oldServletContext) {

        if (this.servletContext == oldServletContext) {

            Collection<ServiceReference> refs;
            synchronized (this) {
                refs = new ArrayList<ServiceReference>(servletsByReference.keySet());
                pendingServlets.addAll(refs);
                this.servletContext = null;
            }

            // destroy all servlets
            destroyAllServlets(refs);
        }
    }

    // ---------- Servlet Management -------------------------------------------

    private void createAllServlets(ServletContext servletContext,
            Collection<ServiceReference> pendingServlets) {
        for (ServiceReference serviceReference : pendingServlets) {
            createServlet(servletContext, serviceReference);
        }
    }

    private boolean createServlet(ServletContext servletContext,
            ServiceReference reference) {
        Object typeObject = reference.getProperty(SLING_RESOURCE_TYPES);
        if (typeObject == null) {
            log.info(
                "bindServlet: Ignoring Servlet service {} without resource types",
                reference.getProperty(Constants.SERVICE_ID));
            return false;
        }

        // super:addingService
        String name = AbstractServiceReferenceConfig.getName(reference);
        if (name == null) {
            log.error(
                "bindServlet: Cannot register servlet {} without a servlet name",
                reference);
            return false;
        }

        Servlet servlet = (Servlet) context.locateService(REF_SERVLET,
            reference);
        if (servlet == null) {
            log.error(
                "bindServlet: Servlet service not available from reference {}",
                reference);
            return false;
        }

        try {
            servlet.init(new SlingServletConfig(servletContext, reference, name));
            log.debug("bindServlet: Servlet {} added", name);
        } catch (ServletException ce) {
            log.error("bindServlet: Component " + name
                + " failed to initialize", ce);
            return false;
        } catch (Throwable t) {
            log.error("bindServlet: Unexpected problem initializing component "
                + name, t);
            return false;
        }

        if (typeObject instanceof String[]) {
            String[] types = (String[]) typeObject;
            for (String type : types) {
                log.debug("bindServlet: Servlet {} handles type {}", name, type);
                putServlet(type, servlet);
            }
        } else {
            log.debug("bindServlet: Servlet {} handles type {}", name,
                typeObject);
            putServlet(typeObject.toString(), servlet);
        }

        servletsByReference.put(reference, servlet);

        return true;
    }

    private void destroyAllServlets(Collection<ServiceReference> refs) {
        for (ServiceReference serviceReference : refs) {
            destroyServlet(serviceReference);
        }
    }

    private void destroyServlet(ServiceReference reference) {
        Servlet servlet = servletsByReference.remove(reference);
        if (servlet != null) {
            String name = servlet.getServletConfig().getServletName();
            log.debug("unbindServlet: Servlet {} removed", name);

            Object typeObject = reference.getProperty(SLING_RESOURCE_TYPES);
            if (typeObject instanceof String[]) {
                String[] types = (String[]) typeObject;
                for (String type : types) {
                    log.debug("unbindServlet: Servlet {} unregistered for  {}",
                        name, type);
                    removeServlet(type);
                }
            } else {
                log.debug("unbindServlet: Servlet {} unregistered for {}",
                    name, typeObject);
                removeServlet((String) typeObject);
            }

            try {
                servlet.destroy();
            } catch (Throwable t) {
                log.error(
                    "unbindServlet: Unexpected problem destroying servlet "
                        + name, t);
            }
        }
    }

    private void putServlet(String resourceType, Servlet servlet) {
        servlets.put(resourceType, servlet);
    }

    private Servlet getServlet(String resourceType) {
        return servlets.get(resourceType);
    }

    private void removeServlet(String resourceType) {
        servlets.remove(resourceType);
    }

}
