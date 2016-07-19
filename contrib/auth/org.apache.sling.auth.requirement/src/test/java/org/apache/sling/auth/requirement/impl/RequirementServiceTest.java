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
package org.apache.sling.auth.requirement.impl;

import java.io.BufferedReader;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.google.common.collect.ImmutableMap;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.junit.Test;
import org.osgi.framework.BundleContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class RequirementServiceTest extends RequirementBaseTest {

    private final TestHandler reqHandler = new TestHandler();
    private final RequirementService reqService = new RequirementService();

    @Override
    public void before() throws Exception {
        super.before();

        context.registerService(RequirementHandler.class, reqHandler);
        context.registerService(Executor.class, new TestExecutor());

        BundleContext bundleContext = context.bundleContext();
        MockOsgi.injectServices(reqService, bundleContext);
        MockOsgi.activate(reqService, bundleContext, ImmutableMap.<String, Object>of());
    }

    @Override
    boolean initJcrRepo() {
        return false;
    }

    @Test
    public void getLoginPath() {
        assertEquals(reqHandler.getLoginPath("/resourcePath"), reqService.getLoginPath(new TestRequest("/servletPath", "/resourcePath")));
        assertEquals(reqHandler.getLoginPath("/resourcePath"), reqService.getLoginPath(new TestRequest(null, "/resourcePath")));
        assertEquals(reqHandler.getLoginPath("/servletPath"), reqService.getLoginPath(new TestRequest("/servletPath", null)));
        assertNull(reqService.getLoginPath(new TestRequest(null, null)));
    }

    @Test
    public void getLoginPathIncomplete() {
        RequirementService rs = new RequirementService();
        assertNull(rs.getLoginPath(new TestRequest("/servletPath", "/resourcePath")));
        assertNull(rs.getLoginPath(new TestRequest(null, "/resourcePath")));
        assertNull(rs.getLoginPath(new TestRequest("/servletPath", null)));
        assertNull(rs.getLoginPath(new TestRequest(null, null)));
    }

    @Test
    public void deactivateComplete() throws Exception {
        // must not fail
        reqService.deactivate();
    }

    @Test
    public void deactivateIncomplete() throws Exception {
        RequirementService rs = new RequirementService();
        // must not fail
        rs.deactivate();
    }


    private static final class TestExecutor implements Executor {

        @Override
        public void execute(@Nonnull Runnable runnable) {
            // nop
        }
    }

    private static final class TestRequest implements HttpServletRequest {

        private final String servletPath;
        private final String resource;

        private TestRequest(@Nullable String servletPath, @Nullable String resource) {
            this.servletPath = servletPath;
            this.resource = resource;
        }

        @Override
        public String getServletPath() {
            return servletPath;
        }

        @Override
        public String getParameter(String name) {
            if ("resource".equals(name)) {
                return resource;
            } else {
                return null;
            }
        }

        @Override
        public String getAuthType() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Cookie[] getCookies() {
            throw new UnsupportedOperationException();
        }

        @Override
        public long getDateHeader(String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getHeader(String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Enumeration getHeaders(String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Enumeration getHeaderNames() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getIntHeader(String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getMethod() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getPathInfo() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getPathTranslated() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getContextPath() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getQueryString() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getRemoteUser() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isUserInRole(String role) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Principal getUserPrincipal() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getRequestedSessionId() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getRequestURI() {
            throw new UnsupportedOperationException();
        }

        @Override
        public StringBuffer getRequestURL() {
            throw new UnsupportedOperationException();
        }

        @Override
        public HttpSession getSession(boolean create) {
            throw new UnsupportedOperationException();
        }

        @Override
        public HttpSession getSession() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isRequestedSessionIdValid() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isRequestedSessionIdFromCookie() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isRequestedSessionIdFromURL() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isRequestedSessionIdFromUrl() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object getAttribute(String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Enumeration getAttributeNames() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getCharacterEncoding() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setCharacterEncoding(String env) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getContentLength() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getContentType() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ServletInputStream getInputStream() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Enumeration getParameterNames() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String[] getParameterValues(String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Map getParameterMap() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getProtocol() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getScheme() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getServerName() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getServerPort() {
            throw new UnsupportedOperationException();
        }

        @Override
        public BufferedReader getReader() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getRemoteAddr() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getRemoteHost() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setAttribute(String name, Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeAttribute(String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Locale getLocale() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Enumeration getLocales() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isSecure() {
            throw new UnsupportedOperationException();
        }

        @Override
        public RequestDispatcher getRequestDispatcher(String path) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getRealPath(String path) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getRemotePort() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getLocalName() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getLocalAddr() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getLocalPort() {
            throw new UnsupportedOperationException();
        }
    }
}