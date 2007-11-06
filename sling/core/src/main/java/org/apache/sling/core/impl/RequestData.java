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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.Locale;

import javax.jcr.Session;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.ServletResponse;
import javax.servlet.ServletResponseWrapper;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.component.ComponentException;
import org.apache.sling.component.ComponentRequest;
import org.apache.sling.component.ComponentRequestWrapper;
import org.apache.sling.component.ComponentResponse;
import org.apache.sling.component.ComponentResponseWrapper;
import org.apache.sling.content.ContentManager;
import org.apache.sling.core.Constants;
import org.apache.sling.core.impl.output.BufferProvider;
import org.apache.sling.core.impl.parameters.ParameterSupport;
import org.apache.sling.core.impl.resolver.ResolvedURLImpl;
import org.apache.sling.core.resolver.ResolvedURL;
import org.apache.sling.core.theme.Theme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The <code>RequestData</code> class provides access to objects which are set
 * on a Servlet Request wide basis such as the repository session, the
 * persistence manager, etc.
 *
 * @see ContentData
 */
public class RequestData implements BufferProvider {

    /** default log */
    private final Logger log = LoggerFactory.getLogger(RequestData.class);

    /** The original servlet Servlet Request Object */
    private HttpServletRequest servletRequest;

    /** The parameter support class */
    private ParameterSupport parameterSupport;

    /** The original servlet Servlet Response object */
    private HttpServletResponse servletResponse;

    /**
     * <code>true</code> if the servlet is
     * <code>RequestDispatcher.include()</code>-ed
     */
    private boolean included;

    /**
     * The prepared request URI. This URI is either the URI from the HTTP
     * request line or the request URI from the
     * <code>javax.servlet.include.request_uri</code> request attribute with
     * the context path removed.
     */
    private String requestURI;

    /** Caches the real context path returned by {@link #getRealContextPath()} */
    private String contextPath;

    /** Caches the real servlet path returned by {@link #getRealServletPath()} */
    private String servletPath;

    /** Caches the real path info returned by {@link #getRealPathInfo()} */
    private String pathInfo;

    /** Caches the real query string returned by {@link #getRealQueryString()} */
    private String queryString;

    /** Caches the real method name returned by {@link #getRealMethod()} */
    private String method;

    private Session session;

    private ContentManager contentManager;

    private Locale locale;

    private Theme theme;

    /** the current ContentData */
    private ContentData currentContentData;

    /** the stack of ContentData objects */
    private LinkedList<ContentData> contentDataStack;

    public RequestData(HttpServletRequest request, HttpServletResponse response) {
        this.servletRequest = request;
        this.servletResponse = response;

        // some more preparation
        this.included = request.getAttribute(Constants.INCLUDE_REQUEST_URI) != null;
    }

    /* package */void dispose() {
        // make sure our request attributes do not exist anymore
        this.servletRequest.removeAttribute(Constants.ATTR_REQUEST_CONTENT);
        this.servletRequest.removeAttribute(Constants.ATTR_REQUEST_COMPONENT);
        this.servletRequest.removeAttribute(Constants.ATTR_CONTENT_MANAGER);

        // clear the content data stack
        if (this.contentDataStack != null) {
            while (!this.contentDataStack.isEmpty()) {
                ContentData cd = this.contentDataStack.removeLast();
                cd.dispose();
            }
        }

        // dispose current content data, if any
        if (this.currentContentData != null) {
            this.currentContentData.dispose();
        }

        // logout the session
        if (this.session != null) {
            this.session.logout();
        }

        // clear fields
        this.contentDataStack = null;
        this.currentContentData = null;
        this.servletRequest = null;
        this.servletResponse = null;
        this.contentManager = null;
        this.session = null;
    }

    public HttpServletRequest getServletRequest() {
        return this.servletRequest;
    }

    public HttpServletResponse getServletResponse() {
        return this.servletResponse;
    }

    //---------- Request Helper

    /**
     * Unwraps the ServletRequest to a ComponentRequest.
     */
    public static ComponentRequest unwrap(ServletRequest request) throws ComponentException {

        // early check for most cases
        if (request instanceof ComponentRequest) {
            return (ComponentRequest) request;
        }

        // unwrap wrappers
        while (request instanceof ServletRequestWrapper) {
            request = ((ServletRequestWrapper) request).getRequest();

            // immediate termination if we found one
            if (request instanceof ComponentRequest) {
                return (ComponentRequest) request;
            }
        }

        // if we unwrapped everything and did not find a ComponentRequest, we lost
        throw new ComponentException("ServletRequest not wrapping ComponentRequest");
    }

