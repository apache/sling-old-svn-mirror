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

package org.apache.sling.tracer.internal;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.read.ListAppender;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestProgressTracker;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.apache.sling.testing.mock.osgi.junit.OsgiContextCallback;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.LoggerFactory;

import static org.apache.sling.tracer.internal.TestUtil.createTracker;
import static org.apache.sling.tracer.internal.TestUtil.getRequestId;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.slf4j.LoggerFactory.getLogger;

public class LogTracerTest {

    @Rule
    public final LogCollector logCollector = new LogCollector();

    @Rule
    public final OsgiContext context = new OsgiContext(null, new OsgiContextCallback() {
        @Override
        public void execute(OsgiContext context) throws IOException {
            if (context.getService(TurboFilter.class) != null) {
                TurboFilter[] tfs = context.getServices(TurboFilter.class, null);
                for (TurboFilter tf : tfs) {
                    getLogContext().getTurboFilterList().remove(tf);
                }
            }
        }
    });

    @Test
    public void defaultSetup() throws Exception {
        context.registerInjectActivateService(new LogTracer());
        assertNull("Filters should not be registered unless enabled", context.getService(Filter.class));
    }

    @Test
    public void enableTracer() throws Exception {
        LogTracer tracer = context.registerInjectActivateService(new LogTracer(),
                ImmutableMap.<String, Object>of("enabled", "true"));
        assertEquals(2, context.getServices(Filter.class, null).length);
        assertNull(context.getService(Servlet.class));

        MockOsgi.deactivate(tracer);
        assertNull(context.getService(Filter.class));
    }

    @Test
    public void enableTracerLogServlet() throws Exception {
        LogTracer tracer = context.registerInjectActivateService(new LogTracer(),
                ImmutableMap.<String, Object>of("enabled", "true", "servletEnabled", "true"));
        assertEquals(2, context.getServices(Filter.class, null).length);
        assertNotNull(context.getService(Servlet.class));

        MockOsgi.deactivate(tracer);
        assertNull(context.getService(Filter.class));
        assertNull(context.getService(Servlet.class));
    }

