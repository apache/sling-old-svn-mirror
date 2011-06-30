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
package org.apache.sling.servlets.resolver.internal;

import static org.apache.sling.api.SlingConstants.ERROR_MESSAGE;
import static org.apache.sling.api.SlingConstants.ERROR_SERVLET_NAME;
import static org.apache.sling.api.SlingConstants.ERROR_STATUS;
import static org.apache.sling.api.SlingConstants.SLING_CURRENT_SERVLET_NAME;
import static org.apache.sling.servlets.resolver.internal.ServletResolverConstants.SLING_SERLVET_NAME;
import static org.osgi.framework.Constants.SERVICE_ID;
import static org.osgi.framework.Constants.SERVICE_PID;
import static org.osgi.service.component.ComponentConstants.COMPONENT_NAME;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
import org.apache.sling.api.request.RequestUtil;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.scripting.SlingScript;
import org.apache.sling.api.scripting.SlingScriptResolver;
import org.apache.sling.api.servlets.OptingServlet;
import org.apache.sling.api.servlets.ServletResolver;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.engine.servlets.ErrorHandler;
import org.apache.sling.servlets.resolver.internal.defaults.DefaultErrorHandlerServlet;
import org.apache.sling.servlets.resolver.internal.defaults.DefaultServlet;
import org.apache.sling.servlets.resolver.internal.helper.AbstractResourceCollector;
import org.apache.sling.servlets.resolver.internal.helper.NamedScriptResourceCollector;
import org.apache.sling.servlets.resolver.internal.helper.ResourceCollector;
import org.apache.sling.servlets.resolver.internal.helper.SlingServletConfig;
import org.apache.sling.servlets.resolver.internal.resource.ServletResourceProvider;
import org.apache.sling.servlets.resolver.internal.resource.ServletResourceProviderFactory;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>SlingServletResolver</code> has two functions: It resolves scripts
 * by implementing the {@link SlingScriptResolver} interface and it resolves a
 * servlet for a request by implementing the {@link ServletResolver} interface.
 *
 * The resolver uses an own session to find the scripts.
 *
 * @scr.component name="org.apache.sling.servlets.resolver.SlingServletResolver"
 *                label="%servletresolver.name"
 *                description="%servletresolver.description"
 * @scr.property name="service.description" value="Sling Servlet Resolver and
 *               Error Handler"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.service interface="ServletResolver"
 * @scr.service interface="SlingScriptResolver"
 * @scr.service interface="ErrorHandler"
 * @scr.reference name="Servlet" interface="javax.servlet.Servlet"
 *                cardinality="0..n" policy="dynamic"
 * @scr.property name="event.topics" values.1="org/apache/sling/api/resource/*"
 *               values.2="javax/script/ScriptEngineFactory/*"
 *               values.3="org/apache/sling/api/adapter/AdapterFactory/*"
 *               private="true"
 */
public class SlingServletResolver implements ServletResolver, SlingScriptResolver, ErrorHandler, EventHandler {

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * @scr.property valueRef="DEFAULT_SERVLET_ROOT"
     */
    public static final String PROP_SERVLET_ROOT = "servletresolver.servletRoot";

    /**
     * @scr.property
     */
    public static final String PROP_SCRIPT_USER = "servletresolver.scriptUser";

    /**
     * @scr.property valueRef="DEFAULT_CACHE_SIZE"
     */
    public static final String PROP_CACHE_SIZE = "servletresolver.cacheSize";

    /**
     * @scr.property
     */
    public static final String PROP_DEFAULT_SCRIPT_WORKSPACE = "servletresolver.defaultScriptWorkspace";

    /**
     * @scr.property valueRef="DEFAULT_USE_REQUEST_WORKSPACE"
     */
    public static final String PROP_USE_REQUEST_WORKSPACE = "servletresolver.useRequestWorkspace";

    private static final boolean DEFAULT_USE_REQUEST_WORKSPACE = false;

    /**
     * @scr.property valueRef="DEFAULT_USE_DEFAULT_WORKSPACE"
     */
    public static final String PROP_USE_DEFAULT_WORKSPACE = "servletresolver.useDefaultWorkspace";

    private static final boolean DEFAULT_USE_DEFAULT_WORKSPACE = false;

    /**
     * The default servlet root is the first search path (which is usally /apps)
     */
    public static final String DEFAULT_SERVLET_ROOT = "0";

