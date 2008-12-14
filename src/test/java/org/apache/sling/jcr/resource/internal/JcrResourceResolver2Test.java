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
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.testing.jcr.RepositoryTestBase;
import org.apache.sling.commons.testing.jcr.RepositoryUtil;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.apache.sling.jcr.resource.internal.helper.MapEntries;
import org.apache.sling.jcr.resource.internal.helper.Mapping;
import org.apache.sling.jcr.resource.internal.helper.RedirectResource;
import org.apache.sling.jcr.resource.internal.helper.starresource.StarResource;

public class JcrResourceResolver2Test extends RepositoryTestBase {

    private String rootPath;

    private Node rootNode;

    private Node mapRoot;

    private JcrResourceResolverFactoryImpl resFac;

    private ResourceResolver resResolver;

    private MapEntries mapEntries;

    protected void setUp() throws Exception {
        super.setUp();
        assertTrue(RepositoryUtil.registerNodeType(getSession(),
            this.getClass().getResourceAsStream(
                "/SLING-INF/nodetypes/folder.cnd")));
        assertTrue(RepositoryUtil.registerNodeType(getSession(),
            this.getClass().getResourceAsStream(
                "/SLING-INF/nodetypes/resource.cnd")));
        assertTrue(RepositoryUtil.registerNodeType(getSession(),
            this.getClass().getResourceAsStream(
                "/SLING-INF/nodetypes/vanitypath.cnd")));
        assertTrue(RepositoryUtil.registerNodeType(getSession(),
            this.getClass().getResourceAsStream(
                "/SLING-INF/nodetypes/mapping.cnd")));

        // test data
        rootPath = "/test" + System.currentTimeMillis();
        rootNode = getSession().getRootNode().addNode(rootPath.substring(1),
            "nt:unstructured");

        // test mappings
        mapRoot = getSession().getRootNode().addNode("etc", "nt:folder");
        Node map = mapRoot.addNode("map", "sling:Mapping");
        Node https = map.addNode("https", "sling:Mapping");
        https.addNode("localhost.443", "sling:Mapping");
        Node http = map.addNode("http", "sling:Mapping");
        http.addNode("localhost.80", "sling:Mapping");

        session.save();

        resFac = new JcrResourceResolverFactoryImpl();

        Field repoField = resFac.getClass().getDeclaredField("repository");
        repoField.setAccessible(true);
        repoField.set(resFac, getRepository());

        // setup mappings
        Field mappingsField = resFac.getClass().getDeclaredField("mappings");
        mappingsField.setAccessible(true);
        mappingsField.set(resFac, new Mapping[] { new Mapping("/-/"),
            new Mapping(rootPath + "/-/") });

        // ensure using JcrResourceResolver2
        Field unrrField = resFac.getClass().getDeclaredField(
            "useNewResourceResolver");
        unrrField.setAccessible(true);
        unrrField.set(resFac, true);

        Field mapEntriesField = resFac.getClass().getDeclaredField("mapEntries");
        mapEntriesField.setAccessible(true);
        mapEntries = new MapEntries(resFac, getRepository());
        mapEntriesField.set(resFac, mapEntries);

        try {
            NamespaceRegistry nsr = session.getWorkspace().getNamespaceRegistry();
            nsr.registerNamespace(SlingConstants.NAMESPACE_PREFIX,
                JcrResourceConstants.SLING_NAMESPACE_URI);
        } catch (Exception e) {
            // don't care for now
        }

        resResolver = resFac.getResourceResolver(session);
    }

    @Override
    protected void tearDown() throws Exception {
        if (mapEntries != null) {
            mapEntries.dispose();
        }

        if (rootNode != null) {
            rootNode.remove();
        }

        if (mapRoot != null) {
            mapRoot.remove();
        }

        session.save();
    }

