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
package org.apache.sling.testing.mock.sling.servlet;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.CharEncoding;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MockSlingHttpServletRequestTest {

    @Mock
    private ResourceResolver resourceResolver;
    @Mock
    private Resource resource;

    private MockSlingHttpServletRequest request;

    @Before
    public void setUp() throws Exception {
        request = new MockSlingHttpServletRequest(resourceResolver);
    }

    @Test
    public void testResourceResolver() {
        assertSame(resourceResolver, request.getResourceResolver());
    }

    @Test
    public void testDefaultResourceResolver() {
        assertNotNull(new MockSlingHttpServletRequest().getResourceResolver());
    }

    @Test
    public void testSession() {
        HttpSession session = request.getSession(false);
        assertNull(session);
        session = request.getSession();
        assertNotNull(session);
    }

    @Test
    public void testRequestPathInfo() {
        assertNotNull(request.getRequestPathInfo());
    }

    @Test
    public void testAttributes() {
        request.setAttribute("attr1", "value1");
        assertTrue(request.getAttributeNames().hasMoreElements());
        assertEquals("value1", request.getAttribute("attr1"));
        request.removeAttribute("attr1");
        assertFalse(request.getAttributeNames().hasMoreElements());
    }

    @Test
    public void testResource() {
        assertNull(request.getResource());
        request.setResource(resource);
        assertSame(resource, request.getResource());
    }

    @Test
    public void testContextPath() {
        assertNull(request.getContextPath());
        request.setContextPath("/ctx");
        assertEquals("/ctx", request.getContextPath());
    }

    @Test
    public void testLocale() {
        assertEquals(Locale.US, request.getLocale());
    }

    @Test
    public void testQueryString() throws UnsupportedEncodingException {
        assertNull(request.getQueryString());
        assertEquals(0, request.getParameterMap().size());
        assertFalse(request.getParameterNames().hasMoreElements());

        request.setQueryString("param1=123&param2=" + URLEncoder.encode("äöüß€!:!", CharEncoding.UTF_8)
                + "&param3=a&param3=b");

        assertNotNull(request.getQueryString());
        assertEquals(3, request.getParameterMap().size());
        assertTrue(request.getParameterNames().hasMoreElements());
        assertEquals("123", request.getParameter("param1"));
        assertEquals("äöüß€!:!", request.getParameter("param2"));
        assertArrayEquals(new String[] { "a", "b" }, request.getParameterValues("param3"));

        Map<String, Object> paramMap = new LinkedHashMap<String, Object>();
        paramMap.put("p1", "a");
        paramMap.put("p2", new String[] { "b", "c" });
        paramMap.put("p3", null);
        paramMap.put("p4", new String[] { null });
        paramMap.put("p5", 22);
        request.setParameterMap(paramMap);

        assertEquals("p1=a&p2=b&p2=c&p4=&p5=22", request.getQueryString());
    }

    @Test
    public void testSchemeSecure() {
        assertEquals("http", request.getScheme());
        assertFalse(request.isSecure());

        request.setScheme("https");
        assertEquals("https", request.getScheme());
        assertTrue(request.isSecure());
    }

    @Test
    public void testServerNamePort() {
        assertEquals("localhost", request.getServerName());
        assertEquals(80, request.getServerPort());

        request.setServerName("myhost");
        request.setServerPort(12345);
        assertEquals("myhost", request.getServerName());
        assertEquals(12345, request.getServerPort());
    }

    @Test
    public void testMethod() {
        assertEquals(HttpConstants.METHOD_GET, request.getMethod());

        request.setMethod(HttpConstants.METHOD_POST);
        assertEquals(HttpConstants.METHOD_POST, request.getMethod());
    }

    @Test
    public void testHeaders() {
        assertFalse(request.getHeaderNames().hasMoreElements());

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MILLISECOND, 0);
        long dateValue = calendar.getTimeInMillis();

        request.addHeader("header1", "value1");
        request.addIntHeader("header2", 5);
        request.addDateHeader("header3", dateValue);

        assertEquals("value1", request.getHeader("header1"));
        assertEquals(5, request.getIntHeader("header2"));
        assertEquals(dateValue, request.getDateHeader("header3"));

        request.setHeader("header1", "value2");
        request.addIntHeader("header2", 10);

        Enumeration<String> header1Values = request.getHeaders("header1");
        assertEquals("value2", header1Values.nextElement());
        assertFalse(header1Values.hasMoreElements());

        Enumeration<String> header2Values = request.getHeaders("header2");
        assertEquals("5", header2Values.nextElement());
        assertEquals("10", header2Values.nextElement());
        assertFalse(header2Values.hasMoreElements());
    }

    @Test
    public void testCookies() {
        assertNull(request.getCookies());

        request.addCookie(new Cookie("cookie1", "value1"));
        request.addCookie(new Cookie("cookie2", "value2"));

        assertEquals("value1", request.getCookie("cookie1").getValue());

        Cookie[] cookies = request.getCookies();
        assertEquals(2, cookies.length);
        assertEquals("value1", cookies[0].getValue());
        assertEquals("value2", cookies[1].getValue());
    }

}
