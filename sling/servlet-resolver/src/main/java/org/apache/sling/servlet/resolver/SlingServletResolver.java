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
import static org.apache.sling.core.CoreConstants.SLING_CURRENT_SERVLET_NAME;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
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
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.scripting.SlingScript;
import org.apache.sling.api.scripting.SlingScriptResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.OptingServlet;
import org.apache.sling.api.servlets.ServletResolver;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.core.servlets.AbstractServiceReferenceConfig;
import org.apache.sling.core.servlets.ErrorHandler;
import org.apache.sling.servlet.resolver.defaults.DefaultErrorHandlerServlet;
import org.apache.sling.servlet.resolver.defaults.DefaultServlet;
import org.apache.sling.servlet.resolver.helper.LocationResource;
import org.apache.sling.servlet.resolver.helper.LocationUtil;
import org.apache.sling.servlet.resolver.helper.PathIterator;
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

    private Servlet coreDefaultServlet;

    private Servlet defaultErrorServlet;

    // ---------- ServletResolver interface -----------------------------------

    public Servlet resolveServlet(SlingHttpServletRequest request) {

        Servlet servlet = null;

        // first check whether the type of a resource is the absolute
        // path of a servlet (or script)
        String type = request.getResource().getResourceType();
        if (type.charAt(0) == '/') {
            Resource res = request.getResourceResolver().getResource(type);
            if (res != null) {
                servlet = res.adaptTo(Servlet.class);
            }
        }
        
        if (servlet == null) {
            LocationUtil lu = LocationUtil.create(request);
            Collection<LocationResource> candidates = lu.getScripts(request);
            Iterator<LocationResource> lri = candidates.iterator();
            while (lri.hasNext() && servlet == null) {
                Resource candidateResource = lri.next().getResource();
                Servlet candidate = candidateResource.adaptTo(Servlet.class);
                if (candidate != null) {
                    boolean servletAcceptsRequest = !(candidate instanceof OptingServlet)
                        || ((OptingServlet) candidate).accepts(request);
                    if (servletAcceptsRequest) {
                        servlet = candidate;
                    }
                }
            }
        }

        // last resort, use the core bundle default servlet
        if (servlet == null) {
            servlet = getCoreDefaultServlet();
        }

        if (log.isDebugEnabled()) {
            if (servlet != null) {
                String name;
                if (servlet.getServletConfig() != null) {
                    name = servlet.getServletConfig().getServletName();
                } else {
                    name = servlet.getServletInfo();
                }
                if (name == null) {
                    name = servlet.getClass().getName();
                }
                log.info("Servlet {} found for Resource={}", name,
                    request.getResource());
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

        if (name.startsWith("/")) {
            Resource resource = resourceResolver.getResource(name);
            return (resource != null)
                    ? resource.adaptTo(SlingScript.class)
                    : null;
        }

        for (int i = 0; i < path.length; i++) {
            String scriptPath = path[i] + name;
            Resource resource = resourceResolver.getResource(scriptPath);
            if (resource != null) {
                SlingScript script = resource.adaptTo(SlingScript.class);
                if (script == null) {
                    log.debug("findScript: Using script {} for {}",
                        script.getScriptResource().getPath(), name);
                    return script;
                }
            }
        }

        log.info("findScript: No script {} found in path", name);
        return null;
    }

    // ---------- ErrorHandler interface --------------------------------------

    /**
     * @see org.apache.sling.core.servlets.ErrorHandler#handleError(int, String,
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

        // find the error handler component
        ResourceResolver resolver = request.getResourceResolver();
        String baseName = String.valueOf(status);

        // search the servlet by absolute path
        PathIterator pathIterator = new PathIterator(
            ServletResolverConstants.ERROR_HANDLER_PATH, path);
        Servlet servlet = getServlet(resolver, pathIterator, baseName, request);
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

        handleError(servlet, request, response);
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

        // find the error handler component
        Servlet servlet = null;
        ResourceResolver resolver = request.getResourceResolver();

        PathIterator pathIterator = new PathIterator(
            ServletResolverConstants.ERROR_HANDLER_PATH, path);

        Class<?> tClass = throwable.getClass();
        while (servlet == null && tClass != Object.class) {
            String baseName = tClass.getSimpleName();

            pathIterator.reset();
            servlet = getServlet(resolver, pathIterator, baseName, request);

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

        handleError(servlet, request, response);
    }

    // ---------- internal helper ---------------------------------------------

    private Servlet getServlet(ResourceResolver resolver,
            Iterator<String> paths, String baseName,
            SlingHttpServletRequest request) {

        while (paths.hasNext()) {
            String location = paths.next();
            try {
                Servlet result = getServletAt(resolver, location, baseName);
                if (result != null) {
                    boolean servletAcceptsRequest = !(result instanceof OptingServlet)
                        || ((OptingServlet) result).accepts(request);
                    if (servletAcceptsRequest) {
                        return result;
                    }
                }
            } catch (SlingException se) {
                log.warn("getServlet: Problem resolving servlet at " + location
                    + "/" + baseName, se);
            }
        }

        // exhausted all
        return null;
    }

    private Servlet getServletAt(ResourceResolver resolver, String location,
            String baseName) throws SlingException {
        Servlet result = null;

        Resource scriptRoot = resolver.getResource(location);
        if (scriptRoot != null) {

            log.debug("Looking for servlet with filename={} under {}",
                baseName, scriptRoot.getPath());

            // get the item and ensure it is a node
            Iterator<Resource> children = resolver.listChildren(scriptRoot);
            while (result == null && children.hasNext()) {
                Resource resource = children.next();

                // extract the name of the resource
                int lastSlash = resource.getPath().lastIndexOf('/');
                String name = resource.getPath().substring(lastSlash + 1);

                // only accept it if it is equal to the base name or
                // if there is just a single extension after the base name
                if (name.startsWith(baseName)
                    && (name.length() == baseName.length() || name.lastIndexOf('.') == baseName.length())) {
                    result = resource.adaptTo(Servlet.class);
                }
            }

            // there is no child node with the basename, try without
            if (result == null) {
                result = scriptRoot.adaptTo(Servlet.class);
            }

        } else {
            // check alternative variant using location/basename directly
            Resource scriptResource = resolver.getResource(location + "/"
                + baseName);
            if (scriptResource != null) {
                result = scriptResource.adaptTo(Servlet.class);
            }
        }

        return result;
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
                String name = servlet.getServletConfig().getServletName();
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

    private static String getScriptBaseName(SlingHttpServletRequest request) {
        String methodName = request.getMethod();
        String extension = request.getRequestPathInfo().getExtension();

        if (methodName == null || methodName.length() == 0) {

            throw new IllegalArgumentException(
                "HTTP Method name must not be empty");

        } else if ((HttpConstants.METHOD_GET.equalsIgnoreCase(methodName) || HttpConstants.METHOD_HEAD.equalsIgnoreCase(methodName))
            && extension != null && extension.length() > 0) {

            // for GET, we use the request extension
            return extension;

        } else {

            // for other methods use the method name
            return methodName;
        }
    }
}
