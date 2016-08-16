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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.plugins.nodetype.NodeTypeConstants;
import org.apache.jackrabbit.oak.spi.commit.Observer;
import org.apache.jackrabbit.oak.spi.whiteboard.Whiteboard;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.apache.sling.testing.resourceresolver.MockResource;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DefaultRequirementHandlerTest extends RequirementBaseTest {

    private static final String[] SUPPORTED_PATHS = new String[] {"/content", "/another/content"};

    private static final String SUPPORTED_PATH = "/content/a";
    private static final String UNSUPPORTED_PATH = "/unsupportedPath";
    private static final String LOGIN_PATH = "/login/path";

    private final TestAuthRequirement authenticationRequirement = new TestAuthRequirement();
    private final TestResourceResolverFactory resourceResolverFactory = new TestResourceResolverFactory();
    private DefaultRequirementHandler drh = new DefaultRequirementHandler();

    @Before
    @Override
    public void before() throws Exception {
        super.before();

        context.registerService(AuthenticationRequirement.class, authenticationRequirement);
        context.registerService(ResourceResolverFactory.class, resourceResolverFactory);

        Map<String, Object> properties = ImmutableMap.<String, Object>of(DefaultRequirementHandler.PARAM_SUPPORTED_PATHS, SUPPORTED_PATHS);
        BundleContext bundleContext = context.bundleContext();
        MockOsgi.injectServices(drh, bundleContext);
        MockOsgi.activate(drh, bundleContext, properties);

        assertAuthReq(new AuthReq("setRequirements", ImmutableMap.<String, Boolean>of()));
        authenticationRequirement.calls.clear();
    }

    @Override
    protected Oak withEditors(Oak oak) {
        Whiteboard whiteboard = oak.getWhiteboard();
        whiteboard.register(Observer.class, new RequirementObserver(drh), Collections.emptyMap());
        return oak;
    }

    @Override
    boolean initJcrRepo() {
        return true;
    }

    private void assertAuthReq(AuthReq expected) {
        assertAuthReq(ImmutableList.of(expected));
    }

    private void assertAuthReq(List<AuthReq> expected) {
        assertEquals(expected, authenticationRequirement.calls);
    }

    @Test
    public void getLoginPathUnsupportedPath() {
        assertNull(drh.getLoginPath("/"));
        assertNull(drh.getLoginPath("/sling"));
    }

    @Test
    public void getLoginPathNoneDefined() {
        for (String str : SUPPORTED_PATHS) {
            assertNull(drh.getLoginPath(str));
        }
    }

    @Test
    public void getLoginPath() {
        drh.requirementAdded(SUPPORTED_PATH, LOGIN_PATH);

        assertNull(drh.getLoginPath("/"));
        assertNull(drh.getLoginPath("/content"));
        assertNull(drh.getLoginPath("/content/b"));
        assertNull(drh.getLoginPath("/content/aa"));

        assertEquals(LOGIN_PATH, drh.getLoginPath(SUPPORTED_PATH));
        assertEquals(LOGIN_PATH, drh.getLoginPath(SUPPORTED_PATH + "/b/c"));
    }

    @Test
    public void requirementAddedUnsupportedPath() {
        drh.requirementAdded(UNSUPPORTED_PATH, LOGIN_PATH);

        assertTrue(authenticationRequirement.calls.isEmpty());
    }

    @Test
    public void requirementAddedNoLoginPath() {
        drh.requirementAdded(SUPPORTED_PATH, null);

        assertAuthReq(new AuthReq("appendRequirements", ImmutableMap.of(SUPPORTED_PATH, true)));
    }

    @Test
    public void requirementAddedLoginException() {
        resourceResolverFactory.throwOnLogin = true;
        drh.requirementAdded(SUPPORTED_PATH, LOGIN_PATH);

        assertTrue(authenticationRequirement.calls.isEmpty());
    }

    @Test
    public void requirementAddedWithLoginPath() {
        drh.requirementAdded(SUPPORTED_PATH, LOGIN_PATH);

        assertAuthReq(new AuthReq("appendRequirements", ImmutableMap.of(SUPPORTED_PATH, true, LOGIN_PATH, false)));
    }

    @Test
    public void requirementRemovedUnsupportedPath() {
        drh.requirementRemoved(UNSUPPORTED_PATH, LOGIN_PATH);

        assertTrue(authenticationRequirement.calls.isEmpty());
    }

    @Test
    public void requirementRemovedNoLoginPath() {
        drh.requirementRemoved(SUPPORTED_PATH, null);

        assertAuthReq(new AuthReq("removeRequirements", ImmutableMap.of(SUPPORTED_PATH, true)));
    }

    @Test
    public void requirementRemovedWithLoginPath() {
        drh.requirementRemoved(SUPPORTED_PATH, LOGIN_PATH);

        assertAuthReq(new AuthReq("removeRequirements", ImmutableMap.of(SUPPORTED_PATH, true, LOGIN_PATH, false)));
    }

    @Test
    public void requirementRemovedLoginException() {
        resourceResolverFactory.throwOnLogin = true;
        drh.requirementRemoved(SUPPORTED_PATH, LOGIN_PATH);

        assertTrue(authenticationRequirement.calls.isEmpty());
    }

    @Test
    public void loginPathAddedUnsupportedPath() {
        drh.loginPathAdded(UNSUPPORTED_PATH, LOGIN_PATH);

        assertTrue(authenticationRequirement.calls.isEmpty());
    }

    @Test
    public void loginPathAdded() {
        drh.loginPathAdded(SUPPORTED_PATH, LOGIN_PATH);

        assertAuthReq(new AuthReq("appendRequirements", ImmutableMap.of(LOGIN_PATH, false)));
    }

    @Test
    public void loginPathAddedLoginException() {
        resourceResolverFactory.throwOnLogin = true;
        drh.loginPathAdded(SUPPORTED_PATH, LOGIN_PATH);

        assertTrue(authenticationRequirement.calls.isEmpty());
    }

    @Test
    public void loginPathChangedUnsupportedPath() {
        drh.loginPathChanged(UNSUPPORTED_PATH, "/login/path/before", "/login/path/after");

        assertTrue(authenticationRequirement.calls.isEmpty());
    }

    @Test
    public void loginPathChanged() {
        drh.loginPathChanged(SUPPORTED_PATH, "/login/path/before", "/login/path/after");

        assertAuthReq(ImmutableList.of(
                new AuthReq("removeRequirements", ImmutableMap.of("/login/path/before", false)),
                new AuthReq("appendRequirements", ImmutableMap.of("/login/path/after", false))));
    }

    @Test
    public void loginPathChangedLoginException() {
        resourceResolverFactory.throwOnLogin = true;
        drh.loginPathChanged(SUPPORTED_PATH, "/login/path/before", "/login/path/after");

        assertTrue(authenticationRequirement.calls.isEmpty());
    }

    @Test
    public void loginPathRemovedUnsupportedPath() {
        drh.loginPathRemoved(UNSUPPORTED_PATH, LOGIN_PATH);

        assertTrue(authenticationRequirement.calls.isEmpty());
    }

    @Test
    public void loginPathRemoved() {
        drh.loginPathRemoved(SUPPORTED_PATH, LOGIN_PATH);

        assertAuthReq(new AuthReq("removeRequirements", ImmutableMap.of(LOGIN_PATH, false)));
    }

    @Test
    public void loginPathRemovedLoginException() {
        resourceResolverFactory.throwOnLogin = true;
        drh.loginPathRemoved(SUPPORTED_PATH, LOGIN_PATH);

        assertTrue(authenticationRequirement.calls.isEmpty());
    }

    @Test
    public void activteExisting() throws Exception {
        Node rootNode = getSession().getRootNode();
        Node content = rootNode.addNode("content", NodeTypeConstants.NT_OAK_UNSTRUCTURED);
        content.addMixin(Constants.MIX_SLING_AUTHENTICATION_REQUIRED);

        Node child = content.addNode("child", NodeTypeConstants.NT_OAK_UNSTRUCTURED);
        child.addMixin(Constants.MIX_SLING_AUTHENTICATION_REQUIRED);
        child.setProperty(Constants.NAME_SLING_LOGIN_PATH, LOGIN_PATH);

        Node b = rootNode.addNode("a", NodeTypeConstants.NT_OAK_UNSTRUCTURED).addNode("b", NodeTypeConstants.NT_OAK_UNSTRUCTURED);
        b.addMixin(Constants.MIX_SLING_AUTHENTICATION_REQUIRED);
        b.setProperty(Constants.NAME_SLING_LOGIN_PATH, LOGIN_PATH);

        rootNode.getSession().save();
        authenticationRequirement.calls.clear();

        Map<String,Boolean> expected = ImmutableMap.of("/content", true, "/content/child", true, LOGIN_PATH, false);

        drh.activate(context.bundleContext(), ImmutableMap.<String, Object>of(DefaultRequirementHandler.PARAM_SUPPORTED_PATHS, SUPPORTED_PATHS));
        assertAuthReq(new AuthReq("setRequirements", expected));
    }

    @Test
    public void modified() throws Exception {
        String[] changedPaths = new String[] {"/a", "/b"};
        drh.modified(context.bundleContext(), ImmutableMap.<String,Object>of(DefaultRequirementHandler.PARAM_SUPPORTED_PATHS, changedPaths));

        assertAuthReq(ImmutableList.of(
                new AuthReq("clearRequirements", ImmutableMap.<String, Boolean>of()),
                new AuthReq("setRequirements", ImmutableMap.<String, Boolean>of())));
    }

    @Test
    public void modifiedExisting() throws Exception {
        Node rootNode = getSession().getRootNode();

        Node content = rootNode.addNode("content", NodeTypeConstants.NT_OAK_UNSTRUCTURED);
        content.addMixin(Constants.MIX_SLING_AUTHENTICATION_REQUIRED);

        Node b = rootNode.addNode("a", NodeTypeConstants.NT_OAK_UNSTRUCTURED).addNode("b", NodeTypeConstants.NT_OAK_UNSTRUCTURED);
        b.addMixin(Constants.MIX_SLING_AUTHENTICATION_REQUIRED);
        b.setProperty(Constants.NAME_SLING_LOGIN_PATH, LOGIN_PATH);

        rootNode.getSession().save();

        assertAuthReq(new AuthReq("appendRequirements", ImmutableMap.of("/content", true)));
        authenticationRequirement.calls.clear();

        String[] changedPaths = new String[] {"/a", "/b"};
        drh.modified(context.bundleContext(), ImmutableMap.<String,Object>of(DefaultRequirementHandler.PARAM_SUPPORTED_PATHS, changedPaths));

        assertAuthReq(ImmutableList.of(
                new AuthReq("clearRequirements", ImmutableMap.<String, Boolean>of()),
                new AuthReq("setRequirements", ImmutableMap.of("/a/b", true, LOGIN_PATH, false))));
    }

    @Test
    public void modifiedSameSupportedPaths() throws Exception {
        drh.modified(context.bundleContext(), ImmutableMap.<String,Object>of(DefaultRequirementHandler.PARAM_SUPPORTED_PATHS, SUPPORTED_PATHS));
        assertTrue(authenticationRequirement.calls.isEmpty());
    }

    @Test
    public void modifiedEmptySupportedPaths() throws Exception {
        drh.modified(context.bundleContext(), ImmutableMap.<String,Object>of(DefaultRequirementHandler.PARAM_SUPPORTED_PATHS, new String[0]));
        assertAuthReq(new AuthReq("clearRequirements", ImmutableMap.<String, Boolean>of()));
    }

    @Test
    public void modifiedMissingSupportedPaths() throws Exception {
        drh.modified(context.bundleContext(), ImmutableMap.<String,Object>of());
        assertAuthReq(new AuthReq("clearRequirements", ImmutableMap.<String, Boolean>of()));
    }

    @Test
    public void deactivate() {
        MockOsgi.deactivate(drh, ImmutableMap.<String, Object>of());

        assertAuthReq(new AuthReq("clearRequirements", ImmutableMap.<String, Boolean>of()));
    }

    private static final class TestAuthRequirement implements AuthenticationRequirement {

        private List<AuthReq> calls = new ArrayList<AuthReq>();

        @Override
        public void setRequirements(@Nonnull String id, @Nonnull Map<String, Boolean> requirements) {
            calls.add(new AuthReq("setRequirements", requirements));

        }

        @Override
        public void appendRequirements(@Nonnull String id, @Nonnull Map<String, Boolean> requirements) {
            calls.add(new AuthReq("appendRequirements", requirements));
        }

        @Override
        public void removeRequirements(@Nonnull String id, @Nonnull Map<String, Boolean> requirements) {
            calls.add(new AuthReq("removeRequirements", requirements));
        }

        @Override
        public void clearRequirements(@Nonnull String id) {
            calls.add(new AuthReq("clearRequirements", ImmutableMap.<String, Boolean>of()));
        }
    }

    private static final class AuthReq {
        private final String action;
        private final Map<String, Boolean> requirements;

        private AuthReq(@Nonnull String action, @Nonnull Map<String, Boolean> requirements) {
            this.action = action;
            this.requirements = requirements;
        }

        @Override
        public int hashCode() {
            return action.hashCode() * requirements.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (o instanceof AuthReq) {
                AuthReq other = (AuthReq) o;
                return action.equals(other.action) && requirements.equals(other.requirements);
            }
            return false;
        }

        @Override
        public String toString() {
            return action + ": " + Iterables.toString(requirements.entrySet());
        }
    }

    private class TestResourceResolverFactory implements ResourceResolverFactory {

        boolean throwOnLogin = false;

        @Nonnull
        @Override
        public ResourceResolver getResourceResolver(Map<String, Object> map) throws LoginException {
            if (throwOnLogin) {
                throw new LoginException();
            } else {
                return new TestResourceResolver();
            }        }

        @Nonnull
        @Override
        public ResourceResolver getAdministrativeResourceResolver(Map<String, Object> map) throws LoginException {
            return getResourceResolver(map);
        }

        @Nonnull
        @Override
        public ResourceResolver getServiceResourceResolver(Map<String, Object> map) throws LoginException {
            return getResourceResolver(map);
        }

        @Override
        public ResourceResolver getThreadResourceResolver() {
            return new TestResourceResolver();
        }
    }

    private class TestResourceResolver implements ResourceResolver {


        @Nonnull
        @Override
        public Resource resolve(@Nonnull HttpServletRequest httpServletRequest, @Nonnull String s) {
            throw new UnsupportedOperationException();
        }

        @Nonnull
        @Override
        public Resource resolve(@Nonnull String s) {
            return new MockResource(s, ImmutableMap.<String, Object>of(), this);
        }

        @Nonnull
        @Override
        public Resource resolve(@Nonnull HttpServletRequest httpServletRequest) {
            throw new UnsupportedOperationException();
        }

        @Nonnull
        @Override
        public String map(@Nonnull String s) {
            return s;
        }

        @Override
        public String map(@Nonnull HttpServletRequest httpServletRequest, @Nonnull String s) {
            return s;
        }

        @Override
        public Resource getResource(@Nonnull String s) {
            return new MockResource(s, ImmutableMap.<String, Object>of(), this);
        }

        @Override
        public Resource getResource(Resource resource, @Nonnull String s) {
            throw new UnsupportedOperationException();
        }

        @Nonnull
        @Override
        public String[] getSearchPath() {
            throw new UnsupportedOperationException();
        }

        @Nonnull
        @Override
        public Iterator<Resource> listChildren(@Nonnull Resource resource) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Resource getParent(@Nonnull Resource resource) {
            throw new UnsupportedOperationException();
        }

        @Nonnull
        @Override
        public Iterable<Resource> getChildren(@Nonnull Resource resource) {
            throw new UnsupportedOperationException();
        }

        @Nonnull
        @Override
        public Iterator<Resource> findResources(@Nonnull String s, String s1) {
            throw new UnsupportedOperationException();
        }

        @Nonnull
        @Override
        public Iterator<Map<String, Object>> queryResources(@Nonnull String s, String s1) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasChildren(@Nonnull Resource resource) {
            throw new UnsupportedOperationException();
        }

        @Nonnull
        @Override
        public ResourceResolver clone(Map<String, Object> map) throws LoginException {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isLive() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close() {
            // nop
        }

        @Override
        public String getUserID() {
            throw new UnsupportedOperationException();
        }

        @Nonnull
        @Override
        public Iterator<String> getAttributeNames() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object getAttribute(@Nonnull String s) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void delete(@Nonnull Resource resource) throws PersistenceException {
            throw new UnsupportedOperationException();
        }

        @Nonnull
        @Override
        public Resource create(@Nonnull Resource resource, @Nonnull String s, Map<String, Object> map) throws PersistenceException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void revert() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void commit() throws PersistenceException {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasChanges() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getParentResourceType(Resource resource) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getParentResourceType(String s) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isResourceType(Resource resource, String s) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void refresh() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Resource copy(String s, String s1) throws PersistenceException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Resource move(String s, String s1) throws PersistenceException {
            throw new UnsupportedOperationException();
        }

        @Override
        public <AdapterType> AdapterType adaptTo(@Nonnull Class<AdapterType> aClass) {
            if (aClass == Session.class) {
                return  (AdapterType) getSession();
            } else {
                throw new UnsupportedOperationException();
            }
        }
    }
}