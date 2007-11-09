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

import java.io.IOException;
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
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.HttpStatusCodeException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.services.ServiceLocator;
import org.apache.sling.api.servlets.ServletResolver;
import org.apache.sling.commons.mime.MimeTypeService;
import org.apache.sling.core.impl.auth.SlingAuthenticator;
import org.apache.sling.core.impl.filter.ErrorHandler;
import org.apache.sling.core.impl.filter.RequestSlingFilterChain;
import org.apache.sling.core.impl.filter.SlingComponentFilterChain;
import org.apache.sling.core.impl.filter.SlingFilterChainHelper;
import org.apache.sling.core.impl.filter.SlingServletResolver;
import org.apache.sling.core.impl.helper.AbstractServiceReferenceConfig;
import org.apache.sling.core.impl.helper.RequestData;
import org.apache.sling.core.impl.helper.SlingFilterConfig;
import org.apache.sling.core.impl.helper.SlingServletContext;
import org.apache.sling.core.impl.services.ServiceLocatorImpl;
import org.apache.sling.jcr.resource.JcrResourceManagerFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
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
 * @scr.reference name="ComponentFilter"
 *                interface="org.apache.sling.component.ComponentFilter"
 *                cardinality="0..n" policy="dynamic"
 */
public class SlingMainServlet extends GenericServlet {

    /** default log */
    private static final Logger log = LoggerFactory.getLogger(SlingMainServlet.class);

    /**
     * The name of the product to report in the {@link #getServerInfo()} method
     * (value is "Apache Sling").
     */
    private static String PRODUCT_NAME = "Apache Sling";

    /**
     * The name of the Declarative Services reference to the ComponentFilter
     * services (value is "ComponentFilter").
     */
    private static String COMPONENT_FILTER_NAME = "ComponentFilter";

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
    private JcrResourceManagerFactory resourceManagerFactory;

    /** @scr.reference cardinality="0..1" policy="dynamic" */
    private MimeTypeService mimeTypeService;

    private SlingHttpContext slingHttpContext;

    private SlingFilterChainHelper requestFilterChain = new SlingFilterChainHelper();

    private SlingFilterChainHelper innerFilterChain = new SlingFilterChainHelper();

    private String slingRoot;

    private ServiceLocatorImpl slingServiceLocator;

    private SlingAuthenticator slingAuthenticator;

    private SlingServletResolver servletResolver;

    private ErrorHandler errorHandler;

    // ---------- Servlet API -------------------------------------------------

    public void service(ServletRequest req, ServletResponse res)
            throws ServletException, IOException {

        if (req instanceof HttpServletRequest
            && res instanceof HttpServletResponse) {

            this.service((HttpServletRequest) req, (HttpServletResponse) res);
        } else {
            throw new ServletException(
                "Apache Sling must be run in an HTTP servlet environment.");
        }
    }

    // ---------- Request Handling on behalf of the servlet -------------------

