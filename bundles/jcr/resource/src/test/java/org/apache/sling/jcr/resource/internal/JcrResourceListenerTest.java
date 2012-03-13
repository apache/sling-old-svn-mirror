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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import junitx.util.PrivateAccessor;

import org.apache.sling.api.SlingConstants;
import org.apache.sling.commons.testing.jcr.EventHelper;
import org.apache.sling.commons.testing.jcr.RepositoryTestBase;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Test of JcrResourceListener.
 */
public class JcrResourceListenerTest extends RepositoryTestBase {
    private String createdPath;

    private String pathToDelete;

    private String pathToModify;

    public void testDefaultWorkspace() throws Exception {
        List<Event> events = generateEvents(null);

        assertTrue("Received: " + events, events.size() >= 3);
        Event event = events.get(0);
        assertEquals(SlingConstants.TOPIC_RESOURCE_ADDED, event.getTopic());
        assertEquals(createdPath, event.getProperty(SlingConstants.PROPERTY_PATH));
        assertNotNull(event.getProperty(SlingConstants.PROPERTY_USERID));

        event = events.get(1);
        assertEquals(SlingConstants.TOPIC_RESOURCE_CHANGED, event.getTopic());
        assertEquals(pathToModify, event.getProperty(SlingConstants.PROPERTY_PATH));
        assertNotNull(event.getProperty(SlingConstants.PROPERTY_USERID));

        event = events.get(2);
        assertEquals(SlingConstants.TOPIC_RESOURCE_REMOVED, event.getTopic());
        assertEquals(pathToDelete, event.getProperty(SlingConstants.PROPERTY_PATH));
        assertNotNull(event.getProperty(SlingConstants.PROPERTY_USERID));

    }

    public void testInWs2() throws Exception {
        List<Event> events = generateEvents("ws2");

        assertTrue("Received: " + events, events.size() >= 3);
        Event event = events.get(0);
        assertEquals(SlingConstants.TOPIC_RESOURCE_ADDED, event.getTopic());
        assertEquals("ws2:" + createdPath, event.getProperty(SlingConstants.PROPERTY_PATH));
        assertNotNull(event.getProperty(SlingConstants.PROPERTY_USERID));

        event = events.get(1);
        assertEquals(SlingConstants.TOPIC_RESOURCE_CHANGED, event.getTopic());
        assertEquals("ws2:" + pathToModify, event.getProperty(SlingConstants.PROPERTY_PATH));
        assertNotNull(event.getProperty(SlingConstants.PROPERTY_USERID));

        event = events.get(2);
        assertEquals(SlingConstants.TOPIC_RESOURCE_REMOVED, event.getTopic());
        assertEquals("ws2:" + pathToDelete, event.getProperty(SlingConstants.PROPERTY_PATH));
        assertNotNull(event.getProperty(SlingConstants.PROPERTY_USERID));

    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        try {
            getSession().getWorkspace().createWorkspace("ws2");
        } catch (Exception e) {
            if (!e.getMessage().equals("workspace 'ws2' already exists.")) {
                throw e;
            }
        }

    }

    private static void createNode(Session session, String path) throws RepositoryException {
        session.getRootNode().addNode(path.substring(1), "nt:unstructured");
        session.save();
    }


    private void addNodeToDelete(Session session) throws RepositoryException {
        pathToDelete = createTestPath();
        createNode(session, pathToDelete);

    }

    private void addNodeToModify(Session session) throws RepositoryException {
        pathToModify = createTestPath();
        createNode(session, pathToModify);

    }

    int counter = 0;

    private String createTestPath() {
        counter++;
        return "/test" + System.currentTimeMillis() + counter;
    }

    private List<Event> generateEvents(String workspaceName) throws Exception {
        final Session session = getRepository().loginAdministrative(workspaceName);

        final List<Event> events = new ArrayList<Event>();

        addNodeToModify(session);
        addNodeToDelete(session);

        final JcrResourceResolverFactoryImpl factory = new JcrResourceResolverFactoryImpl();
        PrivateAccessor.setField(factory, "repository", getRepository());
        PrivateAccessor.setField(factory, "useMultiWorkspaces", Boolean.TRUE);

        final EventAdmin mockEA = new EventAdmin() {

            public void postEvent(Event event) {
                events.add(event);
            }

            public void sendEvent(Event event) {
                events.add(event);
            }
        };
        final ServiceTracker tracker = mock(ServiceTracker.class);
        when(tracker.getService()).thenReturn(mockEA);

        JcrResourceListener listener = new SynchronousJcrResourceListener(workspaceName, factory, "/",
                "/", tracker);

        createdPath = createTestPath();
        createNode(session, createdPath);

        Node modified = session.getNode(pathToModify);
        modified.setProperty("foo", "bar");
        session.save();

        Node deleted = session.getNode(pathToDelete);
        deleted.remove();
        session.save();

        Session newSession = getRepository().loginAdministrative(workspaceName);
        EventHelper helper = new EventHelper(newSession);
        helper.waitForEvents(5000);
        helper.dispose();
        newSession.logout();
        session.logout();

        listener.dispose();

        return events;
    }
}
