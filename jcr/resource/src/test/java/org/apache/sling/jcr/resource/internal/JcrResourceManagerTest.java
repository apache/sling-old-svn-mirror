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
import javax.jcr.Session;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import junit.framework.TestCase;

import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.NodeProvider;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceManager;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.apache.sling.jcr.resource.internal.helper.Mapping;
import org.apache.sling.jcr.resource.testhelper.RepositoryUtil;

public class JcrResourceManagerTest extends TestCase {

    private Session session;

    private String root;

    private Node rootNode;

    private ResourceManager resMgr;

    protected void setUp() throws Exception {
        RepositoryUtil.startRepository();
        SlingRepository repository = RepositoryUtil.getRepository();

        JcrResourceManagerFactoryImpl resFac = new JcrResourceManagerFactoryImpl();

        Field repoField = resFac.getClass().getDeclaredField("repository");
        repoField.setAccessible(true);
        repoField.set(resFac, repository);

        Field mappingsField = resFac.getClass().getDeclaredField("mappings");
        mappingsField.setAccessible(true);
        mappingsField.set(resFac, new Mapping[] { Mapping.DIRECT });

        session = RepositoryUtil.getRepository().loginAdministrative(null);

        try {
            NamespaceRegistry nsr = session.getWorkspace().getNamespaceRegistry();
            nsr.registerNamespace(SlingConstants.NAMESPACE_PREFIX,
                JcrResourceConstants.SLING_NAMESPACE_URI);
        } catch (Exception e) {
            // don't care for now
        }

        resMgr = resFac.getResourceManager(session);

        root = "/test" + System.currentTimeMillis();
        rootNode = session.getRootNode().addNode(root.substring(1),
            "nt:unstructured");
        session.save();
    }

    @Override
    protected void tearDown() throws Exception {
        if (rootNode != null) {
            rootNode.remove();
            session.save();
        }

        if (session != null) {
            session.logout();
        }

        RepositoryUtil.stopRepository();
    }

    public void testGetResource() throws Exception {
        // existing resource
        Resource res = resMgr.getResource(root);
        assertNotNull(res);
        assertEquals(root, res.getURI());
        assertEquals(rootNode.getPrimaryNodeType().getName(),
            res.getResourceType());

        assertTrue(res instanceof NodeProvider);
        assertNotNull(((NodeProvider) res).getNode());
        assertTrue(rootNode.isSame(((NodeProvider) res).getNode()));

        // missing resource
        String path = root + "/missing";
        res = resMgr.getResource(path);
        assertNull(res);
    }

    public void testResolveResource() throws Exception {
        // existing resource
        Resource res = resMgr.resolve(new ResourceManagerTestRequest(root));
        assertNotNull(res);
        assertEquals(root, res.getURI());
        assertEquals(rootNode.getPrimaryNodeType().getName(),
            res.getResourceType());

        assertTrue(res instanceof NodeProvider);
        assertNotNull(((NodeProvider) res).getNode());
        assertTrue(rootNode.isSame(((NodeProvider) res).getNode()));

        // missing resource below root should resolve root
        String path = root + "/missing";
        res = resMgr.resolve(new ResourceManagerTestRequest(path));
        assertNotNull(res);
        assertEquals(root, res.getURI());
        assertEquals(rootNode.getPrimaryNodeType().getName(),
            res.getResourceType());

        assertTrue(res instanceof NodeProvider);
        assertNotNull(((NodeProvider) res).getNode());
        assertTrue(rootNode.isSame(((NodeProvider) res).getNode()));

        // root with selectors/ext should resolve root
        path = root + ".print.a4.html";
        res = resMgr.resolve(new ResourceManagerTestRequest(path));
        assertNotNull(res);
        assertEquals(root, res.getURI());
        assertEquals(rootNode.getPrimaryNodeType().getName(),
            res.getResourceType());

        assertTrue(res instanceof NodeProvider);
        assertNotNull(((NodeProvider) res).getNode());
        assertTrue(rootNode.isSame(((NodeProvider) res).getNode()));

        // missing resource should return NON_EXISTING Resource
        path = root + System.currentTimeMillis();
        res = resMgr.resolve(new ResourceManagerTestRequest(path));
        assertNotNull(res);
        assertTrue(res instanceof NonExistingResource);
        assertEquals(path, res.getURI());
        assertEquals(Resource.RESOURCE_TYPE_NON_EXISTING, res.getResourceType());

        assertTrue(res instanceof NodeProvider);
        assertNull(((NodeProvider) res).getNode());
    }

    private static class ResourceManagerTestRequest implements HttpServletRequest {

        private String pathInfo;

        ResourceManagerTestRequest(String pathInfo) {
            this.pathInfo = pathInfo;
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
            return null;
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
