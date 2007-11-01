/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.microsling.servlet;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;

/**
 * This is helper code, not very interesting to study (but it's not in an
 * "helpers" package as that would require too much public stuff). Manages the
 * microsling chain of servlet Filters: stores the list, calls them when
 * processing a request and calls microSlingServlet.doService after that.
 */
class MicroSlingFilterHelper {

    private MicroslingMainServlet microSling;

    private List<Filter> requestFilterList = new LinkedList<Filter>();

    private Filter[] requestFilters;

    MicroSlingFilterHelper(MicroslingMainServlet microSling) {
        this.microSling = microSling;
    }

    void destroy() {
        Filter[] filters = getFilters();

        // clean up
        requestFilterList.clear();
        requestFilters = null;

        // destroy the filters
        for (int i = 0; i < filters.length; i++) {
            try {
                filters[i].destroy();
            } catch (Throwable t) {
                // TODO: some logging would probably be usefull
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.FilterChain#doFilter(javax.servlet.ServletRequest,
     *      javax.servlet.ServletResponse)
     */
    void service(ServletRequest request, ServletResponse response)
            throws IOException, ServletException {

        MicroSlingFilterChain filterChain = new MicroSlingFilterChain(
            microSling, getFilters());
        filterChain.doFilter(request, response);

    }

    /** return our Filters as a (lazily created) array */
    private Filter[] getFilters() {
        if (requestFilters == null) {
            requestFilters = requestFilterList.toArray(new Filter[requestFilterList.size()]);
        }
        return requestFilters;
    }

    /** Add a Filter at the end of our current chain */
    void addFilter(final Filter filter) throws ServletException {
        FilterConfig config = new FilterConfig() {
            public String getFilterName() {
                return filter.getClass().getName();
            }

            public String getInitParameter(String arg0) {
                // no parameters for now
                return null;
            }

            public Enumeration<?> getInitParameterNames() {
                // no parameters for now
                return Collections.enumeration(Collections.emptyList());
            }

            public ServletContext getServletContext() {
                return microSling.getServletContext();
            }
        };

        // initialize the filter and add it to the list
        filter.init(config);
        requestFilterList.add(filter);

        // force recreation of filter list
        requestFilters = null;
    }

    /**
     * A FilterChain that applies all Filters in an array and calls
     * MicroSlingServlet.doFilter when done
     */
    private static class MicroSlingFilterChain implements FilterChain {

        private final MicroslingMainServlet microSlingServlet;

        private final Filter[] requestFilters;

        private int currentFilter;

        private MicroSlingFilterChain(MicroslingMainServlet microSlingServlet,
                Filter[] requestFilters) {
            this.microSlingServlet = microSlingServlet;
            this.requestFilters = requestFilters;
            this.currentFilter = -1;
        }

        public void doFilter(ServletRequest request, ServletResponse response)
                throws IOException, ServletException {

            currentFilter++;

            if (currentFilter < requestFilters.length) {
                // call the next filter
                requestFilters[currentFilter].doFilter(request, response, this);

            } else {
                // done with filters, call microsling servlet resolution and
                // handling
                microSlingServlet.doService((SlingHttpServletRequest) request,
                    (SlingHttpServletResponse) response);
            }

        }
    }
}