    /** The default cache size for the script resolution. */
    public static final Integer DEFAULT_CACHE_SIZE = 200;

    private static final String REF_SERVLET = "Servlet";

    /**
     * @scr.property values="/"
     */
    public static final String PROP_PATHS = "servletresolver.paths";

    private static final String[] DEFAULT_PATHS = new String[] {"/"};

    /**
     * @scr.property values="html"
     */
    public static final String PROP_DEFAULT_EXTENSIONS = "servletresolver.defaultExtensions";
    
    private static final String[] DEFAULT_DEFAULT_EXTENSIONS = new String[] {"html"};

    /** @scr.reference */
    private ServletContext servletContext;

    /** @scr.reference */
    private ResourceResolverFactory resourceResolverFactory;

    private ResourceResolver scriptResolver;

    private final Map<ServiceReference, ServletReg> servletsByReference = new HashMap<ServiceReference, ServletReg>();

    private final List<ServiceReference> pendingServlets = new ArrayList<ServiceReference>();

    /** The component context. */
    private ComponentContext context;

    private ServletResourceProviderFactory servletResourceProviderFactory;

    // the default servlet if no other servlet applies for a request. This
    // field is set on demand by getDefaultServlet()
    private Servlet defaultServlet;

    // the default error handler servlet if no other error servlet applies for
    // a request. This field is set on demand by getDefaultErrorServlet()
    private Servlet fallbackErrorServlet;

    /** The script resolution cache. */
    private Map<AbstractResourceCollector, Servlet> cache;

    /** The cache size. */
    private int cacheSize;

    /** Flag to log warning if cache size exceed only once. */
    private volatile boolean logCacheSizeWarning;

    /** Registration as event handler. */
    private ServiceRegistration eventHandlerReg;

    /**
     * If true, the primary workspace name for script resolution will be the
     * same as that used to resolve the request's resource.
     */
    private boolean useRequestWorkspace;

    /**
     * If true and useRequestWorkspace is true and no scripts are found using
     * the request workspace, also use the default workspace. If
     * useRequestWorkspace is false, this value is ignored.
     */
    private boolean useDefaultWorkspace;

    /**
     * The default workspace to use (might be null to use the default
     * workspace).
     */
    private String defaultWorkspaceName;

    /**
     * The allowed execution paths.
     */
    private String[] executionPaths;
    
    /**
     * The default extensions
     */
    private String[] defaultExtensions;

    // ---------- ServletResolver interface -----------------------------------

    /**
     * @see org.apache.sling.api.servlets.ServletResolver#resolveServlet(org.apache.sling.api.SlingHttpServletRequest)
     */
    public Servlet resolveServlet(final SlingHttpServletRequest request) {
        final Resource resource = request.getResource();

        // start tracking servlet resolution
        final RequestProgressTracker tracker = request.getRequestProgressTracker();
        final String timerName = "resolveServlet(" + resource + ")";
        tracker.startTimer(timerName);

        final String type = resource.getResourceType();
        if (log.isDebugEnabled()) {
            log.debug("resolveServlet called for resource {}", resource);
        }

        Servlet servlet = null;

        if (this.useRequestWorkspace) {
            final String wspName = getWorkspaceName(request);
            // First, we use a resource resolver using the same workspace as the
            // resource
            servlet = resolveServlet(request, type, scriptResolver, wspName);

            // now we try the default workspace
            if (servlet == null && this.useDefaultWorkspace && wspName != null ) {
                servlet = resolveServlet(request, type, scriptResolver, this.defaultWorkspaceName);
            }

        } else {
            servlet = resolveServlet(request, type, scriptResolver, this.defaultWorkspaceName);
        }

        // last resort, use the core bundle default servlet
        if (servlet == null) {
            if (log.isDebugEnabled()) {
                log.debug("No specific servlet found, trying default");
            }
            servlet = getDefaultServlet();
        }

        // track servlet resolution termination
        if (servlet == null) {
            tracker.logTimer(timerName, "Servlet resolution failed. See log for details");
        } else {
            tracker.logTimer(timerName, "Using servlet {0}", RequestUtil.getServletName(servlet));
        }

        // log the servlet found
        if (log.isDebugEnabled()) {
            if (servlet != null) {
                log.debug("Servlet {} found for resource={}", RequestUtil.getServletName(servlet), resource);
            } else {
                log.debug("No servlet found for resource={}", resource);
            }
        }

        return servlet;
    }

