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
package org.apache.sling.testing.mock.sling.servlet;

import java.io.BufferedReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListResourceBundle;
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
import javax.servlet.http.Part;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.adapter.SlingAdaptable;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.request.RequestProgressTracker;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.i18n.ResourceBundleProvider;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.apache.sling.testing.mock.sling.MockSling;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * Mock {@link SlingHttpServletRequest} implementation.
 */
public class MockSlingHttpServletRequest extends SlingAdaptable implements SlingHttpServletRequest {

    private final ResourceResolver resourceResolver;
    private final BundleContext bundleContext;
    private RequestPathInfo requestPathInfo = new MockRequestPathInfo();
    private Map<String, Object> attributeMap = new HashMap<String, Object>();
    private Map<String, String[]> parameterMap = new LinkedHashMap<String, String[]>();
    private HttpSession session;
    private Resource resource;
    private String contextPath;
    private String queryString;
    private String scheme = "http";
    private String serverName = "localhost";
    private int serverPort = 80;
    private String method = HttpConstants.METHOD_GET;
    private final HeaderSupport headerSupport = new HeaderSupport();
    private final CookieSupport cookieSupport = new CookieSupport();
    
    private static final ResourceBundle EMPTY_RESOURCE_BUNDLE = new ListResourceBundle() {
        @Override
        protected Object[][] getContents() {
            return new Object[0][0];
        }
    };

    /**
     * Instantiate with default resource resolver
     */
    public MockSlingHttpServletRequest() {
        this(MockSling.newResourceResolver());
    }

    /**
     * @param resourceResolver Resource resolver
     */
    public MockSlingHttpServletRequest(ResourceResolver resourceResolver) {
        this(resourceResolver, MockOsgi.newBundleContext());
    }

    /**
     * @param resourceResolver Resource resolver
     * @param bundleContext Bundle context
     */
    public MockSlingHttpServletRequest(ResourceResolver resourceResolver, BundleContext bundleContext) {
        this.resourceResolver = resourceResolver;
        this.bundleContext = bundleContext;
    }

    @Override
    public ResourceResolver getResourceResolver() {
        return this.resourceResolver;
    }

    @Override
    public HttpSession getSession() {
        return getSession(true);
    }

    @Override
    public HttpSession getSession(boolean create) {
        if (this.session == null && create) {
            this.session = new MockHttpSession();
        }
        return this.session;
    }

    @Override
    public RequestPathInfo getRequestPathInfo() {
        return this.requestPathInfo;
    }

