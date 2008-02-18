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
package org.apache.sling.core.impl;

import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static org.apache.sling.api.SlingConstants.ERROR_REQUEST_URI;
import static org.apache.sling.api.SlingConstants.ERROR_SERVLET_NAME;
import static org.apache.sling.core.CoreConstants.SESSION;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.SocketException;
import java.net.URL;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import javax.jcr.Session;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.GenericServlet;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.services.ServiceLocator;
import org.apache.sling.api.servlets.ServletResolver;
import org.apache.sling.commons.mime.MimeTypeService;
import org.apache.sling.core.impl.auth.MissingRepositoryException;
import org.apache.sling.core.impl.auth.SlingAuthenticator;
import org.apache.sling.core.impl.filter.RequestSlingFilterChain;
import org.apache.sling.core.impl.filter.SlingComponentFilterChain;
import org.apache.sling.core.impl.filter.SlingFilterChainHelper;
import org.apache.sling.core.impl.helper.SlingFilterConfig;
import org.apache.sling.core.impl.helper.SlingServletContext;
import org.apache.sling.core.impl.request.ContentData;
import org.apache.sling.core.impl.request.RequestData;
import org.apache.sling.core.impl.services.ServiceLocatorImpl;
import org.apache.sling.core.servlets.AbstractServiceReferenceConfig;
import org.apache.sling.core.servlets.ErrorHandler;
import org.apache.sling.jcr.resource.JcrResourceResolverFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>SlingMainServlet</code> TODO
 *
 * @scr.component immediate="true" label="%sling.name"
 *                description="%sling.description"
 * @scr.property name="sling.root" value="/" private="true"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.property name="service.description" value="Sling Servlet"
 * @scr.reference name="Filter" interface="javax.servlet.Filter"
 *                cardinality="0..n" policy="dynamic"
 */
public class SlingMainServlet extends GenericServlet implements ErrorHandler {

    /** default log */
    private static final Logger log = LoggerFactory.getLogger(SlingMainServlet.class);

    /**
     * The name of the product to report in the {@link #getServerInfo()} method
     * (value is "ApacheSling").
     */
    private static String PRODUCT_NAME = "ApacheSling";

    /**
     * The name of the Declarative Services reference to the Servlet API Filter
     * services (value is "Filter").
     */
    private static String FILTER_NAME = "Filter";

    private SlingServletContext slingServletContext;

    private ComponentContext osgiComponentContext;

    private List<ServiceReference> delayedComponentFilters;

    /**
     * The server information to report in the {@link #getServerInfo()} method.
     * By default this is just the {@link #PRODUCT_NAME}. The
     * {@link #activate(org.osgi.service.component.ComponentContext)} method
     * appends this value with the version (major and minor part) of this bundle
     * as well as the operating system and java version it is running on.
     */
    private String serverInfo = PRODUCT_NAME;

    /**
     * @scr.reference
     */
    private HttpService httpService;

    /** @scr.reference cardinality="0..1" policy="dynamic" */
    private JcrResourceResolverFactory resourceResolverFactory;

    /** @scr.reference cardinality="0..1" policy="dynamic" */
    private MimeTypeService mimeTypeService;

    /** @scr.reference cardinality="0..1" policy="dynamic" */
    private ServletResolver servletResolver;

    /** @scr.reference cardinality="0..1" policy="dynamic" */
    private ErrorHandler errorHandler;

    private SlingFilterChainHelper requestFilterChain = new SlingFilterChainHelper();

    private SlingFilterChainHelper innerFilterChain = new SlingFilterChainHelper();

    private String slingRoot;

    private ServiceLocatorImpl slingServiceLocator;

    private SlingAuthenticator slingAuthenticator;

    public static final String INCLUDE_COUNTER = "Sling.ScriptHelper.include.counter";
    public static final int MAX_INCLUDE_RECURSION_LEVEL = 50;

    public static class InfiniteIncludeLoopException extends SlingException {
        InfiniteIncludeLoopException(String path) {
            super("Infinite include loop (> " + MAX_INCLUDE_RECURSION_LEVEL + " levels) for path '" + path + "'");
        }
    }

    // ---------- Servlet API -------------------------------------------------

