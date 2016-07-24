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
package org.apache.sling.karaf.tests.bootstrap;

import java.io.Reader;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.jcr.Credentials;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;
import javax.jcr.query.Query;

import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.karaf.testing.KarafTestSupport;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.ops4j.pax.exam.util.Filter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public abstract class AbstractSlingLaunchpadOakTestSupport extends KarafTestSupport {

    @Inject
    @Filter(timeout = 300000)
    public SlingRepository slingRepository;

    @Inject
    @Filter(timeout = 300000)
    public ResourceResolverFactory resourceResolverFactory;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public AbstractSlingLaunchpadOakTestSupport() {
    }

    @Test
    public void testSlingRepository() throws Exception {
        assertNotNull(slingRepository);
    }

    @Test
    public void testVarSlingExists() throws Exception {
        final Session session = slingRepository.loginAdministrative(null);
        session.getRootNode().getNode("var/sling");
        session.logout();
    }

    // see org.apache.sling.jcr.repository.it.CommonTests

    private final List<String> toDelete = new LinkedList<String>();
    private final AtomicInteger uniqueNameCounter = new AtomicInteger();
    protected static final Integer TEST_SCALE = Integer.getInteger("test.scale", 1);

    public static final String I18N_MESSAGE_CND =
        "<sling = 'http://sling.apache.org/jcr/sling/1.0'>\n"
            + "[mix:language]\n"
            + "mixin\n"
            + "- jcr:language (string)\n"
            + "\n"
            + "[sling:Message]\n"
            + "mixin\n"
            + "- sling:key (string)\n"
            + "- sling:message (undefined)\n"
            + "\n"
            + "[sling:MessageEntry] > nt:hierarchyNode, sling:Message\n"
        ;

    protected class JcrEventsCounter implements EventListener {
        private final Session s;
        private int jcrEventsCounter;

        public JcrEventsCounter() throws RepositoryException {
            s = slingRepository.loginAdministrative(null);
            final ObservationManager om = s.getWorkspace().getObservationManager();
            final int eventTypes = 255; // not sure if that's a recommended value, but common
            final boolean deep = true;
            final String [] uuid = null;
            final String [] nodeTypeNames = new String [] { "mix:language", "sling:Message" };
            final boolean noLocal = true;
            final String root = "/";
            om.addEventListener(this, eventTypes, root, deep, uuid, nodeTypeNames, noLocal);
        }

        void close() {
            s.logout();
        }

        @Override
        public void onEvent(EventIterator it) {
            while(it.hasNext()) {
                it.nextEvent();
                jcrEventsCounter++;
            }
        }

        int get() {
            return jcrEventsCounter;
        }
    }

    /** Simple Retry loop for tests */
    public abstract class Retry {

        public Retry(int timeoutMsec) {
            final long timeout = System.currentTimeMillis() + timeoutMsec;
            Throwable lastT = null;
            while(System.currentTimeMillis() < timeout) {
                try {
                    lastT = null;
                    exec();
                    break;
                } catch(Throwable t) {
                    lastT = t;
                }
            }

            if(lastT != null) {
                fail("Failed after " + timeoutMsec + " msec: " + lastT);
            }
        }

        protected abstract void exec() throws Exception;
    }

    private <ItemType extends Item> ItemType deleteAfterTests(ItemType it) throws RepositoryException {
        toDelete.add(it.getPath());
        return it;
    }

    /** Verify that admin can create and retrieve a node of the specified type.
     * @return the path of the test node that was created.
     */
    private String assertCreateRetrieveNode(String nodeType) throws RepositoryException {
        Session s = slingRepository.loginAdministrative(null);
        try {
            final Node root = s.getRootNode();
            final String name = uniqueName("assertCreateRetrieveNode");
            final String propName = "PN_" + name;
            final String propValue = "PV_" + name;
            final Node child = nodeType == null ? root.addNode(name) : root.addNode(name, nodeType);
            child.setProperty(propName, propValue);
            child.setProperty("foo", child.getPath());
            s.save();
            s.logout();
            s = slingRepository.loginAdministrative(null);
            final Node n = s.getNode("/" + name);
            assertNotNull(n);
            assertEquals(propValue, n.getProperty(propName).getString());
            return n.getPath();
        } finally {
            s.logout();
        }
    }

    protected String uniqueName(String hint) {
        return hint + "_" + uniqueNameCounter.incrementAndGet() + "_" + System.currentTimeMillis();
    }

    @After
    public void deleteTestItems() throws RepositoryException {
        if(toDelete.isEmpty()) {
            return;

        }

        final Session s = slingRepository.loginAdministrative(null);
        try {
            for(String path : toDelete) {
                if(s.itemExists(path)) {
                    s.getItem(path).remove();
                }
            }
            s.save();
            toDelete.clear();
        } finally {
            s.logout();
        }
    }

    @Test
    public void testRepositoryPresent() {
        assertNotNull(slingRepository);
    }

    @Test
    public void testLoginAdministrative() throws RepositoryException {
        final Session s = slingRepository.loginAdministrative(null);
        assertNotNull(s);
        s.logout();
    }

    @Test
    public void testExplicitAdminLogin() throws RepositoryException {
        final Credentials creds = new SimpleCredentials("admin", "admin".toCharArray());
        slingRepository.login(creds).logout();
    }

    @Test(expected=RepositoryException.class)
    public void testWrongLogin() throws RepositoryException {
        final Credentials creds = new SimpleCredentials("badName", "badPAssword".toCharArray());
        slingRepository.login(creds);
    }

    @Test
    public void testAnonymousLoginA() throws RepositoryException {
        final Session s = slingRepository.login();
        assertNotNull(s);
        s.logout();
    }

    @Test
    public void testAnonymousLoginB() throws RepositoryException {
        final Session s = slingRepository.login(null, null);
        assertNotNull(s);
        s.logout();
    }

    @Test
    public void testCreateRetrieveNode() throws RepositoryException {
        assertCreateRetrieveNode(null);
    }

    @Test
    public void testCreateRetrieveSlingFolder() throws RepositoryException {
        assertCreateRetrieveNode("sling:Folder");
    }

    @Test
    public void testAnonymousHasReadAccess() throws RepositoryException {
        final String path = assertCreateRetrieveNode(null);
        final Session s = slingRepository.login();
        try {
            assertTrue("Expecting anonymous to see " + path, s.itemExists(path));
            final Node n = s.getNode(path);
            assertEquals("Expecting anonymous to see the foo property", path, n.getProperty("foo").getString());
        } finally {
            s.logout();
        }
    }

    @Test
    public void testSqlQuery() throws RepositoryException {
        final Session s = slingRepository.loginAdministrative(null);
        final String id = "ID_" + System.currentTimeMillis();
        final String propName = "PROP_" + id;
        final String value = "VALUE_" + id;
        try {
            final int N_NODES = 100;
            for(int i=0 ; i < N_NODES; i++) {
                final Node root = s.getRootNode();
                root.addNode(id + i).setProperty(propName, value);
            }
            s.save();

            final String stmt = "SELECT * FROM nt:base WHERE " + propName + " IS NOT NULL";

            @SuppressWarnings("deprecation")
            final Query q = s.getWorkspace().getQueryManager().createQuery(stmt, Query.SQL);

            final NodeIterator it = q.execute().getNodes();
            int count = 0;
            while(it.hasNext()) {
                it.next();
                count++;
            }
            assertEquals("Expected " + N_NODES + " result for query " + stmt, N_NODES, count);
        } finally {
            s.logout();
        }
    }

    @Test
    public void testXpathQueryWithMixin() throws RepositoryException {
        Session s = slingRepository.loginAdministrative(null);
        try {
            final String path = "XPATH_QUERY_" + System.currentTimeMillis();
            final String absPath = "/" + path;
            final Node n = deleteAfterTests(s.getRootNode().addNode(path));
            n.addMixin("mix:title");
            s.save();

            final String statement = "/jcr:root//element(*, mix:title)";
            @SuppressWarnings("deprecation")
            final Query q = s.getWorkspace().getQueryManager().createQuery(statement, Query.XPATH);
            final NodeIterator it = q.execute().getNodes();
            assertTrue("Expecting a non-empty result", it.hasNext());
            boolean found = false;
            while(it.hasNext()) {
                if(it.nextNode().getPath().equals(absPath)) {
                    found = true;
                    break;
                }
            }
            assertTrue("Expecting test node " + absPath + " to be found", found);
        } finally {
            s.logout();
        }
    }

    // TODO: see SLING-4841 why the template method pattern was removed
    //@Test
    //public final void checkRepositoryDescriptors() {
    //    doCheckRepositoryDescriptors();
    //}

    @Test
    public void testSingleValueInputStream() throws RepositoryException {
        Session s = slingRepository.loginAdministrative(null);
        try {
            final String path = getClass().getSimpleName() + System.currentTimeMillis();
            final Node child = deleteAfterTests(s.getRootNode().addNode(path));
            final Property p = child.setProperty("foo", "bar");
            s.save();
            assertNotNull(p.getBinary().getStream());
        } finally {
            s.logout();
        }

    }

    @Test
    public void testMultiValueInputStream() throws RepositoryException {
        final Session s = slingRepository.loginAdministrative(null);
        try {
            final String path = getClass().getSimpleName() + System.currentTimeMillis();
            final Node child = deleteAfterTests(s.getRootNode().addNode(path));
            final Property p = child.setProperty("foo", new String[] { "bar", "wii " });
            s.save();
            try {
                p.getBinary().getStream();
                fail("Expecting getStream() to fail on a multi-value Property");
            } catch(RepositoryException asExpected) {
            }
        } finally {
            s.logout();
        }
    }

    @Test
    @Ignore // TODO failure: OSGi add resource events are missing for 2500/2500 paths after 5000 msec: 2500 paths missing
    public void testOsgiResourceEvents() throws RepositoryException {
        final ResourceEventListener listener = new ResourceEventListener();
        final ServiceRegistration reg = listener.register(bundleContext, SlingConstants.TOPIC_RESOURCE_ADDED);
        final Session s = slingRepository.loginAdministrative(null);
        final int nPaths = 2500 * TEST_SCALE;
        final int timeoutMsec = 2 * nPaths;
        final String prefix = uniqueName("testOsgiResourceEvents");

        // Create N nodes with a unique name under /
        // and verify that ResourceEventListener gets an event
        // for each of them
        try {
            for(int i=0; i  < nPaths; i++) {
                s.getRootNode().addNode(prefix + i);
            }
            s.save();

            logger.info("Added {} nodes, checking what ResourceEventListener got...", nPaths);
            final long timeout = System.currentTimeMillis() + timeoutMsec;
            final Set<String> missing = new HashSet<String>();
            while(System.currentTimeMillis() < timeout) {
                missing.clear();
                final Set<String> paths = listener.getPaths();
                for(int i=0; i  < nPaths; i++) {
                    final String path = "/" + prefix + i;
                    if(!paths.contains(path)) {
                        missing.add(path);
                    }
                }

                if(missing.isEmpty()) {
                    break;
                }
            }

            if(!missing.isEmpty()) {
                final String missingStr = missing.size() > 10 ? missing.size() + " paths missing" : missing.toString();
                fail("OSGi add resource events are missing for "
                    + missing.size() + "/" + nPaths + " paths after "
                    + timeoutMsec + " msec: " + missingStr);
            }
        } finally {
            reg.unregister();
            s.logout();
        }

        logger.info("Successfuly detected OSGi observation events for " + nPaths + " paths");
    }

    @Test
    public void testNodetypeObservation() throws Exception {
        Session s = slingRepository.loginAdministrative(null);
        final Reader cnd = new StringReader(I18N_MESSAGE_CND);
        JcrEventsCounter counter = null;
        final String path = "/" + uniqueName("observation");

        // Add a sling:MessageEntry and verify that we get JCR events
        try {
            CndImporter.registerNodeTypes(cnd, s);
            counter = new JcrEventsCounter();

            final Node n = s.getRootNode().addNode(path.substring(1), "sling:MessageEntry");
            toDelete.add(n.getPath());
            n.setProperty("sling:key", "foo");
            n.setProperty("sling:message", "bar");
            s.save();

            final JcrEventsCounter c = counter;
            new Retry(5000) {
                @Override
                protected void exec() throws Exception {
                    assertTrue("Expecting JCR events after adding " + path, c.get() > 0);
                }
            };

        } finally {
            s.logout();
            cnd.close();
            if(counter != null) {
                counter.close();
            }
        }

        // In a separate session, modify node and verify that we get events
        counter = new JcrEventsCounter();
        s = slingRepository.loginAdministrative(null);
        try {

            final Node n = s.getNode(path);
            n.setProperty("sling:message", "CHANGED now");
            s.save();

            final JcrEventsCounter c = counter;
            new Retry(5000) {
                @Override
                protected void exec() throws Exception {
                    assertTrue("Expecting JCR events after modifying " + path, c.get() > 0);
                }
            };

        } finally {
            s.logout();
            cnd.close();
            counter.close();
        }

    }

    /** Keep track of OSGi events received on a given topic */
    public class ResourceEventListener implements EventHandler {

        private final Logger log = LoggerFactory.getLogger(getClass());
        private final Set<String> paths = new HashSet<String>();

        ServiceRegistration register(BundleContext ctx, String osgiEventTopic) {
            final Hashtable<String, Object> props = new Hashtable<String, Object>();
            props.put(EventConstants.EVENT_TOPIC, osgiEventTopic);
            return ctx.registerService(EventHandler.class.getName(), this, props);
        }

        @Override
        public void handleEvent(Event event) {
            final String path = (String) event.getProperty("path");
            if(path != null) {
                final int n = paths.size();
                synchronized (paths) {
                    if(n % 1000 == 0) {
                        log.info("Got events for {} paths so far, last path={}", n, path);
                    }
                    paths.add(path);
                }
            }
        }

        void clear() {
            synchronized (paths) {
                paths.clear();
            }
        }

        Set<String> getPaths() {
            synchronized (paths) {
                return Collections.unmodifiableSet(paths);
            }
        }
    }

}
