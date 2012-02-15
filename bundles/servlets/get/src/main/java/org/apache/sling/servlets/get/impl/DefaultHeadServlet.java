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
package org.apache.sling.servlets.get.impl;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;
import org.apache.sling.api.wrappers.SlingHttpServletResponseWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>DefaultHeadServlet</code> class implements default support for the
 * HTTP <i>HEAD</i> request method. It basically wraps the response to provide
 * output which does not really write to the client and the forwards to the same
 * requested URL (resource actually) acting as if the request was placed with a
 * <i>GET</i> method.
 */
@Component(immediate=true, policy=ConfigurationPolicy.IGNORE)
@Service(Servlet.class)
@Properties({
    @Property(name="service.description", value="Default HEAD Servlet"),
    @Property(name="service.vendor", value="The Apache Software Foundation"),
    @Property(name="sling.servlet.resourceTypes", value="sling/servlet/default"),
    @Property(name="sling.servlet.prefix", intValue=-1),
    @Property(name="sling.servlet.methods", value="HEAD")    
})
public class DefaultHeadServlet extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = 7416222678552027044L;

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    protected void doHead(SlingHttpServletRequest request,
            SlingHttpServletResponse response) throws ServletException,
            IOException {

        // don't do nothing if the request has already been committed
        // or this servlet is called for a servlet include
        if (response.isCommitted()) {
            // committed response cannot be redirected
            log.warn("DefaultHeadServlet: Ignoring request because response is committed");
            request.getRequestProgressTracker().log(
                "DefaultHeadServlet: Ignoring request because response is committed");
            return;
        } else if (request.getAttribute(SlingConstants.ATTR_REQUEST_SERVLET) != null) {
            // included request will not redirect
            log.warn("DefaultHeadServlet: Ignoring request because request is included");
            request.getRequestProgressTracker().log(
                "DefaultHeadServlet: Ignoring request because request is included");
            return;
        }

        request = new HeadServletRequest(request);
        response = new HeadServletResponse(response);

        RequestDispatcher dispatcher = request.getRequestDispatcher(request.getResource());
        if (dispatcher != null) {
            dispatcher.forward(request, response);
        }
    }

    /**
     * The <code>HeadServletRequest</code> is a Sling request wrapper which
     * simulates a GET request to the included servlets/scripts such that the
     * HEAD request acts as if a GET request is being processed without any
     * response data being sent back.
     */
    private static class HeadServletRequest extends
            SlingHttpServletRequestWrapper {

        public HeadServletRequest(SlingHttpServletRequest wrappedRequest) {
            super(wrappedRequest);
        }

        @Override
        public String getMethod() {
            return "GET";
        }
    }

    /**
     * The <code>HeadServletResponse</code> is a Sling response wrapper which
     * ensures that nothing will ever be written by return null writers or
     * output streams.
     */
    private static class HeadServletResponse extends
            SlingHttpServletResponseWrapper {

        private ServletOutputStream stream;

        private PrintWriter writer;

        public HeadServletResponse(SlingHttpServletResponse wrappedResponse) {
            super(wrappedResponse);
        }

        @Override
        public ServletOutputStream getOutputStream() {
            if (writer != null) {
                throw new IllegalStateException("Writer already obtained");
            }

            if (stream == null) {
                stream = new NullServletOutputStream();
            }

            return stream;
        }

        @Override
        public PrintWriter getWriter() {
            if (stream != null) {
                throw new IllegalStateException("OutputStream already obtained");
            }

            if (writer == null) {
                writer = new PrintWriter(new NullWriter());
            }

            return writer;
        }
    }

    /**
     * The <code>NullServletOutputStream</code> is a
     * <code>ServletOutputStream</code> which simply does not write out
     * anything.
     *
     * @see HeadServletResponse#getOutputStream()
     */
    private static class NullServletOutputStream extends ServletOutputStream {
        @Override
        public void write(int b) {
        }

        @Override
        public void write(byte[] b) {
        }

        @Override
        public void write(byte[] b, int off, int len) {
        }
    }

    /**
     * The <code>NullWriter</code> is a <code>Writer</code> which simply does
     * not write out anything.
     *
     * @see HeadServletResponse#getWriter()
     */
    private static class NullWriter extends Writer {
        @Override
        public void write(char[] cbuf, int off, int len) {
        }

        @Override
        public void write(char[] cbuf) {
        }

        @Override
        public void write(int c) {
        }

        @Override
        public void write(String str) {
        }

        @Override
        public void write(String str, int off, int len) {
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }
}
