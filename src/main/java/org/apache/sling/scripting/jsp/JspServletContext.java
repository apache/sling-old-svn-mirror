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
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

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

    public Set<?> getResourcePaths(String path) {
        return ioProvider.getResourcePaths(path);
    }

    public void log(String msg) {
        log.info(msg);
    }

    @Deprecated
    public void log(Exception exception, String msg) {
        log(msg, exception);
    }

    public void log(String message, Throwable throwable) {
        log.error(message, throwable);
    }

    //---------- delegated methods --------------------------------------------

    public Object getAttribute(String name) {
        return delegatee.getAttribute(name);
    }

    public Enumeration<?> getAttributeNames() {
        return delegatee.getAttributeNames();
    }

    public void removeAttribute(String name) {
        delegatee.removeAttribute(name);
    }

    public void setAttribute(String name, Object object) {
        delegatee.setAttribute(name, object);
    }

    public ServletContext getContext(String uripath) {
        return delegatee.getContext(uripath);
    }

    public String getInitParameter(String name) {
        return delegatee.getInitParameter(name);
    }

    public Enumeration<?> getInitParameterNames() {
        return delegatee.getInitParameterNames();
    }

    public int getMajorVersion() {
        return delegatee.getMajorVersion();
    }

    public String getMimeType(String file) {
        return delegatee.getMimeType(file);
    }

    public int getMinorVersion() {
        return delegatee.getMinorVersion();
    }

    public RequestDispatcher getNamedDispatcher(String name) {
        return delegatee.getNamedDispatcher(name);
    }

    public String getRealPath(String path) {
        return delegatee.getRealPath(path);
    }

    public RequestDispatcher getRequestDispatcher(String path) {
        return delegatee.getRequestDispatcher(path);
    }

    public String getServerInfo() {
        return delegatee.getServerInfo();
    }

    @Deprecated
    public Servlet getServlet(String name) throws ServletException {
        return delegatee.getServlet(name);
    }

    public String getServletContextName() {
        return delegatee.getServletContextName();
    }

    @Deprecated
    public Enumeration<?> getServletNames() {
        return delegatee.getServletNames();
    }

    @Deprecated
    public Enumeration<?> getServlets() {
        return delegatee.getServlets();
    }

    // Servlet API 2.5 method
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
}
