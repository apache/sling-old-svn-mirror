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

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletRegistration.Dynamic;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.descriptor.JspConfigDescriptor;

import org.apache.sling.engine.impl.SlingMainServlet;
import org.apache.sling.engine.impl.request.SlingRequestDispatcher;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>SlingServletContext</code> class is the <code>ServletContext</code>
 * which is registered as a service usable by servlets and helpers inside Sling.
 * Most methods just call into the servlet context in which the
 * {@link SlingMainServlet} is running.
 * <dl>
 * <dt><b>MIME Type Mapping</b></dt>
 * <dd>Just forwards to the servlet context of the {@link SlingMainServlet} for
 * MIME type mapping.</dd>
 * <dt><b>Resources</b></dt>
 * <dd>This class provides access to the resources in the web application by
 * means of the respective resource accessor methods. These are not the same
 * resources as available through the <code>ResourceResolver</code>.</dd>
 * <dt><b>Request Dispatcher</b></dt>
 * <dd>The {@link #getRequestDispatcher(String)} method returns a
 * {@link SlingRequestDispatcher} which may dispatch a request inside sling
 * without going through the servlet container. The
 * {@link #getNamedDispatcher(String)} method returns a servlet container
 * request dispatcher which always goes through the servlet container.</dd>
 * <dt><b>Parameters and Attributes</b></dt>
 * <dd>Initialization parameters and context attributes are shared with the
 * servlet context in which the {@link SlingMainServlet} is running.</dd>
 * <dt><b>Logging</b></dt>
 * <dd>Logging is diverted to a logger whose name is the fully qualified name of
 * this class.</dd>
 * </dl>
 * <p>
 * This class implements the Servlet API 3.0 {@code ServletContext} interface.
 */
public class SlingServletContext implements ServletContext {

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /** The {@link SlingMainServlet} to which some calls are delegated */
    private final SlingMainServlet slingMainServlet;

    /**
     * The service registration of this service as ServletContext
     * @see #SlingServletContext(SlingMainServlet)
     * @see #dispose()
     */
    private final ServiceRegistration registration;

    /**
     * Creates an instance of this class delegating some methods to the given
     * {@link SlingMainServlet}. In addition the new instance is registered as
     * a<code>ServletContext</code>.
     * <p>
     * This method must only be called <b>after</b> the sling main servlet
     * has been fully initialized. Otherwise the {@link #getServletContext()}
     * method may cause a {@link NullPointerException} !
     * @see #dispose()
     */
    public SlingServletContext(final BundleContext bundleContext,
            final SlingMainServlet slingMainServlet) {
        this.slingMainServlet = slingMainServlet;

        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_PID, getClass().getName());
        props.put(Constants.SERVICE_DESCRIPTION, "Apache Sling ServletContext");
        props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        props.put("name", "org.apache.sling"); // property to identify this context
        registration = bundleContext.registerService(
            ServletContext.class.getName(), this, props);
    }

    /**
     * Unregisters this servlet context as a service (if registered at all)
     * <p>
     * This method must be called <b>before</b> the sling main servlet
     * is destroyed. Otherwise the {@link #getServletContext()} method may
     * cause a {@link NullPointerException} !
     * @see #SlingServletContext(SlingMainServlet)
     */
    public void dispose() {
        if (registration != null) {
            registration.unregister();
        }
    }

    // ---------- Web App configuration ----------------------------------------

    /**
     * Returns the name of the servlet context in which Sling is configured.
     * This method calls on the <code>ServletContext</code> in which the
     * {@link SlingMainServlet} is running.
     */
    @Override
    public String getServletContextName() {
        ServletContext delegatee = getServletContext();
        if (delegatee != null) {
            return delegatee.getServletContextName();
        }

        return null;
    }

    /** Returns the context path of the web application. (Servlet API 2.5) */
    @Override
    public String getContextPath() {
        ServletContext delegatee = getServletContext();
        if (delegatee != null) {
            return delegatee.getContextPath();
        }

        return null;
    }

    /**
     * Returns the init-param of the servlet context in which Sling is
     * configured. This method calls on the <code>ServletContext</code> in
     * which the {@link SlingMainServlet} is running.
     */
    @Override
    public String getInitParameter(String name) {
        ServletContext delegatee = getServletContext();
        if (delegatee != null) {
            return delegatee.getInitParameter(name);
        }

        return null;
    }

    /**
     * Returns the names of the init-params of the servlet context in which
     * Sling is configured. This method calls on the <code>ServletContext</code>
     * in which the {@link SlingMainServlet} is running.
     */
    @Override
    public Enumeration<String> getInitParameterNames() {
        ServletContext delegatee = getServletContext();
        if (delegatee != null) {
            return delegatee.getInitParameterNames();
        }

        return null;
    }

    // ---------- attributes ---------------------------------------------------

    /**
     * Returns the named servlet context attribute. This method calls on the
     * <code>ServletContext</code> in which the {@link SlingMainServlet} is
     * running.
     */
    @Override
    public Object getAttribute(String name) {
        ServletContext delegatee = getServletContext();
        if (delegatee != null) {
            return delegatee.getAttribute(name);
        }

        return null;
    }

    /**
     * Returns the names of all servlet context attributes. This method calls on
     * the <code>ServletContext</code> in which the {@link SlingMainServlet}
     * is running.
     */
    @Override
    public Enumeration<String> getAttributeNames() {
        ServletContext delegatee = getServletContext();
        if (delegatee != null) {
            return delegatee.getAttributeNames();
        }

        return Collections.enumeration(Collections.<String>emptyList());
    }

    /**
     * Removes the named servlet context attribute. This method calls on the
     * <code>ServletContext</code> in which the {@link SlingMainServlet} is
     * running.
     */
    @Override
    public void removeAttribute(String name) {
        ServletContext delegatee = getServletContext();
        if (delegatee != null) {
            delegatee.removeAttribute(name);
        }
    }

    /**
     * Sets the name servlet context attribute to the requested value. This
     * method calls on the <code>ServletContext</code> in which the
     * {@link SlingMainServlet} is running.
     */
    @Override
    public void setAttribute(String name, Object object) {
        ServletContext delegatee = getServletContext();
        if (delegatee != null) {
            delegatee.setAttribute(name, object);
        }
    }

    // ---------- Servlet Container information --------------------------------

    /**
     * Returns the Sling server info string. This is not the same server info
     * string as returned by the servlet context in which Sling is configured.
     */
    @Override
    public String getServerInfo() {
        return slingMainServlet.getServerInfo();
    }

    /**
     * Returns the major version number of the Servlet API supported by the
     * servlet container in which Sling is running. This method calls on the
     * <code>ServletContext</code> in which the {@link SlingMainServlet} is
     * running.
     */
    @Override
    public int getMajorVersion() {
        ServletContext delegatee = getServletContext();
        if (delegatee != null) {
            return delegatee.getMajorVersion();
        }

        return 3; // hard coded major version as fall back
    }

    /**
     * Returns the minor version number of the Servlet API supported by the
     * servlet container in which Sling is running. This method calls on the
     * <code>ServletContext</code> in which the {@link SlingMainServlet} is
     * running.
     */
    @Override
    public int getMinorVersion() {
        ServletContext delegatee = getServletContext();
        if (delegatee != null) {
            return delegatee.getMinorVersion();
        }

        return 0; // hard coded minor version as fall back
    }

    // ---------- MIME type mapping --------------------------------------------

    /**
     * Returns a MIME type for the extension of the given file name. This method
     * calls on the <code>ServletContext</code> in which the
     * {@link SlingMainServlet} is running.
     */
    @Override
    public String getMimeType(String file) {
        ServletContext delegatee = getServletContext();
        if (delegatee != null) {
            return delegatee.getMimeType(file);
        }

        return null;
    }

    // ---------- Request Dispatcher -------------------------------------------

    /**
     * Returns a {@link SlingRequestDispatcher} for the given path if not
     * <code>null</code>. Otherwise <code>null</code> is returned.
     */
    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        // return no dispatcher if content is null
        if (path == null) {
            log.error("getRequestDispatcher: No path, cannot create request dispatcher");
            return null;
        }

        return new SlingRequestDispatcher(path, null);
    }

    /**
     * Returns a servlet container request dispatcher for the named servlet.
     * This method calls on the <code>ServletContext</code> in which the
     * {@link SlingMainServlet} is running.
     */
    @Override
    public RequestDispatcher getNamedDispatcher(String name) {
        ServletContext delegatee = getServletContext();
        if (delegatee != null) {
            return delegatee.getNamedDispatcher(name);
        }

        return null;
    }

    // ---------- Resource Access ----------------------------------------------

    /**
     * Returns the URI for the given path. This method calls on the
     * <code>ServletContext</code> in which the {@link SlingMainServlet} is
     * running.
     */
    @Override
    public URL getResource(String path) throws MalformedURLException {
        ServletContext delegatee = getServletContext();
        if (delegatee != null) {
            return delegatee.getResource(path);
        }

        return null;
    }

    /**
     * Returns an input stream to the given path. This method calls on the
     * <code>ServletContext</code> in which the {@link SlingMainServlet} is
     * running.
     */
    @Override
    public InputStream getResourceAsStream(String path) {
        ServletContext delegatee = getServletContext();
        if (delegatee != null) {
            return delegatee.getResourceAsStream(path);
        }

        return null;
    }

    /**
     * Returns a set of names for path entries considered children of the given
     * path. This method calls on the <code>ServletContext</code> in which the
     * {@link SlingMainServlet} is running.
     */
    @Override
    public Set<String> getResourcePaths(String parentPath) {
        ServletContext delegatee = getServletContext();
        if (delegatee != null) {
            return delegatee.getResourcePaths(parentPath);
        }

        return null;
    }

    /**
     * Returns the real file inside the web application to which the given path
     * maps or <code>null</code> if no such file exists. This method calls on
     * the <code>ServletContext</code> in which the {@link SlingMainServlet}
     * is running.
     */
    @Override
    public String getRealPath(String path) {
        ServletContext delegatee = getServletContext();
        if (delegatee != null) {
            return delegatee.getRealPath(path);
        }

        return null;
    }

    // ---------- logging ------------------------------------------------------

    /** Logs the message and optional throwable at error level to the logger */
    @Override
    public void log(String message, Throwable throwable) {
        log.error(message, throwable);
    }

    /** Logs the message at info level to the logger */
    @Override
    public void log(String message) {
        log.info(message);
    }

    /** Logs the message and optional exception at error level to the logger */
    @SuppressWarnings("deprecation")
    @Override
    @Deprecated
    public void log(Exception exception, String message) {
        log(message, exception);
    }

    // ---------- foreign Servlets ---------------------------------------------

    /**
     * Returns the servlet context from the servlet container in which sling is
     * running. This method calls on the <code>ServletContext</code> in which
     * the {@link SlingMainServlet} is running.
     */
    @Override
    public ServletContext getContext(String uripath) {
        ServletContext delegatee = getServletContext();
        if (delegatee != null) {
            ServletContext otherContext = delegatee.getContext(uripath);
            if (otherContext != null && otherContext != delegatee) {
                return wrapServletContext(otherContext);
            }
        }

        return null;
    }

    /** Returns <code>null</code> as defined in Servlet API 2.4 */
    @SuppressWarnings("deprecation")
    @Override
    @Deprecated
    public Servlet getServlet(String name) {
        return null;
    }

    /** Returns an empty enumeration as defined in Servlet API 2.4 */
    @SuppressWarnings("deprecation")
    @Override
    @Deprecated
    public Enumeration<String> getServletNames() {
        return Collections.enumeration(Collections.<String>emptyList());
    }

    /** Returns an empty enumeration as defined in Servlet API 2.4 */
    @SuppressWarnings("deprecation")
    @Override
    @Deprecated
    public Enumeration<Servlet> getServlets() {
        return Collections.enumeration(Collections.<Servlet>emptyList());
    }

    @Override
    public int getEffectiveMajorVersion() {
        ServletContext delegatee = getServletContext();
        if (delegatee != null) {
            return delegatee.getEffectiveMajorVersion();
        }
        return 3;
    }

    @Override
    public int getEffectiveMinorVersion() {
        ServletContext delegatee = getServletContext();
        if (delegatee != null) {
            return delegatee.getEffectiveMinorVersion();
        }
        return 0;
    }

    @Override
    public boolean setInitParameter(String name, String value) {
        // only supported in ServletContextListener.contextInitialized or
        // ServletContainerInitializer.onStartuo
        throw new IllegalStateException();
    }

    @Override
    public SessionCookieConfig getSessionCookieConfig() {
        // result in NPE if context is not set anymore
        return getServletContext().getSessionCookieConfig();
    }

    @Override
    public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {
        // only supported in ServletContextListener.contextInitialized or
        // ServletContainerInitializer.onStartuo
        throw new IllegalStateException();
    }

    @Override
    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        // result in NPE if context is not set anymore
        return getServletContext().getDefaultSessionTrackingModes();
    }

    @Override
    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        // result in NPE if context is not set anymore
        return getServletContext().getEffectiveSessionTrackingModes();
    }

    @Override
    public JspConfigDescriptor getJspConfigDescriptor() {
        // result in NPE if context is not set anymore
        return getServletContext().getJspConfigDescriptor();
    }

    @Override
    public ClassLoader getClassLoader() {
        // we don't allow access to any class loader here since we are
        // running in the OSGi Framework and we don't want code to fiddle
        // with class laoders obtained from the ServletContext
        throw new SecurityException();
    }

    @Override
    public void declareRoles(String... roleNames) {
        // only supported in ServletContextListener.contextInitialized or
        // ServletContainerInitializer.onStartuo
        throw new IllegalStateException();
    }

    // Servlet API 3.0, Section 4.4 Configuration methods

    @Override
    public Dynamic addServlet(String servletName, String className) {
        // only supported in ServletContextListener.contextInitialized or
        // ServletContainerInitializer.onStartuo
        throw new IllegalStateException();
    }

    @Override
    public Dynamic addServlet(String servletName, Servlet servlet) {
        // only supported in ServletContextListener.contextInitialized or
        // ServletContainerInitializer.onStartuo
        throw new IllegalStateException();
    }

    @Override
    public Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass) {
        // only supported in ServletContextListener.contextInitialized or
        // ServletContainerInitializer.onStartuo
        throw new IllegalStateException();
    }

    @Override
    public <T extends Servlet> T createServlet(Class<T> clazz) {
        // only supported in ServletContextListener.contextInitialized or
        // ServletContainerInitializer.onStartuo
        throw new IllegalStateException();
    }

    @Override
    public ServletRegistration getServletRegistration(String servletName) {
        // only supported in ServletContextListener.contextInitialized or
        // ServletContainerInitializer.onStartuo
        throw new IllegalStateException();
    }

    @Override
    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
        // only supported in ServletContextListener.contextInitialized or
        // ServletContainerInitializer.onStartuo
        throw new IllegalStateException();
    }

    @Override
    public javax.servlet.FilterRegistration.Dynamic addFilter(String filterName, String className) {
        // only supported in ServletContextListener.contextInitialized or
        // ServletContainerInitializer.onStartuo
        throw new IllegalStateException();
    }

    @Override
    public javax.servlet.FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
        // only supported in ServletContextListener.contextInitialized or
        // ServletContainerInitializer.onStartuo
        throw new IllegalStateException();
    }

    @Override
    public javax.servlet.FilterRegistration.Dynamic addFilter(String filterName, Class<? extends Filter> filterClass) {
        // only supported in ServletContextListener.contextInitialized or
        // ServletContainerInitializer.onStartuo
        throw new IllegalStateException();
    }

    @Override
    public <T extends Filter> T createFilter(Class<T> clazz) {
        // only supported in ServletContextListener.contextInitialized or
        // ServletContainerInitializer.onStartuo
        throw new IllegalStateException();
    }

    @Override
    public FilterRegistration getFilterRegistration(String filterName) {
        // only supported in ServletContextListener.contextInitialized or
        // ServletContainerInitializer.onStartuo
        throw new IllegalStateException();
    }

    @Override
    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        // only supported in ServletContextListener.contextInitialized or
        // ServletContainerInitializer.onStartuo
        throw new IllegalStateException();
    }

    @Override
    public void addListener(String className) {
        // only supported in ServletContextListener.contextInitialized or
        // ServletContainerInitializer.onStartuo
        throw new IllegalStateException();
    }

    @Override
    public <T extends EventListener> void addListener(T t) {
        // only supported in ServletContextListener.contextInitialized or
        // ServletContainerInitializer.onStartuo
        throw new IllegalStateException();
    }

    @Override
    public void addListener(Class<? extends EventListener> listenerClass) {
        // only supported in ServletContextListener.contextInitialized or
        // ServletContainerInitializer.onStartuo
        throw new IllegalStateException();
    }

    @Override
    public <T extends EventListener> T createListener(Class<T> clazz) {
        // only supported in ServletContextListener.contextInitialized or
        // ServletContainerInitializer.onStartuo
        throw new IllegalStateException();
    }

    // ---------- internal -----------------------------------------------------

    /**
     * Returns the real servlet context of the servlet container in which the
     * Sling Servlet is running.
     */
    protected ServletContext getServletContext() {
        return slingMainServlet.getServletContext();
    }

    protected ServletContext wrapServletContext(ServletContext context) {
        return new ExternalServletContextWrapper(context);
    }
}