    public void testBasicAPIAssumptions() throws Exception {

        final String no_resource_path = "/no_resource/at/this/location";

        try {
            resResolver.resolve((String) null);
            fail("Expected NullPointerException trying to resolve null path");
        } catch (NullPointerException npe) {
            // expected
        }

        assertNull("Expecting no resource for relative path",
            resResolver.resolve("relPath/relPath"));

        assertNull("Expecting null if resource cannot be found",
            resResolver.resolve(no_resource_path));

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
        assertTrue("Resource must not be NonExistingResource",
            !(res2 instanceof NonExistingResource));
        assertEquals("Path must be the the root path", "/", res2.getPath());
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
        HttpServletRequest request = new ResourceResolverTestRequest(rootPath);
        Resource res = resResolver.resolve(request, rootPath);
        assertNotNull(res);
        assertEquals(rootPath, res.getPath());
        assertEquals(rootNode.getPrimaryNodeType().getName(),
            res.getResourceType());

        assertNotNull(res.adaptTo(Node.class));
        assertTrue(rootNode.isSame(res.adaptTo(Node.class)));

        // missing resource below root should resolve "missing resource"
        String path = rootPath + "/missing";
        res = resResolver.resolve(new ResourceResolverTestRequest(path), path);
        assertNotNull(res);
        assertEquals(path, res.getPath());
        assertEquals(Resource.RESOURCE_TYPE_NON_EXISTING, res.getResourceType());

        assertNull(res.adaptTo(Node.class));

        // root with selectors/ext should resolve root
        path = rootPath + ".print.a4.html";
        res = resResolver.resolve(new ResourceResolverTestRequest(path), path);
        assertNotNull(res);
        assertEquals(rootPath, res.getPath());
        assertEquals(rootNode.getPrimaryNodeType().getName(),
            res.getResourceType());

        assertNotNull(res.adaptTo(Node.class));
        assertTrue(rootNode.isSame(res.adaptTo(Node.class)));

        // missing resource should return NON_EXISTING Resource
        path = rootPath + System.currentTimeMillis();
        res = resResolver.resolve(new ResourceResolverTestRequest(path), path);
        assertNotNull(res);
        assertTrue(res instanceof NonExistingResource);
        assertEquals(path, res.getPath());
        assertEquals(Resource.RESOURCE_TYPE_NON_EXISTING, res.getResourceType());
    }

    public void testResolveResourceExternalRedirect() throws Exception {
        HttpServletRequest request = new ResourceResolverTestRequest(rootPath) {
            @Override
            public String getScheme() {
                return "https";
            }

            @Override
            public String getServerName() {
                return "localhost";
            }

            @Override
            public int getServerPort() {
                return -1;
            }
        };

        Node localhost443 = mapRoot.getNode("map/https/localhost.443");
        localhost443.setProperty(JcrResourceResolver2.PROP_REDIRECT_EXTERNAL,
            "http://localhost");
        session.save();

        Thread.sleep(1000L);

        resResolver = resFac.getResourceResolver(session);

        Resource res = resResolver.resolve(request, rootPath);
        assertNotNull(res);
        assertEquals(rootPath, res.getPath());
        assertTrue(res instanceof RedirectResource);
        assertNotNull(res.adaptTo(ValueMap.class));
        assertEquals("http://localhost" + rootPath,
            res.adaptTo(ValueMap.class).get("sling:target", String.class));
    }

    public void testResolveResourceInternalRedirectUrl() throws Exception {
        HttpServletRequest request = new ResourceResolverTestRequest(rootPath) {
            @Override
            public String getScheme() {
                return "https";
            }

            @Override
            public String getServerName() {
                return "localhost";
            }

            @Override
            public int getServerPort() {
                return -1;
            }
        };

        Node localhost443 = mapRoot.getNode("map/https/localhost.443");
        localhost443.setProperty(JcrResourceResolver2.PROP_REDIRECT_INTERNAL,
            "http://localhost");
        session.save();
        resResolver = resFac.getResourceResolver(session);

        Resource res = resResolver.resolve(request, rootPath);
        assertNotNull(res);
        assertEquals(rootPath, res.getPath());
        assertEquals(rootNode.getPrimaryNodeType().getName(),
            res.getResourceType());

        assertNotNull(res.adaptTo(Node.class));
        assertTrue(rootNode.isSame(res.adaptTo(Node.class)));
    }

