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
package org.apache.sling.servlets.resolver.internal;

import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.OptingServlet;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.commons.testing.osgi.MockBundle;
import org.apache.sling.commons.testing.osgi.MockBundleContext;
import org.apache.sling.commons.testing.osgi.MockServiceReference;
import org.apache.sling.commons.testing.sling.MockResource;
import org.apache.sling.commons.testing.sling.MockResourceResolver;
import org.apache.sling.commons.testing.sling.MockSlingHttpServletRequest;
import org.apache.sling.servlets.resolver.internal.resource.MockServletResource;
import org.apache.sling.servlets.resolver.internal.resource.ServletResourceProvider;
import org.apache.sling.servlets.resolver.internal.resource.ServletResourceProviderFactory;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

@RunWith(JMock.class)
public class SlingServletResolverTest {

    protected final Mockery context = new JUnit4Mockery();

    private Servlet servlet;

    private SlingServletResolver servletResolver;

    public static final String SERVLET_PATH = "/mock";

    public static final String SERVLET_NAME = "TestServlet";

    private static final String SERVLET_EXTENSION = "html";

    private MockResourceResolver mockResourceResolver;

    @Before public void setUp() throws Exception {
        mockResourceResolver = new MockResourceResolver() {
            @Override
            public void close() {
                // nothing to do;
            }

            @Override
            public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
                return null;
            }

            @Override
            public ResourceResolver clone(Map<String, Object> authenticationInfo)
                    throws LoginException {
                throw new LoginException("MockResourceResolver can't be cloned - excepted for this test!");
            }

            @Override
            public void refresh() {
                // nothing to do
            }
        };
        mockResourceResolver.setSearchPath("/");

        final ResourceResolverFactory factory = new ResourceResolverFactory() {

            @Override
            public ResourceResolver getAdministrativeResourceResolver(
                    Map<String, Object> authenticationInfo)
                    throws LoginException {
                return mockResourceResolver;
            }

            @Override
            public ResourceResolver getResourceResolver(
                    Map<String, Object> authenticationInfo)
                    throws LoginException {
                return mockResourceResolver;
            }

            @Override
            public ResourceResolver getServiceResourceResolver(Map<String, Object> authenticationInfo)
                    throws LoginException {
                return mockResourceResolver;
            }

            @Override
            public ResourceResolver getThreadResourceResolver() {
                // TODO Auto-generated method stub
                return null;
            }
        };

        servlet = new MockSlingRequestHandlerServlet();
        servletResolver = new SlingServletResolver();

        Class<?> resolverClass = servletResolver.getClass();

        // set resource resolver factory
        final Field resolverField = resolverClass.getDeclaredField("resourceResolverFactory");
        resolverField.setAccessible(true);
        resolverField.set(servletResolver, factory);

        MockBundle bundle = new MockBundle(1L);
        MockBundleContext bundleContext = new MockBundleContext(bundle) {
            @Override
            public ServiceRegistration registerService(String s, Object o, Dictionary dictionary) {
                return null;
            }

            @Override
            public ServiceRegistration registerService(String[] strings, Object o, Dictionary dictionary) {
                return null;
            }
        };
        MockServiceReference serviceReference = new MockServiceReference(bundle);
        serviceReference.setProperty(Constants.SERVICE_ID, 1L);
        serviceReference.setProperty(ServletResolverConstants.SLING_SERVLET_NAME,
            SERVLET_NAME);
        serviceReference.setProperty(
                ServletResolverConstants.SLING_SERVLET_PATHS, SERVLET_PATH);
        serviceReference.setProperty(
            ServletResolverConstants.SLING_SERVLET_EXTENSIONS,
            SERVLET_EXTENSION);

        servletResolver.bindServlet(SlingServletResolverTest.this.servlet, serviceReference);
        servletResolver.activate(bundleContext, new SlingServletResolver.Config() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return SlingServletResolver.Config.class;
            }

            @Override
            public String servletresolver_servletRoot() {
                return "0";
            }

            @Override
            public String[] servletresolver_paths() {
                return new String[] {"/"};
            }

            @Override
            public String[] servletresolver_defaultExtensions() {
                // TODO Auto-generated method stub
                return new String[] {"html"};
            }

