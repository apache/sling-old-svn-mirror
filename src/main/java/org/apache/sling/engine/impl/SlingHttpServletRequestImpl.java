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
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
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
import org.apache.sling.engine.impl.request.RequestData;
import org.apache.sling.engine.impl.request.SlingRequestDispatcher;

/**
 * The <code>SlingHttpServletRequestImpl</code> TODO
 */
public class SlingHttpServletRequestImpl extends HttpServletRequestWrapper implements
        SlingHttpServletRequest {

    private final RequestData requestData;
    private String responseContentType;

    public SlingHttpServletRequestImpl(RequestData requestData,
            HttpServletRequest servletRequest) {
        super(servletRequest);
        this.requestData = requestData;
    }

    /**
     * @return the requestData
     */
    public final RequestData getRequestData() {
        return this.requestData;
    }

    ParameterSupport getParameterSupport() {
        return this.getRequestData().getParameterSupport();
    }

    public Resource getResource() {
        return getRequestData().getContentData().getResource();
    }

    public ResourceResolver getResourceResolver() {
        return getRequestData().getResourceResolver();
    }

    public RequestProgressTracker getRequestProgressTracker() {
        return getRequestData().getRequestProgressTracker();
    }

    /**
     * Returns <code>null</code> if <code>resource</code> is <code>null</code>.
     */
    public RequestDispatcher getRequestDispatcher(Resource resource) {
        return getRequestDispatcher(resource, null);
    }

    /**
     * Returns <code>null</code> if <code>resource</code> is <code>null</code>.
     */
    public RequestDispatcher getRequestDispatcher(Resource resource,
            RequestDispatcherOptions options) {
        return (resource != null) ? new SlingRequestDispatcher(resource, options) : null;
    }

    /**
     * Returns <code>null</code> if <code>path</code> is <code>null</code>.
     */
    public RequestDispatcher getRequestDispatcher(String path) {
        return getRequestDispatcher(path, null);
    }

    /**
     * Returns <code>null</code> if <code>path</code> is <code>null</code>.
     */
    public RequestDispatcher getRequestDispatcher(String path,
            RequestDispatcherOptions options) {
        return (path != null) ? new SlingRequestDispatcher(path, options) : null;
    }

    /**
     * @see javax.servlet.ServletRequestWrapper#getParameter(java.lang.String)
     */
    public String getParameter(String name) {
        return this.getParameterSupport().getParameter(name);
    }

    /**
     * @see javax.servlet.ServletRequestWrapper#getParameterMap()
     */
    public Map<String, String[]> getParameterMap() {
        return this.getParameterSupport().getParameterMap();
    }

    /**
     * @see javax.servlet.ServletRequestWrapper#getParameterNames()
     */
    public Enumeration<String> getParameterNames() {
        return this.getParameterSupport().getParameterNames();
    }

    /**
     * @see javax.servlet.ServletRequestWrapper#getParameterValues(java.lang.String)
     */
    public String[] getParameterValues(String name) {
        return this.getParameterSupport().getParameterValues(name);
    }

    /**
     * @see org.apache.sling.api.SlingHttpServletRequest#getRequestParameter(java.lang.String)
     */
    public RequestParameter getRequestParameter(String name) {
        return this.getParameterSupport().getRequestParameter(name);
    }

    /**
     * @see org.apache.sling.api.SlingHttpServletRequest#getRequestParameters(java.lang.String)
     */
    public RequestParameter[] getRequestParameters(String name) {
        return this.getParameterSupport().getRequestParameters(name);
    }

    /**
     * @see org.apache.sling.api.SlingHttpServletRequest#getRequestParameterMap()
     */
    public RequestParameterMap getRequestParameterMap() {
        return this.getParameterSupport().getRequestParameterMap();
    }

    /**
     * @see org.apache.sling.api.SlingHttpServletRequest#getCookie(java.lang.String)
     */
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
    public RequestPathInfo getRequestPathInfo() {
        return getRequestData().getContentData().getRequestPathInfo();
    }

    /**
     * @see org.apache.sling.api.SlingHttpServletRequest#getResourceBundle(java.util.Locale)
     */
    public ResourceBundle getResourceBundle(Locale locale) {
        return getResourceBundle(null, locale);
    }
    
    /**
     * @see org.apache.sling.api.SlingHttpServletRequest#getResourceBundle(String, Locale)
     */
    public ResourceBundle getResourceBundle(String baseName, Locale locale) {
        if (locale == null) {
            locale = getLocale();
        }
        
        return new NullResourceBundle(locale);
    }

    /**
     * @see org.apache.sling.api.SlingHttpServletRequest#getResponseContentType()
     */
    public String getResponseContentType() {
        if(responseContentType == null) {
            final String ext = getRequestPathInfo().getExtension();
            responseContentType = requestData.getSlingMainServlet().getServletContext().getMimeType("dummy." + ext);
        }
        return responseContentType;
    }

    /**
     * @see org.apache.sling.api.SlingHttpServletRequest#getResponseContentTypes()
     */
    @SuppressWarnings("unchecked")
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
    public ServletInputStream getInputStream() throws IOException {
        return this.getRequestData().getInputStream();
    }

    /**
     * @see javax.servlet.ServletRequestWrapper#getReader()
     */
    public BufferedReader getReader() throws UnsupportedEncodingException,
            IOException {
        return this.getRequestData().getReader();
    }
}
