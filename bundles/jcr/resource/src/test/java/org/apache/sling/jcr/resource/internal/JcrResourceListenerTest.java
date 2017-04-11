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
package org.apache.sling.jcr.resource.internal;

import static java.util.Collections.synchronizedList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChange.ChangeType;
import org.apache.sling.api.resource.path.PathSet;
import org.apache.sling.commons.testing.jcr.RepositoryUtil;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.spi.resource.provider.ObservationReporter;
import org.apache.sling.spi.resource.provider.ObserverConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test of JcrResourceListener.
 */
public class JcrResourceListenerTest {

    private JcrListenerBaseConfig config;

    private JcrResourceListener listener;

    private Session adminSession;

    private String createdPath = "/test" + System.currentTimeMillis() + "-create";

    private String pathToDelete = "/test" + System.currentTimeMillis() + "-delete";

    private String pathToModify = "/test" + System.currentTimeMillis() + "-modify";

    private final List<ResourceChange> events = synchronizedList(new ArrayList<ResourceChange>());

    @SuppressWarnings("deprecation")
    @Before
    public void setUp() throws Exception {
        RepositoryUtil.startRepository();
        this.adminSession = RepositoryUtil.getRepository().loginAdministrative(null);
        RepositoryUtil.registerSlingNodeTypes(adminSession);
        final SlingRepository repo = RepositoryUtil.getRepository();
        this.config = new JcrListenerBaseConfig(getObservationReporter(),
                new SlingRepository() {

                    @Override
                    public Session login(Credentials credentials, String workspaceName)
                            throws LoginException, NoSuchWorkspaceException, RepositoryException {
                        return repo.login(credentials, workspaceName);
                    }

                    @Override
                    public Session login(String workspaceName) throws LoginException, NoSuchWorkspaceException, RepositoryException {
                        return repo.login(workspaceName);
                    }

                    @Override
                    public Session login(Credentials credentials) throws LoginException, RepositoryException {
                        return repo.login(credentials);
                    }

                    @Override
                    public Session login() throws LoginException, RepositoryException {
                        return repo.login();
                    }

                    @Override
                    public boolean isStandardDescriptor(String key) {
                        return repo.isStandardDescriptor(key);
                    }

                    @Override
                    public boolean isSingleValueDescriptor(String key) {
                        return repo.isSingleValueDescriptor(key);
                    }

                    @Override
                    public Value[] getDescriptorValues(String key) {
                        return repo.getDescriptorValues(key);
                    }

                    @Override
                    public Value getDescriptorValue(String key) {
                        return repo.getDescriptorValue(key);
                    }

                    @Override
                    public String[] getDescriptorKeys() {
                        return repo.getDescriptorKeys();
                    }

                    @Override
                    public String getDescriptor(String key) {
                        return repo.getDescriptor(key);
                    }

                    @Override
                    public Session loginService(String subServiceName, String workspace) throws LoginException, RepositoryException {
                        return repo.loginAdministrative(workspace);
                    }

                    @Override
                    public Session loginAdministrative(String workspace) throws LoginException, RepositoryException {
                        return repo.loginAdministrative(workspace);
                    }

                    @Override
                    public String getDefaultWorkspace() {
                        // TODO Auto-generated method stub
                        return repo.getDefaultWorkspace();
                    }
                });
        this.listener = new JcrResourceListener(this.config,
                getObservationReporter().getObserverConfigurations().get(0));
    }

    @After
    public void tearDown() throws Exception {
        if ( adminSession != null ) {
            adminSession.logout();
            adminSession = null;
        }
        RepositoryUtil.stopRepository();
        if ( listener != null ) {
            listener.close();
            listener = null;
        }
        if ( config != null ) {
            config.close();
            config = null;
        }
    }

    @Test public void testSimpleOperations() throws Exception {
        generateEvents();

        assertEquals("Received: " + events, 5, events.size());
        final Set<String> addPaths = new HashSet<String>();
        final Set<String> modifyPaths = new HashSet<String>();
        final Set<String> removePaths = new HashSet<String>();

        for (final ResourceChange event : events) {
            if (event.getType() == ChangeType.ADDED) {
                addPaths.add(event.getPath());
            } else if (event.getType() == ChangeType.CHANGED) {
                modifyPaths.add(event.getPath());
            } else if (event.getType() == ChangeType.REMOVED) {
                removePaths.add(event.getPath());
            } else {
                fail("Unexpected event: " + event);
            }
            assertNotNull(event.getUserId());
        }
        assertEquals(3, addPaths.size());
        assertTrue("Added set should contain " + createdPath, addPaths.contains(createdPath));
        assertTrue("Added set should contain " + pathToDelete, addPaths.contains(pathToDelete));
        assertTrue("Added set should contain " + pathToModify, addPaths.contains(pathToModify));

        assertEquals(1, modifyPaths.size());
        assertTrue("Modified set should contain " + pathToModify, modifyPaths.contains(pathToModify));

        assertEquals(1, removePaths.size());
        assertTrue("Removed set should contain " + pathToDelete, removePaths.contains(pathToDelete));
    }

