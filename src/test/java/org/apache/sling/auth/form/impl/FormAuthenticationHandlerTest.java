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
package org.apache.sling.auth.form.impl;

import static org.easymock.EasyMock.cmpEq;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.lang.reflect.Method;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.auth.Authenticator;
import org.apache.sling.auth.core.spi.AuthenticationInfo;
import org.apache.sling.auth.core.spi.DefaultAuthenticationFeedbackHandler;
import org.hamcrest.Description;
import org.hamcrest.text.StringStartsWith;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Action;
import org.jmock.api.Invocation;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.BundleContext;
import org.powermock.api.easymock.PowerMock;
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(FormAuthenticationHandler.class)
public class FormAuthenticationHandlerTest {

    @Test public void test_getTokenFile() {
        final File root = new File("bundle999").getAbsoluteFile();
        final SlingHomeAction slingHome = new SlingHomeAction();
        slingHome.setSlingHome(new File("sling").getAbsolutePath());

        Mockery context = new Mockery();
        final BundleContext bundleContext = context.mock(BundleContext.class);

        context.checking(new Expectations() {
            {
                // mock access to sling.home framework property
                allowing(bundleContext).getProperty("sling.home");
                will(slingHome);

                // mock no data file support with file names starting with sl
                allowing(bundleContext).getDataFile(
                    with(new StringStartsWith("sl")));
                will(returnValue(null));

                // mock data file support for any other name
                allowing(bundleContext).getDataFile(with(any(String.class)));
                will(new RVA(root));
            }
        });

        final FormAuthenticationHandler handler = new FormAuthenticationHandler();

        // test files relative to bundle context
        File relFile0 = handler.getTokenFile("", bundleContext);
        assertEquals(root, relFile0);

        String relName1 = "rel/path";
        File relFile1 = handler.getTokenFile(relName1, bundleContext);
        assertEquals(new File(root, relName1), relFile1);

        // test file relative to sling.home if no data file support
        String relName2 = "sl/rel_to_sling.home";
        File relFile2 = handler.getTokenFile(relName2, bundleContext);
        assertEquals(new File(slingHome.getSlingHome(), relName2), relFile2);

        // test file relative to current working directory
        String relName3 = "sl/test";
        slingHome.setSlingHome(null);
        File relFile3 = handler.getTokenFile(relName3, bundleContext);
        assertEquals(new File(relName3).getAbsoluteFile(), relFile3);

        // test absolute file return
        File absFile = new File("test").getAbsoluteFile();
        File absFile0 = handler.getTokenFile(absFile.getPath(), bundleContext);
        assertEquals(absFile, absFile0);
    }

    @Test public void test_getUserid() {
        final FormAuthenticationHandler handler = new FormAuthenticationHandler();
        assertEquals(null, handler.getUserId(null));
        assertEquals(null, handler.getUserId(""));
        assertEquals(null, handler.getUserId("field0"));
        assertEquals(null, handler.getUserId("field0@field1"));
        assertEquals("field3", handler.getUserId("field0@field1@field3"));
        assertEquals("field3@field4", handler.getUserId("field0@field1@field3@field4"));
    }

    /**
     * Test for SLING-3443 Parameter based redirection should only handle relative paths
     * @throws Exception PowerMock.expectPrivate throws Exception and UrlEncoder.encode
     *                          throws UnsupportedEncodingException
     * @since 1.0.6
     */
    @Test public void testRedirectionAfterLogin() throws Exception {
        // Create mocks
        final HttpServletRequest request = createMock(HttpServletRequest.class);
        final HttpServletResponse response = createMock(HttpServletResponse.class);
        final AuthenticationInfo authenticationInfo = createMock(AuthenticationInfo.class);

        // Use PowerMock to mock private method
        final String methodName = "refreshAuthData";
        final FormAuthenticationHandler authenticationHandler = PowerMock.createPartialMock(FormAuthenticationHandler.class,
                methodName);
        final Method[] methods = MemberMatcher.methods(FormAuthenticationHandler.class, methodName);
        PowerMock.expectPrivate(authenticationHandler, methods[0], request, response, authenticationInfo);

        // Mock the static method since we are just unit testing the authentication succeeded flow
        PowerMock.mockStatic(DefaultAuthenticationFeedbackHandler.class);
        expect(DefaultAuthenticationFeedbackHandler.handleRedirect(request, response)).andReturn(false);

        // Mocks the Authenticator.LOGIN_RESOURCE attribute
        final String url = "http://www.blah.com";
        expect(request.getAttribute(Authenticator.LOGIN_RESOURCE)).andReturn(url);

        // Mocks the HttpServletRequest and HttpServletResponse object
        expect(request.getMethod()).andReturn("POST");
        expect(request.getRequestURI()).andReturn("http://blah/blah/j_security_check");
        String contextPath = "/blah";
        expect(request.getContextPath()).andReturn(contextPath).anyTimes();
        expect(response.isCommitted()).andReturn(false);

        // Mocking method with void return type
        response.resetBuffer();
        expectLastCall().once();

        // The request should be redirected to the context root rather than the
        // passing the parameter directly
        response.sendRedirect(cmpEq(contextPath));

        // Replay the mocks
        replay(request);
        replay(response);
        replay(authenticationInfo);
        replay(authenticationHandler);

        // Test the method
        authenticationHandler.authenticationSucceeded(request, response, authenticationInfo);

        // Verify mocks
        verify(request, response, authenticationInfo, authenticationHandler);
    }

    /**
     * The <code>RVA</code> action returns a file relative to some root file as
     * requested by the first parameter of the invocation, expecting the first
     * parameter to be a string.
     */
    private static class RVA implements Action {

        private final File root;

        RVA(final File root) {
            this.root = root;
        }

        public Object invoke(Invocation invocation) throws Throwable {
            String data = (String) invocation.getParameter(0);
            if (data.startsWith("/")) {
                data = data.substring(1);
            }
            return new File(root, data);
        }

        public void describeTo(Description description) {
            description.appendText("returns new File(root, arg0)");
        }
    }

    /**
     * The <code>SlingHomeAction</code> action returns the current value of the
     * <code>slingHome</code> field on all invocations
     */
    private static class SlingHomeAction implements Action {
        private String slingHome;

        public void setSlingHome(String slingHome) {
            this.slingHome = slingHome;
        }

        public String getSlingHome() {
            return slingHome;
        }

        public Object invoke(Invocation invocation) throws Throwable {
            return slingHome;
        }

        public void describeTo(Description description) {
            description.appendText("returns " + slingHome);
        }
    }
}
