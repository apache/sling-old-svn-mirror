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
package org.apache.sling.engine.impl.request;

import static org.apache.sling.api.SlingConstants.ATTR_REQUEST_CONTENT;
import static org.apache.sling.api.SlingConstants.ATTR_REQUEST_SERVLET;
import static org.apache.sling.engine.EngineConstants.SLING_CURRENT_SERVLET_NAME;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;

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

import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RecursionTooDeepException;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.request.RequestProgressTracker;
import org.apache.sling.api.request.TooManyCallsException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.ServletResolver;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;
import org.apache.sling.api.wrappers.SlingHttpServletResponseWrapper;
import org.apache.sling.engine.RequestUtil;
import org.apache.sling.engine.impl.SlingHttpServletRequestImpl;
import org.apache.sling.engine.impl.SlingHttpServletResponseImpl;
import org.apache.sling.engine.impl.SlingMainServlet;
import org.apache.sling.engine.impl.adapter.SlingServletRequestAdapter;
import org.apache.sling.engine.impl.adapter.SlingServletResponseAdapter;
import org.apache.sling.engine.impl.output.BufferProvider;
import org.apache.sling.engine.impl.parameters.ParameterSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>RequestData</code> class provides access to objects which are set
 * on a Servlet Request wide basis such as the repository session, the
 * persistence manager, etc.
 *
 * The setup order is:
 * <ol>
 *   <li>Invoke constructor</li>
 *   <li>Invoke initResource()</li>
 *   <li>Invoke initServlet()</li>
 * </ol>
 * @see ContentData
 */
public class RequestData implements BufferProvider {

    /** default log */
    private final Logger log = LoggerFactory.getLogger(RequestData.class);

    /**
     * The default value for the number of recursive inclusions for a single
     * instance of this class (value is 50).
     */
    public static final int DEFAULT_MAX_INCLUSION_COUNTER = 50;

    /**
     * The default value for the number of calls to the
     * {@link #service(SlingHttpServletRequest, SlingHttpServletResponse)}
     * method for a single instance of this class (value is 1000).
     */
    public static final int DEFAULT_MAX_CALL_COUNTER = 1000;

    /**
     * The maximum inclusion depth (default
     * {@link #DEFAULT_MAX_INCLUSION_COUNTER}). This value is compared to the
     * number of entries in the {@link #contentDataStack} when the
     * {@link #pushContent(Resource, RequestPathInfo)} method is called.
     */
    private static int maxInclusionCounter = DEFAULT_MAX_INCLUSION_COUNTER;

    /**
     * The maximum number of scripts which may be included through the
     * {@link #service(SlingHttpServletRequest, SlingHttpServletResponse)}
     * method (default {@link #DEFAULT_MAX_CALL_COUNTER}). This number should
     * not be too small to prevent request aborts.
     */
    private static int maxCallCounter = DEFAULT_MAX_CALL_COUNTER;
    
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

    private ResourceResolver resourceResolver;

    private RequestProgressTracker requestProgressTracker;

    /** the current ContentData */
    private ContentData currentContentData;

    /** the stack of ContentData objects */
    private LinkedList<ContentData> contentDataStack;

    /**
     * the number of servlets called by
     * {@link #service(SlingHttpServletRequest, SlingHttpServletResponse)}
     */
    private int servletCallCounter;

    /**
     * The name of the currently active serlvet.
     * 
     * @see #setActiveServletName(String)
     * @see #getActiveServletName()
     */
    private String activeServletName;

    public static void setMaxCallCounter(int maxCallCounter) {
        RequestData.maxCallCounter = maxCallCounter;
    }

    public static int getMaxCallCounter() {
        return maxCallCounter;
    }

    public static void setMaxIncludeCounter(int maxInclusionCounter) {
        RequestData.maxInclusionCounter = maxInclusionCounter;
    }

    public static int getMaxIncludeCounter() {
        return maxInclusionCounter;
    }

