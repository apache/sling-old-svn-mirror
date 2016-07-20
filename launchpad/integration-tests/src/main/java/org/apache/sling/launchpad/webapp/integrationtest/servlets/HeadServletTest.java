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
package org.apache.sling.launchpad.webapp.integrationtest.servlets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.sling.commons.testing.integration.HttpTest;
import org.apache.sling.commons.testing.integration.HttpTestBase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Test HEAD requests */
public class HeadServletTest {

    private final HttpTest H = new HttpTest();
    
    /** Test content provided by the test-services bundle */
    public static final String HTML_URL = HttpTestBase.HTTP_BASE_URL + "/sling-test/sling/sling-test.html";
    
    /** Test content provided by the test-services bundle */
    public static final String PNG_URL = HttpTestBase.HTTP_BASE_URL + "/sling-logo.png";
    
    public static final String NONEXISTENT_URL = HttpTestBase.HTTP_BASE_URL + "/notfound-" + UUID.randomUUID().toString();  
    
    private void assertResponseHeader(HttpMethod m, String name, String expectedRegex) {
        final Header h = m.getResponseHeader(name);
        assertNotNull("Expecting header " + name, h);
        final String value = h.getValue();
        assertTrue("Expected regexp " + expectedRegex + " for header " + name + ", header value is " + value,
                Pattern.matches(expectedRegex, value));
    }
    
    private void assertCommonHeaders(HttpMethod m, String expectedContentType) {
        assertResponseHeader(m, "Content-Length", "[0-9][0-9][0-9]+");
        assertResponseHeader(m, "Content-Type", expectedContentType);
        assertResponseHeader(m, "Last-Modified", ".*[a-zA-Z0-9]+.*");
    }
    
    @Before
    public void setup() throws Exception {
        H.setUp();
    }
    
    @After
    public void cleanup() throws Exception {
        H.tearDown();
    }
    
    @Test
    public void htmlGet() throws IOException {
        final String content = H.getContent(HTML_URL, HttpTest.CONTENT_TYPE_HTML);
        HttpTest.assertContains(content, "Automated Sling client library tests");
    }
    
    @Test
    public void htmlHead() throws IOException {
        final HeadMethod head = new HeadMethod(HTML_URL);
        final int status = H.getHttpClient().executeMethod(head);
        assertEquals(200, status);
        assertNull("Expecting null body", head.getResponseBody());
        assertCommonHeaders(head, "text/html");
    }
    
    @Test
    public void pngGet() throws IOException {
        final GetMethod get = new GetMethod(PNG_URL);
        final int status = H.getHttpClient().executeMethod(get);
        assertEquals(200, status);
        assertNotNull(get.getResponseBody());
        assertTrue("Expecting non-empty body", get.getResponseBody().length > 500);
        assertCommonHeaders(get, "image/png");
    }
    
    @Test
    public void pngHead() throws IOException {
        final HeadMethod head = new HeadMethod(PNG_URL);
        final int status = H.getHttpClient().executeMethod(head);
        assertEquals(200, status);
        assertNull("Expecting null body", head.getResponseBody());
        assertCommonHeaders(head, "image/png");
    }
    
    @Test
    public void nonexistentGet() throws IOException {
        final GetMethod get = new GetMethod(NONEXISTENT_URL);
        assertEquals(404, H.getHttpClient().executeMethod(get));
    }
    
    @Test
    public void nonexistentHead() throws IOException {
        final HeadMethod head = new HeadMethod(NONEXISTENT_URL);
        assertEquals(404, H.getHttpClient().executeMethod(head));
    }
}