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
package org.apache.sling.bgservlets.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;

public class BackgroundServletStarterFilterTest {
    private BackgroundServletStarterFilter filter;
    private Mockery mockery;
    private SlingHttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain chain;

    @SuppressWarnings("serial")
    static class BackgroundJobIsStarting extends RuntimeException {
    }

    @Before
    public void setup() {
        mockery = new Mockery();
        request = mockery.mock(SlingHttpServletRequest.class);
        response = mockery.mock(HttpServletResponse.class);
        chain = mockery.mock(FilterChain.class);

        final Map<String, Object> props = new HashMap<String, Object>();

        filter = new BackgroundServletStarterFilter();

        mockery.checking(new Expectations() {{
            allowing(request).getParameter("sling:bg");
            will(returnValue("true"));

            // If this method is called it means the BackgroundHttpServletRequest
            // is being created to start a background job, that's all we need to know
            allowing(request).getContextPath();
            will(throwException(new BackgroundJobIsStarting()));

        }});

        filter.activate(props);
    }

    private void testWithMethod(final String method) throws IOException, ServletException {
        mockery.checking(new Expectations() {{
            allowing(request).getMethod();
            will(returnValue(method));
        }});
        filter.doFilter(request, response, chain);
    }

    @Test(expected=ServletException.class)
    public void testGetRequest() throws IOException, ServletException {
        testWithMethod("GET");
    }

    @Test(expected=BackgroundJobIsStarting.class)
    public void testPostRequest() throws IOException, ServletException {
        testWithMethod("POST");
    }
}