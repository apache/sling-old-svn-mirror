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
package org.apache.sling.commons.testing.sling;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.request.RequestProgressTracker;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;

/**
 * Mock request object. This does not do anything useful, it just returns the
 * constructor parameter <code>secure</code> in the <code>isSecure</code>
 * method.
 */
public class MockSlingHttpServletRequest implements SlingHttpServletRequest {

    private Resource resource;

    private String method;

    private final RequestPathInfo requestPathInfo;

    private final String queryString;

    private boolean secure = false;

    private MockResourceResolver mockResourceResolver;
    
    private RequestProgressTracker mockRequestProgressTracker;

    public static final String RESOURCE_TYPE = "foo/bar";

    MockSlingHttpServletRequest() {
        this(null, null, null, null, null);
    }

    public MockSlingHttpServletRequest(String resourcePath, String selectors,
            String extension, String suffix, String queryString) {
        this.resource = new SyntheticResource(null, resourcePath, RESOURCE_TYPE);
        this.requestPathInfo = new MockRequestPathInfo(selectors, extension,
            suffix);
        this.queryString = queryString;

        setMethod(null);
    }

    public void setResourceResolver(MockResourceResolver resolver) {
        this.mockResourceResolver = resolver;

        // recreate request resource with the new resolver
        if (resource.getResourceResolver() == null) {
            this.resource = new SyntheticResource(resolver, resource.getPath(),
                resource.getResourceType());
        }
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public void setMethod(String method) {
        this.method = (method == null) ? "GET" : method.toUpperCase();
    }

    public Cookie getCookie(String name) {
        return null;
    }

    public RequestDispatcher getRequestDispatcher(String path,
            RequestDispatcherOptions options) {
        return null;
    }

    public RequestDispatcher getRequestDispatcher(Resource resource,
            RequestDispatcherOptions options) {
        return null;
    }

    public RequestDispatcher getRequestDispatcher(Resource resource) {
        return null;
    }

    public RequestParameter getRequestParameter(String name) {
        return null;
    }

    public RequestParameterMap getRequestParameterMap() {
        return null;
    }

    public RequestParameter[] getRequestParameters(String name) {
        return null;
    }

    public RequestPathInfo getRequestPathInfo() {
        return requestPathInfo;
    }

    public RequestProgressTracker getRequestProgressTracker() {
        if (mockRequestProgressTracker == null) {
            mockRequestProgressTracker = new MockRequestProgressTracker();
        }
        return mockRequestProgressTracker;
    }

    public Resource getResource() {
        return resource;
    }

    public ResourceBundle getResourceBundle(Locale locale) {
        return null;
    }

    public ResourceBundle getResourceBundle(String baseName, Locale locale) {
        return null;
    }

    public ResourceResolver getResourceResolver() {
        return mockResourceResolver;
    }

    public String getResponseContentType() {
        return null;
    }

    public Enumeration<String> getResponseContentTypes() {
        return null;
    }

    public String getAuthType() {
        return null;
    }

    public String getContextPath() {
        return null;
    }

    public Cookie[] getCookies() {
        return null;
    }

    public long getDateHeader(String name) {
        return 0;
    }

    public String getHeader(String name) {
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
        return null;
    }

    public String getPathTranslated() {
        return null;
    }

    public String getQueryString() {
        return queryString;
    }

    public String getRemoteUser() {
        return null;
    }

    public String getRequestURI() {
        return null;
    }

    public StringBuffer getRequestURL() {
        return null;
    }

    public String getRequestedSessionId() {
        return null;
    }

    public String getServletPath() {
        return null;
    }

    public HttpSession getSession() {
        return null;
    }

    public HttpSession getSession(boolean create) {
        return null;
    }

    public Principal getUserPrincipal() {
        return null;
    }

    public boolean isRequestedSessionIdFromCookie() {
        return false;
    }

    public boolean isRequestedSessionIdFromURL() {
        return false;
    }

    public boolean isRequestedSessionIdFromUrl() {
        return false;
    }

    public boolean isRequestedSessionIdValid() {
        return false;
    }

    public boolean isUserInRole(String role) {
        return false;
    }

    public Object getAttribute(String name) {
        return null;
    }

    public Enumeration<?> getAttributeNames() {
        return null;
    }

    public String getCharacterEncoding() {
        return null;
    }

    public int getContentLength() {
        return 0;
    }

    public String getContentType() {
        return null;
    }

    public ServletInputStream getInputStream() {
        return null;
    }

    public String getLocalAddr() {
        return null;
    }

    public String getLocalName() {
        return null;
    }

    public int getLocalPort() {
        return 0;
    }

    public Locale getLocale() {
        return null;
    }

    public Enumeration<?> getLocales() {
        return null;
    }

    public String getParameter(String name) {
        return null;
    }

    public Map<?, ?> getParameterMap() {
        return null;
    }

    public Enumeration<?> getParameterNames() {
        return null;
    }

    public String[] getParameterValues(String name) {
        return null;
    }

    public String getProtocol() {
        return null;
    }

    public BufferedReader getReader() {
        return null;
    }

    public String getRealPath(String path) {
        return null;
    }

    public String getRemoteAddr() {
        return null;
    }

    public String getRemoteHost() {
        return null;
    }

    public int getRemotePort() {
        return 0;
    }

    public RequestDispatcher getRequestDispatcher(String path) {
        return null;
    }

    public String getScheme() {
        return null;
    }

    public String getServerName() {
        return null;
    }

    public int getServerPort() {
        return 0;
    }

    public boolean isSecure() {
        return this.secure;
    }

    public void removeAttribute(String name) {

    }

    public void setAttribute(String name, Object o) {

    }

    public void setCharacterEncoding(String env) {

    }
}
