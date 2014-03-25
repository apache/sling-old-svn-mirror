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
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.api.SlingConstants;
import org.apache.sling.jcr.api.SlingRepository;
import org.junit.Test;
import org.osgi.service.event.Event;

/**
 * Base for the two different listener tests.
 */
public abstract class AbstractListenerTest {

    private String createdPath = "/test" + System.currentTimeMillis() + "-create";

    private String pathToDelete = "/test" + System.currentTimeMillis() + "-delete";

    private String pathToModify = "/test" + System.currentTimeMillis() + "-modify";

    private final List<Event> events = synchronizedList(new ArrayList<Event>());

    protected void addEvent(final Event e) {
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        for(final String name : e.getPropertyNames()) {
            props.put(name, e.getProperty(name));
        }
        this.events.add(new Event(e.getTopic(), props) {

            @Override
            public String toString() {
                return "Event(topic=" + e.getTopic() + ", properties=" + props + ")";
            }
        });
    }

    @Test public void testSimpleOperations() throws Exception {
        generateEvents();

        assertEquals("Received: " + events, 5, events.size());
        final Set<String> addPaths = new HashSet<String>();
        final Set<String> modifyPaths = new HashSet<String>();
        final Set<String> removePaths = new HashSet<String>();

        for(final Event event : events) {
            if ( event.getTopic().equals(SlingConstants.TOPIC_RESOURCE_ADDED) ) {
                addPaths.add((String)event.getProperty(SlingConstants.PROPERTY_PATH));
            } else if ( event.getTopic().equals(SlingConstants.TOPIC_RESOURCE_CHANGED) ) {
                modifyPaths.add((String)event.getProperty(SlingConstants.PROPERTY_PATH));
            } else if ( event.getTopic().equals(SlingConstants.TOPIC_RESOURCE_REMOVED) ) {
                removePaths.add((String)event.getProperty(SlingConstants.PROPERTY_PATH));
            } else {
                fail("Unexpected event: " + event);
            }
            assertNotNull(event.getProperty(SlingConstants.PROPERTY_USERID));
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
}
