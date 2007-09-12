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
package org.apache.sling.component;

import java.io.IOException;

/**
 * The <code>ComponentFilter</code> interface defines the API to be implemented
 * by filters. Like Servlet API filters, these filters will be called by a
 * {@link ComponentFilterChain} and either handle the request, manipulate the
 * request and/or response object and finally forward the request and response
 * optionally wrapped to the
 * {@link ComponentFilterChain#doFilter(ComponentRequest, ComponentResponse)} method.
 * <p>
 * Note: This specification does not define how <code>ComponentFilter</code>
 * objects are registered with the Component Framework nor is it specified how
 * the order is defined in which the filters are called.
 */
public interface ComponentFilter {

    /**
     * Initializes this filter with the given {@link ComponentContext}.
     * <p>
     * This method is called by the Componnent Framework before the
     * {@link #doFilter(ComponentRequest, ComponentResponse, ComponentFilterChain)}
     * method is first used. If this method fails by throwing an exception, the
     * filter will not be used and the <code>doFilter</code> method is never
     * called.
     *
     * @param componentContext The {@link ComponentContext} from which to
     *            initialize the filter.
     * @throws ComponentException May be thrown if an error occurrs setting up
     *             the filter. If this exception is thrown, the filter must not
     *             be used by the Component Framework.
     */
    void init(ComponentContext componentContext) throws ComponentException;

    /**
     * Handles any filtering on the <code>request</code> and
     * <code>response</code> objects. It is up to the implementation of this
     * interface what happens, for example:
     * <ul>
     * <li>The <code>request</code> and <code>response</code> objects may
     * be wrapped by an extension of the {@link ComponentRequestWrapper} or
     * {@link ComponentResponseWrapper} classes before continuing with filter
     * processing.
     * <li>Set request, session or context attributes
     * <li>Handle the request and terminate processing by not calling the
     * {@link ComponentFilterChain#doFilter(ComponentRequest, ComponentResponse)} method
     * at all.
     * </ul>
     *
     * @param request The {@link ComponentRequest} representing the request
     * @param response The {@link ComponentResponse} representing the response
     * @param filterChain The {@link ComponentFilterChain} object whose
     *            <code>doFilter</code> method is to be called to continue
     *            with normal request processing.
     * @throws IOException May be thrown (or forwarded from further filters
     *             called) if an input or output error occurrs.
     * @throws ComponentException May be thrown (or forwarded from further
     *             filters called) if some request processing error occurrs.
     */
    void doFilter(ComponentRequest request, ComponentResponse response,
            ComponentFilterChain filterChain) throws IOException,
            ComponentException;

    /**
     * Destroys this filter by cleaing up any resources used. This method is not
     * expected to throw an exception, so it should be written safely.
     * <p>
     * This method is called by the Component Framework just before taking the
     * filter out of service. After this method has terminated, the filter will
     * not be used anymore by the Component Framework.
     */
    void destroy();

}
