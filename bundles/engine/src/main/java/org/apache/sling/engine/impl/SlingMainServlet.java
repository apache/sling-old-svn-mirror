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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.References;
import org.apache.sling.api.adapter.AdapterManager;
import org.apache.sling.api.request.SlingRequestEvent;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.ServletResolver;
import org.apache.sling.auth.core.AuthenticationSupport;
import org.apache.sling.commons.mime.MimeTypeService;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.engine.SlingRequestProcessor;
import org.apache.sling.engine.impl.filter.ServletFilterManager;
import org.apache.sling.engine.impl.helper.RequestListenerManager;
import org.apache.sling.engine.impl.helper.SlingServletContext;
import org.apache.sling.engine.impl.helper.SlingServletContext3;
import org.apache.sling.engine.impl.request.RequestData;
import org.apache.sling.engine.impl.request.RequestHistoryConsolePlugin;
import org.apache.sling.engine.jmx.RequestProcessorMBean;
import org.apache.sling.engine.servlets.ErrorHandler;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>SlingMainServlet</code> TODO
 */
@SuppressWarnings("serial")
@Component(immediate = true, metatype = true, label = "%sling.name", description = "%sling.description")
@Properties( {
    @Property(name = Constants.SERVICE_VENDOR, value = "The Apache Software Foundation"),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "Sling Servlet")

})
@References( {
    @Reference(name = "ErrorHandler", referenceInterface = ErrorHandler.class, cardinality = ReferenceCardinality.OPTIONAL_UNARY, policy = ReferencePolicy.DYNAMIC, bind = "setErrorHandler", unbind = "unsetErrorHandler"),
    @Reference(name = "ServletResolver", referenceInterface = ServletResolver.class, cardinality = ReferenceCardinality.OPTIONAL_UNARY, policy = ReferencePolicy.DYNAMIC, bind = "setServletResolver", unbind = "unsetServletResolver"),
    @Reference(name = "MimeTypeService", referenceInterface = MimeTypeService.class, cardinality = ReferenceCardinality.OPTIONAL_UNARY, policy = ReferencePolicy.DYNAMIC, bind = "setMimeTypeService", unbind = "unsetMimeTypeService"),
    @Reference(name = "AuthenticationSupport", referenceInterface = AuthenticationSupport.class, cardinality = ReferenceCardinality.OPTIONAL_UNARY, policy = ReferencePolicy.DYNAMIC, bind = "setAuthenticationSupport", unbind = "unsetAuthenticationSupport") })
public class SlingMainServlet extends GenericServlet {

    @Property(intValue=RequestData.DEFAULT_MAX_CALL_COUNTER)
    public static final String PROP_MAX_CALL_COUNTER = "sling.max.calls";

    @Property(intValue=RequestData.DEFAULT_MAX_INCLUSION_COUNTER)
    public static final String PROP_MAX_INCLUSION_COUNTER = "sling.max.inclusions";

    public static final boolean DEFAULT_ALLOW_TRACE = false;

    @Property(boolValue=DEFAULT_ALLOW_TRACE)
    public static final String PROP_ALLOW_TRACE = "sling.trace.allow";

    public static final boolean DEFAULT_FILTER_COMPAT_MODE = false;

    @Property(boolValue=DEFAULT_FILTER_COMPAT_MODE)
    public static final String PROP_FILTER_COMPAT_MODE = "sling.filter.compat.mode";

    @Property(intValue = RequestHistoryConsolePlugin.STORED_REQUESTS_COUNT)
    private static final String PROP_MAX_RECORD_REQUESTS = "sling.max.record.requests";

    @Property(unbounded=PropertyUnbounded.ARRAY)
    private static final String PROP_TRACK_PATTERNS_REQUESTS = "sling.store.pattern.requests";

    private static final String PROP_DEFAULT_PARAMETER_ENCODING = "sling.default.parameter.encoding";

