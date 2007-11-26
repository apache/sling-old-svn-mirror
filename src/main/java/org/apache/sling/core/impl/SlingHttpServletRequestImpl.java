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
package org.apache.sling.core.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
import org.apache.sling.api.services.ServiceLocator;
import org.apache.sling.core.impl.parameters.ParameterSupport;
import org.apache.sling.core.impl.request.RequestData;
import org.apache.sling.core.impl.request.SlingRequestDispatcher;

/**
 * The <code>SlingHttpServletRequestImpl</code> TODO
 */
public class SlingHttpServletRequestImpl extends HttpServletRequestWrapper implements
        SlingHttpServletRequest {

    private final RequestData requestData;

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
        return getRequestData().getResourceManager();
    }

    public ServiceLocator getServiceLocator() {
        return getRequestData().getServiceLocator();
    }

    public RequestProgressTracker getRequestProgressTracker() {
        return getRequestData().getRequestProgressTracker();
    }

    /**
     * @see org.apache.sling.core.component.ComponentRequest#getRequestDispatcher(org.apache.sling.core.component.Content)
     */
    public RequestDispatcher getRequestDispatcher(Resource resource) {
        return getRequestDispatcher(resource, null);
    }

    public RequestDispatcher getRequestDispatcher(Resource resource,
            RequestDispatcherOptions options) {
        return new SlingRequestDispatcher(resource, options);
    }

    /**
     * @see javax.servlet.ServletRequestWrapper#getRequestDispatcher(java.lang.String)
     */
    public RequestDispatcher getRequestDispatcher(String path) {
        return new SlingRequestDispatcher(path);
    }

    /**
     * @see javax.servlet.ServletRequestWrapper#getLocale()
     */
    public Locale getLocale() {
        return (this.getRequestData() != null)
                ? this.getRequestData().getLocale()
                : super.getLocale();
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
     * @see org.apache.sling.core.component.ComponentRequest#getRequestParameter(java.lang.String)
     */
    public RequestParameter getRequestParameter(String name) {
        return this.getParameterSupport().getRequestParameter(name);
    }

    /**
     * @see org.apache.sling.core.component.ComponentRequest#getRequestParameters(java.lang.String)
     */
    public RequestParameter[] getRequestParameters(String name) {
        return this.getParameterSupport().getRequestParameters(name);
    }

    /**
     * @see org.apache.sling.core.component.ComponentRequest#getRequestParameterMap()
     */
    public RequestParameterMap getRequestParameterMap() {
        return this.getParameterSupport().getRequestParameterMap();
    }

    /**
     * @see org.apache.sling.core.component.ComponentRequest#getCookie(java.lang.String)
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

    public RequestPathInfo getRequestPathInfo() {
        return getRequestData().getContentData().getRequestPathInfo();
    }

    /**
     * @see org.apache.sling.core.component.ComponentRequest#getResourceBundle(java.util.Locale)
     */
    public ResourceBundle getResourceBundle(Locale locale) {
        // TODO should use our resource bundle !!
        return null;
    }

    /**
     * @see org.apache.sling.core.component.ComponentRequest#getResponseContentType()
     */
    public String getResponseContentType() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see org.apache.sling.core.component.ComponentRequest#getResponseContentTypes()
     */
    @SuppressWarnings("unchecked")
    public Enumeration<String> getResponseContentTypes() {
        List<String> empty = Collections.emptyList();
        return Collections.enumeration(empty);
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
