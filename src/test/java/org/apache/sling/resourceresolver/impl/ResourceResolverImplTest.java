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
package org.apache.sling.resourceresolver.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.security.Principal;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import javax.jcr.Session;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.sling.api.resource.AbstractResource;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.resourceresolver.impl.helper.ResourceResolverContext;
import org.junit.Before;
import org.junit.Test;

public class ResourceResolverImplTest {

    private ResourceResolver resResolver;

    private ResourceResolverFactoryImpl resFac;

    @Before public void setup() {
        resFac = new ResourceResolverFactoryImpl();
        resResolver = new ResourceResolverImpl(resFac, new ResourceResolverContext(false, null));
    }

    @Test public void testClose() throws Exception {
        final ResourceResolver rr = new ResourceResolverImpl(resFac, new ResourceResolverContext(false, null));
        assertTrue(rr.isLive());
        rr.close();
        assertFalse(rr.isLive());
        // close is always allowed to be called
        rr.close();
        assertFalse(rr.isLive());
        // now check all public method - they should all throw!
        try {
            rr.adaptTo(Session.class);
            fail();
        } catch (final IllegalStateException ise) {
            // expected
        }
        try {
            rr.clone(null);
            fail();
        } catch (final IllegalStateException ise) {
            // expected
        }
        try {
            rr.findResources("a", "b");
            fail();
        } catch (final IllegalStateException ise) {
            // expected
        }
        try {
            rr.getAttribute("a");
            fail();
        } catch (final IllegalStateException ise) {
            // expected
        }
        try {
            rr.getAttributeNames();
            fail();
        } catch (final IllegalStateException ise) {
            // expected
        }
        try {
            rr.getResource(null);
            fail();
        } catch (final IllegalStateException ise) {
            // expected
        }
        try {
            rr.getResource(null, "/a");
            fail();
        } catch (final IllegalStateException ise) {
            // expected
        }
        try {
            rr.getSearchPath();
            fail();
        } catch (final IllegalStateException ise) {
            // expected
        }
        try {
            rr.getUserID();
            fail();
        } catch (final IllegalStateException ise) {
            // expected
        }
        try {
            rr.listChildren(null);
            fail();
        } catch (final IllegalStateException ise) {
            // expected
        }
        try {
            rr.map("/somepath");
            fail();
        } catch (final IllegalStateException ise) {
            // expected
        }
        try {
            rr.map(null, "/somepath");
            fail();
        } catch (final IllegalStateException ise) {
            // expected
        }
        try {
            rr.queryResources("a", "b");
            fail();
        } catch (final IllegalStateException ise) {
            // expected
        }
        try {
            rr.resolve((HttpServletRequest)null);
            fail();
        } catch (final IllegalStateException ise) {
            // expected
        }
        try {
            rr.resolve("/path");
            fail();
        } catch (final IllegalStateException ise) {
            // expected
        }
        try {
            rr.resolve(null, "/path");
            fail();
        } catch (final IllegalStateException ise) {
            // expected
        }
    }

    @SuppressWarnings("deprecation")
    @Test public void testBasicAPIAssumptions() throws Exception {

        // null resource is accessing /, which exists of course
        final Resource res00 = resResolver.resolve((String) null);
        assertNotNull(res00);
        assertTrue("Resource must be NonExistingResource",
                        res00 instanceof NonExistingResource);
        assertEquals("Null path is expected to return root", "/",
            res00.getPath());

        // relative paths are treated as if absolute
        final String path01 = "relPath/relPath";
        final Resource res01 = resResolver.resolve(path01);
        assertNotNull(res01);
        assertEquals("Expecting absolute path for relative path", "/" + path01,
            res01.getPath());
        assertTrue("Resource must be NonExistingResource",
            res01 instanceof NonExistingResource);

        final String no_resource_path = "/no_resource/at/this/location";
        final Resource res02 = resResolver.resolve(no_resource_path);
        assertNotNull(res02);
        assertEquals("Expecting absolute path for relative path",
            no_resource_path, res02.getPath());
        assertTrue("Resource must be NonExistingResource",
            res01 instanceof NonExistingResource);

        try {
            resResolver.resolve((HttpServletRequest) null);
            fail("Expected NullPointerException trying to resolve null request");
        } catch (NullPointerException npe) {
            // expected
        }

        final Resource res0 = resResolver.resolve(null, no_resource_path);
        assertNotNull("Expecting resource if resolution fails", res0);
        assertTrue("Resource must be NonExistingResource",
            res0 instanceof NonExistingResource);
        assertEquals("Path must be the original path", no_resource_path,
            res0.getPath());

        final HttpServletRequest req1 = new ResourceResolverTestRequest(
            no_resource_path);
        final Resource res1 = resResolver.resolve(req1);
        assertNotNull("Expecting resource if resolution fails", res1);
        assertTrue("Resource must be NonExistingResource",
            res1 instanceof NonExistingResource);
        assertEquals("Path must be the original path", no_resource_path,
            res1.getPath());

        final HttpServletRequest req2 = new ResourceResolverTestRequest(null);
        final Resource res2 = resResolver.resolve(req2);
        assertNotNull("Expecting resource if resolution fails", res2);
        assertTrue("Resource must be NonExistingResource",
            res2 instanceof NonExistingResource);
        assertEquals("Path must be the the root path", "/", res2.getPath());
    }

