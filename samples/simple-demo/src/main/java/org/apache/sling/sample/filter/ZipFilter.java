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
package org.apache.sling.sample.filter;

import static org.apache.sling.api.SlingConstants.ATTR_REQUEST_CONTENT;
import static org.apache.sling.api.wrappers.SlingRequestPaths.INCLUDE_REQUEST_URI;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.wrappers.SlingHttpServletResponseWrapper;
import org.apache.sling.engine.RequestUtil;

/**
 * The <code>ZipFilter</code> is an optionally installable global filter,
 * which must be first in the filter chain for the compression functionality to
 * be effective.
 * <p>
 * <b>NOTES</b>
 * <p>
 * When considering this filter, please keep the following notes in mind:
 * <ul>
 * <li>This filter serves as a model for implementing filters.
 * <li>If using this filter it must be registered with a
 * <code>filter.scope</code> parameter of <i><code>request</code></i> and
 * a <code>filter.order</code> value which is smaller than any other
 * <code>request</code> scope filter.
 * <li>With small response data and fast networks this filter is not effective
 * and in fact even slows response time down !!
 * </ul>
 *
 * @scr.component metatype="no" enabled="no"
 * @scr.property name="service.description" value="Sample Request Filter"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.property name="filter.scope" value="request" private="true"
 * @scr.property name="filter.order" value="-2147483648" type="Integer" private="true"
 * @scr.service
 */
public class ZipFilter implements Filter {

    /**
     * Does nothing. This filter has not initialization requirement.
     */
    public void init(FilterConfig filterConfig) {
    }

    /**
     * Checks the <code>Accept-Encoding</code> request header for its values.
     * If <i>gzip</i> or <i>x-gzip</i> is desired, the response stream is
     * compressed using the GZip algorithm. If <i>deflate</i> is desired, the
     * response stream is compressed using the standard Zip Algorithm. Otherwise
     * the response stream is transferred unmodified (<i>identity</i>
     * encoding).
     */
    public void doFilter(ServletRequest req, ServletResponse res,
            FilterChain chain) throws IOException, ServletException {

        // request is Servlet API or Sling API included, do not filter
        if (req.getAttribute(INCLUDE_REQUEST_URI) != null
            || req.getAttribute(ATTR_REQUEST_CONTENT) != null) {
            chain.doFilter(req, res);
            return;
        }

        SlingHttpServletRequest request = (SlingHttpServletRequest) req;
        SlingHttpServletResponse response = (SlingHttpServletResponse) res;

        // check for compress header
        String enc = request.getHeader("Accept-Encoding");
        if (enc == null || enc.length() == 0) {
            // assume identity, no wrapping
            chain.doFilter(request, response);
            return;
        }

        // get settings
        enc = this.getEncoding(enc);
        if ("gzip".equals(enc) || "x-gzip".equals(enc)) {
            // mark the response encoded
            response.setHeader("Content-Encoding", enc);
            response = new DeflaterComponentResponse(response, false);
        } else if ("deflate".equals(enc)) {
            // mark the response encoded
            response.setHeader("Content-Encoding", enc);
            response = new DeflaterComponentResponse(response, true);
        }

        // continue filtering
        chain.doFilter(request, response);

        // finish the output
        if (response instanceof DeflaterComponentResponse) {
            ((DeflaterComponentResponse) response).finish();
        }
    }

    /**
     * Does nothing. This filter has not destroyal requirement.
     */
    public void destroy() {
    }

    // ---------- internal -----------------------------------------------------

    /**
     * Parses the <code>Accept-Encoding</code> header and returns the token
     * with the highest <i>q</i> value or <code>null</code> if the header is
     * empty.
     */
    String getEncoding(String encodingHeader) {
        String select = null;
        double q = Double.MIN_VALUE;
        Map<String, Double> settings = RequestUtil.parserAcceptHeader(encodingHeader);
        for (Iterator<Map.Entry<String, Double>> ti = settings.entrySet().iterator(); ti.hasNext();) {
            Map.Entry<String, Double> token = ti.next();
            Double qv = token.getValue();
            if (qv.doubleValue() > q) {
                q = qv.doubleValue();
                select = token.getKey();
            }
        }
        return select;
    }

    // ---------- internal classes ---------------------------------------------

    /**
     * Compressing <code>ComponentResponseWrapper</code> which compresses the
     * output stream using GZip or Zip algorithm depending on the constructor
     * flag <code>deflate</code>.
     * <p>
     * This wrapper is only
     */
    private static class DeflaterComponentResponse extends
            SlingHttpServletResponseWrapper {

        private boolean deflate;

        private DeflaterServletOutputStream stream;

        private PrintWriter writer;

        public DeflaterComponentResponse(SlingHttpServletResponse delegatee,
                boolean deflate) {
            super(delegatee);
            this.deflate = deflate;
        }

        public ServletOutputStream getOutputStream() throws IOException {
            if (this.stream != null) {
                return this.stream;
            } else if (this.writer != null) {
                throw new IllegalStateException(
                    "Writer has already been obtained");
            }

            this.stream = new DeflaterServletOutputStream(
                this.getDeflaterOutputStream());
            return this.stream;
        }

        public PrintWriter getWriter() throws IOException {
            if (this.writer != null) {
                return this.writer;
            } else if (this.stream != null) {
                throw new IllegalStateException(
                    "OutputStream has already been obtained");
            }

            DeflaterOutputStream base = this.getDeflaterOutputStream();
            String enc = this.getCharacterEncoding();
            this.writer = new PrintWriter(new OutputStreamWriter(base, enc));
            return this.writer;
        }

        public void flushBuffer() throws IOException {
            if (this.writer != null) {
                this.writer.flush();
            } else if (this.stream != null) {
                this.stream.flush();
            }

            super.flushBuffer();
        }

        void finish() throws IOException {
            if (this.writer != null) {
                this.writer.flush();
            } else if (this.stream != null) {
                this.stream.getDeflaterStream().finish();
            }
        }

        protected DeflaterOutputStream getDeflaterOutputStream()
                throws IOException {
            OutputStream base = super.getOutputStream();
            return this.deflate
                    ? new DeflaterOutputStream(base)
                    : new GZIPOutputStream(base);
        }

        private static class DeflaterServletOutputStream extends
                ServletOutputStream {

            private DeflaterOutputStream deflaterStream;

            DeflaterServletOutputStream(DeflaterOutputStream deflaterStream) {
                this.deflaterStream = deflaterStream;
            }

            DeflaterOutputStream getDeflaterStream() {
                return this.deflaterStream;
            }

            public void write(int b) throws IOException {
                this.deflaterStream.write(b);
            }

            public void write(byte[] b) throws IOException {
                this.deflaterStream.write(b);
            }

            public void write(byte[] b, int off, int len) throws IOException {
                this.deflaterStream.write(b, off, len);
            }

            public void flush() throws IOException {
                this.deflaterStream.flush();
            }

            public void close() throws IOException {
                this.deflaterStream.close();
            }
        }
    }
}