    public RequestData(SlingMainServlet slingMainServlet,
            HttpServletRequest request, HttpServletResponse response) {

        this.slingMainServlet = slingMainServlet;

        this.servletRequest = request;
        this.servletResponse = response;

        this.slingRequest = new SlingHttpServletRequestImpl(this,
            servletRequest);
        this.slingResponse = new SlingHttpServletResponseImpl(this,
            servletResponse);

        this.requestProgressTracker = new SlingRequestProgressTracker();
    }

    public Resource initResource(ResourceResolver resourceResolver) {
        // keep the resource resolver for request processing
        this.resourceResolver = resourceResolver;

        // resolve the resource
        requestProgressTracker.startTimer("ResourceResolution");
        final HttpServletRequest request = getServletRequest();
        Resource resource = resourceResolver.resolve(request, request.getPathInfo());
        requestProgressTracker.logTimer("ResourceResolution",
            "URI={0} resolves to Resource={1}",
            getServletRequest().getRequestURI(), resource);
        return resource;
    }

    public void initServlet(final Resource resource) {
        // the resource and the request path info, will never be null
        RequestPathInfo requestPathInfo = new SlingRequestPathInfo(resource);
        ContentData contentData = pushContent(resource, requestPathInfo);

	    requestProgressTracker.log("Resource Path Info: {0}", requestPathInfo);

        // finally resolve the servlet for the resource
        ServletResolver sr = slingMainServlet.getServletResolver();
        if (sr != null) {
            requestProgressTracker.startTimer("ServletResolution");
            Servlet servlet = sr.resolveServlet(slingRequest);
            requestProgressTracker.logTimer("ServletResolution",
                "URI={0} handled by Servlet={1}",
                getServletRequest().getRequestURI(), RequestUtil.getServletName(servlet));
            contentData.setServlet(servlet);
        } else {
            log.warn("init(): No ServletResolver available");
        }
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
        resourceResolver = null;
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
     *
     * @throws IllegalArgumentException If the <code>request</code> is not a
     *             <code>SlingHttpServletRequest</code> and not a
     *             <code>ServletRequestWrapper</code> wrapping a
     *             <code>SlingHttpServletRequest</code>.
     */
    public static SlingHttpServletRequest unwrap(ServletRequest request) {

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
        throw new IllegalArgumentException(
            "ServletRequest not wrapping SlingHttpServletRequest");
    }

    /**
     * Unwraps the SlingHttpServletRequest to a SlingHttpServletRequestImpl
     *
     * @param request
     * @throws IllegalArgumentException If <code>request</code> is not a
     *             <code>SlingHttpServletRequestImpl</code> and not
     *             <code>SlingHttpServletRequestWrapper</code> wrapping a
     *             <code>SlingHttpServletRequestImpl</code>.
     */
    public static SlingHttpServletRequestImpl unwrap(
            SlingHttpServletRequest request) {
        while (request instanceof SlingHttpServletRequestWrapper) {
            request = ((SlingHttpServletRequestWrapper) request).getSlingRequest();
        }

        if (request instanceof SlingHttpServletRequestImpl) {
            return (SlingHttpServletRequestImpl) request;
        }

        throw new IllegalArgumentException(
            "SlingHttpServletRequest not of correct type");
    }

    /**
     * Unwraps the ServletRequest to a SlingHttpServletRequest.
     *
     * @throws IllegalArgumentException If the <code>response</code> is not a
     *             <code>SlingHttpServletResponse</code> and not a
     *             <code>ServletResponseWrapper</code> wrapping a
     *             <code>SlingHttpServletResponse</code>.
     */
    public static SlingHttpServletResponse unwrap(ServletResponse response) {

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
        throw new IllegalArgumentException(
            "ServletResponse not wrapping SlingHttpServletResponse");
    }

    /**
     * Unwraps a SlingHttpServletResponse to a SlingHttpServletResponseImpl
     *
     * @param response
     * @throws IllegalArgumentException If <code>response</code> is not a
     *             <code>SlingHttpServletResponseImpl</code> and not
     *             <code>SlingHttpServletResponseWrapper</code> wrapping a
     *             <code>SlingHttpServletResponseImpl</code>.
     */
    public static SlingHttpServletResponseImpl unwrap(
            SlingHttpServletResponse response) {
        while (response instanceof SlingHttpServletResponseWrapper) {
            response = ((SlingHttpServletResponseWrapper) response).getSlingResponse();
        }

        if (response instanceof SlingHttpServletResponseImpl) {
            return (SlingHttpServletResponseImpl) response;
        }

        throw new IllegalArgumentException(
            "SlingHttpServletResponse not of correct type");
    }

    /**
     * @param request
     * @throws IllegalArgumentException If the <code>request</code> is not a
     *             <code>SlingHttpServletRequest</code> and not a
     *             <code>ServletRequestWrapper</code> wrapping a
     *             <code>SlingHttpServletRequest</code>.
     */
    public static RequestData getRequestData(ServletRequest request) {
        return unwrap(unwrap(request)).getRequestData();
    }

    /**
     * @param request
     * @throws IllegalArgumentException If <code>request</code> is not a
     *             <code>SlingHttpServletRequestImpl</code> and not
     *             <code>SlingHttpServletRequestWrapper</code> wrapping a
     *             <code>SlingHttpServletRequestImpl</code>.
     */
    public static RequestData getRequestData(SlingHttpServletRequest request) {
        return unwrap(request).getRequestData();
    }

    /**
     * @param request
     * @throws IllegalArgumentException if <code>request</code> is not a
     *             <code>HttpServletRequest</code> of if <code>request</code>
     *             is not backed by <code>SlingHttpServletRequestImpl</code>.
     */
    public static SlingHttpServletRequest toSlingHttpServletRequest(
            ServletRequest request) {

        // unwrap to SlingHttpServletRequest, may throw if no
        // SlingHttpServletRequest is wrapped in request
        SlingHttpServletRequest cRequest = unwrap(request);

        // ensure the SlingHttpServletRequest is backed by
        // SlingHttpServletRequestImpl
        RequestData.unwrap(cRequest);

        // if the request is not wrapper at all, we are done
        if (cRequest == request) {
            return cRequest;
        }

        // ensure the request is a HTTP request
        if (!(request instanceof HttpServletRequest)) {
            throw new IllegalArgumentException("Request is not an HTTP request");
        }

        // otherwise, we create a new response wrapping the servlet response
        // and unwrapped component response
        return new SlingServletRequestAdapter(cRequest,
            (HttpServletRequest) request);
    }

    /**
     * @param response
     * @throws IllegalArgumentException if <code>response</code> is not a
     *             <code>HttpServletResponse</code> of if
     *             <code>response</code> is not backed by
     *             <code>SlingHttpServletResponseImpl</code>.
     */
    public static SlingHttpServletResponse toSlingHttpServletResponse(
            ServletResponse response) {

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
            throw new IllegalArgumentException(
                "Response is not an HTTP response");
        }

        // otherwise, we create a new response wrapping the servlet response
        // and unwrapped component response
        return new SlingServletResponseAdapter(cResponse,
            (HttpServletResponse) response);
    }

