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
package org.apache.sling.microsling;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.jcr.Session;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.sling.api.SlingException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.request.RequestProgressTracker;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.services.ServiceLocator;
import org.apache.sling.api.wrappers.SlingRequestPaths;
import org.apache.sling.microsling.request.helpers.MicroslingRequestPathInfo;
import org.apache.sling.microsling.request.helpers.MicroslingRequestProgressTracker;
import org.apache.sling.microsling.request.helpers.SlingRequestParameterMap;
import org.apache.sling.microsling.resource.MicroslingResourceResolver;

/**
 * The <code>MicroslingSlingHttpServletRequest</code> TODO
 */
public class MicroslingSlingHttpServletRequest extends
        HttpServletRequestWrapper implements SlingHttpServletRequest {

    private RequestProgressTracker requestProgressTracker;

    private ServiceLocator serviceLocator;

    private ResourceResolver resourceResolver;

    private Resource resource;

    private RequestPathInfo requestPathInfo;

    private RequestParameterMap requestParameterMap;

    /**
     * @param request
     */
    public MicroslingSlingHttpServletRequest(HttpServletRequest request,
            Session session, ServiceLocator serviceLocator)
            throws SlingException {
        super(request);

        this.requestProgressTracker = new MicroslingRequestProgressTracker();
        this.serviceLocator = serviceLocator;
        this.resourceResolver = new MicroslingResourceResolver(session);
        this.resource = resourceResolver.resolve(request);
        this.requestPathInfo = 
            new MicroslingRequestPathInfo(resource, SlingRequestPaths.getPathInfo(request));
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.sling.api.SlingHttpServletRequest#getCookie(java.lang.String)
     */
    public Cookie getCookie(String name) {
        Cookie[] cookies = getCookies();
        for (int i = 0; cookies != null && i < cookies.length; i++) {
            if (name.equals(cookies[i].getName())) {
                return cookies[i];
            }
        }
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.sling.api.SlingHttpServletRequest#getRequestDispatcher(org.apache.sling.api.resource.Resource,
     *      org.apache.sling.api.request.RequestDispatcherOptions)
     */
    public RequestDispatcher getRequestDispatcher(Resource resource,
            RequestDispatcherOptions options) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.sling.api.SlingHttpServletRequest#getRequestDispatcher(org.apache.sling.api.resource.Resource)
     */
    public RequestDispatcher getRequestDispatcher(Resource resource) {
        return getRequestDispatcher(resource, null);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.sling.api.SlingHttpServletRequest#getRequestParameter(java.lang.String)
     */
    public RequestParameter getRequestParameter(String name) {
        return getRequestParameterMap().getValue(name);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.sling.api.SlingHttpServletRequest#getRequestParameterMap()
     */
    public RequestParameterMap getRequestParameterMap() {
        if (requestParameterMap == null) {
            requestParameterMap = new SlingRequestParameterMap(this);
        }
        return requestParameterMap;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.sling.api.SlingHttpServletRequest#getRequestParameters(java.lang.String)
     */
    public RequestParameter[] getRequestParameters(String name) {
        return getRequestParameterMap().getValues(name);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.sling.api.SlingHttpServletRequest#getRequestPathInfo()
     */
    public RequestPathInfo getRequestPathInfo() {
        return requestPathInfo;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.sling.api.SlingHttpServletRequest#getRequestProgressTracker()
     */
    public RequestProgressTracker getRequestProgressTracker() {
        return requestProgressTracker;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.sling.api.SlingHttpServletRequest#getResource()
     */
    public Resource getResource() {
        return resource;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.sling.api.SlingHttpServletRequest#getResourceBundle(java.util.Locale)
     */
    public ResourceBundle getResourceBundle(Locale locale) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.sling.api.SlingHttpServletRequest#getResourceResolver()
     */
    public ResourceResolver getResourceResolver() {
        return resourceResolver;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.sling.api.SlingHttpServletRequest#getResponseContentType()
     */
    public String getResponseContentType() {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.sling.api.SlingHttpServletRequest#getResponseContentTypes()
     */
    public Enumeration<String> getResponseContentTypes() {
        List<String> s = Collections.emptyList();
        return Collections.enumeration(s);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.sling.api.SlingHttpServletRequest#getServiceLocator()
     */
    public ServiceLocator getServiceLocator() {
        return serviceLocator;
    }
}
