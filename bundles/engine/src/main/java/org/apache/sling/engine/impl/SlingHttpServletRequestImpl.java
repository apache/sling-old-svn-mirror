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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.Part;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.request.RequestProgressTracker;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.engine.impl.helper.NullResourceBundle;
import org.apache.sling.engine.impl.parameters.ParameterSupport;
import org.apache.sling.engine.impl.request.ContentData;
import org.apache.sling.engine.impl.request.RequestData;
import org.apache.sling.engine.impl.request.SlingRequestDispatcher;
import org.osgi.service.http.HttpContext;
import org.osgi.service.useradmin.Authorization;

public class SlingHttpServletRequestImpl extends HttpServletRequestWrapper implements
        SlingHttpServletRequest {

    private final RequestData requestData;
    private final String pathInfo;
    private String responseContentType;

    public SlingHttpServletRequestImpl(RequestData requestData,
            HttpServletRequest servletRequest) {
        super(servletRequest);
        this.requestData = requestData;

        // prepare the pathInfo property
        String pathInfo = servletRequest.getServletPath();
        if (servletRequest.getPathInfo() != null) {
            pathInfo = pathInfo.concat(servletRequest.getPathInfo());
        }
        this.pathInfo = pathInfo;
    }

    /**
     * @return the requestData
     */
    public final RequestData getRequestData() {
        return this.requestData;
    }

    //---------- Adaptable interface

    @Override
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        return getRequestData().adaptTo(this, type);
    }

    //---------- SlingHttpServletRequest interface

    ParameterSupport getParameterSupport() {
        return this.getRequestData().getParameterSupport();
    }

    @Override
    public Resource getResource() {
        final ContentData cd = getRequestData().getContentData();
        return (cd == null) ? null : cd.getResource();
    }

    @Override
    public ResourceResolver getResourceResolver() {
        return getRequestData().getResourceResolver();
    }

    @Override
    public RequestProgressTracker getRequestProgressTracker() {
        return getRequestData().getRequestProgressTracker();
    }

    /**
     * Returns <code>null</code> if <code>resource</code> is <code>null</code>.
     */
    @Override
    public RequestDispatcher getRequestDispatcher(Resource resource) {
        return getRequestDispatcher(resource, null);
    }

    /**
     * Returns <code>null</code> if <code>resource</code> is <code>null</code>.
     */
    @Override
    public RequestDispatcher getRequestDispatcher(Resource resource,
            RequestDispatcherOptions options) {
        return (resource != null) ? new SlingRequestDispatcher(resource, options) : null;
    }

    /**
     * Returns <code>null</code> if <code>path</code> is <code>null</code>.
     */
    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        return getRequestDispatcher(path, null);
    }

    /**
     * Returns <code>null</code> if <code>path</code> is <code>null</code>.
     */
    @Override
    public RequestDispatcher getRequestDispatcher(String path,
            RequestDispatcherOptions options) {
        return (path != null) ? new SlingRequestDispatcher(path, options) : null;
    }

    /**
     * @see javax.servlet.ServletRequestWrapper#getParameter(java.lang.String)
     */
    @Override
    public String getParameter(String name) {
        return this.getParameterSupport().getParameter(name);
    }

    /**
     * @see javax.servlet.ServletRequestWrapper#getParameterMap()
     */
    @Override
    public Map<String, String[]> getParameterMap() {
        return this.getParameterSupport().getParameterMap();
    }

    /**
     * @see javax.servlet.ServletRequestWrapper#getParameterNames()
     */
    @Override
    public Enumeration<String> getParameterNames() {
        return this.getParameterSupport().getParameterNames();
    }

    /**
     * @see javax.servlet.ServletRequestWrapper#getParameterValues(java.lang.String)
     */
    @Override
    public String[] getParameterValues(String name) {
        return this.getParameterSupport().getParameterValues(name);
    }

    /**
     * @see org.apache.sling.api.SlingHttpServletRequest#getRequestParameter(java.lang.String)
     */
    @Override
    public RequestParameter getRequestParameter(String name) {
        return this.getParameterSupport().getRequestParameter(name);
    }

    /**
     * @see org.apache.sling.api.SlingHttpServletRequest#getRequestParameters(java.lang.String)
     */
    @Override
    public RequestParameter[] getRequestParameters(String name) {
        return this.getParameterSupport().getRequestParameters(name);
    }

    /**
     * @see org.apache.sling.api.SlingHttpServletRequest#getRequestParameterMap()
     */
    @Override
    public RequestParameterMap getRequestParameterMap() {
        return this.getParameterSupport().getRequestParameterMap();
    }

    /**
     * @see org.apache.sling.api.SlingHttpServletRequest#getRequestParameterList()
     */
    @Override
    public List<RequestParameter> getRequestParameterList() {
        return this.getParameterSupport().getRequestParameterList();
    }

    /**
     * @see org.apache.sling.api.SlingHttpServletRequest#getCookie(java.lang.String)
     */
    @Override
    public Cookie getCookie(String name) {
        Cookie[] cookies = getCookies();

        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                if (cookies[i].getName().equals(name)) {
                    return cookies[i];
                }
            }
        }

        return null;
    }

    /**
     * @see org.apache.sling.api.SlingHttpServletRequest#getRequestPathInfo()
     */
    @Override
    public RequestPathInfo getRequestPathInfo() {
        final ContentData cd = getRequestData().getContentData();
        return (cd == null) ? null : cd.getRequestPathInfo();
    }

    /**
     * @see org.apache.sling.api.SlingHttpServletRequest#getResourceBundle(java.util.Locale)
     */
    @Override
    public ResourceBundle getResourceBundle(Locale locale) {
        return getResourceBundle(null, locale);
    }

    /**
     * @see org.apache.sling.api.SlingHttpServletRequest#getResourceBundle(String, Locale)
     */
    @Override
    public ResourceBundle getResourceBundle(String baseName, Locale locale) {
        if (locale == null) {
            locale = getLocale();
        }

        return new NullResourceBundle(locale);
    }

    /**
     * @see org.apache.sling.api.SlingHttpServletRequest#getResponseContentType()
     */
    @Override
    public String getResponseContentType() {
        if(responseContentType == null) {
            final String ext = getRequestPathInfo().getExtension();
            responseContentType = requestData.getMimeType("dummy." + ext);
        }
        return responseContentType;
    }

    /**
     * @see org.apache.sling.api.SlingHttpServletRequest#getResponseContentTypes()
     */
    @Override
    public Enumeration<String> getResponseContentTypes() {
        List<String> result = new ArrayList<String>();

        // TODO for now this returns a single value
        final String singleType = getResponseContentType();
        if(singleType!=null) {
            result.add(singleType);
        }

        return Collections.enumeration(result);
    }

    /**
     * @see javax.servlet.ServletRequestWrapper#getInputStream()
     */
    @Override
    public ServletInputStream getInputStream() throws IOException {
        return this.getRequestData().getInputStream();
    }

    /**
     * @see javax.servlet.ServletRequestWrapper#getReader()
     */
    @Override
    public BufferedReader getReader() throws UnsupportedEncodingException,
            IOException {
        return this.getRequestData().getReader();
    }

    /**
     * @see javax.servlet.http.HttpServletRequestWrapper#getUserPrincipal()
     */
    @Override
    public Principal getUserPrincipal() {
        Principal principal = getResourceResolver().adaptTo(Principal.class);
        if (principal != null) {
            return principal;
        }
        //fallback to the userid
        String remoteUser = getRemoteUser();
        return (remoteUser != null) ? new UserPrincipal(remoteUser) : null;
    }

    /**
     * @see javax.servlet.http.HttpServletRequestWrapper#isUserInRole(String)
     */
    @Override
    public boolean isUserInRole(String role) {
        Object authorization = getAttribute(HttpContext.AUTHORIZATION);
        return (authorization instanceof Authorization)
                ? ((Authorization) authorization).hasRole(role)
                : false;
    }

    /**
     * Always returns the empty string since the actual servlet registered with
     * the servlet container (the HttpService actually) is registered as if
     * the servlet path is "/*".
     */
    @Override
    public String getServletPath() {
        return "";
    }

    /**
     * Returns the part of the request URL without the leading servlet context
     * path.
     */
    @Override
    public String getPathInfo() {
        return pathInfo;
    }

    public Part getPart(String name) {
        return (Part) this.getParameterSupport().getPart(name);
    }

    @SuppressWarnings("unchecked")
    public Collection<Part> getParts() {
        return (Collection<Part>) this.getParameterSupport().getParts();
    }

    /**
     * A <code>UserPrincipal</code> ...
     */
    private static class UserPrincipal implements Principal, Serializable {

        private final String name;

        /**
         * Creates a <code>UserPrincipal</code> with the given name.
         *
         * @param name the name of this principal
         * @throws IllegalArgumentException if <code>name</code> is <code>null</code>.
         */
        public UserPrincipal(String name) throws IllegalArgumentException {
            if (name == null) {
                throw new IllegalArgumentException("name can not be null");
            }
            this.name = name;
        }

        @Override
        public String toString() {
            return ("UserPrincipal: " + name);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof UserPrincipal) {
                UserPrincipal other = (UserPrincipal) obj;
                return name.equals(other.name);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        //------------------------------------------------------------< Principal >
        /**
         * {@inheritDoc}
         */
        @Override
        public String getName() {
            return name;
        }
    }

}
