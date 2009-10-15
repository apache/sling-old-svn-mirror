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
import org.apache.sling.engine.impl.request.RequestData;

public abstract class AbstractSlingFilterChain implements FilterChain {

    private Filter[] filters;

    private int current;

    protected AbstractSlingFilterChain(Filter[] filters) {
        this.filters = filters;
        this.current = -1;
    }

    public void doFilter(ServletRequest request, ServletResponse response)
            throws ServletException, IOException {
        this.current++;

        // the previous filter may have wrapped non-Sling request and response
        // wrappers (e.g. WebCastellum does this), so we have to make
        // sure the request and response are Sling types again
        SlingHttpServletRequest slingRequest = toSlingRequest(request);
        SlingHttpServletResponse slingResponse = toSlingResponse(response);

        if (this.current < this.filters.length) {

            // continue filtering with the next filter
            Filter filter = this.filters[this.current];
            trackFilter(slingRequest, filter);
            filter.doFilter(slingRequest, slingResponse, this);

        } else {

            this.render(slingRequest, slingResponse);

        }
    }

    protected abstract void render(SlingHttpServletRequest request,
            SlingHttpServletResponse response) throws IOException,
            ServletException;

    // ---------- internal helper

    private void trackFilter(ServletRequest request, Filter filter) {
        RequestData data = RequestData.getRequestData(request);
        if (data != null) {
            RequestProgressTracker tracker = data.getRequestProgressTracker();
            tracker.log("Calling filter: {0}",
                this.filters[this.current].getClass().getName());
        }
    }

    private SlingHttpServletRequest toSlingRequest(ServletRequest request) {
        if (request instanceof SlingHttpServletRequest) {
            return (SlingHttpServletRequest) request;
        }

        // wrap
        return RequestData.toSlingHttpServletRequest(request);
    }

    private SlingHttpServletResponse toSlingResponse(ServletResponse response) {
        if (response instanceof SlingHttpServletResponse) {
            return (SlingHttpServletResponse) response;
        }

        // wrap
        return RequestData.toSlingHttpServletResponse(response);
    }
}