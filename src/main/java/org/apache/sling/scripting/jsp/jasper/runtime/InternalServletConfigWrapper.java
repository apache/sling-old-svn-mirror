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
import java.util.EventListener;
import java.util.Map;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletRegistration.Dynamic;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.descriptor.JspConfigDescriptor;
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

    @Override
    public String getServletName() {
        return delegate.getServletName();
    }

    @Override
    public ServletContext getServletContext() {
        return new InternalServletContextWrapper(delegate.getServletContext());
    }

    @Override
    public String getInitParameter(String s) {
        return delegate.getInitParameter(s);
    }

    @Override
    public Enumeration getInitParameterNames() {
        return delegate.getInitParameterNames();
    }

    class InternalServletContextWrapper implements ServletContext {

        private final ServletContext delegate;

        @Override
        public int getEffectiveMajorVersion() {
            return delegate.getEffectiveMajorVersion();
        }

        @Override
        public int getEffectiveMinorVersion() {
            return delegate.getEffectiveMinorVersion();
        }

        @Override
        public boolean setInitParameter(String name, String value) {
            return delegate.setInitParameter(name, value);
        }

        @Override
        public Dynamic addServlet(String servletName, String className) {
            return delegate.addServlet(servletName, className);
        }

        @Override
        public Dynamic addServlet(String servletName, Servlet servlet) {
            return delegate.addServlet(servletName, servlet);
        }

        @Override
        public Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass) {
            return delegate.addServlet(servletName, servletClass);
        }

        @Override
        public <T extends Servlet> T createServlet(Class<T> clazz) throws ServletException {
            return delegate.createServlet(clazz);
        }

        @Override
        public ServletRegistration getServletRegistration(String servletName) {
            return delegate.getServletRegistration(servletName);
        }

        @Override
        public Map<String, ? extends ServletRegistration> getServletRegistrations() {
            return delegate.getServletRegistrations();
        }

        @Override
        public javax.servlet.FilterRegistration.Dynamic addFilter(String filterName, String className) {
            return delegate.addFilter(filterName, className);
        }

        @Override
        public javax.servlet.FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
            return delegate.addFilter(filterName, filter);
        }

        @Override
        public javax.servlet.FilterRegistration.Dynamic addFilter(String filterName,
                Class<? extends Filter> filterClass) {
            return delegate.addFilter(filterName, filterClass);
        }

        @Override
        public <T extends Filter> T createFilter(Class<T> clazz) throws ServletException {
            return delegate.createFilter(clazz);
        }

        @Override
        public FilterRegistration getFilterRegistration(String filterName) {
            return delegate.getFilterRegistration(filterName);
        }

        @Override
        public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
            return delegate.getFilterRegistrations();
        }

        @Override
        public SessionCookieConfig getSessionCookieConfig() {
            return delegate.getSessionCookieConfig();
        }

        @Override
        public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {
            delegate.setSessionTrackingModes(sessionTrackingModes);
        }

        @Override
        public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
            return delegate.getDefaultSessionTrackingModes();
        }

        @Override
        public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
            return delegate.getEffectiveSessionTrackingModes();
        }

        @Override
        public void addListener(String className) {
            delegate.addListener(className);
        }

        @Override
        public <T extends EventListener> void addListener(T t) {
            delegate.addListener(t);
        }

        @Override
        public void addListener(Class<? extends EventListener> listenerClass) {
            delegate.addListener(listenerClass);
        }

        @Override
        public <T extends EventListener> T createListener(Class<T> clazz) throws ServletException {
            return delegate.createListener(clazz);
        }

        @Override
        public JspConfigDescriptor getJspConfigDescriptor() {
            return delegate.getJspConfigDescriptor();
        }

        @Override
        public ClassLoader getClassLoader() {
            return delegate.getClassLoader();
        }

        @Override
        public void declareRoles(String... roleNames) {
            delegate.declareRoles(roleNames);
        }

        @Override
        public String getVirtualServerName() {
            return delegate.getVirtualServerName();
        }

        public InternalServletContextWrapper(ServletContext sc) {
            this.delegate = sc;
        }

        @Override
        public ServletContext getContext(String s) {
            ServletContext sc = delegate.getContext(s);
            if (sc == delegate) {
                return this;
            } else {
                return new ExternalServletContextWrapper(sc, pageContext);
            }
        }

        @Override
        public String getContextPath() {
            return delegate.getContextPath();
        }

        @Override
        public int getMajorVersion() {
            return delegate.getMajorVersion();
        }

        @Override
        public int getMinorVersion() {
            return delegate.getMinorVersion();
        }

        @Override
        public String getMimeType(String s) {
            return delegate.getMimeType(s);
        }

        @Override
        public Set<String> getResourcePaths(String s) {
            return delegate.getResourcePaths(s);
        }

        @Override
        public URL getResource(String s) throws MalformedURLException {
            return delegate.getResource(s);
        }

        @Override
        public InputStream getResourceAsStream(String s) {
            return delegate.getResourceAsStream(s);
        }

        @Override
        public RequestDispatcher getRequestDispatcher(String s) {
            return delegate.getRequestDispatcher(s);
        }

        @Override
        public RequestDispatcher getNamedDispatcher(String s) {
            return delegate.getNamedDispatcher(s);
        }

        @Override
        public Servlet getServlet(String s) throws ServletException {
            return delegate.getServlet(s);
        }

        @Override
        public Enumeration<Servlet> getServlets() {
            return delegate.getServlets();
        }

        @Override
        public Enumeration<String> getServletNames() {
            return delegate.getServletNames();
        }

        @Override
        public void log(String s) {
            delegate.log(s);
        }

        @Override
        public void log(Exception exception, String s) {
            delegate.log(exception, s);
        }

        @Override
        public void log(String s, Throwable throwable) {
            delegate.log(s, throwable);
        }

        @Override
        public String getRealPath(String s) {
            return delegate.getRealPath(s);
        }

        @Override
        public String getServerInfo() {
            return delegate.getServerInfo();
        }

        @Override
        public String getInitParameter(String s) {
            return delegate.getInitParameter(s);
        }

        @Override
        public Enumeration<String> getInitParameterNames() {
            return delegate.getInitParameterNames();
        }

        @Override
        public Object getAttribute(String s) {
            return delegate.getAttribute(s);
        }

        @Override
        public Enumeration<String> getAttributeNames() {
            return delegate.getAttributeNames();
        }

        @Override
        public void setAttribute(String s, Object obj) {
            delegate.setAttribute(s, obj);
        }

        @Override
        public void removeAttribute(String s) {
            delegate.removeAttribute(s);
        }

        @Override
        public String getServletContextName() {
            return delegate.getServletContextName();
        }


    }



}
