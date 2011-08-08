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
package org.apache.sling.scripting.jsp.jasper.runtime;

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
import javax.servlet.ServletResponse;
import javax.servlet.jsp.JspFactory;
import javax.servlet.jsp.PageContext;

import org.apache.sling.scripting.jsp.jasper.compiler.JspRuntimeContext.JspFactoryHandler;

/**
 * Wrapper around a ServletContext for an external servlet context, i.e. one
 * returned by servletContext.getContext(String)
 */
class ExternalServletContextWrapper implements ServletContext {
    
    private final ServletContext delegate;
    private final PageContext pageContext;
    
    public ExternalServletContextWrapper(ServletContext sc, PageContext pageContext) {
        this.delegate = sc;
        this.pageContext = pageContext;
    }

    public ServletContext getContext(String s) {
        return delegate.getContext(s);
    }

    public String getContextPath() {
        return delegate.getContextPath();
    }

    public int getMajorVersion() {
        return delegate.getMajorVersion();
    }

    public int getMinorVersion() {
        return delegate.getMinorVersion();
    }

    public String getMimeType(String s) {
        return delegate.getMimeType(s);
    }

    public Set getResourcePaths(String s) {
        return delegate.getResourcePaths(s);
    }

    public URL getResource(String s) throws MalformedURLException {
        return delegate.getResource(s);
    }

    public InputStream getResourceAsStream(String s) {
        return delegate.getResourceAsStream(s);
    }

    public RequestDispatcher getRequestDispatcher(String s) {
        return new RequestDispatcherWrapper(delegate.getRequestDispatcher(s));
    }

    public RequestDispatcher getNamedDispatcher(String s) {
        return new RequestDispatcherWrapper(delegate.getNamedDispatcher(s));
    }

    public Servlet getServlet(String s) throws ServletException {
        return delegate.getServlet(s);
    }

    public Enumeration getServlets() {
        return delegate.getServlets();
    }

    public Enumeration getServletNames() {
        return delegate.getServletNames();
    }

    public void log(String s) {
        delegate.log(s);
    }

    public void log(Exception exception, String s) {
        delegate.log(exception, s);
    }

    public void log(String s, Throwable throwable) {
        delegate.log(s, throwable);
    }

    public String getRealPath(String s) {
        return delegate.getRealPath(s);
    }

    public String getServerInfo() {
        return delegate.getServerInfo();
    }

    public String getInitParameter(String s) {
        return delegate.getInitParameter(s);
    }

    public Enumeration getInitParameterNames() {
        return delegate.getInitParameterNames();
    }

    public Object getAttribute(String s) {
        return delegate.getAttribute(s);
    }

    public Enumeration getAttributeNames() {
        return delegate.getAttributeNames();
    }

    public void setAttribute(String s, Object obj) {
        delegate.setAttribute(s, obj);
    }

    public void removeAttribute(String s) {
        delegate.removeAttribute(s);
    }

    public String getServletContextName() {
        return delegate.getServletContextName();
    }

    class RequestDispatcherWrapper implements RequestDispatcher {
        
        private final RequestDispatcher delegate;
        
        public RequestDispatcherWrapper(RequestDispatcher rd) {
            this.delegate = rd;
        }

        public void forward(ServletRequest request, ServletResponse response) throws ServletException,
                IOException {
            JspFactory jspFactory = JspFactory.getDefaultFactory();
            if (jspFactory instanceof JspFactoryHandler) {
                pageContext.getOut().flush();
                final int count = ((JspFactoryHandler)jspFactory).resetUsage();
                try {
                    delegate.forward(request, response);
                } finally {
                    ((JspFactoryHandler)jspFactory).setUsage(count);
                }
            }
        }

        public void include(ServletRequest request, ServletResponse response) throws ServletException,
                IOException {
            JspFactory jspFactory = JspFactory.getDefaultFactory();
            if (jspFactory instanceof JspFactoryHandler) {
                pageContext.getOut().flush();
                final int count = ((JspFactoryHandler)jspFactory).resetUsage();
                try {
                    delegate.include(request, response);
                } finally {
                    ((JspFactoryHandler)jspFactory).setUsage(count);
                }
            }

        }

    }

}