    @Override
    public Object getAttribute(String name) {
        return this.attributeMap.get(name);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Enumeration<String> getAttributeNames() {
        return IteratorUtils.asEnumeration(this.attributeMap.keySet().iterator());
    }

    @Override
    public void removeAttribute(String name) {
        this.attributeMap.remove(name);
    }

    @Override
    public void setAttribute(String name, Object object) {
        this.attributeMap.put(name, object);
    }

    @Override
    public Resource getResource() {
        return this.resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    @Override
    public String getParameter(String name) {
        Object object = this.parameterMap.get(name);
        if (object instanceof String) {
            return (String) object;
        } else if (object instanceof String[]) {
            String[] values = (String[]) object;
            if (values.length > 0) {
                return values[0];
            }
        }
        return null;
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return this.parameterMap;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Enumeration<String> getParameterNames() {
        return IteratorUtils.asEnumeration(this.parameterMap.keySet().iterator());
    }

    @Override
    public String[] getParameterValues(String name) { // NOPMD
        Object object = this.parameterMap.get(name);
        if (object instanceof String) {
            return new String[] { (String) object };
        } else if (object instanceof String[]) {
            return (String[]) object;
        }
        return null; // NOPMD
    }

    /**
     * @param parameterMap Map of parameters
     */
    public void setParameterMap(Map<String, Object> parameterMap) {
        this.parameterMap.clear();
        for (Map.Entry<String, Object> entry : parameterMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof String[]) {
                this.parameterMap.put(key, (String[]) value);
            } else if (value != null) {
                this.parameterMap.put(key, new String[] { value.toString() });
            } else {
                this.parameterMap.put(key, null);
            }
        }
        try {
            this.queryString = formatQueryString(this.parameterMap);
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }

    private String formatQueryString(Map<String, String[]> map) throws UnsupportedEncodingException {
        StringBuilder querystring = new StringBuilder();
        for (Map.Entry<String, String[]> entry : this.parameterMap.entrySet()) {
            if (entry.getValue() != null) {
                for (String value : entry.getValue()) {
                    if (querystring.length() != 0) {
                        querystring.append('&');
                    }
                    querystring.append(URLEncoder.encode(entry.getKey(), CharEncoding.UTF_8));
                    querystring.append('=');
                    if (value != null) {
                        querystring.append(URLEncoder.encode(value, CharEncoding.UTF_8));
                    }
                }
            }
        }
        if (querystring.length() > 0) {
            return querystring.toString();
        } else {
            return null;
        }
    }

    @Override
    public Locale getLocale() {
        return Locale.US;
    }

    @Override
    public String getContextPath() {
        return this.contextPath;
    }

    /**
     * @param contextPath Webapp context path
     */
    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    /**
     * @param queryString Query string (with proper URL encoding)
     */
    public void setQueryString(String queryString) {
        this.queryString = queryString;
        try {
            parseQueryString(this.parameterMap, this.queryString);
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void parseQueryString(Map<String, String[]> map, String query) throws UnsupportedEncodingException {
        Map<String, List<String>> queryPairs = new LinkedHashMap<String, List<String>>();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf('=');
            String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), CharEncoding.UTF_8) : pair;
            if (!queryPairs.containsKey(key)) {
                queryPairs.put(key, new ArrayList<String>());
            }
            String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1),
                    CharEncoding.UTF_8) : null;
            queryPairs.get(key).add(value);
        }
        map.clear();
        for (Map.Entry<String, List<String>> entry : queryPairs.entrySet()) {
            map.put(entry.getKey(), entry.getValue().toArray(new String[entry.getValue().size()]));
        }
    }

    @Override
    public String getQueryString() {
        return this.queryString;
    }

