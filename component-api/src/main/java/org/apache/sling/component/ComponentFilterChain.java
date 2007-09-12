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
 * The <code>ComponentFilterChain</code> interface provides the functionality for
 * {@link ComponentFilter} objects to forward request processing along the filter
 * chain.
 * <p>
 * This interface is not intended to be implemented by application programmes.
 * Instead instances of this interface are handed to {@link ComponentFilter}
 * objects which are provided by application programmers.
 */
public interface ComponentFilterChain {

    /**
     * Forwards the request to the next {@link ComponentFilter} in the filter chain
     * or, if no more filters follow, to the component processing step of the
     * {@link Content} of the request.
     *
     * @param request The {@link ComponentRequest} representing the request
     * @param response The {@link ComponentResponse} representing the response
     * @throws IOException May be thrown (or forwarded from further filters
     *             called) if an input or output error occurrs.
     * @throws ComponentException May be thrown (or forwarded from further
     *             filters called) if some request processing error occurrs.
     */
    void doFilter(ComponentRequest request, ComponentResponse response)
            throws IOException, ComponentException;
}