    /**
     * Helper method to call the servlet for the current content data. If the
     * current content data has no servlet, <em>NOT_FOUND</em> (404) error is
     * sent and the method terminates.
     * <p>
     * If the the servlet exists, the
     * {@link org.apache.sling.engine.EngineConstants#SLING_CURRENT_SERVLET_NAME} request attribute is set
     * to the name of that servlet and that servlet name is also set as the
     * {@link #setActiveServletName(String) currently active servlet}. After
     * the termination of the servlet (normal or throwing a Throwable) the
     * request attribute is reset to the previous value. The name of the
     * currently active servlet is only reset to the previous value if the
     * servlet terminates normally. In case of a Throwable, the active servlet
     * name is not reset and indicates which servlet caused the potential abort
     * of the request.
     *
     * @param request The request object for the service method
     * @param response The response object for the service method
     * @throws IOException May be thrown by the servlet's service method
     * @throws ServletException May be thrown by the servlet's service method
     */
    public static void service(SlingHttpServletRequest request,
            SlingHttpServletResponse response) throws IOException,
            ServletException {

        RequestData requestData = RequestData.getRequestData(request);
        Servlet servlet = requestData.getContentData().getServlet();
        if (servlet == null) {

            response.sendError(HttpServletResponse.SC_NOT_FOUND,
                "No Servlet to handle request");

        } else {

            String name = RequestUtil.getServletName(servlet);

            // verify the number of service calls in this request
            if (requestData.servletCallCounter >= maxCallCounter) {
                throw new TooManyCallsException(name);
            }
            
            // replace the current servlet name in the request
            Object oldValue = request.getAttribute(SLING_CURRENT_SERVLET_NAME);
            request.setAttribute(SLING_CURRENT_SERVLET_NAME, name);

            // setup the tracker for this service call
            String timerName = name + "#" + requestData.servletCallCounter;
            requestData.servletCallCounter++;
            requestData.getRequestProgressTracker().startTimer(timerName);

            try {

                String callerServlet = requestData.setActiveServletName(name);

                servlet.service(request, response);

                requestData.setActiveServletName(callerServlet);

            } finally {

                request.setAttribute(SLING_CURRENT_SERVLET_NAME, oldValue);

                requestData.getRequestProgressTracker().logTimer(timerName);

            }
        }
    }

