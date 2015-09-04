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

package org.apache.sling.jcr.resource.it;

import static org.junit.Assert.*;

import java.util.Arrays;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.VersionManager;
import javax.naming.NamingException;

import org.apache.commons.lang.ArrayUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.testing.mock.sling.MockResolverProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ResourceVersioningTest {

    private VersionManager versionManager;

    private Node testNode;

    private ResourceResolver resolver;

    private Session session;

    @Before
    public void setUp() throws Exception {
        resolver = MockResolverProvider.getResourceResolver();
        session = resolver.adaptTo(Session.class);
        versionManager = session.getWorkspace().getVersionManager();
        registerNamespace("sling", "http://sling.apache.org/jcr/sling/1.0");

        Node testRoot = session.getRootNode().addNode("content");
        testNode = testRoot.addNode("test");
        testNode.addMixin(JcrConstants.MIX_VERSIONABLE);
        session.save();

        versionManager.checkout(testNode.getPath());
        testNode.setProperty("prop", "oldvalue");
        testNode.addNode("x").addNode("y").setProperty("child_prop", "child_old_value");
        session.save();
        versionManager.checkin(testNode.getPath());

        versionManager.checkout(testNode.getPath());
        testNode.setProperty("prop", "newvalue");
        testNode.getProperty("x/y/child_prop").setValue("child_new_value");
        session.save();
        versionManager.checkin(testNode.getPath());
    }

    @After
    public void tearDown() throws Exception {
        session.removeItem("/content");
        session.save();
        resolver.close();
    }

    @Test
    public void getResourceOnVersionableNode() throws RepositoryException, NamingException {
        Resource resource = resolver.getResource("/content/test;v='1.0'");
        String prop = resource.adaptTo(ValueMap.class).get("prop", String.class);
        assertEquals("oldvalue", prop);
        assertEquals("/content/test;v='1.0'", resource.getPath());
    }

    @Test
    public void getResourceOnVersionableProperty() throws RepositoryException, NamingException {
        Resource resource = resolver.getResource("/content/test/prop;v='1.0'");
        String prop = resource.adaptTo(String.class);
        assertEquals("oldvalue", prop);
        assertEquals("/content/test/prop;v='1.0'", resource.getPath());
    }

    @Test
    public void resolveOnVersionableNode() throws RepositoryException, NamingException {
        for (String path : Arrays.asList("/content/test;v='1.0'.html", "/content/test.html;v=1.0",
                "/content/test;v='1.0'.html/some/suffix", "/content/test.html;v=1.0/some/suffix")) {
            Resource resource = resolver.resolve(path);
            String prop = resource.adaptTo(ValueMap.class).get("prop", String.class);
            assertEquals("oldvalue", prop);
            assertEquals("/content/test;v='1.0'", resource.getPath());
        }
    }

    @Test
    public void getResourceOnVersionableDescendant() throws RepositoryException, NamingException {
        Resource resource = resolver.getResource("/content/test/x/y;v='1.0'");
        String prop = resource.adaptTo(ValueMap.class).get("child_prop", String.class);
        assertEquals("child_old_value", prop);
        assertEquals("/content/test/x/y;v='1.0'", resource.getPath());
    }

    @Test
    public void getResourceOnVersionableDescendantProperty() throws RepositoryException, NamingException {
        Resource resource = resolver.getResource("/content/test/x/y/child_prop;v='1.0'");
        String prop = resource.adaptTo(String.class);
        assertEquals("child_old_value", prop);
        assertEquals("/content/test/x/y/child_prop;v='1.0'", resource.getPath());
    }

    @Test
    public void getChildOnVersionableResource() throws RepositoryException, NamingException {
        Resource resource = resolver.getResource("/content/test;v='1.0'").getChild("x/y");
        String prop = resource.adaptTo(ValueMap.class).get("child_prop", String.class);
        assertEquals("child_old_value", prop);
        assertEquals("/content/test/x/y;v='1.0'", resource.getPath());
    }

    @Test
    public void listChildrenOnVersionableResource() throws RepositoryException, NamingException {
        Resource resource = resolver.getResource("/content/test/x;v='1.0'").listChildren().next();
        String prop = resource.adaptTo(ValueMap.class).get("child_prop", String.class);
        assertEquals("child_old_value", prop);
        assertEquals("/content/test/x/y;v='1.0'", resource.getPath());
    }

    @Test
    public void getParentOnVersionableResource() throws RepositoryException, NamingException {
        Resource resource = resolver.getResource("/content/test/x;v='1.0'").getParent();
        String prop = resource.adaptTo(ValueMap.class).get("prop", String.class);
        assertEquals("newvalue", prop);
        assertEquals("/content/test", resource.getPath());
    }

    private void registerNamespace(String prefix, String uri) throws RepositoryException {
        NamespaceRegistry registry = session.getWorkspace().getNamespaceRegistry();
        if (!ArrayUtils.contains(registry.getPrefixes(), prefix)) {
            session.getWorkspace().getNamespaceRegistry().registerNamespace(prefix, uri);
        }

    }
}
