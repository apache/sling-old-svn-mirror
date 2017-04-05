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
package org.apache.sling.api.wrappers;

import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.request.RequestProgressTracker;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * The <code>SlingHttpServletRequestWrapper</code> class is a default wrapper
 * class around a {@link SlingHttpServletRequest} which may be extended to amend
 * the functionality of the original request object.
 */
public class SlingHttpServletRequestWrapper extends HttpServletRequestWrapper
        implements SlingHttpServletRequest {

    /**
     * Create a wrapper for the supplied wrappedRequest
     * @param wrappedRequest The request.
     */
    public SlingHttpServletRequestWrapper(SlingHttpServletRequest wrappedRequest) {
        super(wrappedRequest);
    }

    /**
     * Return the original {@link SlingHttpServletRequest} object wrapped by
     * this.
     * @return The wrapped request.
     */
    public SlingHttpServletRequest getSlingRequest() {
        return (SlingHttpServletRequest) getRequest();
    }

    @Override
    public Cookie getCookie(String name) {
        return getSlingRequest().getCookie(name);
    }

    @Override
    public RequestProgressTracker getRequestProgressTracker() {
        return getSlingRequest().getRequestProgressTracker();
    }

    @Override
    public RequestDispatcher getRequestDispatcher(Resource resource) {
        return getSlingRequest().getRequestDispatcher(resource);
    }

    @Override
    public RequestDispatcher getRequestDispatcher(Resource resource,
            RequestDispatcherOptions options) {
        return getSlingRequest().getRequestDispatcher(resource, options);
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path,
            RequestDispatcherOptions options) {
        return getSlingRequest().getRequestDispatcher(path, options);
    }

    @Override
    public RequestParameter getRequestParameter(String name) {
        return getSlingRequest().getRequestParameter(name);
    }

    @Override
    public RequestParameterMap getRequestParameterMap() {
        return getSlingRequest().getRequestParameterMap();
    }

    @Override
    public List<RequestParameter> getRequestParameterList() {
        return getSlingRequest().getRequestParameterList();
    }

    @Override
    public RequestParameter[] getRequestParameters(String name) {
        return getSlingRequest().getRequestParameters(name);
    }

    @Override
    public RequestPathInfo getRequestPathInfo() {
        return getSlingRequest().getRequestPathInfo();
    }

    @Override
    public Resource getResource() {
        return getSlingRequest().getResource();
    }

    @Override
    public ResourceResolver getResourceResolver() {
        return getSlingRequest().getResourceResolver();
    }

    @Override
    public ResourceBundle getResourceBundle(Locale locale) {
        return getSlingRequest().getResourceBundle(locale);
    }

    @Override
    public ResourceBundle getResourceBundle(String baseName, Locale locale) {
        return getSlingRequest().getResourceBundle(baseName, locale);
    }

    @Override
    public String getResponseContentType() {
        return getSlingRequest().getResponseContentType();
    }

    @Override
    public Enumeration<String> getResponseContentTypes() {
        return getSlingRequest().getResponseContentTypes();
    }

    @Override
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        return getSlingRequest().adaptTo(type);
    }
}
