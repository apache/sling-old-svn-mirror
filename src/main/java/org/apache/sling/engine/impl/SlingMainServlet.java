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
package org.apache.sling.engine.impl;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.GenericServlet;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.adapter.AdapterManager;
import org.apache.sling.api.request.SlingRequestEvent;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.ServletResolver;
import org.apache.sling.auth.core.AuthenticationSupport;
import org.apache.sling.commons.mime.MimeTypeService;
import org.apache.sling.engine.SlingRequestProcessor;
import org.apache.sling.engine.impl.filter.ServletFilterManager;
import org.apache.sling.engine.impl.helper.ClientAbortException;
import org.apache.sling.engine.impl.helper.RequestListenerManager;
import org.apache.sling.engine.impl.helper.SlingServletContext;
import org.apache.sling.engine.impl.request.RequestData;
import org.apache.sling.engine.impl.request.RequestHistoryConsolePlugin;
import org.apache.sling.engine.jmx.RequestProcessorMBean;
import org.apache.sling.engine.servlets.ErrorHandler;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>SlingMainServlet</code>
 */
@SuppressWarnings("serial")
@Component(property = {
        Constants.SERVICE_VENDOR + "=The Apache Software Foundation",
        Constants.SERVICE_DESCRIPTION + "=Sling Servlet"
})
@Designate(ocd=SlingMainServlet.Config.class)
public class SlingMainServlet extends GenericServlet {

    @ObjectClassDefinition(name ="Apache Sling Main Servlet",
            description="Main processor of the Sling framework controlling all " +
                    "aspects of processing requests inside of Sling, namely authentication, " +
                    "resource resolution, servlet/script resolution and execution of servlets " +
                    "and scripts.")
    public @interface Config {

        @AttributeDefinition(name = "Number of Calls per Request",
                description = "Defines the maximum number of Servlet and Script " +
                     "calls while processing a single client request. This number should be high " +
                     "enough to not limit request processing artificially. On the other hand it " +
                     "should not be too high to allow the mechanism to limit the resources required " +
                     "to process a request in case of errors. The default value is 1000.")
        int sling_max_calls() default RequestData.DEFAULT_MAX_CALL_COUNTER;

        @AttributeDefinition(name = "Recursion Depth",
                description = "The maximum number of recursive Servlet and " +
                     "Script calls while processing a single client request. This number should not " +
                     "be too high, otherwise StackOverflowErrors may occurr in case of erroneous " +
                     "scripts and servlets. The default value is 50. ")
        int sling_max_inclusions() default RequestData.DEFAULT_MAX_INCLUSION_COUNTER;

        @AttributeDefinition(name = "Allow the HTTP TRACE method",
                description = "If set to true, the HTTP TRACE method will be " +
                     "enabled. By default the HTTP TRACE methods is disabled as it can be used in " +
                     "Cross Site Scripting attacks on HTTP servers.")
        boolean sling_trace_allow() default false;

        @AttributeDefinition(name = "Number of Requests to Record",
                description = "Defines the number of requests that " +
                     "internally recorded for display on the \"Recent Requests\" Web Console page. If " +
                     "this value is less than or equal to zero, no requests are internally kept. The " +
                     "default value is 20. ")
        int sling_max_record_requests() default RequestHistoryConsolePlugin.STORED_REQUESTS_COUNT;

        @AttributeDefinition(name = "Recorded Request Path Patterns",
                description = "One or more regular expressions which " +
                            "limit the requests which are stored by the \"Recent Requests\" Web Console page.")
        String[] sling_store_pattern_requests();

        @AttributeDefinition(name = "Server Info",
                description = "The server info returned by Sling. If this field is left empty, Sling generates a default into.")
        String sling_serverinfo();

        @AttributeDefinition(name = "Additional response headers",
                description = "Provides mappings for additional response headers "
                    + "Each entry is of the form 'bundleId [ \":\" responseHeaderName ] \"=\" responseHeaderValue'")
        String[] sling_additional_response_headers() default {"X-Content-Type-Options=nosniff", "X-Frame-Options=SAMEORIGIN"};
    }

    private static final String DEPRECATED_ENCODING_PROPERTY = "sling.default.parameter.encoding";

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile AdapterManager adapterManager;

    /** default log */
    private final Logger log = LoggerFactory.getLogger(SlingMainServlet.class);

    /**
     * The registration path for the SlingMainServlet is hard wired to always
     * be the root, aka "<code>/</code>" (value is "/").
     */
    private static final String SLING_ROOT = "/";

