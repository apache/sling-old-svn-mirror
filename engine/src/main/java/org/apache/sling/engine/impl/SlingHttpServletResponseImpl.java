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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.engine.ResponseUtil;
import org.apache.sling.engine.impl.request.RequestData;
import org.apache.sling.engine.servlets.ErrorHandler;

/**
 * The <code>SlingHttpServletResponseImpl</code> TODO
 */
public class SlingHttpServletResponseImpl extends HttpServletResponseWrapper implements SlingHttpServletResponse {

    // the content type header name
    private static final String HEADER_CONTENT_TYPE = "Content-Type";

    // the content length header name
    private static final String HEADER_CONTENT_LENGTH = "Content-Length";

    /** format for RFC 1123 date string -- "Sun, 06 Nov 1994 08:49:37 GMT" */
    private final static SimpleDateFormat RFC1123_FORMAT = new SimpleDateFormat(
        "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);

    /**
     * The counter for request gone through this filter. As this is the first
     * request level filter hit, this counter should actually count each request
     * which at least enters the request level component filter processing.
     * <p>
     * This counter is reset to zero, when this component is activated. That is,
     * each time this component is restarted (system start, bundle start,
     * reconfiguration), the request counter restarts at zero.
     */
    private static int requestCounter = 0;

    // TODO: more content related headers, namely Content-Language should
    // probably be supported

    // the request counter
    private int requestId;

    // the system time in ms when the request entered the system, this is
    // the time this instance was created
    private long requestStart;

    // the system time in ms when the request exited the system, this is
    // the time of the call to the requestEnd() method
    private long requestEnd;

    // the output stream wrapper providing the transferred byte count
    private LoggerResponseOutputStream out;

    // the print writer wrapper providing the transferred character count
    private LoggerResponseWriter writer;

    // the caches status
    private int status = SC_OK;

    // the cookies set during the request, indexed by cookie name
    private Map<String, Cookie> cookies;

    // the headers set during the request, indexed by lower-case header
    // name, value is string for single-valued and list for multi-valued
    // headers
    private Map<String, Object> headers;

    private final RequestData requestData;

    public SlingHttpServletResponseImpl(RequestData requestData,
            HttpServletResponse response) {
        super(response);
        this.requestData = requestData;

        this.requestId = requestCounter++;
        this.requestStart = System.currentTimeMillis();
    }

    /**
     * Called to indicate the request processing has ended. This method
     * currently sets the request end time returned by {@link #getRequestEnd()}
     * and which is used to calculate the request duration.
     */
    public void requestEnd() {
        this.requestEnd = System.currentTimeMillis();
    }

    protected final RequestData getRequestData() {
        return requestData;
    }

    @Override
    public void flushBuffer() throws IOException {
        getRequestData().getContentData().flushBuffer();
    }

    @Override
    public int getBufferSize() {
        return getRequestData().getContentData().getBufferSize();
    }

    @Override
    public Locale getLocale() {
        // TODO Should use our Locale Resolver and not let the component set the locale, right ??
        return getResponse().getLocale();
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (this.out == null) {
            ServletOutputStream sos = getRequestData().getBufferProvider().getOutputStream();
            this.out = new LoggerResponseOutputStream(sos);
        }
        return this.out;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (this.writer == null) {
            PrintWriter pw = getRequestData().getBufferProvider().getWriter();
            this.writer = new LoggerResponseWriter(pw);
        }
        return this.writer;
    }

    @Override
    public boolean isCommitted() {
        // TODO: integrate with our output catcher
        return getResponse().isCommitted();
    }

    @Override
    public void reset() {
        // TODO: integrate with our output catcher
        getResponse().reset();
    }

    @Override
    public void resetBuffer() {
        getRequestData().getContentData().resetBuffer();
    }

    @Override
    public void setBufferSize(int size) {
        getRequestData().getContentData().setBufferSize(size);
    }

    // ---------- Redirection support through PathResolver --------------------

    @Override
    public String encodeURL(String url) {
        // make the path absolute
        url = makeAbsolutePath(url);

        // resolve the url to as if it would be a resource path
        url = map(url);

        // have the servlet container to further encodings
        return super.encodeURL(url);
    }

    @Override
    public String encodeRedirectURL(String url) {
        // make the path absolute
        url = makeAbsolutePath(url);

        // resolve the url to as if it would be a resource path
        url = map(url);

        // have the servlet container to further encodings
        return super.encodeRedirectURL(url);
    }

    @Override
    @Deprecated
    public String encodeUrl(String url) {
        return encodeURL(url);
    }

    @Override
    @Deprecated
    public String encodeRedirectUrl(String url) {
        return encodeRedirectURL(url);
    }

    // ---------- Error handling through Sling Error Resolver -----------------


    @Override
    public void sendRedirect(String location) throws IOException {
        super.sendRedirect(location);

        // replicate the status code of call to base class
        this.status = SC_MOVED_TEMPORARILY;
    }

    @Override
    public void sendError(int status) throws IOException {
        sendError(status, null);
    }

    @Override
    public void sendError(int status, String message) throws IOException {
        checkCommitted();

        this.status = status;
        ErrorHandler eh = getRequestData().getSlingMainServlet().getErrorHandler();
        eh.handleError(status, message, requestData.getSlingRequest(), this);
    }

    @Override
    public void setStatus(int status, String message) {
        checkCommitted();
        this.status = status;
        super.setStatus(status, message);
    }

    @Override
    public void setStatus(int status) {
        checkCommitted();
        this.status = status;
        super.setStatus(status);
    }

    public void addCookie(Cookie cookie) {

        // register the cookie for later use
        if (this.cookies == null) {
            this.cookies = new HashMap<String, Cookie>();
        }
        this.cookies.put(cookie.getName(), cookie);

        super.addCookie(cookie);
    }

    public void addDateHeader(String name, long date) {
        this.registerHeader(name, toDateString(date), true);
        super.addDateHeader(name, date);
    }

    public void addHeader(String name, String value) {
        this.registerHeader(name, value, true);
        super.addHeader(name, value);
    }

    public void addIntHeader(String name, int value) {
        this.registerHeader(name, String.valueOf(value), true);
        super.addIntHeader(name, value);
    }

    public void setContentLength(int len) {
        this.registerHeader(HEADER_CONTENT_LENGTH, String.valueOf(len), false);
        super.setContentLength(len);
    }

    public void setContentType(String type) {
        // SLING-726 No handling required since this seems to be correct
        this.registerHeader(HEADER_CONTENT_TYPE, type, false);
        super.setContentType(type);
    }

    @Override
    public void setCharacterEncoding(String charset) {
        // SLING-726 Ignore call if getWriter() has been called
        if (writer == null) {
            super.setCharacterEncoding(charset);
        }
    }
    
    public void setDateHeader(String name, long date) {
        this.registerHeader(name, toDateString(date), false);
        super.setDateHeader(name, date);
    }

    public void setHeader(String name, String value) {
        this.registerHeader(name, value, false);
        super.setHeader(name, value);
    }

    public void setIntHeader(String name, int value) {
        this.registerHeader(name, String.valueOf(value), false);
        this.setHeader(name, String.valueOf(value));
    }

    public void setLocale(Locale loc) {
        // TODO: Might want to register the Content-Language header
        super.setLocale(loc);
    }

    // ---------- Retrieving response information ------------------------------

    public int getRequestId() {
        return this.requestId;
    }

    public long getRequestStart() {
        return this.requestStart;
    }

    public long getRequestEnd() {
        return this.requestEnd;
    }

    public long getRequestDuration() {
        return this.requestEnd - this.requestStart;
    }

    public int getStatus() {
        return this.status;
    }

    public int getCount() {
        if (this.out != null) {
            return this.out.getCount();
        } else if (this.writer != null) {
            return this.writer.getCount();
        }

        // otherwise return zero
        return 0;
    }

    public Cookie getCookie(String name) {
        return (this.cookies != null) ? (Cookie) this.cookies.get(name) : null;
    }

    public String getHeaders(String name) {
        // normalize header name to lower case to support case-insensitive
        // headers
        name = name.toLowerCase();

        Object header = (this.headers != null) ? this.headers.get(name) : null;
        if (header == null) {
            return null;
        } else if (header instanceof String) {
            return (String) header;
        } else {
            StringBuffer headerBuf = new StringBuffer();
            for (Iterator<?> hi = ((List<?>) header).iterator(); hi.hasNext();) {
                if (headerBuf.length() > 0) {
                    headerBuf.append(",");
                }
                headerBuf.append(hi.next());
            }
            return headerBuf.toString();
        }
    }

    // ---------- Internal helper ---------------------------------------------

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
        return getRequestData().getResourceResolver().map(url);
    }

