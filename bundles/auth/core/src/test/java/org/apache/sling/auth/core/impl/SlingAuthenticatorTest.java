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
package org.apache.sling.auth.core.impl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.auth.core.spi.AuthenticationFeedbackHandler;
import org.apache.sling.auth.core.spi.AuthenticationInfo;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;

import junit.framework.TestCase;
import junitx.util.PrivateAccessor;

public class SlingAuthenticatorTest extends TestCase {

    private final Mockery context = new JUnit4Mockery();

    public void test_quoteCookieValue() throws UnsupportedEncodingException {

        try {
            SlingAuthenticator.quoteCookieValue(null);
            fail("Expected IllegalArgumentExcepion on null value");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        checkQuote("\"", "\"\\\"\"");
        checkQuote("simplevalue", "\"simplevalue\"");
        checkQuote("simple value", "\"simple+value\"");
        checkQuote("email@address.com", "\"email%40address.com\"");

        checkQuote("string\ttab", "\"string%09tab\"");
        checkQuote("test中文", "\"test%E4%B8%AD%E6%96%87\"");


        try {
            SlingAuthenticator.quoteCookieValue("string\rCR");
            fail("Expected IllegalArgumentExcepion on value containing CR");
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }

    public void test_unquoteCookieValue() {

        assertNull(SlingAuthenticator.unquoteCookieValue(null));
        assertEquals("", SlingAuthenticator.unquoteCookieValue(""));

        checkUnQuote("unquoted", "unquoted");
        checkUnQuote("unquoted\"", "unquoted\"");
        checkUnQuote("un\"quoted", "un\"quoted");

        checkUnQuote("\"\\\"\"", "\"");
        checkUnQuote("\"simplevalue\"", "simplevalue");
        checkUnQuote("\"simple value\"", "simple value");
        checkUnQuote("\"email@address.com\"", "email@address.com");

        checkUnQuote("\"string\ttab\"", "string\ttab");
    }

    private void checkQuote(final String value, final String expected) throws UnsupportedEncodingException {
        final String actual = SlingAuthenticator.quoteCookieValue(value);
        assertEquals(expected, actual);
    }

    private void checkUnQuote(final String value, final String expected) {
        final String actual = SlingAuthenticator.unquoteCookieValue(value);
        assertEquals(expected, actual);
    }

    //SLING-4864
    public void  test_isAnonAllowed() throws Throwable {
        SlingAuthenticator slingAuthenticator = new SlingAuthenticator();

        PathBasedHolderCache<AuthenticationRequirementHolder> authRequiredCache = new PathBasedHolderCache<AuthenticationRequirementHolder>();

        authRequiredCache.addHolder(new AuthenticationRequirementHolder("/", false, null));

        PrivateAccessor.setField(slingAuthenticator, "authRequiredCache", authRequiredCache);
        final HttpServletRequest request = context.mock(HttpServletRequest.class);
        context.checking(new Expectations() {
            {
                allowing(request).getServletPath();
                will(returnValue(null));
                allowing(request).getPathInfo();
                will(returnValue(null));
                allowing(request).getServerName();
                will(returnValue("localhost"));
                allowing(request).getServerPort();
                will(returnValue(80));
                allowing(request).getScheme();
                will(returnValue("http"));
            }
        });

        Boolean allowed = (Boolean) PrivateAccessor.invoke(slingAuthenticator,"isAnonAllowed",  new Class[]{HttpServletRequest.class},new Object[]{request});
        assertTrue(allowed);
    }

    /**
     * Test is OK after commit 	424e412
     * JIRA: SLING-6053
     * Issue can be reproduced with the following steps:
     *
     * Create node "/page"
     * Create sibling node "/page1"
     * Define an auth handler for node: "/page"
     *
     * Expected: "/page" has AuthenticationInfo
     *           "/page1" does not have AuthenticationInfo (has anonymous)
     *
     * Actual:  "/page" & "page1" are both having AuthenticationInfo
     *
     * Reason: SlingAuthenticator.java line 726:  if (path.startsWith(holder.path))
     * Warning: The same check is used in 4 more places in code with similar behaviour.
     *
     * @throws Throwable
     */
    public void test_siblingNodeShouldNotHaveAuthenticationInfo() throws Throwable {
        final String AUTH_TYPE = "AUTH_TYPE_TEST";
        final String PROTECTED_PATH = "/content/en/test";
        final String REQUEST_NOT_PROTECTED_PATH = "/content/en/test2";

        SlingAuthenticator slingAuthenticator = new SlingAuthenticator();

        PathBasedHolderCache<AbstractAuthenticationHandlerHolder> authRequiredCache = new PathBasedHolderCache<AbstractAuthenticationHandlerHolder>();
        authRequiredCache.addHolder(buildAuthHolderForAuthTypeAndPath(AUTH_TYPE, PROTECTED_PATH));

        PrivateAccessor.setField(slingAuthenticator, "authHandlerCache", authRequiredCache);
        final HttpServletRequest request = context.mock(HttpServletRequest.class);
        buildExpectationsForRequestPathAndAuthPath(request, REQUEST_NOT_PROTECTED_PATH, PROTECTED_PATH);

        AuthenticationInfo authInfo = (AuthenticationInfo) PrivateAccessor.invoke(slingAuthenticator, "getAuthenticationInfo",
                new Class[]{HttpServletRequest.class, HttpServletResponse.class}, new Object[]{request, context.mock(HttpServletResponse.class)});
        /**
         * The AUTH TYPE defined aboved should not be used for the path /test2.
         */
        assertFalse(AUTH_TYPE.equals(authInfo.getAuthType()));
    }

    /**
     * Test is OK for child node;
     * @throws Throwable
     */
    public void test_childNodeShouldHaveAuthenticationInfo() throws Throwable {
        final String AUTH_TYPE = "AUTH_TYPE_TEST";
        final String PROTECTED_PATH = "/content/en/test";
        final String REQUEST_CHILD_NODE = "/content/en/test/childnodetest";

        SlingAuthenticator slingAuthenticator = new SlingAuthenticator();

        PathBasedHolderCache<AbstractAuthenticationHandlerHolder> authRequiredCache = new PathBasedHolderCache<AbstractAuthenticationHandlerHolder>();
        authRequiredCache.addHolder(buildAuthHolderForAuthTypeAndPath(AUTH_TYPE, PROTECTED_PATH));

        PrivateAccessor.setField(slingAuthenticator, "authHandlerCache", authRequiredCache);
        final HttpServletRequest request = context.mock(HttpServletRequest.class);
        buildExpectationsForRequestPathAndAuthPath(request, REQUEST_CHILD_NODE, PROTECTED_PATH);

        AuthenticationInfo authInfo = (AuthenticationInfo) PrivateAccessor.invoke(slingAuthenticator, "getAuthenticationInfo",
                new Class[]{HttpServletRequest.class, HttpServletResponse.class}, new Object[]{request, context.mock(HttpServletResponse.class)});
        /**
         * The AUTH TYPE defined aboved should  be used for the path /test and his children: eg /test/childnode.
         */
        assertTrue(AUTH_TYPE.equals(authInfo.getAuthType()));
    }

    /**
     * Mocks the request to accept method calls on path;
     *
     * @param request              http request
     * @param requestPath          path in the http request
     * @param authProtectedPath    path protected by the auth handler
     */
    private void buildExpectationsForRequestPathAndAuthPath(final HttpServletRequest request,
                                                            final String requestPath,
                                                            final String authProtectedPath) {
        {
            context.checking(new Expectations() {
                {
                    allowing(request).getServletPath();
                    will(returnValue(requestPath));
                    allowing(request).getPathInfo();
                    will(returnValue(null));
                    allowing(request).getServerName();
                    will(returnValue("localhost"));
                    allowing(request).getServerPort();
                    will(returnValue(80));
                    allowing(request).getScheme();
                    will(returnValue("http"));
                    allowing(request).getAttribute("path");
                    will(returnValue(requestPath));
                    allowing(request).setAttribute("path", requestPath);
                    allowing(request).setAttribute("path", authProtectedPath);
                }
            });
        }
    }


    /**
     * Builds an auth handler for a specific path;
     * @param authType             name of the auth for this path
     * @param authProtectedPath    path protected by the auth handler
     * @return AbstractAuthenticationHandlerHolder with only an AuthenticationInfo
     */
    private AbstractAuthenticationHandlerHolder buildAuthHolderForAuthTypeAndPath(final String authType, final String authProtectedPath) {
        return new AbstractAuthenticationHandlerHolder(authProtectedPath, null) {

            @Override
            protected AuthenticationFeedbackHandler getFeedbackHandler() {
                return null;
            }

            @Override
            protected AuthenticationInfo doExtractCredentials(HttpServletRequest request, HttpServletResponse response) {
                return new AuthenticationInfo(authType);
            }

            @Override
            protected boolean doRequestCredentials(HttpServletRequest request, HttpServletResponse response) throws IOException {
                return false;
            }

            @Override
            protected void doDropCredentials(HttpServletRequest request, HttpServletResponse response) throws IOException {

            }
        };
    }

    public void test_childNodeAuthenticationHandlerPath() throws Throwable {
        final String requestPath = "/content/test/test2";
        final String handlerPath = "/content/test";

        assertTrue(new SlingAuthenticator.AuthenticationHandlerPath(requestPath, handlerPath).isNodeRequiresAuthHandler());
    }

    public void test_siblingNodeAuthenticationHandlerPath() throws Throwable {
        final String requestPath = "/content/test2";
        final String handlerPath = "/content/test";

        assertFalse(new SlingAuthenticator.AuthenticationHandlerPath(requestPath, handlerPath).isNodeRequiresAuthHandler());
    }

    public void test_actualNodeAuthenticationHandlerPath() throws Throwable {
        final String requestPath = "/content/test";
        final String handlerPath = "/content/test";

        assertTrue(new SlingAuthenticator.AuthenticationHandlerPath(requestPath, handlerPath).isNodeRequiresAuthHandler());
    }

    public void test_rootNodeAuthenticationHandlerPath() throws Throwable {
        final String requestPath = "/content/test";
        final String handlerPath = "/";

        assertTrue(new SlingAuthenticator.AuthenticationHandlerPath(requestPath, handlerPath).isNodeRequiresAuthHandler());
    }

    public void test_emptyNodeAuthenticationHandlerPath() throws Throwable {
        final String requestPath = "/content/test";
        final String handlerPath = "";

        assertTrue(new SlingAuthenticator.AuthenticationHandlerPath(requestPath, handlerPath).isNodeRequiresAuthHandler());
    }

}