    public void service(HttpServletRequest clientRequest,
            HttpServletResponse clientResponse) throws IOException {

        Session session = getSlingAuthenticator().authenticate(clientRequest,
            clientResponse);
        if (session != null) {
            RequestData requestData = null;
            try {

                // prepare internal request stuff
                requestData = new RequestData(this, session, clientRequest,
                    clientResponse);
                SlingHttpServletRequest request = requestData.getSlingRequest();
                SlingHttpServletResponse response = requestData.getSlingResponse();

                Filter[] filters = requestFilterChain.getFilters();
                if (filters != null) {
                    FilterChain processor = new RequestSlingFilterChain(this,
                        filters);

                    processor.doFilter(request, response);

                } else {
                    log.error("service: No Request Handling filters, cannot process request");
                    response.sendError(
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Cannot process Request");
                }

            } catch (AccessControlException ace) {

                // try to request authentication fail, if not possible
                if (!getSlingAuthenticator().requestAuthentication(
                    clientRequest, clientResponse)) {
                    getErrorHandler().handleError(
                        HttpServletResponse.SC_FORBIDDEN, "Access Denied",
                        clientRequest, clientResponse);
                }

            } catch (HttpStatusCodeException hsce) {
                // convert the status code exception to sendError
                getErrorHandler().handleError(hsce.getStatusCode(),
                    hsce.getMessage(), clientRequest, clientResponse);

            } catch (Throwable t) {
                getErrorHandler().handleError(t, clientRequest, clientResponse);

            } finally {
                if (requestData != null) {
                    requestData.dispose();
                }

                session.logout();
            }
        }

    }

    // ---------- Generic Content Request processor ----------------------------

    public void includeServlet(ServletRequest request,
            ServletResponse response, String path) throws IOException,
            ComponentException {

        // check type of response, don't care actually for the response itself
        RequestData.unwrap(response);

        // get the request data (and btw check the correct type
        RequestData requestData = RequestData.getRequestData(request);

        RequestDispatcher rd = requestData.getServletRequest().getRequestDispatcher(
            path);
        if (rd != null) {
            try {
                rd.include(request, response);
            } catch (ServletException se) {
                throw new ComponentException(se.getMessage(), se);
            }
        } else {
            throw new ComponentException("Got no request dispatcher for "
                + path);
        }
    }

    public void includeContent(ServletRequest request,
            ServletResponse response, Resource resource,
            RequestPathInfo resolvedURL) throws IOException, ComponentException {

        // we need a ComponentRequest/ComponentResponse tupel to continue
        SlingHttpServletRequest cRequest = RequestData.toSlingHttpServletRequest(request);
        SlingHttpServletResponse cResponse = RequestData.toSlingHttpServletResponse(response);

        // get the request data (and btw check the correct type)
        RequestData requestData = RequestData.getRequestData(cRequest);
        requestData.pushContent(resource, resolvedURL);

        try {
            processRequest(cRequest, cResponse);
        } catch (ServletException se) {
            throw new ComponentException(se.getMessage(), se);
        } finally {
            requestData.popContent();
        }
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
            log.error("service: No Inner Request Handling filters, cannot process request");
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Cannot process Request");
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

    public JcrResourceManagerFactory getResourceManagerFactory() {
        return resourceManagerFactory;
    }

    public ServletResolver getServletResolver() {
        return servletResolver;
    }

    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }

    // ---------- Property Setter for SCR --------------------------------------

    protected void activate(ComponentContext componentContext) {

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
        this.slingServiceLocator = new ServiceLocatorImpl(bundleContext);
        this.slingAuthenticator = new SlingAuthenticator(bundleContext);
        this.servletResolver = new SlingServletResolver(bundleContext, slingServletContext);
        this.errorHandler = new ErrorHandler(bundleContext, slingServletContext);

        // register the servlet and resources
        try {
            this.slingHttpContext = new SlingHttpContext(this.mimeTypeService);
            this.httpService.registerServlet(this.slingRoot, this,
                configuration, this.slingHttpContext);

            log.info("{} ready to serve requests", this.getServerInfo());

        } catch (Exception e) {
            log.error("Cannot register " + this.getServerInfo(), e);
        }

        // prepare the Sling Component Context now after having finished the
        // handler setup but before initializing the filters
        this.slingServletContext = new SlingServletContext(this);

        // register render filters already registered after registration with
        // the HttpService as filter initialization may cause the servlet
        // context to be required (see SLING-42)

        List<ServiceReference> filterList;
        synchronized (this) {
            filterList = delayedComponentFilters;

            // assign the OSGi Component Context now, after leaving this
            // synched block, bindFilter will be "active" and set the
            // delayedComponentFilters field to null for GC
            osgiComponentContext = componentContext;
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

        destroyFilters(innerFilterChain);
        destroyFilters(requestFilterChain);

        if (errorHandler != null) {
            errorHandler.dispose();
            errorHandler = null;
        }
        if (servletResolver != null) {
            servletResolver.dispose();
            servletResolver = null;
        }
        if (slingAuthenticator != null) {
            slingAuthenticator.dispose();
            slingAuthenticator = null;
        }
        if (slingServiceLocator != null) {
            slingServiceLocator.dispose();
            slingServiceLocator = null;
        }

        this.httpService.unregister(this.slingRoot);

        if (this.slingHttpContext != null) {
            this.slingHttpContext.dispose();
        }

        this.slingServletContext = null;
        this.osgiComponentContext = null;

        log.info(this.getServerInfo() + " shut down");
    }

    protected void bindComponentFilter(ServiceReference ref) {
        synchronized (this) {
            if (osgiComponentContext == null) {
                if (delayedComponentFilters == null) {
                    delayedComponentFilters = new ArrayList<ServiceReference>();
                }
                delayedComponentFilters.add(ref);
            } else {
                initFilter(osgiComponentContext, ref);
            }
        }
    }

    protected void unbindComponentFilter(ServiceReference ref) {
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
        Filter filter = (Filter) osgiContext.locateService(
            COMPONENT_FILTER_NAME, ref);

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

        } catch (ComponentException ce) {
            log.error("ComponentFilter " + "" + " failed to initialize", ce);
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
}
