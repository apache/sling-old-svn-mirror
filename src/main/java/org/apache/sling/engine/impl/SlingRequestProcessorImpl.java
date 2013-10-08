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
package org.apache.sling.engine.impl;

import static org.apache.sling.api.SlingConstants.ERROR_SERVLET_NAME;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.security.AccessControlException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.SlingServletException;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.ServletResolver;
import org.apache.sling.api.wrappers.SlingHttpServletResponseWrapper;
import org.apache.sling.engine.SlingRequestProcessor;
import org.apache.sling.engine.impl.filter.AbstractSlingFilterChain;
import org.apache.sling.engine.impl.filter.RequestSlingFilterChain;
import org.apache.sling.engine.impl.filter.ServletFilterManager;
import org.apache.sling.engine.impl.filter.ServletFilterManager.FilterChainType;
import org.apache.sling.engine.impl.filter.SlingComponentFilterChain;
import org.apache.sling.engine.impl.request.ContentData;
import org.apache.sling.engine.impl.request.RequestData;
import org.apache.sling.engine.impl.request.RequestHistoryConsolePlugin;
import org.apache.sling.engine.servlets.ErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SlingRequestProcessorImpl implements SlingRequestProcessor {

    /** default log */
    private final Logger log = LoggerFactory.getLogger(SlingRequestProcessorImpl.class);

    // used fields ....

    private final DefaultErrorHandler defaultErrorHandler = new DefaultErrorHandler();

    private ErrorHandler errorHandler = defaultErrorHandler;

    private ServletResolver servletResolver;

    private ServletFilterManager filterManager;

    private RequestProcessorMBeanImpl mbean;

    // ---------- helper setters

    void setServerInfo(final String serverInfo) {
        defaultErrorHandler.setServerInfo(serverInfo);
    }

    void setErrorHandler(final ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    void unsetErrorHandler(final ErrorHandler errorHandler) {
        if (this.errorHandler == errorHandler) {
            this.errorHandler = defaultErrorHandler;
        }
    }

    void setServletResolver(final ServletResolver servletResolver) {
        this.servletResolver = servletResolver;
    }

    void unsetServletResolver(final ServletResolver servletResolver) {
        if (this.servletResolver == servletResolver) {
            this.servletResolver = null;
        }
    }

    void setFilterManager(final ServletFilterManager filterManager) {
        this.filterManager = filterManager;
    }

    void setMBean(final RequestProcessorMBeanImpl mbean) {
        this.mbean = mbean;
    }

    // ---------- SlingRequestProcessor interface

    public void processRequest(final HttpServletRequest servletRequest,
            final HttpServletResponse servletResponse,
            final ResourceResolver resourceResolver) throws IOException {

        // setting the Sling request and response
        final RequestData requestData = new RequestData(this, servletRequest,
            servletResponse);
        final SlingHttpServletRequest request = requestData.getSlingRequest();
        final SlingHttpServletResponse response = requestData.getSlingResponse();

        // record the request for the web console display
        RequestHistoryConsolePlugin.recordRequest(request);

        try {
            final ServletResolver sr = this.servletResolver;

            // check that we have all required services
            if (resourceResolver == null) {
                throw new UnavailableException("ResourceResolver");
            } else if (sr == null) {
                throw new UnavailableException("ServletResolver");
            }

            // initialize the request data - resolve resource and servlet
            Resource resource = requestData.initResource(resourceResolver);
            requestData.initServlet(resource, sr);

            Filter[] filters = filterManager.getFilters(FilterChainType.REQUEST);
            if (filters != null) {
                FilterChain processor = new RequestSlingFilterChain(this,
                    filters);

                request.getRequestProgressTracker().log(
                    "Applying " + FilterChainType.REQUEST + "filters");

                processor.doFilter(request, response);

            } else {

                // no filters, directly call resource level filters and servlet
                processComponent(request, response, FilterChainType.COMPONENT);

            }

        } catch ( final SlingHttpServletResponseImpl.WriterAlreadyClosedException wace ) {
            log.error("Writer has already been closed.", wace);
        } catch (ResourceNotFoundException rnfe) {

            // send this exception as a 404 status
            log.info("service: Resource {} not found", rnfe.getResource());

            handleError(HttpServletResponse.SC_NOT_FOUND, rnfe.getMessage(),
                request, response);

        } catch (final SlingException se) {

            // if we have request data and a non-null active servlet name
            // we assume, that this is the name of the causing servlet
            if (requestData.getActiveServletName() != null) {
                request.setAttribute(ERROR_SERVLET_NAME,
                    requestData.getActiveServletName());
            }

            // send this exception as is (albeit unwrapping and wrapped
            // exception.
            Throwable t = se;
            while ( t instanceof SlingException && t.getCause() != null ) {
                t = t.getCause();
            }
            log.error("service: Uncaught SlingException", t);
            handleError(t, request, response);

        } catch (AccessControlException ace) {

            // SLING-319 if anything goes wrong, send 403/FORBIDDEN
            log.info(
                "service: Authenticated user {} does not have enough rights to executed requested action",
                request.getRemoteUser());
            handleError(HttpServletResponse.SC_FORBIDDEN, null, request,
                response);

        } catch (UnavailableException ue) {

            // exception is thrown before the SlingHttpServletRequest/Response
            // is properly set up due to missing dependencies. In this case
            // we must not use the Sling error handling infrastructure but
            // just return a 503 status response handled by the servlet
            // container environment

            final int status = HttpServletResponse.SC_SERVICE_UNAVAILABLE;
            final String errorMessage = ue.getMessage()
                + " service missing, cannot service requests";
            log.error("{} , sending status {}", errorMessage, status);
            servletResponse.sendError(status, errorMessage);

        } catch (IOException ioe) {

            // forward IOException up the call chain to properly handle it
            throw ioe;

        } catch (Throwable t) {

            // if we have request data and a non-null active servlet name
            // we assume, that this is the name of the causing servlet
            if (requestData.getActiveServletName() != null) {
                request.setAttribute(ERROR_SERVLET_NAME,
                    requestData.getActiveServletName());
            }

            log.error("service: Uncaught Throwable", t);
            handleError(t, request, response);

        } finally {
            if (mbean != null) {
                mbean.addRequestData(requestData);
            }
        }
    }

    /**
     * Renders the component defined by the RequestData's current ComponentData
     * instance after calling all filters of the given
     * {@link org.apache.sling.engine.impl.filter.ServletFilterManager.FilterChainType
     * filterChainType}.
     *
     * @param request
     * @param response
     * @param filterChainType
     * @throws IOException
     * @throws ServletException
     */

    public void processComponent(SlingHttpServletRequest request,
            SlingHttpServletResponse response,
            final FilterChainType filterChainType) throws IOException,
            ServletException {

        Filter filters[] = filterManager.getFilters(filterChainType);
        if (filters != null) {

            FilterChain processor = new SlingComponentFilterChain(filters);
            request.getRequestProgressTracker().log(
                "Applying " + filterChainType + "filters");
            processor.doFilter(request, response);

        } else {

            log.debug("service: No Resource level filters, calling servlet");
            RequestData.service(request, response);

        }
    }

    // ---------- Generic Content Request processor ----------------------------

    /**
     * Dispatches the request on behalf of the
     * {@link org.apache.sling.engine.impl.request.SlingRequestDispatcher}.
     */
    public void dispatchRequest(ServletRequest request,
            ServletResponse response, Resource resource,
            RequestPathInfo resolvedURL, boolean include) throws IOException,
            ServletException {

        // we need a SlingHttpServletRequest/SlingHttpServletResponse tupel
        // to continue
        SlingHttpServletRequest cRequest = RequestData.toSlingHttpServletRequest(request);
        SlingHttpServletResponse cResponse = RequestData.toSlingHttpServletResponse(response);

        // get the request data (and btw check the correct type)
        final RequestData requestData = RequestData.getRequestData(cRequest);
        final ContentData oldContentData = requestData.getContentData();
        final ContentData contentData = requestData.setContent(resource, resolvedURL);

        try {
            // resolve the servlet
            Servlet servlet = servletResolver.resolveServlet(cRequest);
            contentData.setServlet(servlet);

            FilterChainType type = include
                    ? FilterChainType.INCLUDE
                    : FilterChainType.FORWARD;

            processComponent(cRequest, cResponse, type);
        } finally {
            requestData.resetContent(oldContentData);
        }
    }

    // ---------- Error Handling with Filters

    void handleError(final int status, final String message,
            final SlingHttpServletRequest request,
            SlingHttpServletResponse response) throws IOException {

        // wrap the response ensuring getWriter will fall back to wrapping
        // the response output stream if reset does not reset this
        response = new ErrorResponseWrapper(response);

        Filter[] filters = filterManager.getFilters(FilterChainType.ERROR);
        if (filters != null && filters.length > 0) {
            FilterChain processor = new AbstractSlingFilterChain(filters) {

                @Override
                protected void render(SlingHttpServletRequest request,
                        SlingHttpServletResponse response) throws IOException {
                    errorHandler.handleError(status, message, request, response);
                }
            };
            request.getRequestProgressTracker().log(
                "Applying " + FilterChainType.ERROR + " filters");

            try {
                processor.doFilter(request, response);
            } catch (ServletException se) {
                throw new SlingServletException(se);
            }
        } else {
            errorHandler.handleError(status, message, request, response);
        }
    }

    // just rethrow the exception as explained in the class comment
    private void handleError(final Throwable throwable,
            final SlingHttpServletRequest request,
            SlingHttpServletResponse response) throws IOException {

        // wrap the response ensuring getWriter will fall back to wrapping
        // the response output stream if reset does not reset this
        response = new ErrorResponseWrapper(response);

        Filter[] filters = filterManager.getFilters(FilterChainType.ERROR);
        if (filters != null && filters.length > 0) {
            FilterChain processor = new AbstractSlingFilterChain(filters) {

                @Override
                protected void render(SlingHttpServletRequest request,
                        SlingHttpServletResponse response) throws IOException {
                    errorHandler.handleError(throwable, request, response);
                }
            };
            request.getRequestProgressTracker().log(
                "Applying " + FilterChainType.ERROR + " filters");

            try {
                processor.doFilter(request, response);
            } catch (ServletException se) {
                throw new SlingServletException(se);
            }
        } else {
            errorHandler.handleError(throwable, request, response);
        }
    }

    private static class ErrorResponseWrapper extends
            SlingHttpServletResponseWrapper {

        private PrintWriter writer;

        public ErrorResponseWrapper(SlingHttpServletResponse wrappedResponse) {
            super(wrappedResponse);
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            if (writer == null) {
                try {
                    writer = super.getWriter();
                } catch (IllegalStateException ise) {
                    // resetting the response did not reset the output channel
                    // status and we have to create a writer based on the output
                    // stream using the character encoding already set on the
                    // response, defaulting to ISO-8859-1
                    OutputStream out = getOutputStream();
                    String encoding = getCharacterEncoding();
                    if (encoding == null) {
                        encoding = "ISO-8859-1";
                        setCharacterEncoding(encoding);
                    }
                    Writer w = new OutputStreamWriter(out, encoding);
                    writer = new PrintWriter(w);
                }
            }
            return writer;
        }

        /**
         * Flush the writer if the {@link #getWriter()} method was called
         * to potentially wrap an OuputStream still existing in the response.
         */
        @Override
        public void flushBuffer() throws IOException {
            if (writer != null) {
                writer.flush();
            }
            super.flushBuffer();
        }
    }
}
