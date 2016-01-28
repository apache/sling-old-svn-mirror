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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChange.ChangeType;
import org.apache.sling.api.resource.util.PathSet;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.spi.resource.provider.ObservationReporter;
import org.apache.sling.spi.resource.provider.ObserverConfiguration;
import org.junit.Test;

/**
 * Base for the two different listener tests.
 */
public abstract class AbstractListenerTest {

    private String createdPath = "/test" + System.currentTimeMillis() + "-create";

    private String pathToDelete = "/test" + System.currentTimeMillis() + "-delete";

    private String pathToModify = "/test" + System.currentTimeMillis() + "-modify";

    private final List<ResourceChange> events = synchronizedList(new ArrayList<ResourceChange>());

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
                assertEquals(Collections.singleton("foo"), event.getAddedPropertyNames());
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

    private static void createNode(final Session session, final String path) throws RepositoryException {
        session.getRootNode().addNode(path.substring(1), "nt:unstructured");
        session.save();
    }

    private void generateEvents() throws Exception {
        @SuppressWarnings("deprecation")
        final Session session = getRepository().loginAdministrative(null);

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

    public abstract SlingRepository getRepository();

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
            };
            return Collections.singletonList(config);
        }
    }

}
