/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.hc.support.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class InternalRequest implements HttpServletRequest {

    private final String path;
    private final Map<String, Object> attributes = new HashMap<String, Object>();
    
    public InternalRequest(String path) {
        this.path = path;
    }
    
    @Override
    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    @Override
    public Enumeration<?> getAttributeNames() {
        return new Vector<String>(attributes.keySet()).elements();
    }

    @Override
    public String getCharacterEncoding() {
        return "UTF-8";
    }

    @Override
    public int getContentLength() {
        return 0;
    }

    @Override
    public String getContentType() {
        return "text/plain";
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return new ServletInputStream() {
            @Override
            public int read() throws IOException {
                return 0;
            }
        };
    }

    @Override
    public String getLocalAddr() {
        return "127.0.0.1";
    }

    @Override
    public String getLocalName() {
        return "localhost";
    }

    @Override
    public int getLocalPort() {
        return 0;
    }

    @Override
    public Locale getLocale() {
        return Locale.getDefault();
    }

    @Override
    public Enumeration<?> getLocales() {
        return new Vector<Locale>().elements();
    }

    @Override
    public String getParameter(String arg0) {
        return null;
    }

    @Override
    public Map<?,?> getParameterMap() {
        return new HashMap<String, Object>();
    }

    @Override
    public Enumeration<?> getParameterNames() {
        return new Vector<String>().elements();
    }

    @Override
    public String[] getParameterValues(String arg0) {
        return null;
    }

    @Override
    public String getProtocol() {
        return "http";
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new StringReader(""));
    }

    @Override
    public String getRealPath(String arg0) {
        return path;
    }

    @Override
    public String getRemoteAddr() {
        return "127.0.0.1";
    }

    @Override
    public String getRemoteHost() {
        return "localhost";
    }

    @Override
    public int getRemotePort() {
        return 1234;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String arg0) {
        return null;
    }

    @Override
    public String getScheme() {
        return "http";
    }

    @Override
    public String getServerName() {
        return "localhost";
    }

    @Override
    public int getServerPort() {
        return 80;
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public void removeAttribute(String arg0) {
    }

    @Override
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @Override
    public void setCharacterEncoding(String arg0)
            throws UnsupportedEncodingException {
    }

    @Override
    public String getAuthType() {
        return null;
    }

    @Override
    public String getContextPath() {
        return "";
    }

    @Override
    public Cookie[] getCookies() {
        return null;
    }

    @Override
    public long getDateHeader(String arg0) {
        return 0;
    }

    @Override
    public String getHeader(String arg0) {
        return null;
    }

    @Override
    public Enumeration<?> getHeaderNames() {
        return new Vector<String>().elements();
    }

    @Override
    public Enumeration<?> getHeaders(String arg0) {
        return new Vector<String>().elements();
    }

    @Override
    public int getIntHeader(String arg0) {
        return 0;
    }

    @Override
    public String getMethod() {
        return "GET";
    }

    @Override
    public String getPathInfo() {
        return path;
    }

    @Override
    public String getPathTranslated() {
        return path;
    }

    @Override
    public String getQueryString() {
        return "";
    }

    @Override
    public String getRemoteUser() {
        return "remoteuser";
    }

    @Override
    public String getRequestURI() {
        return "http://localhost" + path;
    }

    @Override
    public StringBuffer getRequestURL() {
        return new StringBuffer(getRequestURI());
    }

    @Override
    public String getRequestedSessionId() {
        return "";
    }

    @Override
    public String getServletPath() {
        return "";
    }

    @Override
    public HttpSession getSession() {
        return null;
    }

    @Override
    public HttpSession getSession(boolean arg0) {
        return null;
    }

    @Override
    public Principal getUserPrincipal() {
        return null;
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromUrl() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        return false;
    }

    @Override
    public boolean isUserInRole(String arg0) {
        return false;
    }
}