    public void service(ServletRequest req, ServletResponse res)
            throws ServletException {

        if (req instanceof HttpServletRequest
            && res instanceof HttpServletResponse) {

            try {

                // real request handling for HTTP requests
                service((HttpServletRequest) req, (HttpServletResponse) res);

            } catch (IOException ioe) {

                // unwrap any causes (Jetty wraps SocketException in
                // EofException)
                Throwable cause = ioe;
                while (cause.getCause() != null) {
                    cause = cause.getCause();
                }

                if (cause instanceof SocketException) {

                    // if the cause is a SocketException, the client most
                    // probably
                    // aborted the request, we do not fill the log with errors
                    // in this case
                    log.debug(
                        "service: Socketexception (Client abort or network problem",
                        ioe);

                } else {

                    // otherwise we want to know why the servlet failed
                    log.error(
                        "service: Uncaught IO Problem while handling the request",
                        ioe);
                }

            } catch (Throwable t) {

                // some failure while handling the request, log the issue
                // and terminate. We do not call error handling here, because
                // we assume the real request handling would have done this.
                // So here we just log

                log.error("service: Uncaught Problem handling the request", t);
            }

        } else {
            throw new ServletException(
                "Apache Sling must be run in an HTTP servlet environment.");
        }
    }

    // ---------- Request Handling on behalf of the servlet -------------------

    public void service(HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) throws IOException {

        // check that we have all required services
        String missing = null;
        if(getResourceResolverFactory() == null) {
            missing = "ResourceResolverFactory";
        } else if(getServletResolver() == null) {
            missing = "ServletResolver";
        } else if(mimeTypeService == null) {
            missing = "MimeTypeService";
        }

        if(missing != null) {
            final String err = missing + " service missing, cannot service requests";
            final int status = HttpServletResponse.SC_SERVICE_UNAVAILABLE;
            log.error(err + ", sending status " + status);
            sendError(status, err, null, servletRequest, servletResponse);
            return;
        }

        // get JCR Session
        Session session = (Session) servletRequest.getAttribute(SESSION);
        if (session == null) {
            log.error("service: Cannot handle request: Missing JCR Session");
            sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Missing JCR Session", null, servletRequest, servletResponse);
            return;
        }

        // setting the Sling request and response
        final RequestData requestData = new RequestData(this,
            getResourceResolverFactory().getResourceResolver(session),
            servletRequest, servletResponse);
        SlingHttpServletRequest request = requestData.getSlingRequest();
        SlingHttpServletResponse response = requestData.getSlingResponse();

        try {

            // initialize the request data - resolve resource and servlet
            requestData.init();

            Filter[] filters = requestFilterChain.getFilters();
            if (filters != null) {
                FilterChain processor = new RequestSlingFilterChain(this,
                    filters);

                processor.doFilter(request, response);

            } else {
                log.error("service: No Request Handling filters, cannot process request");
                response.sendError(SC_INTERNAL_SERVER_ERROR,
                    "Cannot process Request");
            }

        } catch (ResourceNotFoundException rnfe) {

            // send this exception as a 404 status
            log.info("service: Resource {} not found", rnfe.getResource());

            getErrorHandler().handleError(HttpServletResponse.SC_NOT_FOUND,
                rnfe.getMessage(), request, response);

        } catch (SlingException se) {

            // if we have request data and a non-null active servlet name
            // we assume, that this is the name of the causing servlet
            if (requestData.getActiveServletName() != null) {
                request.setAttribute(ERROR_SERVLET_NAME,
                    requestData.getActiveServletName());
            }

            // send this exception as is (albeit unwrapping and wrapped
            // exception.
            Throwable t = (se.getCause() != null) ? se.getCause() : se;
            log.error("service: Uncaught SlingException", t);
            getErrorHandler().handleError(t, request, response);

        } catch (AccessControlException ace) {

            // try to request authentication fail, if not possible
            log.info(
                "service: Authenticated user {} does not have enough rights to executed requested action",
                request.getRemoteUser());
            getSlingAuthenticator().requestAuthentication(request, response);

        } catch (Throwable t) {

            // if we have request data and a non-null active servlet name
            // we assume, that this is the name of the causing servlet
            if (requestData.getActiveServletName() != null) {
                request.setAttribute(ERROR_SERVLET_NAME,
                    requestData.getActiveServletName());
            }

            log.error("service: Uncaught Throwable", t);
            getErrorHandler().handleError(t, request, response);

        } finally {
            requestData.dispose();

            session.logout();
        }
    }

