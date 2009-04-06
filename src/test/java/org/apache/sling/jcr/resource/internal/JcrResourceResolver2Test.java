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
        
        // ensure namespace mangling
        Field mangeNamespacePrefixesField = resFac.getClass().getDeclaredField(
            "mangleNamespacePrefixes");
        mangeNamespacePrefixesField.setAccessible(true);
        mangeNamespacePrefixesField.set(resFac, true);

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


        // null resource is accessing /, which exists of course
        final Resource res00 = resResolver.resolve((String) null);
        assertNotNull(res00);
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
        HttpServletRequest request = new ResourceResolverTestRequest("https",
            null, -1, rootPath);
        Node localhost443 = mapRoot.getNode("map/https/localhost.443");
        localhost443.setProperty(JcrResourceResolver2.PROP_REDIRECT_EXTERNAL,
            "http://localhost");
        session.save();

        Thread.sleep(1000L);

        Resource res = resResolver.resolve(request, rootPath);
        assertNotNull(res);
        assertEquals(rootPath, res.getPath());
        assertTrue(res instanceof RedirectResource);
        assertNotNull(res.adaptTo(ValueMap.class));
        assertEquals("http://localhost" + rootPath,
            res.adaptTo(ValueMap.class).get("sling:target", String.class));
    }

    public void testResolveResourceInternalRedirectUrl() throws Exception {
        HttpServletRequest request = new ResourceResolverTestRequest("https",
            null, -1, rootPath);
        Node localhost443 = mapRoot.getNode("map/https/localhost.443");
        localhost443.setProperty(JcrResourceResolver2.PROP_REDIRECT_INTERNAL,
            "http://localhost");
        session.save();

        Thread.sleep(1000L);

        Resource res = resResolver.resolve(request, rootPath);
        assertNotNull(res);
        assertEquals(rootPath, res.getPath());
        assertEquals(rootNode.getPrimaryNodeType().getName(),
            res.getResourceType());

        assertNotNull(res.adaptTo(Node.class));
        assertTrue(rootNode.isSame(res.adaptTo(Node.class)));
    }

    public void testResolveResourceInternalRedirectPath() throws Exception {
        HttpServletRequest request = new ResourceResolverTestRequest("https",
            null, -1, rootPath);
        Node localhost443 = mapRoot.getNode("map/https/localhost.443");
        Node toContent = localhost443.addNode("_playground_designground_",
            "sling:Mapping");
        toContent.setProperty(JcrResourceResolver2.PROP_REG_EXP,
            "(playground|designground)");
        toContent.setProperty(JcrResourceResolver2.PROP_REDIRECT_INTERNAL,
            "/content/$1");
        session.save();

        Thread.sleep(1000L);

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
        HttpServletRequest request = new ResourceResolverTestRequest(null,
            "virtual.host.com", -1, rootPath);
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

        final String mapped00 = resResolver.map(res0.getPath());
        assertEquals("http://virtual.host.com/playground.html", mapped00);
        final String mapped01 = resResolver.map(request, res0.getPath());
        assertEquals("/playground.html", mapped01);

        final String mapped10 = resResolver.map(res1.getPath());
        assertEquals("http://virtual.host.com/playground/en.html", mapped10);
        final String mapped11 = resResolver.map(request, res1.getPath());
        assertEquals("/playground/en.html", mapped11);
    }

    public void testResolveVirtualHostHttp80Multiple() throws Exception {

        final String de = "de";
        final String en = "en";
        final String hostDE = de + ".host.com";
        final String hostEN = en + ".host.com";
        final String contentDE = "/content/" + de;
        final String contentEN = "/content/" + en;

        Node virtualhost80 = mapRoot.getNode("map/http").addNode(
            hostDE + ".80", "sling:Mapping");
        virtualhost80.setProperty(JcrResourceResolver2.PROP_REDIRECT_INTERNAL,
            contentDE);
        virtualhost80 = mapRoot.getNode("map/http").addNode(hostEN + ".80",
            "sling:Mapping");
        virtualhost80.setProperty(JcrResourceResolver2.PROP_REDIRECT_INTERNAL,
            contentEN);
        session.save();

        Thread.sleep(1000L);

        // de content mapping

        final HttpServletRequest requestDE = new ResourceResolverTestRequest(
            null, hostDE, -1, rootPath);
        final Resource resDE0 = resResolver.resolve(requestDE,
            "/playground.html");
        assertNotNull(resDE0);
        assertEquals(contentDE + "/playground.html", resDE0.getPath());

        final Resource resDE1 = resResolver.resolve(requestDE,
            "/playground/index.html");
        assertNotNull(resDE1);
        assertEquals(contentDE + "/playground/index.html", resDE1.getPath());

        final String mappedDE00 = resResolver.map(resDE0.getPath());
        assertEquals("http://" + hostDE + "/playground.html", mappedDE00);
        final String mappedDE01 = resResolver.map(requestDE, resDE0.getPath());
        assertEquals("/playground.html", mappedDE01);

        final String mappedDE10 = resResolver.map(resDE1.getPath());
        assertEquals("http://" + hostDE + "/playground/index.html", mappedDE10);
        final String mappedDE11 = resResolver.map(requestDE, resDE1.getPath());
        assertEquals("/playground/index.html", mappedDE11);

        // en content mapping

        final HttpServletRequest requestEN = new ResourceResolverTestRequest(
            null, hostEN, -1, rootPath);
        final Resource resEN0 = resResolver.resolve(requestEN,
            "/playground.html");
        assertNotNull(resEN0);
        assertEquals(contentEN + "/playground.html", resEN0.getPath());

        final Resource resEN1 = resResolver.resolve(requestEN,
            "/playground/index.html");
        assertNotNull(resEN1);
        assertEquals(contentEN + "/playground/index.html", resEN1.getPath());

        final String mappedEN00 = resResolver.map(resEN0.getPath());
        assertEquals("http://" + hostEN + "/playground.html", mappedEN00);
        final String mappedEN01 = resResolver.map(requestEN, resEN0.getPath());
        assertEquals("/playground.html", mappedEN01);

        final String mappedEN10 = resResolver.map(resEN1.getPath());
        assertEquals("http://" + hostEN + "/playground/index.html", mappedEN10);
        final String mappedEN11 = resResolver.map(requestEN, resEN1.getPath());
        assertEquals("/playground/index.html", mappedEN11);
    }

    public void testResolveVirtualHostHttp80MultipleRoot() throws Exception {

        final String de = "de";
        final String en = "en";
        final String fr = "fr";
        final String hostDE = de + ".host.com";
        final String hostEN = en + ".host.com";
        final String hostFR = fr + ".host.com";

        Node virtualhost80 = mapRoot.getNode("map/http").addNode(
            hostDE + ".80", "sling:Mapping");
        virtualhost80.setProperty(JcrResourceResolver2.PROP_REDIRECT_INTERNAL,
            "/");
        virtualhost80 = mapRoot.getNode("map/http").addNode(hostEN + ".80",
            "sling:Mapping");
        virtualhost80.setProperty(JcrResourceResolver2.PROP_REDIRECT_INTERNAL,
            "/");
        session.save();

        Thread.sleep(1000L);

        // de content mapping

        final HttpServletRequest requestDE = new ResourceResolverTestRequest(
            null, hostDE, -1, rootPath);
        final Resource resDE0 = resResolver.resolve(requestDE,
            "/playground.html");
        assertNotNull(resDE0);
        assertEquals("/playground.html", resDE0.getPath());

        final Resource resDE1 = resResolver.resolve(requestDE,
            "/playground/index.html");
        assertNotNull(resDE1);
        assertEquals("/playground/index.html", resDE1.getPath());

        final String mappedDE00 = resResolver.map(resDE0.getPath());
        assertEquals("http://" + hostDE + "/playground.html", mappedDE00);
        final String mappedDE01 = resResolver.map(requestDE, resDE0.getPath());
        assertEquals("/playground.html", mappedDE01);

        final String mappedDE10 = resResolver.map(resDE1.getPath());
        assertEquals("http://" + hostDE + "/playground/index.html", mappedDE10);
        final String mappedDE11 = resResolver.map(requestDE, resDE1.getPath());
        assertEquals("/playground/index.html", mappedDE11);

        // en content mapping

        final HttpServletRequest requestEN = new ResourceResolverTestRequest(
            null, hostEN, -1, rootPath);
        final Resource resEN0 = resResolver.resolve(requestEN,
            "/playground.html");
        assertNotNull(resEN0);
        assertEquals("/playground.html", resEN0.getPath());

        final Resource resEN1 = resResolver.resolve(requestEN,
            "/playground/index.html");
        assertNotNull(resEN1);
        assertEquals("/playground/index.html", resEN1.getPath());

        // here we get back the hostDE, since this is the first configured
        // and we have no request information to map the correct of the
        // duplicate entries !
        final String mappedEN00 = resResolver.map(resEN0.getPath());
        assertEquals("http://" + hostDE + "/playground.html", mappedEN00);

        // here we expect the path without scheme/host/port since we have
        // the request and can select the right mapping
        final String mappedEN01 = resResolver.map(requestEN, resEN0.getPath());
        assertEquals("/playground.html", mappedEN01);

        // here we get back the hostDE, since this is the first configured
        // and we have no request information to map the correct of the
        // duplicate entries !
        final String mappedEN10 = resResolver.map(resEN1.getPath());
        assertEquals("http://" + hostDE + "/playground/index.html", mappedEN10);

        // here we expect the path without scheme/host/port since we have
        // the request and can select the right mapping
        final String mappedEN11 = resResolver.map(requestEN, resEN1.getPath());
        assertEquals("/playground/index.html", mappedEN11);

        final HttpServletRequest requestFR = new ResourceResolverTestRequest(
            null, hostFR, -1, rootPath);
        final Resource resFR1 = resResolver.resolve(requestFR,
            "/playground/index.html");
        assertNotNull(resFR1);
        assertEquals("/playground/index.html", resFR1.getPath());

        // here we get back the hostDE, since this is the first configured
        // and we have no request information to map the correct of the
        // duplicate entries !
        final String mappedFR10 = resResolver.map(resFR1.getPath());
        assertEquals("http://" + hostDE + "/playground/index.html", mappedFR10);

        // here we get back the hostDE, since this is the first configured
        // and we have request information which does not map any of the
        // configured duplicate entries !
        final String mappedFR11 = resResolver.map(requestFR, resFR1.getPath());
        assertEquals("http://" + hostDE + "/playground/index.html", mappedFR11);
    }

    public void testResolveVirtualHostHttp8080() throws Exception {
        HttpServletRequest request = new ResourceResolverTestRequest(null,
            "virtual.host.com", 8080, rootPath);
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

        final String mapped00 = resResolver.map(res0.getPath());
        assertEquals("http://virtual.host.com:8080/playground.html", mapped00);
        final String mapped01 = resResolver.map(request, res0.getPath());
        assertEquals("/playground.html", mapped01);

        final String mapped10 = resResolver.map(res1.getPath());
        assertEquals("http://virtual.host.com:8080/playground/en.html",
            mapped10);
        final String mapped11 = resResolver.map(request, res1.getPath());
        assertEquals("/playground/en.html", mapped11);
    }

    public void testResolveVirtualHostHttp8080Root() throws Exception {
        HttpServletRequest request = new ResourceResolverTestRequest(null,
            "virtual.host.com", 8080, rootPath);
        Node virtualhost80 = mapRoot.getNode("map/http").addNode(
            "virtual.host.com.8080", "sling:Mapping");
        virtualhost80.setProperty(JcrResourceResolver2.PROP_REDIRECT_INTERNAL,
            "/");
        session.save();

        Thread.sleep(1000L);

        final Resource res0 = resResolver.resolve(request, "/playground.html");
        assertNotNull(res0);
        assertEquals("/playground.html", res0.getPath());

        final Resource res1 = resResolver.resolve(request,
            "/playground/en.html");
        assertNotNull(res1);
        assertEquals("/playground/en.html", res1.getPath());

        final String mapped00 = resResolver.map(res0.getPath());
        assertEquals("http://virtual.host.com:8080/playground.html", mapped00);
        final String mapped01 = resResolver.map(request, res0.getPath());
        assertEquals("/playground.html", mapped01);

        final String mapped10 = resResolver.map(res1.getPath());
        assertEquals("http://virtual.host.com:8080/playground/en.html",
            mapped10);
        final String mapped11 = resResolver.map(request, res1.getPath());
        assertEquals("/playground/en.html", mapped11);
    }

    public void testResolveVirtualHostHttps443() throws Exception {
        HttpServletRequest request = new ResourceResolverTestRequest("https",
            "virtual.host.com", -1, rootPath);
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

        final String mapped00 = resResolver.map(res0.getPath());
        assertEquals("https://virtual.host.com/playground.html", mapped00);
        final String mapped01 = resResolver.map(request, res0.getPath());
        assertEquals("/playground.html", mapped01);

        final String mapped10 = resResolver.map(res1.getPath());
        assertEquals("https://virtual.host.com/playground/en.html", mapped10);
        final String mapped11 = resResolver.map(request, res1.getPath());
        assertEquals("/playground/en.html", mapped11);
    }

    public void testResolveVirtualHostHttps4443() throws Exception {
        HttpServletRequest request = new ResourceResolverTestRequest("https",
            "virtual.host.com", 4443, rootPath);
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

        final String mapped00 = resResolver.map(res0.getPath());
        assertEquals("https://virtual.host.com:4443/playground.html", mapped00);
        final String mapped01 = resResolver.map(request, res0.getPath());
        assertEquals("/playground.html", mapped01);

        final String mapped10 = resResolver.map(res1.getPath());
        assertEquals("https://virtual.host.com:4443/playground/en.html",
            mapped10);
        final String mapped11 = resResolver.map(request, res1.getPath());
        assertEquals("/playground/en.html", mapped11);
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
    
    public void testMapNamespaceMangling() throws Exception {
        
        final String mapHost = "virtual.host.com";
        final String mapRootPath = "/content/virtual";
        final String contextPath = "/context";

        Node virtualhost80 = mapRoot.getNode("map/http").addNode(
            mapHost + ".80", "sling:Mapping");
        virtualhost80.setProperty(JcrResourceResolver2.PROP_REDIRECT_INTERNAL,
            mapRootPath);
        session.save();

        Thread.sleep(1000L);
        
        //---------------------------------------------------------------------
        // tests expecting paths without context
        
        final HttpServletRequest virtualRequest = new ResourceResolverTestRequest(null,
            mapHost, -1, rootPath);
        
        // simple mapping - cut off prefix and add host
        final String pathv0 = "/sample";
        final String mappedv0 = resResolver.map(virtualRequest, mapRootPath + pathv0);
        assertEquals("Expect unmangled path", pathv0, mappedv0);
        
        // expected name mangling without host prefix
        final String pathv1 = "/sample/jcr:content";
        final String mangledv1 = "/sample/_jcr_content";
        final String mappedv1 = resResolver.map(virtualRequest, mapRootPath + pathv1);
        assertEquals("Expect mangled path", mangledv1, mappedv1);
        

        //---------------------------------------------------------------------
        // tests expecting paths with context "/context"
        
        ((ResourceResolverTestRequest) virtualRequest).setContextPath(contextPath);
        
        // simple mapping - cut off prefix and add host
        final String pathvc0 = "/sample";
        final String mappedvc0 = resResolver.map(virtualRequest, mapRootPath + pathvc0);
        assertEquals("Expect unmangled path", contextPath + pathv0, mappedvc0);

        // expected name mangling without host prefix
        final String pathvc1 = "/sample/jcr:content";
        final String mangledvc1 = "/sample/_jcr_content";
        final String mappedvc1 = resResolver.map(virtualRequest, mapRootPath + pathvc1);
        assertEquals("Expect mangled path", contextPath + mangledvc1, mappedvc1);

        //---------------------------------------------------------------------
        // tests expecting absolute URLs without context
        
        final HttpServletRequest foreignRequest = new ResourceResolverTestRequest(null,
            "foreign.host.com", -1, rootPath);
        
        final String pathf0 = "/sample";
        final String mappedf0 = resResolver.map(foreignRequest, mapRootPath + pathf0);
        assertEquals("Expect unmangled absolute URI", "http://" + mapHost + pathf0, mappedf0);
        
        final String pathf1 = "/sample/jcr:content";
        final String mangledf1 = "/sample/_jcr_content";
        final String mappedf1 = resResolver.map(foreignRequest, mapRootPath + pathf1);
        assertEquals("Expect mangled absolute URI", "http://" + mapHost + mangledf1, mappedf1);

        //---------------------------------------------------------------------
        // tests expecting absolute URLs with context "/context"
        
        ((ResourceResolverTestRequest) foreignRequest).setContextPath(contextPath);

        final String pathfc0 = "/sample";
        final String mappedfc0 = resResolver.map(foreignRequest, mapRootPath + pathfc0);
        assertEquals("Expect unmangled absolute URI", "http://" + mapHost + contextPath + pathfc0, mappedfc0);

        final String pathfc1 = "/sample/jcr:content";
        final String mangledfc1 = "/sample/_jcr_content";
        final String mappedfc1 = resResolver.map(foreignRequest, mapRootPath + pathfc1);
        assertEquals("Expect mangled absolute URI", "http://" + mapHost + contextPath + mangledfc1, mappedfc1);
    }
    
    public void testMapContext() throws Exception {
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

    private static final class ResourceResolverTestRequest implements
            HttpServletRequest {

        private final String pathInfo;

        private final String method;

        private final String scheme;

        private final String host;

        private final int port;
        
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