    /**
     * Stores the name header-value pair in the header map. The name is
     * converted to lower-case before using it as an index in the map.
     *
     * @param name The name of the header to register
     * @param value The value of the header to register
     * @param add If <code>true</code> the header value is added to the list
     *            of potentially existing header values. Otherwise the new value
     *            replaces any existing values.
     */
    private void registerHeader(String name, String value, boolean add) {
        // ensure the headers map
        if (this.headers == null) {
            this.headers = new HashMap<String, Object>();
        }

        // normalize header name to lower case to support case-insensitive
        // headers
        name = name.toLowerCase();

        // retrieve the current contents if adding, otherwise assume no current
        Object current = add ? this.headers.get(name) : null;

        if (current == null) {
            // set the single value (forced if !add)
            this.headers.put(name, value);

        } else if (current instanceof String) {
            // create list if a single value is already set
            List<String> list = new ArrayList<String>();
            list.add((String) current);
            list.add(value);
            this.headers.put(name, list);

        } else {
            // append to the list of more than one already set
            ((List<Object>) current).add(value);
        }
    }

    /**
     * Converts the time value given as the number of milliseconds since January
     * 1, 1970 to a date and time string compliant with RFC 1123 date
     * specification. The resulting string is compliant with section 3.3.1, Full
     * Date, of <a href="http://www.faqs.org/rfcs/rfc2616.html">RFC 2616</a>
     * and may thus be used as the value of date header such as
     * <code>Date</code>.
     *
     * @param date The date value to convert to a string
     * @return The string representation of the date and time value.
     */
    public static String toDateString(long date) {
        synchronized (RFC1123_FORMAT) {
            return RFC1123_FORMAT.format(new Date(date));
        }
    }