    /**
     * The name of the servlet context for Sling
     */
    public static final String SERVLET_CONTEXT_NAME = "org.apache.sling";

    /**
     * The name of the product to report in the {@link #getServerInfo()} method
     * (value is "ApacheSling").
     */
    static String PRODUCT_NAME = "ApacheSling";

    private SlingServletContext slingServletContext;

    /**
     * The product information part of the {@link #serverInfo} returns from the
     * <code>ServletContext.getServerInfo()</code> method. This field defaults
     * to {@link #PRODUCT_NAME} and is amended with the major and minor version
     * of the Sling Engine bundle while this component is being
     * {@link #activate(BundleContext, Map)} activated}.
     */
    private String productInfo = PRODUCT_NAME;

    /**
     * The server information to report in the {@link #getServerInfo()} method.
     * By default this is just the {@link #PRODUCT_NAME} (same as
     * {@link #productInfo}. During {@link #activate(BundleContext, Map)}
     * activation} the field is updated with the full {@link #productInfo} value
     * as well as the operating system and java version it is running on.
     * Finally during servlet initialization the product information from the
     * servlet container's server info is added to the comment section.
     */
    private String serverInfo = PRODUCT_NAME;

    private RequestListenerManager requestListenerManager;

    private boolean allowTrace;

    private Object printerRegistration;

    // new properties

    private SlingHttpContext slingHttpContext = new SlingHttpContext();

    private ServletFilterManager filterManager;

    private final SlingRequestProcessorImpl requestProcessor = new SlingRequestProcessorImpl();

    private ServiceRegistration<SlingRequestProcessor> requestProcessorRegistration;

    private ServiceRegistration<RequestProcessorMBean> requestProcessorMBeanRegistration;

    private ServiceRegistration<ServletContextHelper> contextRegistration;

    private ServiceRegistration<Servlet> servletRegistration;

    private String configuredServerInfo;

    // ---------- Servlet API -------------------------------------------------

    @Override
    public void service(ServletRequest req, ServletResponse res)
            throws ServletException {

        if (req instanceof HttpServletRequest
            && res instanceof HttpServletResponse) {

            HttpServletRequest request = (HttpServletRequest) req;

            // set the thread name according to the request
            String threadName = setThreadName(request);

            requestListenerManager.sendEvent( request, SlingRequestEvent.EventType.EVENT_INIT );

            ResourceResolver resolver = null;
            try {
                if (!allowTrace && "TRACE".equals(request.getMethod())) {
                    HttpServletResponse response = (HttpServletResponse) res;
                    response.setStatus(405);
                    response.setHeader("Allow", "GET, HEAD, POST, PUT, DELETE, OPTIONS");
                    return;
                }

                // get ResourceResolver (set by AuthenticationSupport)
                Object resolverObject = request.getAttribute(AuthenticationSupport.REQUEST_ATTRIBUTE_RESOLVER);
                resolver = (resolverObject instanceof ResourceResolver)
                        ? (ResourceResolver) resolverObject
                        : null;

                // real request handling for HTTP requests
                requestProcessor.doProcessRequest(request, (HttpServletResponse) res,
                    resolver);

            } catch (ClientAbortException cae) {
                log.debug("service: ClientAbortException, probable cause is client aborted request or network problem", cae);

            } catch (Throwable t) {

                // some failure while handling the request, log the issue
                // and terminate. We do not call error handling here, because
                // we assume the real request handling would have done this.
                // So here we just log

                log.error("service: Uncaught Problem handling the request", t);

            } finally {


                // close the resource resolver (not relying on servlet request
                // listener to do this for now; see SLING-1270)
                if (resolver != null) {
                    resolver.close();
                }

                requestListenerManager.sendEvent( request, SlingRequestEvent.EventType.EVENT_DESTROY );

                // reset the thread name
                if (threadName != null) {
                    Thread.currentThread().setName(threadName);
                }
            }

        } else {
            throw new ServletException(
                "Apache Sling must be run in an HTTP servlet environment.");
        }
    }

    // ---------- Internal helper ----------------------------------------------

