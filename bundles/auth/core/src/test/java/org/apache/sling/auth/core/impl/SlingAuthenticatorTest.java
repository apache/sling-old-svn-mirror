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

import java.io.UnsupportedEncodingException;

import javax.servlet.http.HttpServletRequest;

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
        checkQuote("email@address.com", "\"email@address.com\"");

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

}