    /**
     * Unwraps the ComponentRequest to a ComponentRequestImpl
     * @param request
     * @return
     * @throws ComponentException
     */
    public static ComponentRequestImpl unwrap(ComponentRequest request) throws ComponentException {
        while (request instanceof ComponentRequestWrapper) {
            request = ((ComponentRequestWrapper) request).getComponentRequest();
        }

        if (request instanceof ComponentRequestImpl) {
            return (ComponentRequestImpl) request;
        }

        throw new ComponentException("ComponentRequest not of correct type");
    }

    /**
     * Unwraps the ServletRequest to a ComponentRequest.
     */
    public static ComponentResponse unwrap(ServletResponse response) throws ComponentException {

        // early check for most cases
        if (response instanceof ComponentResponse) {
            return (ComponentResponse) response;
        }

        // unwrap wrappers
        while (response instanceof ServletResponseWrapper) {
            response = ((ServletResponseWrapper) response).getResponse();

            // immediate termination if we found one
            if (response instanceof ComponentResponse) {
                return (ComponentResponse) response;
            }
        }

        // if we unwrapped everything and did not find a ComponentResponse, we lost
        throw new ComponentException("ServletResponse not wrapping ComponentResponse");
    }

    /**
     * Unwraps a ComponentResponse to a ComponentResponseImpl
     * @param response
     * @return
     * @throws ComponentException
     */
    public static ComponentResponseImpl unwrap(ComponentResponse response) throws ComponentException {
        while (response instanceof ComponentResponseWrapper) {
            response = ((ComponentResponseWrapper) response).getComponentResponse();
        }

        if (response instanceof ComponentResponseImpl) {
            return (ComponentResponseImpl) response;
        }

        throw new ComponentException("ComponentResponse not of correct type");
    }

    public static RequestData getRequestData(ComponentRequest request) throws ComponentException {
        return unwrap(request).getRequestData();
    }

    public static RequestData getRequestData(ServletRequest request) throws ComponentException {
        return unwrap(unwrap(request)).getRequestData();
    }

    public static ComponentRequest toComponentRequest(ServletRequest request) throws ComponentException {
        // unwrap to ComponentRequest
        ComponentRequest cRequest = unwrap(request);

        // check type of response, don't care actually for the response itself
        RequestData.unwrap(cRequest);

        // if the servlet response is actually the ComponentResponse, we are done
        if (cRequest == request) {
            return cRequest;
        }

        // ensure the request is a HTTP request
        if (!(request instanceof HttpServletRequest)) {
            throw new ComponentException("Request is not an HTTP request");
        }

        // otherwise, we create a new response wrapping the servlet response
        // and unwrapped component response
        return new ComponentServletRequestWrapper(cRequest, (HttpServletRequest) request);
    }

    public static ComponentResponse toComponentResponse(ServletResponse response) throws ComponentException {
        // unwrap to ComponentResponse
        ComponentResponse cResponse = unwrap(response);

        // check type of response, don't care actually for the response itself
        RequestData.unwrap(cResponse);

        // if the servlet response is actually the ComponentResponse, we are done
        if (cResponse == response) {
            return cResponse;
        }

        // ensure the response is a HTTP response
        if (!(response instanceof HttpServletResponse)) {
            throw new ComponentException("Response is not an HTTP response");
        }

        // otherwise, we create a new response wrapping the servlet response
        // and unwrapped component response
        return null;
    }

    // ---------- Content inclusion stacking -----------------------------------

    public void pushContent(ResolvedURL resolvedURL) {
        BufferProvider parent;
        if (this.currentContentData != null) {
            if (this.contentDataStack == null) {
                this.contentDataStack = new LinkedList<ContentData>();
            }

            // ensure the selectors, extension and suffix are inherited
            // from the parent if none have been declared on inclusion
            if (resolvedURL.getExtension() == null || resolvedURL.getExtension().length() == 0) {
                ResolvedURLImpl copy = new ResolvedURLImpl(resolvedURL);
                copy.setSelectorString(currentContentData.getSelectorString());
                copy.setExtension(currentContentData.getExtension());
                copy.setSuffix(currentContentData.getSuffix());
                resolvedURL = copy;
            }

            // remove the request attributes if the stack is empty now
            this.servletRequest.setAttribute(Constants.ATTR_REQUEST_CONTENT,
                this.currentContentData.getContent());
            this.servletRequest.setAttribute(Constants.ATTR_REQUEST_COMPONENT,
                this.currentContentData.getComponent());

            this.contentDataStack.add(this.currentContentData);
            parent = this.currentContentData;
        } else {
            parent = this;
        }

        this.currentContentData = new ContentData(resolvedURL, parent);
    }