            @Override
            public int servletresolver_cacheSize() {
                return 200;
            }
        });

        String path = "/"
            + MockSlingHttpServletRequest.RESOURCE_TYPE
            + "/"
            + ResourceUtil.getName(MockSlingHttpServletRequest.RESOURCE_TYPE)
            + ".servlet";
        MockServletResource res = new MockServletResource(mockResourceResolver,
            servlet, path);
        mockResourceResolver.addResource(res);

        MockResource parent = new MockResource(mockResourceResolver,
            ResourceUtil.getParent(res.getPath()), "nt:folder");
        mockResourceResolver.addResource(parent);

        List<Resource> childRes = new ArrayList<>();
        childRes.add(res);
        mockResourceResolver.addChildren(parent, childRes);
    }

    protected String getRequestWorkspaceName() {
        return "fromRequest";
    }

    @Test public void testAcceptsRequest() {
        MockSlingHttpServletRequest secureRequest = new MockSlingHttpServletRequest(
            SERVLET_PATH, null, SERVLET_EXTENSION, null, null);
        secureRequest.setResourceResolver(mockResourceResolver);
        secureRequest.setSecure(true);
        Servlet result = servletResolver.resolveServlet(secureRequest);
        assertEquals("Did not resolve to correct servlet", servlet, result);
    }

    @Test public void testIgnoreRequest() {
        MockSlingHttpServletRequest insecureRequest = new MockSlingHttpServletRequest(
            SERVLET_PATH, null, SERVLET_EXTENSION, null, null);
        insecureRequest.setResourceResolver(mockResourceResolver);
        insecureRequest.setSecure(false);
        Servlet result = servletResolver.resolveServlet(insecureRequest);
        assertTrue("Did not ignore unwanted request",
            result.getClass() != MockSlingRequestHandlerServlet.class);
    }

    @Test public void testCreateServiceRegistrationProperties() throws Throwable {
        MockServiceReference msr = new MockServiceReference(null);

        msr.setProperty(ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES, "sample");
        msr.setProperty(ServletResolverConstants.SLING_SERVLET_METHODS, "GET");

        Field srpf = SlingServletResolver.class.getDeclaredField("servletResourceProviderFactory");
        srpf.setAccessible(true);
        ServletResourceProviderFactory factory = (ServletResourceProviderFactory) srpf.get(servletResolver);

        ServletResourceProvider servlet = factory.create(msr, null);

        Method createServiceProperties = SlingServletResolver.class.getDeclaredMethod("createServiceProperties", ServiceReference.class, ServletResourceProvider.class, String.class);
        createServiceProperties.setAccessible(true);

        // no ranking
        assertNull(msr.getProperty(Constants.SERVICE_RANKING));
        @SuppressWarnings("unchecked")
        final Dictionary<String, Object> p1 = (Dictionary<String, Object>) createServiceProperties.invoke(servletResolver, msr, servlet, "/a");
        assertNull(p1.get(Constants.SERVICE_RANKING));

        // illegal type of ranking
        Object nonIntValue = "Some Non Integer Value";
        msr.setProperty(Constants.SERVICE_RANKING, nonIntValue);
        assertEquals(nonIntValue, msr.getProperty(Constants.SERVICE_RANKING));
        @SuppressWarnings("unchecked")
        final Dictionary<String, Object> p2 = (Dictionary<String, Object>) createServiceProperties.invoke(servletResolver, msr, servlet, "/a");
        assertNull(p2.get(Constants.SERVICE_RANKING));

        // illegal type of ranking
        Object intValue = Integer.valueOf(123);
        msr.setProperty(Constants.SERVICE_RANKING, intValue);
        assertEquals(intValue, msr.getProperty(Constants.SERVICE_RANKING));
        @SuppressWarnings("unchecked")
        final Dictionary<String, Object> p3 = (Dictionary<String, Object>) createServiceProperties.invoke(servletResolver, msr, servlet, "/a");
        assertEquals(intValue, p3.get(Constants.SERVICE_RANKING));
    }

    /**
     * This sample servlet will only handle secure requests.
     *
     * @see org.apache.sling.api.servlets.OptingServlet#accepts
     */
    @SuppressWarnings("serial")
    private static class MockSlingRequestHandlerServlet extends HttpServlet
            implements OptingServlet {

        @Override
        public boolean accepts(SlingHttpServletRequest request) {
            return request.isSecure();
        }
    }

}