    public void testResolveResourceInternalRedirectPath() throws Exception {
        HttpServletRequest request = new ResourceResolverTestRequest(rootPath) {
            @Override
            public String getScheme() {
                return "https";
            }

            @Override
            public String getServerName() {
                return "localhost";
            }

            @Override
            public int getServerPort() {
                return -1;
            }
        };

        Node localhost443 = mapRoot.getNode("map/https/localhost.443");
        Node toContent = localhost443.addNode("_playground_designground_",
            "sling:Mapping");
        toContent.setProperty(JcrResourceResolver2.PROP_REG_EXP,
            "(playground|designground)");
        toContent.setProperty(JcrResourceResolver2.PROP_REDIRECT_INTERNAL,
            "/content/$1");
        session.save();

        Thread.sleep(1000L);

        resResolver = resFac.getResourceResolver(session);

        Resource res = resResolver.resolve(request, "/playground.html");
        assertNotNull(res);
        assertEquals("/content/playground.html", res.getPath());

        res = resResolver.resolve(request, "/playground/en.html");
        assertNotNull(res);
        assertEquals("/content/playground/en.html", res.getPath());

        res = resResolver.resolve(request, "/libs/nt/folder.html");
        assertNotNull(res);
        assertEquals("/libs/nt/folder.html", res.getPath());
    }

    public void testResolveVirtualHostHttp80() throws Exception {
        HttpServletRequest request = new ResourceResolverTestRequest(rootPath) {
            @Override
            public String getScheme() {
                return "http";
            }

            @Override
            public String getServerName() {
                return "virtual.host.com";
            }

            @Override
            public int getServerPort() {
                return -1;
            }
        };

        Node virtualhost80 = mapRoot.getNode("map/http").addNode(
            "virtual.host.com.80", "sling:Mapping");
        virtualhost80.setProperty(JcrResourceResolver2.PROP_REDIRECT_INTERNAL,
            "/content/virtual");
        session.save();

        Thread.sleep(1000L);

        final Resource res0 = resResolver.resolve(request, "/playground.html");
        assertNotNull(res0);
        assertEquals("/content/virtual/playground.html", res0.getPath());

        final Resource res1 = resResolver.resolve(request,
            "/playground/en.html");
        assertNotNull(res1);
        assertEquals("/content/virtual/playground/en.html", res1.getPath());

        final String mapped0 = resResolver.map(request, res0.getPath());
        assertEquals("http://virtual.host.com/playground.html", mapped0);

        final String mapped1 = resResolver.map(request, res1.getPath());
        assertEquals("http://virtual.host.com/playground/en.html", mapped1);
    }

    public void testResolveVirtualHostHttp8080() throws Exception {
        HttpServletRequest request = new ResourceResolverTestRequest(rootPath) {
            @Override
            public String getScheme() {
                return "http";
            }

            @Override
            public String getServerName() {
                return "virtual.host.com";
            }

            @Override
            public int getServerPort() {
                return 8080;
            }
        };

        Node virtualhost80 = mapRoot.getNode("map/http").addNode(
            "virtual.host.com.8080", "sling:Mapping");
        virtualhost80.setProperty(JcrResourceResolver2.PROP_REDIRECT_INTERNAL,
            "/content/virtual");
        session.save();

        Thread.sleep(1000L);

        final Resource res0 = resResolver.resolve(request, "/playground.html");
        assertNotNull(res0);
        assertEquals("/content/virtual/playground.html", res0.getPath());

        final Resource res1 = resResolver.resolve(request,
            "/playground/en.html");
        assertNotNull(res1);
        assertEquals("/content/virtual/playground/en.html", res1.getPath());

        final String mapped0 = resResolver.map(request, res0.getPath());
        assertEquals("http://virtual.host.com:8080/playground.html", mapped0);

        final String mapped1 = resResolver.map(request, res1.getPath());
        assertEquals("http://virtual.host.com:8080/playground/en.html", mapped1);
    }

