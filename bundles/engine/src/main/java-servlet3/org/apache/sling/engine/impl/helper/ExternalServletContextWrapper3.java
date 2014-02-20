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

import java.util.EventListener;
import java.util.Map;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletRegistration.Dynamic;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.descriptor.JspConfigDescriptor;

/**
 * Wrapper around a ServletContext for an external servlet context, i.e. one
 * returned by servletContext.getContext(String)
 */
class ExternalServletContextWrapper3 extends ExternalServletContextWrapper {

    public ExternalServletContextWrapper3(ServletContext sc) {
        super(sc);
    }

    public int getEffectiveMajorVersion() {
        return getServletContext().getEffectiveMajorVersion();
    }

    public int getEffectiveMinorVersion() {
        return getServletContext().getEffectiveMinorVersion();
    }

    public boolean setInitParameter(String name, String value) {
        return getServletContext().setInitParameter(name, value);
    }

    public Dynamic addServlet(String servletName, String className) {
        return getServletContext().addServlet(servletName, className);
    }

    public Dynamic addServlet(String servletName, Servlet servlet) {
        return getServletContext().addServlet(servletName, servlet);
    }

    public Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass) {
        return getServletContext().addServlet(servletName, servletClass);
    }

    public <T extends Servlet> T createServlet(Class<T> clazz) throws ServletException {
        return getServletContext().createServlet(clazz);
    }

    public ServletRegistration getServletRegistration(String servletName) {
        return getServletContext().getServletRegistration(servletName);
    }

    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
        return getServletContext().getServletRegistrations();
    }

    public javax.servlet.FilterRegistration.Dynamic addFilter(String filterName, String className) {
        return getServletContext().addFilter(filterName, className);
    }

    public javax.servlet.FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
        return getServletContext().addFilter(filterName, filter);
    }

    public javax.servlet.FilterRegistration.Dynamic addFilter(String filterName, Class<? extends Filter> filterClass) {
        return getServletContext().addFilter(filterName, filterClass);
    }

    public <T extends Filter> T createFilter(Class<T> clazz) throws ServletException {
        return getServletContext().createFilter(clazz);
    }

    public FilterRegistration getFilterRegistration(String filterName) {
        return getServletContext().getFilterRegistration(filterName);
    }

    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        return getServletContext().getFilterRegistrations();
    }

    public SessionCookieConfig getSessionCookieConfig() {
        return getServletContext().getSessionCookieConfig();
    }

    public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {
        getServletContext().setSessionTrackingModes(sessionTrackingModes);
    }

    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        return getServletContext().getDefaultSessionTrackingModes();
    }

    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        return getServletContext().getEffectiveSessionTrackingModes();
    }

    public void addListener(String className) {
        getServletContext().addListener(className);
    }

    public <T extends EventListener> void addListener(T t) {
        getServletContext().addListener(t);
    }

    public void addListener(Class<? extends EventListener> listenerClass) {
        getServletContext().addListener(listenerClass);
    }

    public <T extends EventListener> T createListener(Class<T> clazz) throws ServletException {
        return getServletContext().createListener(clazz);
    }

    public JspConfigDescriptor getJspConfigDescriptor() {
        return getServletContext().getJspConfigDescriptor();
    }

    public ClassLoader getClassLoader() {
        return getServletContext().getClassLoader();
    }

    public void declareRoles(String... roleNames) {
        getServletContext().declareRoles(roleNames);
    }
}
