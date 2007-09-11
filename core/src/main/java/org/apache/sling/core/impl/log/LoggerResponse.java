/*
 * Copyright 2007 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.core.impl.log;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;

import org.apache.sling.RequestUtil;
import org.apache.sling.component.ComponentResponse;
import org.apache.sling.component.ComponentResponseWrapper;

/**
 * The <code>LoggerResponse</code> class is a
 * <code>ComponentResponseWrapper</code> which catches all header settings
 * during the request for the request loggers to be able to access them. In
 * addition, the {@link #getOutputStream()} and {@link #getWriter()} methods
 * returned wrapped <code>SerlvetOutputStream</code> and
 * <code>PrintWriter</code> instances which count the number of bytes and
 * characters, resp., written.
 */
class LoggerResponse extends ComponentResponseWrapper {

    // the content type header name
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    
    // the content length header name
    private static final String HEADER_CONTENT_LENGTH = "Content-Length";

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
    private Map cookies;
    
    // the headers set during the request, indexed by lower-case header
    // name, value is string for single-valued and list for multi-valued
    // headers
    private Map headers;
    
    /**
     * Creates an instance of this response wrapper setting the request counter
     * and the time of request start returned by {@link #getRequestStart()} and
     * which is used to calculate the request duration.
     * 
     * @param delegatee The <code>ComponentResponse</code> wrapped by this
     *            wrapper.
     * @param requestId The request counter value to report by
     *            {@link #getRequestId()}.
     */
    LoggerResponse(ComponentResponse delegatee, int requestId) {
        super(delegatee);
        
        this.requestId = requestId;
        this.requestStart = System.currentTimeMillis();
    }

    /**
     * Called to indicate the request processing has ended. This method
     * currently sets the request end time returned by {@link #getRequestEnd()}
     * and which is used to calculate the request duration.
     */
    void requestEnd() {
        requestEnd = System.currentTimeMillis();
    }

    //---------- Retrieving response information ------------------------------
    
    int getRequestId() {
        return requestId;
    }
    
    long getRequestStart() {
        return requestStart;
    }
    
    long getRequestEnd() {
        return requestEnd;
    }
    
    long getRequestDuration() {
        return requestEnd - requestStart;
    }
    
    int getStatus() {
        return status;
    }
    
    int getCount() {
        if (out != null) {
            return out.getCount();
        } else if (writer != null) {
            return writer.getCount();
        }
        
        // otherwise return zero
        return 0;
    }

    Cookie getCookie(String name) {
        return (cookies != null) ? (Cookie) cookies.get(name) : null;
    }
    
    String getHeaders(String name) {
        // normalize header name to lower case to support case-insensitive headers
        name = name.toLowerCase();

        Object header = (headers != null) ? headers.get(name) : null;
        if (header == null) {
            return null;
        } else if (header instanceof String) {
            return (String) header;
        } else {
            StringBuffer headerBuf = new StringBuffer();
            for (Iterator hi=((List) header).iterator(); hi.hasNext(); ) {
                if (headerBuf.length() > 0) {
                    headerBuf.append(",");
                }
                headerBuf.append(hi.next());
            }
            return headerBuf.toString();
        }
    }
    
    //---------- Standard Response overwrites ---------------------------------
 
    public ServletOutputStream getOutputStream() throws IOException {
        if (out == null) {
            ServletOutputStream sos = super.getOutputStream();
            out = new LoggerResponseOutputStream(sos);
        }
        return out;
    }
    
    public PrintWriter getWriter() throws IOException {
        if (writer == null) {
            PrintWriter pw = super.getWriter();
            writer = new LoggerResponseWriter(pw);
        }
        return writer;
    }
    
    public void addCookie(Cookie cookie) {
        
        // register the cookie for later use
        if (cookies == null) {
            cookies = new HashMap();
        }
        cookies.put(cookie.getName(), cookie);
        
        super.addCookie(cookie);
    }
    
