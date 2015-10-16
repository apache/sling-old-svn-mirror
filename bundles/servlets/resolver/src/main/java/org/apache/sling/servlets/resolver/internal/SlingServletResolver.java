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
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.SlingException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.request.RequestProgressTracker;
import org.apache.sling.api.request.RequestUtil;
import org.apache.sling.api.request.SlingRequestEvent;
import org.apache.sling.api.request.SlingRequestListener;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.scripting.SlingScript;
import org.apache.sling.api.scripting.SlingScriptResolver;
import org.apache.sling.api.servlets.OptingServlet;
import org.apache.sling.api.servlets.ServletResolver;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.engine.ResponseUtil;
import org.apache.sling.engine.servlets.ErrorHandler;
import org.apache.sling.servlets.resolver.internal.defaults.DefaultErrorHandlerServlet;
import org.apache.sling.servlets.resolver.internal.defaults.DefaultServlet;
import org.apache.sling.servlets.resolver.internal.helper.AbstractResourceCollector;
import org.apache.sling.servlets.resolver.internal.helper.NamedScriptResourceCollector;
import org.apache.sling.servlets.resolver.internal.helper.ResourceCollector;
import org.apache.sling.servlets.resolver.internal.helper.SlingServletConfig;
import org.apache.sling.servlets.resolver.internal.resource.ServletResourceProvider;
import org.apache.sling.servlets.resolver.internal.resource.ServletResourceProviderFactory;
import org.apache.sling.servlets.resolver.jmx.SlingServletResolverCacheMBean;
import org.osgi.framework.BundleContext;
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
 */
@Component(name="org.apache.sling.servlets.resolver.SlingServletResolver", metatype=true,
           label="%servletresolver.name", description="%servletresolver.description")
@Service(value={ServletResolver.class, SlingScriptResolver.class, ErrorHandler.class, SlingRequestListener.class})
@Properties({
    @Property(name="service.description", value="Sling Servlet Resolver and Error Handler"),
    @Property(name="event.topics", propertyPrivate=true,
         value={"org/apache/sling/api/resource/Resource/*",
                    "org/apache/sling/api/resource/ResourceProvider/*",
                    "javax/script/ScriptEngineFactory/*",
                    "org/apache/sling/api/adapter/AdapterFactory/*",
                    "org/apache/sling/scripting/core/BindingsValuesProvider/*"})
})
@Reference(name="Servlet", referenceInterface=javax.servlet.Servlet.class,
           cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE, policy=ReferencePolicy.DYNAMIC)