    public void testResolveVirtualHostHttps443() throws Exception {
        HttpServletRequest request = new ResourceResolverTestRequest(rootPath) {
            @Override
            public String getScheme() {
                return "https";
            }

            @Override
            public String getServerName() {
                return "virtual.host.com";
            }

            @Override
            public int getServerPort() {
                return -1;
            }
        };

        Node virtualhost443 = mapRoot.getNode("map/https").addNode(
            "virtual.host.com.443", "sling:Mapping");
        virtualhost443.setProperty(JcrResourceResolver2.PROP_REDIRECT_INTERNAL,
            "/content/virtual");
        session.save();

        Thread.sleep(1000L);

        final Resource res0 = resResolver.resolve(request, "/playground.html");
        assertNotNull(res0);
        assertEquals("/content/virtual/playground.html", res0.getPath());

        final Resource res1 = resResolver.resolve(request,
            "/playground/en.html");
        assertNotNull(res1);
        assertEquals("/content/virtual/playground/en.html", res1.getPath());

        final String mapped0 = resResolver.map(request, res0.getPath());
        assertEquals("https://virtual.host.com/playground.html", mapped0);

        final String mapped1 = resResolver.map(request, res1.getPath());
        assertEquals("https://virtual.host.com/playground/en.html", mapped1);
    }

    public void testResolveVirtualHostHttps4443() throws Exception {
        HttpServletRequest request = new ResourceResolverTestRequest(rootPath) {
            @Override
            public String getScheme() {
                return "https";
            }

            @Override
            public String getServerName() {
                return "virtual.host.com";
            }

            @Override
            public int getServerPort() {
                return 4443;
            }
        };

        Node virtualhost4443 = mapRoot.getNode("map/https").addNode(
            "virtual.host.com.4443", "sling:Mapping");
        virtualhost4443.setProperty(
            JcrResourceResolver2.PROP_REDIRECT_INTERNAL, "/content/virtual");
        session.save();

        Thread.sleep(1000L);

        final Resource res0 = resResolver.resolve(request, "/playground.html");
        assertNotNull(res0);
        assertEquals("/content/virtual/playground.html", res0.getPath());

        final Resource res1 = resResolver.resolve(request,
            "/playground/en.html");
        assertNotNull(res1);
        assertEquals("/content/virtual/playground/en.html", res1.getPath());

        final String mapped0 = resResolver.map(request, res0.getPath());
        assertEquals("https://virtual.host.com:4443/playground.html", mapped0);

        final String mapped1 = resResolver.map(request, res1.getPath());
        assertEquals("https://virtual.host.com:4443/playground/en.html",
            mapped1);
    }

    public void testResolveResourceAlias() throws Exception {
        // define an alias for the rootPath
        String alias = "testAlias";
        rootNode.setProperty(JcrResourceResolver2.PROP_ALIAS, alias);
        session.save();

        String path = ResourceUtil.normalize(ResourceUtil.getParent(rootPath)
            + "/" + alias + ".print.html");

        HttpServletRequest request = new ResourceResolverTestRequest(path);
        Resource res = resResolver.resolve(request, path);
        assertNotNull(res);
        assertEquals(rootPath, res.getPath());
        assertEquals(rootNode.getPrimaryNodeType().getName(),
            res.getResourceType());

        assertEquals(".print.html",
            res.getResourceMetadata().getResolutionPathInfo());

        assertNotNull(res.adaptTo(Node.class));
        assertTrue(rootNode.isSame(res.adaptTo(Node.class)));

        path = ResourceUtil.normalize(ResourceUtil.getParent(rootPath) + "/"
            + alias + ".print.html/suffix.pdf");

        request = new ResourceResolverTestRequest(path);
        res = resResolver.resolve(request, path);
        assertNotNull(res);
        assertEquals(rootPath, res.getPath());
        assertEquals(rootNode.getPrimaryNodeType().getName(),
            res.getResourceType());

        assertEquals(".print.html/suffix.pdf",
            res.getResourceMetadata().getResolutionPathInfo());

        assertNotNull(res.adaptTo(Node.class));
        assertTrue(rootNode.isSame(res.adaptTo(Node.class)));
    }

