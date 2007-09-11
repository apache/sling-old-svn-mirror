/*
 * Copyright 2007 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.core.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import javax.servlet.GenericServlet;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.component.ComponentContext;
import org.apache.sling.component.ComponentException;
import org.apache.sling.component.ComponentFilter;
import org.apache.sling.component.ComponentFilterChain;
import org.apache.sling.component.ComponentRequest;
import org.apache.sling.component.ComponentResponse;
import org.apache.sling.component.Content;
import org.apache.sling.core.impl.filter.ComponentFilterChainHelper;
import org.apache.sling.mime.MimeTypeService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>ComponentRequestHandlerImpl</code> TODO
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
public class ComponentRequestHandlerImpl extends GenericServlet {

    /** default log */
    private static final Logger log = LoggerFactory.getLogger(ComponentRequestHandlerImpl.class);

    /**
     * The name of the product to report in the {@link #getServerInfo()} method
     * (value is "Apache Sling").
     */
    private static String PRODUCT_NAME = "Apache Sling";

    private ComponentContext componentContext;

    private BundleContext bundleContext;

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
    private MimeTypeService mimeTypeService;

    private SlingHttpContext slingHttpContext;

    private ComponentFilterChainHelper requestFilterChain = new ComponentFilterChainHelper();

    private ComponentFilterChainHelper innerFilterChain = new ComponentFilterChainHelper();

    private String slingRoot;

    // ---------- Servlet API -------------------------------------------------

    public void service(ServletRequest req, ServletResponse res)
            throws ServletException, IOException {

        if (req instanceof HttpServletRequest
            && res instanceof HttpServletResponse) {

            this.service((HttpServletRequest) req, (HttpServletResponse) res);
        } else {
            throw new ServletException(
                "Sling must be run in an HTTP Servlet Environment");
        }
    }

    // ---------- Request Handling on behalf of the servlet -------------------

    public void service(HttpServletRequest clientRequest,
            HttpServletResponse clientResponse) throws IOException,
            ServletException {

        // prepare internal request stuff
        RequestData requestData = new RequestData(clientRequest, clientResponse);
        ComponentRequestImpl request = new ComponentRequestImpl(requestData);
        ComponentResponseImpl response = new ComponentResponseImpl(requestData);

        ComponentFilter[] filters = this.requestFilterChain.getFilters();
        if (filters != null) {
            ComponentFilterChain processor = new RequestComponentFilterChain(
                this, filters);

            try {
                processor.doFilter(request, response);
            } catch (IOException ioe) {
                log.error("service: IO problem handling the request", ioe);
                throw ioe;
            } catch (ComponentException ce) {
                log.error("service: Component problem handling the request", ce);
                throw ce;
            } catch (Throwable t) {
                // otherwise send an internal server error
                log.error("service: Unexpected problem handling the request", t);
                response.sendError(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Unexpected problem handling the request");
            }
        } else {
            log.error("service: No Request Handling filters, cannot process request");
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Cannot process Request");
        }
    }

    // ---------- Generic Content Request processor ----------------------------

    /* package */void includeServlet(ComponentRequest request,
            ComponentResponse response, String path) throws IOException,
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

    /* package */void includeContent(ComponentRequest request,
            ComponentResponse response, Content content) throws IOException,
            ComponentException {

        // check type of response, don't care actually for the response itself
        RequestData.unwrap(response);

        // get the request data (and btw check the correct type
        RequestData requestData = RequestData.getRequestData(request);

        requestData.pushContent(content);
        try {
            this.processRequest(request, response);
        } catch (ServletException se) {
            throw new ComponentException(se.getMessage(), se);
        } finally {
            requestData.popContent();
        }
    }

    void processRequest(ComponentRequest request, ComponentResponse response)
            throws IOException, ComponentException {

        // 2.0 Set Content from mappedURL
        // ContentData contentData = null;

        ComponentFilter filters[] = this.innerFilterChain.getFilters();
        if (filters != null) {
            ComponentFilterChain processor = new ComponentComponentFilterChain(
                filters);

            processor.doFilter(request, response);
        } else {
            log.error("service: No Inner Request Handling filters, cannot process request");
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Cannot process Request");
        }
    }

    // ---------- Internal helper ----------------------------------------------

    /* package */String getServerInfo() {
        return this.serverInfo;
    }

    // ---------- Property Setter for SCR --------------------------------------

    protected void activate(
            org.osgi.service.component.ComponentContext componentContext) {

        // get the bundle context and register delayed filters
        this.bundleContext = componentContext.getBundleContext();

        // the component context for filters and components
        this.componentContext = new ComponentContextImpl(this);

        // register render filters already registered before our activation
        List<ServiceReference> filterList = this.delayedComponentFilters;
        this.delayedComponentFilters = null;
        if (filterList != null) {
            for (ServiceReference serviceReference : filterList) {
                this.bindComponentFilter(serviceReference);
            }
        }

        // setup server info
        Dictionary<?, ?> props = this.bundleContext.getBundle().getHeaders();
        Version bundleVersion = Version.parseVersion((String) props.get(Constants.BUNDLE_VERSION));
        String productVersion = bundleVersion.getMajor() + "."
            + bundleVersion.getMinor();
        this.serverInfo = PRODUCT_NAME + "/" + productVersion + " ("
            + System.getProperty("java.vm.name") + " "
            + System.getProperty("java.version") + "; "
            + System.getProperty("os.name") + " "
            + System.getProperty("os.version") + " "
            + System.getProperty("os.arch") + ")";

        // get the web manager root path
        Dictionary configuration = componentContext.getProperties();
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

        // register the servlet and resources
        try {
            this.slingHttpContext = new SlingHttpContext(this.mimeTypeService);
            this.httpService.registerServlet(this.slingRoot, this, configuration,
                this.slingHttpContext);

            log.info("{} ready to serve requests", this.getServerInfo());

        } catch (Exception e) {
            log.error("Cannot register " + this.getServerInfo(), e);
        }

    }

    protected void deactivate(
            org.osgi.service.component.ComponentContext componentContext) {

        this.httpService.unregister(this.slingRoot);

        if (this.slingHttpContext != null) {
            this.slingHttpContext.dispose();
        }

        this.componentContext = null;
        this.bundleContext = null;

        log.info(this.getServerInfo() + " shut down");
    }

    protected void bindComponentFilter(ServiceReference ref) {
        if (this.bundleContext == null) {
            if (this.delayedComponentFilters == null) {
                this.delayedComponentFilters = new ArrayList<ServiceReference>();
            }
            this.delayedComponentFilters.add(ref);
        } else {
            ComponentFilter filter = (ComponentFilter) this.bundleContext.getService(ref);

            // initialize the filter first
            try {
                filter.init(this.componentContext);

                // service id
                Object serviceId = ref.getProperty(Constants.SERVICE_ID);

                // get the order, Integer.MAX_VALUE by default
                Object orderObj = ref.getProperty("filter.order");
                int order = (orderObj instanceof Integer)
                        ? ((Integer) orderObj).intValue()
                        : Integer.MAX_VALUE;

                // register by scope
                Object scope = ref.getProperty("filter.scope");
                if ("component".equals(scope)) {
                    // component rendering filter
                    this.innerFilterChain.addFilter(filter, serviceId, order);
                } else {
                    // global filter by default
                    this.requestFilterChain.addFilter(filter, serviceId, order);
                }

                // mark success by setting the filter variable to null
                filter = null;
            } catch (ComponentException ce) {
                log.error("ComponentFilter " + "" + " failed to initialize", ce);
            } catch (Throwable t) {
                log.error("Unexpected Problem initializing ComponentFilter "
                    + "", t);
            } finally {
                // if filter is not null, there was an error binding
                if (filter != null) {
                    this.bundleContext.ungetService(ref);
                }
            }
        }
    }

    protected void unbindComponentFilter(ServiceReference ref) {
        // service id
        Object serviceId = ref.getProperty(Constants.SERVICE_ID);

        // unregister by scope
        ComponentFilter filter;
        Object scope = ref.getProperty("filter.scope");
        if ("component".equals(scope)) {
            // component rendering filter
            filter = this.innerFilterChain.removeFilterById(serviceId);
        } else {
            // global filter by default
            filter = this.requestFilterChain.removeFilterById(serviceId);
        }

        // if a filter has actually been removed, destroy it
        if (filter != null) {
            try {
                filter.destroy();
            } catch (Throwable t) {
                log.error(
                    "Unexpected problem destroying ComponentFilter " + "", t);
            }

            // unget the filter service
            this.bundleContext.ungetService(ref);
        }
    }
}
