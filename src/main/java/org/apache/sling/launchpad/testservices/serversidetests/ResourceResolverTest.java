/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.launchpad.testservices.serversidetests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.junit.annotations.SlingAnnotationsTestRunner;
import org.apache.sling.junit.annotations.TestReference;
import org.apache.sling.launchpad.testservices.events.EventsCounter;
import org.apache.sling.launchpad.testservices.exported.FakeSlingHttpServletRequest;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(SlingAnnotationsTestRunner.class)
public class ResourceResolverTest {

    public static final String PROP_REDIRECT_INTERNAL = "sling:internalRedirect";
    public static final String PROP_REDIRECT_EXTERNAL = "sling:redirect";
    public static final String MAPPING_EVENT_TOPIC = "org/apache/sling/api/resource/ResourceResolverMapping/CHANGED";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static ResourceResolver resResolver;
    private static Session session;
    private String rootPath;
    private Node rootNode;
    private Node mapRoot;
    private String [] vanity;
    private static List<String> toDelete = new ArrayList<String>();
    private static ResourceResolverFactory cleanupResolverFactory;
    
    @TestReference
    private EventsCounter eventsCounter;
    
    @TestReference
    private ResourceResolverFactory resourceResolverFactory;  
    
    // How long to wait for mapping updates
    public static final String MAPPING_UPDATE_TIMEOUT_MSEC = "ResourceResolverTest.mapping.update.timeout.msec";
    private static final long updateTimeout = Long.valueOf(System.getProperty(MAPPING_UPDATE_TIMEOUT_MSEC, "10000"));

    public ResourceResolverTest() throws Exception {
        logger.info("updateTimeout = {}, use {} system property to change", updateTimeout, MAPPING_UPDATE_TIMEOUT_MSEC);
    }
    
    /** Save a Session that has mapping changes, and wait for the OSGi event
     *  that signals that mappings have been updated.
     */
    private void saveMappings(Session session) throws Exception {
        final int oldEventsCount = eventsCounter.getEventsCount(MAPPING_EVENT_TOPIC);
        session.save();
        final long timeout = System.currentTimeMillis() + updateTimeout;
        while(System.currentTimeMillis() < timeout) {
            if(eventsCounter.getEventsCount(MAPPING_EVENT_TOPIC) != oldEventsCount) {
                // Sleeping here shouldn't be needed but it looks
                // like mappings are not immediately updated once the event arrives
                Thread.sleep(updateTimeout / 50);
                return;
            }
            try {
                Thread.sleep(10);
            } catch(InterruptedException ignore) {
            }
        }
        fail("Timeout waiting for " + MAPPING_EVENT_TOPIC + " event, after " + updateTimeout + " msec");
    }
    
    private Node maybeCreateNode(Node parent, String name, String type) throws RepositoryException {
        if(parent.hasNode(name)) {
            return parent.getNode(name);
        } else {
            return parent.addNode(name, type);
        }
    }

    @Before
    public synchronized void setup() throws Exception {
        closeResolver();
        resResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
        cleanupResolverFactory = resourceResolverFactory;
        session = resResolver.adaptTo(Session.class);
        
        // Do the mappings setup only once, and clean it up 
        // after all tests
        rootNode = maybeCreateNode(session.getRootNode(), "content", "nt:unstructured");
        rootPath = rootNode.getPath();
        session.save();
        if(toDelete.isEmpty()) {
            final Node mapRoot = maybeCreateNode(session.getRootNode(), "etc", "nt:folder");
            final Node map = maybeCreateNode(mapRoot, "map", "sling:Mapping");
            final Node http = maybeCreateNode(map, "http", "sling:Mapping");
            maybeCreateNode(http, "localhost.80", "sling:Mapping");
            final Node https = maybeCreateNode(map, "https", "sling:Mapping");
            maybeCreateNode(https, "localhost.443", "sling:Mapping");
            toDelete.add(map.getPath());
            toDelete.add(rootNode.getPath());
        }
        
        mapRoot = session.getNode("/etc");
        
        // define a vanity path for the rootPath
        vanity = new String[] {"testVanity","testV", "testVanityToUpdate"};
        rootNode.setProperty("sling:vanityPath", vanity);
        rootNode.addMixin("sling:VanityPath");
        session.save();
    }
    
    private void closeResolver() {
        if(session != null) {
            if(session.isLive()) {
                session.logout();
            }
            session = null;
        }
        if (resResolver != null ) {
            resResolver.close();
            resResolver = null;
        }
        
    }

    @AfterClass
    public static void deleteTestNodes() throws Exception {
        final ResourceResolver resolver = cleanupResolverFactory.getAdministrativeResourceResolver(null);
        final Session session = resolver.adaptTo(Session.class);
        
        try {
            for(String path : toDelete) {
                if(session.itemExists(path)) {
                    session.getItem(path).remove();
                }
            }
            toDelete.clear();
            session.save();
        } finally {
            session.logout();
            resolver.close();
        }
    }

    @Test public void test_clone_based_on_anonymous() throws Exception {
        final ResourceResolver anon0 = this.resourceResolverFactory.getResourceResolver((Map<String, Object>) null);
        final Session anon0Session = anon0.adaptTo(Session.class);
        assertEquals("anonymous", anon0.getUserID());

        // same user and workspace
        final ResourceResolver anon1 = anon0.clone(null);
        final Session anon1Session = anon1.adaptTo(Session.class);
        assertEquals(anon0.getUserID(), anon1.getUserID());
        assertEquals(anon0Session.getWorkspace().getName(),
            anon1Session.getWorkspace().getName());
        anon1.close();

        // same workspace but admin user
        final Map<String, Object> admin0Cred = new HashMap<String, Object>();
        admin0Cred.put(ResourceResolverFactory.USER, "admin");
        admin0Cred.put(ResourceResolverFactory.PASSWORD, "admin".toCharArray());
        final ResourceResolver admin0 = anon0.clone(admin0Cred);
        final Session admin0Session = admin0.adaptTo(Session.class);
        assertEquals("admin", admin0.getUserID());
        assertEquals(anon0Session.getWorkspace().getName(),
            admin0Session.getWorkspace().getName());
        admin0.close();

        // same user but different workspace
        final Map<String, Object> anon2Cred = new HashMap<String, Object>();
        final ResourceResolver anon2 = anon0.clone(anon2Cred);
        assertEquals("anonymous", anon2.getUserID());
        anon2.close();

        // different user and workspace
        final Map<String, Object> admin1Cred = new HashMap<String, Object>();
        admin1Cred.put(ResourceResolverFactory.USER, "admin");
        admin1Cred.put(ResourceResolverFactory.PASSWORD, "admin".toCharArray());
        final ResourceResolver admin1 = anon0.clone(admin1Cred);
        assertEquals("admin", admin1.getUserID());
        admin1.close();

        anon0.close();
    }

    @Test public void test_clone_based_on_admin() throws Exception {
        final ResourceResolver admin0 = this.resourceResolverFactory.getAdministrativeResourceResolver((Map<String, Object>) null);
        final Session admin0Session = admin0.adaptTo(Session.class);
        assertEquals("admin", admin0.getUserID());

        // same user and workspace
        final ResourceResolver admin1 = admin0.clone(null);
        final Session admin1Session = admin1.adaptTo(Session.class);
        assertEquals(admin0.getUserID(), admin1.getUserID());
        assertEquals(admin0Session.getWorkspace().getName(),
            admin1Session.getWorkspace().getName());
        admin1.close();

        // same workspace but anonymous user
        final Map<String, Object> anon0Cred = new HashMap<String, Object>();
        anon0Cred.put(ResourceResolverFactory.USER, "anonymous");
        final ResourceResolver anon0 = admin0.clone(anon0Cred);
        final Session anon0Session = anon0.adaptTo(Session.class);
        assertEquals("anonymous", anon0.getUserID());
        assertEquals(admin0Session.getWorkspace().getName(),
            anon0Session.getWorkspace().getName());
        anon0.close();

        // same user but different workspace
        final Map<String, Object> admin2Cred = new HashMap<String, Object>();
        final ResourceResolver admin2 = admin0.clone(admin2Cred);
        assertEquals("admin", admin2.getUserID());
        admin2.close();

        // different user and workspace
        final Map<String, Object> anon1Cred = new HashMap<String, Object>();
        anon1Cred.put(ResourceResolverFactory.USER, "anonymous");
        final ResourceResolver anon1 = admin0.clone(anon1Cred);
        assertEquals("anonymous", anon1.getUserID());
        anon1.close();

        admin0.close();
    }

    /*@Test public void test_attributes_from_session() throws Exception {
        // test assumes admin password is admin (which is default)

        final Credentials creds0 = new SimpleCredentials("admin",
            "admin".toCharArray());
        final Session session0 = getRepository().login(creds0);
        final Map<String, Object> authInfo = new HashMap<String, Object>();
        authInfo.put(ResourceResolverFactory.USER, "admin");
        authInfo.put(ResourceResolverFactory.PASSWORD, "admin".toCharArray());
        authInfo.put("testAttributeString", "AStringValue");
        authInfo.put("testAttributeNumber", 999);

        final ResourceResolver resolver0 = resFac.getResourceResolver(authInfo);

        final Iterator<String> attrNames0 = resolver0.getAttributeNames();
        assertTrue("Expected one attribute", attrNames0.hasNext());
        final String attrName0 = attrNames0.next();
        assertEquals("Expected attribute name to address session",
            JcrResourceConstants.AUTHENTICATION_INFO_SESSION, attrName0);
        assertFalse("Expected no more attributes", attrNames0.hasNext());
        assertEquals("Expected session attribute to be the session", session0,
            resolver0.getAttribute(attrName0));

        assertEquals("Expected no Session attributes", 0,
            session0.getAttributeNames().length);

        resolver0.close();
        assertTrue("Expect session to still be live after resolver close",
            session0.isLive());
        session0.logout();

        final SimpleCredentials creds1 = new SimpleCredentials("admin",
            "admin".toCharArray());
        creds1.setAttribute("testAttributeString", "AStringValue");
        creds1.setAttribute("testAttributeNumber", 999);
        final Session session1 = getRepository().login(creds1);

        final ResourceResolver resolver1 = resFac.getResourceResolver(authInfo);

        assertEquals("Expected 2 Session attributes", 2,
            session1.getAttributeNames().length);
        assertEquals("AStringValue",
            session1.getAttribute("testAttributeString"));
        assertEquals(999, session1.getAttribute("testAttributeNumber"));

        assertEquals(
            session1,
            resolver1.getAttribute(JcrResourceConstants.AUTHENTICATION_INFO_SESSION));
        assertEquals("AStringValue",
            resolver1.getAttribute("testAttributeString"));
        assertEquals(999, resolver1.getAttribute("testAttributeNumber"));

        final Iterator<String> attrNames1 = resolver1.getAttributeNames();
        assertTrue("Expecting first attribute", attrNames1.hasNext());
        attrNames1.next();
        assertTrue("Expecting second attribute", attrNames1.hasNext());
        attrNames1.next();
        assertTrue("Expecting third attribute", attrNames1.hasNext());
        attrNames1.next();
        assertFalse("Expecting no more attribute", attrNames1.hasNext());

        resolver1.close();
        assertTrue("Expect session to still be live after resolver close",
            session1.isLive());
        session1.logout();
    }*/