    /**
     * @see org.apache.sling.api.servlets.ServletResolver#resolveServlet(org.apache.sling.api.resource.Resource, java.lang.String)
     */
    public Servlet resolveServlet(final Resource resource, final String scriptName) {
        if ( resource == null ) {
            throw new IllegalArgumentException("Resource must not be null");
        }
        if (log.isDebugEnabled()) {
            log.debug("resolveServlet called for resource {} with script name {}", resource, scriptName);
        }

        final Servlet servlet = resolveServlet(scriptResolver, resource, scriptName);

        // log the servlet found
        if (log.isDebugEnabled()) {
            if (servlet != null) {
                log.debug("Servlet {} found for resource {} and script name {}", new Object[] {RequestUtil.getServletName(servlet), resource, scriptName});
            } else {
                log.debug("No servlet found for resource {} and script name {}", resource, scriptName);
            }
        }

        return servlet;
    }

    /**
     * @see org.apache.sling.api.servlets.ServletResolver#resolveServlet(org.apache.sling.api.resource.ResourceResolver, java.lang.String)
     */
    public Servlet resolveServlet(final ResourceResolver resolver, final String scriptName) {
        if ( resolver == null ) {
            throw new IllegalArgumentException("Resource resolver must not be null");
        }
        if (log.isDebugEnabled()) {
            log.debug("resolveServlet called for for script name {}", scriptName);
        }

        final Servlet servlet = resolveServlet(scriptResolver, null, scriptName);

        // log the servlet found
        if (log.isDebugEnabled()) {
            if (servlet != null) {
                log.debug("Servlet {} found for script name {}", RequestUtil.getServletName(servlet), scriptName);
            } else {
                log.debug("No servlet found for script name {}", scriptName);
            }
        }

        return servlet;
    }

    /** Internal method to resolve a servlet. */
    private Servlet resolveServlet(final ResourceResolver resolver,
            final Resource resource,
            final String scriptName) {
        Servlet servlet = null;

        // first check whether the type of a resource is the absolute
        // path of a servlet (or script)
        if (scriptName.charAt(0) == '/') {
            if ( this.isPathAllowed(scriptName) ) {
                final Resource res = resolver.getResource(scriptName);
                if (res != null) {
                    servlet = res.adaptTo(Servlet.class);
                }
                if (servlet != null && log.isDebugEnabled()) {
                    log.debug("Servlet {} found using absolute resource type {}", RequestUtil.getServletName(servlet),
                                    scriptName);
                }
            }
        }
        if ( servlet == null ) {
            // the resource type is not absolute, so lets go for the deep search
            final NamedScriptResourceCollector locationUtil = NamedScriptResourceCollector.create(scriptName, resource, this.executionPaths);
            servlet = getServlet(locationUtil, null, resolver);

            if (log.isDebugEnabled() && servlet != null) {
                log.debug("resolveServlet returns servlet {}", RequestUtil.getServletName(servlet));
            }
        }
        return servlet;

    }
    // ---------- ScriptResolver interface ------------------------------------

    /**
     * @see org.apache.sling.api.scripting.SlingScriptResolver#findScript(org.apache.sling.api.resource.ResourceResolver, java.lang.String)
     */
    public SlingScript findScript(final ResourceResolver resourceResolver, final String name)
    throws SlingException {

        // is the path absolute
        SlingScript script = null;
        if (name.startsWith("/")) {

            if ( this.isPathAllowed(name) ) {
                final Resource resource = resourceResolver.getResource(name);
                if (resource != null) {
                    script = resource.adaptTo(SlingScript.class);
                }
            }
        } else {

            // relative script resolution against search path
            final String[] path = resourceResolver.getSearchPath();
            for (int i = 0; script == null && i < path.length; i++) {
                final String scriptPath = path[i] + name;
                if ( this.isPathAllowed(scriptPath) ) {
                    final Resource resource = resourceResolver.getResource(scriptPath);
                    if (resource != null) {
                        script = resource.adaptTo(SlingScript.class);
                    }
                }
            }

        }

        // some logging
        if (script != null) {
            log.debug("findScript: Using script {} for {}", script.getScriptResource().getPath(), name);
        } else {
            log.info("findScript: No script {} found in path", name);
        }

        // and finally return the script (null or not)
        return script;
    }