    public void popContent() {
        // dispose current content data before replacing it
        if (this.currentContentData != null) {
            this.currentContentData.dispose();
        }

        if (this.contentDataStack != null && !this.contentDataStack.isEmpty()) {
            // remove the topmost content data object
            this.currentContentData = this.contentDataStack.removeLast();

            // remove the request attributes if the stack is empty now
            if (this.contentDataStack.isEmpty()) {
                this.servletRequest.removeAttribute(Constants.ATTR_REQUEST_COMPONENT);
                this.servletRequest.removeAttribute(Constants.ATTR_REQUEST_CONTENT);
            }

        } else {
            this.currentContentData = null;
        }
    }

    public ContentData getContentData() {
        return this.currentContentData;
    }

    /**
     * Returns <code>true</code> if request processing is currently processing
     * a component which has been included by
     * <code>ComponentRequestDispatcher.include</code>.
     */
    public boolean isContentIncluded() {
        return this.contentDataStack != null && !this.contentDataStack.isEmpty();
    }

    // ---------- parameters differing in included servlets --------------------

    /**
     * Returns <code>true</code> if the servlet is executed through
     * <code>RequestDispatcher.include()</code>.
     *
     * @return <code>true</code> if the servlet is executed through
     *         <code>RequestDispatcher.include()</code>.
     */
    public boolean isIncluded() {
        return this.included;
    }

    /**
     * Returns the contents of the
     * <code>javax.servlet.include.request_uri</code> attribute if
     * {@link #isIncluded()} or <code>request.getRequestURI()</code>. The
     * context path has been removed from the beginning of the returned string.
     * That is for request, which is not {@link #isIncluded() included}:
     * <code>getRealRequestURI() == getRealContextPath() + getRequestURI()</code>.
     *
     * @return The relevant request URI according to environment with the
     *         context path removed.
     */
    public String getRequestURI() {
        if (this.requestURI == null) {

            // get the unmodified request URI and context information
            this.requestURI = this.included
                    ? (String) this.servletRequest.getAttribute(Constants.INCLUDE_REQUEST_URI)
                    : this.servletRequest.getRequestURI();

            String ctxPrefix = this.getContextPath();

            if (log.isDebugEnabled()) {
                log.debug("getRequestURI: Servlet request URI is {}",
                    this.requestURI);
            }

            // check to remove the context prefix
            if (ctxPrefix == null) {
                log.error("getRequestURI: Context path not expected to be null");
            } else if (ctxPrefix.length() == 0) {
                // default root context, no change
                if (log.isDebugEnabled()) {
                    log.debug("getRequestURI: Default root context, no change to uri");
                }
            } else if (ctxPrefix.length() < this.requestURI.length()
                && this.requestURI.startsWith(ctxPrefix)
                && this.requestURI.charAt(ctxPrefix.length()) == '/') {
                // some path below context root
                if (log.isDebugEnabled()) {
                    log.debug("getRequestURI: removing '{}' from '{}'",
                        ctxPrefix, this.requestURI);
                }
                this.requestURI = this.requestURI.substring(ctxPrefix.length());
            } else if (ctxPrefix.equals(this.requestURI)) {
                // context root
                if (log.isDebugEnabled()) {
                    log.debug("getRequestURI: URI equals context prefix, assuming '/'");
                }
                this.requestURI = "/";
            }
        }

        return this.requestURI;
    }

    /**
     * Returns the contents of the
     * <code>javax.servlet.include.context_path</code> attribute if
     * {@link #isIncluded()} or <code>request.getContextPath()</code>.
     *
     * @return The relevant context path according to environment.
     */
    public String getContextPath() {
        if (this.contextPath == null) {
            this.contextPath = this.included
                    ? (String) this.servletRequest.getAttribute(Constants.INCLUDE_CONTEXT_PATH)
                    : this.servletRequest.getContextPath();
        }

        return this.contextPath;
    }