    public void addDateHeader(String name, long date) {
        registerHeader(name, RequestUtil.toDateString(date), true);
        super.addDateHeader(name, date);
    }
    
    public void addHeader(String name, String value) {
        registerHeader(name, value, true);
        super.addHeader(name, value);
    }
    
    public void addIntHeader(String name, int value) {
        registerHeader(name, String.valueOf(value), true);
        super.addIntHeader(name, value);
    }
    
    public void setContentLength(int len) {
        registerHeader(HEADER_CONTENT_LENGTH, String.valueOf(len), false);
        super.setContentLength(len);
    }
    
    public void setContentType(String type) {
        registerHeader(HEADER_CONTENT_TYPE, type, false);
        super.setContentType(type);
    }
    
    public void setDateHeader(String name, long date) {
        registerHeader(name, RequestUtil.toDateString(date), false);
        super.setDateHeader(name, date);
    }
    
    public void setHeader(String name, String value) {
        registerHeader(name, value, false);
        super.setHeader(name, value);
    }
    
    public void setIntHeader(String name, int value) {
        registerHeader(name, String.valueOf(value), false);
        setHeader(name, String.valueOf(value));
    }
    
    public void setLocale(Locale loc) {
        // TODO: Might want to register the Content-Language header
        super.setLocale(loc);
    }
    
    public void setStatus(int status) {
        this.status = status;
        super.setStatus(status);
    }

    public void setStatus(int status, String message) {
        this.status = status;
        super.setStatus(status, message);
    }

    public void sendError(int status) throws IOException {
        this.status = status;
        super.sendError(status);
    }

    public void sendError(int status, String message) throws IOException {
        this.status = status;
        super.sendError(status, message);
    }
    
    //--------- Helper Methods ------------------------------------------------
    
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
        if (headers == null) {
            headers = new HashMap();
        }

        // normalize header name to lower case to support case-insensitive headers
        name = name.toLowerCase();

        // retrieve the current contents if adding, otherwise assume no current
        Object current = add ? headers.get(name) : null;
        
        if (current == null) {
            // set the single value (forced if !add)
            headers.put(name, value);
            
        } else if (current instanceof String) {
            // create list if a single value is already set
            List list = new ArrayList();
            list.add(current);
            list.add(value);
            headers.put(name, list);
            
        } else {
            // append to the list of more than one already set
            ((List) current).add(value);
        }
    }
    
    // byte transfer counting ServletOutputStream
    private static class LoggerResponseOutputStream extends ServletOutputStream {
        private ServletOutputStream delegatee;
        private int count;
        
        LoggerResponseOutputStream(ServletOutputStream delegatee) {
            this.delegatee = delegatee;
        }
        
        public int getCount() {
            return count;
        }
        
        public void write(int b) throws IOException {
            delegatee.write(b);
            count++;
        }
        
        public void write(byte[] b) throws IOException {
            delegatee.write(b);
            count += b.length;
        }
        
        public void write(byte[] b, int off, int len) throws IOException {
            delegatee.write(b, off, len);
            count += len;
        }
        
        public void flush() throws IOException {
            delegatee.flush();
        }
        
        public void close() throws IOException {
            delegatee.close();
        }
    }
    
    // character transfer counting PrintWriter
    private static class LoggerResponseWriter extends PrintWriter {

        private static final int LINE_SEPARATOR_LENGTH =
            System.getProperty("line.separator").length();
        
        private int count;
        
        LoggerResponseWriter(PrintWriter delegatee) {
            super(delegatee);
        }
        
        public int getCount() {
            return count;
        }
        
        public void write(int c) {
            super.write(c);
            count++;
        }
        
        public void write(char[] buf, int off, int len) {
            super.write(buf, off, len);
            count += len;
        }
        
        public void write(String s, int off, int len) {
            super.write(s, off, len);
            count += len;
        }
        
        public void println() {
            super.println();
            count += LINE_SEPARATOR_LENGTH;
        }
    }
}