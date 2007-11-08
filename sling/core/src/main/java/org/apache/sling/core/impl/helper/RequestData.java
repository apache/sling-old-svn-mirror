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

import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.request.RequestProgressTracker;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceManager;
import org.apache.sling.api.services.ServiceLocator;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;
import org.apache.sling.api.wrappers.SlingHttpServletResponseWrapper;
import org.apache.sling.core.impl.SlingHttpServletRequestImpl;
import org.apache.sling.core.impl.SlingHttpServletResponseImpl;
import org.apache.sling.core.impl.adapter.SlingServletRequestAdapter;
import org.apache.sling.core.impl.output.BufferProvider;
import org.apache.sling.core.impl.parameters.ParameterSupport;
import org.apache.sling.core.impl.request.SlingRequestProgressTracker;
import org.apache.sling.core.impl.resolver.ResolvedURLImpl;
import org.apache.sling.core.theme.Theme;
import org.osgi.service.component.ComponentException;
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

    private ResourceManager resourceManager;

    private RequestProgressTracker requestProgressTracker;

    private ServiceLocator serviceLocator;

    private Locale locale;

    private Theme theme;

    /** the current ContentData */
    private ContentData currentContentData;

    /** the stack of ContentData objects */
    private LinkedList<ContentData> contentDataStack;

    public RequestData(HttpServletRequest request, HttpServletResponse response) {
        this.servletRequest = request;
        this.servletResponse = response;

        requestProgressTracker = new SlingRequestProgressTracker();

        // some more preparation
        this.included = request.getAttribute(SlingConstants.INCLUDE_REQUEST_URI) != null;
    }

    /* package */void dispose() {
        // make sure our request attributes do not exist anymore
        this.servletRequest.removeAttribute(SlingConstants.ATTR_REQUEST_CONTENT);
        this.servletRequest.removeAttribute(SlingConstants.ATTR_REQUEST_SERVLET);

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
        this.resourceManager = null;
        this.session = null;
    }

    public HttpServletRequest getServletRequest() {
        return this.servletRequest;
    }

    public HttpServletResponse getServletResponse() {
        return this.servletResponse;
    }

    // ---------- Request Helper

    /**
     * Unwraps the ServletRequest to a SlingHttpServletRequest.
     */
    public static SlingHttpServletRequest unwrap(ServletRequest request)
            throws ComponentException {

        // early check for most cases
        if (request instanceof SlingHttpServletRequest) {
            return (SlingHttpServletRequest) request;
        }

        // unwrap wrappers
        while (request instanceof ServletRequestWrapper) {
            request = ((ServletRequestWrapper) request).getRequest();

            // immediate termination if we found one
            if (request instanceof SlingHttpServletRequest) {
                return (SlingHttpServletRequest) request;
            }
        }

        // if we unwrapped everything and did not find a
        // SlingHttpServletRequest, we lost
        throw new ComponentException(
            "ServletRequest not wrapping SlingHttpServletRequest");
    }

    /**
     * Unwraps the SlingHttpServletRequest to a SlingHttpServletRequestImpl
     *
     * @param request
     * @return
     * @throws ComponentException
     */
    public static SlingHttpServletRequestImpl unwrap(
            SlingHttpServletRequest request) throws ComponentException {
        while (request instanceof SlingHttpServletRequestWrapper) {
            request = ((SlingHttpServletRequestWrapper) request).getSlingRequest();
        }

        if (request instanceof SlingHttpServletRequestImpl) {
            return (SlingHttpServletRequestImpl) request;
        }

        throw new ComponentException(
            "SlingHttpServletRequest not of correct type");
    }

    /**
     * Unwraps the ServletRequest to a SlingHttpServletRequest.
     */
    public static SlingHttpServletResponse unwrap(ServletResponse response)
            throws ComponentException {

        // early check for most cases
        if (response instanceof SlingHttpServletResponse) {
            return (SlingHttpServletResponse) response;
        }

        // unwrap wrappers
        while (response instanceof ServletResponseWrapper) {
            response = ((ServletResponseWrapper) response).getResponse();

            // immediate termination if we found one
            if (response instanceof SlingHttpServletResponse) {
                return (SlingHttpServletResponse) response;
            }
        }

        // if we unwrapped everything and did not find a
        // SlingHttpServletResponse, we lost
        throw new ComponentException(
            "ServletResponse not wrapping SlingHttpServletResponse");
    }

    /**
     * Unwraps a SlingHttpServletResponse to a SlingHttpServletResponseImpl
     *
     * @param response
     * @return
     * @throws ComponentException
     */
    public static SlingHttpServletResponseImpl unwrap(
            SlingHttpServletResponse response) throws ComponentException {
        while (response instanceof SlingHttpServletResponseWrapper) {
            response = ((SlingHttpServletResponseWrapper) response).getSlingResponse();
        }

        if (response instanceof SlingHttpServletResponseImpl) {
            return (SlingHttpServletResponseImpl) response;
        }

        throw new ComponentException(
            "SlingHttpServletResponse not of correct type");
    }

    public static RequestData getRequestData(SlingHttpServletRequest request)
            throws ComponentException {
        return unwrap(request).getRequestData();
    }

    public static RequestData getRequestData(ServletRequest request)
            throws ComponentException {
        return unwrap(unwrap(request)).getRequestData();
    }

    public static SlingHttpServletRequest toSlingHttpServletRequest(
            ServletRequest request) throws ComponentException {
        // unwrap to SlingHttpServletRequest
        SlingHttpServletRequest cRequest = unwrap(request);

        // check type of response, don't care actually for the response itself
        RequestData.unwrap(cRequest);

        // if the servlet response is actually the SlingHttpServletResponse, we
        // are done
        if (cRequest == request) {
            return cRequest;
        }

        // ensure the request is a HTTP request
        if (!(request instanceof HttpServletRequest)) {
            throw new ComponentException("Request is not an HTTP request");
        }

        // otherwise, we create a new response wrapping the servlet response
        // and unwrapped component response
        return new SlingServletRequestAdapter(cRequest,
            (HttpServletRequest) request);
    }

    public static SlingHttpServletResponse toSlingHttpServletResponse(
            ServletResponse response) throws ComponentException {
        // unwrap to SlingHttpServletResponse
        SlingHttpServletResponse cResponse = unwrap(response);

        // check type of response, don't care actually for the response itself
        RequestData.unwrap(cResponse);

        // if the servlet response is actually the SlingHttpServletResponse, we
        // are done
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

    public void pushContent(Resource resource, RequestPathInfo requestPathInfo) {
        BufferProvider parent;
        if (this.currentContentData != null) {
            if (this.contentDataStack == null) {
                this.contentDataStack = new LinkedList<ContentData>();
            }

            // ensure the selectors, extension and suffix are inherited
            // from the parent if none have been declared on inclusion
            if (requestPathInfo.getExtension() == null
                || requestPathInfo.getExtension().length() == 0) {
                ResolvedURLImpl copy = new ResolvedURLImpl(requestPathInfo);
                RequestPathInfo current = currentContentData.getRequestPathInfo();
                copy.setSelectorString(current.getSelectorString());
                copy.setExtension(current.getExtension());
                copy.setSuffix(current.getSuffix());
                requestPathInfo = copy;
            }

            // remove the request attributes if the stack is empty now
            this.servletRequest.setAttribute(
                SlingConstants.ATTR_REQUEST_CONTENT,
                this.currentContentData.getResource());
            this.servletRequest.setAttribute(
                SlingConstants.ATTR_REQUEST_SERVLET,
                this.currentContentData.getServlet());

            this.contentDataStack.add(this.currentContentData);
            parent = this.currentContentData;
        } else {
            parent = this;
        }

        this.currentContentData = new ContentData(resource, requestPathInfo,
            parent);
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
                this.servletRequest.removeAttribute(SlingConstants.ATTR_REQUEST_CONTENT);
                this.servletRequest.removeAttribute(SlingConstants.ATTR_REQUEST_SERVLET);
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
     * <code>SlingHttpServletRequestDispatcher.include</code>.
     */
    public boolean isContentIncluded() {
        return this.contentDataStack != null
            && !this.contentDataStack.isEmpty();
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
                    ? (String) this.servletRequest.getAttribute(SlingConstants.INCLUDE_REQUEST_URI)
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
                    ? (String) this.servletRequest.getAttribute(SlingConstants.INCLUDE_CONTEXT_PATH)
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
                    ? (String) this.servletRequest.getAttribute(SlingConstants.INCLUDE_SERVLET_PATH)
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
                    ? (String) this.servletRequest.getAttribute(SlingConstants.INCLUDE_PATH_INFO)
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
                    ? (String) this.servletRequest.getAttribute(SlingConstants.INCLUDE_QUERY_STRING)
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

    public ResourceManager getResourceManager() {
        return resourceManager;
    }

    /**
     * @param persistenceManager the persistenceManager to set
     */
    public void setResourceManager(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    public RequestProgressTracker getRequestProgressTracker() {
        return requestProgressTracker;
    }

    public void setServiceLocator(ServiceLocator serviceLocator) {
        this.serviceLocator = serviceLocator;
    }

    public ServiceLocator getServiceLocator() {
        return serviceLocator;
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
        if (this.parameterSupport != null
            && this.parameterSupport.requestDataUsed()) {
            throw new IllegalStateException(
                "Request Data has already been read");
        }

        // may throw IllegalStateException if the reader has already been
        // acquired
        return this.getServletRequest().getInputStream();
    }

    BufferedReader getReader() throws UnsupportedEncodingException, IOException {
        if (this.parameterSupport != null
            && this.parameterSupport.requestDataUsed()) {
            throw new IllegalStateException(
                "Request Data has already been read");
        }

        // may throw IllegalStateException if the input stream has already been
        // acquired
        return this.getServletRequest().getReader();
    }

    ParameterSupport getParameterSupport() {
        if (this.parameterSupport == null) {
            this.parameterSupport = new ParameterSupport(this /* getServletRequest() */);
        }

        return this.parameterSupport;
    }
}
