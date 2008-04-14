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
package org.apache.sling.servlet.resolver;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;

import junit.framework.TestCase;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.servlets.OptingServlet;
import org.apache.sling.core.CoreConstants;
import org.apache.sling.servlet.resolver.mock.MockBundle;
import org.apache.sling.servlet.resolver.mock.MockComponentContext;
import org.apache.sling.servlet.resolver.mock.MockResourceResolver;
import org.apache.sling.servlet.resolver.mock.MockServiceReference;
import org.apache.sling.servlet.resolver.mock.MockSlingHttpServletRequest;
import org.apache.sling.servlet.resolver.resource.MockServletResource;
import org.osgi.framework.Constants;

public class SlingServletResolverTest extends TestCase {
    private Servlet servlet;

    private SlingServletResolver servletResolver;

    public static final String SERVLET_PATH = "/mock";

    public static final String SERVLET_NAME = "TestServlet";

    private static final String ROOT = "/";

    private static final String SERVLET_EXTENSION = ".";

    private MockResourceResolver mockResourceResolver;

    protected void setUp() throws Exception {
        super.setUp();
        servlet = new MockSlingRequestHandlerServlet();
        servletResolver = new SlingServletResolver();
        MockBundle bundle = new MockBundle(1L);
        MockComponentContext mockComponentContext = new MockComponentContext(
            bundle, SlingServletResolverTest.this.servlet);
        MockServiceReference serviceReference = new MockServiceReference(bundle);
        serviceReference.setProperty(Constants.SERVICE_ID, 1L);
        serviceReference.setProperty(CoreConstants.SLING_SERLVET_NAME,
            SERVLET_NAME);
        serviceReference.setProperty(
            ServletResolverConstants.SLING_SERVLET_PATHS, SERVLET_PATH);
        serviceReference.setProperty(
            ServletResolverConstants.SLING_SERVLET_EXTENSIONS,
            SERVLET_EXTENSION);
        mockComponentContext.locateService("MockService", serviceReference);

        servletResolver.bindServlet(serviceReference);
        servletResolver.activate(mockComponentContext);
        mockResourceResolver = new MockResourceResolver();
        mockResourceResolver.setSearchPath(new String[] { "/" });
        mockResourceResolver.addResource("/"
            + MockSlingHttpServletRequest.RESOURCE_TYPE,
            new MockServletResource(mockResourceResolver, servlet, ROOT));
    }

    public void testAcceptsRequest() {
        MockSlingHttpServletRequest secureRequest = new MockSlingHttpServletRequest(
            SERVLET_PATH, "", SERVLET_EXTENSION, "", "");
        secureRequest.setResourceResolver(mockResourceResolver);
        secureRequest.setSecure(true);
        Servlet result = servletResolver.resolveServlet(secureRequest);
        assertEquals("Did not resolve to correct servlet", servlet, result);
    }

    public void testIgnoreRequest() {
        MockSlingHttpServletRequest insecureRequest = new MockSlingHttpServletRequest(
            SERVLET_PATH, "", SERVLET_EXTENSION, "", "");
        insecureRequest.setResourceResolver(mockResourceResolver);
        insecureRequest.setSecure(false);
        Servlet result = servletResolver.resolveServlet(insecureRequest);
        assertTrue("Did not ignore unwanted request",
            result.getClass() != MockSlingRequestHandlerServlet.class);
    }

    /**
     * This sample servlet will only handle secure requests.
     * 
     * @see org.apache.sling.api.servlets.OptingServlet#accepts
     */
    private static class MockSlingRequestHandlerServlet extends HttpServlet
            implements OptingServlet {

        public boolean accepts(SlingHttpServletRequest request) {
            return request.isSecure();
        }
    }

}
