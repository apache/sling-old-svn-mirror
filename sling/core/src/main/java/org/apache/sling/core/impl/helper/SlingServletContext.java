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
import java.util.Enumeration;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;

import org.apache.sling.core.impl.SlingMainServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>SlingServletContext</code> TODO
 */
public class SlingServletContext implements ServletContext {

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    SlingMainServlet requestHandler;

    public SlingServletContext(SlingMainServlet requestHandler) {
        this.requestHandler = requestHandler;
    }

    /**
     * @see javax.servlet.ServletContext#getAttribute(java.lang.String)
     */
    public Object getAttribute(String name) {
        return getServletContext().getAttribute(name);
    }

    /**
     * @see javax.servlet.ServletContext#getAttributeNames()
     */
    @SuppressWarnings("unchecked")
    public Enumeration<String> getAttributeNames() {
        return getServletContext().getAttributeNames();
    }

    /**
     * @see javax.servlet.ServletContext#getInitParameter(java.lang.String)
     */
    public String getInitParameter(String name) {
        return getServletContext().getInitParameter(name);
    }

    /**
     * @see javax.servlet.ServletContext#getInitParameterNames()
     */
    @SuppressWarnings("unchecked")
    public Enumeration<String> getInitParameterNames() {
        return getServletContext().getInitParameterNames();
    }

    /**
     * @see javax.servlet.ServletContext#getMajorVersion()
     */
    public int getMajorVersion() {
        return getServletContext().getMajorVersion();
    }

    /**
     * @see javax.servlet.ServletContext#getMimeType(java.lang.String)
     */
    public String getMimeType(String file) {
        return getServletContext().getMimeType(file);
    }

    /**
     * @see javax.servlet.ServletContext#getMinorVersion()
     */
    public int getMinorVersion() {
        return getServletContext().getMinorVersion();
    }

    public RequestDispatcher getRequestDispatcher(String path) {
        // return no dispatcher if content is null
        if (path == null) {
            log.error("getRequestDispatcher: No path, cannot create request dispatcher");
            return null;
        }

        return new SlingRequestDispatcher(path);
    }

    public URL getResource(String path) throws MalformedURLException {
        // TODO Auto-generated method stub
        return null;
    }

    public InputStream getResourceAsStream(String path) {
        // TODO Auto-generated method stub
        return null;
    }

    public Set getResourcePaths(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.ServletContext#getServerInfo()
     */
    public String getServerInfo() {
        return requestHandler.getServerInfo();
    }

    public void log(String message, Throwable throwable) {
        log.error(message, throwable);
    }

    public void log(String msg) {
        log.info(msg);
    }

    public void log(Exception exception, String msg) {
        log(msg, exception);
    }

    /**
     * @see javax.servlet.ServletContext#removeAttribute(java.lang.String)
     */
    public void removeAttribute(String name) {
        getServletContext().removeAttribute(name);
    }

    /**
     * @see javax.servlet.ServletContext#setAttribute(java.lang.String,
     *      java.lang.Object)
     */
    public void setAttribute(String name, Object object) {
        getServletContext().removeAttribute(name);
    }

    private ServletContext getServletContext() {
        return requestHandler.getServletContext();
    }

    public ServletContext getContext(String uripath) {
        // check whether to return ComponentContext ??
        return getServletContext().getContext(uripath);
    }

    public RequestDispatcher getNamedDispatcher(String name) {
        return getServletContext().getNamedDispatcher(name);
    }

    public String getRealPath(String path) {
        return getServletContext().getRealPath(path);
    }

    public String getServletContextName() {
        return getServletContext().getServletContextName();
    }

    @Deprecated
    public Servlet getServlet(String name) {
        return null;
    }

    @Deprecated
    public Enumeration<?> getServletNames() {
        return null;
    }

    @Deprecated
    public Enumeration<?> getServlets() {
        return null;
    }

}
