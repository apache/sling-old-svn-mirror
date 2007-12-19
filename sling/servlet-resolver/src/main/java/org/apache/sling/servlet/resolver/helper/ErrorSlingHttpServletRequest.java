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
package org.apache.sling.servlet.resolver.helper;

import java.util.Enumeration;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.servlet.RequestDispatcher;
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

public class ErrorSlingHttpServletRequest extends
        HttpServletRequestWrapper implements SlingHttpServletRequest {

    private SlingHttpServletRequest delegatee;
    private String methodName;

    public ErrorSlingHttpServletRequest(HttpServletRequest delegatee,
            String errorMethod) {
        super(delegatee);

        this.delegatee = (delegatee instanceof SlingHttpServletRequest)
                ? (SlingHttpServletRequest) delegatee
                : null;
        this.methodName = errorMethod;
    }

    public void setMethod(String methodName) {
        this.methodName = methodName.toLowerCase();
    }

    @Override
    public String getMethod() {
        return methodName;
    }


    // ---------- SlingHttpServletRequest ----------------------------------

    public Cookie getCookie(String name) {
        return (delegatee == null) ? null : delegatee.getCookie(name);
    }

    public RequestDispatcher getRequestDispatcher(Resource resource,
            RequestDispatcherOptions options) {
        return (delegatee == null) ? null : delegatee.getRequestDispatcher(resource, options);
    }

    public RequestDispatcher getRequestDispatcher(Resource resource) {
        return (delegatee == null) ? null : delegatee.getRequestDispatcher(resource);
    }

    public RequestParameter getRequestParameter(String name) {
        return (delegatee == null) ? null : delegatee.getRequestParameter(name);
    }

    public RequestParameterMap getRequestParameterMap() {
        return (delegatee == null) ? null : delegatee.getRequestParameterMap();
    }

    public RequestParameter[] getRequestParameters(String name) {
        return (delegatee == null) ? null : delegatee.getRequestParameters(name);
    }

    public RequestPathInfo getRequestPathInfo() {
        return (delegatee == null) ? null : delegatee.getRequestPathInfo();
    }

    public RequestProgressTracker getRequestProgressTracker() {
        return (delegatee == null) ? null : delegatee.getRequestProgressTracker();
    }

    public Resource getResource() {
        return (delegatee == null) ? null : delegatee.getResource();
    }

    public ResourceBundle getResourceBundle(Locale locale) {
        return (delegatee == null) ? null : delegatee.getResourceBundle(locale);
    }

    public ResourceResolver getResourceResolver() {
        return (delegatee == null) ? null : delegatee.getResourceResolver();
    }

    public String getResponseContentType() {
        return (delegatee == null) ? null : delegatee.getResponseContentType();
    }

    public Enumeration<String> getResponseContentTypes() {
        return (delegatee == null) ? null : delegatee.getResponseContentTypes();
    }

    public ServiceLocator getServiceLocator() {
        return (delegatee == null) ? null : delegatee.getServiceLocator();
    }

}