public class SlingServletResolver
    implements ServletResolver,
               SlingScriptResolver,
               SlingRequestListener,
               ErrorHandler,
               EventHandler {

    /**
     * The default servlet root is the first search path (which is usally /apps)
     */
    public static final String DEFAULT_SERVLET_ROOT = "0";

    /** The default cache size for the script resolution. */
    public static final int DEFAULT_CACHE_SIZE = 200;

    /** Servlet resolver logger */
    public static final Logger LOGGER = LoggerFactory.getLogger(SlingServletResolver.class);

    @Property(value=DEFAULT_SERVLET_ROOT)
    public static final String PROP_SERVLET_ROOT = "servletresolver.servletRoot";

    @Property
    public static final String PROP_SCRIPT_USER = "servletresolver.scriptUser";

    @Property(intValue=DEFAULT_CACHE_SIZE)
    public static final String PROP_CACHE_SIZE = "servletresolver.cacheSize";

    private static final String REF_SERVLET = "Servlet";

    @Property(value="/", unbounded=PropertyUnbounded.ARRAY)
    public static final String PROP_PATHS = "servletresolver.paths";

    private static final String[] DEFAULT_PATHS = new String[] {"/"};

    @Property(value="html", unbounded=PropertyUnbounded.ARRAY)
    public static final String PROP_DEFAULT_EXTENSIONS = "servletresolver.defaultExtensions";

    private static final String[] DEFAULT_DEFAULT_EXTENSIONS = new String[] {"html"};

    @Reference
    private ServletContext servletContext;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    private ResourceResolver sharedScriptResolver;

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
     * The allowed execution paths.
     */
    private String[] executionPaths;

    /**
     * The search paths
     */
    private String[] searchPaths;

    /**
     * The default extensions
     */
    private String[] defaultExtensions;

    private ServletResolverWebConsolePlugin plugin;

    // ---------- ServletResolver interface -----------------------------------

    /**
     * @see org.apache.sling.api.servlets.ServletResolver#resolveServlet(org.apache.sling.api.SlingHttpServletRequest)
     */
    @Override
    public Servlet resolveServlet(final SlingHttpServletRequest request) {
        final Resource resource = request.getResource();

        // start tracking servlet resolution
        final RequestProgressTracker tracker = request.getRequestProgressTracker();
        final String timerName = "resolveServlet(" + resource.getPath() + ")";
        tracker.startTimer(timerName);

        final String type = resource.getResourceType();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("resolveServlet called for resource {}", resource);
        }

        final ResourceResolver scriptResolver = this.getScriptResourceResolver();
        Servlet servlet = null;

        if ( type != null && type.length() > 0 ) {
            servlet = resolveServletInternal(request, null, type, scriptResolver);
        }

        // last resort, use the core bundle default servlet
        if (servlet == null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("No specific servlet found, trying default");
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
        if (LOGGER.isDebugEnabled()) {
            if (servlet != null) {
                LOGGER.debug("Servlet {} found for resource={}", RequestUtil.getServletName(servlet), resource);
            } else {
                LOGGER.debug("No servlet found for resource={}", resource);
            }
        }

        return servlet;
    }

    /**
     * @see org.apache.sling.api.servlets.ServletResolver#resolveServlet(org.apache.sling.api.resource.Resource, java.lang.String)
     */
    @Override
    public Servlet resolveServlet(final Resource resource, final String scriptName) {
        if ( resource == null ) {
            throw new IllegalArgumentException("Resource must not be null");
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("resolveServlet called for resource {} with script name {}", resource, scriptName);
        }

        final ResourceResolver scriptResolver = this.getScriptResourceResolver();
        final Servlet servlet = resolveServletInternal(null, resource, scriptName, scriptResolver);

        // log the servlet found
        if (LOGGER.isDebugEnabled()) {
            if (servlet != null) {
                LOGGER.debug("Servlet {} found for resource {} and script name {}", new Object[] {RequestUtil.getServletName(servlet), resource, scriptName});
            } else {
                LOGGER.debug("No servlet found for resource {} and script name {}", resource, scriptName);
            }
        }

        return servlet;
    }

    /**
     * @see org.apache.sling.api.servlets.ServletResolver#resolveServlet(org.apache.sling.api.resource.ResourceResolver, java.lang.String)
     */
    @Override
    public Servlet resolveServlet(final ResourceResolver resolver, final String scriptName) {
        if ( resolver == null ) {
            throw new IllegalArgumentException("Resource resolver must not be null");
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("resolveServlet called for for script name {}", scriptName);
        }

        final ResourceResolver scriptResolver = this.getScriptResourceResolver();
        final Servlet servlet = resolveServletInternal(null, (Resource)null, scriptName, scriptResolver);

        // log the servlet found
        if (LOGGER.isDebugEnabled()) {
            if (servlet != null) {
                LOGGER.debug("Servlet {} found for script name {}", RequestUtil.getServletName(servlet), scriptName);
            } else {
                LOGGER.debug("No servlet found for script name {}", scriptName);
            }
        }

        return servlet;
    }

    /**
     * Get the servlet for the resource.
     */
    private Servlet getServlet(final Resource scriptResource) {
        // no resource -> no servlet
        if ( scriptResource == null ) {
            return null;
        }
        // if resource is fetched using shared resource resolver
        // or resource is a servlet resource, just adapt to servlet
        if ( scriptResource.getResourceResolver() == this.sharedScriptResolver
             || "sling/bundle/resource".equals(scriptResource.getResourceSuperType()) ) {
            return scriptResource.adaptTo(Servlet.class);
        }
        // return a resource wrapper to make sure the implementation
        // switches from the per thread resource resolver to the shared once
        // the per thread resource resolver is closed
        return new ScriptResource(scriptResource, perThreadScriptResolver, this.sharedScriptResolver).adaptTo(Servlet.class);
    }

    // ---------- ScriptResolver interface ------------------------------------

    /**
     * @see org.apache.sling.api.scripting.SlingScriptResolver#findScript(org.apache.sling.api.resource.ResourceResolver, java.lang.String)
     */
    @Override
    public SlingScript findScript(final ResourceResolver resourceResolver, final String name)
    throws SlingException {

        // is the path absolute
        SlingScript script = null;
        if (name.startsWith("/")) {

            final String path = ResourceUtil.normalize(name);
            if ( this.isPathAllowed(path) ) {
                final Resource resource = resourceResolver.getResource(path);
                if ( resource != null ) {
                    script = resource.adaptTo(SlingScript.class);
                }
            }
        } else {

            // relative script resolution against search path
            final String[] path = resourceResolver.getSearchPath();
            for (int i = 0; script == null && i < path.length; i++) {
                final String scriptPath = ResourceUtil.normalize(path[i] + name);
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
            LOGGER.debug("findScript: Using script {} for {}", script.getScriptResource().getPath(), name);
        } else {
            LOGGER.info("findScript: No script {} found in path", name);
        }

        // and finally return the script (null or not)
        return script;
    }

    // ---------- ErrorHandler interface --------------------------------------

    /**
     * @see org.apache.sling.engine.servlets.ErrorHandler#handleError(int,
     *      String, SlingHttpServletRequest, SlingHttpServletResponse)
     */
    @Override
    public void handleError(final int status,
            final String message,
            final SlingHttpServletRequest request,
            final SlingHttpServletResponse response) throws IOException {

        // do not handle, if already handling ....
        if (request.getAttribute(SlingConstants.ERROR_REQUEST_URI) != null) {
            LOGGER.error("handleError: Recursive invocation. Not further handling status " + status + "(" + message + ")");
            return;
        }

        // start tracker
        RequestProgressTracker tracker = request.getRequestProgressTracker();
        String timerName = "handleError:status=" + status;
        tracker.startTimer(timerName);

        final ResourceResolver scriptResolver = this.getScriptResourceResolver();
        try {
            // find the error handler component
            Resource resource = getErrorResource(request);

            // find a servlet for the status as the method name
            ResourceCollector locationUtil = new ResourceCollector(String.valueOf(status),
                    ServletResolverConstants.ERROR_HANDLER_PATH, resource,
                    this.executionPaths);
            Servlet servlet = getServletInternal(locationUtil, request, scriptResolver);

            // fall back to default servlet if none
            if (servlet == null) {
                servlet = getDefaultErrorServlet(request, resource, scriptResolver);
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

    /**
     * @see org.apache.sling.engine.servlets.ErrorHandler#handleError(java.lang.Throwable, org.apache.sling.api.SlingHttpServletRequest, org.apache.sling.api.SlingHttpServletResponse)
     */
    @Override
    public void handleError(final Throwable throwable, final SlingHttpServletRequest request, final SlingHttpServletResponse response)
    throws IOException {
        // do not handle, if already handling ....
        if (request.getAttribute(SlingConstants.ERROR_REQUEST_URI) != null) {
            LOGGER.error("handleError: Recursive invocation. Not further handling Throwable:", throwable);
            return;
        }

        // start tracker
        RequestProgressTracker tracker = request.getRequestProgressTracker();
        String timerName = "handleError:throwable=" + throwable.getClass().getName();
        tracker.startTimer(timerName);

        final ResourceResolver scriptResolver = this.getScriptResourceResolver();
        try {
            // find the error handler component
            Servlet servlet = null;
            Resource resource = getErrorResource(request);

            Class<?> tClass = throwable.getClass();
            while (servlet == null && tClass != Object.class) {
                // find a servlet for the simple class name as the method name
                ResourceCollector locationUtil = new ResourceCollector(tClass.getSimpleName(),
                        ServletResolverConstants.ERROR_HANDLER_PATH, resource,
                        this.executionPaths);
                servlet = getServletInternal(locationUtil, request, scriptResolver);

                // go to the base class
                tClass = tClass.getSuperclass();
            }

            if (servlet == null) {
                servlet = getDefaultErrorServlet(request, resource, scriptResolver);
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

    private ResourceResolver getScriptResourceResolver() {
        ResourceResolver scriptResolver = this.perThreadScriptResolver.get();
        if ( scriptResolver == null ) {
            // no per thread, let's use the shared one
            synchronized ( this.sharedScriptResolver ) {
                this.sharedScriptResolver.refresh();
            }
            scriptResolver = this.sharedScriptResolver;
        }
        return scriptResolver;
    }

    private final ThreadLocal<ResourceResolver> perThreadScriptResolver = new ThreadLocal<ResourceResolver>();

    private ServiceRegistration mbeanRegistration;

    /**
     * @see org.apache.sling.api.request.SlingRequestListener#onEvent(org.apache.sling.api.request.SlingRequestEvent)
     */
    @Override
    public void onEvent(final SlingRequestEvent event) {
        if ( event.getType() == SlingRequestEvent.EventType.EVENT_INIT ) {
            try {
                this.perThreadScriptResolver.set(this.sharedScriptResolver.clone(null));
            } catch (final LoginException e) {
                LOGGER.error("Unable to create new script resolver clone", e);
            }
        } else if ( event.getType() == SlingRequestEvent.EventType.EVENT_DESTROY ) {
            final ResourceResolver resolver = this.perThreadScriptResolver.get();
            if ( resolver != null ) {
                this.perThreadScriptResolver.remove();
                resolver.close();
            }
        }
    }

    /**
     * Returns the resource of the given request to be used as the basis for
     * error handling. If the resource has not yet been set in the request
     * because the error occurred before the resource could be set (e.g. during
     * resource resolution) a synthetic resource is returned whose type is
     * {@link ServletResolverConstants#ERROR_HANDLER_PATH}.
     *
     * @param request The request whose resource is to be returned.
     */
    private Resource getErrorResource(final SlingHttpServletRequest request) {
        Resource res = request.getResource();
        if (res == null) {
            res = new SyntheticResource(request.getResourceResolver(), request.getPathInfo(),
                    ServletResolverConstants.ERROR_HANDLER_PATH);
        }
        return res;
    }

     /**
     * Resolve an appropriate servlet for a given request and resource type
     * using the provided ResourceResolver
     */
    private Servlet resolveServletInternal(final SlingHttpServletRequest request,
            final Resource resource,
            final String scriptName,
            final ResourceResolver resolver) {
        Servlet servlet = null;

        // first check whether the type of a resource is the absolute
        // path of a servlet (or script)
        if (scriptName.charAt(0) == '/') {
            final String scriptPath = ResourceUtil.normalize(scriptName);
            if ( this.isPathAllowed(scriptPath) ) {
                final Resource res = resolver.getResource(scriptPath);
                servlet = this.getServlet(res);
                if (servlet != null && LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Servlet {} found using absolute resource type {}", RequestUtil.getServletName(servlet),
                                    scriptName);
                }
            } else {
                if ( request != null ) {
                    request.getRequestProgressTracker().log(
                            "Will not look for a servlet at {0} as it is not in the list of allowed paths",
                            scriptName
                            );
                }
            }
        }
        if ( servlet == null ) {
            // the resource type is not absolute, so lets go for the deep search
            final AbstractResourceCollector locationUtil;
            if ( request != null ) {
                locationUtil = ResourceCollector.create(request, this.executionPaths, this.defaultExtensions);
            } else {
                locationUtil = NamedScriptResourceCollector.create(scriptName, resource, this.executionPaths);
            }
            servlet = getServletInternal(locationUtil, request, resolver);

            if (servlet != null && LOGGER.isDebugEnabled()) {
                LOGGER.debug("getServletInternal returns servlet {}", RequestUtil.getServletName(servlet));
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
     * @param resolver The <code>ResourceResolver</code> used for resolving the servlets.
     * @return a servlet for handling the request or <code>null</code> if no
     *         such servlet willing to handle the request could be found.
     */
    private Servlet getServletInternal(final AbstractResourceCollector locationUtil,
            final SlingHttpServletRequest request,
            final ResourceResolver resolver) {
        final Servlet scriptServlet = (this.cache != null ? this.cache.get(locationUtil) : null);
        if (scriptServlet != null) {
            if ( LOGGER.isDebugEnabled() ) {
                LOGGER.debug("Using cached servlet {}", RequestUtil.getServletName(scriptServlet));
            }
            return scriptServlet;
        }

        final Collection<Resource> candidates = locationUtil.getServlets(resolver);

        if (LOGGER.isDebugEnabled()) {
            if (candidates.isEmpty()) {
                LOGGER.debug("No servlet candidates found");
            } else {
                LOGGER.debug("Ordered list of servlet candidates follows");
                for (Resource candidateResource : candidates) {
                    LOGGER.debug("Servlet candidate: {}", candidateResource.getPath());
                }
            }
        }

        boolean hasOptingServlet = false;
        for (final Resource candidateResource : candidates) {
            LOGGER.debug("Checking if candidate resource {} adapts to servlet and accepts request", candidateResource
                        .getPath());
            Servlet candidate = this.getServlet(candidateResource);
            if (candidate != null) {
                final boolean isOptingServlet = candidate instanceof OptingServlet;
                boolean servletAcceptsRequest = !isOptingServlet || (request != null && ((OptingServlet) candidate).accepts(request));
                if (servletAcceptsRequest) {
                    if (!hasOptingServlet && !isOptingServlet && this.cache != null) {
                        if ( this.cache.size() < this.cacheSize ) {
                            this.cache.put(locationUtil, candidate);
                        } else if ( this.logCacheSizeWarning ) {
                            this.logCacheSizeWarning = false;
                            LOGGER.warn("Script cache has reached its limit of {}. You might want to increase the cache size for the servlet resolver.",
                                    this.cacheSize);
                        }
                    }
                    LOGGER.debug("Using servlet provided by candidate resource {}", candidateResource.getPath());
                    return candidate;
                }
                if (isOptingServlet) {
                    hasOptingServlet = true;
                }
                LOGGER.debug("Candidate {} does not accept request, ignored", candidateResource.getPath());
            } else {
                LOGGER.debug("Candidate {} does not adapt to a servlet, ignored", candidateResource.getPath());
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
                servlet.init(new SlingServletConfig(servletContext, null, "Apache Sling Core Default Servlet"));
                defaultServlet = servlet;
            } catch (final ServletException se) {
                LOGGER.error("Failed to initialize default servlet", se);
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
            final Resource resource,
            final ResourceResolver resolver) {

        // find a default error handler according to the resource type
        // tree of the given resource
        final ResourceCollector locationUtil = new ResourceCollector(
            ServletResolverConstants.DEFAULT_ERROR_HANDLER_NAME,
            ServletResolverConstants.ERROR_HANDLER_PATH, resource,
            this.executionPaths);
        final Servlet servlet = getServletInternal(locationUtil, request, resolver);
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
                LOGGER.error("Failed to initialize error servlet", se);
            }
        }
        return fallbackErrorServlet;
    }

    private void handleError(final Servlet errorHandler, final HttpServletRequest request, final HttpServletResponse response)
            throws IOException {

        request.setAttribute(SlingConstants.ERROR_REQUEST_URI, request.getRequestURI());

        // if there is no explicitly known error causing servlet, use
        // the name of the error handler servlet
        if (request.getAttribute(SlingConstants.ERROR_SERVLET_NAME) == null) {
            request.setAttribute(SlingConstants.ERROR_SERVLET_NAME, errorHandler.getServletConfig().getServletName());
        }

        // Let the error handler servlet process the request and
        // forward all exceptions if it fails.
        // Before SLING-4143 we only forwarded IOExceptions.
        try {
            errorHandler.service(request, response);
            // commit the response
            response.flushBuffer();
            // close the response (SLING-2724)
            response.getWriter().close();
        } catch (final Throwable t) {
            LOGGER.error("Calling the error handler resulted in an error", t);
            LOGGER.error("Original error " + request.getAttribute(SlingConstants.ERROR_EXCEPTION_TYPE),
                    (Throwable) request.getAttribute(SlingConstants.ERROR_EXCEPTION));
            final IOException x = new IOException("Error handler failed: " + t.getClass().getName());
            x.initCause(t);
            throw x;
        }
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

        final Collection<ServiceReference> refs;
        synchronized (this.pendingServlets) {

            refs = new ArrayList<ServiceReference>(pendingServlets);
            pendingServlets.clear();

            this.sharedScriptResolver =
                    resourceResolverFactory.getAdministrativeResourceResolver(this.createAuthenticationInfo(context.getProperties()));
            this.searchPaths = this.sharedScriptResolver.getSearchPath();
            servletResourceProviderFactory = new ServletResourceProviderFactory(servletRoot, this.searchPaths);

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
                        break;
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

        // setup default servlet
        this.getDefaultServlet();

        // and finally register as event listener
        this.eventHandlerReg = context.getBundleContext().registerService(EventHandler.class.getName(), this,
                properties);

        this.plugin = new ServletResolverWebConsolePlugin(context.getBundleContext());

        if (this.cacheSize > 0) {
            try {
                Dictionary<String, String> mbeanProps = new Hashtable<String, String>();
                mbeanProps.put("jmx.objectname", "org.apache.sling:type=servletResolver,service=SlingServletResolverCache");

                ServletResolverCacheMBeanImpl mbean = new ServletResolverCacheMBeanImpl();
                mbeanRegistration = context.getBundleContext().registerService(SlingServletResolverCacheMBean.class.getName(), mbean, mbeanProps);
            } catch (Throwable t) {
                LOGGER.debug("Unable to register mbean");
            }
        }
    }

    /**
     * Deactivate this component.
     */
    protected void deactivate(final ComponentContext context) {
        // stop registering of servlets immediately
        this.context = null;

        if (this.plugin != null) {
            this.plugin.dispose();
        }

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

        if (this.sharedScriptResolver != null) {
            this.sharedScriptResolver.close();
            this.sharedScriptResolver = null;
        }

        this.cache = null;
        this.servletResourceProviderFactory = null;

        if (this.mbeanRegistration != null) {
            this.mbeanRegistration.unregister();
            this.mbeanRegistration = null;
        }
    }

    protected void bindServlet(final ServiceReference reference) {
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

    protected void unbindServlet(final ServiceReference reference) {
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
            LOGGER.error("bindServlet: Cannot register servlet {} without a servlet name", reference);
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
            LOGGER.warn("bindServlet: Failed getting the service for reference " + reference, t);
        }
        if (servlet == null) {
            LOGGER.error("bindServlet: Servlet service not available from reference {}", reference);
            return false;
        }

        // assign the servlet to the provider
        provider.setServlet(servlet);

        // initialize now
        try {
            servlet.init(new SlingServletConfig(servletContext, reference, name));
            LOGGER.debug("bindServlet: Servlet {} added", name);
        } catch (ServletException ce) {
            LOGGER.error("bindServlet: Component " + name + " failed to initialize", ce);
            return false;
        } catch (Throwable t) {
            LOGGER.error("bindServlet: Unexpected problem initializing component " + name, t);
            return false;
        }

        final ServiceRegistration reg = context.getBundleContext().registerService(
            ResourceProvider.SERVICE_NAME,
            provider,
            createServiceProperties(reference, provider));

        LOGGER.debug("Registered {}", provider.toString());
        synchronized (this.servletsByReference) {
            servletsByReference.put(reference, new ServletReg(servlet, reg));
        }

        return true;
    }

    private Dictionary<String, Object> createServiceProperties(final ServiceReference reference,
            final ServletResourceProvider provider) {

        final Dictionary<String, Object> params = new Hashtable<String, Object>();
        params.put(ResourceProvider.ROOTS, provider.getServletPaths());
        params.put(Constants.SERVICE_DESCRIPTION,
            "ServletResourceProvider for Servlets at " + Arrays.asList(provider.getServletPaths()));

        // inherit service ranking
        Object rank = reference.getProperty(Constants.SERVICE_RANKING);
        if (rank instanceof Integer) {
            params.put(Constants.SERVICE_RANKING, rank);
        }

        return params;
    }

    private void destroyAllServlets(final Collection<ServiceReference> refs) {
        for (ServiceReference serviceReference : refs) {
            destroyServlet(serviceReference);
        }
    }

    private void destroyServlet(final ServiceReference reference) {
        ServletReg registration;
        synchronized (this.servletsByReference) {
            registration = servletsByReference.remove(reference);
        }
        if (registration != null) {

            registration.registration.unregister();
            final String name = RequestUtil.getServletName(registration.servlet);
            LOGGER.debug("unbindServlet: Servlet {} removed", name);

            try {
                registration.servlet.destroy();
            } catch (Throwable t) {
                LOGGER.error("unbindServlet: Unexpected problem destroying servlet " + name, t);
            }
        }
    }

    /**
     * @see org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
     */
    @Override
    public void handleEvent(final Event event) {
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
            } else if (topic.startsWith("org/apache/sling/scripting/core/BindingsValuesProvider/")) {
                // bindings values provide factory added or removed: we always flush
                flushCache = true;
            } else {
                // this is a resource or resource provider event

                // if the path of the event is a sub path of a search path
                // we flush the whole cache
                final String path = (String) event.getProperty(SlingConstants.PROPERTY_PATH);
                if ( path != null ) {
                    int index = 0;
                    while (!flushCache && index < searchPaths.length) {
                        if (path.startsWith(this.searchPaths[index])) {
                            flushCache = true;
                        }
                        index++;
                    }
                }
            }
            if (flushCache) {
                flushCache();
            }
        }
    }

    private void flushCache() {
        this.cache.clear();
        this.logCacheSizeWarning = true;
    }

    /** The list of property names checked by {@link #getName(ServiceReference)} */
    private static final String[] NAME_PROPERTIES = { SLING_SERLVET_NAME,
        COMPONENT_NAME, SERVICE_PID, SERVICE_ID };

    /**
     * Looks for a name value in the service reference properties. See the
     * class comment at the top for the list of properties checked by this
     * method.
     */
    private static String getName(final ServiceReference reference) {
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

    @SuppressWarnings("serial")
    class ServletResolverWebConsolePlugin extends HttpServlet {
        private static final String PARAMETER_URL = "url";
        private static final String PARAMETER_METHOD = "method";

        private ServiceRegistration service;

        public ServletResolverWebConsolePlugin(final BundleContext context) {
            Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put(Constants.SERVICE_DESCRIPTION,
                    "Sling Servlet Resolver Web Console Plugin");
            props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
            props.put(Constants.SERVICE_PID, getClass().getName());
            props.put("felix.webconsole.label", "servletresolver");
            props.put("felix.webconsole.title", "Sling Servlet Resolver");
            props.put("felix.webconsole.css", "/servletresolver/res/ui/styles.css");
            props.put("felix.webconsole.category", "Sling");

            service = context.registerService(
                    new String[] { "javax.servlet.Servlet" }, this, props);
        }

        public void dispose() {
            if (service != null) {
                service.unregister();
                service = null;
            }
        }

        @Override
        protected void service(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
            final String url = request.getParameter(PARAMETER_URL);
            final RequestPathInfo requestPathInfo = new DecomposedURL(url).getRequestPathInfo();
            String method = request.getParameter(PARAMETER_METHOD);
            if (StringUtils.isBlank(method)) {
                method = "GET";
            }

            final String CONSOLE_PATH_WARNING =
                    "<em>"
                    + "Note that in a real Sling request, the path might vary depending on the existence of"
                    + " resources that partially match it."
                    + "<br/>This utility does not take this into account and uses the first dot to split"
                    + " between path and selectors/extension."
                    + "<br/>As a workaround, you can replace dots with underline characters, for example, when testing such an URL."
                    + "</em>";

            ResourceResolver resourceResolver = null;
            try {
                resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);

                final PrintWriter pw = response.getWriter();

                pw.print("<form method='get'>");
                pw.println("<table class='content' cellpadding='0' cellspacing='0' width='100%'>");

                titleHtml(
                        pw,
                        "Servlet Resolver Test",
                        "To check which servlet is responsible for rendering a response, enter a request path into " +
                                 "the field and click 'Resolve' to resolve it.");

                tr(pw);
                tdLabel(pw, "URL");
                tdContent(pw);

                pw.print("<input type='text' name='");
                pw.print(PARAMETER_URL);
                pw.print("' value='");
                if ( url != null ) {
                    pw.print(ResponseUtil.escapeXml(url));
                }
                pw.println("' class='input' size='50'>");
                closeTd(pw);
                closeTr(pw);
                closeTr(pw);

                tr(pw);
                tdLabel(pw, "Method");
                tdContent(pw);
                pw.print("<select name='");
                pw.print(PARAMETER_METHOD);
                pw.println("'>");
                pw.println("<option value='GET'>GET</option>");
                pw.println("<option value='POST'>POST</option>");
                pw.println("</select>");
                pw.println("&nbsp;&nbsp;<input type='submit' value='Resolve' class='submit'>");

                closeTd(pw);
                closeTr(pw);

                if (StringUtils.isNotBlank(url)) {
                    tr(pw);
                    tdLabel(pw, "Decomposed URL");
                    tdContent(pw);
                    pw.println("<dl>");
                    pw.println("<dt>Path</dt>");
                    pw.print("<dd>");
                    pw.print(ResponseUtil.escapeXml(requestPathInfo.getResourcePath()));
                    pw.print("<br/>");
                    pw.print(CONSOLE_PATH_WARNING);
                    pw.println("</dd>");
                    pw.println("<dt>Selectors</dt>");
                    pw.print("<dd>");
                    if (requestPathInfo.getSelectors().length == 0) {
                        pw.print("&lt;none&gt;");
                    } else {
                        pw.print("[");
                        pw.print(ResponseUtil.escapeXml(StringUtils.join(requestPathInfo.getSelectors(), ", ")));
                        pw.print("]");
                    }
                    pw.println("</dd>");
                    pw.println("<dt>Extension</dt>");
                    pw.print("<dd>");
                    pw.print(ResponseUtil.escapeXml(requestPathInfo.getExtension()));
                    pw.println("</dd>");
                    pw.println("</dl>");
                    pw.println("</dd>");
                    pw.println("<dt>Suffix</dt>");
                    pw.print("<dd>");
                    pw.print(ResponseUtil.escapeXml(requestPathInfo.getSuffix()));
                    pw.println("</dd>");
                    pw.println("</dl>");
                    closeTd(pw);
                    closeTr(pw);
                }

                if (StringUtils.isNotBlank(requestPathInfo.getResourcePath())) {
                    final Collection<Resource> servlets;
                    Resource resource = resourceResolver.resolve(requestPathInfo.getResourcePath());
                    if (resource.adaptTo(Servlet.class) != null) {
                        servlets = Collections.singleton(resource);
                    } else {
                        final ResourceCollector locationUtil = ResourceCollector.create(
                                resource,
                                requestPathInfo.getExtension(),
                                executionPaths,
                                defaultExtensions,
                                method,
                                requestPathInfo.getSelectors());
                        servlets = locationUtil.getServlets(resourceResolver);
                    }
                    tr(pw);
                    tdLabel(pw, "Candidates");
                    tdContent(pw);

                    if (servlets == null || servlets.isEmpty()) {
                        pw.println("Could not find a suitable servlet for this request!");
                    } else {
                        pw.print("Candidate servlets and scripts in order of preference for method ");
                        pw.print(ResponseUtil.escapeXml(method));
                        pw.println(":<br/>");
                        pw.println("<ol class='servlets'>");
                        outputServlets(pw, servlets.iterator());
                        pw.println("</ol>");
                    }
                    pw.println("</td>");
                    closeTr(pw);
                }

                pw.println("</table>");
                pw.print("</form>");
            } catch (LoginException e) {
                throw new ServletException(e);
            } finally {
                if (resourceResolver != null) {
                    resourceResolver.close();
                }
            }
        }

        private void tdContent(final PrintWriter pw) {
            pw.print("<td class='content' colspan='2'>");
        }

        private void closeTd(final PrintWriter pw) {
            pw.print("</td>");
        }

        @SuppressWarnings("unused")
        private URL getResource(final String path) {
            if (path.startsWith("/servletresolver/res/ui")) {
                return this.getClass().getResource(path.substring(16));
            } else {
                return null;
            }
        }

        private void closeTr(final PrintWriter pw) {
            pw.println("</tr>");
        }

        private void tdLabel(final PrintWriter pw, final String label) {
            pw.print("<td class='content'>");
            pw.print(ResponseUtil.escapeXml(label));
            pw.println("</td>");
        }

        private void tr(final PrintWriter pw) {
            pw.println("<tr class='content'>");
        }

        private void outputServlets(final PrintWriter pw, final Iterator<Resource> iterator) {
            while (iterator.hasNext()) {
                Resource candidateResource = iterator.next();
                Servlet candidate = candidateResource.adaptTo(Servlet.class);
                if (candidate != null) {
                    final boolean allowed = isPathAllowed(candidateResource.getPath());
                    pw.print("<li>");
                    if ( !allowed ) {
                        pw.print("<del>");
                    }

                    if (candidate instanceof SlingScript) {
                        pw.print(ResponseUtil.escapeXml(candidateResource.getPath()));
                    } else {
                        final boolean isOptingServlet = candidate instanceof OptingServlet;
                        pw.print(ResponseUtil.escapeXml((candidate.getClass().getName())));
                        if ( isOptingServlet ) {
                            pw.print(" (OptingServlet)");
                        }
                    }

                    if ( !allowed ) {
                        pw.print("</del>");
                    }
                    pw.println("</li>");
                }
            }
        }

        private void titleHtml(final PrintWriter pw, final String title, final String description) {
            tr(pw);
            pw.print("<th colspan='3' class='content container'>");
            pw.print(ResponseUtil.escapeXml(title));
            pw.println("</th>");
            closeTr(pw);

            if (description != null) {
                tr(pw);
                pw.print("<td colspan='3' class='content'>");
                pw.print(ResponseUtil.escapeXml(description));
                pw.println("</th>");
                closeTr(pw);
            }
        }

    }

    class ServletResolverCacheMBeanImpl extends StandardMBean implements SlingServletResolverCacheMBean {

        ServletResolverCacheMBeanImpl() throws NotCompliantMBeanException {
            super(SlingServletResolverCacheMBean.class);
        }

        @Override
        public int getCacheSize() {
            return cache != null ? cache.size() : 0;
        }

        @Override
        public void flushCache() {
            SlingServletResolver.this.flushCache();
        }

        @Override
        public int getMaximumCacheSize() {
            return cacheSize;
        }

    }
}