    //---------- byte/character counting output channels ----------------------

    // byte transfer counting ServletOutputStream
    private static class LoggerResponseOutputStream extends ServletOutputStream {
        private ServletOutputStream delegatee;

        private int count;

        LoggerResponseOutputStream(ServletOutputStream delegatee) {
            this.delegatee = delegatee;
        }

        public int getCount() {
            return this.count;
        }

        public void write(int b) throws IOException {
            this.delegatee.write(b);
            this.count++;
        }

        public void write(byte[] b) throws IOException {
            this.delegatee.write(b);
            this.count += b.length;
        }

        public void write(byte[] b, int off, int len) throws IOException {
            this.delegatee.write(b, off, len);
            this.count += len;
        }

        public void flush() throws IOException {
            this.delegatee.flush();
        }

        public void close() throws IOException {
            this.delegatee.close();
        }
    }

    // character transfer counting PrintWriter
    private static class LoggerResponseWriter extends PrintWriter {

        private static final int LINE_SEPARATOR_LENGTH = System.getProperty(
            "line.separator").length();

        private int count;

        LoggerResponseWriter(PrintWriter delegatee) {
            super(delegatee);
        }

        public int getCount() {
            return this.count;
        }

        public void write(int c) {
            super.write(c);
            this.count++;
        }

        public void write(char[] buf, int off, int len) {
            super.write(buf, off, len);
            this.count += len;
        }

        public void write(String s, int off, int len) {
            super.write(s, off, len);
            this.count += len;
        }

        public void println() {
            super.println();
            this.count += LINE_SEPARATOR_LENGTH;
        }
    }
}
