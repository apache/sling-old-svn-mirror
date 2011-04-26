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

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.jsp.PageContext;

/**
 * Wrapper around the Sling servlet config in order to return a wrapped servlet context
 * for other servlet contexts.
 */
class InternalServletConfigWrapper implements ServletConfig {
    
    private final ServletConfig delegate;
    
    private final PageContext pageContext;
    
    public InternalServletConfigWrapper(ServletConfig sc, PageContext pageContext) {
        this.delegate = sc;
        this.pageContext = pageContext;
    }

    public String getServletName() {
        return delegate.getServletName();
    }

    public ServletContext getServletContext() {
        return new InternalServletContextWrapper(delegate.getServletContext());
    }

    public String getInitParameter(String s) {
        return delegate.getInitParameter(s);
    }

    public Enumeration getInitParameterNames() {
        return delegate.getInitParameterNames();
    }
    
    class InternalServletContextWrapper implements ServletContext {
        
        private final ServletContext delegate;
        
        public InternalServletContextWrapper(ServletContext sc) {
            this.delegate = sc;
        }

        public ServletContext getContext(String s) {
            ServletContext sc = delegate.getContext(s);
            if (sc == delegate) {
                return this;
            } else {
                return new ExternalServletContextWrapper(sc, pageContext);
            }
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
            return delegate.getRequestDispatcher(s);
        }

        public RequestDispatcher getNamedDispatcher(String s) {
            return delegate.getNamedDispatcher(s);
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
        
        
    }
    
    

}
