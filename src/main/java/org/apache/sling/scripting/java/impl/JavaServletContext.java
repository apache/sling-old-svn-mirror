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
package org.apache.sling.scripting.java.impl;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>JavaServletContext</code>
 * is passed to the compiled servlets.
 */
public class JavaServletContext implements ServletContext {

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final SlingIOProvider ioProvider;
    private final ServletContext delegatee;

    JavaServletContext(SlingIOProvider ioProvider, ServletContext componentContext) {
        this.ioProvider = ioProvider;
        this.delegatee = componentContext;
    }

    /**
     * @see javax.servlet.ServletContext#getResource(java.lang.String)
     */
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

    /**
     * @see javax.servlet.ServletContext#getResourceAsStream(java.lang.String)
     */
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

    /**
     * @see javax.servlet.ServletContext#getResourcePaths(java.lang.String)
     */
    public Set<?> getResourcePaths(String path) {
        return ioProvider.getResourcePaths(path);
    }

    /**
     * @see javax.servlet.ServletContext#log(java.lang.String)
     */
    public void log(String msg) {
        log.info(msg);
    }

    /**
     * @see javax.servlet.ServletContext#log(java.lang.Exception, java.lang.String)
     */
    @Deprecated
    public void log(Exception exception, String msg) {
        log(msg, exception);
    }

    /**
     * @see javax.servlet.ServletContext#log(java.lang.String, java.lang.Throwable)
     */
    public void log(String message, Throwable throwable) {
        log.error(message, throwable);
    }

    /**
     * @see javax.servlet.ServletContext#getAttribute(java.lang.String)
     */
    public Object getAttribute(String name) {
        return delegatee.getAttribute(name);
    }

    /**
     * @see javax.servlet.ServletContext#getAttributeNames()
     */
    public Enumeration<?> getAttributeNames() {
        return delegatee.getAttributeNames();
    }

    /**
     * @see javax.servlet.ServletContext#removeAttribute(java.lang.String)
     */
    public void removeAttribute(String name) {
        delegatee.removeAttribute(name);
    }

    /**
     * @see javax.servlet.ServletContext#setAttribute(java.lang.String, java.lang.Object)
     */
    public void setAttribute(String name, Object object) {
        delegatee.setAttribute(name, object);
    }

    /**
     * @see javax.servlet.ServletContext#getContext(java.lang.String)
     */
    public ServletContext getContext(String uripath) {
        return delegatee.getContext(uripath);
    }

    /**
     * @see javax.servlet.ServletContext#getInitParameter(java.lang.String)
     */
    public String getInitParameter(String name) {
        return delegatee.getInitParameter(name);
    }

    /**
     * @see javax.servlet.ServletContext#getInitParameterNames()
     */
    public Enumeration<?> getInitParameterNames() {
        return delegatee.getInitParameterNames();
    }

    /**
     * @see javax.servlet.ServletContext#getMajorVersion()
     */
    public int getMajorVersion() {
        return delegatee.getMajorVersion();
    }

    /**
     * @see javax.servlet.ServletContext#getMimeType(java.lang.String)
     */
    public String getMimeType(String file) {
        return delegatee.getMimeType(file);
    }

    /**
     * @see javax.servlet.ServletContext#getMinorVersion()
     */
    public int getMinorVersion() {
        return delegatee.getMinorVersion();
    }

    /**
     * @see javax.servlet.ServletContext#getNamedDispatcher(java.lang.String)
     */
    public RequestDispatcher getNamedDispatcher(String name) {
        return delegatee.getNamedDispatcher(name);
    }

    /**
     * @see javax.servlet.ServletContext#getRealPath(java.lang.String)
     */
    public String getRealPath(String path) {
        return delegatee.getRealPath(path);
    }

    /**
     * @see javax.servlet.ServletContext#getRequestDispatcher(java.lang.String)
     */
    public RequestDispatcher getRequestDispatcher(String path) {
        return delegatee.getRequestDispatcher(path);
    }

    /**
     * @see javax.servlet.ServletContext#getServerInfo()
     */
    public String getServerInfo() {
        return delegatee.getServerInfo();
    }

    /**
     * @see javax.servlet.ServletContext#getServlet(java.lang.String)
     */
    @Deprecated
    public Servlet getServlet(String name) throws ServletException {
        return delegatee.getServlet(name);
    }

    /**
     * @see javax.servlet.ServletContext#getServletContextName()
     */
    public String getServletContextName() {
        return delegatee.getServletContextName();
    }

    /**
     * @see javax.servlet.ServletContext#getServletNames()
     */
    @Deprecated
    public Enumeration<?> getServletNames() {
        return delegatee.getServletNames();
    }

    /**
     * @see javax.servlet.ServletContext#getServlets()
     */
    @Deprecated
    public Enumeration<?> getServlets() {
        return delegatee.getServlets();
    }

    /**
     * @see javax.servlet.ServletContext#getContextPath()
     */
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
}