    // ---------- Content inclusion stacking -----------------------------------

    public ContentData pushContent(Resource resource,
            RequestPathInfo requestPathInfo) {
        BufferProvider parent;
        if (currentContentData != null) {
            if (contentDataStack == null) {
                contentDataStack = new LinkedList<ContentData>();
            } else if (contentDataStack.size() >= maxInclusionCounter) {
                throw new RecursionTooDeepException(
                    requestPathInfo.getResourcePath());
            }

            // set the request attributes of the include content data
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

            if (contentDataStack.isEmpty()) {

                // remove the request attributes if the stack is empty now
                servletRequest.removeAttribute(SlingConstants.ATTR_REQUEST_CONTENT);
                servletRequest.removeAttribute(SlingConstants.ATTR_REQUEST_SERVLET);

            } else {

                // otherwise reset the attributes of the including content data
                ContentData including = contentDataStack.getLast();
                servletRequest.setAttribute(ATTR_REQUEST_CONTENT,
                    including.getResource());
                servletRequest.setAttribute(ATTR_REQUEST_SERVLET,
                    including.getServlet());

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

    public ResourceResolver getResourceResolver() {
        return resourceResolver;
    }

    public RequestProgressTracker getRequestProgressTracker() {
        return requestProgressTracker;
    }

    /**
     * Sets the name of the currently active servlet and returns the name of the
     * previously active servlet.
     */
    public String setActiveServletName(String servletName) {
        String old = activeServletName;
        activeServletName = servletName;
        return old;
    }

    /**
     * Returns the name of the currently active servlet. If this name is not
     * <code>null</code> at the end of request processing, more precisly in
     * the case of an uncaught <code>Throwable</code> at the end of request
     * processing, this is the name of the servlet causing the uncaught
     * <code>Throwable</code>.
     */
    public String getActiveServletName() {
        return activeServletName;
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

    public ServletInputStream getInputStream() throws IOException {
        if (parameterSupport != null && parameterSupport.requestDataUsed()) {
            throw new IllegalStateException(
                "Request Data has already been read");
        }

        // may throw IllegalStateException if the reader has already been
        // acquired
        return getServletRequest().getInputStream();
    }

    public BufferedReader getReader() throws UnsupportedEncodingException,
            IOException {
        if (parameterSupport != null && parameterSupport.requestDataUsed()) {
            throw new IllegalStateException(
                "Request Data has already been read");
        }

        // may throw IllegalStateException if the input stream has already been
        // acquired
        return getServletRequest().getReader();
    }

    public ParameterSupport getParameterSupport() {
        if (parameterSupport == null) {
            parameterSupport = ParameterSupport.getInstance(getServletRequest());
        }

        return parameterSupport;
    }
}
