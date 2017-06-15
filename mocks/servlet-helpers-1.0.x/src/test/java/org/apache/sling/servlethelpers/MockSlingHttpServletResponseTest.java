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
package org.apache.sling.servlethelpers;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.CharEncoding;
import org.apache.sling.servlethelpers.MockSlingHttpServletResponse;
import org.junit.Before;
import org.junit.Test;

public class MockSlingHttpServletResponseTest {

    private MockSlingHttpServletResponse response;

    @Before
    public void setUp() throws Exception {
        this.response = new MockSlingHttpServletResponse();
    }

    @Test
    public void testContentTypeCharset() throws Exception {
        assertNull(response.getContentType());
        assertNull(response.getCharacterEncoding());

        response.setContentType("image/gif");
        assertEquals("image/gif", response.getContentType());
        assertNull(response.getCharacterEncoding());
        
        response.setContentType("text/plain;charset=UTF-8");
        assertEquals("text/plain;charset=UTF-8", response.getContentType());
        assertEquals(CharEncoding.UTF_8, response.getCharacterEncoding());
        
        response.setCharacterEncoding(CharEncoding.ISO_8859_1);
        assertEquals("text/plain;charset=ISO-8859-1", response.getContentType());
        assertEquals(CharEncoding.ISO_8859_1, response.getCharacterEncoding());
    }

    @Test
    public void testContentLength() throws Exception {
        assertEquals(0, response.getContentLength());

        response.setContentLength(55);
        assertEquals(55, response.getContentLength());
    }

    @Test
    public void testHeaders() throws Exception {
        assertEquals(0, response.getHeaderNames().size());

        response.addHeader("header1", "value1");
        response.addIntHeader("header2", 5);
        response.addDateHeader("header3", System.currentTimeMillis());

        assertEquals(3, response.getHeaderNames().size());
        assertTrue(response.containsHeader("header1"));
        assertEquals("value1", response.getHeader("header1"));
        assertEquals("5", response.getHeader("header2"));
        assertNotNull(response.getHeader("header3"));

        response.setHeader("header1", "value2");
        response.addIntHeader("header2", 10);

        assertEquals(3, response.getHeaderNames().size());

        Collection<String> header1Values = response.getHeaders("header1");
        assertEquals(1, header1Values.size());
        assertEquals("value2", header1Values.iterator().next());

        Collection<String> header2Values = response.getHeaders("header2");
        assertEquals(2, header2Values.size());
        Iterator<String> header2Iterator = header2Values.iterator();
        assertEquals("5", header2Iterator.next());
        assertEquals("10", header2Iterator.next());

        response.reset();
        assertEquals(0, response.getHeaderNames().size());
    }

    @Test
    public void testRedirect() throws Exception {
        response.sendRedirect("/location.html");
        assertEquals(HttpServletResponse.SC_MOVED_TEMPORARILY, response.getStatus());
        assertEquals("/location.html", response.getHeader("Location"));
    }

    @Test
    public void testSendError() throws Exception {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
        assertEquals(HttpServletResponse.SC_NOT_FOUND, response.getStatus());
    }

    @Test
    public void testSetStatus() throws Exception {
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());

        response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
        assertEquals(HttpServletResponse.SC_BAD_GATEWAY, response.getStatus());

        response.reset();
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    }

    @Test
    public void testWriteStringContent() throws Exception {
        final String TEST_CONTENT = "Der Jodelkaiser äöüß€ ᚠᛇᚻ";
        response.setCharacterEncoding(CharEncoding.UTF_8);
        response.getWriter().write(TEST_CONTENT);
        assertEquals(TEST_CONTENT, response.getOutputAsString());

        response.resetBuffer();
        assertEquals(0, response.getOutputAsString().length());
    }

    @Test
    public void testWriteBinaryContent() throws Exception {
        final byte[] TEST_DATA = new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05 };
        response.getOutputStream().write(TEST_DATA);
        assertArrayEquals(TEST_DATA, response.getOutput());

        response.resetBuffer();
        assertEquals(0, response.getOutput().length);
    }

    @Test
    public void testIsCommitted() throws Exception {
        assertFalse(response.isCommitted());
        response.flushBuffer();
        assertTrue(response.isCommitted());
    }

    @Test
    public void testCookies() {
        assertNull(response.getCookies());

        response.addCookie(new Cookie("cookie1", "value1"));
        response.addCookie(new Cookie("cookie2", "value2"));

        assertEquals("value1", response.getCookie("cookie1").getValue());

        Cookie[] cookies = response.getCookies();
        assertEquals(2, cookies.length);
        assertEquals("value1", cookies[0].getValue());
        assertEquals("value2", cookies[1].getValue());

        response.reset();
        assertNull(response.getCookies());
    }

    @Test
    public void testLocale() {
        assertEquals(Locale.US, response.getLocale());
        response.setLocale(Locale.GERMAN);
        assertEquals(Locale.GERMAN, response.getLocale());
    }

}
