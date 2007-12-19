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
package org.apache.sling.core.impl.request;

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
import org.apache.sling.api.SlingException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.request.RequestProgressTracker;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.services.ServiceLocator;
import org.apache.sling.api.servlets.ServletResolver;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;
import org.apache.sling.api.wrappers.SlingHttpServletResponseWrapper;
import org.apache.sling.core.impl.SlingHttpServletRequestImpl;
import org.apache.sling.core.impl.SlingHttpServletResponseImpl;
import org.apache.sling.core.impl.SlingMainServlet;
import org.apache.sling.core.impl.adapter.SlingServletRequestAdapter;
import org.apache.sling.core.impl.output.BufferProvider;
import org.apache.sling.core.impl.parameters.ParameterSupport;
import org.apache.sling.core.theme.Theme;
import org.apache.sling.jcr.resource.JcrResourceResolverFactory;
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

    private ResourceResolver resourceResolver;

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

        // the resource manager factory may be missing
        JcrResourceResolverFactory rmf = slingMainServlet.getResourceResolverFactory();
        if (rmf == null) {
            log.error("RequestData: Missing JcrResourceResolverFactory");
            throw new HttpStatusCodeException(HttpServletResponse.SC_NOT_FOUND,
                "No resource can be found");
        }

        // officially, getting the manager may fail, but not i this implementation
        this.resourceResolver = rmf.getResourceResolver(session);

        // resolve the resource and the request path info, will never be null
        Resource resource = resourceResolver.resolve(request);
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
     */
    public static SlingHttpServletRequest unwrap(ServletRequest request)
            throws SlingException {

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
        throw new SlingException(
            "ServletRequest not wrapping SlingHttpServletRequest");
    }

    /**
     * Unwraps the SlingHttpServletRequest to a SlingHttpServletRequestImpl
     *
     * @param request
     * @return
     * @throws SlingException
     */
    public static SlingHttpServletRequestImpl unwrap(
            SlingHttpServletRequest request) throws SlingException {
        while (request instanceof SlingHttpServletRequestWrapper) {
            request = ((SlingHttpServletRequestWrapper) request).getSlingRequest();
        }

        if (request instanceof SlingHttpServletRequestImpl) {
            return (SlingHttpServletRequestImpl) request;
        }

        throw new SlingException(
            "SlingHttpServletRequest not of correct type");
    }

    /**
     * Unwraps the ServletRequest to a SlingHttpServletRequest.
     */
    public static SlingHttpServletResponse unwrap(ServletResponse response)
            throws SlingException {

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
        throw new SlingException(
            "ServletResponse not wrapping SlingHttpServletResponse");
    }

    /**
     * Unwraps a SlingHttpServletResponse to a SlingHttpServletResponseImpl
     *
     * @param response
     * @return
     * @throws SlingException
     */
    public static SlingHttpServletResponseImpl unwrap(
            SlingHttpServletResponse response) throws SlingException {
        while (response instanceof SlingHttpServletResponseWrapper) {
            response = ((SlingHttpServletResponseWrapper) response).getSlingResponse();
        }

        if (response instanceof SlingHttpServletResponseImpl) {
            return (SlingHttpServletResponseImpl) response;
        }

        throw new SlingException(
            "SlingHttpServletResponse not of correct type");
    }

    public static RequestData getRequestData(SlingHttpServletRequest request)
            throws SlingException {
        return unwrap(request).getRequestData();
    }

    public static RequestData getRequestData(ServletRequest request)
            throws SlingException {
        return unwrap(unwrap(request)).getRequestData();
    }

    public static SlingHttpServletRequest toSlingHttpServletRequest(
            ServletRequest request) throws SlingException {
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
            throw new SlingException("Request is not an HTTP request");
        }

        // otherwise, we create a new response wrapping the servlet response
        // and unwrapped component response
        return new SlingServletRequestAdapter(cRequest,
            (HttpServletRequest) request);
    }

    public static SlingHttpServletResponse toSlingHttpServletResponse(
            ServletResponse response) throws SlingException {
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
            throw new SlingException("Response is not an HTTP response");
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

    public ResourceResolver getResourceResolver() {
        return resourceResolver;
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

    public ServletInputStream getInputStream() throws IOException {
        if (parameterSupport != null && parameterSupport.requestDataUsed()) {
            throw new IllegalStateException(
                "Request Data has already been read");
        }

        // may throw IllegalStateException if the reader has already been
        // acquired
        return getServletRequest().getInputStream();
    }

    public BufferedReader getReader() throws UnsupportedEncodingException, IOException {
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
            parameterSupport = new ParameterSupport(this /* getServletRequest() */);
        }

        return parameterSupport;
    }
}
