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
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;

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
    protected Hashtable<String, Object> attributes;

    /**
     * The log writer we will write log messages to.
     */
    protected Log log;

    /**
     * The base URL (document root) for this context.
     */
    protected URL resourceBaseURL;

    /**
     * Create a new instance of this ServletContext implementation.
     *
     * @param log The Log which is used for <code>log()</code> calls
     * @param resourceBaseURL Resource base URL
     */
    public JspCServletContext(Log log, URL resourceBaseURL) {
        attributes = new Hashtable<String, Object>();
        this.log = log;
        this.resourceBaseURL = resourceBaseURL;
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Return the specified context attribute, if any.
     *
     * @param name Name of the requested attribute
     */
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    /**
     * Return an enumeration of context attribute names.
     */
    public Enumeration<String> getAttributeNames() {
        return attributes.keys();
    }

    /**
     * Return the servlet context for the specified path.
     *
     * @param uripath Server-relative path starting with '/'
     */
    public ServletContext getContext(String uripath) {
        return null;
    }

    /**
     * Return the specified context initialization parameter.
     *
     * @param name Name of the requested parameter
     */
    public String getInitParameter(String name) {
        return null;
    }

    /**
     * Return an enumeration of the names of context initialization parameters.
     */
    public Enumeration<String> getInitParameterNames() {
        return new Vector<String>().elements();
    }

    /**
     * Return the Servlet API major version number.
     */
    public int getMajorVersion() {
        return 2;
    }

    /**
     * Return the MIME type for the specified filename.
     *
     * @param file Filename whose MIME type is requested
     */
    public String getMimeType(String file) {
        return null;
    }

    /**
     * Return the Servlet API minor version number.
     */
    public int getMinorVersion() {
        return 3;
    }

    /**
     * Return a request dispatcher for the specified servlet name.
     *
     * @param name Name of the requested servlet
     */
    public RequestDispatcher getNamedDispatcher(String name) {
        return null;
    }

    /**
     * Return the real path for the specified context-relative virtual path.
     *
     * @param path The context-relative virtual path to resolve
     */
    public String getRealPath(String path) {
        if (!resourceBaseURL.getProtocol().equals("file")) {
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
    public URL getResource(String path) throws MalformedURLException {

        // catch for dummy web.xml
        if (WEB_XML.equals(path)) {
            return this.getClass().getResource("web.xml");
        }

        if (!path.startsWith("/")) {
            throw new MalformedURLException("Path '" + path
                + "' does not start with '/'");
        }

        URL url = new URL(resourceBaseURL, path.substring(1));
        InputStream is = null;
        try {
            is = url.openStream();
        } catch (Throwable t) {
            url = null;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Throwable t2) {
                    // Ignore
                }
            }
        }
        return url;
    }

    /**
     * Return an InputStream allowing access to the resource at the specified
     * context-relative path.
     *
     * @param path Context-relative path of the desired resource
     */
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
    public Set<String> getResourcePaths(String path) {
        Set<String> thePaths = new HashSet<String>();

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

        String theFiles[] = theBaseDir.list();
        for (int i = 0; i < theFiles.length; i++) {
            File testFile = new File(basePath + File.separator + theFiles[i]);
            if (testFile.isFile()) {
                thePaths.add(path + theFiles[i]);
            } else if (testFile.isDirectory()) {
                thePaths.add(path + theFiles[i] + "/");
            }
        }

        return thePaths;
    }

    /**
     * Return descriptive information about this server.
     */
    public String getServerInfo() {
        return "JspCServletContext/1.0";
    }

    /**
     * Return a null reference for the specified servlet name.
     *
     * @param name Name of the requested servlet
     * @deprecated This method has been deprecated with no replacement
     */
    @Deprecated
    public Servlet getServlet(String name) {
        return null;
    }

    /**
     * Return the name of this servlet context.
     */
    public String getServletContextName() {
        return getServerInfo();
    }

    /**
     * Return "/" as the context path for compilation.
     */
    public String getContextPath() {
        return "/";
    }

    /**
     * Return an empty enumeration of servlet names.
     *
     * @deprecated This method has been deprecated with no replacement
     */
    @Deprecated
    public Enumeration<String> getServletNames() {
        return new Vector<String>().elements();
    }

    /**
     * Return an empty enumeration of servlets.
     *
     * @deprecated This method has been deprecated with no replacement
     */
    @Deprecated
    public Enumeration<Servlet> getServlets() {
        return new Vector<Servlet>().elements();
    }

    /**
     * Log the specified message.
     *
     * @param message The message to be logged
     */
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
    public void log(String message, Throwable exception) {
        log.error(message, exception);
    }

    /**
     * Remove the specified context attribute.
     *
     * @param name Name of the attribute to remove
     */
    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    /**
     * Set or replace the specified context attribute.
     *
     * @param name Name of the context attribute to set
     * @param value Corresponding attribute value
     */
    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }
}
