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
package org.apache.sling.bgservlets.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class BackgroundHttpServletRequest implements HttpServletRequest {

    private final String contextPath;
    private final String method;
    private final String pathInfo;
    private final String servletPath;
    private final String queryString;
    private final String requestURI;
    private final StringBuffer requestURL;
    private final String characterEncoding;
    private final int contentLength;
    private final String contentType;
    private final Locale locale;
    private final String protocol;
    private final String remoteAddr;
    private final String remoteHost;
    private final int remotePort;
    private final int serverPort;
    private final String scheme;
    private final String remoteUser;
    private final String serverName;

    private final Map<String, Object> attributes;
    private final Map<String, ?> parameters;

    static class IteratorEnumeration<T> implements Enumeration<T> {
        private final Iterator<T> it;

        IteratorEnumeration(Iterator<T> it) {
            this.it = it;
        }

        public boolean hasMoreElements() {
            return it.hasNext();
        }

        public T nextElement() {
            return it.next();
        }
    }

    /**
     * We throw this for any method for which we do not have data that's safe to
     * use outside of the container's request/response cycle. Start by throwing
     * this everywhere and implement methods as needed, if their data is safe to
     * use.
     */
    @SuppressWarnings("serial")
    class UnsupportedBackgroundOperationException extends
            UnsupportedOperationException {
        UnsupportedBackgroundOperationException() {
            super("This operation is not supported for background requests");
        }
    }

    @SuppressWarnings("unchecked")
    BackgroundHttpServletRequest(HttpServletRequest r,
            String[] parametersToRemove) {

        // Store objects which are safe to use outside
        // of the container's request/response cycle - the
        // goal is to release r once this request starts
        // executing in the background
        contextPath = r.getContextPath();
        method = r.getMethod();
        pathInfo = r.getPathInfo();
        servletPath = r.getServletPath();
        queryString = r.getQueryString();
        requestURI = r.getRequestURI();
        requestURL = r.getRequestURL();
        characterEncoding = r.getCharacterEncoding();
        contentLength = r.getContentLength();
        contentType = r.getContentType();
        locale = r.getLocale();
        protocol = r.getProtocol();
        remoteAddr = r.getRemoteAddr();
        remoteHost = r.getRemoteHost();
        remotePort = r.getRemotePort();
        serverPort = r.getServerPort();
        scheme = r.getScheme();
        remoteUser = r.getRemoteUser();
        serverName = r.getServerName();

        attributes = new HashMap<String, Object>();
        /*
         * Don't copy attributes, we consider this to be a "fresh" request final
         * Enumeration<?> e = r.getAttributeNames(); while(e.hasMoreElements())
         * { final String key = (String)e.nextElement(); attributes.put(key,
         * r.getAttribute(key)); }
         */

        parameters = new HashMap<String, String>();
        parameters.putAll(r.getParameterMap());
        for (String key : parametersToRemove) {
            parameters.remove(key);
        }
    }

    public String getAuthType() {
        return null;
    }

    public String getContextPath() {
        return contextPath;
    }

    public Cookie[] getCookies() {
        return null;
    }

    public long getDateHeader(String arg0) {
        return 0;
    }

    public String getHeader(String arg0) {
        return null;
    }

    public Enumeration<?> getHeaderNames() {
        return null;
    }

    public Enumeration<?> getHeaders(String name) {
        return null;
    }

    public int getIntHeader(String name) {
        return 0;
    }

    public String getMethod() {
        return method;
    }

    public String getPathInfo() {
        return pathInfo;
    }

    public String getPathTranslated() {
        throw new UnsupportedBackgroundOperationException();
    }

    public String getQueryString() {
        return queryString;
    }

    public String getRemoteUser() {
        return remoteUser;
    }

    public String getRequestedSessionId() {
        throw new UnsupportedBackgroundOperationException();
    }

    public String getRequestURI() {
        return requestURI;
    }

    public StringBuffer getRequestURL() {
        return requestURL;
    }

    public String getServletPath() {
        return servletPath;
    }

    public HttpSession getSession() {
        throw new UnsupportedBackgroundOperationException();
    }

    public HttpSession getSession(boolean arg0) {
        throw new UnsupportedBackgroundOperationException();
    }

    public Principal getUserPrincipal() {
        throw new UnsupportedBackgroundOperationException();
    }

    public boolean isRequestedSessionIdFromCookie() {
        throw new UnsupportedBackgroundOperationException();
    }

    public boolean isRequestedSessionIdFromUrl() {
        throw new UnsupportedBackgroundOperationException();
    }

    public boolean isRequestedSessionIdFromURL() {
        throw new UnsupportedBackgroundOperationException();
    }

    public boolean isRequestedSessionIdValid() {
        throw new UnsupportedBackgroundOperationException();
    }

    public boolean isUserInRole(String arg0) {
        throw new UnsupportedBackgroundOperationException();
    }

    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    public Enumeration<?> getAttributeNames() {
        return new IteratorEnumeration<String>(attributes.keySet().iterator());
    }

    public String getCharacterEncoding() {
        return characterEncoding;
    }

    public int getContentLength() {
        return contentLength;
    }

    public String getContentType() {
        return contentType;
    }

    public ServletInputStream getInputStream() throws IOException {
        throw new UnsupportedBackgroundOperationException();
    }

    public String getLocalAddr() {
        throw new UnsupportedBackgroundOperationException();
    }

    public Locale getLocale() {
        return locale;
    }

    public Enumeration<?> getLocales() {
        throw new UnsupportedBackgroundOperationException();
    }

    public String getLocalName() {
        throw new UnsupportedBackgroundOperationException();
    }

    public int getLocalPort() {
        throw new UnsupportedBackgroundOperationException();
    }

    public String getParameter(String name) {
        final Object obj = parameters.get(name);
        if (obj instanceof String[]) {
            return ((String[]) obj)[0];
        }
        return (String) obj;
    }

    public Map<?, ?> getParameterMap() {
        return parameters;
    }

    public Enumeration<?> getParameterNames() {
        return new IteratorEnumeration<String>(parameters.keySet().iterator());
    }

    public String[] getParameterValues(String key) {
        throw new UnsupportedBackgroundOperationException();
    }

    public String getProtocol() {
        return protocol;
    }

    public BufferedReader getReader() throws IOException {
        throw new UnsupportedBackgroundOperationException();
    }

    public String getRealPath(String arg0) {
        throw new UnsupportedBackgroundOperationException();
    }

    public String getRemoteAddr() {
        return remoteAddr;
    }

    public String getRemoteHost() {
        return remoteHost;
    }

    public int getRemotePort() {
        return remotePort;
    }

    public RequestDispatcher getRequestDispatcher(String arg0) {
        throw new UnsupportedBackgroundOperationException();
    }

    public String getScheme() {
        return scheme;
    }

    public String getServerName() {
        return serverName;
    }

    public int getServerPort() {
        return serverPort;
    }

    public boolean isSecure() {
        return false;
    }

    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    public void setCharacterEncoding(String arg0)
            throws UnsupportedEncodingException {
        throw new UnsupportedBackgroundOperationException();

    }
}
