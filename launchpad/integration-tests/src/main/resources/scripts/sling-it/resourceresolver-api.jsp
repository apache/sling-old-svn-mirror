<%@page session="false" %><%
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
%><%@page import="org.junit.Assert,
                  org.apache.sling.api.resource.ResourceUtil,
                  org.apache.sling.api.resource.Resource,
                  org.apache.sling.api.resource.ResourceResolver"%><%
%><%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.0" %><%
%><sling:defineObjects/><%!
    private static final class ResourceResolverTestRequest implements
            HttpServletRequest {

        private final String pathInfo;

        private final String method;

        private final String scheme;

        private final String host;

        private final int port;

        private final java.util.Map<String, Object> attrs = new java.util.HashMap<String, Object>();

        private String contextPath;

        ResourceResolverTestRequest(String pathInfo) {
            this(pathInfo, null);
        }

        ResourceResolverTestRequest(String pathInfo, String httpMethod) {
            this(null, null, -1, pathInfo, httpMethod);
        }

        ResourceResolverTestRequest(String scheme, String host, int port,
                String pathInfo) {
            this(scheme, host, port, pathInfo, null);
        }

        ResourceResolverTestRequest(String scheme, String host, int port,
                String pathInfo, String httpMethod) {
            this.scheme = (scheme == null) ? "http" : scheme;
            this.host = (host == null) ? "localhost" : host;
            this.port = port;
            this.pathInfo = pathInfo;
            this.method = httpMethod;
        }

        void setContextPath(String contextPath) {
            this.contextPath = contextPath;
        }

        public String getPathInfo() {
            return pathInfo;
        }

        public Object getAttribute(String name) {
            return attrs.get(name);
        }

        public java.util.Enumeration<?> getAttributeNames() {
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

        public java.util.Locale getLocale() {
            return null;
        }

        public java.util.Enumeration<?> getLocales() {
            return null;
        }

        public String getParameter(String name) {
            return null;
        }

        public java.util.Map<?, ?> getParameterMap() {
            return null;
        }

        public java.util.Enumeration<?> getParameterNames() {
            return null;
        }

        public String[] getParameterValues(String name) {
            return null;
        }

        public String getProtocol() {
            return null;
        }

        public java.io.BufferedReader getReader() {
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

        public java.util.Enumeration<?> getHeaderNames() {
            return null;
        }

        public java.util.Enumeration<?> getHeaders(String name) {
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

        public java.security.Principal getUserPrincipal() {
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
%><%
    final ResourceResolver resResolver = slingRequest.getResourceResolver();

    // null resource is accessing /, which exists of course
    final Resource res00 = resResolver.resolve((String) null);
    Assert.assertNotNull(res00);
    Assert.assertEquals("Null path is expected to return root", "/",
            res00.getPath());

    // relative paths are treated as if absolute
    final String path01 = "relPath/relPath";
    final Resource res01 = resResolver.resolve(path01);
    Assert.assertNotNull(res01);
    Assert.assertEquals("Expecting absolute path for relative path", "/" + path01,
            res01.getPath());
    Assert.assertTrue("Resource must be NonExistingResource: " + res01.getClass().getName(),
            ResourceUtil.isNonExistingResource(res01));

    final String no_resource_path = "/no_resource/at/this/location";
    final Resource res02 = resResolver.resolve(no_resource_path);
    Assert.assertNotNull(res02);
    Assert.assertEquals("Expecting absolute path for relative path",
            no_resource_path, res02.getPath());
    Assert.assertTrue("Resource must be NonExistingResource",
            ResourceUtil.isNonExistingResource(res02));

        try {
            resResolver.resolve((HttpServletRequest) null);
            Assert.fail("Expected NullPointerException trying to resolve null request");
        } catch (NullPointerException npe) {
            // expected
        }

        final Resource res0 = resResolver.resolve(null, no_resource_path);
        Assert.assertNotNull("Expecting resource if resolution fails", res0);
        Assert.assertTrue("Resource must be NonExistingResource",
                ResourceUtil.isNonExistingResource(res0));
        Assert.assertEquals("Path must be the original path", no_resource_path,
            res0.getPath());

        final HttpServletRequest req1 = new ResourceResolverTestRequest(
            no_resource_path);
        final Resource res1 = resResolver.resolve(req1);
        Assert.assertNotNull("Expecting resource if resolution fails", res1);
        Assert.assertTrue("Resource must be NonExistingResource",
                ResourceUtil.isNonExistingResource(res1));
        Assert.assertEquals("Path must be the original path", no_resource_path,
            res1.getPath());

        final HttpServletRequest req2 = new ResourceResolverTestRequest(null);
        final Resource res2 = resResolver.resolve(req2);
        Assert.assertNotNull("Expecting resource if resolution fails", res2);
        Assert.assertFalse("Resource must not be NonExistingResource was ",
            ResourceUtil.isNonExistingResource(res2));
        Assert.assertEquals("Path must be the the root path", "/", res2.getPath());
%>TEST_PASSED