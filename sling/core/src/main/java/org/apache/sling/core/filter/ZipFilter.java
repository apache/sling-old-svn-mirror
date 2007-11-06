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
package org.apache.sling.core.filter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletOutputStream;

import org.apache.sling.component.ComponentContext;
import org.apache.sling.component.ComponentException;
import org.apache.sling.component.ComponentFilter;
import org.apache.sling.component.ComponentFilterChain;
import org.apache.sling.component.ComponentRequest;
import org.apache.sling.component.ComponentResponse;
import org.apache.sling.component.ComponentResponseWrapper;
import org.apache.sling.core.Constants;
import org.apache.sling.core.RequestUtil;


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
 */
public class ZipFilter implements ComponentFilter {

    /**
     * Does nothing. This filter has not initialization requirement.
     */
    public void init(ComponentContext componentContext) {
    }

    /**
     * Checks the <code>Accept-Encoding</code> request header for its values.
     * If <i>gzip</i> or <i>x-gzip</i> is desired, the response stream is
     * compressed using the GZip algorithm. If <i>deflate</i> is desired, the
     * response stream is compressed using the standard Zip Algorithm. Otherwise
     * the response stream is transferred unmodified (<i>identity</i>
     * encoding).
     */
    public void doFilter(ComponentRequest request, ComponentResponse response,
            ComponentFilterChain filterChain) throws IOException,
            ComponentException {

        // request is Servlet API or Component API included, do not filter
        if (request.getAttribute(Constants.INCLUDE_REQUEST_URI) != null
            || request.getAttribute(Constants.ATTR_REQUEST_CONTENT) != null) {
            filterChain.doFilter(request, response);
            return;
        }

        // check for compress header
        String enc = request.getHeader("Accept-Encoding");
        if (enc == null || enc.length() == 0) {
            // assume identity, no wrapping
            filterChain.doFilter(request, response);
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
        filterChain.doFilter(request, response);

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
        Map settings = RequestUtil.parserAcceptHeader(encodingHeader);
        for (Iterator ti = settings.entrySet().iterator(); ti.hasNext();) {
            Map.Entry token = (Map.Entry) ti.next();
            Double qv = (Double) token.getValue();
            if (qv.doubleValue() > q) {
                q = qv.doubleValue();
                select = (String) token.getKey();
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
    private static class DeflaterComponentResponse extends ComponentResponseWrapper {

        private boolean deflate;

        private DeflaterServletOutputStream stream;

        private PrintWriter writer;

        public DeflaterComponentResponse(ComponentResponse delegatee, boolean deflate) {
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

            this.stream = new DeflaterServletOutputStream(this.getDeflaterOutputStream());
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

        private static class DeflaterServletOutputStream extends ServletOutputStream {

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