    @Test public void test_clone_based_on_anonymous() throws Exception {
        final ResourceResolver anon0 = resFac.getResourceResolver((Map<String, Object>) null);
        // no session
        final Session anon0Session = anon0.adaptTo(Session.class);
        assertNull("Session should not be available", anon0Session);
        // no user information, so user id is null
        assertEquals(null, anon0.getUserID());

        // same user and workspace
        final ResourceResolver anon1 = anon0.clone(null);
        final Session anon1Session = anon1.adaptTo(Session.class);
        assertEquals(anon0.getUserID(), anon1.getUserID());
        assertNull("Session should not be available", anon1Session);
        anon1.close();

        // same workspace but admin user
        final Map<String, Object> admin0Cred = new HashMap<String, Object>();
        admin0Cred.put(ResourceResolverFactory.USER, "admin");
        admin0Cred.put(ResourceResolverFactory.PASSWORD, "admin".toCharArray());
        final ResourceResolver admin0 = anon0.clone(admin0Cred);
        assertEquals("admin", admin0.getUserID());
        admin0.close();

        anon0.close();
    }

    @Test public void test_clone_based_on_admin() throws Exception {
        final ResourceResolver admin0 = resFac.getAdministrativeResourceResolver((Map<String, Object>) null);
        // no user information, so user id is null
        assertEquals(null, admin0.getUserID());

        // same user and workspace
        final ResourceResolver admin1 = admin0.clone(null);
        assertEquals(admin0.getUserID(), admin1.getUserID());
        admin1.close();

        // same workspace but anonymous user
        final Map<String, Object> anon0Cred = new HashMap<String, Object>();
        anon0Cred.put(ResourceResolverFactory.USER, "anonymous");
        final ResourceResolver anon0 = admin0.clone(anon0Cred);
        assertEquals("anonymous", anon0.getUserID());
        anon0.close();

        admin0.close();
    }

    @Test public void test_attributes_from_authInfo() throws Exception {
        final Map<String, Object> authInfo = new HashMap<String, Object>();
        authInfo.put(ResourceResolverFactory.USER, "admin");
        authInfo.put(ResourceResolverFactory.PASSWORD, "admin".toCharArray());
        authInfo.put("testAttributeString", "AStringValue");
        authInfo.put("testAttributeNumber", 999);
        final ResourceResolver rr = resFac.getResourceResolver(authInfo);

        assertEquals("AStringValue", rr.getAttribute("testAttributeString"));
        assertEquals(999, rr.getAttribute("testAttributeNumber"));
        assertEquals("admin", rr.getAttribute(ResourceResolverFactory.USER));
        assertNull(rr.getAttribute(ResourceResolverFactory.PASSWORD));

        final HashSet<String> validNames = new HashSet<String>();
        validNames.add(ResourceResolverFactory.USER);
        validNames.add("testAttributeString");
        validNames.add("testAttributeNumber");
        final Iterator<String> names = rr.getAttributeNames();
        assertTrue(validNames.remove(names.next()));
        assertTrue(validNames.remove(names.next()));
        assertTrue(validNames.remove(names.next()));
        assertFalse("Expect no more names", names.hasNext());
        assertTrue("Expect validNames set to be empty now",
            validNames.isEmpty());

        rr.close();
    }

    @Test public void testBasicCrud() throws Exception {
        try {
            this.resResolver.addChild(null, "a", null);
            fail("Null parent resource should throw NPE");
        } catch (final NullPointerException npe) {
            // correct
        }
        try {
            this.resResolver.addChild(new ResourceImpl(), null, null);
            fail("Null name should throw NPE");
        } catch (final NullPointerException npe) {
            // correct
        }
        try {
            this.resResolver.addChild(new ResourceImpl(), "a/b", null);
            fail("Slash in name should throw persistence exception");
        } catch (final PersistenceException pe) {
            // correct
        }
    }

