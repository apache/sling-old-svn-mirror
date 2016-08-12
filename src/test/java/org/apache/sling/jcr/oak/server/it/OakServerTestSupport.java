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

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;

import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.testing.paxexam.SlingOptions;
import org.apache.sling.testing.paxexam.TestSupport;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.osgi.framework.BundleContext;

import static org.apache.sling.testing.paxexam.SlingOptions.jackrabbitSling;
import static org.apache.sling.testing.paxexam.SlingOptions.scr;
import static org.apache.sling.testing.paxexam.SlingOptions.slingJcr;
import static org.apache.sling.testing.paxexam.SlingOptions.tikaSling;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.cm.ConfigurationAdminOptions.newConfiguration;

public abstract class OakServerTestSupport extends TestSupport {

    @Inject
    protected SlingRepository slingRepository;

    @Inject
    protected BundleContext bundleContext;

    @Inject
    protected SlingRepository repository;

    @Inject
    protected ResourceResolverFactory resourceResolverFactory;


    protected final List<String> toDelete = new LinkedList<String>();

    private final AtomicInteger uniqueNameCounter = new AtomicInteger();

    protected static final Integer TEST_SCALE = Integer.getInteger("test.scale", 1);

    protected class JcrEventsCounter implements EventListener {
        private final Session s;
        private int jcrEventsCounter;

        public JcrEventsCounter() throws RepositoryException {
            s = repository.loginAdministrative(null);
            final ObservationManager om = s.getWorkspace().getObservationManager();
            final int eventTypes = 255; // not sure if that's a recommended value, but common
            final boolean deep = true;
            final String[] uuid = null;
            final String[] nodeTypeNames = new String[]{"mix:language", "sling:Message"};
            final boolean noLocal = true;
            final String root = "/";
            om.addEventListener(this, eventTypes, root, deep, uuid, nodeTypeNames, noLocal);
        }

        void close() {
            s.logout();
        }

        @Override
        public void onEvent(EventIterator it) {
            while (it.hasNext()) {
                it.nextEvent();
                jcrEventsCounter++;
            }
        }

        int get() {
            return jcrEventsCounter;
        }
    }

    protected <ItemType extends Item> ItemType deleteAfterTests(ItemType it) throws RepositoryException {
        toDelete.add(it.getPath());
        return it;
    }

    /**
     * Verify that admin can create and retrieve a node of the specified type.
     *
     * @return the path of the test node that was created.
     */
    protected String assertCreateRetrieveNode(String nodeType) throws RepositoryException {
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

    @Configuration
    public Option[] configuration() {
        return new Option[]{
            baseConfiguration(),
            launchpad(),
            // Sling JCR Oak Server
            testBundle("bundle.filename"),
            // testing
            junitBundles()
        };
    }

    protected Option launchpad() {
        final String slingHome = String.format("%s/sling", workingDirectory());
        final String repositoryHome = String.format("%s/repository", slingHome);
        final String localIndexDir = String.format("%s/index", repositoryHome);
        return composite(
            scr(),
            slingJcr(),
            jackrabbitSling(),
            tikaSling(),
            mavenBundle().groupId("org.apache.jackrabbit").artifactId("oak-core").version(SlingOptions.versionResolver),
            mavenBundle().groupId("org.apache.jackrabbit").artifactId("oak-commons").version(SlingOptions.versionResolver),
            mavenBundle().groupId("org.apache.jackrabbit").artifactId("oak-blob").version(SlingOptions.versionResolver),
            mavenBundle().groupId("org.apache.jackrabbit").artifactId("oak-jcr").version(SlingOptions.versionResolver),
            mavenBundle().groupId("com.google.guava").artifactId("guava").version(SlingOptions.versionResolver),
            mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.jaas").version(SlingOptions.versionResolver),
            mavenBundle().groupId("org.apache.jackrabbit").artifactId("oak-lucene").version(SlingOptions.versionResolver), // TODO  make Oak Lucene optional
            mavenBundle().groupId("org.apache.jackrabbit").artifactId("oak-segment").version(SlingOptions.versionResolver),
            newConfiguration("org.apache.jackrabbit.oak.plugins.segment.SegmentNodeStoreService")
                .put("repository.home", repositoryHome)
                .put("name", "Default NodeStore")
                .asOption(),
            newConfiguration("org.apache.jackrabbit.oak.plugins.index.lucene.LuceneIndexProviderService")
                .put("localIndexDir", localIndexDir)
                .asOption(),
            newConfiguration("org.apache.sling.resourceresolver.impl.observation.OsgiObservationBridge")
                .put("enabled", true)
                .asOption()
        );
    }

}
