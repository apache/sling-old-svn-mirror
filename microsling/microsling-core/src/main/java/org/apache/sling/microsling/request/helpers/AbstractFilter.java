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
package org.apache.sling.microsling.request.helpers;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The <code>AbstractFilter</code> is a basic abstract implementation of the
 * <code>javax.servlet.Filter</code> interface.
 */
public abstract class AbstractFilter implements Filter {

    private FilterConfig filterConfig;

    /**
     * Default implementation of filter initialization keeping the filter
     * configuration made available through the {@link #getFilterConfig()}
     * method and calls the {@link #init()} method.
     * <p>
     * Implementations should not overwrite this method but rather implement
     * their initialization in the {@link #init()} method.
     *
     * @param filterConfig The filter configuration
     * @throws ServletException forwarded from calling the {@link #init()}
     *             method.
     */
    public void init(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;

        init();
    }

    /**
     * Further initialization by the implementation of this class.
     *
     * @throws ServletException may be thrown if an error occurrs initializing
     *             the filter.
     */
    protected abstract void init() throws ServletException;

    /**
     * Calls the
     * {@link #doFilter(HttpServletRequest, HttpServletResponse, FilterChain)}
     * method casting the <code>request</code> and <code>response</code>
     * objects to the respective HTTP request and response objects. In case of a
     * non-http request and/or response a <code>ServletException</code> is
     * thrown.
     *
     * @param request The servlet request
     * @param response The servlet response
     * @param filterChain The filter chain to which processing is to be handed.
     * @throws IOException forwarded from calling the
     *             {@link #doFilter(HttpServletRequest, HttpServletResponse, FilterChain)}
     *             method
     * @throws ServletException if <code>request</code> and/or
     *             <code>response</code> is not a HTTP request or response or
     *             forwarded from calling the
     *             {@link #doFilter(HttpServletRequest, HttpServletResponse, FilterChain)}
     *             method.
     */
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain filterChain) throws IOException, ServletException {

        try {
            HttpServletRequest req = (HttpServletRequest) request;
            HttpServletResponse res = (HttpServletResponse) response;
            doFilter(req, res, filterChain);
        } catch (ClassCastException cce) {
            throw new ServletException("HTTP Requests supported only");
        }
    }

    /**
     * Called by the
     * {@link #doFilter(ServletRequest, ServletResponse, FilterChain)} method
     * for HTTP requests.
     *
     * @param request The HTTP servlet request
     * @param response The HTTP servlet response
     * @param filterChain The filter chain to which processing is to be handed.
     * @throws IOException may be thrown in case of an Input/Output problem
     * @throws ServletException may be thrown in case of any other servlet
     *             processing issue.
     */
    protected abstract void doFilter(HttpServletRequest request,
            HttpServletResponse response, FilterChain filterChain)
            throws IOException, ServletException;

    /**
     * This default implementation does nothing. Implementations of this class
     * may overwrite but should for stability reasons call this base class
     * implementation.
     */
    public void destroy() {
    }

    /**
     * Returns the <code>FilterConfig</code> with which this filter has been
     * initialized.
     */
    protected FilterConfig getFilterConfig() {
        return filterConfig;
    }
}