    @Override
    public String getScheme() {
        return this.scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    @Override
    public String getServerName() {
        return this.serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    @Override
    public int getServerPort() {
        return this.serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    @Override
    public boolean isSecure() {
        return StringUtils.equals("https", getScheme());
    }

    @Override
    public String getMethod() {
        return this.method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    @Override
    public long getDateHeader(String name) {
        return headerSupport.getDateHeader(name);
    }

    @Override
    public String getHeader(String name) {
        return headerSupport.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return HeaderSupport.toEnumeration(headerSupport.getHeaderNames());
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        return HeaderSupport.toEnumeration(headerSupport.getHeaders(name));
    }

    @Override
    public int getIntHeader(String name) {
        return headerSupport.getIntHeader(name);
    }

    /**
     * Add header, keep existing ones with same name.
     * @param name Header name
     * @param value Header value
     */
    public void addHeader(String name, String value) {
        headerSupport.addHeader(name, value);
    }

    /**
     * Add header, keep existing ones with same name.
     * @param name Header name
     * @param value Header value
     */
    public void addIntHeader(String name, int value) {
        headerSupport.addIntHeader(name, value);
    }

    /**
     * Add header, keep existing ones with same name.
     * @param name Header name
     * @param date Header value
     */
    public void addDateHeader(String name, long date) {
        headerSupport.addDateHeader(name, date);
    }

    /**
     * Set header, overwrite existing ones with same name.
     * @param name Header name
     * @param value Header value
     */
    public void setHeader(String name, String value) {
        headerSupport.setHeader(name, value);
    }

    /**
     * Set header, overwrite existing ones with same name.
     * @param name Header name
     * @param value Header value
     */
    public void setIntHeader(String name, int value) {
        headerSupport.setIntHeader(name, value);
    }

    /**
     * Set header, overwrite existing ones with same name.
     * @param name Header name
     * @param date Header value
     */
    public void setDateHeader(String name, long date) {
        headerSupport.setDateHeader(name, date);
    }

    @Override
    public Cookie getCookie(String name) {
        return cookieSupport.getCookie(name);
    }

    @Override
    public Cookie[] getCookies() {
        return cookieSupport.getCookies();
    }

    /**
     * Set cookie
     * @param cookie Cookie
     */
    public void addCookie(Cookie cookie) {
        cookieSupport.addCookie(cookie);
    }

    @Override
    public ResourceBundle getResourceBundle(Locale locale) {
        return getResourceBundle(null, locale);
    }

    @Override
    public ResourceBundle getResourceBundle(String baseName, Locale locale) {
        // check of ResourceBundleProvider is registered in mock OSGI context
        ResourceBundle resourceBundle = null;
        ServiceReference serviceReference = bundleContext.getServiceReference(ResourceBundleProvider.class.getName());
        if (serviceReference != null) {
            ResourceBundleProvider provider = (ResourceBundleProvider)bundleContext.getService(serviceReference);
            resourceBundle = provider.getResourceBundle(baseName, locale);
        }       
        // if no ResourceBundleProvider exists return empty bundle
        if (resourceBundle == null) {
            resourceBundle = EMPTY_RESOURCE_BUNDLE;
        }
        return resourceBundle;
    }
    
    // --- unsupported operations ---

    @Override
    public RequestDispatcher getRequestDispatcher(Resource dispatcherResource) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String dispatcherPath, RequestDispatcherOptions options) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RequestDispatcher getRequestDispatcher(Resource dispatcherResource, RequestDispatcherOptions options) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RequestParameter getRequestParameter(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RequestParameterMap getRequestParameterMap() {
        throw new UnsupportedOperationException();
    }

    @Override
    public RequestParameter[] getRequestParameters(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RequestProgressTracker getRequestProgressTracker() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getResponseContentType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Enumeration<String> getResponseContentTypes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getAuthType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getPathInfo() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getPathTranslated() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getRemoteUser() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getRequestURI() {
        throw new UnsupportedOperationException();
    }

    @Override
    public StringBuffer getRequestURL() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getRequestedSessionId() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getServletPath() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Principal getUserPrincipal() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isRequestedSessionIdFromUrl() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isUserInRole(String role) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getCharacterEncoding() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getContentLength() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getContentType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServletInputStream getInputStream() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getLocalAddr() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getLocalName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getLocalPort() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Enumeration<Locale> getLocales() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getProtocol() {
        throw new UnsupportedOperationException();
    }

    @Override
    public BufferedReader getReader() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getRealPath(String path) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getRemoteAddr() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getRemoteHost() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getRemotePort() {
        throw new UnsupportedOperationException();
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setCharacterEncoding(String env) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean authenticate(HttpServletResponse response) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void login(String pUsername, String password) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void logout() throws ServletException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<Part> getParts() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Part getPart(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServletContext getServletContext() {
        throw new UnsupportedOperationException();
    }

    @Override
    public AsyncContext startAsync() {
        throw new UnsupportedOperationException();
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAsyncStarted() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAsyncSupported() {
        throw new UnsupportedOperationException();
    }

    @Override
    public AsyncContext getAsyncContext() {
        throw new UnsupportedOperationException();
    }

    @Override
    public DispatcherType getDispatcherType() {
        throw new UnsupportedOperationException();
    }

    // part of Sling API 2.7
    public List<RequestParameter> getRequestParameterList() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        AdapterType adapted = super.adaptTo(type);
        if (adapted == null && Resource.class.equals(type) && getResource() != null) {
            adapted = (AdapterType) getResource();
        }
        return adapted;
    }
}
