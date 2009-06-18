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
package org.apache.sling.engine.impl.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestProgressTracker;

public abstract class AbstractSlingFilterChain implements FilterChain {

    private Filter[] filters;

    private int current;

    protected AbstractSlingFilterChain(Filter[] filters) {
        this.filters = filters;
        this.current = -1;
    }

    /**
     * @see javax.servlet.FilterChain#doFilter(javax.servlet.ServletRequest,
     *      javax.servlet.ServletResponse)
     */
    public void doFilter(ServletRequest request, ServletResponse response)
            throws ServletException, IOException {
        this.current++;

        if (this.current < this.filters.length) {
            
            RequestProgressTracker tracker = ((SlingHttpServletRequest) request).getRequestProgressTracker();
            tracker.log("Calling filter: {0}", this.filters[this.current].getClass().getName());
            
            this.filters[this.current].doFilter(request, response, this);
        } else {
            this.render((SlingHttpServletRequest) request,
                (SlingHttpServletResponse) response);
        }
    }

    protected abstract void render(SlingHttpServletRequest request,
            SlingHttpServletResponse response) throws IOException,
            ServletException;
}