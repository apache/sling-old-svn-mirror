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
package org.apache.sling.core.impl.helper;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;

import org.apache.sling.core.impl.SlingMainServlet;
import org.apache.sling.core.impl.request.SlingRequestDispatcher;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>SlingServletContext</code> class is the
 * <code>ServletContext</code> which is registered as a service usable by
 * servlets and helpers inside Sling. Most methods just call into the servlet
 * context in which the {@link SlingMainServlet} is running.
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
 * <dd>Logging is diverted to a logger whose name is the fully qualified name
 * of this class.</dd>
 * </dl>
 */
public class SlingServletContext implements ServletContext {

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /** The {@link SlingMainServlet} to which some calls are delegated */
    private final SlingMainServlet requestHandler;

    /** The service registration of this service as a ManagedService */
    private final ServiceRegistration registration;

    /**
     * Creates an instance of this class delegating some methods to the given
     * {@link SlingMainServlet}. In addition the new instance is registered as
     * a <code>ManagedService</code> and <code>ServletContext</code> to
     * receive configuration information.
     */
    public SlingServletContext(SlingMainServlet requestHandler) {
        this.requestHandler = requestHandler;

        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_PID, getClass().getName());
        props.put(Constants.SERVICE_DESCRIPTION, "Sling ServletContext");
        props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        registration = requestHandler.getBundleContext().registerService(
            ServletContext.class.getName(), this, props);
    }

    /**
     * Disposes of this servlet context by just unregistering as a service.
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
    public String getServletContextName() {
        return getServletContext().getServletContextName();
    }

    /**
     * Returns the init-param of the servlet context in which Sling is
     * configured. This method calls on the <code>ServletContext</code> in
     * which the {@link SlingMainServlet} is running.
     */
    public String getInitParameter(String name) {
        return getServletContext().getInitParameter(name);
    }

    /**
     * Returns the names of the init-params of the servlet context in which
     * Sling is configured. This method calls on the <code>ServletContext</code>
     * in which the {@link SlingMainServlet} is running.
     */
    @SuppressWarnings("unchecked")
    public Enumeration<String> getInitParameterNames() {
        return getServletContext().getInitParameterNames();
    }

    // ---------- attributes ---------------------------------------------------

    /**
     * Returns the named servlet context attribute. This method calls on the
     * <code>ServletContext</code> in which the {@link SlingMainServlet} is
     * running.
     */
    public Object getAttribute(String name) {
        return getServletContext().getAttribute(name);
    }

    /**
     * Returns the names of all servlet context attributes. This method calls on
     * the <code>ServletContext</code> in which the {@link SlingMainServlet}
     * is running.
     */
    @SuppressWarnings("unchecked")
    public Enumeration<String> getAttributeNames() {
        return getServletContext().getAttributeNames();
    }

    /**
     * Removes the named servlet context attribute. This method calls on the
     * <code>ServletContext</code> in which the {@link SlingMainServlet} is
     * running.
     */
    public void removeAttribute(String name) {
        getServletContext().removeAttribute(name);
    }

    /**
     * Sets the name servlet context attribute to the requested value. This
     * method calls on the <code>ServletContext</code> in which the
     * {@link SlingMainServlet} is running.
     */
    public void setAttribute(String name, Object object) {
        getServletContext().removeAttribute(name);
    }

    // ---------- Servlet Container information --------------------------------

    /**
     * Returns the Sling server info string. This is not the same server info
     * string as returned by the servlet context in which Sling is configured.
     */
    public String getServerInfo() {
        return requestHandler.getServerInfo();
    }

    /**
     * Returns the major version number of the Servlet API supported by the
     * servlet container in which Sling is running. This method calls on the
     * <code>ServletContext</code> in which the {@link SlingMainServlet} is
     * running.
     */
    public int getMajorVersion() {
        return getServletContext().getMajorVersion();
    }

    /**
     * Returns the minor version number of the Servlet API supported by the
     * servlet container in which Sling is running. This method calls on the
     * <code>ServletContext</code> in which the {@link SlingMainServlet} is
     * running.
     */
    public int getMinorVersion() {
        return getServletContext().getMinorVersion();
    }

    // ---------- MIME type mapping --------------------------------------------

    /**
     * Returns a MIME type for the extension of the given file name. This method
     * calls on the <code>ServletContext</code> in which the
     * {@link SlingMainServlet} is running.
     */
    public String getMimeType(String file) {
        return getServletContext().getMimeType(file);
    }

    // ---------- Request Dispatcher -------------------------------------------

    /**
     * Returns a {@link SlingRequestDispatcher} for the given path if not
     * <code>null</code>. Otherwise <code>null</code> is returned.
     */
    public RequestDispatcher getRequestDispatcher(String path) {
        // return no dispatcher if content is null
        if (path == null) {
            log.error("getRequestDispatcher: No path, cannot create request dispatcher");
            return null;
        }

        return new SlingRequestDispatcher(path);
    }

    /**
     * Returns a servlet container request dispatcher for the named servlet.
     * This method calls on the <code>ServletContext</code> in which the
     * {@link SlingMainServlet} is running.
     */
    public RequestDispatcher getNamedDispatcher(String name) {
        return getServletContext().getNamedDispatcher(name);
    }

    // ---------- Resource Access ----------------------------------------------

    /**
     * Returns the URI for the given path. This method calls on the
     * <code>ServletContext</code> in which the {@link SlingMainServlet} is
     * running.
     */
    public URL getResource(String path) throws MalformedURLException {
        return getServletContext().getResource(path);
    }

    /**
     * Returns an input stream to the given path. This method calls on the
     * <code>ServletContext</code> in which the {@link SlingMainServlet} is
     * running.
     */
    public InputStream getResourceAsStream(String path) {
        return getServletContext().getResourceAsStream(path);
    }

    /**
     * Returns a set of names for path entries considered children of the given
     * path. This method calls on the <code>ServletContext</code> in which the
     * {@link SlingMainServlet} is running.
     */
    @SuppressWarnings("unchecked")
    public Set<String> getResourcePaths(String parentPath) {
        return getServletContext().getResourcePaths(parentPath);
    }

    /**
     * Returns the real file inside the web application to which the given path
     * maps or <code>null</code> if no such file exists. This method calls on
     * the <code>ServletContext</code> in which the {@link SlingMainServlet}
     * is running.
     */
    public String getRealPath(String path) {
        return getServletContext().getRealPath(path);
    }

    // ---------- logging ------------------------------------------------------

    /** Logs the message and optional throwable at error level to the logger */
    public void log(String message, Throwable throwable) {
        log.error(message, throwable);
    }

    /** Logs the message at info level to the logger */
    public void log(String message) {
        log.info(message);
    }

    /** Logs the message and optional exception at error level to the logger */
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
    public ServletContext getContext(String uripath) {
        return getServletContext().getContext(uripath);
    }

    /** Returns <code>null</code> as defined in Servlet API 2.4 */
    @Deprecated
    public Servlet getServlet(String name) {
        return null;
    }

    /** Returns <code>null</code> as defined in Servlet API 2.4 */
    @Deprecated
    public Enumeration<?> getServletNames() {
        return Collections.enumeration(Collections.emptyList());
    }

    /** Returns <code>null</code> as defined in Servlet API 2.4 */
    @Deprecated
    public Enumeration<?> getServlets() {
        return Collections.enumeration(Collections.emptyList());
    }

    // ---------- internal -----------------------------------------------------

    /**
     * Returns the real servlet context of the servlet container in which the
     * Sling Servlet is running.
     */
    private ServletContext getServletContext() {
        return requestHandler.getServletContext();
    }

}
