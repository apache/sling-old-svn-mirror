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

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestProgressTracker;
import org.apache.sling.engine.impl.request.RequestData;

public abstract class AbstractSlingFilterChain implements FilterChain {

    private FilterHandle[] filters;

    private int current;

    private long[] times;

    protected AbstractSlingFilterChain(FilterHandle[] filters) {
        this.filters = filters;
        this.current = -1;
        this.times = (filters != null) ? new long[filters.length + 1] : null;
    }

    public void doFilter(ServletRequest request, ServletResponse response)
            throws ServletException, IOException {

        final int filterIdx = ++this.current;
        final long start = System.currentTimeMillis();

        // the previous filter may have wrapped non-Sling request and response
        // wrappers (e.g. WebCastellum does this), so we have to make
        // sure the request and response are Sling types again
        SlingHttpServletRequest slingRequest = toSlingRequest(request);
        SlingHttpServletResponse slingResponse = toSlingResponse(response);

        try {

            if (this.current < this.filters.length) {

                // continue filtering with the next filter
                FilterHandle filter = this.filters[this.current];
                
                if (filter.select(slingRequest)) {
                    trackFilter(slingRequest, filter);
                    filter.getFilter().doFilter(slingRequest, slingResponse, this);
                } else {
                    if (this.current == this.filters.length-1) {
                        this.render(slingRequest, slingResponse);
                    } else {
                       doFilter(slingRequest, slingResponse);
                    }
                }
            } else {
                this.render(slingRequest, slingResponse);
            }

        } finally {
            times[filterIdx] = System.currentTimeMillis() - start;
            if (filterIdx == 0) {
                consolidateFilterTimings(slingRequest);
            }
        }
    }

    protected abstract void render(SlingHttpServletRequest request,
            SlingHttpServletResponse response) throws IOException,
            ServletException;

    // ---------- internal helper

    private void trackFilter(ServletRequest request, FilterHandle filter) {
        RequestData data = RequestData.getRequestData(request);
        if (data != null) {
            RequestProgressTracker tracker = data.getRequestProgressTracker();
            tracker.log("Calling filter: {0}",
                filter.getFilter().getClass().getName());
        }
        filter.track();
    }

    private void consolidateFilterTimings(ServletRequest request) {
        if (filters != null) {
            RequestData data = RequestData.getRequestData(request);
            RequestProgressTracker tracker = (data != null) ? data.getRequestProgressTracker() : null;

            for (int i = filters.length - 1; i > 0; i--) {
                filters[i].trackTime(times[i] - times[i + 1]);
                if (tracker != null) {
                    tracker.log("Filter timing: filter={0}, inner={1}, total={2}, outer={3}",
                        filters[i].getFilter().getClass().getName(), times[i + 1], times[i], (times[i] - times[i + 1]));
                }
            }
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