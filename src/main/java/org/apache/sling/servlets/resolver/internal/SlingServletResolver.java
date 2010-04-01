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

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
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
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.scripting.SlingScript;
import org.apache.sling.api.scripting.SlingScriptResolver;
import org.apache.sling.api.servlets.OptingServlet;
import org.apache.sling.api.servlets.ServletResolver;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.engine.servlets.ErrorHandler;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.JcrResourceResolverFactory;
import org.apache.sling.servlets.resolver.internal.defaults.DefaultErrorHandlerServlet;
import org.apache.sling.servlets.resolver.internal.defaults.DefaultServlet;
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
     * @scr.property options true="True" false="False"
     */
    public static final String PROP_USE_REQUEST_WORKSPACE = "servletresolver.useRequestWorkspace";

    /**
     * @scr.property options true="True" false="False"
     */
    public static final String PROP_USE_DEFAULT_WORKSPACE = "servletresolver.useDefaultWorkspace";

    /**
     * The default servlet root is the first search path (which is usally /apps)
     */
    public static final String DEFAULT_SERVLET_ROOT = "0";

    /** The default cache size for the script resolution. */
    public static final Integer DEFAULT_CACHE_SIZE = 200;

    private static final String REF_SERVLET = "Servlet";

    /** @scr.reference */
    private ServletContext servletContext;

    /** @scr.reference */
    private JcrResourceResolverFactory resourceResolverFactory;

    /** @scr.reference */
    private SlingRepository repository;

    /** The session used for script resolution. */
    private ConcurrentHashMap<String, Session> scriptSessions;

    private Session defaultScriptSession;

    /** The resource resolver used for script resolution. */
    private ConcurrentHashMap<String, WorkspaceResourceResolver> scriptResolvers;

    private WorkspaceResourceResolver defaultScriptResolver;

    private String[] searchPath;

    private Map<ServiceReference, ServiceRegistration> servletsByReference = new HashMap<ServiceReference, ServiceRegistration>();

    private List<ServiceReference> pendingServlets = new ArrayList<ServiceReference>();

    /** The component context. */
    private ComponentContext context;

    private ServletResourceProviderFactory servletResourceProviderFactory;

    // the default servlet if no other servlet applies for a request. This
    // field is set on demand by getDefaultServlet()
    private Servlet defaultServlet;

    // the default error handler servlet if no other error servlet applies for
    // a request. This field is set on demand by getDefaultErrorServlet()
    private Servlet fallbackErrorServlet;

    private Map<ResourceCollector, Servlet> cache;

    private ServiceRegistration eventHandlerReg;

    /**
     * The workspace name which should be used as a default for script
     * resolution.
     */
    private String defaultScriptWorkspaceName;

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
        String wspName = getWorkspaceName(request);

        if (this.useRequestWorkspace) {
            // First, we use a resource resolver using the same workspace as the
            // resource
            WorkspaceResourceResolver scriptResolver = getScriptResolver(wspName);
            servlet = resolveServlet(request, type, scriptResolver);

            if (servlet == null && defaultScriptWorkspaceName != wspName && this.useDefaultWorkspace) {
                servlet = resolveServlet(request, type, defaultScriptResolver);
            }

        } else {
            servlet = resolveServlet(request, type, defaultScriptResolver);
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
                log.info("Servlet {} found for resource={}", RequestUtil.getServletName(servlet), resource);
            } else {
                log.debug("No servlet found for resource={}", resource);
            }
        }

        return servlet;
    }

    // ---------- ScriptResolver interface ------------------------------------

    public SlingScript findScript(ResourceResolver resourceResolver, String name) throws SlingException {

        // is the path absolute
        SlingScript script = null;
        if (name.startsWith("/")) {

            Resource resource = resourceResolver.getResource(name);
            if (resource != null) {
                script = resource.adaptTo(SlingScript.class);
            }

        } else {

            // relative script resolution against search path
            String[] path = resourceResolver.getSearchPath();
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
        String wspName = getWorkspaceName(request);
        WorkspaceResourceResolver scriptResolver = getScriptResolver(wspName);

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

            // find the error handler component
            Resource resource = getErrorResource(request);

            // find a servlet for the status as the method name
            ResourceCollector locationUtil = new ResourceCollector(String.valueOf(status),
                    ServletResolverConstants.ERROR_HANDLER_PATH, resource, scriptResolver.getWorkspaceName());
            Servlet servlet = getServlet(locationUtil, request, scriptResolver);

            // fall back to default servlet if none
            if (servlet == null) {
                servlet = getDefaultErrorServlet(request, scriptResolver,
                    resource);
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
        String wspName = getWorkspaceName(request);
        WorkspaceResourceResolver scriptResolver = getScriptResolver(wspName);

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

            // find the error handler component
            Servlet servlet = null;
            Resource resource = getErrorResource(request);

            Class<?> tClass = throwable.getClass();
            while (servlet == null && tClass != Object.class) {
                // find a servlet for the simple class name as the method name
                ResourceCollector locationUtil = new ResourceCollector(tClass.getSimpleName(),
                        ServletResolverConstants.ERROR_HANDLER_PATH, resource, scriptResolver.getWorkspaceName());
                servlet = getServlet(locationUtil, request, scriptResolver);

                // go to the base class
                tClass = tClass.getSuperclass();
            }

            if (servlet == null) {
                servlet = getDefaultErrorServlet(request, scriptResolver,
                    resource);
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
     * using the provided ResourceResolver.
     */
    private Servlet resolveServlet(final SlingHttpServletRequest request, final String type,
            final WorkspaceResourceResolver resolver) {
        Servlet servlet = null;

        // first check whether the type of a resource is the absolute
        // path of a servlet (or script)
        if (type.charAt(0) == '/') {
            final Resource res = resolver.getResource(type);
            if (res != null) {
                servlet = res.adaptTo(Servlet.class);
            }
            if (servlet != null && log.isDebugEnabled()) {
                log
                        .debug("Servlet {} found using absolute resource type {}", RequestUtil.getServletName(servlet),
                                type);
            }
        }

        // the resource type is not absolute, so lets go for the deep search
        if (servlet == null) {
            final ResourceCollector locationUtil = ResourceCollector.create(request, resolver.getWorkspaceName());
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
    private Servlet getServlet(final ResourceCollector locationUtil, final SlingHttpServletRequest request,
            final ResourceResolver scriptResolver) {
        final Servlet scriptServlet = (this.cache != null ? this.cache.get(locationUtil) : null);
        if (scriptServlet != null) {
            log.debug("Using cached servlet {}", scriptServlet.getClass().getName());
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
                boolean servletAcceptsRequest = !isOptingServlet || ((OptingServlet) candidate).accepts(request);
                if (servletAcceptsRequest) {
                    if (!hasOptingServlet && !isOptingServlet && this.cache != null) {
                        this.cache.put(locationUtil, candidate);
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
            final WorkspaceResourceResolver scriptResolver,
            final Resource resource) {

        // find a default error handler according to the resource type
        // tree of the given resource
        final ResourceCollector locationUtil = new ResourceCollector(
            ServletResolverConstants.DEFAULT_ERROR_HANDLER_NAME,
            ServletResolverConstants.ERROR_HANDLER_PATH, resource,
            scriptResolver.getWorkspaceName());
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
        return request.getResourceResolver().adaptTo(Session.class).getWorkspace().getName();
    }

    private WorkspaceResourceResolver getScriptResolver(String wspName) {
        if (wspName.equals(defaultScriptWorkspaceName)) {
            return defaultScriptResolver;
        }

        WorkspaceResourceResolver scriptResolver = scriptResolvers.get(wspName);
        if (scriptResolver != null) {
            return scriptResolver;
        }

        // create the script session
        Session scriptSession = createScriptSession(wspName);

        Session sessionFromMap = scriptSessions.putIfAbsent(wspName, scriptSession);
        if (sessionFromMap != null) {
            // another session was bound while this session was being created
            // abandon ship
            scriptSession.logout();
            scriptSession = sessionFromMap;
        }

        scriptResolver = new WorkspaceResourceResolver(this.resourceResolverFactory.getResourceResolver(scriptSession),
                wspName);
        WorkspaceResourceResolver resolverFromMap = scriptResolvers.putIfAbsent(wspName, scriptResolver);
        if (resolverFromMap != null) {
            // another resolver was bound while this resolver was being created
            // abandon ship
            scriptResolver = resolverFromMap;
        }

        return scriptResolver;
    }

    private Session createScriptSession(String wspName) {
        Session scriptSession = null;
        try {
            scriptSession = this.repository.loginAdministrative(wspName);
        } catch (RepositoryException e) {
            throw new SlingException("Unable to create new admin session.", e);
        }
        // if a script user is configured we use this user to read the scripts
        final String scriptUser = OsgiUtil.toString(context.getProperties().get(PROP_SCRIPT_USER), null);
        if (scriptUser != null && scriptUser.length() > 0) {
            Credentials creds = new SimpleCredentials(scriptUser, new char[0]);
            try {
                scriptSession = scriptSession.impersonate(creds);
            } catch (RepositoryException e) {
                throw new SlingException("Unable to impersonate to script user: " + scriptUser, e);
            }
        }
        return scriptSession;
    }

    // ---------- SCR Integration ----------------------------------------------

    protected void activate(ComponentContext context) {

        // from configuration if available
        Dictionary<?, ?> properties = context.getProperties();
        Object servletRoot = properties.get(PROP_SERVLET_ROOT);
        if (servletRoot == null) {
            servletRoot = DEFAULT_SERVLET_ROOT;
        }

        Collection<ServiceReference> refs;
        synchronized (this) {

            refs = pendingServlets;
            pendingServlets = new ArrayList<ServiceReference>();

            searchPath = this.resourceResolverFactory.getResourceResolver(null).getSearchPath();

            servletResourceProviderFactory = new ServletResourceProviderFactory(servletRoot, searchPath);

            // register servlets immediately from now on
            this.context = context;

        }

        this.useDefaultWorkspace = OsgiUtil.toBoolean(properties.get(PROP_USE_DEFAULT_WORKSPACE), false);
        this.useRequestWorkspace = OsgiUtil.toBoolean(properties.get(PROP_USE_REQUEST_WORKSPACE), false);

        this.scriptSessions = new ConcurrentHashMap<String, Session>();
        this.scriptResolvers = new ConcurrentHashMap<String, WorkspaceResourceResolver>();

        String defaultWorkspaceProp = (String) properties.get(PROP_DEFAULT_SCRIPT_WORKSPACE);
        this.defaultScriptSession = createScriptSession(defaultWorkspaceProp);

        // we load the workspaceName out of the session to ensure the value is
        // non-null
        this.defaultScriptWorkspaceName = this.defaultScriptSession.getWorkspace().getName();
        this.defaultScriptResolver = new WorkspaceResourceResolver(resourceResolverFactory
                .getResourceResolver(defaultScriptSession), defaultScriptWorkspaceName);

        // create cache - if a cache size is configured
        final int cacheSize = OsgiUtil.toInteger(properties.get(PROP_CACHE_SIZE), DEFAULT_CACHE_SIZE);
        if (cacheSize > 5) {
            this.cache = new ConcurrentHashMap<ResourceCollector, Servlet>(cacheSize);
        }
        createAllServlets(refs);

        // and finally register as event listener
        this.eventHandlerReg = this.context.getBundleContext().registerService(EventHandler.class.getName(), this,
                properties);
    }

    protected void deactivate(ComponentContext context) {
        // unregister event handler
        if (this.eventHandlerReg != null) {
            this.eventHandlerReg.unregister();
            this.eventHandlerReg = null;
        }

        // Copy the list of servlets first, to minimize the need for
        // synchronization
        Collection<ServiceReference> refs;
        synchronized (this) {
            refs = new ArrayList<ServiceReference>(servletsByReference.keySet());
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

        // destroy all servlets
        destroyAllServlets(refs);
        if (this.scriptSessions != null && (!this.scriptSessions.isEmpty())) {
            for (Session session : this.scriptSessions.values()) {
                session.logout();
            }
        }
        this.scriptSessions = null;
        this.scriptResolvers = null;

        if (this.defaultScriptSession != null) {
            this.defaultScriptSession.logout();
        }
        this.defaultScriptSession = null;
        this.defaultScriptResolver = null;

        this.context = null;
        this.cache = null;
        this.servletResourceProviderFactory = null;
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

    private boolean createServlet(ServletContext servletContext, ServiceReference reference) {

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

        Dictionary<String, Object> params = new Hashtable<String, Object>();
        params.put(ResourceProvider.ROOTS, provider.getServletPaths());
        params.put(Constants.SERVICE_DESCRIPTION, "ServletResourceProvider for Servlets at "
                + Arrays.asList(provider.getServletPaths()));

        ServiceRegistration reg = context.getBundleContext().registerService(ResourceProvider.SERVICE_NAME, provider,
                params);

        log.info("Registered {}", provider.toString());
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

            Servlet servlet = (Servlet) context.locateService(REF_SERVLET, reference);
            if (servlet == null) {
                log.error("destroyServlet: Servlet not found for reference {}", reference.toString());
            } else {
                String name = RequestUtil.getServletName(servlet);
                log.debug("unbindServlet: Servlet {} removed", name);

                try {
                    servlet.destroy();
                } catch (Throwable t) {
                    log.error("unbindServlet: Unexpected problem destroying servlet " + name, t);
                }
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
                final String path = (String) event.getProperty(SlingConstants.PROPERTY_PATH);
                final String[] searchPaths = this.searchPath;
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
}
