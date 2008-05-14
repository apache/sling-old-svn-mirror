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

import static org.apache.sling.api.SlingConstants.ERROR_MESSAGE;
import static org.apache.sling.api.SlingConstants.ERROR_SERVLET_NAME;
import static org.apache.sling.api.SlingConstants.ERROR_STATUS;
import static org.apache.sling.engine.EngineConstants.SLING_CURRENT_SERVLET_NAME;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
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
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestProgressTracker;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.scripting.SlingScript;
import org.apache.sling.api.scripting.SlingScriptResolver;
import org.apache.sling.api.servlets.OptingServlet;
import org.apache.sling.api.servlets.ServletResolver;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.engine.RequestUtil;
import org.apache.sling.engine.servlets.AbstractServiceReferenceConfig;
import org.apache.sling.engine.servlets.ErrorHandler;
import org.apache.sling.servlet.resolver.defaults.DefaultErrorHandlerServlet;
import org.apache.sling.servlet.resolver.defaults.DefaultServlet;
import org.apache.sling.servlet.resolver.helper.ResourceCollector;
import org.apache.sling.servlet.resolver.helper.SlingServletConfig;
import org.apache.sling.servlet.resolver.resource.ServletResourceProvider;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
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
public class SlingServletResolver implements ServletResolver,
        SlingScriptResolver, ErrorHandler {

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * @scr.property values.1="/apps" values.2="/libs"
     *               label="%resolver.path.name"
     *               description="%resolver.path.description"
     */
    public static final String PROP_PATH = "path";

    /**
     * @scr.property value="/apps" label="%resolver.servletRoot.name"
     *               description="%resolver.servletRoot.description"
     */
    public static final String PROP_SERVLET_ROOT = "servletRoot";

    private static final String REF_SERVLET = "Servlet";

    /** @scr.reference */
    private ServletContext servletContext;

    private Map<ServiceReference, ServiceRegistration> servletsByReference = new HashMap<ServiceReference, ServiceRegistration>();

    private List<ServiceReference> pendingServlets = new ArrayList<ServiceReference>();

    private ComponentContext context;

    private String[] path;

    private String servletRoot;

    // the default servlet if no other servlet applies for a request. This
    // field is set on demand by getDefaultServlet()
    private Servlet defaultServlet;

    // the default error handler servlet if no other error servlet applies for
    // a request. This field is set on demand by getDefaultErrorServlet()
    private Servlet defaultErrorServlet;

    // ---------- ServletResolver interface -----------------------------------

    public Servlet resolveServlet(SlingHttpServletRequest request) {

        Resource resource = request.getResource();
        
        // start tracking servlet resolution
        RequestProgressTracker tracker = request.getRequestProgressTracker();
        String timerName = "resolverServlet(" + resource +")";
        tracker.startTimer(timerName);
        
        Servlet servlet = null;

        // first check whether the type of a resource is the absolute
        // path of a servlet (or script)
        String type = resource.getResourceType();
        if (type.charAt(0) == '/') {
            Resource res = request.getResourceResolver().getResource(type);
            if (res != null) {
                servlet = res.adaptTo(Servlet.class);
            }
        }
        
        // the resource type is not absolute, so lets go for the deep search
        if (servlet == null) {
            ResourceCollector locationUtil = ResourceCollector.create(request);
            servlet = getServlet(locationUtil, request, resource);
        }

        // last resort, use the core bundle default servlet
        if (servlet == null) {
            servlet = getDefaultServlet();
        }

        // track servlet resolution termination
        if (servlet == null) {
            tracker.logTimer(timerName,
                "Servlet Resolution failed. See log for details");
        } else {
            tracker.logTimer(timerName, "Using Servlet {0}",
                RequestUtil.getServletName(servlet));
        }
        
        // log the servlet found
        if (log.isDebugEnabled()) {
            if (servlet != null) {
                log.info("Servlet {} found for Resource={}",
                    RequestUtil.getServletName(servlet), request.getResource());
            } else {
                log.debug("No servlet found for Resource={}",
                    request.getResource());
            }
        }

        return servlet;
    }

    // ---------- ScriptResolver interface ------------------------------------

    public SlingScript findScript(ResourceResolver resourceResolver, String name)
            throws SlingException {

        // is the path absolute
        SlingScript script = null;
        if (name.startsWith("/")) {

            Resource resource = resourceResolver.getResource(name);
            if (resource != null) {
                script = resource.adaptTo(SlingScript.class);
            }

        } else {

            // relative script resolution against search path
            for (int i = 0; script == null && i < path.length; i++) {
                String scriptPath = path[i] + name;
                Resource resource = resourceResolver.getResource(scriptPath);
                if (resource != null) {
                    script = resource.adaptTo(SlingScript.class);
                }
            }

        }

        // some logging
        if (script != null) {
            log.debug("findScript: Using script {} for {}",
                script.getScriptResource().getPath(), name);
        } else {
            log.info("findScript: No script {} found in path", name);
        }

        // and finally return the script (null or not)
        return script;
    }

    // ---------- ErrorHandler interface --------------------------------------

    /**
     * @see org.apache.sling.engine.servlets.ErrorHandler#handleError(int, String,
     *      SlingHttpServletRequest, SlingHttpServletResponse)
     */
    public void handleError(int status, String message,
            SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws IOException {

        // do not handle, if already handling ....
        if (request.getAttribute(SlingConstants.ERROR_REQUEST_URI) != null) {
            log.error("handleError: Recursive invocation. Not further handling status "
                + status + "(" + message + ")");
            return;
        }

        // start tracker
        RequestProgressTracker tracker = request.getRequestProgressTracker();
        String timerName = "handleError:status=" + status;
        tracker.startTimer(timerName);
        
        try {
            
            // find the error handler component
            Resource resource = getErrorResource(request);

            // find a servlet for the status as the method name
            ResourceCollector locationUtil = new ResourceCollector(
                String.valueOf(status),
                ServletResolverConstants.ERROR_HANDLER_PATH);
            Servlet servlet = getServlet(locationUtil, request, resource);

            // fall back to default servlet if none
            if (servlet == null) {
                servlet = getDefaultErrorServlet();
            }

            // set the message properties
            request.setAttribute(ERROR_STATUS, new Integer(status));
            request.setAttribute(ERROR_MESSAGE, message);

            // the servlet name for a sendError handling is still stored
            // as the request attribute
            Object servletName = request.getAttribute(SLING_CURRENT_SERVLET_NAME);
            if (servletName instanceof String) {
                request.setAttribute(ERROR_SERVLET_NAME, servletName);
            }

            // log a track entry after resolution before calling the handler
            tracker.logTimer(timerName, "Using handler {0}",
                RequestUtil.getServletName(servlet));

            handleError(servlet, request, response);
            
        } finally {
            
            tracker.logTimer(timerName, "Error handler finished");
            
        }
    }

    public void handleError(Throwable throwable,
            SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws IOException {

        // do not handle, if already handling ....
        if (request.getAttribute(SlingConstants.ERROR_REQUEST_URI) != null) {
            log.error(
                "handleError: Recursive invocation. Not further handling Throwable:",
                throwable);
            return;
        }

        // start tracker
        RequestProgressTracker tracker = request.getRequestProgressTracker();
        String timerName = "handleError:throwable="
            + throwable.getClass().getName();
        tracker.startTimer(timerName);
        
        try {
            
            // find the error handler component
            Servlet servlet = null;
            Resource resource = getErrorResource(request);

            Class<?> tClass = throwable.getClass();
            while (servlet == null && tClass != Object.class) {
                // find a servlet for the simple class name as the method name
                ResourceCollector locationUtil = new ResourceCollector(
                    tClass.getSimpleName(),
                    ServletResolverConstants.ERROR_HANDLER_PATH);
                servlet = getServlet(locationUtil, request, resource);

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

            // log a track entry after resolution before calling the handler
            tracker.logTimer(timerName, "Using handler {0}",
                RequestUtil.getServletName(servlet));
            
            handleError(servlet, request, response);
            
        } finally {
            
            tracker.logTimer(timerName, "Error handler finished");
            
        }
    }

    // ---------- internal helper ---------------------------------------------

    /**
     * Returns the resource of the given request to be used as the basis for
     * error handling. If the resource has not yet been set in the request
     * because the error occurred before the resource could be set (e.g. during
     * resource resolution) a synthetic resource is returned whose type is
     * {@link ServletResolverConstants#ERROR_HANDLER_PATH}.
     * 
     * @param request The request whose resource is to be returned.
     */
    private Resource getErrorResource(SlingHttpServletRequest request) {
        Resource res = request.getResource();
        if (res == null) {
            res = new SyntheticResource(request.getResourceResolver(),
                request.getPathInfo(),
                ServletResolverConstants.ERROR_HANDLER_PATH);
        }
        return res;
    }
    
    /**
     * Returns a servlet suitable for handling a request. The
     * <code>locationUtil</code> is used find any servlets or scripts usable
     * for the request. Each servlet returned is in turn asked whether it is
     * actually willing to handle the request in case the servlet is an
     * <code>OptingServlet</code>. The first servlet willing to handle the
     * request is used.
     * 
     * @param locationUtil The helper used to find appropriate servlets ordered
     *            by matching priority.
     * @param request The request used to give to any <code>OptingServlet</code>
     *            for them to decide on whether they are willing to handle the
     *            request
     * @param resource The <code>Resource</code> for which to find a script.
     *            This need not be the same as
     *            <code>request.getResource()</code> in case of error handling
     *            where the resource may not have been assigned to the request
     *            yet.
     * @return a servlet for handling the request or <code>null</code> if no
     *         such servlet willing to handle the request could be found.
     */
    private Servlet getServlet(ResourceCollector locationUtil,
            SlingHttpServletRequest request, Resource resource) {
        Collection<Resource> candidates = locationUtil.getServlets(resource);
        for (Resource candidateResource : candidates) {
            Servlet candidate = candidateResource.adaptTo(Servlet.class);
            if (candidate != null) {
                boolean servletAcceptsRequest = !(candidate instanceof OptingServlet)
                    || ((OptingServlet) candidate).accepts(request);
                if (servletAcceptsRequest) {
                    return candidate;
                }
            }
        }
        
        // exhausted all candidates, we don't have a servlet
        return null;
    }
    
    /**
     * Returns the internal default servlet which is called in case no other
     * servlet applies for handling a request. This servlet should really only
     * be used if the default servlets have not been registered (yet).
     */
    private Servlet getDefaultServlet() {
        if (defaultServlet == null) {
            try {
                Servlet servlet = new DefaultServlet();
                servlet.init(new SlingServletConfig(servletContext, null,
                    "Sling Core Default Servlet"));
                defaultServlet = servlet;
            } catch (ServletException se) {
                log.error("Failed to initiliaze Servlet", se);
            }
        }

        return defaultServlet;
    }


    /**
     * Returns the default error handler servlet, which is called in case there
     * is no other - better matching - servlet registered to handle an error or
     * exception. As it is expected, that most of the time, there will be no
     * such more specific servlet, the default error handler servlet is quite
     * complete.
     */
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

    private void handleError(Servlet errorHandler, HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        request.setAttribute(SlingConstants.ERROR_REQUEST_URI,
            request.getRequestURI());

        // if there is no explicitly known error causing servlet, use
        // the name of the error handler servlet
        if (request.getAttribute(SlingConstants.ERROR_SERVLET_NAME) == null) {
            request.setAttribute(SlingConstants.ERROR_SERVLET_NAME,
                errorHandler.getServletConfig().getServletName());
        }

        try {
            errorHandler.service(request, response);
        } catch (IOException ioe) {
            // forware the IOException
            throw ioe;
        } catch (Throwable t) {
            log.error("Calling the error handler resulted in an error", t);
            log.error(
                "Original error "
                    + request.getAttribute(SlingConstants.ERROR_EXCEPTION_TYPE),
                (Exception) request.getAttribute(SlingConstants.ERROR_EXCEPTION));
        }
    }

    // ---------- SCR Integration ----------------------------------------------

    protected void activate(ComponentContext context) {

        // empty path to begin with
        path = null;

        // from configuration if available
        Dictionary<?, ?> properties = context.getProperties();
        if (properties != null) {
            String[] tmpPath = OsgiUtil.toStringArray(properties.get(PROP_PATH));
            if (tmpPath != null && tmpPath.length > 0) {
                for (int i = 0; i < tmpPath.length; i++) {
                    // ensure leading slash
                    if (!tmpPath[i].startsWith("/")) {
                        tmpPath[i] = "/" + tmpPath;
                    }
                    // ensure trailing slash
                    if (!tmpPath[i].endsWith("/")) {
                        tmpPath[i] += "/";
                    }
                }
            }
            path = tmpPath;
        }

        if (path == null) {
            path = new String[] { "/" };
        }

        String tmpRoot = OsgiUtil.toString(properties.get(PROP_SERVLET_ROOT),
            null);
        if (tmpRoot == null) {
            tmpRoot = path[0];
        } else {
            if (!tmpRoot.startsWith("/")) {
                tmpRoot = "/" + tmpRoot;
            }
            if (!tmpRoot.endsWith("/")) {
                tmpRoot += "/";
            }
        }
        servletRoot = tmpRoot;

        Collection<ServiceReference> refs;
        synchronized (this) {

            refs = pendingServlets;
            pendingServlets = new ArrayList<ServiceReference>();

            // register servlets immediately from now on
            this.context = context;

        }

        createAllServlets(refs);
    }

    protected void deactivate(ComponentContext context) {

        // destroy all active servlets
        Collection<ServiceReference> refs;
        synchronized (this) {
            refs = new ArrayList<ServiceReference>(servletsByReference.keySet());
            // destroy all servlets
            destroyAllServlets(refs);
            this.context = null;
        }
    }

    protected synchronized void bindServlet(ServiceReference reference) {
        if (context == null) {
            pendingServlets.add(reference);
        } else {
            createServlet(servletContext, reference);
        }
    }

    protected synchronized void unbindServlet(ServiceReference reference) {
        pendingServlets.remove(reference);
        destroyServlet(reference);
    }

    // ---------- Servlet Management -------------------------------------------

    private void createAllServlets(Collection<ServiceReference> pendingServlets) {
        for (ServiceReference serviceReference : pendingServlets) {
            createServlet(servletContext, serviceReference);
        }
    }

    private boolean createServlet(ServletContext servletContext,
            ServiceReference reference) {

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

        ServletResourceProvider provider = ServletResourceProvider.create(
            reference, servlet, servletRoot);
        if (provider == null) {
            log.error(
                "createServlet: Cannot register servlet {} without path or resource type configuration",
                name);
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

        Dictionary<String, Object> params = new Hashtable<String, Object>();
        params.put(ResourceProvider.ROOTS, provider.getSerlvetPaths());
        params.put(Constants.SERVICE_DESCRIPTION,
            "ServletResourceProvider for Servlets at "
                + Arrays.asList(provider.getSerlvetPaths()));

        ServiceRegistration reg = context.getBundleContext().registerService(
            ResourceProvider.SERVICE_NAME, provider, params);

        servletsByReference.put(reference, reg);

        return true;
    }

    private void destroyAllServlets(Collection<ServiceReference> refs) {
        for (ServiceReference serviceReference : refs) {
            destroyServlet(serviceReference);
        }
    }

    private void destroyServlet(ServiceReference reference) {
        ServiceRegistration registration = servletsByReference.remove(reference);
        if (registration != null) {

            registration.unregister();

            Servlet servlet = (Servlet) context.locateService(REF_SERVLET,
                reference);
            if (servlet == null) {
                log.error("destroyServlet: Servlet not found for reference {}",
                    reference.toString());
            } else {
                String name = RequestUtil.getServletName(servlet);
                log.debug("unbindServlet: Servlet {} removed", name);

                try {
                    servlet.destroy();
                } catch (Throwable t) {
                    log.error(
                        "unbindServlet: Unexpected problem destroying servlet "
                            + name, t);
                }
            }
        }
    }
}
