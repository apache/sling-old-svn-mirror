/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.engine.impl.helper;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.ServletResponse;
import javax.servlet.ServletResponseWrapper;

import org.apache.sling.engine.impl.SlingHttpServletRequestImpl;
import org.apache.sling.engine.impl.SlingHttpServletResponseImpl;

/**
 * Wrapper around a ServletContext for an external servlet context, i.e. one
 * returned by servletContext.getContext(String)
 */
class ExternalServletContextWrapper implements ServletContext {

    private final ServletContext delegate;

    public ExternalServletContextWrapper(ServletContext sc) {
        this.delegate = sc;
    }

    protected final ServletContext getServletContext() {
        return delegate;
    }

    public ServletContext getContext(String s) {
        return getServletContext().getContext(s);
    }

    public int getMajorVersion() {
        return getServletContext().getMajorVersion();
    }

    public int getMinorVersion() {
        return getServletContext().getMinorVersion();
    }

    public String getMimeType(String s) {
        return getServletContext().getMimeType(s);
    }

    public Set getResourcePaths(String s) {
        return getServletContext().getResourcePaths(s);
    }

    public URL getResource(String s) throws MalformedURLException {
        return getServletContext().getResource(s);
    }

    public InputStream getResourceAsStream(String s) {
        return getServletContext().getResourceAsStream(s);
    }

    public RequestDispatcher getRequestDispatcher(String s) {
        return new RequestDispatcherWrapper(getServletContext().getRequestDispatcher(s));
    }

    public RequestDispatcher getNamedDispatcher(String s) {
        return new RequestDispatcherWrapper(getServletContext().getNamedDispatcher(s));
    }

    public Servlet getServlet(String s) throws ServletException {
        return getServletContext().getServlet(s);
    }

    public Enumeration getServlets() {
        return getServletContext().getServlets();
    }

    public Enumeration getServletNames() {
        return getServletContext().getServletNames();
    }

    public void log(String s) {
        getServletContext().log(s);
    }

    public void log(Exception exception, String s) {
        getServletContext().log(exception, s);
    }

    public void log(String s, Throwable throwable) {
        getServletContext().log(s, throwable);
    }

    public String getRealPath(String s) {
        return getServletContext().getRealPath(s);
    }

    public String getServerInfo() {
        return getServletContext().getServerInfo();
    }

    public String getInitParameter(String s) {
        return getServletContext().getInitParameter(s);
    }

    public Enumeration getInitParameterNames() {
        return getServletContext().getInitParameterNames();
    }

    public Object getAttribute(String s) {
        return getServletContext().getAttribute(s);
    }

    public Enumeration getAttributeNames() {
        return getServletContext().getAttributeNames();
    }

    public void setAttribute(String s, Object obj) {
        getServletContext().setAttribute(s, obj);
    }

    public void removeAttribute(String s) {
        getServletContext().removeAttribute(s);
    }

    public String getServletContextName() {
        return getServletContext().getServletContextName();
    }

    public String getContextPath() {
        return getServletContext().getContextPath();
    }

    static class RequestDispatcherWrapper implements RequestDispatcher {

        private final RequestDispatcher delegate;

        public RequestDispatcherWrapper(final RequestDispatcher rd) {
            this.delegate = rd;
        }

        public void forward(final ServletRequest request, final ServletResponse response) throws ServletException,
                IOException {
            delegate.forward(unwrapServletRequest(request), unwrapServletResponse(response));
        }

        public void include(final ServletRequest request, final ServletResponse response) throws ServletException,
                IOException {
            delegate.include(unwrapServletRequest(request), unwrapServletResponse(response));
        }

        RequestDispatcher getDelegate() {
            return delegate;
        }

        static ServletRequest unwrapServletRequest(ServletRequest request) {
            ServletRequest lastRequest = request;
            while (request != null) {
                if (request instanceof SlingHttpServletRequestImpl) {
                    return ((SlingHttpServletRequestImpl) request).getRequest();
                } else if (request instanceof ServletRequestWrapper) {
                    lastRequest = request;
                    request = ((ServletRequestWrapper) request).getRequest();
                } else {
                    return request;
                }
            }
            return lastRequest;
        }

        static ServletResponse unwrapServletResponse(ServletResponse response) {
            ServletResponse lastResponse = response;
            while (response != null) {
                if (response instanceof SlingHttpServletResponseImpl) {
                    return ((SlingHttpServletResponseImpl) response).getResponse();
                } else if (response instanceof ServletResponseWrapper) {
                    lastResponse = response;
                    response = ((ServletResponseWrapper) response).getResponse();
                } else {
                    return response;
                }
            }
            return lastResponse;
        }

    }
}
