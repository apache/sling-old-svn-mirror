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

package org.apache.sling.junit.impl.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Properties;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.read.CyclicBufferAppender;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.junit.runner.Description;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

@Component(immediate=true, metatype=true,
        label = "Apache Sling Test Log Collector",
        description = "Servlet that exposes logs collected for a particular test execution"
)
public class TestLogServlet extends HttpServlet {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //These name should be kept in sync with
    // org.apache.sling.testing.tools.junit.RemoteLogDumper
    public static final String TEST_NAME = "X-Sling-Test-Name";
    public static final String TEST_CLASS = "X-Sling-Test-Class";

    @Property(value="/system/sling/testlog")
    static final String SERVLET_PATH_NAME = "servlet.path";

    static final int DEFAULT_SIZE = 1000;
    @Property(intValue = DEFAULT_SIZE,
            label = "Log Buffer Size",
            description = "Size of in memory log buffer. Only recent logs upto buffer size would be retained"
    )
    static final String LOG_BUFFER_SIZE = "log.buffer.size";

    private static final String DEFAULT_PATTERN = "%d{dd.MM.yyyy HH:mm:ss.SSS} *%level* [%thread] %logger %msg%n";

    @Property(label = "Log Pattern",
            description = "Message Pattern for formatting the log messages",
            value = DEFAULT_PATTERN
    )
    private static final String PROP_MSG_PATTERN = "logPattern";

    /** Non-null if we are registered with HttpService */
    private String servletPath;

    @Reference
    private HttpService httpService;

    private CyclicBufferAppender<ILoggingEvent> appender;

    private Layout<ILoggingEvent> layout;

    private ServiceRegistration filter;

    private volatile Description currentTest;

    private final Object appenderLock = new Object();

    @Activate
    protected void activate(BundleContext ctx, Map<String, ?> config) throws Exception {
        registerServlet(config);
        registerAppender(config);
        registerFilter(ctx);
        createLayout(config);
    }

    @Deactivate
    protected void deactivate() throws Exception {
        deregisterFilter();
        deregisterServlet();
        deregisterAppender();
        stopLayout();
    }

    public void testRunStarted(Description description) {
        if (description != null && !description.equals(currentTest)){
            currentTest = description;
            resetAppender();
            log.info("Starting test execution {}", description);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        final PrintWriter pw = response.getWriter();
        final String className = request.getParameter(TEST_CLASS);
        final String testName = request.getParameter(TEST_NAME);

        //If className and testName explicitly specified check if the logs
        //are being collected for expected test
        if (className != null && testName != null){
            Description expected = Description.createTestDescription(className, testName);

            if (!expected.equals(currentTest)){
                pw.printf("Test name mismatch : Current test [%s], Expected test [%s]%n", currentTest, expected);
                return;
            }
        }

        //Detach the appender so that we can extract its content safely
        rootLogger().detachAppender(appender);
        try {
            for (int i = 0; i < appender.getLength(); i++) {
                pw.print(layout.doLayout(appender.get(i)));
            }
            resetAppender();
        } finally {
            rootLogger().addAppender(appender);
        }
    }

    private void resetAppender() {
        synchronized (appenderLock) {
            if (appender.isStarted()) {
                appender.reset();
            }
        }
    }

    private void registerAppender(Map<String, ?> config) {
        synchronized (appenderLock) {
            int size = PropertiesUtil.toInteger(config.get(LOG_BUFFER_SIZE), DEFAULT_SIZE);
            appender = new CyclicBufferAppender<ILoggingEvent>();
            appender.setMaxSize(size);
            appender.setContext(getContext());
            appender.setName("TestLogCollector");
            appender.start();
            rootLogger().addAppender(appender);
        }
    }

    private void deregisterAppender() {
        if (appender != null) {
            synchronized (appenderLock) {
                rootLogger().detachAppender(appender);
                appender.stop();
                appender = null;
            }
        }
    }

    private void createLayout(Map<String, ?> config) {
        String pattern = PropertiesUtil.toString(config.get(PROP_MSG_PATTERN), DEFAULT_PATTERN);
        PatternLayout pl = new PatternLayout();
        pl.setPattern(pattern);
        pl.setOutputPatternAsHeader(false);
        pl.setContext(getContext());
        pl.start();

        layout = pl;
    }

    private void stopLayout() {
        if (layout != null){
            layout.stop();
        }
    }

    private void registerServlet(Map<String, ?> config) throws ServletException, NamespaceException {
        servletPath = getServletPath(config);
        if(servletPath == null) {
            log.info("Servlet path is null, not registering with HttpService");
        } else {
            httpService.registerServlet(servletPath, this, null, null);
            log.info("Servlet registered at {}", servletPath);
        }
    }

    private void deregisterServlet() {
        if(servletPath != null) {
            httpService.unregister(servletPath);
            log.info("Servlet unregistered from path {}", servletPath);
        }
        servletPath = null;
    }

    private void registerFilter(BundleContext ctx) {
        Properties props = new Properties();
        props.put(Constants.SERVICE_DESCRIPTION, "Filter to extract testName from request headers");
        props.put(Constants.SERVICE_VENDOR, ctx.getBundle().getHeaders().get(Constants.BUNDLE_VENDOR));

        props.put("pattern", "/.*");
        filter = ctx.registerService(Filter.class.getName(), new TestNameLoggingFilter(), props);
    }

    private void deregisterFilter() {
        if (filter != null) {
            filter.unregister();
        }
    }

    private class TestNameLoggingFilter implements Filter {

        public void init(FilterConfig filterConfig) throws ServletException {

        }

        public void doFilter(ServletRequest request, ServletResponse response,
                             FilterChain chain) throws IOException, ServletException {

            final HttpServletRequest httpRequest = (HttpServletRequest) request;
            final String className = httpRequest.getHeader(TEST_CLASS);
            final String testName = httpRequest.getHeader(TEST_NAME);

            if (className == null || testName == null) {
                chain.doFilter(request, response);
                return;
            }

            try {
                MDC.put(TEST_NAME, testName);
                MDC.put(TEST_CLASS, className);

                testRunStarted(Description.createTestDescription(className, testName));

                chain.doFilter(request, response);
            } finally {

                MDC.remove(TEST_NAME);
                MDC.remove(TEST_CLASS);
            }

        }

        public void destroy() {

        }
    }

    //~------------------------------------------------< utility >

    /**
     * Return the path at which to mount this servlet, or null
     * if it must not be mounted.
     * @param ctx
     */
    private static String getServletPath(Map<String, ?> config) {
        String result = (String)config.get(SERVLET_PATH_NAME);
        if(result != null && result.trim().length() == 0) {
            result = null;
        }
        return result;
    }

    private static LoggerContext getContext(){
        return (LoggerContext) LoggerFactory.getILoggerFactory();
    }

    private static ch.qos.logback.classic.Logger rootLogger() {
        return getContext().getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
    }
}
