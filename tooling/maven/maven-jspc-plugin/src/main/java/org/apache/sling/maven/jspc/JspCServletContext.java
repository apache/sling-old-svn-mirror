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
package org.apache.sling.maven.jspc;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

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

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.logging.Log;

/**
 * Simple <code>ServletContext</code> implementation without HTTP-specific
 * methods.
 * <p>
 * This class has been copied from the JspCServletContext of the
 * Jasper 5.5.20 distribution and reformated to match the formating rules of
 * Sling. Additionally, the {@link #getResource(String)} method has special
 * knowledge of the <em>/WEB-INF/web.xml</em> file, which is not required by
 * the JspC plugin but by the TldLocationsCache. Hence this method simulates
 * the <em>web.xml</em> with an embedded empty resource.
 */

public class JspCServletContext implements ServletContext {

    /**
     * The path of the standard <em>web.xml</em> file, which is handled by the
     * embedded file by the {@link #getResource(String)} method (value is
     * "/WEB-INF/web.xml").
     */
    private static final String WEB_XML = "/WEB-INF/web.xml";

    /**
     * Servlet context attributes.
     */
    private Hashtable<String, Object> attributes;

    /**
     * The log writer we will write log messages to.
     */
    private Log log;

    /**
     * The base URL (document root) for this context.
     */
    private URL resourceBaseURL;

    /**
     * alternative base urls
     */
    private List<URL> baseURLs = new ArrayList<>();

    /**
     * Create a new instance of this ServletContext implementation.
     *
     * @param log The Log which is used for <code>log()</code> calls
     * @param resourceBaseURL Resource base URL
     */
    public JspCServletContext(Log log, URL resourceBaseURL) {
        this.attributes = new Hashtable<>();
        this.log = log;
        this.resourceBaseURL = resourceBaseURL;
        this.baseURLs.add(resourceBaseURL);
    }

