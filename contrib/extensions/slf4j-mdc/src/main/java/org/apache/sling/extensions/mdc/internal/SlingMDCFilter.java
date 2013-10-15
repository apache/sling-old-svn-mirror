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
package org.apache.sling.extensions.mdc.internal;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.MDC;

import javax.jcr.Session;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

class SlingMDCFilter implements Filter {
    public static final String SLING_USER = "sling.userId";
    public static final String JCR_SESSION_ID = "jcr.sessionId";

    private static final String[] DEFAULT_KEY_NAMES = {
            SLING_USER,
    };

    public void init(FilterConfig filterConfig) throws ServletException {

    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                         FilterChain filterChain) throws IOException, ServletException {
        final SlingHttpServletRequest request = (SlingHttpServletRequest) servletRequest;

        try {
            insertIntoMDC(request);
            filterChain.doFilter(request, servletResponse);
        } finally {
            clearMDC();
        }
    }

    private void clearMDC() {
        for (String key : DEFAULT_KEY_NAMES) {
            MDC.remove(key);
        }
    }

    private void insertIntoMDC(SlingHttpServletRequest request) {
        ResourceResolver rr = request.getResourceResolver();
        if(rr.getUserID() != null){
            MDC.put(SLING_USER,rr.getUserID());
        }
    }

    public void destroy() {

    }
}