    /**
     * Sets the {@link #productInfo} field from the providing bundle's version
     * and the {@link #PRODUCT_NAME}.
     * <p>
     * Also {@link #setServerInfo() updates} the {@link #serverInfo} based
     * on the product info calculated.
     *
     * @param bundleContext Provides access to the "Bundle-Version" manifest
     *            header of the containing bundle.
     */
    private void setProductInfo(final BundleContext bundleContext) {
        final Dictionary<?, ?> props = bundleContext.getBundle().getHeaders();
        final Version bundleVersion = Version.parseVersion((String) props.get(Constants.BUNDLE_VERSION));
        final String productVersion = bundleVersion.getMajor() + "."
            + bundleVersion.getMinor();
        this.productInfo = PRODUCT_NAME + "/" + productVersion;

        // update the server info
        this.setServerInfo();
    }

    public String getServerInfo() {
        return serverInfo;
    }

    /**
     * Sets up the server info to be returned for the
     * <code>ServletContext.getServerInfo()</code> method for servlets and
     * filters deployed inside Sling. The {@link SlingRequestProcessor} instance
     * is also updated with the server information.
     * <p>
     * This server info is either configured through an OSGi configuration or
     * it is made up of the following components:
     * <ol>
     * <li>The {@link #productInfo} field as the primary product information</li>
     * <li>The primary product information of the servlet container into which
     * the Sling Main Servlet is deployed. If the servlet has not yet been
     * deployed this will show as <i>unregistered</i>. If the servlet container
     * does not provide a server info this will show as <i>unknown</i>.</li>
     * <li>The name and version of the Java VM as reported by the
     * <code>java.vm.name</code> and <code>java.vm.version</code> system
     * properties</li>
     * <li>The name, version, and architecture of the OS platform as reported by
     * the <code>os.name</code>, <code>os.version</code>, and
     * <code>os.arch</code> system properties</li>
     * </ol>
     */
    private void setServerInfo() {
        if ( this.configuredServerInfo != null ) {
            this.serverInfo = this.configuredServerInfo;
        } else {
            final String containerProductInfo;
            if (getServletConfig() == null || getServletContext() == null) {
                containerProductInfo = "unregistered";
            } else {
                final String containerInfo = getServletContext().getServerInfo();
                if (containerInfo != null && containerInfo.length() > 0) {
                    int lbrace = containerInfo.indexOf('(');
                    if (lbrace < 0) {
                        lbrace = containerInfo.length();
                    }
                    containerProductInfo = containerInfo.substring(0, lbrace).trim();
                } else {
                    containerProductInfo = "unknown";
                }
            }

            this.serverInfo = String.format("%s (%s, %s %s, %s %s %s)",
                this.productInfo, containerProductInfo,
                System.getProperty("java.vm.name"),
                System.getProperty("java.version"), System.getProperty("os.name"),
                System.getProperty("os.version"), System.getProperty("os.arch"));
        }
        if (this.requestProcessor != null) {
            this.requestProcessor.setServerInfo(serverInfo);
        }
    }

    // ---------- Property Setter for SCR --------------------------------------