    public void testResolveResourceAliasJcrContent() throws Exception {
        // define an alias for the rootPath in the jcr:content child node
        String alias = "testAlias";
        Node content = rootNode.addNode("jcr:content", "nt:unstructured");
        content.setProperty(JcrResourceResolver2.PROP_ALIAS, alias);
        session.save();

        String path = ResourceUtil.normalize(ResourceUtil.getParent(rootPath)
            + "/" + alias + ".print.html");

        HttpServletRequest request = new ResourceResolverTestRequest(path);
        Resource res = resResolver.resolve(request, path);
        assertNotNull(res);
        assertEquals(rootPath, res.getPath());
        assertEquals(rootNode.getPrimaryNodeType().getName(),
            res.getResourceType());

        assertEquals(".print.html",
            res.getResourceMetadata().getResolutionPathInfo());

        assertNotNull(res.adaptTo(Node.class));
        assertTrue(rootNode.isSame(res.adaptTo(Node.class)));

        path = ResourceUtil.normalize(ResourceUtil.getParent(rootPath) + "/"
            + alias + ".print.html/suffix.pdf");

        request = new ResourceResolverTestRequest(path);
        res = resResolver.resolve(request, path);
        assertNotNull(res);
        assertEquals(rootPath, res.getPath());
        assertEquals(rootNode.getPrimaryNodeType().getName(),
            res.getResourceType());

        assertEquals(".print.html/suffix.pdf",
            res.getResourceMetadata().getResolutionPathInfo());

        assertNotNull(res.adaptTo(Node.class));
        assertTrue(rootNode.isSame(res.adaptTo(Node.class)));
    }

    public void testGetDoesNotGoUp() throws Exception {

        final String path = rootPath + "/nothing";

        {
            final Resource res = resResolver.resolve(
                new ResourceResolverTestRequest(path, "POST"), path);
            assertNotNull(res);
            assertEquals("POST request resolution does not go up the path",
                Resource.RESOURCE_TYPE_NON_EXISTING, res.getResourceType());
        }

        {
            final Resource res = resResolver.resolve(
                new ResourceResolverTestRequest(path, "GET"), path);
            assertNotNull(res);
            assertEquals("GET request resolution does not go up the path",
                Resource.RESOURCE_TYPE_NON_EXISTING, res.getResourceType());
        }
    }

    public void testGetRemovesExtensionInResolution() throws Exception {
        final String path = rootPath + ".whatever";
        final Resource res = resResolver.resolve(
            new ResourceResolverTestRequest(path, "GET"), path);
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
        final String path = rootPath + "/" + System.currentTimeMillis()
            + "/*.html";
        testStarResourceHelper(path, "GET");
        testStarResourceHelper(path, "POST");
        testStarResourceHelper(path, "PUT");
        testStarResourceHelper(path, "DELETE");
    }

    public void testStarResourceSelectorExtension() throws Exception {
        final String path = rootPath + "/" + System.currentTimeMillis()
            + "/*.print.a4.html";
        testStarResourceHelper(path, "GET");
        testStarResourceHelper(path, "POST");
        testStarResourceHelper(path, "PUT");
        testStarResourceHelper(path, "DELETE");
    }

    public void testSlingFolder() throws Exception {

        // create a folder
        String folderPath = "folder";
        Node folder = rootNode.addNode(folderPath, "sling:Folder");
        rootNode.save();

        // test default child node type
        Node child = folder.addNode("child0");
        folder.save();
        assertEquals("sling:Folder", child.getPrimaryNodeType().getName());

        // test explicit sling:Folder child
        child = folder.addNode("child1", "sling:Folder");
        folder.save();
        assertEquals("sling:Folder", child.getPrimaryNodeType().getName());

        // test explicit nt:folder child
        child = folder.addNode("child2", "nt:folder");
        folder.save();
        assertEquals("nt:folder", child.getPrimaryNodeType().getName());

        // test any child node -- use nt:unstructured here
        child = folder.addNode("child3", "nt:unstructured");
        folder.save();
        assertEquals("nt:unstructured", child.getPrimaryNodeType().getName());
    }

    public void testMap() throws Exception {
        String path = rootNode.getPath();
        String mapped = resResolver.map(path);
        assertEquals(path, mapped);
        
        Node child = rootNode.addNode("child");
        session.save();
        
        // absolute path, expect rootPath segment to be
        // cut off the mapped path because we map the rootPath
        // onto root
        path = "/child";
        mapped = resResolver.map(child.getPath());
        assertEquals(path, mapped);
    }
    
    public void testMapExtension() throws Exception {
        String path = rootNode.getPath();
        String mapped = resResolver.map(path);
        assertEquals(path, mapped);
        
        Node child = rootNode.addNode("child");
        session.save();
        
        // absolute path, expect rootPath segment to be
        // cut off the mapped path because we map the rootPath
        // onto root
        final String selExt = ".html";
        path = "/child" + selExt;
        mapped = resResolver.map(child.getPath() + selExt);
        assertEquals(path, mapped);
    }
    
