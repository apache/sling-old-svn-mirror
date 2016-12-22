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

import static org.apache.sling.testing.paxexam.SlingOptions.jackrabbitSling;
import static org.apache.sling.testing.paxexam.SlingOptions.scr;
import static org.apache.sling.testing.paxexam.SlingOptions.slingJcr;
import static org.apache.sling.testing.paxexam.SlingOptions.tikaSling;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.cm.ConfigurationAdminOptions.factoryConfiguration;
import static org.ops4j.pax.exam.cm.ConfigurationAdminOptions.newConfiguration;

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

import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.testing.paxexam.SlingOptions;
import org.apache.sling.testing.paxexam.TestSupport;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.util.PathUtils;
import org.osgi.framework.BundleContext;

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
        return assertCreateRetrieveNode(nodeType, null);
    }

    protected String assertCreateRetrieveNode(String nodeType, String relParentPath) throws RepositoryException {
        Session session = repository.loginAdministrative(null);
        try {
            final Node root = session.getRootNode();
            final String name = uniqueName("assertCreateRetrieveNode");
            final String propName = "PN_" + name;
            final String propValue = "PV_" + name;
            final Node parent = relParentPath == null ? root : JcrUtils.getOrAddNode(root, relParentPath);
            final Node child = nodeType == null ? parent.addNode(name) : parent.addNode(name, nodeType);
            child.setProperty(propName, propValue);
            child.setProperty("foo", child.getPath());
            session.save();
            session.logout();
            session = repository.loginAdministrative(null);
            final String path = relParentPath == null ? "/" + name : "/" + relParentPath + "/" + name;
            final Node n = session.getNode(path);
            assertNotNull(n);
            assertEquals(propValue, n.getProperty(propName).getString());
            return n.getPath();
        } finally {
            session.logout();
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
        SlingOptions.versionResolver.setVersionFromProject("org.apache.sling", "org.apache.sling.jcr.base");
        SlingOptions.versionResolver.setVersionFromProject("org.apache.sling", "org.apache.sling.jcr.resource");
        SlingOptions.versionResolver.setVersionFromProject("org.apache.sling", "org.apache.sling.resourceresolver");
        SlingOptions.versionResolver.setVersionFromProject("org.apache.sling", "org.apache.sling.api");
        SlingOptions.versionResolver.setVersionFromProject("org.apache.jackrabbit", "oak-core");
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-commons", SlingOptions.versionResolver.getVersion("org.apache.jackrabbit", "oak-core"));
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-blob", SlingOptions.versionResolver.getVersion("org.apache.jackrabbit", "oak-core"));
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-jcr", SlingOptions.versionResolver.getVersion("org.apache.jackrabbit", "oak-core"));
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-segment", SlingOptions.versionResolver.getVersion("org.apache.jackrabbit", "oak-core"));
        SlingOptions.versionResolver.setVersionFromProject("org.apache.jackrabbit", "jackrabbit-api");
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "jackrabbit-data", SlingOptions.versionResolver.getVersion("org.apache.jackrabbit", "jackrabbit-api"));
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "jackrabbit-jcr-commons", SlingOptions.versionResolver.getVersion("org.apache.jackrabbit", "jackrabbit-api"));
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "jackrabbit-jcr-rmi", SlingOptions.versionResolver.getVersion("org.apache.jackrabbit", "jackrabbit-api"));
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "jackrabbit-spi", SlingOptions.versionResolver.getVersion("org.apache.jackrabbit", "jackrabbit-api"));
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "jackrabbit-spi-commons", SlingOptions.versionResolver.getVersion("org.apache.jackrabbit", "jackrabbit-api"));
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "jackrabbit-webdav", SlingOptions.versionResolver.getVersion("org.apache.jackrabbit", "jackrabbit-api"));
        SlingOptions.versionResolver.setVersion("org.apache.felix", "org.apache.felix.http.jetty", "3.1.6"); // SLING-6080 – Java 7
        SlingOptions.versionResolver.setVersion("org.apache.felix", "org.apache.felix.http.whiteboard", "2.3.2"); // SLING-6080 – Java 7
        final String repoinit = String.format("raw:file:%s/src/test/resources/repoinit.txt", PathUtils.getBaseDir());
        final String slingHome = String.format("%s/sling", workingDirectory());
        final String repositoryHome = String.format("%s/repository", slingHome);
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
            mavenBundle().groupId("org.apache.jackrabbit").artifactId("oak-segment").version(SlingOptions.versionResolver),
            // repoinit (temp)
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.jcr.repoinit").version("1.1.0"),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.repoinit.parser").version("1.1.0"),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.provisioning.model").version("1.7.0"),
            newConfiguration("org.apache.jackrabbit.oak.plugins.segment.SegmentNodeStoreService")
                .put("repository.home", repositoryHome)
                .put("name", "Default NodeStore")
                .asOption(),
            newConfiguration("org.apache.sling.resourceresolver.impl.observation.OsgiObservationBridge")
                .put("enabled", true)
                .asOption(),
            newConfiguration("org.apache.sling.jcr.repoinit.impl.RepositoryInitializer")
                .put("references", new String[]{repoinit})
                .asOption(),
            getWhitelistRegexpOption(),
            // To generate the list of whitelisted bundles after a failed test-run:
            // grep -R 'NOT white' target/failsafe-reports/ | awk -F': Bundle ' '{print substr($2, 1, index($2, " is NOT "))}' | sort -u
            factoryConfiguration("org.apache.sling.jcr.base.internal.LoginAdminWhitelist.fragment")
                .put("whitelist.bundles", new String[]{
                    "org.apache.sling.jcr.oak.server",
                    "org.apache.sling.jcr.contentloader",
                    "org.apache.sling.jcr.resource",
                    "org.apache.sling.resourceresolver"
                })
                .asOption()
        );
    }

    protected Option getWhitelistRegexpOption() {
        return newConfiguration("org.apache.sling.jcr.base.internal.LoginAdminWhitelist")
            .put("whitelist.bundles.regexp", "PAXEXAM-PROBE-.*")
            .asOption();
    }
}
