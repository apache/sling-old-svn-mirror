/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.jcr.repository.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.vmOptions;

import java.io.Reader;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashSet;
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
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.jcr.api.SlingRepository;
import org.junit.After;
import org.junit.Test;
import org.ops4j.pax.exam.Option;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Base class for SlingRepository tests, contains tests
 *  that apply to all implementations.
 *  PaxExamParameterized could also be used in theory to
 *  have single class that tests all implementations, but
 *  in a quick test that didn't work well with variable
 *  @Config annotations.
 */
public abstract class CommonTests {

    private final Logger log = LoggerFactory.getLogger(getClass());

    protected SlingRepository repository;

    @Inject
    protected BundleContext bundleContext;
    
    /** Check some repository descriptors to make sure we're
     *  testing the expected implementation. */
    // TODO: see SLING-4841 why the template method pattern was removed
    // protected abstract void doCheckRepositoryDescriptors();

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
            s = repository.loginAdministrative(null);
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
    
    public Collection<Option> commonOptions() {
        final String tikaVersion = System.getProperty("tika.version", "NO_TIKA_VERSION??");
        final String localRepo = System.getProperty("maven.repo.local", "");
        final String paxVmOptions = System.getProperty("pax.vm.options", "");
        final boolean webconsole = "true".equals(System.getProperty("webconsole.active", "false"));
        
        final List<Option> opt = new LinkedList<Option>();
        if(localRepo.length() > 0 ) {
            opt.add(systemProperty("org.ops4j.pax.url.mvn.localRepository").value(localRepo));
        }
        if(paxVmOptions.length() > 0) {
            opt.add(vmOptions(paxVmOptions));
        }
        
        // Optionally add webconsole
        if(webconsole) {
            opt.add(mavenBundle("org.apache.felix", "org.apache.felix.webconsole", "4.2.6"));
            opt.add(mavenBundle("org.apache.felix", "org.apache.felix.webconsole.plugins.ds", "1.0.0"));
            opt.add(mavenBundle("org.apache.felix", "org.apache.felix.webconsole.plugins.packageadmin", "1.0.0"));
            opt.add(mavenBundle("org.apache.felix", "org.apache.felix.webconsole.plugins.event", "1.1.0"));
            opt.add(mavenBundle("org.apache.sling", "org.apache.sling.jcr.webconsole", "1.0.2"));
            opt.add(mavenBundle("org.apache.geronimo.bundles", "json", "20090211_1"));
        }
        
        final String SLF4J_VERSION = "1.7.5";
        opt.add(mavenBundle("org.slf4j", "slf4j-api", SLF4J_VERSION));
        opt.add(mavenBundle("org.slf4j", "jcl-over-slf4j", SLF4J_VERSION));
        opt.add(mavenBundle("org.slf4j", "log4j-over-slf4j", SLF4J_VERSION));
        
        opt.add(mavenBundle("org.apache.sling", "org.apache.sling.fragment.xml", "1.0.2"));
        opt.add(mavenBundle("org.apache.sling", "org.apache.sling.fragment.transaction", "1.0.0"));
        opt.add(mavenBundle("org.apache.sling", "org.apache.sling.fragment.activation", "1.0.2"));
        opt.add(mavenBundle("org.apache.sling", "org.apache.sling.fragment.ws", "1.0.2"));

        opt.add(mavenBundle("org.apache.sling", "org.apache.sling.commons.log", "4.0.0"));
        opt.add(mavenBundle("org.apache.sling", "org.apache.sling.commons.logservice", "1.0.2"));

        opt.add(mavenBundle("commons-io", "commons-io", "2.4"));
        opt.add(mavenBundle("commons-fileupload", "commons-fileupload", "1.3.1"));
        opt.add(mavenBundle("commons-collections", "commons-collections", "3.2.1"));
        opt.add(mavenBundle("commons-codec", "commons-codec", "1.9"));
        opt.add(mavenBundle("commons-lang", "commons-lang", "2.6"));
        opt.add(mavenBundle("commons-pool", "commons-pool", "1.6"));

        opt.add(mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.concurrent", "1.3.4_1"));

        opt.add(mavenBundle("com.google.guava", "guava", "15.0"));

        opt.add(mavenBundle("org.apache.geronimo.bundles", "commons-httpclient", "3.1_1"));
        opt.add(mavenBundle("org.apache.tika", "tika-core", tikaVersion));
        opt.add(mavenBundle("org.apache.tika", "tika-bundle", tikaVersion));

        opt.add(mavenBundle("org.apache.felix", "org.apache.felix.http.jetty", "2.2.2"));
        opt.add(mavenBundle("org.apache.felix", "org.apache.felix.eventadmin", "1.3.2"));
        opt.add(mavenBundle("org.apache.felix", "org.apache.felix.scr", "1.8.2"));
        opt.add(mavenBundle("org.apache.felix", "org.apache.felix.configadmin", "1.8.0"));
        opt.add(mavenBundle("org.apache.felix", "org.apache.felix.inventory", "1.0.4"));

        opt.add(mavenBundle("org.apache.sling", "org.apache.sling.commons.osgi", "2.2.2"));
        opt.add(mavenBundle("org.apache.sling", "org.apache.sling.commons.json", "2.0.10"));
        opt.add(mavenBundle("org.apache.sling", "org.apache.sling.commons.mime", "2.1.8"));
        opt.add(mavenBundle("org.apache.sling", "org.apache.sling.commons.classloader", "1.3.2"));
        opt.add(mavenBundle("org.apache.sling", "org.apache.sling.commons.scheduler", "2.4.6"));
        opt.add(mavenBundle("org.apache.sling", "org.apache.sling.commons.threads", "3.2.0"));

        opt.add(mavenBundle("org.apache.sling", "org.apache.sling.launchpad.api", "1.1.0"));
        opt.add(mavenBundle("org.apache.sling", "org.apache.sling.auth.core", "1.3.6"));
        opt.add(mavenBundle("org.apache.sling", "org.apache.sling.discovery.api", "1.0.0"));
        opt.add(mavenBundle("org.apache.sling", "org.apache.sling.discovery.standalone", "1.0.0"));

        opt.add(mavenBundle("org.apache.sling", "org.apache.sling.api", "2.9.0"));
        opt.add(mavenBundle("org.apache.sling", "org.apache.sling.settings", "1.3.6"));
        opt.add(mavenBundle("org.apache.sling", "org.apache.sling.resourceresolver", "1.2.4"));
        opt.add(mavenBundle("org.apache.sling", "org.apache.sling.adapter", "2.1.2"));
        opt.add(mavenBundle("org.apache.sling", "org.apache.sling.jcr.resource", "2.5.0"));
        opt.add(mavenBundle("org.apache.sling", "org.apache.sling.jcr.classloader", "3.2.2"));
        opt.add(mavenBundle("org.apache.sling", "org.apache.sling.jcr.contentloader", "2.1.10"));
        opt.add(mavenBundle("org.apache.sling", "org.apache.sling.engine", "2.4.0"));
        opt.add(mavenBundle("org.apache.sling", "org.apache.sling.event", "3.6.0"));
        opt.add(mavenBundle("org.apache.sling", "org.apache.sling.serviceusermapper", "1.2.0"));
        
        opt.add(mavenBundle("org.apache.sling", "org.apache.sling.testing.tools", "1.0.8"));
        opt.add(mavenBundle("org.apache.httpcomponents", "httpcore-osgi", "4.4.1"));
        opt.add(mavenBundle("org.apache.httpcomponents", "httpclient-osgi", "4.4.1"));

        opt.add(mavenBundle("org.apache.sling", "org.apache.sling.jcr.jcr-wrapper", "2.0.0"));
        opt.add(mavenBundle("org.apache.sling", "org.apache.sling.jcr.api", "2.3.1-SNAPSHOT"));
        opt.add(mavenBundle("org.apache.sling", "org.apache.sling.jcr.base", "2.3.3-SNAPSHOT"));
        
        opt.add(junitBundles());
        return opt;
    }


    private <ItemType extends Item> ItemType deleteAfterTests(ItemType it) throws RepositoryException {
        toDelete.add(it.getPath());
        return it;
    }

    /** Verify that admin can create and retrieve a node of the specified type.
     * @return the path of the test node that was created.
     */
    private String assertCreateRetrieveNode(String nodeType) throws RepositoryException {
        Session s = repository.loginAdministrative(null);
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
            s = repository.loginAdministrative(null);
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

        final Session s = repository.loginAdministrative(null);
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

    // TODO: see SLING-4841 why the template method pattern was removed
    //@Test
    //public final void checkRepositoryDescriptors() {
    //    doCheckRepositoryDescriptors();
    //}

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

    public void setup() throws Exception {
        final ServiceTracker st = new ServiceTracker(bundleContext, SlingRepository.class.getName(), null);
        st.open(true);
        try {
            this.repository = (SlingRepository) st.waitForService(10000);
        } catch (InterruptedException e) {
        }
        
        // Make sure the JcrResourceProvider is initialized, as it
        // setups conversion of JCR to OSGi events, and some tests use this
        // @Injecting the ResourceResolverFactory fails, haven't found why.
        final int timeout = 10;
        final long timeoutAt = System.currentTimeMillis() + (timeout * 1000L);
        ServiceReference ref = null;
        while(System.currentTimeMillis() < timeoutAt) {
            ref = bundleContext.getServiceReference(ResourceResolverFactory.class.getName());
            if(ref != null) {
                break;
            }
        }
        
        assertNotNull("Expecting ResourceResolverFactory within " + timeout + " seconds", ref);
        ResourceResolver rr = null;
        try {
            final ResourceResolverFactory f = (ResourceResolverFactory)bundleContext.getService(ref);
            rr = f.getAdministrativeResourceResolver(null);
            rr.getResource("/");
        } finally {
            if(rr != null) {
                rr.close();
            }
            bundleContext.ungetService(ref);
        }
    }
}
