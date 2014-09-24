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
package org.apache.sling.auth.openid.impl;

import javax.servlet.http.HttpServletRequest;

import junit.framework.TestCase;

import org.apache.sling.auth.openid.impl.OpenIDAuthenticationHandler;
import org.jmock.Expectations;
import org.jmock.Mockery;

public class OpenIDAuthenticationHandlerTest extends TestCase {

    private static final String NAME = "the.server.com";

    private final String HTTP = "http";

    private final String HTTPS = "https";

    private final String ANY = "any";

    private final int port80 = 80;

    private final int port443 = 443;

    private final int port999 = 999;

    private final int port_1 = -1;

    private static final String CTX = "";

    public void test_getBaseUrl() {

        final Mockery ctx = new Mockery();
        final HttpServletRequest request = ctx.mock(HttpServletRequest.class);
        ctx.checking(new Expectations() {
            {
                // general setup on each calls
                allowing(request).getServerName();
                will(returnValue(NAME));
                allowing(request).getContextPath();
                will(returnValue(CTX));

                // http/80
                one(request).getScheme();
                will(returnValue(HTTP));
                one(request).getServerPort();
                will(returnValue(port80));

                // https/443
                one(request).getScheme();
                will(returnValue(HTTPS));
                one(request).getServerPort();
                will(returnValue(port443));

                // http/999
                one(request).getScheme();
                will(returnValue(HTTP));
                one(request).getServerPort();
                will(returnValue(port999));

                // https/999
                one(request).getScheme();
                will(returnValue(HTTPS));
                one(request).getServerPort();
                will(returnValue(port999));

                // any/80
                one(request).getScheme();
                will(returnValue(ANY));
                one(request).getServerPort();
                will(returnValue(port80));

                // any/443
                one(request).getScheme();
                will(returnValue(ANY));
                one(request).getServerPort();
                will(returnValue(port443));

                // any/999
                one(request).getScheme();
                will(returnValue(ANY));
                one(request).getServerPort();
                will(returnValue(port999));

                // http/-1
                one(request).getScheme();
                will(returnValue(HTTP));
                one(request).getServerPort();
                will(returnValue(port_1));

                // https/-1
                one(request).getScheme();
                will(returnValue(HTTPS));
                one(request).getServerPort();
                will(returnValue(port_1));

                // any/-1
                one(request).getScheme();
                will(returnValue(ANY));
                one(request).getServerPort();
                will(returnValue(port_1));
            }
        });

        final OpenIDAuthenticationHandler handler1 = new OpenIDAuthenticationHandler();
        // http/80
        assertEquals(HTTP + "://" + NAME, handler1.getBaseUrl(request));
        // https/443
        assertEquals(HTTPS + "://" + NAME, handler1.getBaseUrl(request));
        // http/999
        assertEquals(HTTP + "://" + NAME + ":" + port999,
            handler1.getBaseUrl(request));
        // https/999
        assertEquals(HTTPS + "://" + NAME + ":" + port999,
            handler1.getBaseUrl(request));
        // any/80
        assertEquals(ANY + "://" + NAME + ":" + port80,
            handler1.getBaseUrl(request));
        // any/443
        assertEquals(ANY + "://" + NAME + ":" + port443,
            handler1.getBaseUrl(request));
        // any/999
        assertEquals(ANY + "://" + NAME + ":" + port999,
            handler1.getBaseUrl(request));
        // http/-1
        assertEquals(HTTP + "://" + NAME, handler1.getBaseUrl(request));
        // https/-1
        assertEquals(HTTPS + "://" + NAME, handler1.getBaseUrl(request));
        // any/-1
        assertEquals(ANY+ "://" + NAME, handler1.getBaseUrl(request));
    }
}