    /**
     * Adds an alternative base url for finding resources.
     * @param altBaseURL alternative resource base
     */
    public void addAlternativeBaseURL(URL altBaseURL) {
        this.baseURLs.add(altBaseURL);
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Return the specified context attribute, if any.
     *
     * @param name Name of the requested attribute
     */
    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    /**
     * Return an enumeration of context attribute names.
     */
    @Override
    public Enumeration<String> getAttributeNames() {
        return attributes.keys();
    }

    /**
     * Return the servlet context for the specified path.
     *
     * @param uripath Server-relative path starting with '/'
     */
    @Override
    public ServletContext getContext(String uripath) {
        return null;
    }

    /**
     * Return the specified context initialization parameter.
     *
     * @param name Name of the requested parameter
     */
    @Override
    public String getInitParameter(String name) {
        return null;
    }

    /**
     * Return an enumeration of the names of context initialization parameters.
     */
    @Override
    public Enumeration<String> getInitParameterNames() {
        return new Vector<String>().elements();
    }

    /**
     * Return the Servlet API major version number.
     */
    @Override
    public int getMajorVersion() {
        return 3;
    }

    /**
     * Return the MIME type for the specified filename.
     *
     * @param file Filename whose MIME type is requested
     */
    @Override
    public String getMimeType(String file) {
        return null;
    }

    /**
     * Return the Servlet API minor version number.
     */
    @Override
    public int getMinorVersion() {
        return 1;
    }

    /**
     * Return a request dispatcher for the specified servlet name.
     *
     * @param name Name of the requested servlet
     */
    @Override
    public RequestDispatcher getNamedDispatcher(String name) {
        return null;
    }

    /**
     * Return the real path for the specified context-relative virtual path.
     *
     * @param path The context-relative virtual path to resolve
     */
    @Override
    public String getRealPath(String path) {
        if (!"file".equals(resourceBaseURL.getProtocol())) {
            return null;
        }

        if (!path.startsWith("/")) {
            return null;
        }

        try {
            return getResource(path).getFile().replace('/', File.separatorChar);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Return a request dispatcher for the specified context-relative path.
     *
     * @param path Context-relative path for which to acquire a dispatcher
     */
    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        return null;
    }

    /**
     * Return a URL object of a resource that is mapped to the specified
     * context-relative path.
     *
     * @param path Context-relative path of the desired resource
     * @exception MalformedURLException if the resource path is not properly
     *                formed
     */
    @Override
    public URL getResource(String path) throws MalformedURLException {

        // catch for dummy web.xml
        if (WEB_XML.equals(path)) {
            return this.getClass().getResource("web.xml");
        }

        if (!path.startsWith("/")) {
            throw new MalformedURLException("Path '" + path + "' does not start with '/'");
        }

        for (URL base: baseURLs) {
            URL url = new URL(base, path.substring(1));
            InputStream is = null;
            try {
                is = url.openStream();
                // open stream succeeds, so resource exists.
                return url;
            } catch (Throwable t) {
                // ignore
            } finally {
                IOUtils.closeQuietly(is);
            }
        }
        return null;
    }

    /**
     * Return an InputStream allowing access to the resource at the specified
     * context-relative path.
     *
     * @param path Context-relative path of the desired resource
     */
    @Override
    public InputStream getResourceAsStream(String path) {
        try {
            return getResource(path).openStream();
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Return the set of resource paths for the "directory" at the specified
     * context path.
     *
     * @param path Context-relative base path
     */
    @Override
    public Set<String> getResourcePaths(String path) {
        Set<String> thePaths = new HashSet<>();

        if (!path.endsWith("/")) {
            path += "/";
        }

        String basePath = getRealPath(path);
        if (basePath == null) {
            return (thePaths);
        }

        File theBaseDir = new File(basePath);
        if (!theBaseDir.exists() || !theBaseDir.isDirectory()) {
            return (thePaths);
        }

        for (String theFile : theBaseDir.list()) {
            File testFile = new File(basePath + File.separator + theFile);
            if (testFile.isFile()) {
                thePaths.add(path + theFile);
            } else if (testFile.isDirectory()) {
                thePaths.add(path + theFile + "/");
            }
        }

        return thePaths;
    }

    /**
     * Return descriptive information about this server.
     */
    @Override
    public String getServerInfo() {
        return "JspCServletContext/1.0";
    }

    /**
     * Return a null reference for the specified servlet name.
     *
     * @param name Name of the requested servlet
     * @deprecated This method has been deprecated with no replacement
     */
    @Override
    @Deprecated
    public Servlet getServlet(String name) {
        return null;
    }

    /**
     * Return the name of this servlet context.
     */
    @Override
    public String getServletContextName() {
        return getServerInfo();
    }

    /**
     * Return "/" as the context path for compilation.
     */
    @Override
    public String getContextPath() {
        return "/";
    }

    /**
     * Return an empty enumeration of servlet names.
     *
     * @deprecated This method has been deprecated with no replacement
     */
    @Override
    @Deprecated
    public Enumeration<String> getServletNames() {
        return new Vector<String>().elements();
    }

    /**
     * Return an empty enumeration of servlets.
     *
     * @deprecated This method has been deprecated with no replacement
     */
    @Override
    @Deprecated
    public Enumeration<Servlet> getServlets() {
        return new Vector<Servlet>().elements();
    }

    /**
     * Log the specified message.
     *
     * @param message The message to be logged
     */
    @Override
    public void log(String message) {
        log.info(message);
    }

    /**
     * Log the specified message and exception.
     *
     * @param exception The exception to be logged
     * @param message The message to be logged
     * @deprecated Use log(String,Throwable) instead
     */
    @Override
    @Deprecated
    public void log(Exception exception, String message) {
        this.log(message, exception);
    }

    /**
     * Log the specified message and exception.
     *
     * @param message The message to be logged
     * @param exception The exception to be logged
     */
    @Override
    public void log(String message, Throwable exception) {
        log.error(message, exception);
    }

    /**
     * Remove the specified context attribute.
     *
     * @param name Name of the attribute to remove
     */
    @Override
    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    /**
     * Set or replace the specified context attribute.
     *
     * @param name Name of the context attribute to set
     * @param value Corresponding attribute value
     */
    @Override
    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    @Override
    public int getEffectiveMajorVersion() {
        return this.getMinorVersion();
    }

    @Override
    public int getEffectiveMinorVersion() {
        return this.getMajorVersion();
    }

    @Override
    public boolean setInitParameter(String name, String value) {
        throw new IllegalStateException();
    }

    @Override
    public Dynamic addServlet(String servletName, String className) {
        throw new IllegalStateException();
    }

    @Override
    public Dynamic addServlet(String servletName, Servlet servlet) {
        throw new IllegalStateException();
    }

    @Override
    public Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass) {
        throw new IllegalStateException();
    }

    @Override
    public <T extends Servlet> T createServlet(Class<T> clazz) throws ServletException {
        throw new IllegalStateException();
    }

    @Override
    public ServletRegistration getServletRegistration(String servletName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public javax.servlet.FilterRegistration.Dynamic addFilter(String filterName, String className) {
        throw new IllegalStateException();
    }

    @Override
    public javax.servlet.FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
        throw new IllegalStateException();
    }

    @Override
    public javax.servlet.FilterRegistration.Dynamic addFilter(String filterName, Class<? extends Filter> filterClass) {
        throw new IllegalStateException();
    }

    @Override
    public <T extends Filter> T createFilter(Class<T> clazz) throws ServletException {
        throw new IllegalStateException();
    }

    @Override
    public FilterRegistration getFilterRegistration(String filterName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SessionCookieConfig getSessionCookieConfig() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {
        // TODO Auto-generated method stub

    }

    @Override
    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addListener(String className) {
        throw new IllegalStateException();
    }

    @Override
    public <T extends EventListener> void addListener(T t) {
        throw new IllegalStateException();
    }

    @Override
    public void addListener(Class<? extends EventListener> listenerClass) {
        throw new IllegalStateException();
    }

    @Override
    public <T extends EventListener> T createListener(Class<T> clazz) throws ServletException {
        throw new IllegalStateException();
    }

    @Override
    public JspConfigDescriptor getJspConfigDescriptor() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ClassLoader getClassLoader() {
        // TODO Auto-generated method stub
        return this.getClass().getClassLoader();
    }

    @Override
    public void declareRoles(String... roleNames) {
        throw new IllegalStateException();
    }

    @Override
    public String getVirtualServerName() {
        // TODO Auto-generated method stub
        return null;
    }
}
