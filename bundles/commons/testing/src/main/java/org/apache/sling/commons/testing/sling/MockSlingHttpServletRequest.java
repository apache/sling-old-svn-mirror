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
import java.io.IOException;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;

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

	private final String scheme;
	private final String server;
	private final int port;
	private final String contextPath;

    private boolean secure = false;

    private ResourceResolver mockResourceResolver;

    private RequestProgressTracker mockRequestProgressTracker;

    public static final String RESOURCE_TYPE = "foo/bar";

    MockSlingHttpServletRequest() {
        this(null, null, null, null, null);
    }

    public MockSlingHttpServletRequest(String resourcePath, String selectors,
            String extension, String suffix, String queryString) {
		this(resourcePath, selectors, extension, suffix, queryString,
			resourcePath, null, null, 0, null);
    }

	public MockSlingHttpServletRequest(String resourcePath, String selectors,
			String extension, String suffix, String queryString,
			String requestPath, String scheme, String server, int port,
			String contextPath) {
		this.resource = new SyntheticResource(null, resourcePath, RESOURCE_TYPE);
		this.requestPathInfo = new MockRequestPathInfo(selectors, extension,
			suffix, requestPath);
		this.queryString = queryString;
		this.scheme = scheme;
		this.server = server;
		this.port = port;
		this.contextPath = contextPath;

		setMethod(null);
	}

    public void setResourceResolver(ResourceResolver resolver) {
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

    @Override
    public Cookie getCookie(String name) {
        return null;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path,
            RequestDispatcherOptions options) {
        return null;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(Resource resource,
            RequestDispatcherOptions options) {
        return null;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(Resource resource) {
        return null;
    }

    @Override
    public RequestParameter getRequestParameter(String name) {
        return null;
    }

    @Override
    public RequestParameterMap getRequestParameterMap() {
        return null;
    }

    @Override
    public RequestParameter[] getRequestParameters(String name) {
        return null;
    }

    @Override
    public RequestPathInfo getRequestPathInfo() {
        return requestPathInfo;
    }

    @Override
    public RequestProgressTracker getRequestProgressTracker() {
        if (mockRequestProgressTracker == null) {
            mockRequestProgressTracker = new MockRequestProgressTracker();
        }
        return mockRequestProgressTracker;
    }

    @Override
    public Resource getResource() {
        return resource;
    }

    @Override
    public ResourceBundle getResourceBundle(Locale locale) {
        return null;
    }

    @Override
    public ResourceBundle getResourceBundle(String baseName, Locale locale) {
        return null;
    }

    @Override
    public ResourceResolver getResourceResolver() {
        return mockResourceResolver;
    }

    @Override
    public String getResponseContentType() {
        return null;
    }

    @Override
    public Enumeration<String> getResponseContentTypes() {
        return null;
    }

    @Override
    public String getAuthType() {
        return null;
    }

	@Override
    public String getContextPath() {
		return contextPath;
	}

    @Override
    public Cookie[] getCookies() {
        return null;
    }

    @Override
    public long getDateHeader(String name) {
        return 0;
    }

    @Override
    public String getHeader(String name) {
        return null;
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return null;
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        return null;
    }

    @Override
    public int getIntHeader(String name) {
        return 0;
    }

    @Override
    public String getMethod() {
        return method;
    }

    @Override
    public String getPathInfo() {
        return null;
    }

    @Override
    public String getPathTranslated() {
        return null;
    }

    @Override
    public String getQueryString() {
        return queryString;
    }

    @Override
    public String getRemoteUser() {
        return null;
    }

    @Override
    public String getRequestURI() {
        return null;
    }

    @Override
    public StringBuffer getRequestURL() {
        return null;
    }

    @Override
    public String getRequestedSessionId() {
        return null;
    }

    @Override
    public String getServletPath() {
        return null;
    }

    @Override
    public HttpSession getSession() {
        return null;
    }

    @Override
    public HttpSession getSession(boolean create) {
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
    @SuppressWarnings("deprecation")
    @Deprecated
    public boolean isRequestedSessionIdFromUrl() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        return false;
    }

    @Override
    public boolean isUserInRole(String role) {
        return false;
    }

    @Override
    public Object getAttribute(String name) {
        return null;
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return null;
    }

    @Override
    public String getCharacterEncoding() {
        return null;
    }

    @Override
    public int getContentLength() {
        return 0;
    }

    @Override
    public String getContentType() {
        return null;
    }

    @Override
    public ServletInputStream getInputStream() {
        return null;
    }

    @Override
    public String getLocalAddr() {
        return null;
    }

    @Override
    public String getLocalName() {
        return null;
    }

    @Override
    public int getLocalPort() {
        return 0;
    }

    @Override
    public Locale getLocale() {
        return null;
    }

    @Override
    public Enumeration<Locale> getLocales() {
        return null;
    }

    @Override
    public String getParameter(String name) {
        return null;
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return null;
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return null;
    }

    @Override
    public String[] getParameterValues(String name) {
        return null;
    }

    @Override
    public String getProtocol() {
        return null;
    }

    @Override
    public BufferedReader getReader() {
        return null;
    }

    @Override
    @SuppressWarnings("deprecation")
    @Deprecated
    public String getRealPath(String path) {
        return null;
    }

    @Override
    public String getRemoteAddr() {
        return null;
    }

    @Override
    public String getRemoteHost() {
        return null;
    }

    @Override
    public int getRemotePort() {
        return 0;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        return null;
    }

    @Override
    public String getScheme() {
        return scheme;
    }

    @Override
    public String getServerName() {
        return server;
    }

    @Override
    public int getServerPort() {
        return port;
    }

    @Override
    public boolean isSecure() {
        return this.secure;
    }

    @Override
    public void removeAttribute(String name) {

    }

    @Override
    public void setAttribute(String name, Object o) {

    }

    @Override
    public void setCharacterEncoding(String env) {

    }

    @Override
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        return null;
    }

    @Override
    public String changeSessionId() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void login(String username, String password) throws ServletException {
        // TODO Auto-generated method stub

    }

    @Override
    public void logout() throws ServletException {
        // TODO Auto-generated method stub

    }

    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Part getPart(String name) throws IOException, ServletException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getContentLengthLong() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public ServletContext getServletContext() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse)
            throws IllegalStateException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isAsyncStarted() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isAsyncSupported() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public AsyncContext getAsyncContext() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DispatcherType getDispatcherType() {
        // TODO Auto-generated method stub
        return null;
    }
}