    @Test public void test_attributes_from_authInfo() throws Exception {
        final Map<String, Object> authInfo = new HashMap<String, Object>();
        authInfo.put(ResourceResolverFactory.USER, "admin");
        authInfo.put(ResourceResolverFactory.PASSWORD, "admin".toCharArray());
        authInfo.put("testAttributeString", "AStringValue");
        authInfo.put("testAttributeNumber", 999);
        final ResourceResolver rr = this.resourceResolverFactory.getResourceResolver(authInfo);
        final Session s = rr.adaptTo(Session.class);

        try {
            assertEquals("Expect 3 session attributes", 3,
                s.getAttributeNames().length);
            assertEquals("AStringValue", s.getAttribute("testAttributeString"));
            assertEquals(999, s.getAttribute("testAttributeNumber"));
            assertEquals("admin", s.getAttribute(ResourceResolverFactory.USER));
            assertNull(session.getAttribute(ResourceResolverFactory.PASSWORD));

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
        } finally {
            rr.close();
        }
    }

    @Test public void testGetResource() throws Exception {
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

    @Test public void testResolveResource() throws Exception {
        // existing resource
        HttpServletRequest request = new FakeSlingHttpServletRequest(rootPath);
        Resource res = resResolver.resolve(request, rootPath);
        assertNotNull(res);
        assertEquals(rootPath, res.getPath());
        assertEquals(rootNode.getPrimaryNodeType().getName(),
            res.getResourceType());

        assertNotNull(res.adaptTo(Node.class));
        assertTrue(rootNode.isSame(res.adaptTo(Node.class)));

        // missing resource below root should resolve "missing resource"
        String path = rootPath + "/missing";
        res = resResolver.resolve(new FakeSlingHttpServletRequest(path), path);
        assertNotNull(res);
        assertEquals(path, res.getPath());
        assertEquals(Resource.RESOURCE_TYPE_NON_EXISTING, res.getResourceType());

        assertNull(res.adaptTo(Node.class));

        // root with selectors/ext should resolve root
        path = rootPath + ".print.a4.html";
        res = resResolver.resolve(new FakeSlingHttpServletRequest(path), path);
        assertNotNull(res);
        assertEquals(rootPath, res.getPath());
        assertEquals(rootNode.getPrimaryNodeType().getName(),
            res.getResourceType());

        assertNotNull(res.adaptTo(Node.class));
        assertTrue(rootNode.isSame(res.adaptTo(Node.class)));

        // missing resource should return NON_EXISTING Resource
        path = rootPath + System.currentTimeMillis();
        res = resResolver.resolve(new FakeSlingHttpServletRequest(path), path);
        assertNotNull(res);
        assertTrue(ResourceUtil.isNonExistingResource(res));
        assertEquals(path, res.getPath());
        assertEquals(Resource.RESOURCE_TYPE_NON_EXISTING, res.getResourceType());
    }



    @Test public void testResolveResourceExternalRedirect() throws Exception {
        HttpServletRequest request = new FakeSlingHttpServletRequest("https",
            null, -1, rootPath);
        Node localhost443 = mapRoot.getNode("map/https/localhost.443");
        localhost443.setProperty(PROP_REDIRECT_EXTERNAL,
            "http://localhost");

        try {
            saveMappings(session);
            Resource res = resResolver.resolve(request, rootPath);
            assertNotNull(res);
            assertEquals(rootPath, res.getPath());
            assertEquals("sling:redirect", res.getResourceType());
            assertNotNull(res.adaptTo(ValueMap.class));
            assertEquals("http://localhost" + rootPath,
                res.adaptTo(ValueMap.class).get("sling:target", String.class));
        } finally {
            localhost443.getProperty(PROP_REDIRECT_EXTERNAL).remove();
            session.save();
        }
    }

    @Test public void testResolveResourceInternalRedirectUrl() throws Exception {
        HttpServletRequest request = new FakeSlingHttpServletRequest("https",
            null, -1, rootPath);
        Node localhost443 = mapRoot.getNode("map/https/localhost.443");
        localhost443.setProperty(PROP_REDIRECT_INTERNAL,
            "http://localhost");

        try {
            saveMappings(session);
            Resource res = resResolver.resolve(request, rootPath);
            assertNotNull(res);
            assertEquals(rootPath, res.getPath());
            assertEquals(rootNode.getPrimaryNodeType().getName(),
                res.getResourceType());

            assertNotNull(res.adaptTo(Node.class));
            assertTrue(rootNode.isSame(res.adaptTo(Node.class)));
        } finally {
            localhost443.getProperty(PROP_REDIRECT_INTERNAL).remove();
            session.save();
        }
    }

    @Test public void testResolveResourceInternalRedirectPath() throws Exception {
        HttpServletRequest request = new FakeSlingHttpServletRequest("https",
            null, -1, rootPath);
        Node localhost443 = mapRoot.getNode("map/https/localhost.443");

        Node toContent = localhost443.addNode("_playground_designground_",
            "sling:Mapping");
        toContent.setProperty("sling:match",
            "(playground|designground)");
        toContent.setProperty(PROP_REDIRECT_INTERNAL,
            "/content/$1");
        try {
            saveMappings(session);
            Resource res = resResolver.resolve(request, "/playground.html");
            assertNotNull(res);
            assertEquals("/content/playground.html", res.getPath());

            res = resResolver.resolve(request, "/playground/en.html");
            assertNotNull(res);
            assertEquals("/content/playground/en.html", res.getPath());

            res = resResolver.resolve(request, "/libs/nt/folder.html");
            assertNotNull(res);
            assertEquals("/libs/nt/folder.html", res.getPath());
        } finally {
            toContent.remove();
            session.save();
        }
    }
    
    @Test public void testResolveResourceInternalRedirectPathAndVanityPath() throws Exception {
        HttpServletRequest request = new FakeSlingHttpServletRequest("https",
                null, -1, rootPath);
        Node localhost443 = mapRoot.getNode("map/https/localhost.443");

        localhost443.setProperty(PROP_REDIRECT_INTERNAL,
                "/example");
        try {
            saveMappings(session);
            Resource res = resResolver.resolve(request, vanity[0]);
            assertNotNull(res);
            assertEquals(rootPath, res.getPath());
            
            //see SLING-3428
            res = resResolver.resolve(request, vanity[1]);
            assertNotNull(res);
            assertEquals("/example/"+vanity[1], res.getPath());
        } finally {
            localhost443.getProperty(PROP_REDIRECT_INTERNAL).remove();
            session.save();
        }     
    }

    @Test public void testResolveResourceInternalRedirectPathUpdate() throws Exception {
        HttpServletRequest request = new FakeSlingHttpServletRequest("https", null, -1, rootPath);
        Node localhost443 = mapRoot.getNode("map/https/localhost.443");
        Node toContent = localhost443.addNode("_playground_designground_", "sling:Mapping");
        toContent.setProperty("sling:match", "(playground|designground)");
        toContent.setProperty(PROP_REDIRECT_INTERNAL, "/content/$1");

        try {
            saveMappings(session);
            Resource res = resResolver.resolve(request, "/playground.html");
            assertNotNull(res);
            assertEquals("/content/playground.html", res.getPath());

            res = resResolver.resolve(request, "/playground/en.html");
            assertNotNull(res);
            assertEquals("/content/playground/en.html", res.getPath());

            res = resResolver.resolve(request, "/libs/nt/folder.html");
            assertNotNull(res);
            assertEquals("/libs/nt/folder.html", res.getPath());

            // update the match
            toContent.setProperty("sling:match", "(homeground|foreignground)");
            saveMappings(session);

            res = resResolver.resolve(request, "/homeground.html");
            assertNotNull(res);
            assertEquals("/content/homeground.html", res.getPath());

            res = resResolver.resolve(request, "/foreignground/en.html");
            assertNotNull(res);
            assertEquals("/content/foreignground/en.html", res.getPath());

            res = resResolver.resolve(request, "/libs/nt/folder.html");
            assertNotNull(res);
            assertEquals("/libs/nt/folder.html", res.getPath());
        } finally {
            toContent.remove();
            session.save();
        }
    }

    @Test public void testResolveResourceInternalRedirectExact() throws Exception {
        HttpServletRequest request = new FakeSlingHttpServletRequest("https",
            null, -1, rootPath);
        Node localhost443 = mapRoot.getNode("map/https/localhost.443");
        Node toContent = localhost443.addNode("virtual", "sling:Mapping");
        toContent.setProperty("sling:match", "virtual$");
        toContent.setProperty(PROP_REDIRECT_INTERNAL,
            "/content/virtual.html");

        try {
            saveMappings(session);
            Resource res = resResolver.resolve(request, "/virtual");
            assertNotNull(res);
            assertEquals("/content/virtual.html", res.getPath());

            res = resResolver.resolve(request, "/virtual.html");
            assertNotNull(res);
            assertEquals("/virtual.html", res.getPath());

            res = resResolver.resolve(request, "/virtual/child.html");
            assertNotNull(res);
            assertEquals("/virtual/child.html", res.getPath());

            String url = resResolver.map(null, "/content/virtual.html");
            assertNotNull(url);
            assertEquals("https://localhost/virtual", url);

            url = resResolver.map(request, "/content/virtual.html");
            assertNotNull(url);
            assertEquals("/virtual", url);
        } finally {
            toContent.remove();
            session.save();
        }
    }

    @Test public void testResolveResourceInternalRedirectDepthFirst()
            throws Exception {
        HttpServletRequest request = new FakeSlingHttpServletRequest("https",
            null, -1, rootPath);

        // map anything
        Node localhost443 = mapRoot.getNode("map/https/localhost.443");
        localhost443.setProperty(PROP_REDIRECT_INTERNAL,
            "/content2");

        // map only ../virtual
        Node toContent = localhost443.addNode("virtual", "sling:Mapping");
        toContent.setProperty("sling:match", "virtual$");
        toContent.setProperty(PROP_REDIRECT_INTERNAL,
            "/content2/virtual.html");

        try {
            saveMappings(session);
            Resource res = resResolver.resolve(request, "/virtual");
            assertNotNull(res);
            assertEquals("/content2/virtual.html", res.getPath());

            res = resResolver.resolve(request, "/virtual.html");
            assertNotNull(res);
            assertEquals("/content2/virtual.html", res.getPath());

            res = resResolver.resolve(request, "/virtual/child.html");
            assertNotNull(res);
            assertEquals("/content2/virtual/child.html", res.getPath());
        } finally {
            localhost443.getProperty(PROP_REDIRECT_INTERNAL).remove();
            toContent.remove();
            session.save();
        }
    }

    @Test public void testResolveVirtualHostHttp80() throws Exception {
        HttpServletRequest request = new FakeSlingHttpServletRequest(null,
            "virtual.host.com", -1, rootPath);
        Node virtualhost80 = mapRoot.getNode("map/http").addNode(
            "virtual.host.com.80", "sling:Mapping");
        virtualhost80.setProperty(PROP_REDIRECT_INTERNAL,
            "/content/virtual");

        try {
            saveMappings(session);
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
        } finally {
            virtualhost80.remove();
            session.save();
        }
    }

    @Test public void testResolveVirtualHostHttp80Multiple() throws Exception {

        final String de = "de";
        final String en = "en";
        final String hostDE = de + ".host.com";
        final String hostEN = en + ".host.com";
        final String contentDE = "/content/" + de;
        final String contentEN = "/content/" + en;

        Node virtualhost80a = mapRoot.getNode("map/http").addNode(
            hostDE + ".80", "sling:Mapping");
        virtualhost80a.setProperty(PROP_REDIRECT_INTERNAL,
            contentDE);
        Node virtualhost80 = mapRoot.getNode("map/http").addNode(hostEN + ".80",
            "sling:Mapping");
        virtualhost80.setProperty(PROP_REDIRECT_INTERNAL,
            contentEN);

        try {
            saveMappings(session);
            
            // de content mapping
            final HttpServletRequest requestDE = new FakeSlingHttpServletRequest(
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

            final HttpServletRequest requestEN = new FakeSlingHttpServletRequest(
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
        } finally {
            virtualhost80a.remove();
            virtualhost80.remove();
            session.save();
        }
    }

    @Test public void testResolveVirtualHostHttp80MultipleRoot() throws Exception {

        final String de = "de";
        final String en = "en";
        final String fr = "fr";
        final String hostDE = de + ".host.com";
        final String hostEN = en + ".host.com";
        final String hostFR = fr + ".host.com";

        Node virtualhost80a = mapRoot.getNode("map/http").addNode(
            hostDE + ".80", "sling:Mapping");
        virtualhost80a.setProperty(PROP_REDIRECT_INTERNAL,
            "/");
        Node virtualhost80 = mapRoot.getNode("map/http").addNode(hostEN + ".80",
            "sling:Mapping");
        virtualhost80.setProperty(PROP_REDIRECT_INTERNAL,
            "/");

        try {
            saveMappings(session);
            
            // de content mapping
            final HttpServletRequest requestDE = new FakeSlingHttpServletRequest(
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

            final HttpServletRequest requestEN = new FakeSlingHttpServletRequest(
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

            final HttpServletRequest requestFR = new FakeSlingHttpServletRequest(
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
        } finally {
            virtualhost80a.remove();
            virtualhost80.remove();
            session.save();
        }
    }

    @Test public void testResolveVirtualHostHttp8080() throws Exception {
        HttpServletRequest request = new FakeSlingHttpServletRequest(null,
            "virtual.host.com", 8080, rootPath);
        Node virtualhost80 = mapRoot.getNode("map/http").addNode(
            "virtual.host.com.8080", "sling:Mapping");
        virtualhost80.setProperty(PROP_REDIRECT_INTERNAL,
            "/content/virtual");

        try {
            saveMappings(session);
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
        } finally {
            virtualhost80.remove();
            session.save();
        }
    }

    @Test public void testResolveVirtualHostHttp8080Root() throws Exception {
        HttpServletRequest request = new FakeSlingHttpServletRequest(null,
            "virtual.host.com", 8080, rootPath);
        Node virtualhost80 = mapRoot.getNode("map/http").addNode(
            "virtual.host.com.8080", "sling:Mapping");
        virtualhost80.setProperty(PROP_REDIRECT_INTERNAL,
            "/");

        try {
            saveMappings(session);
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
        } finally {
            virtualhost80.remove();
            session.save();
        }
    }

    @Test public void testResolveVirtualHostHttps443() throws Exception {
        HttpServletRequest request = new FakeSlingHttpServletRequest("https",
            "virtual.host.com", -1, rootPath);
        Node virtualhost443 = mapRoot.getNode("map/https").addNode(
            "virtual.host.com.443", "sling:Mapping");
        virtualhost443.setProperty(PROP_REDIRECT_INTERNAL,
            "/content/virtual");

        try {
            saveMappings(session);
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
        } finally {
            virtualhost443.remove();
            session.save();
        }
    }

    @Test public void testResolveVirtualHostHttps4443() throws Exception {
        HttpServletRequest request = new FakeSlingHttpServletRequest("https",
            "virtual.host.com", 4443, rootPath);
        Node virtualhost4443 = mapRoot.getNode("map/https").addNode(
            "virtual.host.com.4443", "sling:Mapping");
        virtualhost4443.setProperty(PROP_REDIRECT_INTERNAL,
            "/content/virtual");
        
        try {
            saveMappings(session);
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
        } finally {
            virtualhost4443.remove();
            session.save();
        }
    }

    @Test public void testResolveVirtualHostHttpVsHttps() throws Exception {

        final String host0 = "www.host.com";
        final String host1 = "secure.host.com";
        final String content = "/content/page";

        Node virtualhost80 = mapRoot.getNode("map/http").addNode(host0 + ".80",
            "sling:Mapping");
        virtualhost80.setProperty(PROP_REDIRECT_INTERNAL,
            content);
        Node virtualhost443 = mapRoot.getNode("map/https").addNode(
            host0 + ".443", "sling:Mapping");
        virtualhost443.setProperty(PROP_REDIRECT_INTERNAL,
            content);

        // HTTP request
        try {
            saveMappings(session);
            final HttpServletRequest requestHttp0 = new FakeSlingHttpServletRequest(
                null, host0, -1, rootPath);
            final Resource resHttp0 = resResolver.resolve(requestHttp0, "/playground.html");
            assertNotNull(resHttp0);
            assertEquals(content + "/playground.html", resHttp0.getPath());

            final Resource resHttp1 = resResolver.resolve(requestHttp0,
            "/playground/index.html");
            assertNotNull(resHttp1);
            assertEquals(content + "/playground/index.html", resHttp1.getPath());

            // HTTPS request
            final HttpServletRequest requestHttps0 = new FakeSlingHttpServletRequest(
                "https", host0, -1, rootPath);
            final Resource resHttps0 = resResolver.resolve(requestHttps0, "/playground.html");
            assertNotNull(resHttps0);
            assertEquals(content + "/playground.html", resHttps0.getPath());

            final Resource resHttps1 = resResolver.resolve(requestHttps0,
                "/playground/index.html");
            assertNotNull(resHttps1);
            assertEquals(content + "/playground/index.html", resHttps1.getPath());

            // HTTP Mapping

            final String mappedHttp00 = resResolver.map(resHttp0.getPath());
            assertEquals("http://" + host0 + "/playground.html", mappedHttp00);
            final String mappedHttp01 = resResolver.map(requestHttp0, resHttp0.getPath());
            assertEquals("/playground.html", mappedHttp01);

            final String mappedHttp10 = resResolver.map(resHttp1.getPath());
            assertEquals("http://" + host0 + "/playground/index.html", mappedHttp10);
            final String mappedHttp11 = resResolver.map(requestHttp0, resHttp1.getPath());
            assertEquals("/playground/index.html", mappedHttp11);

            // HTTPS Mapping

            final HttpServletRequest requestHttp1 = new FakeSlingHttpServletRequest(
                null, host1, -1, rootPath);
            final HttpServletRequest requestHttps1 = new FakeSlingHttpServletRequest(
                "https", host1, -1, rootPath);

            final String mappedHttps00 = resResolver.map(resHttps0.getPath());
            assertEquals("http://" + host0 + "/playground.html", mappedHttps00);
            final String mappedHttps01 = resResolver.map(requestHttps0, resHttps0.getPath());
            assertEquals("/playground.html", mappedHttps01);
            final String mappedHttps02 = resResolver.map(requestHttp1, resHttps0.getPath());
            assertEquals("http://" + host0 + "/playground.html", mappedHttps02);
            final String mappedHttps03 = resResolver.map(requestHttps1, resHttps0.getPath());
            assertEquals("https://" + host0 + "/playground.html", mappedHttps03);

            final String mappedHttps10 = resResolver.map(resHttps1.getPath());
            assertEquals("http://" + host0 + "/playground/index.html", mappedHttps10);
            final String mappedHttps11 = resResolver.map(requestHttps0, resHttps1.getPath());
            assertEquals("/playground/index.html", mappedHttps11);
            final String mappedHttps12 = resResolver.map(requestHttp1, resHttps1.getPath());
            assertEquals("http://" + host0 + "/playground/index.html", mappedHttps12);
            final String mappedHttps13 = resResolver.map(requestHttps1, resHttps1.getPath());
            assertEquals("https://" + host0 + "/playground/index.html", mappedHttps13);
        } finally {
            virtualhost80.remove();
            virtualhost443.remove();
            session.save();
        }
    }

    @Test public void testResolveResourceAlias() throws Exception {
        // define an alias for the rootPath
        String alias = "testAlias";
        rootNode.setProperty("sling:alias", alias);

        try {
            saveMappings(session);
            String path = ResourceUtil.normalize(ResourceUtil.getParent(rootPath)
                + "/" + alias + ".print.html");

            HttpServletRequest request = new FakeSlingHttpServletRequest(path);
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

            request = new FakeSlingHttpServletRequest(path);
            res = resResolver.resolve(request, path);
            assertNotNull(res);
            assertEquals(rootPath, res.getPath());
            assertEquals(rootNode.getPrimaryNodeType().getName(),
                res.getResourceType());

            assertEquals(".print.html/suffix.pdf",
                res.getResourceMetadata().getResolutionPathInfo());

            assertNotNull(res.adaptTo(Node.class));
            assertTrue(rootNode.isSame(res.adaptTo(Node.class)));
        } finally {
            rootNode.getProperty("sling:alias").remove();
            session.save();
        }
    }

    @Test public void testResolveResourceAliasJcrContent() throws Exception {
        // define an alias for the rootPath in the jcr:content child node
        String alias = "testAlias";
        Node content = rootNode.addNode("jcr:content", "nt:unstructured");
        content.setProperty("sling:alias", alias);

        try {
            saveMappings(session);
            String path = ResourceUtil.normalize(ResourceUtil.getParent(rootPath)
                + "/" + alias + ".print.html");

            HttpServletRequest request = new FakeSlingHttpServletRequest(path);
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

            request = new FakeSlingHttpServletRequest(path);
            res = resResolver.resolve(request, path);
            assertNotNull(res);
            assertEquals(rootPath, res.getPath());
            assertEquals(rootNode.getPrimaryNodeType().getName(),
                res.getResourceType());

            assertEquals(".print.html/suffix.pdf",
                res.getResourceMetadata().getResolutionPathInfo());

            assertNotNull(res.adaptTo(Node.class));
            assertTrue(rootNode.isSame(res.adaptTo(Node.class)));

            path = ResourceUtil.normalize(ResourceUtil.getParent(rootPath)
                    + "/" + alias + "/" + alias + ".print.html");
            res = resResolver.resolve(request, path);
            assertEquals("GET request resolution does not go up the path",
                    Resource.RESOURCE_TYPE_NON_EXISTING, res.getResourceType());

            Node child = rootNode.addNode("child", "nt:unstructured");
            child.setProperty("sling:alias", alias);

            try {
                saveMappings(session);
                res = resResolver.resolve(request, path);
                assertEquals(child.getPath(), res.getPath());
            } finally {
                child.remove();
                session.save();
            }
        } finally {
            content.remove();
            session.save();
        }

    }

    @Test public void testResolveVanityPath() throws Exception {
        String path = ResourceUtil.normalize(ResourceUtil.getParent(rootPath)
                + "/" + vanity[0] + ".print.html");

        HttpServletRequest request = new FakeSlingHttpServletRequest(path);
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
                + vanity[0] + ".print.html/suffix.pdf");

        request = new FakeSlingHttpServletRequest(path);
        res = resResolver.resolve(request, path);
        assertNotNull(res);
        assertEquals(rootPath, res.getPath());
        assertEquals(rootNode.getPrimaryNodeType().getName(),
                res.getResourceType());

        assertEquals(".print.html/suffix.pdf",
                res.getResourceMetadata().getResolutionPathInfo());

        assertNotNull(res.adaptTo(Node.class));
        assertTrue(rootNode.isSame(res.adaptTo(Node.class)));
        
        path = ResourceUtil.normalize(ResourceUtil.getParent(rootPath)
                + "/" + vanity[1] + ".print.html");

        request = new FakeSlingHttpServletRequest(path);
        res = resResolver.resolve(request, path);
        assertNotNull(res);
        assertEquals(rootPath, res.getPath());
        assertEquals(rootNode.getPrimaryNodeType().getName(),
                res.getResourceType());

        assertEquals(".print.html",
                res.getResourceMetadata().getResolutionPathInfo());

        assertNotNull(res.adaptTo(Node.class));
        assertTrue(rootNode.isSame(res.adaptTo(Node.class)));

        path = ResourceUtil.normalize(ResourceUtil.getParent(rootPath) + "/"
                + vanity[1] + ".print.html/suffix.pdf");

        request = new FakeSlingHttpServletRequest(path);
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
    
    @Test public void testResolveVanityPathWithUpdate() throws Exception {
        try {

            String path = ResourceUtil.normalize(ResourceUtil.getParent(rootPath)
                    + "/" + vanity[2] + ".print.html");

            HttpServletRequest request = new FakeSlingHttpServletRequest(path);
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
                    + vanity[2] + ".print.html/suffix.pdf");

            request = new FakeSlingHttpServletRequest(path);
            res = resResolver.resolve(request, path);
            assertNotNull(res);
            assertEquals(rootPath, res.getPath());
            assertEquals(rootNode.getPrimaryNodeType().getName(),
                    res.getResourceType());

            assertEquals(".print.html/suffix.pdf",
                    res.getResourceMetadata().getResolutionPathInfo());

            assertNotNull(res.adaptTo(Node.class));
            assertTrue(rootNode.isSame(res.adaptTo(Node.class)));
            
            //update vanityPath
            String [] vanityPathUpdated = new String[] {"testVanity","testV", "testVanityUpdated"};
            rootNode.setProperty("sling:vanityPath", vanityPathUpdated);
            saveMappings(session);
            
            path = ResourceUtil.normalize(ResourceUtil.getParent(rootPath)
                    + "/" + vanityPathUpdated[2] + ".print.html");

            request = new FakeSlingHttpServletRequest(path);
            res = resResolver.resolve(request, path);
            assertNotNull(res);
            assertEquals(rootPath, res.getPath());
            assertEquals(rootNode.getPrimaryNodeType().getName(),
                    res.getResourceType());

            assertEquals(".print.html",
                    res.getResourceMetadata().getResolutionPathInfo());

            assertNotNull(res.adaptTo(Node.class));
            assertTrue(rootNode.isSame(res.adaptTo(Node.class)));

            path = ResourceUtil.normalize(ResourceUtil.getParent(rootPath) + "/"
                    + vanityPathUpdated[2] + ".print.html/suffix.pdf");

            request = new FakeSlingHttpServletRequest(path);
            res = resResolver.resolve(request, path);
            assertNotNull(res);
            assertEquals(rootPath, res.getPath());
            assertEquals(rootNode.getPrimaryNodeType().getName(),
                    res.getResourceType());

            assertEquals(".print.html/suffix.pdf",
                    res.getResourceMetadata().getResolutionPathInfo());

            assertNotNull(res.adaptTo(Node.class));
            assertTrue(rootNode.isSame(res.adaptTo(Node.class)));
            
            
            path = ResourceUtil.normalize(ResourceUtil.getParent(rootPath)
                    + "/" + vanity[2] + ".print.html");

            request = new FakeSlingHttpServletRequest(path);
            res = resResolver.resolve(request, path);
            assertNotNull(res);
            assertTrue(res instanceof NonExistingResource);
            assertEquals("/"+vanity[2]+".print.html", res.getPath());
            
        } finally {
            //restore vanityPath
            vanity = new String[] {"testVanity","testV", "testVanityToUpdate"};
            rootNode.setProperty("sling:vanityPath", vanity);
            saveMappings(session);
        }
    }
    
    @Test public void testResolveRemovedVanityPath() throws Exception {          
        String path = ResourceUtil.normalize(ResourceUtil.getParent(rootPath)
                + "/" + vanity[0] + ".print.html");

        HttpServletRequest request = new FakeSlingHttpServletRequest(path);
        Resource res = resResolver.resolve(request, path);
        assertNotNull(res);
        assertEquals(rootPath, res.getPath());
        assertEquals(rootNode.getPrimaryNodeType().getName(),
                res.getResourceType());

        assertEquals(".print.html",
                res.getResourceMetadata().getResolutionPathInfo());

        assertNotNull(res.adaptTo(Node.class));
        assertTrue(rootNode.isSame(res.adaptTo(Node.class)));

        //remove vanityPath property
        rootNode.getProperty("sling:vanityPath").remove();
        saveMappings(session);

        res = resResolver.resolve(request, path);
        assertNotNull(res);
        assertTrue(res instanceof NonExistingResource);
        assertEquals("/"+vanity[0]+".print.html", res.getPath());

        //restore vanityPath
        rootNode.setProperty("sling:vanityPath", vanity);
        saveMappings(session);

        //create new child with vanity path
        Node childNode = maybeCreateNode(rootNode, "rootChild", "nt:unstructured");
        childNode.setProperty("sling:vanityPath", "childVanity");
        childNode.addMixin("sling:VanityPath");
        saveMappings(session);

        path = ResourceUtil.normalize(ResourceUtil.getParent(rootPath)
                + "/childVanity.print.html");

        request = new FakeSlingHttpServletRequest(path);
        res = resResolver.resolve(request, path);
        assertNotNull(res);
        assertEquals(childNode.getPath(), res.getPath());
        assertEquals(childNode.getPrimaryNodeType().getName(),
                res.getResourceType());

        assertEquals(".print.html",
                res.getResourceMetadata().getResolutionPathInfo());

        assertNotNull(res.adaptTo(Node.class));
        assertTrue(childNode.isSame(res.adaptTo(Node.class)));

        //remove node with vanity path
        childNode.remove();
        saveMappings(session);

        res = resResolver.resolve(request, path);
        assertNotNull(res);
        assertTrue(res instanceof NonExistingResource);
        assertEquals("/childVanity.print.html", res.getPath());
    }
    
    @Ignore //see SLING-3558
    @Test public void testResolveRemovedMixinVanityPath() throws Exception {   
        Node childNode = null;
        
        try  {
            //create new child with vanity path without mixin
            childNode = maybeCreateNode(rootNode, "rootChild", "nt:unstructured");
            childNode.setProperty("sling:vanityPath", "childVanity");
            saveMappings(session);
            
            String path = ResourceUtil.normalize(ResourceUtil.getParent(rootPath)
                    + "/childVanity.print.html");
            HttpServletRequest request = new FakeSlingHttpServletRequest(path);
            Resource res = resResolver.resolve(request, path);
            assertNotNull(res);
            assertTrue(res instanceof NonExistingResource);
            assertEquals("/childVanity.print.html", res.getPath());
            
            //add mixin
            childNode.addMixin("sling:VanityPath");
            saveMappings(session);
            
            path = ResourceUtil.normalize(ResourceUtil.getParent(rootPath)
                    + "/childVanity.print.html");

            request = new FakeSlingHttpServletRequest(path);
            res = resResolver.resolve(request, path);
            assertNotNull(res);
            assertEquals(childNode.getPath(), res.getPath());
            assertEquals(childNode.getPrimaryNodeType().getName(),
                    res.getResourceType());

            assertEquals(".print.html",
                    res.getResourceMetadata().getResolutionPathInfo());

            assertNotNull(res.adaptTo(Node.class));
            assertTrue(childNode.isSame(res.adaptTo(Node.class)));
            
            //remove mixin  
            childNode.removeMixin("sling:VanityPath");
            saveMappings(session);

            path = ResourceUtil.normalize(ResourceUtil.getParent(rootPath)
                    + "/childVanity.print.html");
            request = new FakeSlingHttpServletRequest(path);
            res = resResolver.resolve(request, path);
            assertNotNull(res);
            assertTrue(res instanceof NonExistingResource);
            assertEquals("/childVanity.print.html", res.getPath());
        } finally {
            if (childNode != null){
                childNode.remove();
                saveMappings(session);
            }
        }
    }

    
    @Test public void testGetDoesNotGoUp() throws Exception {

        final String path = rootPath + "/nothing";

        {
            final Resource res = resResolver.resolve(
                new FakeSlingHttpServletRequest(path, "POST"), path);
            assertNotNull(res);
            assertEquals("POST request resolution does not go up the path",
                Resource.RESOURCE_TYPE_NON_EXISTING, res.getResourceType());
        }

        {
            final Resource res = resResolver.resolve(
                new FakeSlingHttpServletRequest(path, "GET"), path);
            assertNotNull(res);
            assertEquals("GET request resolution does not go up the path",
                Resource.RESOURCE_TYPE_NON_EXISTING, res.getResourceType());
        }
    }

    @Test public void testGetRemovesExtensionInResolution() throws Exception {
        final String path = rootPath + ".whatever";
        final Resource res = resResolver.resolve(
            new FakeSlingHttpServletRequest(path, "GET"), path);
        assertNotNull(res);
        assertEquals(rootPath, res.getPath());
        assertEquals(rootNode.getPrimaryNodeType().getName(),
            res.getResourceType());
    }

    @Test public void testStarResourcePlain() throws Exception {
        final String path = rootPath + "/" + System.currentTimeMillis() + "/*";
        testStarResourceHelper(path, "GET");
        testStarResourceHelper(path, "POST");
        testStarResourceHelper(path, "PUT");
        testStarResourceHelper(path, "DELETE");
    }

    @Test public void testStarResourceExtension() throws Exception {
        final String path = rootPath + "/" + System.currentTimeMillis()
            + "/*.html";
        testStarResourceHelper(path, "GET");
        testStarResourceHelper(path, "POST");
        testStarResourceHelper(path, "PUT");
        testStarResourceHelper(path, "DELETE");
    }

    @Test public void testStarResourceSelectorExtension() throws Exception {
        final String path = rootPath + "/" + System.currentTimeMillis()
            + "/*.print.a4.html";
        testStarResourceHelper(path, "GET");
        testStarResourceHelper(path, "POST");
        testStarResourceHelper(path, "PUT");
        testStarResourceHelper(path, "DELETE");
    }

    @Test public void testSlingFolder() throws Exception {

        // create a folder
        String folderPath = "folder";
        Node folder = rootNode.addNode(folderPath, "sling:Folder");
        rootNode.getSession().save();

        try {
            // test default child node type
            Node child = folder.addNode("child0");
            folder.getSession().save();
            assertEquals("sling:Folder", child.getPrimaryNodeType().getName());

            // test explicit sling:Folder child
            child = folder.addNode("child1", "sling:Folder");
            folder.getSession().save();
            assertEquals("sling:Folder", child.getPrimaryNodeType().getName());

            // test explicit nt:folder child
            child = folder.addNode("child2", "nt:folder");
            folder.getSession().save();
            assertEquals("nt:folder", child.getPrimaryNodeType().getName());

            // test any child node -- use nt:unstructured here
            child = folder.addNode("child3", "nt:unstructured");
            folder.getSession().save();
            assertEquals("nt:unstructured", child.getPrimaryNodeType().getName());
        } finally {
            folder.remove();
            session.save();
        }
    }

    @Test public void testMap() throws Exception {
        String path = rootNode.getPath();
        String mapped = resResolver.map(path);
        assertEquals(path, mapped);

        Node child = rootNode.addNode("child");
        session.save();

        try {
            // absolute path, expect rootPath segment to be
            // cut off the mapped path because we map the rootPath
            // onto root
            path = "/child";
            mapped = resResolver.map(child.getPath());
            assertEquals(path, mapped);
        } finally {
            child.remove();
            session.save();
        }
    }

    @Test public void testMapURLEscaping() throws Exception {

        final String mapHostInternal = "internal.host.com";
        final String mapRootInternal = "/content/internal";

        Node internalRedirect = mapRoot.getNode("map/http").addNode(
            mapHostInternal + ".80", "sling:Mapping");
        internalRedirect.setProperty(
            PROP_REDIRECT_INTERNAL, mapRootInternal);

        try {
            saveMappings(session);
            
            final String path = "/sample with spaces";
            final String escapedPath = "/sample%20with%20spaces";

            // ---------------------------------------------------------------------
            // internal redirect

            // a) test map(String)
            // => return full URL, escaped
            String mapped = resResolver.map(mapRootInternal + path);
            assertEquals("http://" + mapHostInternal + escapedPath, mapped);

            // b) test map(HttpServletRequest, String) with "localhost"
            // => return full URL, escaped
            mapped = resResolver.map(new FakeSlingHttpServletRequest(rootPath),
                mapRootInternal + path);
            assertEquals("http://" + mapHostInternal + escapedPath, mapped);

            // c) test map(HttpServletRequest, String) with "internal.host.com"
            // => only return path, escaped, because request host/port matches (cut
            // off host part)
            mapped = resResolver.map(new FakeSlingHttpServletRequest(null,
                mapHostInternal, -1, rootPath), mapRootInternal + path);
            assertEquals(escapedPath, mapped);

            // ---------------------------------------------------------------------
            // no mapping config
            // => return only escaped path

            final String unmappedRoot = "/unmappedRoot";

            // a) test map(String)
            mapped = resResolver.map(unmappedRoot + path);
            assertEquals(unmappedRoot + escapedPath, mapped);

            // b) test map(HttpServletRequest, String)
            mapped = resResolver.map(new FakeSlingHttpServletRequest(rootPath),
                unmappedRoot + path);
            assertEquals(unmappedRoot + escapedPath, mapped);
        } finally {
            internalRedirect.remove();
            session.save();
        }

    }

    @Test public void testMapNamespaceMangling() throws Exception {

        final String mapHost = "virtual.host.com";
        final String mapRootPath = "/content/virtual";
        final String contextPath = "/context";

        Node virtualhost80 = mapRoot.getNode("map/http").addNode(
            mapHost + ".80", "sling:Mapping");
        virtualhost80.setProperty(PROP_REDIRECT_INTERNAL,
            mapRootPath);

        try {
            saveMappings(session);
            
            // ---------------------------------------------------------------------
            // tests expecting paths without context

            final HttpServletRequest virtualRequest = new FakeSlingHttpServletRequest(
                null, mapHost, -1, rootPath);

            // simple mapping - cut off prefix and add host
            final String pathv0 = "/sample";
            final String mappedv0 = resResolver.map(virtualRequest, mapRootPath
                + pathv0);
            assertEquals("Expect unmangled path", pathv0, mappedv0);

            // expected name mangling without host prefix
            final String pathv1 = "/sample/jcr:content";
            final String mangledv1 = "/sample/_jcr_content";
            final String mappedv1 = resResolver.map(virtualRequest, mapRootPath
                + pathv1);
            assertEquals("Expect mangled path", mangledv1, mappedv1);

            // ---------------------------------------------------------------------
            // tests expecting paths with context "/context"

            ((FakeSlingHttpServletRequest) virtualRequest).setContextPath(contextPath);

            // simple mapping - cut off prefix and add host
            final String pathvc0 = "/sample";
            final String mappedvc0 = resResolver.map(virtualRequest, mapRootPath
                + pathvc0);
            assertEquals("Expect unmangled path", contextPath + pathv0, mappedvc0);

            // expected name mangling without host prefix
            final String pathvc1 = "/sample/jcr:content";
            final String mangledvc1 = "/sample/_jcr_content";
            final String mappedvc1 = resResolver.map(virtualRequest, mapRootPath
                + pathvc1);
            assertEquals("Expect mangled path", contextPath + mangledvc1, mappedvc1);

            // ---------------------------------------------------------------------
            // tests expecting absolute URLs without context

            final HttpServletRequest foreignRequest = new FakeSlingHttpServletRequest(
                null, "foreign.host.com", -1, rootPath);

            final String pathf0 = "/sample";
            final String mappedf0 = resResolver.map(foreignRequest, mapRootPath
                + pathf0);
            assertEquals("Expect unmangled absolute URI", "http://" + mapHost
                + pathf0, mappedf0);

            final String pathf1 = "/sample/jcr:content";
            final String mangledf1 = "/sample/_jcr_content";
            final String mappedf1 = resResolver.map(foreignRequest, mapRootPath
                + pathf1);
            assertEquals("Expect mangled absolute URI", "http://" + mapHost
                + mangledf1, mappedf1);

            // ---------------------------------------------------------------------
            // tests expecting absolute URLs with context "/context"

            ((FakeSlingHttpServletRequest) foreignRequest).setContextPath(contextPath);

            final String pathfc0 = "/sample";
            final String mappedfc0 = resResolver.map(foreignRequest, mapRootPath
                + pathfc0);
            assertEquals("Expect unmangled absolute URI", "http://" + mapHost
                + contextPath + pathfc0, mappedfc0);

            final String pathfc1 = "/sample/jcr:content";
            final String mangledfc1 = "/sample/_jcr_content";
            final String mappedfc1 = resResolver.map(foreignRequest, mapRootPath
                + pathfc1);
            assertEquals("Expect mangled absolute URI", "http://" + mapHost
                + contextPath + mangledfc1, mappedfc1);
        } finally {
            virtualhost80.remove();
            session.save();
        }
    }

    @Test public void testMapContext() throws Exception {
        String path = rootNode.getPath();
        String mapped = resResolver.map(path);
        assertEquals(path, mapped);

        Node child = rootNode.addNode("child");
        session.save();

        try {
            // absolute path, expect rootPath segment to be
            // cut off the mapped path because we map the rootPath
            // onto root
            path = "/child";
            mapped = resResolver.map(child.getPath());
            assertEquals(path, mapped);
        } finally {
            child.remove();
            session.save();
        }
    }

    @Test public void testMapExtension() throws Exception {
        String path = rootNode.getPath();
        String mapped = resResolver.map(path);
        assertEquals(path, mapped);

        Node child = rootNode.addNode("child");
        session.save();

        try {
            // absolute path, expect rootPath segment to be
            // cut off the mapped path because we map the rootPath
            // onto root
            final String selExt = ".html";
            path = "/child" + selExt;
            mapped = resResolver.map(child.getPath() + selExt);
            assertEquals(path, mapped);
        } finally {
            child.remove();
            session.save();
        }
    }

    @Test public void testMapSelectorsExtension() throws Exception {
        String path = rootNode.getPath();
        String mapped = resResolver.map(path);
        assertEquals(path, mapped);

        Node child = rootNode.addNode("child");
        session.save();

        try {
            // absolute path, expect rootPath segment to be
            // cut off the mapped path because we map the rootPath
            // onto root
            final String selExt = ".sel1.sel2.html";
            path = "/child" + selExt;
            mapped = resResolver.map(child.getPath() + selExt);
            assertEquals(path, mapped);
        } finally {
            child.remove();
            session.save();
        }
    }

    @Test public void testMapExtensionSuffix() throws Exception {
        String path = rootNode.getPath();
        String mapped = resResolver.map(path);
        assertEquals(path, mapped);

        Node child = rootNode.addNode("child");
        session.save();

        try {
            // absolute path, expect rootPath segment to be
            // cut off the mapped path because we map the rootPath
            // onto root
            final String selExt = ".html/some/suffx.pdf";
            path = "/child" + selExt;
            mapped = resResolver.map(child.getPath() + selExt);
            assertEquals(path, mapped);
        } finally {
            child.remove();
            session.save();
        }
    }

    @Test public void testMapFragment() throws Exception {
        String path = rootNode.getPath();
        String mapped = resResolver.map(path);
        assertEquals(path, mapped);

        Node child = rootNode.addNode("child");
        session.save();

        try {
            // absolute path, expect rootPath segment to be
            // cut off the mapped path because we map the rootPath
            // onto root
            final String selExt = "#sec:1";
            path = "/child" + selExt;
            mapped = resResolver.map(child.getPath() + selExt);
            assertEquals(path, mapped);
        } finally {
            child.remove();
            session.save();
        }
    }

    @Test public void testMapQuery() throws Exception {
        String path = rootNode.getPath();
        String mapped = resResolver.map(path);
        assertEquals(path, mapped);

        Node child = rootNode.addNode("child");
        session.save();

        try {
            // absolute path, expect rootPath segment to be
            // cut off the mapped path because we map the rootPath
            // onto root
            final String selExt = "?a:b=2";
            path = "/child" + selExt;
            mapped = resResolver.map(child.getPath() + selExt);
            assertEquals(path, mapped);
        } finally {
            child.remove();
            session.save();
        }
    }

    @Test public void testMapFragmentQuery() throws Exception {
        String path = rootNode.getPath();
        String mapped = resResolver.map(path);
        assertEquals(path, mapped);

        Node child = rootNode.addNode("child");
        session.save();

        try {
            // absolute path, expect rootPath segment to be
            // cut off the mapped path because we map the rootPath
            // onto root
            final String selExt = "#sec:1?a:b=1";
            path = "/child" + selExt;
            mapped = resResolver.map(child.getPath() + selExt);
            assertEquals(path, mapped);
        } finally {
            child.remove();
            session.save();
        }
    }

    @Test public void testMapEmptyPath() throws Exception {
        String mapped = resResolver.map("");
        assertEquals("/", mapped);
    }

    @Test public void testMapExtensionFragmentQuery() throws Exception {
        String path = rootNode.getPath();
        String mapped = resResolver.map(path);
        assertEquals(path, mapped);

        Node child = rootNode.addNode("child");
        session.save();

        try {
            // absolute path, expect rootPath segment to be
            // cut off the mapped path because we map the rootPath
            // onto root
            final String selExt = ".html#sec:1?a:b=1";
            path = "/child" + selExt;
            mapped = resResolver.map(child.getPath() + selExt);
            assertEquals(path, mapped);
        } finally {
            child.remove();
            session.save();
        }
    }

    @Test public void testMapResourceAlias() throws Exception {
        // define an alias for the rootPath
        String alias = "testAlias";
        rootNode.setProperty("sling:alias", alias);
        session.save();

        try {
            String path = ResourceUtil.normalize(ResourceUtil.getParent(rootPath)
                    + "/" + alias);
            String mapped = resResolver.map(rootNode.getPath());
            assertEquals(path, mapped);
            Node child = rootNode.addNode("child");
            session.save();

            path = ResourceUtil.normalize(ResourceUtil.getParent(rootPath)
                    + "/" + alias+"/child");
            mapped = resResolver.map(child.getPath());
            assertEquals(path, mapped);
        } finally {
            rootNode.getProperty("sling:alias").remove();
            if ( rootNode.hasNode("child") ) {
                rootNode.getNode("child").remove();
            }
            session.save();
        }
     }

    @Test public void testMapResourceAliasJcrContent() throws Exception {
        // define an alias for the rootPath in the jcr:content child node
        String alias = "testAlias";
        Node content = rootNode.addNode("jcr:content", "nt:unstructured");
        content.setProperty("sling:alias", alias);
        session.save();

        try {
            String path = ResourceUtil.normalize(ResourceUtil.getParent(rootPath)
                    + "/" + alias);
            String mapped = resResolver.map(rootNode.getPath());
            assertEquals(path, mapped);

            path = ResourceUtil.normalize(ResourceUtil.getParent(rootPath)
                    + "/" + alias+"/_jcr_content");
            mapped = resResolver.map(content.getPath());
            assertEquals(path, mapped);

            Node child = content.addNode("child");
            session.save();

            path = ResourceUtil.normalize(ResourceUtil.getParent(rootPath)
                    + "/" + alias+"/_jcr_content/child");
            mapped = resResolver.map(child.getPath());
            assertEquals(path, mapped);
        } finally {
            content.remove();
            session.save();
        }
    }

    @Test public void test_resolve() throws Exception {

        Node child = rootNode.addNode("child");
        session.save();

        try {
            // expect kind due to alias and no parent due to mapping
            // the rootPath onto root
            String path = "/child";
            String mapped = resResolver.map(child.getPath());
            assertEquals(path, mapped);

            Resource res = resResolver.resolve(null, path);
            assertNotNull(res);
            assertEquals(rootNode.getPath() + "/child",
                res.getResourceMetadata().getResolutionPath());
            assertEquals("", res.getResourceMetadata().getResolutionPathInfo());

            Node resNode = res.adaptTo(Node.class);
            assertNotNull(resNode);

            assertEquals(child.getPath(), resNode.getPath());

            // second level alias
            Node grandchild = child.addNode("grandchild");
            session.save();

            // expect kind/enkel due to alias and no parent due to mapping
            // the rootPath onto root
            String pathEnkel = "/child/grandchild";
            String mappedEnkel = resResolver.map(grandchild.getPath());
            assertEquals(pathEnkel, mappedEnkel);

            Resource resEnkel = resResolver.resolve(null, pathEnkel);
            assertNotNull(resEnkel);
            assertEquals(rootNode.getPath() + "/child/grandchild",
                resEnkel.getResourceMetadata().getResolutionPath());
            assertEquals("", resEnkel.getResourceMetadata().getResolutionPathInfo());

            Node resNodeEnkel = resEnkel.adaptTo(Node.class);
            assertNotNull(resNodeEnkel);
            assertEquals(grandchild.getPath(), resNodeEnkel.getPath());
        } finally {
            child.remove();
            session.save();
        }
    }

    @Test public void test_resolve_extension() throws Exception {

        final String selExt = ".html";

        Node child = rootNode.addNode("child");
        session.save();

        try {
            // expect kind due to alias and no parent due to mapping
            // the rootPath onto root
            String path = "/child" + selExt;
            String mapped = resResolver.map(child.getPath() + selExt);
            assertEquals(path, mapped);

            Resource res = resResolver.resolve(null, path);
            assertNotNull(res);
            assertEquals(rootNode.getPath() + "/child",
                res.getResourceMetadata().getResolutionPath());
            assertEquals(selExt, res.getResourceMetadata().getResolutionPathInfo());

            Node resNode = res.adaptTo(Node.class);
            assertNotNull(resNode);
            assertEquals(child.getPath(), resNode.getPath());

            // second level alias
            Node grandchild = child.addNode("grandchild");
            session.save();

            // expect kind/enkel due to alias and no parent due to mapping
            // the rootPath onto root
            String pathEnkel = "/child/grandchild" + selExt;
            String mappedEnkel = resResolver.map(grandchild.getPath() + selExt);
            assertEquals(pathEnkel, mappedEnkel);

            Resource resEnkel = resResolver.resolve(null, pathEnkel);
            assertNotNull(resEnkel);
            assertEquals(rootNode.getPath() + "/child/grandchild",
                resEnkel.getResourceMetadata().getResolutionPath());
            assertEquals(selExt,
                resEnkel.getResourceMetadata().getResolutionPathInfo());

            Node resNodeEnkel = resEnkel.adaptTo(Node.class);
            assertNotNull(resNodeEnkel);
            assertEquals(grandchild.getPath(), resNodeEnkel.getPath());
        } finally {
            child.remove();
            session.save();
        }
    }

    @Test public void test_resolve_selectors_extension() throws Exception {

        final String selExt = ".sel1.sel2.html";

        Node child = rootNode.addNode("child");
        session.save();

        try {
            // expect kind due to alias and no parent due to mapping
            // the rootPath onto root
            String path = "/child" + selExt;
            String mapped = resResolver.map(child.getPath() + selExt);
            assertEquals(path, mapped);

            Resource res = resResolver.resolve(null, path);
            assertNotNull(res);
            assertEquals(rootNode.getPath() + "/child",
                res.getResourceMetadata().getResolutionPath());
            assertEquals(selExt, res.getResourceMetadata().getResolutionPathInfo());

            Node resNode = res.adaptTo(Node.class);
            assertNotNull(resNode);
            assertEquals(child.getPath(), resNode.getPath());

            // second level alias
            Node grandchild = child.addNode("grandchild");
            session.save();

            // expect kind/enkel due to alias and no parent due to mapping
            // the rootPath onto root
            String pathEnkel = "/child/grandchild" + selExt;
            String mappedEnkel = resResolver.map(grandchild.getPath() + selExt);
            assertEquals(pathEnkel, mappedEnkel);

            Resource resEnkel = resResolver.resolve(null, pathEnkel);
            assertNotNull(resEnkel);
            assertEquals(rootNode.getPath() + "/child/grandchild",
                resEnkel.getResourceMetadata().getResolutionPath());
            assertEquals(selExt,
                resEnkel.getResourceMetadata().getResolutionPathInfo());

            Node resNodeEnkel = resEnkel.adaptTo(Node.class);
            assertNotNull(resNodeEnkel);
            assertEquals(grandchild.getPath(), resNodeEnkel.getPath());
        } finally {
            child.remove();
            session.save();
        }
    }

    @Test public void test_resolve_extension_suffix() throws Exception {

        final String selExt = ".html/some/suffx.pdf";

        Node child = rootNode.addNode("child");
        session.save();

        try {
            // expect kind due to alias and no parent due to mapping
            // the rootPath onto root
            String path = "/child" + selExt;
            String mapped = resResolver.map(child.getPath() + selExt);
            assertEquals(path, mapped);

            Resource res = resResolver.resolve(null, path);
            Node resNode = res.adaptTo(Node.class);
            assertNotNull(resNode);

            assertEquals(child.getPath(), resNode.getPath());
        } finally {
            child.remove();
            session.save();
        }
    }

    /**
     * Test the order property of the vanity paths
     */
    @Test public void test_resolve_with_sling_vanity_path_order() throws Exception {
        final String vanityPath = "/ordering";

        // create two nodes - child2 with a higher order
        Node child1 = rootNode.addNode("child1");
        child1.addMixin("sling:VanityPath");
        child1.setProperty("sling:vanityPath", vanityPath);
        child1.setProperty("sling:vanityOrder", 100);
        Node child2 = rootNode.addNode("child2");
        child2.addMixin("sling:VanityPath");
        child2.setProperty("sling:vanityPath", vanityPath);
        child2.setProperty("sling:vanityOrder", 200);

        try {
            saveMappings(session);


            // we should get child2 now
            Resource rsrc = resResolver.resolve(vanityPath);
            assertNotNull(rsrc);
            assertFalse("Resource should exist", ResourceUtil.isNonExistingResource(rsrc));
            assertEquals("Path does not match", child2.getPath(), rsrc.getPath());

            // remove 2
            child2.remove();
            saveMappings(session);

            // we should get child 1 now
            rsrc = resResolver.resolve(vanityPath);
            assertNotNull(rsrc);
            assertFalse("Resource should exist", ResourceUtil.isNonExistingResource(rsrc));
            assertEquals("Path does not match", child1.getPath(), rsrc.getPath());

            // readding child2
            child2 = rootNode.addNode("child2");
            child2.addMixin("sling:VanityPath");
            child2.setProperty("sling:vanityPath", vanityPath);
            child2.setProperty("sling:vanityOrder", 200);
            saveMappings(session);

            // we should get child2 now
            rsrc = resResolver.resolve(vanityPath);
            assertNotNull(rsrc);
            assertFalse("Resource should exist", ResourceUtil.isNonExistingResource(rsrc));
            assertEquals("Path does not match", child2.getPath(), rsrc.getPath());

            // change order of child 1 to make it higher than child 2
            child1.setProperty("sling:vanityOrder", 300);
            saveMappings(session);

            // we should get child 1 now
            rsrc = resResolver.resolve(vanityPath);
            assertNotNull(rsrc);
            assertFalse("Resource should exist", ResourceUtil.isNonExistingResource(rsrc));
            assertEquals("Path does not match", child1.getPath(), rsrc.getPath());

            // change order of child 1 to make it lower than child 2
            child1.setProperty("sling:vanityOrder", 50);
            saveMappings(session);

            // we should get child 2 now
            rsrc = resResolver.resolve(vanityPath);
            assertNotNull(rsrc);
            assertFalse("Resource should exist", ResourceUtil.isNonExistingResource(rsrc));
            assertEquals("Path does not match", child2.getPath(), rsrc.getPath());
        } finally {
            child1.remove();
            if ( rootNode.hasNode("child2") ) {
                rootNode.getNode("child2").remove();
            }
            session.save();
        }
    }

    @Test public void test_resolve_with_sling_alias() throws Exception {

        Node child = rootNode.addNode("child");
        child.setProperty("sling:alias", "kind");
        saveMappings(session);

        try {
            // expect kind due to alias and no parent due to mapping
            // the rootPath onto root
            String path = "/kind";
            String mapped = resResolver.map(child.getPath());
            assertEquals(path, mapped);

            Resource res = resResolver.resolve(null, path);
            assertNotNull(res);
            assertEquals(rootNode.getPath() + "/kind",
                res.getResourceMetadata().getResolutionPath());
            assertEquals("", res.getResourceMetadata().getResolutionPathInfo());

            Node resNode = res.adaptTo(Node.class);
            assertNotNull(resNode);

            assertEquals(child.getPath(), resNode.getPath());

            // second level alias
            Node grandchild = child.addNode("grandchild");
            grandchild.setProperty("sling:alias", "enkel");
            saveMappings(session);

            // expect kind/enkel due to alias and no parent due to mapping
            // the rootPath onto root
            String pathEnkel = "/kind/enkel";
            String mappedEnkel = resResolver.map(grandchild.getPath());
            assertEquals(pathEnkel, mappedEnkel);

            Resource resEnkel = resResolver.resolve(null, pathEnkel);
            assertNotNull(resEnkel);
            assertEquals(rootNode.getPath() + "/kind/enkel",
                resEnkel.getResourceMetadata().getResolutionPath());
            assertEquals("", resEnkel.getResourceMetadata().getResolutionPathInfo());

            Node resNodeEnkel = resEnkel.adaptTo(Node.class);
            assertNotNull(resNodeEnkel);
            assertEquals(grandchild.getPath(), resNodeEnkel.getPath());
        } finally {
            child.remove();
            session.save();
        }
    }

    /*@Test public void test_resolve_with_sling_alias_limited_access() throws Exception {
        Principal testUserPrincipal = AccessControlUtil.getPrincipalManager(session).getPrincipal("testuser");

        Node child = rootNode.addNode("child");
        child.setProperty("sling:alias", "kind");
        AccessControlUtil.replaceAccessControlEntry(session, child.getPath(), testUserPrincipal, null, new String[] {"jcr:all"}, null, "last");
        session.save();
try {
        Session testUserSession = getRepository().login(new SimpleCredentials("testuser", "test".toCharArray()));
        final Map<String, Object> authInfo = new HashMap<String, Object>();
        authInfo.put(ResourceResolverFactory.USER, "admin");
        authInfo.put(ResourceResolverFactory.PASSWORD, "admin".toCharArray());
        authInfo.put("testAttributeString", "AStringValue");
        authInfo.put("testAttributeNumber", 999);
        ResourceResolver testUserResolver = resFac.getResourceResolver(authInfo);

        try {
            // expect child due to the aliased not not being visible and no parent
            // due to mapping the rootPath onto root
            String path = "/child";
            String mapped = testUserResolver.map(child.getPath());
            assertEquals(path, mapped);

            Resource res = testUserResolver.resolve(null, path);
            assertNotNull(res);
            assertTrue(res instanceof NonExistingResource);
            assertEquals("/child",
                res.getResourceMetadata().getResolutionPath());
            // TODO - is this correct?
            assertEquals(null, res.getResourceMetadata().getResolutionPathInfo());

            // second level alias
            Node grandchild = child.addNode("grandchild");
            grandchild.setProperty("sling:alias", "enkel");
            AccessControlUtil.replaceAccessControlEntry(session, grandchild.getPath(), testUserPrincipal, new String[] { "jcr:all" }, null, null, "first");
            session.save();

            // expect /child/enkel due to parent node not being
            // visible to the test user and no parent due to mapping
            // the rootPath onto root
            String pathEnkel = "/child/enkel";
            String mappedEnkel = testUserResolver.map(grandchild.getPath());
            assertEquals(pathEnkel, mappedEnkel);*/

            //TODO already commented
            /*
            Resource resEnkel = testUserResolver.resolve(null, pathEnkel);
            assertNotNull(resEnkel);
            assertEquals(rootNode.getPath() + "/kind/enkel",
                resEnkel.getResourceMetadata().getResolutionPath());
            assertEquals("", resEnkel.getResourceMetadata().getResolutionPathInfo());

            Node resNodeEnkel = resEnkel.adaptTo(Node.class);
            assertNotNull(resNodeEnkel);
            assertEquals(grandchild.getPath(), resNodeEnkel.getPath());
            */
       /*} finally {
            testUserSession.logout();
        }
        } finally {
            child.remove();
            session.save();
        }
    }*/

    @Test public void test_resolve_with_sling_alias_multi_value() throws Exception {

        Node child = rootNode.addNode("child");
        child.setProperty("sling:alias", new String[] {
            "kind", "enfant" });

        try {
            saveMappings(session);
            
            // expect kind due to alias and no parent due to mapping
            // the rootPath onto root
            String path = "/kind";
            String mapped = resResolver.map(child.getPath());
            assertEquals(path, mapped);

            Resource res = resResolver.resolve(null, path);
            assertNotNull(res);
            assertEquals(rootNode.getPath() + "/kind",
                res.getResourceMetadata().getResolutionPath());
            assertEquals("", res.getResourceMetadata().getResolutionPathInfo());

            Node resNode = res.adaptTo(Node.class);
            assertNotNull(resNode);

            assertEquals(child.getPath(), resNode.getPath());

            // expect enfant due to alias and no parent due to mapping
            // the rootPath onto root
            String pathEnfant = "/enfant";
            String mappedEnfant = resResolver.map(child.getPath());
            assertEquals(path, mappedEnfant); // map always selects first alias

            Resource resEnfant = resResolver.resolve(null, pathEnfant);
            assertNotNull(resEnfant);
            assertEquals(rootNode.getPath() + "/enfant",
                resEnfant.getResourceMetadata().getResolutionPath());
            assertEquals("", resEnfant.getResourceMetadata().getResolutionPathInfo());

            Node resNodeEnfant = resEnfant.adaptTo(Node.class);
            assertNotNull(resNodeEnfant);

            assertEquals(child.getPath(), resNodeEnfant.getPath());

            // second level alias
            Node grandchild = child.addNode("grandchild");
            grandchild.setProperty("sling:alias", "enkel");
            saveMappings(session);

            // expect kind/enkel due to alias and no parent due to mapping
            // the rootPath onto root
            String pathEnkel = "/kind/enkel";
            String mappedEnkel = resResolver.map(grandchild.getPath());
            assertEquals(pathEnkel, mappedEnkel);

            Resource resEnkel = resResolver.resolve(null, pathEnkel);
            assertNotNull(resEnkel);
            assertEquals(rootNode.getPath() + "/kind/enkel",
                resEnkel.getResourceMetadata().getResolutionPath());
            assertEquals("", resEnkel.getResourceMetadata().getResolutionPathInfo());

            Node resNodeEnkel = resEnkel.adaptTo(Node.class);
            assertNotNull(resNodeEnkel);
            assertEquals(grandchild.getPath(), resNodeEnkel.getPath());

            // expect kind/enkel due to alias and no parent due to mapping
            // the rootPath onto root
            String pathEnfantEnkel = "/enfant/enkel";
            String mappedEnfantEnkel = resResolver.map(grandchild.getPath());
            assertEquals(pathEnkel, mappedEnfantEnkel); // map always selects first alias

            Resource resEnfantEnkel = resResolver.resolve(null, pathEnfantEnkel);
            assertNotNull(resEnfantEnkel);
            assertEquals(rootNode.getPath() + "/enfant/enkel",
                resEnfantEnkel.getResourceMetadata().getResolutionPath());
            assertEquals("", resEnfantEnkel.getResourceMetadata().getResolutionPathInfo());

            Node resNodeEnfantEnkel = resEnfantEnkel.adaptTo(Node.class);
            assertNotNull(resNodeEnfantEnkel);
            assertEquals(grandchild.getPath(), resNodeEnfantEnkel.getPath());
        } finally {
            child.remove();
            session.save();
        }
    }

    @Test public void test_resolve_with_sling_alias_extension() throws Exception {

        final String selExt = ".html";

        Node child = rootNode.addNode("child");
        child.setProperty("sling:alias", "kind");

        try {
            saveMappings(session);
            
            // expect kind due to alias and no parent due to mapping
            // the rootPath onto root
            String path = "/kind" + selExt;
            String mapped = resResolver.map(child.getPath() + selExt);
            assertEquals(path, mapped);

            Resource res = resResolver.resolve(null, path);
            assertNotNull(res);
            assertEquals(rootNode.getPath() + "/kind",
                res.getResourceMetadata().getResolutionPath());
            assertEquals(selExt, res.getResourceMetadata().getResolutionPathInfo());

            Node resNode = res.adaptTo(Node.class);
            assertNotNull(resNode);
            assertEquals(child.getPath(), resNode.getPath());

            // second level alias
            Node grandchild = child.addNode("grandchild");
            grandchild.setProperty("sling:alias", "enkel");
            saveMappings(session);

            // expect kind/enkel due to alias and no parent due to mapping
            // the rootPath onto root
            String pathEnkel = "/kind/enkel" + selExt;
            String mappedEnkel = resResolver.map(grandchild.getPath() + selExt);
            assertEquals(pathEnkel, mappedEnkel);

            Resource resEnkel = resResolver.resolve(null, pathEnkel);
            assertNotNull(resEnkel);
            assertEquals(rootNode.getPath() + "/kind/enkel",
                resEnkel.getResourceMetadata().getResolutionPath());
            assertEquals(selExt,
                resEnkel.getResourceMetadata().getResolutionPathInfo());

            Node resNodeEnkel = resEnkel.adaptTo(Node.class);
            assertNotNull(resNodeEnkel);
            assertEquals(grandchild.getPath(), resNodeEnkel.getPath());
        } finally {
            child.remove();
            session.save();
        }
    }

    @Test public void test_resolve_with_sling_alias_selectors_extension()
            throws Exception {

        final String selExt = ".sel1.sel2.html";

        Node child = rootNode.addNode("child");
        child.setProperty("sling:alias", "kind");

        try {
            saveMappings(session);
            
            // expect kind due to alias and no parent due to mapping
            // the rootPath onto root
            String path = "/kind" + selExt;
            String mapped = resResolver.map(child.getPath() + selExt);
            assertEquals(path, mapped);

            Resource res = resResolver.resolve(null, path);
            assertNotNull(res);
            assertEquals(rootNode.getPath() + "/kind",
                res.getResourceMetadata().getResolutionPath());
            assertEquals(selExt, res.getResourceMetadata().getResolutionPathInfo());

            Node resNode = res.adaptTo(Node.class);
            assertNotNull(resNode);
            assertEquals(child.getPath(), resNode.getPath());

            // second level alias
            Node grandchild = child.addNode("grandchild");
            grandchild.setProperty("sling:alias", "enkel");
            saveMappings(session);

            // expect kind/enkel due to alias and no parent due to mapping
            // the rootPath onto root
            String pathEnkel = "/kind/enkel" + selExt;
            String mappedEnkel = resResolver.map(grandchild.getPath() + selExt);
            assertEquals(pathEnkel, mappedEnkel);

            Resource resEnkel = resResolver.resolve(null, pathEnkel);
            assertNotNull(resEnkel);
            assertEquals(rootNode.getPath() + "/kind/enkel",
                resEnkel.getResourceMetadata().getResolutionPath());
            assertEquals(selExt,
                resEnkel.getResourceMetadata().getResolutionPathInfo());

            Node resNodeEnkel = resEnkel.adaptTo(Node.class);
            assertNotNull(resNodeEnkel);
            assertEquals(grandchild.getPath(), resNodeEnkel.getPath());
        } finally {
            child.remove();
            session.save();
        }
    }

    @Test public void test_resolve_with_sling_alias_extension_suffix()
            throws Exception {

        final String selExt = ".html/some/suffx.pdf";

        Node child = rootNode.addNode("child");
        child.setProperty("sling:alias", "kind");

        try {
            saveMappings(session);
            
            // expect kind due to alias and no parent due to mapping
            // the rootPath onto root
            String path = "/kind" + selExt;
            String mapped = resResolver.map(child.getPath() + selExt);
            assertEquals(path, mapped);

            Resource res = resResolver.resolve(null, path);
            Node resNode = res.adaptTo(Node.class);
            assertNotNull(resNode);

            assertEquals(child.getPath(), resNode.getPath());
        } finally {
            child.remove();
            session.save();
        }
    }

    // ---------- internal

    private void testStarResourceHelper(final String path, final String method) {
        final Resource res = resResolver.resolve(
            new FakeSlingHttpServletRequest(path, method), path);
        assertNotNull(res);
        assertTrue(ResourceUtil.isStarResource(res));
        assertEquals("sling:syntheticStarResource", res.getResourceType());
    }
}