    public void testMapSelectorsExtension() throws Exception {
        String path = rootNode.getPath();
        String mapped = resResolver.map(path);
        assertEquals(path, mapped);
        
        Node child = rootNode.addNode("child");
        session.save();
        
        // absolute path, expect rootPath segment to be
        // cut off the mapped path because we map the rootPath
        // onto root
        final String selExt = ".sel1.sel2.html";
        path = "/child" + selExt;
        mapped = resResolver.map(child.getPath() + selExt);
        assertEquals(path, mapped);
    }
    
    public void testMapExtensionSuffix() throws Exception {
        String path = rootNode.getPath();
        String mapped = resResolver.map(path);
        assertEquals(path, mapped);

        Node child = rootNode.addNode("child");
        session.save();

        // absolute path, expect rootPath segment to be
        // cut off the mapped path because we map the rootPath
        // onto root
        final String selExt = ".html/some/suffx.pdf";
        path = "/child" + selExt;
        mapped = resResolver.map(child.getPath() + selExt);
        assertEquals(path, mapped);
    }

    public void testAlias() throws Exception {
        
        Node child = rootNode.addNode("child");
        child.setProperty(JcrResourceResolver2.PROP_ALIAS, "kind");
        session.save();
        
        // expect kind due to alias and no parent due to mapping
        // the rootPath onto root
        String path = "/kind";
        String mapped = resResolver.map(child.getPath());
        assertEquals(path, mapped);
        
        Resource res = resResolver.resolve(null, path);
        Node resNode = res.adaptTo(Node.class);
        assertNotNull(resNode);
        
        assertEquals(child.getPath(), resNode.getPath());
    }
    
    public void testAliasExtension() throws Exception {
        
        final String selExt = ".html";
        
        Node child = rootNode.addNode("child");
        child.setProperty(JcrResourceResolver2.PROP_ALIAS, "kind");
        session.save();
        
        // expect kind due to alias and no parent due to mapping
        // the rootPath onto root
        String path = "/kind" + selExt;
        String mapped = resResolver.map(child.getPath() + selExt);
        assertEquals(path, mapped);
        
        Resource res = resResolver.resolve(null, path);
        Node resNode = res.adaptTo(Node.class);
        assertNotNull(resNode);
        
        assertEquals(child.getPath(), resNode.getPath());
    }
    
    public void testAliasSelectorsExtension() throws Exception {
        
        final String selExt = ".sel1.sel2.html";
        
        Node child = rootNode.addNode("child");
        child.setProperty(JcrResourceResolver2.PROP_ALIAS, "kind");
        session.save();
        
        // expect kind due to alias and no parent due to mapping
        // the rootPath onto root
        String path = "/kind" + selExt;
        String mapped = resResolver.map(child.getPath() + selExt);
        assertEquals(path, mapped);
        
        Resource res = resResolver.resolve(null, path);
        Node resNode = res.adaptTo(Node.class);
        assertNotNull(resNode);
        
        assertEquals(child.getPath(), resNode.getPath());
    }
    
    public void testAliasExtensionSuffix() throws Exception {
        
        final String selExt = ".html/some/suffx.pdf";
        
        Node child = rootNode.addNode("child");
        child.setProperty(JcrResourceResolver2.PROP_ALIAS, "kind");
        session.save();
        
        // expect kind due to alias and no parent due to mapping
        // the rootPath onto root
        String path = "/kind" + selExt;
        String mapped = resResolver.map(child.getPath() + selExt);
        assertEquals(path, mapped);
        
        Resource res = resResolver.resolve(null, path);
        Node resNode = res.adaptTo(Node.class);
        assertNotNull(resNode);
        
        assertEquals(child.getPath(), resNode.getPath());
    }
    
    // ---------- internal

    private void testStarResourceHelper(final String path, final String method) {
        final Resource res = resResolver.resolve(
            new ResourceResolverTestRequest(path, method), path);
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
            return "http";
        }

        public String getServerName() {
            return "localhost";
        }

        public int getServerPort() {
            return -1;
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