    @Reference
    private HttpService httpService;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY, policy = ReferencePolicy.DYNAMIC)
    private AdapterManager adapterManager;

    /** default log */
    private final Logger log = LoggerFactory.getLogger(SlingMainServlet.class);

    /**
     * The registration path for the SlingMainServlet is hard wired to always
     * be the root, aka "<code>/</code>" (value is "/").
     */
    private static final String SLING_ROOT = "/";

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

    private boolean allowTrace = DEFAULT_ALLOW_TRACE;

    private Object printerRegistration;

    // new properties

    private SlingHttpContext slingHttpContext = new SlingHttpContext();

    private ServletFilterManager filterManager;

    private final SlingRequestProcessorImpl requestProcessor = new SlingRequestProcessorImpl();

    private ServiceRegistration requestProcessorRegistration;

    private ServiceRegistration requestProcessorMBeanRegistration;

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

            } catch (IOException ioe) {

                // SLING-3498: Jetty with NIO does not have a wrapped
                // SocketException any longer but a plain IOException
                // from the NIO Socket channel. Hence we don't care for
                // unwrapping and just log at DEBUG level
                log.debug("service: Probably client aborted request or any other network problem", ioe);

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
     * This server information is made up of the following components:
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

        if (this.requestProcessor != null) {
            this.requestProcessor.setServerInfo(serverInfo);
        }
    }

    // ---------- Property Setter for SCR --------------------------------------

    @Activate
    protected void activate(final BundleContext bundleContext,
            final Map<String, Object> componentConfig) {

        // setup server info
        setProductInfo(bundleContext);

        // prepare the servlet configuration from the component config
        final Hashtable<String, Object> configuration = new Hashtable<String, Object>(
            componentConfig);

        // ensure the servlet name
        if (!(configuration.get("servlet-name") instanceof String)) {
            configuration.put("servlet-name", this.productInfo);
        }

        // configure method filter
        allowTrace = OsgiUtil.toBoolean(componentConfig.get(PROP_ALLOW_TRACE),
                DEFAULT_ALLOW_TRACE);

        // configure the request limits
        RequestData.setMaxIncludeCounter(OsgiUtil.toInteger(
            componentConfig.get(PROP_MAX_INCLUSION_COUNTER),
            RequestData.DEFAULT_MAX_INCLUSION_COUNTER));
        RequestData.setMaxCallCounter(OsgiUtil.toInteger(
            componentConfig.get(PROP_MAX_CALL_COUNTER),
            RequestData.DEFAULT_MAX_CALL_COUNTER));
        RequestData.setSlingMainServlet(this);

        // configure default request parameter encoding
        // log a message if such configuration exists ....
        if (componentConfig.get(PROP_DEFAULT_PARAMETER_ENCODING) != null) {
            log.warn("Configure default request parameter encoding with 'org.apache.sling.parameters.config' configuration; the property "
                + PROP_DEFAULT_PARAMETER_ENCODING
                + "="
                + componentConfig.get(PROP_DEFAULT_PARAMETER_ENCODING)
                + " is ignored");
        }

        // register the servlet and resources
        try {
            Dictionary<String, String> servletConfig = toStringConfig(configuration);

            this.httpService.registerServlet(SLING_ROOT, this, servletConfig,
                slingHttpContext);

            log.info("{} ready to serve requests", this.getServerInfo());

        } catch (Exception e) {
            log.error("Cannot register " + this.getServerInfo(), e);
        }

        // now that the sling main servlet is registered with the HttpService
        // and initialized we can register the servlet context
        if (getServletContext() == null || getServletContext().getMajorVersion() < 3) {
            slingServletContext = new SlingServletContext(bundleContext, this);
        } else {
            slingServletContext = new SlingServletContext3(bundleContext, this);
        }

        // register render filters already registered after registration with
        // the HttpService as filter initialization may cause the servlet
        // context to be required (see SLING-42)
        filterManager = new ServletFilterManager(bundleContext,
            slingServletContext,
            OsgiUtil.toBoolean(componentConfig.get(PROP_FILTER_COMPAT_MODE), DEFAULT_FILTER_COMPAT_MODE));
        filterManager.open();
        requestProcessor.setFilterManager(filterManager);

        // initialize requestListenerManager
        requestListenerManager = new RequestListenerManager( bundleContext, slingServletContext );

        // Setup configuration printer
        this.printerRegistration = WebConsoleConfigPrinter.register(bundleContext, filterManager);

        // setup the request info recorder
        try {
            int maxRequests = OsgiUtil.toInteger(
                componentConfig.get(PROP_MAX_RECORD_REQUESTS),
                RequestHistoryConsolePlugin.STORED_REQUESTS_COUNT);
            String[] patterns = OsgiUtil.toStringArray(componentConfig.get(PROP_TRACK_PATTERNS_REQUESTS), new String[0]);
            List<Pattern> compiledPatterns = new ArrayList<Pattern>(patterns.length);
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
            Dictionary<String, String> mbeanProps = new Hashtable<String, String>();
            mbeanProps.put("jmx.objectname", "org.apache.sling:type=engine,service=RequestProcessor");

            RequestProcessorMBeanImpl mbean = new RequestProcessorMBeanImpl();
            requestProcessorMBeanRegistration = bundleContext.registerService(RequestProcessorMBean.class.getName(), mbean, mbeanProps);
            requestProcessor.setMBean(mbean);
        } catch (Throwable t) {
            log.debug("Unable to register mbean");
        }

        // provide the SlingRequestProcessor service
        Hashtable<String, String> srpProps = new Hashtable<String, String>();
        srpProps.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        srpProps.put(Constants.SERVICE_DESCRIPTION, "Sling Request Processor");
        requestProcessorRegistration = bundleContext.registerService(
            SlingRequestProcessor.NAME, requestProcessor, srpProps);
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
        if (slingServletContext != null) {
            slingServletContext.dispose();
            slingServletContext = null;
        }

        // third unregister and destroy the sling main servlet
        httpService.unregister(SLING_ROOT);

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

    void setErrorHandler(final ErrorHandler errorHandler) {
        requestProcessor.setErrorHandler(errorHandler);
    }

    void unsetErrorHandler(final ErrorHandler errorHandler) {
        requestProcessor.unsetErrorHandler(errorHandler);
    }

    public void setServletResolver(final ServletResolver servletResolver) {
        requestProcessor.setServletResolver(servletResolver);
    }

    public void unsetServletResolver(final ServletResolver servletResolver) {
        requestProcessor.unsetServletResolver(servletResolver);
    }

    public void setMimeTypeService(final MimeTypeService mimeTypeService) {
        slingHttpContext.setMimeTypeService(mimeTypeService);
    }

    public void unsetMimeTypeService(final MimeTypeService mimeTypeService) {
        slingHttpContext.unsetMimeTypeService(mimeTypeService);
    }

    public void setAuthenticationSupport(
            final AuthenticationSupport authenticationSupport) {
        slingHttpContext.setAuthenticationSupport(authenticationSupport);
    }

    public void unsetAuthenticationSupport(
            final AuthenticationSupport authenticationSupport) {
        slingHttpContext.unsetAuthenticationSupport(authenticationSupport);
    }

    private Dictionary<String, String> toStringConfig(Dictionary<?, ?> config) {
        Dictionary<String, String> stringConfig = new Hashtable<String, String>();
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