    // ---------- ErrorHandler interface --------------------------------------

    /**
     * @see org.apache.sling.engine.servlets.ErrorHandler#handleError(int,
     *      String, SlingHttpServletRequest, SlingHttpServletResponse)
     */
    public void handleError(int status, String message, SlingHttpServletRequest request,
            SlingHttpServletResponse response) throws IOException {

        // do not handle, if already handling ....
        if (request.getAttribute(SlingConstants.ERROR_REQUEST_URI) != null) {
            log.error("handleError: Recursive invocation. Not further handling status " + status + "(" + message + ")");
            return;
        }

        // start tracker
        RequestProgressTracker tracker = request.getRequestProgressTracker();
        String timerName = "handleError:status=" + status;
        tracker.startTimer(timerName);

        try {
            final String wspName = (this.useRequestWorkspace ? getWorkspaceName(request) : null);

            // find the error handler component
            Resource resource = getErrorResource(request);

            // find a servlet for the status as the method name
            ResourceCollector locationUtil = new ResourceCollector(String.valueOf(status),
                    ServletResolverConstants.ERROR_HANDLER_PATH, resource, wspName,
                    this.executionPaths);
            Servlet servlet = getServlet(locationUtil, request, scriptResolver);

            // fall back to default servlet if none
            if (servlet == null) {
                servlet = getDefaultErrorServlet(request, scriptResolver,
                    resource, wspName);
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
            tracker.logTimer(timerName, "Using handler {0}", RequestUtil.getServletName(servlet));

            handleError(servlet, request, response);

        } finally {

            tracker.logTimer(timerName, "Error handler finished");

        }
    }

    public void handleError(Throwable throwable, SlingHttpServletRequest request, SlingHttpServletResponse response)
    throws IOException {
        // do not handle, if already handling ....
        if (request.getAttribute(SlingConstants.ERROR_REQUEST_URI) != null) {
            log.error("handleError: Recursive invocation. Not further handling Throwable:", throwable);
            return;
        }

        // start tracker
        RequestProgressTracker tracker = request.getRequestProgressTracker();
        String timerName = "handleError:throwable=" + throwable.getClass().getName();
        tracker.startTimer(timerName);

        try {
            final String wspName = (this.useRequestWorkspace ? getWorkspaceName(request) : null);

            // find the error handler component
            Servlet servlet = null;
            Resource resource = getErrorResource(request);

            Class<?> tClass = throwable.getClass();
            while (servlet == null && tClass != Object.class) {
                // find a servlet for the simple class name as the method name
                ResourceCollector locationUtil = new ResourceCollector(tClass.getSimpleName(),
                        ServletResolverConstants.ERROR_HANDLER_PATH, resource, wspName,
                        this.executionPaths);
                servlet = getServlet(locationUtil, request, scriptResolver);

                // go to the base class
                tClass = tClass.getSuperclass();
            }

            if (servlet == null) {
                servlet = getDefaultErrorServlet(request, scriptResolver,
                    resource, wspName);
            }

            // set the message properties
            request.setAttribute(SlingConstants.ERROR_EXCEPTION, throwable);
            request.setAttribute(SlingConstants.ERROR_EXCEPTION_TYPE, throwable.getClass());
            request.setAttribute(SlingConstants.ERROR_MESSAGE, throwable.getMessage());

            // log a track entry after resolution before calling the handler
            tracker.logTimer(timerName, "Using handler {0}", RequestUtil.getServletName(servlet));

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
            res = new SyntheticResource(request.getResourceResolver(), request.getPathInfo(),
                    ServletResolverConstants.ERROR_HANDLER_PATH);
        }
        return res;
    }

    /**
     * Resolve an appropriate servlet for a given request and resource type
     * using the provided ResourceResolver and workspace
     */
    private Servlet resolveServlet(final SlingHttpServletRequest request,
            final String type,
            final ResourceResolver resolver,
            final String workspaceName) {
        Servlet servlet = null;

        // first check whether the type of a resource is the absolute
        // path of a servlet (or script)
        if (type.charAt(0) == '/') {
            if ( this.isPathAllowed(type) ) {
                String path = type;
                if ( workspaceName != null ) {
                    path = workspaceName + ':' + type;
                }
                final Resource res = resolver.getResource(path);
                if (res != null) {
                    servlet = res.adaptTo(Servlet.class);
                }
                if (servlet != null && log.isDebugEnabled()) {
                    log.debug("Servlet {} found using absolute resource type {}", RequestUtil.getServletName(servlet),
                                    type);
                }
            }
        }
        if ( servlet == null ) {
            // the resource type is not absolute, so lets go for the deep search
            final ResourceCollector locationUtil = ResourceCollector.create(request, workspaceName, this.executionPaths, this.defaultExtensions);
            servlet = getServlet(locationUtil, request, resolver);

            if (log.isDebugEnabled()) {
                log.debug("getServlet returns servlet {}", RequestUtil.getServletName(servlet));
            }
        }
        return servlet;
    }

    /**
     * Returns a servlet suitable for handling a request. The
     * <code>locationUtil</code> is used find any servlets or scripts usable for
     * the request. Each servlet returned is in turn asked whether it is
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
    private Servlet getServlet(final AbstractResourceCollector locationUtil,
            final SlingHttpServletRequest request,
            final ResourceResolver scriptResolver) {
        final Servlet scriptServlet = (this.cache != null ? this.cache.get(locationUtil) : null);
        if (scriptServlet != null) {
            if ( log.isDebugEnabled() ) {
                log.debug("Using cached servlet {}", RequestUtil.getServletName(scriptServlet));
            }
            return scriptServlet;
        }

        final Collection<Resource> candidates = locationUtil.getServlets(scriptResolver);

        if (log.isDebugEnabled()) {
            if (candidates.isEmpty()) {
                log.debug("No servlet candidates found");
            } else {
                log.debug("Ordered list of servlet candidates follows");
                for (Resource candidateResource : candidates) {
                    log.debug("Servlet candidate: {}", candidateResource.getPath());
                }
            }
        }

        boolean hasOptingServlet = false;
        for (Resource candidateResource : candidates) {
            if (log.isDebugEnabled()) {
                log.debug("Checking if candidate resource {} adapts to servlet and accepts request", candidateResource
                        .getPath());
            }
            Servlet candidate = candidateResource.adaptTo(Servlet.class);
            if (candidate != null) {
                final boolean isOptingServlet = candidate instanceof OptingServlet;
                boolean servletAcceptsRequest = !isOptingServlet || (request != null && ((OptingServlet) candidate).accepts(request));
                if (servletAcceptsRequest) {
                    if (!hasOptingServlet && !isOptingServlet && this.cache != null) {
                        if ( this.cache.size() < this.cacheSize ) {
                            this.cache.put(locationUtil, candidate);
                        } else if ( this.logCacheSizeWarning ) {
                            this.logCacheSizeWarning = false;
                            log.warn("Script cache has reached its limit of {}. You might want to increase the cache size for the servlet resolver.",
                                    this.cacheSize);
                        }
                    }
                    log.debug("Using servlet provided by candidate resource {}", candidateResource.getPath());
                    return candidate;
                }
                if (isOptingServlet) {
                    hasOptingServlet = true;
                }
                if (log.isDebugEnabled()) {
                    log.debug("Candidate {} does not accept request, ignored", candidateResource.getPath());
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Candidate {} does not adapt to a servlet, ignored", candidateResource.getPath());
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
                servlet.init(new SlingServletConfig(servletContext, null, "Sling Core Default Servlet"));
                defaultServlet = servlet;
            } catch (ServletException se) {
                log.error("Failed to initialize default servlet", se);
            }
        }

        return defaultServlet;
    }

    /**
     * Returns the default error handler servlet, which is called in case there
     * is no other - better matching - servlet registered to handle an error or
     * exception.
     * <p>
     * The default error handler servlet is registered for the resource type
     * "sling/servlet/errorhandler" and method "default". This may be
     * overwritten by applications globally or according to the resource type
     * hierarchy of the resource.
     * <p>
     * If no default error handler servlet can be found an adhoc error handler
     * is used as a final fallback.
     */
    private Servlet getDefaultErrorServlet(
            final SlingHttpServletRequest request,
            final ResourceResolver scriptResolver,
            final Resource resource,
            final String workspaceName) {

        // find a default error handler according to the resource type
        // tree of the given resource
        final ResourceCollector locationUtil = new ResourceCollector(
            ServletResolverConstants.DEFAULT_ERROR_HANDLER_NAME,
            ServletResolverConstants.ERROR_HANDLER_PATH, resource,
            workspaceName,
            this.executionPaths);
        final Servlet servlet = getServlet(locationUtil, request,
            scriptResolver);
        if (servlet != null) {
            return servlet;
        }

        // if no registered default error handler could be found use
        // the DefaultErrorHandlerServlet as an ad-hoc fallback
        if (fallbackErrorServlet == null) {
            // fall back to an adhoc instance of the DefaultErrorHandlerServlet
            // if the actual service is not registered (yet ?)
            try {
                final Servlet defaultServlet = new DefaultErrorHandlerServlet();
                defaultServlet.init(new SlingServletConfig(servletContext,
                    null, "Sling (Ad Hoc) Default Error Handler Servlet"));
                fallbackErrorServlet = defaultServlet;
            } catch (ServletException se) {
                log.error("Failed to initialize error servlet", se);
            }
        }
        return fallbackErrorServlet;
    }

    private void handleError(Servlet errorHandler, HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        request.setAttribute(SlingConstants.ERROR_REQUEST_URI, request.getRequestURI());

        // if there is no explicitly known error causing servlet, use
        // the name of the error handler servlet
        if (request.getAttribute(SlingConstants.ERROR_SERVLET_NAME) == null) {
            request.setAttribute(SlingConstants.ERROR_SERVLET_NAME, errorHandler.getServletConfig().getServletName());
        }

        try {
            errorHandler.service(request, response);
        } catch (IOException ioe) {
            // forware the IOException
            throw ioe;
        } catch (Throwable t) {
            log.error("Calling the error handler resulted in an error", t);
            log.error("Original error " + request.getAttribute(SlingConstants.ERROR_EXCEPTION_TYPE),
                    (Throwable) request.getAttribute(SlingConstants.ERROR_EXCEPTION));
        }
    }

    /**
     * Package scoped to help with testing.
     */
    String getWorkspaceName(SlingHttpServletRequest request) {
        if ( this.useRequestWorkspace ) {
            final String path = request.getResource().getPath();
            final int pos = path.indexOf(":/");
            if ( pos == -1 ) {
                return null; // default workspace
            }
            return path.substring(0, pos);
        }
        return null;
    }

    private Map<String, Object> createAuthenticationInfo(final Dictionary<String, Object> props) {
        final Map<String, Object> authInfo = new HashMap<String, Object>();
        // if a script user is configured we use this user to read the scripts
        final String scriptUser = OsgiUtil.toString(props.get(PROP_SCRIPT_USER), null);
        if (scriptUser != null && scriptUser.length() > 0) {
            authInfo.put(ResourceResolverFactory.USER_IMPERSONATION, scriptUser);
        }
        return authInfo;
    }

    // ---------- SCR Integration ----------------------------------------------

    /**
     * Activate this component.
     */
    @SuppressWarnings("unchecked")
    protected void activate(final ComponentContext context) throws LoginException {
        // from configuration if available
        final Dictionary<?, ?> properties = context.getProperties();
        Object servletRoot = properties.get(PROP_SERVLET_ROOT);
        if (servletRoot == null) {
            servletRoot = DEFAULT_SERVLET_ROOT;
        }

        // workspace handling and resource resolver creation
        this.useDefaultWorkspace = OsgiUtil.toBoolean(properties.get(PROP_USE_DEFAULT_WORKSPACE), DEFAULT_USE_DEFAULT_WORKSPACE);
        this.useRequestWorkspace = OsgiUtil.toBoolean(properties.get(PROP_USE_REQUEST_WORKSPACE), DEFAULT_USE_REQUEST_WORKSPACE);

        String defaultWorkspaceProp = (String) properties.get(PROP_DEFAULT_SCRIPT_WORKSPACE);
        if ( defaultWorkspaceProp != null && defaultWorkspaceProp.trim().length() == 0 ) {
            defaultWorkspaceProp = null;
        }
        this.defaultWorkspaceName = defaultWorkspaceProp;


        final Collection<ServiceReference> refs;
        synchronized (this.pendingServlets) {

            refs = new ArrayList<ServiceReference>(pendingServlets);
            pendingServlets.clear();

            this.scriptResolver =
                    resourceResolverFactory.getAdministrativeResourceResolver(this.createAuthenticationInfo(context.getProperties()));

            servletResourceProviderFactory = new ServletResourceProviderFactory(servletRoot,
                    this.scriptResolver.getSearchPath());

            // register servlets immediately from now on
            this.context = context;
        }
        createAllServlets(refs);

        // execution paths
        this.executionPaths = OsgiUtil.toStringArray(properties.get(PROP_PATHS), DEFAULT_PATHS);
        if ( this.executionPaths != null ) {
            // if we find a string combination that basically allows all paths,
            // we simply set the array to null
            if ( this.executionPaths.length == 0 ) {
                this.executionPaths = null;
            } else {
                boolean hasRoot = false;
                for(int i = 0 ; i < this.executionPaths.length; i++) {
                    final String path = this.executionPaths[i];
                    if ( path == null || path.length() == 0 || path.equals("/") ) {
                        hasRoot = true;
                    }
                }
                if ( hasRoot ) {
                    this.executionPaths = null;
                }
            }
        }
        this.defaultExtensions = OsgiUtil.toStringArray(properties.get(PROP_DEFAULT_EXTENSIONS), DEFAULT_DEFAULT_EXTENSIONS);

        // create cache - if a cache size is configured
        this.cacheSize = OsgiUtil.toInteger(properties.get(PROP_CACHE_SIZE), DEFAULT_CACHE_SIZE);
        if (this.cacheSize > 5) {
            this.cache = new ConcurrentHashMap<AbstractResourceCollector, Servlet>(cacheSize);
            this.logCacheSizeWarning = true;
        } else {
            this.cacheSize = 0;
        }

        // and finally register as event listener
        this.eventHandlerReg = context.getBundleContext().registerService(EventHandler.class.getName(), this,
                properties);
    }

    /**
     * Deactivate this component.
     */
    protected void deactivate(final ComponentContext context) {
        // stop registering of servlets immediately
        this.context = null;

        // unregister event handler
        if (this.eventHandlerReg != null) {
            this.eventHandlerReg.unregister();
            this.eventHandlerReg = null;
        }

        // Copy the list of servlets first, to minimize the need for
        // synchronization
        final Collection<ServiceReference> refs;
        synchronized (this.servletsByReference) {
            refs = new ArrayList<ServiceReference>(servletsByReference.keySet());
        }
        // destroy all servlets
        destroyAllServlets(refs);

        // sanity check: clear array (it should be empty now anyway)
        synchronized ( this.servletsByReference ) {
            this.servletsByReference.clear();
        }

        // destroy the fallback error handler servlet
        if (fallbackErrorServlet != null) {
            try {
                fallbackErrorServlet.destroy();
            } catch (Throwable t) {
                // ignore
            } finally {
                fallbackErrorServlet = null;
            }
        }

        if (this.scriptResolver != null) {
            this.scriptResolver.close();
            this.scriptResolver = null;
        }

        this.cache = null;
        this.servletResourceProviderFactory = null;
    }

    protected void bindServlet(ServiceReference reference) {
        boolean directCreate = true;
        if (context == null) {
            synchronized ( pendingServlets ) {
                if (context == null) {
                    pendingServlets.add(reference);
                    directCreate = false;
                }
            }
        }
        if ( directCreate ) {
            createServlet(reference);
        }
    }

    protected void unbindServlet(ServiceReference reference) {
        synchronized ( pendingServlets ) {
            pendingServlets.remove(reference);
        }
        destroyServlet(reference);
    }

    // ---------- Servlet Management -------------------------------------------

    private void createAllServlets(final Collection<ServiceReference> pendingServlets) {
        for (final ServiceReference serviceReference : pendingServlets) {
            createServlet(serviceReference);
        }
    }

    private boolean createServlet(final ServiceReference reference) {

        // check for a name, this is required
        final String name = getName(reference);
        if (name == null) {
            log.error("bindServlet: Cannot register servlet {} without a servlet name", reference);
            return false;
        }

        // check for Sling properties in the service registration
        ServletResourceProvider provider = servletResourceProviderFactory.create(reference);
        if (provider == null) {
            // this is expected if the servlet is not destined for Sling
            return false;
        }

        // only now try to access the servlet service, this may still fail
        Servlet servlet = null;
        try {
            servlet = (Servlet) context.locateService(REF_SERVLET, reference);
        } catch (Throwable t) {
            log.warn("bindServlet: Failed getting the service for reference " + reference, t);
        }
        if (servlet == null) {
            log.error("bindServlet: Servlet service not available from reference {}", reference);
            return false;
        }

        // assign the servlet to the provider
        provider.setServlet(servlet);

        // initialize now
        try {
            servlet.init(new SlingServletConfig(servletContext, reference, name));
            log.debug("bindServlet: Servlet {} added", name);
        } catch (ServletException ce) {
            log.error("bindServlet: Component " + name + " failed to initialize", ce);
            return false;
        } catch (Throwable t) {
            log.error("bindServlet: Unexpected problem initializing component " + name, t);
            return false;
        }

        final Dictionary<String, Object> params = new Hashtable<String, Object>();
        params.put(ResourceProvider.ROOTS, provider.getServletPaths());
        params.put(Constants.SERVICE_DESCRIPTION, "ServletResourceProvider for Servlets at "
                + Arrays.asList(provider.getServletPaths()));

        final ServiceRegistration reg = context.getBundleContext()
                .registerService(ResourceProvider.SERVICE_NAME, provider, params);

        log.info("Registered {}", provider.toString());
        synchronized (this.servletsByReference) {
            servletsByReference.put(reference, new ServletReg(servlet, reg));
        }

        return true;
    }

    private void destroyAllServlets(Collection<ServiceReference> refs) {
        for (ServiceReference serviceReference : refs) {
            destroyServlet(serviceReference);
        }
    }

    private void destroyServlet(ServiceReference reference) {
        ServletReg registration;
        synchronized (this.servletsByReference) {
            registration = servletsByReference.remove(reference);
        }
        if (registration != null) {

            registration.registration.unregister();
            final String name = RequestUtil.getServletName(registration.servlet);
            log.debug("unbindServlet: Servlet {} removed", name);

            try {
                registration.servlet.destroy();
            } catch (Throwable t) {
                log.error("unbindServlet: Unexpected problem destroying servlet " + name, t);
            }
        }
    }

    /**
     * @see org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
     */
    public void handleEvent(Event event) {
        if (this.cache != null) {
            boolean flushCache = false;

            // we may receive different events
            final String topic = event.getTopic();
            if (topic.startsWith("javax/script/ScriptEngineFactory/")) {
                // script engine factory added or removed: we always flush
                flushCache = true;
            } else if (topic.startsWith("org/apache/sling/api/adapter/AdapterFactory/")) {
                // adapter factory added or removed: we always flush
                // as adapting might be transitive
                flushCache = true;
            } else {
                // this is a resource event

                // if the path of the event is a sub path of a search path
                // we flush the whole cache
                String path = (String) event.getProperty(SlingConstants.PROPERTY_PATH);
                if (path.contains(":")) {
                    path = path.substring(path.indexOf(":") + 1);
                }
                final String[] searchPaths = this.scriptResolver.getSearchPath();
                int index = 0;
                while (!flushCache && index < searchPaths.length) {
                    if (path.startsWith(searchPaths[index])) {
                        flushCache = true;
                    }
                    index++;
                }
            }
            if (flushCache) {
                this.cache.clear();
                this.logCacheSizeWarning = true;
            }
        }
    }

    /** The list of property names checked by {@link #getName(ServiceReference)} */
    private static final String[] NAME_PROPERTIES = { SLING_SERLVET_NAME,
        COMPONENT_NAME, SERVICE_PID, SERVICE_ID };

    /**
     * Looks for a name value in the service reference properties. See the
     * class comment at the top for the list of properties checked by this
     * method.
     */
    private static String getName(ServiceReference reference) {
        String servletName = null;
        for (int i = 0; i < NAME_PROPERTIES.length
            && (servletName == null || servletName.length() == 0); i++) {
            Object prop = reference.getProperty(NAME_PROPERTIES[i]);
            if (prop != null) {
                servletName = String.valueOf(prop);
            }
        }
        return servletName;
    }

    private boolean isPathAllowed(final String path) {
        return AbstractResourceCollector.isPathAllowed(path, this.executionPaths);
    }

    private static final class ServletReg {
        public final Servlet servlet;
        public final ServiceRegistration registration;

        public ServletReg(final Servlet s, final ServiceRegistration sr) {
            this.servlet = s;
            this.registration = sr;
        }
    }
}
