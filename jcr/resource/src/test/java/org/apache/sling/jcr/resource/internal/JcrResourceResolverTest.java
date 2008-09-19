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
package org.apache.sling.jcr.resource.internal;

import java.io.BufferedReader;
import java.lang.reflect.Field;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.commons.testing.jcr.RepositoryTestBase;
import org.apache.sling.commons.testing.jcr.RepositoryUtil;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.apache.sling.jcr.resource.internal.helper.Mapping;
import org.apache.sling.jcr.resource.internal.helper.starresource.StarResource;

public class JcrResourceResolverTest extends RepositoryTestBase {

    private String rootPath;

    private Node rootNode;

    private ResourceResolver resResolver;

    protected void setUp() throws Exception {
        super.setUp();
        assertTrue(RepositoryUtil.registerNodeType(getSession(),
            this.getClass().getResourceAsStream("/SLING-INF/nodetypes/folder.cnd")));
        assertTrue(RepositoryUtil.registerNodeType(getSession(),
            this.getClass().getResourceAsStream("/SLING-INF/nodetypes/resource.cnd")));
        assertTrue(RepositoryUtil.registerNodeType(getSession(),
                this.getClass().getResourceAsStream("/SLING-INF/nodetypes/vanitypath.cnd")));

        JcrResourceResolverFactoryImpl resFac = new JcrResourceResolverFactoryImpl();

        Field repoField = resFac.getClass().getDeclaredField("repository");
        repoField.setAccessible(true);
        repoField.set(resFac, getRepository());

        Field mappingsField = resFac.getClass().getDeclaredField("mappings");
        mappingsField.setAccessible(true);
        mappingsField.set(resFac, new Mapping[] { Mapping.DIRECT });

        Field patternsField = resFac.getClass().getDeclaredField("patterns");
        patternsField.setAccessible(true);
        patternsField.set(resFac, new JcrResourceResolverFactoryImpl.ResourcePattern[0]);

        try {
            NamespaceRegistry nsr = session.getWorkspace().getNamespaceRegistry();
            nsr.registerNamespace(SlingConstants.NAMESPACE_PREFIX,
                JcrResourceConstants.SLING_NAMESPACE_URI);
        } catch (Exception e) {
            // don't care for now
        }

        resResolver = resFac.getResourceResolver(session);

        rootPath = "/test" + System.currentTimeMillis();
        rootNode = getSession().getRootNode().addNode(rootPath.substring(1),
            "nt:unstructured");
        session.save();
    }

    @Override
    protected void tearDown() throws Exception {
        if (rootNode != null) {
            rootNode.remove();
            session.save();
        }
    }

    public void testGetResource() throws Exception {
        // existing resource
        Resource res = resResolver.getResource(rootPath);
        assertNotNull(res);
        assertEquals(rootPath, res.getPath());
        assertEquals(rootNode.getPrimaryNodeType().getName(),
            res.getResourceType());

        assertNotNull(res.adaptTo(Node.class));
        assertTrue(rootNode.isSame(res.adaptTo(Node.class)));

        // missing resource
        String path = rootPath + "/missing";
        res = resResolver.getResource(path);
        assertNull(res);
    }

    public void testResolveResource() throws Exception {
        // existing resource
        Resource res = resResolver.resolve(new ResourceResolverTestRequest(
            rootPath));
        assertNotNull(res);
        assertEquals(rootPath, res.getPath());
        assertEquals(rootNode.getPrimaryNodeType().getName(),
            res.getResourceType());

        assertNotNull(res.adaptTo(Node.class));
        assertTrue(rootNode.isSame(res.adaptTo(Node.class)));

        // missing resource below root should resolve "missing resource"
        String path = rootPath + "/missing";
        res = resResolver.resolve(new ResourceResolverTestRequest(path));
        assertNotNull(res);
        assertEquals(path, res.getPath());
        assertEquals(Resource.RESOURCE_TYPE_NON_EXISTING, res.getResourceType());

        assertNull(res.adaptTo(Node.class));

        // root with selectors/ext should resolve root
        path = rootPath + ".print.a4.html";
        res = resResolver.resolve(new ResourceResolverTestRequest(path));
        assertNotNull(res);
        assertEquals(rootPath, res.getPath());
        assertEquals(rootNode.getPrimaryNodeType().getName(),
            res.getResourceType());

        assertNotNull(res.adaptTo(Node.class));
        assertTrue(rootNode.isSame(res.adaptTo(Node.class)));

        // missing resource should return NON_EXISTING Resource
        path = rootPath + System.currentTimeMillis();
        res = resResolver.resolve(new ResourceResolverTestRequest(path));
        assertNotNull(res);
        assertTrue(res instanceof NonExistingResource);
        assertEquals(path, res.getPath());
        assertEquals(Resource.RESOURCE_TYPE_NON_EXISTING, res.getResourceType());
    }

