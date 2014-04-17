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
import java.io.PrintWriter;
import java.util.Locale;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.engine.impl.request.RequestData;

public class SlingHttpServletResponseImpl extends HttpServletResponseWrapper implements SlingHttpServletResponse {

    public static class WriterAlreadyClosedException extends IllegalStateException {
        // just a marker class.
    }

    private final RequestData requestData;

    private final boolean firstSlingResponse;

    public SlingHttpServletResponseImpl(RequestData requestData,
            HttpServletResponse response) {
        super(response);
        this.requestData = requestData;
        this.firstSlingResponse = !(response instanceof SlingHttpServletResponse);
    }

    protected final RequestData getRequestData() {
        return requestData;
    }

    //---------- Adaptable interface

    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        return getRequestData().adaptTo(this, type);
    }

    // ---------- Redirection support through PathResolver --------------------

    @Override
    public String encodeURL(final String url) {
        // remove context path
        String path = removeContextPath(url);

        // make the path absolute
        path = makeAbsolutePath(path);

        // resolve the url to as if it would be a resource path
        path = map(path);

        // have the servlet container to further encodings
        return super.encodeURL(path);
    }

    @Override
    public String encodeRedirectURL(final String url) {
        // remove context path
        String path = removeContextPath(url);

        // make the path absolute
        path = makeAbsolutePath(path);

        // resolve the url to as if it would be a resource path
        path = map(path);

        // have the servlet container to further encodings
        return super.encodeRedirectURL(path);
    }

    @Override
    @Deprecated
    public String encodeUrl(final String url) {
        return encodeURL(url);
    }

    @Override
    @Deprecated
    public String encodeRedirectUrl(final String url) {
        return encodeRedirectURL(url);
    }

    // ---------- Error handling through Sling Error Resolver -----------------

    @Override
    public void sendError(int status) throws IOException {
        sendError(status, null);
    }

    @Override
    public void sendError(int status, String message) throws IOException {
        checkCommitted();

        SlingRequestProcessorImpl eh = getRequestData().getSlingRequestProcessor();
        eh.handleError(status, message, requestData.getSlingRequest(), this);
    }


    // ---------- Internal helper ---------------------------------------------

    @Override
    public PrintWriter getWriter() throws IOException {
        PrintWriter result = super.getWriter();
        if ( firstSlingResponse ) {
            final PrintWriter delegatee = result;
            result = new PrintWriter(result) {

                private boolean isClosed = false;

                private void checkClosed() {
                    if ( this.isClosed ) {
                        throw new WriterAlreadyClosedException();
                    }
                }

                @Override
                public PrintWriter append(final char arg0) {
                    this.checkClosed();
                    return delegatee.append(arg0);
                }

                @Override
                public PrintWriter append(final CharSequence arg0, final int arg1, final int arg2) {
                    this.checkClosed();
                    return delegatee.append(arg0, arg1, arg2);
                }

                @Override
                public PrintWriter append(final CharSequence arg0) {
                    this.checkClosed();
                    return delegatee.append(arg0);
                }

                @Override
                public boolean checkError() {
                    this.checkClosed();
                    return delegatee.checkError();
                }

                @Override
                public void close() {
                    this.checkClosed();
                    this.isClosed = true;
                    delegatee.close();
                }

                @Override
                public void flush() {
                    this.checkClosed();
                    delegatee.flush();
                }

                @Override
                public PrintWriter format(final Locale arg0, final String arg1,
                        final Object... arg2) {
                    this.checkClosed();
                    return delegatee.format(arg0, arg1, arg2);
                }

                @Override
                public PrintWriter format(final String arg0, final Object... arg1) {
                    this.checkClosed();
                    return delegatee.format(arg0, arg1);
                }

                @Override
                public void print(final boolean arg0) {
                    this.checkClosed();
                    delegatee.print(arg0);
                }

                @Override
                public void print(final char arg0) {
                    this.checkClosed();
                    delegatee.print(arg0);
                }

                @Override
                public void print(final char[] arg0) {
                    this.checkClosed();
                    delegatee.print(arg0);
                }

                @Override
                public void print(final double arg0) {
                    this.checkClosed();
                    delegatee.print(arg0);
                }

                @Override
                public void print(final float arg0) {
                    this.checkClosed();
                    delegatee.print(arg0);
                }

                @Override
                public void print(final int arg0) {
                    this.checkClosed();
                    delegatee.print(arg0);
                }

                @Override
                public void print(final long arg0) {
                    this.checkClosed();
                    delegatee.print(arg0);
                }

                @Override
                public void print(final Object arg0) {
                    this.checkClosed();
                    delegatee.print(arg0);
                }

                @Override
                public void print(final String arg0) {
                    this.checkClosed();
                    delegatee.print(arg0);
                }

                @Override
                public PrintWriter printf(final Locale arg0, final String arg1,
                        final Object... arg2) {
                    this.checkClosed();
                    return delegatee.printf(arg0, arg1, arg2);
                }

                @Override
                public PrintWriter printf(final String arg0, final Object... arg1) {
                    this.checkClosed();
                    return delegatee.printf(arg0, arg1);
                }

                @Override
                public void println() {
                    this.checkClosed();
                    delegatee.println();
                }

                @Override
                public void println(final boolean arg0) {
                    this.checkClosed();
                    delegatee.println(arg0);
                }

                @Override
                public void println(final char arg0) {
                    this.checkClosed();
                    delegatee.println(arg0);
                }

                @Override
                public void println(final char[] arg0) {
                    this.checkClosed();
                    delegatee.println(arg0);
                }

                @Override
                public void println(final double arg0) {
                    this.checkClosed();
                    delegatee.println(arg0);
                }

                @Override
                public void println(final float arg0) {
                    this.checkClosed();
                    delegatee.println(arg0);
                }

                @Override
                public void println(final int arg0) {
                    this.checkClosed();
                    delegatee.println(arg0);
                }

                @Override
                public void println(final long arg0) {
                    this.checkClosed();
                    delegatee.println(arg0);
                }

                @Override
                public void println(final Object arg0) {
                    this.checkClosed();
                    delegatee.println(arg0);
                }

                @Override
                public void println(final String arg0) {
                    this.checkClosed();
                    delegatee.println(arg0);
                }

                @Override
                public void write(final char[] arg0, final int arg1, final int arg2) {
                    this.checkClosed();
                    delegatee.write(arg0, arg1, arg2);
                }

                @Override
                public void write(final char[] arg0) {
                    this.checkClosed();
                    delegatee.write(arg0);
                }

                @Override
                public void write(final int arg0) {
                    this.checkClosed();
                    delegatee.write(arg0);
                }

                @Override
                public void write(final String arg0, final int arg1, final int arg2) {
                    this.checkClosed();
                    delegatee.write(arg0, arg1, arg2);
                }

                @Override
                public void write(final String arg0) {
                    this.checkClosed();
                    delegatee.write(arg0);
                }

            };
        }
        return result;
    }

    private void checkCommitted() {
        if (isCommitted()) {
            throw new IllegalStateException(
                "Response has already been committed");
        }
    }

    private String makeAbsolutePath(String path) {
        if (path.startsWith("/")) {
            return path;
        }

        String base = getRequestData().getContentData().getResource().getPath();
        int lastSlash = base.lastIndexOf('/');
        if (lastSlash >= 0) {
            path = base.substring(0, lastSlash+1) + path;
        } else {
            path = "/" + path;
        }

        return path;
    }

    private String map(String url) {
        return getRequestData().getResourceResolver().map(getRequestData().getServletRequest(), url);
    }

    private String removeContextPath(final String path) {
        final String contextPath = this.getRequestData().getSlingRequest().getContextPath().concat("/");
        if ( contextPath.length() > 1 && path.startsWith(contextPath) ) {
            return path.substring(contextPath.length() - 1);
        }
        return path;
    }
}
