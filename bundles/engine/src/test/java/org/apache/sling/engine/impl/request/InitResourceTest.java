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
package org.apache.sling.engine.impl.request;

import java.util.Arrays;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.request.RequestProgressTracker;
import org.apache.sling.api.resource.ResourceResolver;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class InitResourceTest {

    private Mockery context;
    private RequestData requestData;
    private HttpServletRequest req;
    private HttpServletResponse resp;
    private ResourceResolver resourceResolver;

    private final String requestURL;
    private final String pathInfo;
    private final String expectedResolvePath;

    @Parameters(name="URL={0} path={1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { "/one;v=1.1", "one;v=1.1", "/one;v=1.1" },
                { "/two;v=1.1", "two", "/two;v=1.1" },
                { "/three", "three", "/three" },
                { "/four%3Bv=1.1", "four", "/four" },
                { "/five%3Bv=1.1", "five;v=1.1", "/five;v=1.1" },
                { "/six;v=1.1", "six;v=1.1", "/six;v=1.1" },
                { "/seven", "seven;v=1.1", "/seven;v=1.1" },
        });
    }

    public InitResourceTest(String requestURL, String pathInfo, String expectedResolvePath) {
        this.requestURL = requestURL;
        this.pathInfo = pathInfo;
        this.expectedResolvePath = expectedResolvePath;
    }

    @Before
    public void setup() throws Exception {
        context = new Mockery();

        req = context.mock(HttpServletRequest.class);
        resp = context.mock(HttpServletResponse.class);
        resourceResolver = context.mock(ResourceResolver.class);

        context.checking(new Expectations() {{
            allowing(req).getRequestURL();
            will(returnValue(new StringBuffer(requestURL)));

            allowing(req).getRequestURI();

            allowing(req).getPathInfo();
            will(returnValue(pathInfo));

            allowing(req).getServletPath();
            will(returnValue("/"));

            allowing(req).getMethod();
            will(returnValue("GET"));

            allowing(req).getAttribute(RequestData.REQUEST_RESOURCE_PATH_ATTR);
            will(returnValue(null));
            allowing(req).setAttribute(with(equal(RequestData.REQUEST_RESOURCE_PATH_ATTR)), with(any(Object.class)));

            allowing(req).getAttribute(RequestProgressTracker.class.getName());
            will(returnValue(null));

            // Verify that the ResourceResolver is called with the expected path
            allowing(resourceResolver).resolve(with(any(HttpServletRequest.class)),with(equal(expectedResolvePath)));
        }});

        requestData = new RequestData(null, req, resp);
    }

    @Test
    public void resolverPathMatches() {
        requestData.initResource(resourceResolver);
    }
}