    @Test
    public void testMultiplePaths() throws Exception {
        ObserverConfiguration observerConfig = new ObserverConfiguration() {

            @Override
            public boolean includeExternal() {
                return true;
            }

            @Override
            public PathSet getPaths() {
                return PathSet.fromStrings("/libs", "/apps");
            }

            @Override
            public PathSet getExcludedPaths() {
                return PathSet.fromPaths();
            }

            @Override
            public Set<ChangeType> getChangeTypes() {
                return EnumSet.allOf(ChangeType.class);
            }

            @Override
            public boolean matches(String path) {
                return this.getPaths().matches(path) != null;
            }

            @Override
            public Set<String> getPropertyNamesHint() {
                return null;
            }
        };
        this.config.unregister(this.listener);
        this.listener = null;
        final Session session = this.adminSession;
        if ( !session.nodeExists("/libs") ) {
            createNode(session, "/libs");
        }
        if ( !session.nodeExists("/apps") ) {
            createNode(session, "/apps");
        }
        session.getNode("/libs").addNode("foo" + System.currentTimeMillis());
        session.getNode("/apps").addNode("foo" + System.currentTimeMillis());

        session.save();

        Thread.sleep(200);

        this.events.clear();

        try ( final JcrResourceListener l = new JcrResourceListener(this.config, observerConfig)) {
            final String rootName = "test_" + System.currentTimeMillis();
            for ( final String path : new String[] {"/libs", "/", "/apps", "/content"}) {
                final Node parent;
                if ( !session.nodeExists(path) ) {
                    parent = createNode(session, path);
                } else {
                    parent = session.getNode(path);
                }
                final Node node = parent.addNode(rootName, "nt:unstructured");
                session.save();

                node.setProperty("foo", "bar");
                session.save();

                node.remove();
                session.save();
            }
            assertEquals("Received: " + events, 6, events.size());
            final Set<String> addPaths = new HashSet<String>();
            final Set<String> modifyPaths = new HashSet<String>();
            final Set<String> removePaths = new HashSet<String>();

            for (final ResourceChange event : events) {
                if (event.getType() == ChangeType.ADDED) {
                    addPaths.add(event.getPath());
                } else if (event.getType() == ChangeType.CHANGED) {
                    modifyPaths.add(event.getPath());
                } else if (event.getType() == ChangeType.REMOVED) {
                    removePaths.add(event.getPath());
                } else {
                    fail("Unexpected event: " + event);
                }
                assertNotNull(event.getUserId());
            }
            assertEquals("Received: " + addPaths, 2, addPaths.size());
            assertTrue("Added set should contain /libs/" + rootName, addPaths.contains("/libs/" + rootName));
            assertTrue("Added set should contain /apps/" + rootName, addPaths.contains("/apps/" + rootName));

            assertEquals("Received: " + modifyPaths, 2, modifyPaths.size());
            assertTrue("Modified set should contain /libs/" + rootName, modifyPaths.contains("/libs/" + rootName));
            assertTrue("Modified set should contain /apps/" + rootName, modifyPaths.contains("/apps/" + rootName));

            assertEquals("Received: " + removePaths, 2, removePaths.size());
            assertTrue("Removed set should contain /libs/" + rootName, removePaths.contains("/libs/" + rootName));
            assertTrue("Removed set should contain /apps/" + rootName, removePaths.contains("/apps/" + rootName));
        }
    }

    private static Node createNode(final Session session, final String path) throws RepositoryException {
        final Node n = session.getRootNode().addNode(path.substring(1), "nt:unstructured");
        session.save();
        return n;
    }

    private void generateEvents() throws Exception {
        @SuppressWarnings("deprecation")
        final Session session = RepositoryUtil.getRepository().loginAdministrative(null);

        try {
            // create the nodes
            createNode(session, createdPath);
            createNode(session, pathToModify);
            createNode(session, pathToDelete);

            Thread.sleep(1000);

            // modify
            final Node modified = session.getNode(pathToModify);
            modified.setProperty("foo", "bar");

            session.save();

            // delete
            final Node deleted = session.getNode(pathToDelete);
            deleted.remove();
            session.save();

            Thread.sleep(3500);

        } finally {
            session.logout();
        }
    }

    protected ObservationReporter getObservationReporter() {
        return new SimpleObservationReporter();
    }

    private class SimpleObservationReporter implements ObservationReporter {

        @Override
        public void reportChanges(Iterable<ResourceChange> changes, boolean distribute) {
            for (ResourceChange c : changes) {
                events.add(c);
            }
        }

        @Override
        public List<ObserverConfiguration> getObserverConfigurations() {
            ObserverConfiguration config = new ObserverConfiguration() {

                @Override
                public boolean includeExternal() {
                    return true;
                }

                @Override
                public PathSet getPaths() {
                    return PathSet.fromStrings("/");
                }

                @Override
                public PathSet getExcludedPaths() {
                    return PathSet.fromPaths();
                }

                @Override
                public Set<ChangeType> getChangeTypes() {
                    return EnumSet.allOf(ChangeType.class);
                }

                @Override
                public boolean matches(String path) {
                    return true;
                }

                @Override
                public Set<String> getPropertyNamesHint() {
                    return new HashSet<String>();
                }
            };
            return Collections.singletonList(config);
        }

        @Override
        public void reportChanges(ObserverConfiguration config, Iterable<ResourceChange> changes, boolean distribute) {
            this.reportChanges(changes, distribute);
        }
    }

}
