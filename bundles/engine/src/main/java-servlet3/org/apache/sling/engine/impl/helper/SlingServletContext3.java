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
package org.apache.sling.engine.impl.helper;

import java.util.EventListener;
import java.util.Map;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletRegistration.Dynamic;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.descriptor.JspConfigDescriptor;

import org.apache.sling.engine.impl.SlingMainServlet;
import org.osgi.framework.BundleContext;

/**
 * The {@code SlingServletContext3} class extends the
 * {@link SlingServletContext} class with new Servlet API 3 methods. All
 * setter-style methods throw {@code IllegalStateException} since we assume the
 * servlet context to have been fully configured. Getter methods are route
 * through to the original servlet context we got from the HTTP Service.
 */
public class SlingServletContext3 extends SlingServletContext {

    public SlingServletContext3(final BundleContext bundleContext, final SlingMainServlet slingMainServlet) {
        super(bundleContext, slingMainServlet);
    }

    public int getEffectiveMajorVersion() {
        return getServletContext().getEffectiveMajorVersion();
    }

    public int getEffectiveMinorVersion() {
        return getServletContext().getEffectiveMinorVersion();
    }

    public boolean setInitParameter(String name, String value) {
        // only supported in ServletContextListener.contextInitialized or
        // ServletContainerInitializer.onStartuo
        throw new IllegalStateException();
    }

    public SessionCookieConfig getSessionCookieConfig() {
        return getServletContext().getSessionCookieConfig();
    }

    public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {
        // only supported in ServletContextListener.contextInitialized or
        // ServletContainerInitializer.onStartuo
        throw new IllegalStateException();
    }

    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        return getServletContext().getDefaultSessionTrackingModes();
    }

    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        return getServletContext().getEffectiveSessionTrackingModes();
    }

    public JspConfigDescriptor getJspConfigDescriptor() {
        return getServletContext().getJspConfigDescriptor();
    }

    public ClassLoader getClassLoader() {
        // we don't allow access to any class loader here since we are
        // running in the OSGi Framework and we don't want code to fiddle
        // with class laoders obtained from the ServletContext
        throw new SecurityException();
    }

    public void declareRoles(String... roleNames) {
        // only supported in ServletContextListener.contextInitialized or
        // ServletContainerInitializer.onStartuo
        throw new IllegalStateException();
    }

    // Servlet API 3.0, Section 4.4 Configuration methods

    public Dynamic addServlet(String servletName, String className) {
        // only supported in ServletContextListener.contextInitialized or
        // ServletContainerInitializer.onStartuo
        throw new IllegalStateException();
    }

    public Dynamic addServlet(String servletName, Servlet servlet) {
        // only supported in ServletContextListener.contextInitialized or
        // ServletContainerInitializer.onStartuo
        throw new IllegalStateException();
    }

    public Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass) {
        // only supported in ServletContextListener.contextInitialized or
        // ServletContainerInitializer.onStartuo
        throw new IllegalStateException();
    }

    public <T extends Servlet> T createServlet(Class<T> clazz) {
        // only supported in ServletContextListener.contextInitialized or
        // ServletContainerInitializer.onStartuo
        throw new IllegalStateException();
    }

    public ServletRegistration getServletRegistration(String servletName) {
        // only supported in ServletContextListener.contextInitialized or
        // ServletContainerInitializer.onStartuo
        throw new IllegalStateException();
    }

    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
        // only supported in ServletContextListener.contextInitialized or
        // ServletContainerInitializer.onStartuo
        throw new IllegalStateException();
    }

    public javax.servlet.FilterRegistration.Dynamic addFilter(String filterName, String className) {
        // only supported in ServletContextListener.contextInitialized or
        // ServletContainerInitializer.onStartuo
        throw new IllegalStateException();
    }

    public javax.servlet.FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
        // only supported in ServletContextListener.contextInitialized or
        // ServletContainerInitializer.onStartuo
        throw new IllegalStateException();
    }

    public javax.servlet.FilterRegistration.Dynamic addFilter(String filterName, Class<? extends Filter> filterClass) {
        // only supported in ServletContextListener.contextInitialized or
        // ServletContainerInitializer.onStartuo
        throw new IllegalStateException();
    }

    public <T extends Filter> T createFilter(Class<T> clazz) {
        // only supported in ServletContextListener.contextInitialized or
        // ServletContainerInitializer.onStartuo
        throw new IllegalStateException();
    }

    public FilterRegistration getFilterRegistration(String filterName) {
        // only supported in ServletContextListener.contextInitialized or
        // ServletContainerInitializer.onStartuo
        throw new IllegalStateException();
    }

    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        // only supported in ServletContextListener.contextInitialized or
        // ServletContainerInitializer.onStartuo
        throw new IllegalStateException();
    }

    public void addListener(String className) {
        // only supported in ServletContextListener.contextInitialized or
        // ServletContainerInitializer.onStartuo
        throw new IllegalStateException();
    }

    public <T extends EventListener> void addListener(T t) {
        // only supported in ServletContextListener.contextInitialized or
        // ServletContainerInitializer.onStartuo
        throw new IllegalStateException();
    }

    public void addListener(Class<? extends EventListener> listenerClass) {
        // only supported in ServletContextListener.contextInitialized or
        // ServletContainerInitializer.onStartuo
        throw new IllegalStateException();
    }

    public <T extends EventListener> T createListener(Class<T> clazz) {
        // only supported in ServletContextListener.contextInitialized or
        // ServletContainerInitializer.onStartuo
        throw new IllegalStateException();
    }

    protected ServletContext wrapServletContext(ServletContext context) {
        return new ExternalServletContextWrapper3(context);
    }

}
