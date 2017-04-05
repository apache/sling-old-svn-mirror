/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.scripting.jsp;

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
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletRegistration.Dynamic;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.descriptor.JspConfigDescriptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>JspServletContext</code> TODO
 */
public class JspServletContext implements ServletContext {

    /** default log */
    private static final Logger log = LoggerFactory.getLogger(JspServletContext.class);

    private final SlingIOProvider ioProvider;
    private final ServletContext delegatee;
    private final SlingTldLocationsCache tcs;

    JspServletContext(SlingIOProvider ioProvider, ServletContext componentContext,
            SlingTldLocationsCache tcs) {
        this.ioProvider = ioProvider;
        this.delegatee = componentContext;
        this.tcs = tcs;
    }

    //---------- implemented methods ------------------------------------------

    /* (non-Javadoc)
     * @see javax.servlet.ServletContext#getResource(java.lang.String)
     */
    @Override
    public URL getResource(String path) throws MalformedURLException {

        if (path.startsWith("/")) {
            URL url = ioProvider.getURL(path);
            if (url != null) {
                return url;
            }
        }

        // fall back to trying a real URL
        return getUrlForResource(path);
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletContext#getResourceAsStream(java.lang.String)
     */
    @Override
    public InputStream getResourceAsStream(String path) {
        // path might be an URL, so only check resource provider in case of an
        // absolute path - assuming URLs have no leading slash at all, we
        // don't care for the scheme separating colon here
        if (path.startsWith("/")) {
            try {
                return ioProvider.getInputStream(path);
            } catch (Exception ex) {
                // FileNotFoundException or IOException
                log.debug("getResourceAsStream: Cannot get resource {}: {}",
                    path, ex.getMessage());
            }
        }

        // check whether we can resolve as an URL ...
        try {
            // create the URL and try to access
            URL url = getUrlForResource(path);
            if (url != null) {
                return url.openStream();
            }
        } catch (Exception e) {
            log.debug(
                "getResourceAsStream: Cannot access resource {} through URL: {}",
                path, e.getMessage());
        }

        return null;
    }

    @Override
    public Set<String> getResourcePaths(String path) {
        return ioProvider.getResourcePaths(path);
    }

    @Override
    public void log(String msg) {
        log.info(msg);
    }

    @Override
    @Deprecated
    public void log(Exception exception, String msg) {
        log(msg, exception);
    }

    @Override
    public void log(String message, Throwable throwable) {
        log.error(message, throwable);
    }

    //---------- delegated methods --------------------------------------------

    @Override
    public Object getAttribute(String name) {
        return delegatee.getAttribute(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return delegatee.getAttributeNames();
    }

    @Override
    public void removeAttribute(String name) {
        delegatee.removeAttribute(name);
    }

    @Override
    public void setAttribute(String name, Object object) {
        delegatee.setAttribute(name, object);
    }

    @Override
    public ServletContext getContext(String uripath) {
        return delegatee.getContext(uripath);
    }

    @Override
    public String getInitParameter(String name) {
        return delegatee.getInitParameter(name);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return delegatee.getInitParameterNames();
    }

    @Override
    public int getMajorVersion() {
        return delegatee.getMajorVersion();
    }

    @Override
    public String getMimeType(String file) {
        return delegatee.getMimeType(file);
    }

    @Override
    public int getMinorVersion() {
        return delegatee.getMinorVersion();
    }

    @Override
    public RequestDispatcher getNamedDispatcher(String name) {
        return delegatee.getNamedDispatcher(name);
    }

    @Override
    public String getRealPath(String path) {
        return delegatee.getRealPath(path);
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        return delegatee.getRequestDispatcher(path);
    }

    @Override
    public String getServerInfo() {
        return delegatee.getServerInfo();
    }

    @Override
    @Deprecated
    public Servlet getServlet(String name) throws ServletException {
        return delegatee.getServlet(name);
    }

    @Override
    public String getServletContextName() {
        return delegatee.getServletContextName();
    }

    @Override
    @Deprecated
    public Enumeration<String> getServletNames() {
        return delegatee.getServletNames();
    }

    @Override
    @Deprecated
    public Enumeration<Servlet> getServlets() {
        return delegatee.getServlets();
    }

    // Servlet API 2.5 method
    @Override
    public String getContextPath() {
        return delegatee.getContextPath();
    }

    //---------- internal -----------------------------------------------------

    private URL getUrlForResource(String path) {
        int cs = path.indexOf(":/");
        if (cs > 0 && cs < path.length()-2) {
            // insert second slash after scheme (was canonicalized away)
            cs += 2;
            if (cs < path.length() && path.charAt(cs) != '/') {
                path = path.substring(0, cs) + "/" + path.substring(cs);
            }

            // TODO: tmp workaround while URLStreamHandler support is not available
            URL url = tcs.getTldLocationURL(path);
            if (url != null) {
                return url;
            }

            // create the URL and try to access
            try {
                return new URL(path);
            } catch (MalformedURLException mue) {
                log.debug("getUrlForResource: Cannot create URL for {}: {}",
                    path, mue.getMessage());
            }
        }

        return null;
    }

    @Override
    public int getEffectiveMajorVersion() {
        return delegatee.getEffectiveMajorVersion();
    }

    @Override
    public int getEffectiveMinorVersion() {
        return delegatee.getEffectiveMinorVersion();
    }

    @Override
    public boolean setInitParameter(String name, String value) {
        return delegatee.setInitParameter(name, value);
    }

    @Override
    public Dynamic addServlet(String servletName, String className) {
        return delegatee.addServlet(servletName, className);
    }

    @Override
    public Dynamic addServlet(String servletName, Servlet servlet) {
        return delegatee.addServlet(servletName, servlet);
    }

    @Override
    public Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass) {
        return delegatee.addServlet(servletName, servletClass);
    }

    @Override
    public <T extends Servlet> T createServlet(Class<T> clazz) throws ServletException {
        return delegatee.createServlet(clazz);
    }

    @Override
    public ServletRegistration getServletRegistration(String servletName) {
        return delegatee.getServletRegistration(servletName);
    }

    @Override
    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
        return delegatee.getServletRegistrations();
    }