    @Test
    public void noTurboFilterRegisteredUnlessTracingRequested() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        activateTracer();
        FilterChain chain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response)
                    throws IOException, ServletException {
                assertNull(context.getService(TurboFilter.class));
            }
        };

        Filter filter = getFilter(false);
        filter.doFilter(request, response, chain);
    }

    @Test
    public void turboFilterRegisteredWhenTracingRequested() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(request.getHeader(LogTracer.HEADER_TRACER_CONFIG)).thenReturn("foo.bar");
        activateTracer();
        FilterChain chain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response)
                    throws IOException, ServletException {
                assertNotNull(context.getService(TurboFilter.class));
            }
        };

        Filter filter = getFilter(false);
        filter.doFilter(request, response, chain);
        assertNull("TurboFilter should get removed once request is done",
                context.getService(TurboFilter.class));
    }

    @Test
    public void turboFilterRegisteredWhenTracingRequested_Sling() throws Exception {
        HttpServletRequest request = mock(SlingHttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(request.getParameter(LogTracer.PARAM_TRACER_CONFIG)).thenReturn("foo.bar");
        activateTracer();
        FilterChain chain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response)
                    throws IOException, ServletException {
                assertNotNull(context.getService(TurboFilter.class));
            }
        };

        Filter filter = getFilter(true);
        filter.doFilter(request, response, chain);
        assertNull("TurboFilter should get removed once request is done",
                context.getService(TurboFilter.class));
    }

    @Test
    public void checkTracing() throws Exception {
        HttpServletRequest request = mock(SlingHttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(request.getParameter(LogTracer.PARAM_TRACER_CONFIG)).thenReturn("a.b.c;level=trace,a.b;level=debug");
        activateTracer();
        Level oldLevel =  rootLogger().getLevel();
        rootLogger().setLevel(Level.INFO);

        FilterChain chain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response)
                    throws IOException, ServletException {
                TurboFilter turboFilter = context.getService(TurboFilter.class);
                assertNotNull(turboFilter);
                getLogContext().addTurboFilter(turboFilter);
                getLogger("a").info("a-info");
                getLogger("a").debug("a-debug");
                getLogger("a.b").info("a.b-info");
                getLogger("a.b").debug("a.b-debug");
                getLogger("a.b").trace("a.b-trace");
                getLogger("a.b.c").debug("a.b.c-debug");
                getLogger("a.b.c").trace("a.b.c-trace");
                getLogger("a.b.c.d").trace("a.b.c.d-trace");

                if (getLogger("a.b.c").isTraceEnabled()){
                    getLogger("a.b.c").trace("a.b.c-trace2");
                }
            }
        };

        Filter filter = getFilter(true);
        filter.doFilter(request, response, chain);
        assertNull(context.getService(TurboFilter.class));

        List<String> logs = logCollector.getLogs();
        assertThat(logs, hasItem("a-info"));
        assertThat(logs, not(hasItem("a-debug")));

        assertThat(logs, hasItem("a.b-info"));
        assertThat(logs, hasItem("a.b-debug"));
        assertThat(logs, not(hasItem("a.b-trace")));

        assertThat(logs, hasItem("a.b.c-debug"));
        assertThat(logs, hasItem("a.b.c-trace"));
        assertThat(logs, hasItem("a.b.c-trace2"));
        assertThat(logs, hasItem("a.b.c.d-trace"));

        rootLogger().setLevel(oldLevel);
    }

    @Test
    public void recordingWithoutTracing() throws Exception{
        activateTracerAndServlet();
        MockSlingHttpServletRequest request = new MockSlingHttpServletRequest(){
            @Override
            public RequestProgressTracker getRequestProgressTracker() {
                return createTracker("x", "y");
            }
        };
        request.setHeader(TracerLogServlet.HEADER_TRACER_RECORDING, "true");

        HttpServletResponse response = mock(HttpServletResponse.class);

        FilterChain chain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response)
                    throws IOException, ServletException {
                //No TurboFilter should be registered if tracing is not requested
                assertNull(context.getService(TurboFilter.class));
            }
        };

        prepareChain(chain).doFilter(request, response);

        String requestId = getRequestId(response);
        assertNotNull(requestId);
        Recording r = ((TracerLogServlet)context.getService(Servlet.class)).getRecording(requestId);
        assertTrue(r instanceof JSONRecording);
        JSONRecording jr = (JSONRecording) r;

        StringWriter sw = new StringWriter();
        jr.render(sw);
        JSONObject json = new JSONObject(sw.toString());

        assertEquals(2, json.getJSONArray("logs").length());
    }


    private void activateTracer() {
        context.registerInjectActivateService(new LogTracer(),
                ImmutableMap.<String, Object>of("enabled", "true"));
    }

    private void activateTracerAndServlet() {
        context.registerInjectActivateService(new LogTracer(),
                ImmutableMap.<String, Object>of("enabled", "true", "servletEnabled", "true"));
    }

    private FilterChain prepareChain(FilterChain end) throws InvalidSyntaxException {
        Filter servletFilter = getFilter(false);
        Filter slingFilter = getFilter(true);
        return new FilterChainImpl(end, servletFilter, slingFilter);
    }

    private Filter getFilter(boolean slingFilter) throws InvalidSyntaxException {
        Collection<ServiceReference<Filter>> refs =
                context.bundleContext().getServiceReferences(Filter.class, null);
        ServiceReference<Filter> result = null;
        for (ServiceReference<Filter> ref : refs) {
            if (slingFilter && ref.getProperty("filter.scope") != null) {
                result = ref;
                break;
            } else if (!slingFilter && ref.getProperty("pattern") != null) {
                result = ref;
                break;
            }
        }

        if (result != null) {
            return context.bundleContext().getService(result);
        }
        throw new AssertionError("No filter found");
    }

    private static LoggerContext getLogContext(){
        return (LoggerContext)LoggerFactory.getILoggerFactory();
    }

    private static ch.qos.logback.classic.Logger rootLogger() {
        return getLogContext().getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
    }

    private static class LogCollector extends TestWatcher {
        private List<String> msgs = Lists.newArrayList();
        private TestAppender appender = new TestAppender();

        public List<String> getLogs(){
            return msgs;
        }

        @Override
        protected void starting(Description description) {
            appender.setContext(getLogContext());
            appender.setName("TestLogCollector");
            appender.start();
            rootLogger().addAppender(appender);
        }

        @Override
        protected void finished(Description description) {
            rootLogger().detachAppender(appender);
        }

        private class TestAppender extends ListAppender<ILoggingEvent>{
            @Override
            protected void append(ILoggingEvent iLoggingEvent) {
                msgs.add(iLoggingEvent.getFormattedMessage());
            }
        }
    }

    private static class FilterChainImpl implements FilterChain {
        private final Filter[] filters;
        private final FilterChain delegate;
        private int pos;

        public FilterChainImpl(FilterChain delegate, Filter ... filter){
            this.delegate = delegate;
            this.filters = filter;
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
            if (pos == filters.length){
                delegate.doFilter(request, response);
            } else {
                filters[pos++].doFilter(request, response, this);
            }
        }
    }

}
