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
package org.apache.sling.reqanalyzer.impl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Hashtable;
import java.util.Iterator;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.wrappers.SlingHttpServletResponseWrapper;
import org.apache.sling.engine.EngineConstants;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

@Component(metatype = false)
@Service
@Properties({ @Property(name = EngineConstants.FILTER_NAME, value = "RequestAnalysisLogger"),
        @Property(name = EngineConstants.SLING_FILTER_SCOPE, value = EngineConstants.FILTER_SCOPE_REQUEST),
        @Property(name = Constants.SERVICE_RANKING, intValue = Integer.MAX_VALUE) })
public class RequestAnalysisLogger implements Filter {

    @Reference
    private SlingSettingsService settings;

    private BufferedWriter logFile;

    private RequestAnalyzerWebConsole requestAnalyzerWebConsole;
    private ServiceRegistration webConsolePlugin;

    @SuppressWarnings({ "serial" })
    @Activate
    private void activate(final BundleContext ctx) throws IOException {
        final File logFile = new File(settings.getSlingHomePath(), "logs/requesttracker.txt");
        logFile.getParentFile().mkdirs();
        final FileOutputStream out = new FileOutputStream(logFile, true);
        this.logFile = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));

        this.requestAnalyzerWebConsole = new RequestAnalyzerWebConsole(logFile);
        this.webConsolePlugin = ctx.registerService("javax.servlet.Servlet", this.requestAnalyzerWebConsole,
                new Hashtable<String, Object>() {
                    {
                        put("felix.webconsole.label", "requestanalyzer");
                        put("felix.webconsole.title", "Request Analyzer");
                        put("felix.webconsole.category", "Sling");
                    }
                });
    }

    @Deactivate
    private void deactivate() throws IOException {
        if (this.webConsolePlugin != null) {
            this.webConsolePlugin.unregister();
            this.webConsolePlugin = null;
        }

        if (this.requestAnalyzerWebConsole != null) {
            this.requestAnalyzerWebConsole.dispose();
            this.requestAnalyzerWebConsole = null;
        }

        if (this.logFile != null) {
            this.logFile.close();
            this.logFile = null;
        }
    }

    public void init(FilterConfig filterConfig) throws ServletException {
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {

        if (request instanceof SlingHttpServletRequest) {
            final long start = System.currentTimeMillis();
            final AnylserSlingHttpServletResponse slingRes = new AnylserSlingHttpServletResponse(
                    (SlingHttpServletResponse) response);
            try {
                chain.doFilter(request, response);
            } finally {
                final long end = System.currentTimeMillis();
                final SlingHttpServletRequest slingReq = (SlingHttpServletRequest) request;

                StringBuilder pw = new StringBuilder(1024);
                pw.append(String.format(":%d:%d:%s:%s:%s:%d%n", start, (end - start), slingReq.getMethod(),
                        slingReq.getRequestURI(), slingRes.getContentType(), slingRes.getStatus()));

                final Iterator<String> entries = slingReq.getRequestProgressTracker().getMessages();
                while (entries.hasNext()) {
                    pw.append('!').append(entries.next());
                }

                BufferedWriter out = this.logFile;
                if (out != null) {
                    out.write(pw.toString());
                    out.flush();
                }
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    public void destroy() {
    }

    private static class AnylserSlingHttpServletResponse extends SlingHttpServletResponseWrapper {

        private int status = 200;

        public AnylserSlingHttpServletResponse(SlingHttpServletResponse wrappedResponse) {
            super(wrappedResponse);
        }

        public int getStatus() {
            return status;
        }

        @Override
        public void setStatus(int sc) {
            this.status = sc;
            super.setStatus(sc);
        }

        @Override
        public void setStatus(int sc, String sm) {
            this.status = sc;
            super.setStatus(sc, sm);
        }

        @Override
        public void sendError(int sc) throws IOException {
            this.status = sc;
            super.sendError(sc);
        }

        @Override
        public void sendError(int sc, String msg) throws IOException {
            this.status = sc;
            super.sendError(sc, msg);
        }

        @Override
        public void sendRedirect(String location) throws IOException {
            this.status = HttpServletResponse.SC_FOUND;
            super.sendRedirect(location);
        }
    }
}
