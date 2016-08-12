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
package org.apache.sling.jcr.oak.server.it;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.query.Query;

import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.apache.sling.api.SlingConstants;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class OakServerIT extends OakServerTestSupport {

    private final Logger log = LoggerFactory.getLogger(OakServerIT.class);

    @Test
    public void testRepositoryPresent() {
        assertNotNull(repository);
    }

    @Test
    public void testLoginAdministrative() throws RepositoryException {
        final Session s = repository.loginAdministrative(null);
        assertNotNull(s);
        s.logout();
    }

    @Test
    public void testExplicitAdminLogin() throws RepositoryException {
        final Credentials creds = new SimpleCredentials("admin", "admin".toCharArray());
        repository.login(creds).logout();
    }

    @Test(expected=RepositoryException.class)
    public void testWrongLogin() throws RepositoryException {
        final Credentials creds = new SimpleCredentials("badName", "badPAssword".toCharArray());
        repository.login(creds);
    }

    @Test
    public void testAnonymousLoginA() throws RepositoryException {
        final Session s = repository.login();
        assertNotNull(s);
        s.logout();
    }

    @Test
    public void testAnonymousLoginB() throws RepositoryException {
        final Session s = repository.login(null, null);
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
        final Session s = repository.login();
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
        final Session s = repository.loginAdministrative(null);
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
        Session s = repository.loginAdministrative(null);
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

    @Test
    public void testSingleValueInputStream() throws RepositoryException {
        Session s = repository.loginAdministrative(null);
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
        final Session s = repository.loginAdministrative(null);
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
    public void testOsgiResourceEvents() throws RepositoryException {
        final ResourceEventListener listener = new ResourceEventListener();
        final ServiceRegistration reg = listener.register(bundleContext, SlingConstants.TOPIC_RESOURCE_ADDED);
        final Session s = repository.loginAdministrative(null);
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

            log.info("Added {} nodes, checking what ResourceEventListener got...", nPaths);
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

        log.info("Successfuly detected OSGi observation events for " + nPaths + " paths");
    }

    @Test
    public void testNodetypeObservation() throws Exception {
        Session s = repository.loginAdministrative(null);
        final InputStream inputStream = getClass().getResourceAsStream("/i18n.cnd");
        final Reader cnd = new InputStreamReader(inputStream);
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
        s = repository.loginAdministrative(null);
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

    @Test
    public void doCheckRepositoryDescriptors() {
        final String propName = "jcr.repository.name";
        final String name = repository.getDescriptor(propName);
        final String expected = "Oak";
        if(!name.contains(expected)) {
            fail("Expected repository descriptor " + propName + " to contain "
                + expected + ", failed (descriptor=" + name + ")");
        }

        log.info("Running on Oak version {}", repository.getDescriptor("jcr.repository.version"));
    }

}
