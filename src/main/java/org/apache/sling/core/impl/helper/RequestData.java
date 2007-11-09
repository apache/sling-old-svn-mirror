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

import static org.apache.sling.api.SlingConstants.ATTR_REQUEST_CONTENT;
import static org.apache.sling.api.SlingConstants.ATTR_REQUEST_SERVLET;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.Locale;

import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.ServletResponse;
import javax.servlet.ServletResponseWrapper;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.HttpStatusCodeException;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.request.RequestProgressTracker;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceManager;
import org.apache.sling.api.services.ServiceLocator;
import org.apache.sling.api.servlets.ServletResolver;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;
import org.apache.sling.api.wrappers.SlingHttpServletResponseWrapper;
import org.apache.sling.core.impl.SlingMainServlet;
import org.apache.sling.core.impl.adapter.SlingServletRequestAdapter;
import org.apache.sling.core.impl.output.BufferProvider;
import org.apache.sling.core.impl.parameters.ParameterSupport;
import org.apache.sling.core.impl.request.SlingRequestProgressTracker;
import org.apache.sling.core.theme.Theme;
import org.apache.sling.jcr.resource.JcrResourceManagerFactory;
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

    /** The SlingMainServlet used for request dispatching and other stuff */
    private final SlingMainServlet slingMainServlet;

    /** The original servlet Servlet Request Object */
    private HttpServletRequest servletRequest;

    /** The original servlet Servlet Response object */
    private HttpServletResponse servletResponse;

    /** The original servlet Servlet Request Object */
    private SlingHttpServletRequest slingRequest;

    /** The original servlet Servlet Response object */
    private SlingHttpServletResponse slingResponse;

    /** The parameter support class */
    private ParameterSupport parameterSupport;

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

    private ResourceManager resourceManager;

    private RequestProgressTracker requestProgressTracker;

    private Locale locale;

    private Theme theme;

    /** the current ContentData */
    private ContentData currentContentData;

    /** the stack of ContentData objects */
    private LinkedList<ContentData> contentDataStack;

    public RequestData(SlingMainServlet slingMainServlet, Session session,
            HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        this.slingMainServlet = slingMainServlet;

        this.servletRequest = request;
        this.servletResponse = response;

        this.slingRequest = new SlingHttpServletRequestImpl(this, servletRequest);
        this.slingResponse = new SlingHttpServletResponseImpl(this, servletResponse);

        this.requestProgressTracker = new SlingRequestProgressTracker();
        this.included = request.getAttribute(SlingConstants.INCLUDE_REQUEST_URI) != null;

        // the resource manager factory may be missing
        JcrResourceManagerFactory rmf = slingMainServlet.getResourceManagerFactory();
        if (rmf == null) {
            log.error("RequestData: Missing JcrResourceManagerFactory");
            throw new HttpStatusCodeException(HttpServletResponse.SC_NOT_FOUND,
                "No resource can be found");
        }

        // officially, getting the manager may fail, but not i this implementation
        this.resourceManager = rmf.getResourceManager(session);

        // resolve the resource and the request path info, will never be null
        Resource resource = resourceManager.resolve(request);
        RequestPathInfo requestPathInfo = new SlingRequestPathInfo(resource,
            request.getPathInfo());
        ContentData contentData = pushContent(resource, requestPathInfo);

        // finally resolve the servlet for the resource
        ServletResolver sr = slingMainServlet.getServletResolver();
        Servlet servlet = sr.resolveServlet(slingRequest);
        contentData.setServlet(servlet);
    }

    public void dispose() {
        // make sure our request attributes do not exist anymore
        servletRequest.removeAttribute(SlingConstants.ATTR_REQUEST_CONTENT);
        servletRequest.removeAttribute(SlingConstants.ATTR_REQUEST_SERVLET);

        // clear the content data stack
        if (contentDataStack != null) {
            while (!contentDataStack.isEmpty()) {
                ContentData cd = contentDataStack.removeLast();
                cd.dispose();
            }
        }

        // dispose current content data, if any
        if (currentContentData != null) {
            currentContentData.dispose();
        }

        // clear fields
        contentDataStack = null;
        currentContentData = null;
        servletRequest = null;
        servletResponse = null;
        resourceManager = null;
    }

    public SlingMainServlet getSlingMainServlet() {
        return slingMainServlet;
    }

    public HttpServletRequest getServletRequest() {
        return servletRequest;
    }

    public HttpServletResponse getServletResponse() {
        return servletResponse;
    }

    public SlingHttpServletRequest getSlingRequest() {
        return slingRequest;
    }

    public SlingHttpServletResponse getSlingResponse() {
        return slingResponse;
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

    public ContentData pushContent(Resource resource,
            RequestPathInfo requestPathInfo) {
        BufferProvider parent;
        if (currentContentData != null) {
            if (contentDataStack == null) {
                contentDataStack = new LinkedList<ContentData>();
            }

            // remove the request attributes if the stack is empty now
            servletRequest.setAttribute(ATTR_REQUEST_CONTENT,
                currentContentData.getResource());
            servletRequest.setAttribute(ATTR_REQUEST_SERVLET,
                currentContentData.getServlet());

            contentDataStack.add(currentContentData);
            parent = currentContentData;
        } else {
            parent = this;
        }

        currentContentData = new ContentData(resource, requestPathInfo, parent);
        return currentContentData;
    }

    public ContentData popContent() {
        // dispose current content data before replacing it
        if (currentContentData != null) {
            currentContentData.dispose();
        }

        if (contentDataStack != null && !contentDataStack.isEmpty()) {
            // remove the topmost content data object
            currentContentData = contentDataStack.removeLast();

            // remove the request attributes if the stack is empty now
            if (contentDataStack.isEmpty()) {
                servletRequest.removeAttribute(SlingConstants.ATTR_REQUEST_CONTENT);
                servletRequest.removeAttribute(SlingConstants.ATTR_REQUEST_SERVLET);
            }

        } else {
            currentContentData = null;
        }

        return currentContentData;
    }

    public ContentData getContentData() {
        return currentContentData;
    }

    /**
     * Returns <code>true</code> if request processing is currently processing
     * a component which has been included by
     * <code>SlingHttpServletRequestDispatcher.include</code>.
     */
    public boolean isContentIncluded() {
        return contentDataStack != null && !contentDataStack.isEmpty();
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
        return included;
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
        if (requestURI == null) {

            // get the unmodified request URI and context information
            requestURI = included
                    ? (String) servletRequest.getAttribute(SlingConstants.INCLUDE_REQUEST_URI)
                    : servletRequest.getRequestURI();

            String ctxPrefix = getContextPath();

            if (log.isDebugEnabled()) {
                log.debug("getRequestURI: Servlet request URI is {}",
                    requestURI);
            }

            // check to remove the context prefix
            if (ctxPrefix == null) {
                log.error("getRequestURI: Context path not expected to be null");
            } else if (ctxPrefix.length() == 0) {
                // default root context, no change
                if (log.isDebugEnabled()) {
                    log.debug("getRequestURI: Default root context, no change to uri");
                }
            } else if (ctxPrefix.length() < requestURI.length()
                && requestURI.startsWith(ctxPrefix)
                && requestURI.charAt(ctxPrefix.length()) == '/') {
                // some path below context root
                if (log.isDebugEnabled()) {
                    log.debug("getRequestURI: removing '{}' from '{}'",
                        ctxPrefix, requestURI);
                }
                requestURI = requestURI.substring(ctxPrefix.length());
            } else if (ctxPrefix.equals(requestURI)) {
                // context root
                if (log.isDebugEnabled()) {
                    log.debug("getRequestURI: URI equals context prefix, assuming '/'");
                }
                requestURI = "/";
            }
        }

        return requestURI;
    }

    /**
     * Returns the contents of the
     * <code>javax.servlet.include.context_path</code> attribute if
     * {@link #isIncluded()} or <code>request.getContextPath()</code>.
     *
     * @return The relevant context path according to environment.
     */
    public String getContextPath() {
        if (contextPath == null) {
            contextPath = included
                    ? (String) servletRequest.getAttribute(SlingConstants.INCLUDE_CONTEXT_PATH)
                    : servletRequest.getContextPath();
        }

        return contextPath;
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
        if (servletPath == null) {
            servletPath = included
                    ? (String) servletRequest.getAttribute(SlingConstants.INCLUDE_SERVLET_PATH)
                    : servletRequest.getServletPath();
        }

        return servletPath;
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
        if (pathInfo == null) {
            pathInfo = included
                    ? (String) servletRequest.getAttribute(SlingConstants.INCLUDE_PATH_INFO)
                    : servletRequest.getPathInfo();
        }

        return pathInfo;
    }

    /**
     * Returns the contents of the
     * <code>javax.servlet.include.query_string</code> attribute if
     * {@link #isIncluded()} or <code>request.getQueryString()</code>.
     *
     * @return The relevant query string according to environment.
     */
    public String getQueryString() {
        if (queryString == null) {
            queryString = included
                    ? (String) servletRequest.getAttribute(SlingConstants.INCLUDE_QUERY_STRING)
                    : servletRequest.getQueryString();
        }

        return queryString;
    }

    /**
     * @return the locale
     */
    public Locale getLocale() {
        return locale;
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

    public RequestProgressTracker getRequestProgressTracker() {
        return requestProgressTracker;
    }

    public ServiceLocator getServiceLocator() {
        return slingMainServlet.getServiceLocator();
    }

    /**
     * @return the theme
     */
    public Theme getTheme() {
        return theme;
    }

    /**
     * @param theme the theme to set
     */
    public void setTheme(Theme theme) {
        this.theme = theme;
        // provide the current theme to components as a request attribute
        // TODO - We should define a well known constant for this
        servletRequest.setAttribute(Theme.class.getName(), theme);
    }

    // ---------- BufferProvider -----------------------------------------

    public BufferProvider getBufferProvider() {
        return (currentContentData != null)
                ? (BufferProvider) currentContentData
                : this;
    }

    public ServletOutputStream getOutputStream() throws IOException {
        return getServletResponse().getOutputStream();
    }

    public PrintWriter getWriter() throws IOException {
        return getServletResponse().getWriter();
    }

    // ---------- Parameter support -------------------------------------------

    ServletInputStream getInputStream() throws IOException {
        if (parameterSupport != null && parameterSupport.requestDataUsed()) {
            throw new IllegalStateException(
                "Request Data has already been read");
        }

        // may throw IllegalStateException if the reader has already been
        // acquired
        return getServletRequest().getInputStream();
    }

    BufferedReader getReader() throws UnsupportedEncodingException, IOException {
        if (parameterSupport != null && parameterSupport.requestDataUsed()) {
            throw new IllegalStateException(
                "Request Data has already been read");
        }

        // may throw IllegalStateException if the input stream has already been
        // acquired
        return getServletRequest().getReader();
    }

    ParameterSupport getParameterSupport() {
        if (parameterSupport == null) {
            parameterSupport = new ParameterSupport(this /* getServletRequest() */);
        }

        return parameterSupport;
    }
}