    // ---------- Generic Content Request processor ----------------------------

    public void includeServlet(ServletRequest request,
            ServletResponse response, String path) throws IOException,
            ServletException {

        checkRecursionLevel(request, path);

        // check type of response, don't care actually for the response itself
        RequestData.unwrap(response);

        // get the request data (and btw check the correct type
        RequestData requestData = RequestData.getRequestData(request);

        RequestDispatcher rd = requestData.getServletRequest().getRequestDispatcher(
            path);
        if (rd != null) {
            rd.include(request, response);
        } else {
            log.error("includeServlet: Got no request dispatcher for {}", path);
        }
    }

    public void includeContent(ServletRequest request,
            ServletResponse response, Resource resource,
            RequestPathInfo resolvedURL) throws IOException, ServletException {

        checkRecursionLevel(request, resolvedURL.getResourcePath());

        // we need a SlingHttpServletRequest/SlingHttpServletResponse tupel to continue
        SlingHttpServletRequest cRequest = RequestData.toSlingHttpServletRequest(request);
        SlingHttpServletResponse cResponse = RequestData.toSlingHttpServletResponse(response);

        // get the request data (and btw check the correct type)
        RequestData requestData = RequestData.getRequestData(cRequest);
        ContentData contentData = requestData.pushContent(resource, resolvedURL);

        try {
            // resolve the servlet
            Servlet servlet = getServletResolver().resolveServlet(cRequest);
            contentData.setServlet(servlet);

            processRequest(cRequest, cResponse);
        } finally {
            requestData.popContent();
        }
    }

    /** Add a recursion counter to req and fail if it's too high */
    protected void checkRecursionLevel(ServletRequest request, String info)  throws InfiniteIncludeLoopException {
        // Detect infinite loops
        Integer recursionLevel = (Integer)request.getAttribute(INCLUDE_COUNTER);
        if(recursionLevel == null) {
            recursionLevel = new Integer(1);
        } else if(recursionLevel.intValue() > MAX_INCLUDE_RECURSION_LEVEL) {
            throw new InfiniteIncludeLoopException(info);
        } else {
            recursionLevel = new Integer(recursionLevel.intValue() + 1);
        }
        request.setAttribute(INCLUDE_COUNTER, recursionLevel);
    }

    public void processRequest(SlingHttpServletRequest request,
            SlingHttpServletResponse response) throws IOException,
            ServletException {

        // 2.0 Set Content from mappedURL
        // ContentData contentData = null;

        Filter filters[] = innerFilterChain.getFilters();
        if (filters != null) {
            FilterChain processor = new SlingComponentFilterChain(filters);

            processor.doFilter(request, response);
        } else {
            log.debug("service: No Resource level filters, calling servlet");
            RequestData.service(request, response);
        }
    }

    // ---------- ErrorHandler interface (default implementation) --------------

