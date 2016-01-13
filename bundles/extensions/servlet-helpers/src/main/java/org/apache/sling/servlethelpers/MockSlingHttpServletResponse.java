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
package org.apache.sling.servlethelpers;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.adapter.SlingAdaptable;

import aQute.bnd.annotation.ConsumerType;

/**
 * Mock {@link SlingHttpServletResponse} implementation.
 */
@ConsumerType
public class MockSlingHttpServletResponse extends SlingAdaptable implements SlingHttpServletResponse {

    static final String CHARSET_SEPARATOR = ";charset=";

    private String contentType;
    private String characterEncoding;
    private int contentLength;
    private int status = HttpServletResponse.SC_OK;
    private int bufferSize = 1024 * 8;
    private boolean isCommitted;
    private final HeaderSupport headerSupport = new HeaderSupport();
    private final ResponseBodySupport bodySupport = new ResponseBodySupport();
    private final CookieSupport cookieSupport = new CookieSupport();

    @Override
    public String getContentType() {
        if (this.contentType == null) {
            return null;
        } else {
            return this.contentType
                    + (StringUtils.isNotBlank(characterEncoding) ? CHARSET_SEPARATOR + characterEncoding : "");
        }
    }

    @Override
    public void setContentType(String type) {
        this.contentType = type;
        if (StringUtils.contains(this.contentType, CHARSET_SEPARATOR)) {
            this.characterEncoding = StringUtils.substringAfter(this.contentType, CHARSET_SEPARATOR);
            this.contentType = StringUtils.substringBefore(this.contentType, CHARSET_SEPARATOR);
        }
    }

    @Override
    public void setCharacterEncoding(String charset) {
        this.characterEncoding = charset;
    }

    @Override
    public String getCharacterEncoding() {
        return this.characterEncoding;
    }

    @Override
    public void setContentLength(int len) {
        this.contentLength = len;
    }

    public int getContentLength() {
        return this.contentLength;
    }

    @Override
    public void setStatus(int sc, String sm) {
        setStatus(sc);
    }

    @Override
    public void setStatus(int sc) {
        this.status = sc;
    }

    @Override
    public int getStatus() {
        return this.status;
    }

    @Override
    public void sendError(int sc, String msg) {
        setStatus(sc);
    }

    @Override
    public void sendError(int sc) {
        setStatus(sc);
    }

    @Override
    public void sendRedirect(String location) {
        setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
        setHeader("Location", location);
    }

    @Override
    public void addHeader(String name, String value) {
        headerSupport.addHeader(name, value);
    }

    @Override
    public void addIntHeader(String name, int value) {
        headerSupport.addIntHeader(name, value);
    }

    @Override
    public void addDateHeader(String name, long date) {
        headerSupport.addDateHeader(name, date);
    }

    @Override
    public void setHeader(String name, String value) {
        headerSupport.setHeader(name, value);
    }

    @Override
    public void setIntHeader(String name, int value) {
        headerSupport.setIntHeader(name, value);
    }

    @Override
    public void setDateHeader(String name, long date) {
        headerSupport.setDateHeader(name, date);
    }

    @Override
    public boolean containsHeader(String name) {
        return headerSupport.containsHeader(name);
    }

    @Override
    public String getHeader(String name) {
        return headerSupport.getHeader(name);
    }

    @Override
    public Collection<String> getHeaders(String name) {
        return headerSupport.getHeaders(name);
    }

    @Override
    public Collection<String> getHeaderNames() {
        return headerSupport.getHeaderNames();
    }

    @Override
    public PrintWriter getWriter() {
        return bodySupport.getWriter(getCharacterEncoding());
    }

    @Override
    public ServletOutputStream getOutputStream() {
        return bodySupport.getOutputStream();
    }

    @Override
    public void reset() {
        if (isCommitted()) {
            throw new IllegalStateException("Response already committed.");
        }
        bodySupport.reset();
        headerSupport.reset();
        cookieSupport.reset();
        status = HttpServletResponse.SC_OK;
        contentLength = 0;
    }

    @Override
    public void resetBuffer() {
        if (isCommitted()) {
            throw new IllegalStateException("Response already committed.");
        }
        bodySupport.reset();
    }

    @Override
    public int getBufferSize() {
        return this.bufferSize;
    }

    @Override
    public void setBufferSize(int size) {
        this.bufferSize = size;
    }

    @Override
    public void flushBuffer() {
        isCommitted = true;
    }

    @Override
    public boolean isCommitted() {
        return isCommitted;
    }

    public byte[] getOutput() {
        return bodySupport.getOutput();
    }

    public String getOutputAsString() {
        return bodySupport.getOutputAsString(getCharacterEncoding());
    }

    @Override
    public void addCookie(Cookie cookie) {
        cookieSupport.addCookie(cookie);
    }

    /**
     * Get cookie
     * @param name Cookie name
     * @return Cookie or null
     */
    public Cookie getCookie(String name) {
        return cookieSupport.getCookie(name);
    }

    /**
     * Get cookies
     * @return Cookies array or null if no cookie defined
     */
    public Cookie[] getCookies() {
        return cookieSupport.getCookies();
    }

    // --- unsupported operations ---
    @Override
    public Locale getLocale() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setLocale(Locale loc) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String encodeRedirectUrl(String url) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String encodeRedirectURL(String url) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String encodeUrl(String url) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String encodeURL(String url) {
        throw new UnsupportedOperationException();
    }
}