    @Override
    public javax.servlet.FilterRegistration.Dynamic addFilter(String filterName, String className) {
        return delegatee.addFilter(filterName, className);
    }

    @Override
    public javax.servlet.FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
        return delegatee.addFilter(filterName, filter);
    }

    @Override
    public javax.servlet.FilterRegistration.Dynamic addFilter(String filterName, Class<? extends Filter> filterClass) {
        return delegatee.addFilter(filterName, filterClass);
    }

    @Override
    public <T extends Filter> T createFilter(Class<T> clazz) throws ServletException {
        return delegatee.createFilter(clazz);
    }

    @Override
    public FilterRegistration getFilterRegistration(String filterName) {
        return delegatee.getFilterRegistration(filterName);
    }

    @Override
    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        return delegatee.getFilterRegistrations();
    }

    @Override
    public SessionCookieConfig getSessionCookieConfig() {
        return delegatee.getSessionCookieConfig();
    }

    @Override
    public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {
        delegatee.setSessionTrackingModes(sessionTrackingModes);
    }

    @Override
    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        return delegatee.getDefaultSessionTrackingModes();
    }

    @Override
    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        return delegatee.getEffectiveSessionTrackingModes();
    }

    @Override
    public void addListener(String className) {
        delegatee.addListener(className);
    }

    @Override
    public <T extends EventListener> void addListener(T t) {
        delegatee.addListener(t);
    }

    @Override
    public void addListener(Class<? extends EventListener> listenerClass) {
        delegatee.addListener(listenerClass);
    }

    @Override
    public <T extends EventListener> T createListener(Class<T> clazz) throws ServletException {
        return delegatee.createListener(clazz);
    }

    @Override
    public JspConfigDescriptor getJspConfigDescriptor() {
        return delegatee.getJspConfigDescriptor();
    }

    @Override
    public ClassLoader getClassLoader() {
        return delegatee.getClassLoader();
    }

    @Override
    public void declareRoles(String... roleNames) {
        delegatee.declareRoles(roleNames);
    }

    @Override
    public String getVirtualServerName() {
        return delegatee.getVirtualServerName();
    }
}
