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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.testing.jcr.EventHelper;
import org.apache.sling.commons.testing.jcr.RepositoryTestBase;
import org.apache.sling.jcr.resource.internal.helper.jcr.JcrNodeResource;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

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

        final ResourceResolver resolver = new ResourceResolver() {

            public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
                return (AdapterType)session;
            }

            public Resource resolve(HttpServletRequest request, String absPath) {
                // TODO Auto-generated method stub
                return null;
            }

            public Resource resolve(String absPath) {
                // TODO Auto-generated method stub
                return null;
            }

            public Resource resolve(HttpServletRequest request) {
                // TODO Auto-generated method stub
                return null;
            }

            public String map(String resourcePath) {
                // TODO Auto-generated method stub
                return null;
            }

            public String map(HttpServletRequest request, String resourcePath) {
                // TODO Auto-generated method stub
                return null;
            }

            public Resource getResource(String path) {
                // TODO Auto-generated method stub
                try {
                    return new JcrNodeResource(this, session.getNode(path), null);
                } catch (PathNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (RepositoryException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                return null;
            }

            public Resource getResource(Resource base, String path) {
                // TODO Auto-generated method stub
                return null;
            }

            public String[] getSearchPath() {
                // TODO Auto-generated method stub
                return null;
            }

            public Iterator<Resource> listChildren(Resource parent) {
                // TODO Auto-generated method stub
                return null;
            }

            public Iterator<Resource> findResources(String query, String language) {
                // TODO Auto-generated method stub
                return null;
            }

            public Iterator<Map<String, Object>> queryResources(String query, String language) {
                // TODO Auto-generated method stub
                return null;
            }

            public ResourceResolver clone(Map<String, Object> authenticationInfo) throws LoginException {
                // TODO Auto-generated method stub
                return null;
            }

            public boolean isLive() {
                // TODO Auto-generated method stub
                return false;
            }

            public void close() {
                // TODO Auto-generated method stub

            }

            public String getUserID() {
                // TODO Auto-generated method stub
                return null;
            }

            public Iterator<String> getAttributeNames() {
                // TODO Auto-generated method stub
                return null;
            }

            public Object getAttribute(String name) {
                // TODO Auto-generated method stub
                return null;
            }

        };
        final ResourceResolverFactory factory = new ResourceResolverFactory() {

            public ResourceResolver getResourceResolver(Map<String, Object> authenticationInfo) throws LoginException {
                return null;
            }

            public ResourceResolver getAdministrativeResourceResolver(Map<String, Object> authenticationInfo) throws LoginException {
                return resolver;
            }
        };

        final EventAdmin mockEA = new EventAdmin() {

            public void postEvent(Event event) {
                events.add(event);
            }

            public void sendEvent(Event event) {
                events.add(event);
            }
        };

        SynchronousJcrResourceListener listener = new SynchronousJcrResourceListener(factory, mockEA);

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