    private static final class ResourceImpl extends AbstractResource {

        public String getPath() {
            return "/some";
        }

        public String getResourceType() {
            return null;
        }

        public String getResourceSuperType() {
            return null;
        }

        public ResourceMetadata getResourceMetadata() {
            return null;
        }

        public ResourceResolver getResourceResolver() {
            return null;
        }
    };

    private static final class ResourceResolverTestRequest implements
    HttpServletRequest {

        private final String pathInfo;

        private final String method;

        private final String scheme;

        private final String host;

        private final int port;

        private final Map<String, Object> attrs = new HashMap<String, Object>();

        private String contextPath;

        ResourceResolverTestRequest(String pathInfo) {
            this(null, null, -1, pathInfo, null);
        }

        ResourceResolverTestRequest(String scheme, String host, int port,
                String pathInfo, String httpMethod) {
            this.scheme = (scheme == null) ? "http" : scheme;
            this.host = (host == null) ? "localhost" : host;
            this.port = port;
            this.pathInfo = pathInfo;
            this.method = httpMethod;
        }

        public String getPathInfo() {
            return pathInfo;
        }

        public Object getAttribute(String name) {
            return attrs.get(name);
        }

        public Enumeration<?> getAttributeNames() {
            return null;
        }

        public String getCharacterEncoding() {
            return null;
        }

        public int getContentLength() {
            return 0;
        }

        public String getContentType() {
            return null;
        }

        public ServletInputStream getInputStream() {
            return null;
        }

        public String getLocalAddr() {
            return null;
        }

        public String getLocalName() {
            return null;
        }

        public int getLocalPort() {
            return 0;
        }

        public Locale getLocale() {
            return null;
        }

        public Enumeration<?> getLocales() {
            return null;
        }

        public String getParameter(String name) {
            return null;
        }

        public Map<?, ?> getParameterMap() {
            return null;
        }

        public Enumeration<?> getParameterNames() {
            return null;
        }

        public String[] getParameterValues(String name) {
            return null;
        }

        public String getProtocol() {
            return null;
        }

        public BufferedReader getReader() {
            return null;
        }

        public String getRealPath(String path) {
            return null;
        }

        public String getRemoteAddr() {
            return null;
        }

        public String getRemoteHost() {
            return null;
        }

        public int getRemotePort() {
            return 0;
        }

        public RequestDispatcher getRequestDispatcher(String path) {
            return null;
        }

        public String getScheme() {
            return scheme;
        }

        public String getServerName() {
            return host;
        }

        public int getServerPort() {
            return port;
        }

        public boolean isSecure() {
            return false;
        }

        public String getContextPath() {
            return contextPath;
        }

        public void removeAttribute(String name) {
        }

        public void setAttribute(String name, Object o) {
            attrs.put(name, o);
        }

        public void setCharacterEncoding(String env) {
        }

        public String getAuthType() {
            return null;
        }

        public Cookie[] getCookies() {
            return null;
        }

        public long getDateHeader(String name) {
            return 0;
        }

        public String getHeader(String name) {
            return null;
        }

        public Enumeration<?> getHeaderNames() {
            return null;
        }

        public Enumeration<?> getHeaders(String name) {
            return null;
        }

        public int getIntHeader(String name) {
            return 0;
        }

        public String getMethod() {
            return method;
        }

        public String getPathTranslated() {
            return null;
        }

        public String getQueryString() {
            return null;
        }

        public String getRemoteUser() {
            return null;
        }

        public String getRequestURI() {
            return null;
        }

        public StringBuffer getRequestURL() {
            return null;
        }

        public String getRequestedSessionId() {
            return null;
        }

        public String getServletPath() {
            return null;
        }

        public HttpSession getSession() {
            return null;
        }

        public HttpSession getSession(boolean create) {
            return null;
        }

        public Principal getUserPrincipal() {
            return null;
        }

        public boolean isRequestedSessionIdFromCookie() {
            return false;
        }

        public boolean isRequestedSessionIdFromURL() {
            return false;
        }

        public boolean isRequestedSessionIdFromUrl() {
            return false;
        }

        public boolean isRequestedSessionIdValid() {
            return false;
        }

        public boolean isUserInRole(String role) {
            return false;
        }
    }
}