    public void testGetDoesNotGoUp() throws Exception {

        final String path = rootPath + "/nothing";

        {
            final Resource res = resResolver.resolve(new ResourceResolverTestRequest(
                path, "POST"));
            assertNotNull(res);
            assertEquals("POST request resolution does not go up the path",
                Resource.RESOURCE_TYPE_NON_EXISTING, res.getResourceType());
        }

        {
            final Resource res = resResolver.resolve(new ResourceResolverTestRequest(
                path, "GET"));
            assertNotNull(res);
            assertEquals("GET request resolution does not go up the path",
                Resource.RESOURCE_TYPE_NON_EXISTING, res.getResourceType());
        }
    }

    public void testGetRemovesExtensionInResolution() throws Exception {
        final String path = rootPath + ".whatever";
        final Resource res = resResolver.resolve(new ResourceResolverTestRequest(
            path, "GET"));
        assertNotNull(res);
        assertEquals(rootPath, res.getPath());
        assertEquals(rootNode.getPrimaryNodeType().getName(),
            res.getResourceType());
    }

    public void testStarResourcePlain() throws Exception {
        final String path = rootPath + "/" + System.currentTimeMillis() + "/*";
        testStarResourceHelper(path, "GET");
        testStarResourceHelper(path, "POST");
        testStarResourceHelper(path, "PUT");
        testStarResourceHelper(path, "DELETE");
    }

    public void testStarResourceExtension() throws Exception {
        final String path = rootPath + "/" + System.currentTimeMillis() + "/*.html";
        testStarResourceHelper(path, "GET");
        testStarResourceHelper(path, "POST");
        testStarResourceHelper(path, "PUT");
        testStarResourceHelper(path, "DELETE");
    }

    public void testStarResourceSelectorExtension() throws Exception {
        final String path = rootPath + "/" + System.currentTimeMillis() + "/*.print.a4.html";
        testStarResourceHelper(path, "GET");
        testStarResourceHelper(path, "POST");
        testStarResourceHelper(path, "PUT");
        testStarResourceHelper(path, "DELETE");
    }

    private void testStarResourceHelper(final String path, final String method) {
        final Resource res = resResolver.resolve(new ResourceResolverTestRequest(path, method));
        assertNotNull(res);
        assertTrue(ResourceUtil.isStarResource(res));
        assertEquals(StarResource.class.getName(), res.getClass().getName());
        assertEquals(StarResource.DEFAULT_RESOURCE_TYPE, res.getResourceType());
    }

    private static class ResourceResolverTestRequest implements
            HttpServletRequest {

        private final String pathInfo;

        private final String method;

        ResourceResolverTestRequest(String pathInfo) {
            this(pathInfo, null);
        }

        ResourceResolverTestRequest(String pathInfo, String httpMethod) {
            this.pathInfo = pathInfo;
            this.method = httpMethod;
        }

        public String getPathInfo() {
            return pathInfo;
        }

        public Object getAttribute(String name) {
            return null;
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
            return null;
        }

        public String getServerName() {
            return null;
        }

        public int getServerPort() {
            return 0;
        }

        public boolean isSecure() {
            return false;
        }

        public void removeAttribute(String name) {
        }

        public void setAttribute(String name, Object o) {
        }

        public void setCharacterEncoding(String env) {
        }

        public String getAuthType() {
            return null;
        }

        public String getContextPath() {
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