    // reset the response, set the status and write a simple message
    public void handleError(int status, String message,
            SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws IOException {

        if (message == null) {
            message = "HTTP ERROR:" + String.valueOf(status);
        } else {
            message = "HTTP ERROR:" + status + " - " + message;
        }

        sendError(status, message, null, request, response);
    }

    // just rethrow the exception as explained in the class comment
    public void handleError(Throwable throwable,
            SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws IOException {
        sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
            throwable.getMessage(), throwable, request, response);
    }

    private void sendError(int status, String message, Throwable throwable,
            HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        if (response.isCommitted()) {
            log.error(
                "handleError: Response already committed; cannot send error "
                    + status + message, throwable);
        } else {

            // error situation
            String servletName = (String) request.getAttribute(ERROR_SERVLET_NAME);
            String requestURI = (String) request.getAttribute(ERROR_REQUEST_URI);
            if (requestURI == null) {
                requestURI = request.getRequestURI();
            }

            // reset anything in the response first
            response.reset();

            // set the status, content type and encoding
            response.setStatus(status);
            response.setContentType("text/html; charset=UTF-8");

            PrintWriter pw = response.getWriter();
            pw.println("<html><head><title>");
            pw.println(message);
            pw.println("</title></head><body><h1>");
            if(throwable != null) {
                pw.println(throwable.toString());
            } else if(message != null) {
                pw.println(message);
            } else {
                pw.println("Internal error (no Exception to report)");
            }
            pw.println("</h1><p>");
            pw.println("RequestURI=" + request.getRequestURI());
            if (servletName != null) {
                pw.println("</p>Servlet=" + servletName + "<p>");
            }
            pw.println("</p>");

            if (throwable != null) {
                pw.println("<pre>");
                throwable.printStackTrace(pw);
                pw.println("</pre>");
            }

            pw.println("<hr /><address>");
            pw.println(getServerInfo());
            pw.println("</address></body></html>");

            // commit the response
            response.flushBuffer();

        }
    }

    // ---------- Internal helper ----------------------------------------------

    public String getServerInfo() {
        return serverInfo;
    }

    public BundleContext getBundleContext() {
        return osgiComponentContext.getBundleContext();
    }

    public ServiceLocator getServiceLocator() {
        return slingServiceLocator;
    }

    public SlingAuthenticator getSlingAuthenticator() {
        return slingAuthenticator;
    }

    public JcrResourceResolverFactory getResourceResolverFactory() {
        return resourceResolverFactory;
    }

    public ServletResolver getServletResolver() {
        return servletResolver;
    }

    public ErrorHandler getErrorHandler() {
        ErrorHandler eh = errorHandler;
        return (eh != null) ? eh : this;
    }

    // ---------- Property Setter for SCR --------------------------------------

    protected void activate(ComponentContext componentContext) {

        osgiComponentContext = componentContext;

        // setup server info
        BundleContext bundleContext = componentContext.getBundleContext();
        Dictionary<?, ?> props = bundleContext.getBundle().getHeaders();
        Version bundleVersion = Version.parseVersion((String) props.get(Constants.BUNDLE_VERSION));
        String productVersion = bundleVersion.getMajor() + "."
            + bundleVersion.getMinor();
        this.serverInfo = PRODUCT_NAME + "/" + productVersion + " ("
            + System.getProperty("java.vm.name") + " "
            + System.getProperty("java.version") + "; "
            + System.getProperty("os.name") + " "
            + System.getProperty("os.version") + " "
            + System.getProperty("os.arch") + ")";

        // prepare the servlet configuration from the component config
        Hashtable<String, Object> configuration = new Hashtable<String, Object>();
        Dictionary<?, ?> componentConfig = componentContext.getProperties();
        for (Enumeration<?> cce = componentConfig.keys(); cce.hasMoreElements();) {
            Object key = cce.nextElement();
            configuration.put(String.valueOf(key), componentConfig.get(key));
        }

        // get the web manager root path
        Object wmr = configuration.get("sling.root");
        this.slingRoot = (wmr instanceof String) ? (String) wmr : null;
        if (this.slingRoot == null) {
            this.slingRoot = "/";
        } else if (!this.slingRoot.startsWith("/")) {
            this.slingRoot = "/" + this.slingRoot;
        }

        // ensure the servlet name
        if (!(configuration.get("servlet-name") instanceof String)) {
            configuration.put("servlet-name", PRODUCT_NAME + " "
                + productVersion);
        }

        // setup servlet request processing helpers
        SlingServletContext tmpServletContext = new SlingServletContext(this);
        slingServiceLocator = new ServiceLocatorImpl(bundleContext);
        slingAuthenticator = new SlingAuthenticator(bundleContext);

        // register the servlet and resources
        try {
            HttpContext httpContext = new HttpContext() {
                public String getMimeType(String name) {
                    return mimeTypeService.getMimeType(name);
                }

                public URL getResource(String name) {
                    return null;
                }

                public boolean handleSecurity(HttpServletRequest request,
                        HttpServletResponse response) {
                    try {
                        return slingAuthenticator.authenticate(request,
                            response);
                    } catch (MissingRepositoryException mre) {
                        log.error(
                            "handleSecurity: Cannot authenticate request", mre);
                        return false;
                    }
                }
            };

            Dictionary<String, String> servletConfig = toStringConfig(configuration);

            this.httpService.registerServlet(this.slingRoot, this,
                servletConfig, httpContext);

            log.info("{} ready to serve requests", this.getServerInfo());

        } catch (Exception e) {
            log.error("Cannot register " + this.getServerInfo(), e);
        }

        // register render filters already registered after registration with
        // the HttpService as filter initialization may cause the servlet
        // context to be required (see SLING-42)

        List<ServiceReference> filterList;
        synchronized (this) {
            filterList = delayedComponentFilters;

            // prepare the Sling Component Context now after having finished the
            // handler setup but before initializing the filters.
            // After leaving this synched block, bindFilter will be "active" and
            // set the delayedComponentFilters field to null for GC
            slingServletContext = tmpServletContext;
            delayedComponentFilters = null;
        }

        // if there are filters at all, initialize them now
        if (filterList != null) {
            for (ServiceReference serviceReference : filterList) {
                initFilter(componentContext, serviceReference);
            }
        }
    }

    protected void deactivate(ComponentContext componentContext) {

        // first of all, we have to unregister
        httpService.unregister(this.slingRoot);

        destroyFilters(innerFilterChain);
        destroyFilters(requestFilterChain);

        if (slingAuthenticator != null) {
            slingAuthenticator.dispose();
            slingAuthenticator = null;
        }
        if (slingServiceLocator != null) {
            slingServiceLocator.dispose();
            slingServiceLocator = null;
        }

        this.slingServletContext = null;
        this.osgiComponentContext = null;

        log.info(this.getServerInfo() + " shut down");
    }

    protected void bindFilter(ServiceReference ref) {
        synchronized (this) {
            if (slingServletContext == null) {
                if (delayedComponentFilters == null) {
                    delayedComponentFilters = new ArrayList<ServiceReference>();
                }
                delayedComponentFilters.add(ref);
            } else {
                initFilter(osgiComponentContext, ref);
            }
        }
    }

    protected void unbindFilter(ServiceReference ref) {
        // service id
        Object serviceId = ref.getProperty(Constants.SERVICE_ID);

        // unregister by scope and destroy it
        Filter filter = getChain(ref).removeFilterById(serviceId);
        if (filter != null) {
            try {
                filter.destroy();
            } catch (Throwable t) {
                log.error("Unexpected problem destroying ComponentFilter {}",
                    filter, t);
            }
        }
    }

    private void initFilter(ComponentContext osgiContext, ServiceReference ref) {
        Filter filter = (Filter) osgiContext.locateService(FILTER_NAME, ref);

        // require a name for the filter
        String filterName = AbstractServiceReferenceConfig.getName(ref);
        if (filterName == null) {
            log.error("initFilter: Missing name for filter {}", ref);
            return;
        }

        // initialize the filter first
        try {
            FilterConfig config = new SlingFilterConfig(slingServletContext,
                ref, filterName);
            filter.init(config);

            // service id
            Long serviceId = (Long) ref.getProperty(Constants.SERVICE_ID);

            // get the order, Integer.MAX_VALUE by default
            Object orderObj = ref.getProperty("filter.order");
            int order = (orderObj instanceof Integer)
                    ? ((Integer) orderObj).intValue()
                    : Integer.MAX_VALUE;

            // register by scope
            getChain(ref).addFilter(filter, serviceId, order);

        } catch (ServletException ce) {
            log.error("Filter " + filterName + " failed to initialize", ce);
        } catch (Throwable t) {
            log.error("Unexpected Problem initializing ComponentFilter " + "",
                t);
        }
    }

    private void destroyFilters(SlingFilterChainHelper chain) {
        Filter[] filters = chain.removeAllFilters();
        if (filters != null) {
            for (int i = 0; i < filters.length; i++) {
                try {
                    filters[i].destroy();
                } catch (Throwable t) {
                    log.error(
                        "Unexpected problem destroying ComponentFilter {}",
                        filters[i], t);
                }
            }
        }
    }

    private SlingFilterChainHelper getChain(ServiceReference ref) {

        // component rendering filter
        Object scope = ref.getProperty("filter.scope");
        if ("component".equals(scope)) {
            return innerFilterChain;
        }

        // global filter by default
        return requestFilterChain;
    }

    private Dictionary<String, String> toStringConfig(Dictionary<?, ?> config) {
        Dictionary<String, String> stringConfig = new Hashtable<String, String>();
        for (Enumeration<?> ke = config.keys(); ke.hasMoreElements();) {
            Object key = ke.nextElement();
            stringConfig.put(key.toString(), String.valueOf(config.get(key)));
        }
        return stringConfig;
    }

}