    @Activate
    protected void activate(final BundleContext bundleContext,
            final Map<String, Object> componentConfig,
            final Config config) {

        final String[] props = config.sling_additional_response_headers();
        if ( props != null ) {
            final ArrayList<StaticResponseHeader> mappings = new ArrayList<>(props.length);
            for (final String prop : props) {
                if (prop != null && prop.trim().length() > 0 ) {
                    try {
                        final StaticResponseHeader mapping = new StaticResponseHeader(prop.trim());
                        mappings.add(mapping);
                    } catch (final IllegalArgumentException iae) {
                        log.info("configure: Ignoring '{}': {}", prop, iae.getMessage());
                    }
                }
            }
            RequestData.setAdditionalResponseHeaders(mappings);
        }
        configuredServerInfo = config.sling_serverinfo();

        // setup server info
        setProductInfo(bundleContext);

        // prepare the servlet configuration from the component config
        final Hashtable<String, Object> configuration = new Hashtable<>(
            componentConfig);

        // ensure the servlet name
        if (!(configuration.get("servlet-name") instanceof String)) {
            configuration.put("servlet-name", this.productInfo);
        }

        // configure method filter
        allowTrace = config.sling_trace_allow();

        // configure the request limits
        RequestData.setMaxIncludeCounter(config.sling_max_inclusions());
        RequestData.setMaxCallCounter(config.sling_max_calls());
        RequestData.setSlingMainServlet(this);

        // Warn about the obsolete parameter encoding configuration
        if (componentConfig.get(DEPRECATED_ENCODING_PROPERTY) != null) {
            log.warn("Please configure the default request parameter encoding using "
                + "the 'org.apache.sling.engine.parameters' configuration PID; the property "
                + DEPRECATED_ENCODING_PROPERTY + "="
                + componentConfig.get(DEPRECATED_ENCODING_PROPERTY)
                + " is obsolete and ignored");
        }

        // register the servlet context
        final Dictionary<String, String> contextProperties = new Hashtable<>();
        contextProperties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, SERVLET_CONTEXT_NAME);
        contextProperties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, SLING_ROOT);
        contextProperties.put(Constants.SERVICE_DESCRIPTION, "Apache Sling Engine Servlet Context Helper");
        contextProperties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");

        this.contextRegistration = bundleContext.registerService(ServletContextHelper.class, this.slingHttpContext, contextProperties);

        // register the servlet
        final Dictionary<String, String> servletConfig = toStringConfig(configuration);
        servletConfig.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
                "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=" + SERVLET_CONTEXT_NAME + ")");
        servletConfig.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, SLING_ROOT);
        servletConfig.put(Constants.SERVICE_DESCRIPTION, "Apache Sling Engine Main Servlet");
        servletConfig.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        this.servletRegistration = bundleContext.registerService(Servlet.class, this, servletConfig);

        log.info("{} ready to serve requests", this.getServerInfo());

        // now that the sling main servlet is registered with the HttpService
        // and initialized we can register the servlet context
        slingServletContext = new SlingServletContext(bundleContext, this);

        // register render filters already registered after registration with
        // the HttpService as filter initialization may cause the servlet
        // context to be required (see SLING-42)
        filterManager = new ServletFilterManager(bundleContext,
            slingServletContext);
        filterManager.open();
        requestProcessor.setFilterManager(filterManager);

        // initialize requestListenerManager
        requestListenerManager = new RequestListenerManager( bundleContext, slingServletContext );

        // Setup configuration printer
        this.printerRegistration = WebConsoleConfigPrinter.register(bundleContext, filterManager);

        // setup the request info recorder
        try {
            int maxRequests = config.sling_max_record_requests();
            String[] patterns = config.sling_store_pattern_requests();
            if ( patterns == null ) patterns = new String[0];
            List<Pattern> compiledPatterns = new ArrayList<>(patterns.length);
            for (String pattern : patterns) {
                if(pattern != null && pattern.trim().length() > 0) {
                    compiledPatterns.add(Pattern.compile(pattern));
                }
            }
            RequestHistoryConsolePlugin.initPlugin(bundleContext, maxRequests, compiledPatterns);
        } catch (Throwable t) {
            log.debug(
                "Unable to register web console request recorder plugin.", t);
        }

        try {
            Dictionary<String, String> mbeanProps = new Hashtable<>();
            mbeanProps.put("jmx.objectname", "org.apache.sling:type=engine,service=RequestProcessor");

            RequestProcessorMBeanImpl mbean = new RequestProcessorMBeanImpl();
            requestProcessorMBeanRegistration = bundleContext.registerService(RequestProcessorMBean.class, mbean, mbeanProps);
            requestProcessor.setMBean(mbean);
        } catch (Throwable t) {
            log.debug("Unable to register mbean");
        }

        // provide the SlingRequestProcessor service
        Hashtable<String, String> srpProps = new Hashtable<>();
        srpProps.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        srpProps.put(Constants.SERVICE_DESCRIPTION, "Sling Request Processor");
        requestProcessorRegistration = bundleContext.registerService(
            SlingRequestProcessor.class, requestProcessor, srpProps);
    }

    @Override
    public void init() {
        setServerInfo();
    }

    @Deactivate
    protected void deactivate() {
        // unregister the sling request processor
        if (requestProcessorRegistration != null) {
            requestProcessorRegistration.unregister();
            requestProcessorRegistration = null;
        }

        if (requestProcessorMBeanRegistration != null) {
            requestProcessorMBeanRegistration.unregister();
            requestProcessorMBeanRegistration = null;
        }

        // unregister request recorder plugin
        try {
            RequestHistoryConsolePlugin.destroyPlugin();
        } catch (Throwable t) {
            log.debug(
                "Problem unregistering web console request recorder plugin.", t);
        }

        // this reverses the activation setup
        if ( this.printerRegistration != null ) {
            WebConsoleConfigPrinter.unregister(this.printerRegistration);
            this.printerRegistration = null;
        }
        // destroy servlet filters before destroying the sling servlet
        // context because the filters depend on that context
        if (filterManager != null) {
            requestProcessor.setFilterManager(null);
            filterManager.close();
        }

        // second unregister the servlet context *before* unregistering
        // and destroying the the sling main servlet
        if ( this.contextRegistration != null ) {
            this.contextRegistration.unregister();
            this.contextRegistration = null;
        }
        if (slingServletContext != null) {
            slingServletContext.dispose();
            slingServletContext = null;
        }

        // third unregister and destroy the sling main servlet
        // unregister servlet
        if ( this.servletRegistration != null ) {
            this.servletRegistration.unregister();
            this.servletRegistration = null;
        }

        // dispose of request listener manager after unregistering the servlet
        // to prevent a potential NPE in the service method
        if ( this.requestListenerManager != null ) {
            this.requestListenerManager.dispose();
            this.requestListenerManager = null;
        }

        // reset the sling main servlet reference (help GC and be nice)
        RequestData.setSlingMainServlet(null);

        log.info(this.getServerInfo() + " shut down");
    }

    @Reference(name = "ErrorHandler", cardinality=ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, unbind = "unsetErrorHandler")
    void setErrorHandler(final ErrorHandler errorHandler) {
        requestProcessor.setErrorHandler(errorHandler);
    }

    void unsetErrorHandler(final ErrorHandler errorHandler) {
        requestProcessor.unsetErrorHandler(errorHandler);
    }

    @Reference(name = "ServletResolver", cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, unbind = "unsetServletResolver")
    public void setServletResolver(final ServletResolver servletResolver) {
        requestProcessor.setServletResolver(servletResolver);
    }

    public void unsetServletResolver(final ServletResolver servletResolver) {
        requestProcessor.unsetServletResolver(servletResolver);
    }

    @Reference(name = "MimeTypeService", cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, unbind = "unsetMimeTypeService")
    public void setMimeTypeService(final MimeTypeService mimeTypeService) {
        slingHttpContext.setMimeTypeService(mimeTypeService);
    }

    public void unsetMimeTypeService(final MimeTypeService mimeTypeService) {
        slingHttpContext.unsetMimeTypeService(mimeTypeService);
    }

    @Reference(name = "AuthenticationSupport", cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, unbind = "unsetAuthenticationSupport")
    public void setAuthenticationSupport(
            final AuthenticationSupport authenticationSupport) {
        slingHttpContext.setAuthenticationSupport(authenticationSupport);
    }

    public void unsetAuthenticationSupport(
            final AuthenticationSupport authenticationSupport) {
        slingHttpContext.unsetAuthenticationSupport(authenticationSupport);
    }

    private Dictionary<String, String> toStringConfig(Dictionary<?, ?> config) {
        Dictionary<String, String> stringConfig = new Hashtable<>();
        for (Enumeration<?> ke = config.keys(); ke.hasMoreElements();) {
            Object key = ke.nextElement();
            stringConfig.put(key.toString(), String.valueOf(config.get(key)));
        }
        return stringConfig;
    }

    // ---------- HttpContext interface ----------------------------------------

    public String getMimeType(String name) {
        return slingHttpContext.getMimeType(name);
    }

    public <Type> Type adaptTo(Object object, Class<Type> type) {
        AdapterManager adapterManager = this.adapterManager;
        if (adapterManager != null) {
            return adapterManager.getAdapter(object, type);
        }

        // no adapter manager, nothing to adapt to
        return null;
    }

    /**
     * Sets the name of the current thread to the IP address of the remote
     * client with the current system time and the first request line consisting
     * of the method, path and protocol.
     *
     * @param request The request to extract the remote IP address, method,
     *            request URL and protocol from.
     * @return The name of the current thread before setting the new name.
     */
    private String setThreadName(HttpServletRequest request) {

        // get the name of the current thread (to be returned)
        Thread thread = Thread.currentThread();
        String oldThreadName = thread.getName();

        // construct and set the new thread name of the form:
        // 127.0.0.1 [1224156108055] GET /system/console/config HTTP/1.1
        final StringBuilder buf = new StringBuilder();
        buf.append(request.getRemoteAddr());
        buf.append(" [").append(System.currentTimeMillis()).append("] ");
        buf.append(request.getMethod()).append(' ');
        buf.append(request.getRequestURI()).append(' ');
        buf.append(request.getProtocol());
        thread.setName(buf.toString());

        // return the previous thread name
        return oldThreadName;
    }
}
