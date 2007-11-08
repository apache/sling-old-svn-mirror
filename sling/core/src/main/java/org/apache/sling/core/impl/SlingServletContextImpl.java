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
package org.apache.sling.core.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.core.impl.helper.RequestData;
import org.apache.sling.core.impl.resolver.ResolvedURLImpl;
import org.apache.sling.core.resolver.ContentResolver;
import org.osgi.service.component.ComponentException;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>SlingServletContextImpl</code> TODO
 */
public class SlingServletContextImpl implements ServletContext {

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    private SlingMainServlet requestHandler;

    private ServiceTracker contentResolver;

    SlingServletContextImpl(SlingMainServlet requestHandler) {
        this.requestHandler = requestHandler;

        this.contentResolver = new ServiceTracker(
            requestHandler.getBundleContext(), ContentResolver.class.getName(),
            null);
        this.contentResolver.open();
    }

    void dispose() {
        if (contentResolver != null) {
            contentResolver.close();
        }
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

    public RequestDispatcher getRequestDispatcher(Resource resource) {
        // return no dispatcher if content is null
        if (resource == null) {
            log.error("getRequestDispatcher: No content, cannot create request dispatcher");
            return null;
        }

        RequestPathInfo resolvedURL = new ResolvedURLImpl(resource.getURI());
        return new ComponentRequestDispatcherImpl(resource, resolvedURL);
    }

    public RequestDispatcher getRequestDispatcher(String path) {
        // return no dispatcher if content is null
        if (path == null) {
            log.error("getRequestDispatcher: No path, cannot create request dispatcher");
            return null;
        }

        return new ComponentRequestDispatcherImpl(path);
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

    // ---------- Inner class --------------------------------------------------

    private class ComponentRequestDispatcherImpl implements
            RequestDispatcher {

        private RequestPathInfo resolvedURL;

        private final String path;

        private ComponentRequestDispatcherImpl(Resource resource, RequestPathInfo resolvedURL) {
            this.resolvedURL = resolvedURL;
            this.path = resolvedURL.getResourcePath();
        }

        private ComponentRequestDispatcherImpl(String path) {
            this.resolvedURL = null;
            this.path = path;
        }

        public void include(ServletRequest request, ServletResponse response)
                throws ServletException, IOException {

            if (resolvedURL == null) {

                // this may throw an exception in case loading fails, which is
                // ok here, if no content is available at that path null is
                // return, which results in using the servlet container
                SlingHttpServletRequest cRequest = RequestData.unwrap(request);
                String absPath = getAbsolutePath(cRequest, path);

                ContentResolver cr = (ContentResolver) contentResolver.getService();
                ContentManager cm = RequestData.getRequestData(cRequest).getContentManager();
                if (cr != null && cm != null) {
                    resolvedURL = cr.resolveURL(cm, absPath);
                }
            }

            if (resolvedURL != null) {
                requestHandler.includeContent(request, response, resolvedURL);
            } else {
                requestHandler.includeServlet(request, response, path);
            }
        }

        public void forward(ServletRequest request, ServletResponse response)
                throws ServletException, IOException {
            // TODO Auto-generated method stub
            // TODO, use servlet container dispatcher !!
        }

        private String getAbsolutePath(SlingHttpServletRequest request, String path) {
            // path is already absolute
            if (path.startsWith("/")) {
                return path;
            }

            // get parent of current request
            String uri = request.getResource().getURI();
            int lastSlash = uri.lastIndexOf('/');
            if (lastSlash >= 0) {
                uri = uri.substring(0, lastSlash);
            }

            // append relative path to parent
            return uri + '/' + path;
        }

    }

}
