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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.ListResourceBundle;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.i18n.ResourceBundleProvider;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;

@RunWith(MockitoJUnitRunner.class)
public class MockSlingHttpServletRequestTest {

    @Mock
    private ResourceResolver resourceResolver;
    @Mock
    private Resource resource;
    private BundleContext bundleContext = MockOsgi.newBundleContext();

    private MockSlingHttpServletRequest request;

    @Before
    public void setUp() throws Exception {
        request = new MockSlingHttpServletRequest(resourceResolver, bundleContext);
    }
    
    @After
    public void tearDown() {
        MockOsgi.shutdown(bundleContext);
    }

    @Test
    public void testResourceResolver() {
        assertSame(resourceResolver, request.getResourceResolver());
    }

    @Test
    public void testDefaultResourceResolver() {
        assertNotNull(new MockSlingHttpServletRequest(bundleContext).getResourceResolver());
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
    
    @Test
    public void testDefaultResourceBundle() {
        ResourceBundle bundle = request.getResourceBundle(Locale.US);
        assertNotNull(bundle);
        assertFalse(bundle.getKeys().hasMoreElements());
    }

    @Test
    public void testResourceBundleFromProvider() {
        ResourceBundleProvider provider = mock(ResourceBundleProvider.class);
        bundleContext.registerService(ResourceBundleProvider.class.getName(), provider, null);
        when(provider.getResourceBundle("base1", Locale.US)).thenReturn(new ListResourceBundle() {
            @Override
            protected Object[][] getContents() {
                return new Object[][] {
                        { "key1", "value1" }
                };
            }
        });        
        
        ResourceBundle bundle = request.getResourceBundle("base1", Locale.US);
        assertNotNull(bundle);
        assertEquals("value1", bundle.getString("key1"));

        ResourceBundle bundle2 = request.getResourceBundle("base2", Locale.US);
        assertNotNull(bundle2);
        assertFalse(bundle2.getKeys().hasMoreElements());
    }

    @Test
    public void testRequestParameter() throws Exception {
        request.setQueryString("param1=123&param2=" + URLEncoder.encode("äöüß€!:!", CharEncoding.UTF_8)
                + "&param3=a&param3=b");

        assertEquals(3, request.getRequestParameterMap().size());
        assertEquals(4, request.getRequestParameterList().size());
        assertEquals("123", request.getRequestParameter("param1").getString());
        assertEquals("äöüß€!:!", request.getRequestParameter("param2").getString());
        assertEquals("a",request.getRequestParameters("param3")[0].getString());
        assertEquals("b",request.getRequestParameters("param3")[1].getString());

        assertNull(request.getRequestParameter("unknown"));
        assertNull(request.getRequestParameters("unknown"));
    }

    @Test
    public void testContentTypeCharset() throws Exception {
        assertNull(request.getContentType());
        assertNull(request.getCharacterEncoding());

        request.setContentType("image/gif");
        assertEquals("image/gif", request.getContentType());
        assertNull(request.getCharacterEncoding());
        
        request.setContentType("text/plain;charset=UTF-8");
        assertEquals("text/plain;charset=UTF-8", request.getContentType());
        assertEquals(CharEncoding.UTF_8, request.getCharacterEncoding());
        
        request.setCharacterEncoding(CharEncoding.ISO_8859_1);
        assertEquals("text/plain;charset=ISO-8859-1", request.getContentType());
        assertEquals(CharEncoding.ISO_8859_1, request.getCharacterEncoding());
    }

    @Test
    public void testContent() throws Exception {
        assertEquals(0, request.getContentLength());
        assertNull(request.getInputStream());
        
        byte[] data = new byte[] { 0x01,0x02,0x03 };
        request.setContent(data);

        assertEquals(data.length, request.getContentLength());
        assertArrayEquals(data, IOUtils.toByteArray(request.getInputStream()));
    }

    @Test
    public void testGetRequestDispatcher() {
        MockRequestDispatcherFactory requestDispatcherFactory = mock(MockRequestDispatcherFactory.class);
        RequestDispatcher requestDispatcher = mock(RequestDispatcher.class);
        when(requestDispatcherFactory.getRequestDispatcher(any(Resource.class), any(RequestDispatcherOptions.class))).thenReturn(requestDispatcher);
        when(requestDispatcherFactory.getRequestDispatcher(any(String.class), any(RequestDispatcherOptions.class))).thenReturn(requestDispatcher);
        
        request.setRequestDispatcherFactory(requestDispatcherFactory);
        
        assertSame(requestDispatcher, request.getRequestDispatcher("/path"));
        assertSame(requestDispatcher, request.getRequestDispatcher("/path", new RequestDispatcherOptions()));
        assertSame(requestDispatcher, request.getRequestDispatcher(resource));
        assertSame(requestDispatcher, request.getRequestDispatcher(resource, new RequestDispatcherOptions()));
    }
    
    @Test(expected = IllegalStateException.class)
    public void testGetRequestDispatcherWithoutFactory() {
        request.getRequestDispatcher("/path");
    }
    
    @Test
    public void testGetRemoteUserN() {
        
        assertNull(null, request.getRemoteUser());
        
        request.setRemoteUser("admin");
        assertEquals("admin", request.getRemoteUser());
    }
    
}