    /**
     * Returns the contents of the
     * <code>javax.servlet.include.servlet_path</code> attribute if
     * {@link #isIncluded()} or <code>request.getServletPath()</code>.
     * <p>
     * <strong>NOTE</strong>: This is the path to the servlet being executed
     * from the perspective of the servlet container. Thus this path is really
     * the path to the {@link DeliveryServlet}.
     *
     * @return The relevant servlet path according to environment.
     */
    public String getServletPath() {
        if (this.servletPath == null) {
            this.servletPath = this.included
                    ? (String) this.servletRequest.getAttribute(Constants.INCLUDE_SERVLET_PATH)
                    : this.servletRequest.getServletPath();
        }

        return this.servletPath;
    }

    /**
     * Returns the contents of the <code>javax.servlet.include.path_info</code>
     * attribute if {@link #isIncluded()} or <code>request.getPathInfo()</code>.
     * <p>
     * <strong>NOTE</strong>: This is the additional path info extending the
     * servlet path from the perspective of the servlet container. This is not
     * the same as the {@link #getSuffix() suffix}.
     *
     * @return The relevant path info according to environment.
     */
    public String getPathInfo() {
        if (this.pathInfo == null) {
            this.pathInfo = this.included
                    ? (String) this.servletRequest.getAttribute(Constants.INCLUDE_PATH_INFO)
                    : this.servletRequest.getPathInfo();
        }

        return this.pathInfo;
    }

    /**
     * Returns the contents of the
     * <code>javax.servlet.include.query_string</code> attribute if
     * {@link #isIncluded()} or <code>request.getQueryString()</code>.
     *
     * @return The relevant query string according to environment.
     */
    public String getQueryString() {
        if (this.queryString == null) {
            this.queryString = this.included
                    ? (String) this.servletRequest.getAttribute(Constants.INCLUDE_QUERY_STRING)
                    : this.servletRequest.getQueryString();
        }

        return this.queryString;
    }

    /**
     * @return the locale
     */
    public Locale getLocale() {
        return this.locale;
    }

    /**
     * @param locale the locale to set
     */
    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    /**
     * @return the persistenceManager
     */
    public ContentManager getContentManager() {
        return this.contentManager;
    }

    /**
     * @param persistenceManager the persistenceManager to set
     */
    public void setContentManager(ContentManager contentManager) {
        this.contentManager = contentManager;

        // provide the content manager to components as request attribute
        this.servletRequest.setAttribute(Constants.ATTR_CONTENT_MANAGER,
            contentManager);
    }

    /**
     * @return the session
     */
    public Session getSession() {
        return this.session;
    }

    /**
     * @param session the session to set
     */
    public void setSession(Session session) {
        this.session = session;
    }

    /**
     * @return the theme
     */
    public Theme getTheme() {
        return this.theme;
    }

    /**
     * @param theme the theme to set
     */
    public void setTheme(Theme theme) {
        this.theme = theme;
        // provide the current theme to components as a request attribute
        // TODO - We should define a well known constant for this
        this.servletRequest.setAttribute(Theme.class.getName(), theme);
    }

    // ---------- BufferProvider -----------------------------------------

    public BufferProvider getBufferProvider() {
        return (this.currentContentData != null)
                ? (BufferProvider) this.currentContentData
                : this;
    }

    public ServletOutputStream getOutputStream() throws IOException {
        return this.getServletResponse().getOutputStream();
    }

    public PrintWriter getWriter() throws IOException {
        return this.getServletResponse().getWriter();
    }

    // ---------- Parameter support -------------------------------------------

    ServletInputStream getInputStream() throws IOException {
        if (this.parameterSupport != null && this.parameterSupport.requestDataUsed()) {
            throw new IllegalStateException("Request Data has already been read");
        }

        // may throw IllegalStateException if the reader has already been acquired
        return this.getServletRequest().getInputStream();
    }

    BufferedReader getReader() throws UnsupportedEncodingException, IOException {
        if (this.parameterSupport != null && this.parameterSupport.requestDataUsed()) {
            throw new IllegalStateException("Request Data has already been read");
        }

        // may throw IllegalStateException if the input stream has already been acquired
        return this.getServletRequest().getReader();
    }

    ParameterSupport getParameterSupport() {
        if (this.parameterSupport == null) {
            this.parameterSupport = new ParameterSupport(this /* getServletRequest() */);
        }

        return this.parameterSupport;
    }
}
