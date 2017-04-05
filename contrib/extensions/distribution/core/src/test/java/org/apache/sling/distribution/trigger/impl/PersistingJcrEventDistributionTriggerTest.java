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
package org.apache.sling.distribution.trigger.impl;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.observation.Event;
import javax.jcr.observation.ObservationManager;

import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.trigger.DistributionRequestHandler;
import org.apache.sling.jcr.api.SlingRepository;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testcase for {@link PersistedJcrEventDistributionTrigger}
 */
@Ignore("Until SLING-4312 is fixed")
public class PersistingJcrEventDistributionTriggerTest {

    @Test
    public void testProcessEventWithoutPrivileges() throws Exception {
        String serviceName = "serviceId";
        Session session = mock(Session.class);
        when(session.nodeExists("/var/nuggets")).thenReturn(true);
        SlingRepository repository = mock(SlingRepository.class);
        Scheduler scheduler = mock(Scheduler.class);
        ResourceResolverFactory resolverFactory  = mock(ResourceResolverFactory.class);
        when(repository.loginService(serviceName, null)).thenReturn(session);
        String path = "/some/path";
        String nuggetsPath = "/var/nuggets";
        PersistedJcrEventDistributionTrigger persistingJcrEventdistributionTrigger = new PersistedJcrEventDistributionTrigger(
                repository, scheduler, resolverFactory, path, serviceName, nuggetsPath);
        Event event = mock(Event.class);
        DistributionRequest distributionRequest = persistingJcrEventdistributionTrigger.processEvent(event);
        assertNull(distributionRequest);
    }

    @Test
    public void testProcessEventWithoutPrivilegesAndNuggetsPath() throws Exception {
        String serviceName = "serviceId";
        Session session = mock(Session.class);
        when(session.hasPermission(eq("var"), eq(Session.ACTION_ADD_NODE))).thenReturn(true);
        when(session.hasPermission(eq("nuggets"), eq(Session.ACTION_ADD_NODE))).thenReturn(true);
        when(session.nodeExists("/var/nuggets")).thenReturn(false); // first time it doesn't exist and should be created
        when(session.nodeExists("/var/nuggets")).thenReturn(true); // second time it should exist
        Node rootNode = mock(Node.class);
        Node varNode = mock(Node.class);
        Node nuggetsNode = mock(Node.class);
        when(varNode.addNode("nuggets", "sling:Folder")).thenReturn(nuggetsNode);
        when(rootNode.addNode("var", "sling:Folder")).thenReturn(varNode);
        when(session.getRootNode()).thenReturn(rootNode);
        SlingRepository repository = mock(SlingRepository.class);
        Scheduler scheduler = mock(Scheduler.class);
        ResourceResolverFactory resolverFactory  = mock(ResourceResolverFactory.class);

        when(repository.loginService(serviceName, null)).thenReturn(session);
        String path = "/some/path";
        String nuggetsPath = "/var/nuggets";
        PersistedJcrEventDistributionTrigger persistingJcrEventdistributionTrigger = new PersistedJcrEventDistributionTrigger(
                repository, scheduler, resolverFactory, path, serviceName, nuggetsPath);
        Event event = mock(Event.class);
        DistributionRequest distributionRequest = persistingJcrEventdistributionTrigger.processEvent(event);
        assertNull(distributionRequest);
    }

    @Test
    public void testProcessEventWithPrivileges() throws Exception {
        String nuggetsPath = "/var/nuggets";
        String serviceName = "serviceId";
        Session session = mock(Session.class);
        when(session.nodeExists("/var/nuggets")).thenReturn(true);
        Workspace workspace = mock(Workspace.class);
        ObservationManager observationManager = mock(ObservationManager.class);
        when(workspace.getObservationManager()).thenReturn(observationManager);
        when(session.getWorkspace()).thenReturn(workspace);
        when(session.hasPermission(nuggetsPath, Session.ACTION_ADD_NODE)).thenReturn(true);

        SlingRepository repository = mock(SlingRepository.class);
        Scheduler scheduler = mock(Scheduler.class);
        ResourceResolverFactory resolverFactory  = mock(ResourceResolverFactory.class);

        when(repository.loginService(serviceName, null)).thenReturn(session);

        String path = "/some/path";
        PersistedJcrEventDistributionTrigger persistingJcrEventdistributionTrigger = new PersistedJcrEventDistributionTrigger(
                repository, scheduler, resolverFactory, path, serviceName, nuggetsPath);
        DistributionRequestHandler handler = mock(DistributionRequestHandler.class);
        persistingJcrEventdistributionTrigger.register(handler);

        Node nuggetsNode = mock(Node.class);
        Node eventNode = mock(Node.class);
        when(nuggetsNode.addNode(any(String.class), any(String.class))).thenReturn(eventNode);
        when(session.getNode(nuggetsPath)).thenReturn(nuggetsNode);
        Event event = mock(Event.class);
        when(event.getPath()).thenReturn("/some/path/generating/event");
        DistributionRequest distributionRequest = persistingJcrEventdistributionTrigger.processEvent(event);
        assertNotNull(distributionRequest);
    }

    @Test
    public void testProcessEventWithPrivilegesAndNoNuggetsNode() throws Exception {
        String nuggetsPath = "/var/nuggets";
        String serviceName = "serviceId";
        Session session = mock(Session.class);
        when(session.nodeExists("/var/nuggets")).thenReturn(false); // first time it doesn't exist and should be created
        when(session.nodeExists("/var/nuggets")).thenReturn(true); // second time it should exist
        Node rootNode = mock(Node.class);
        Node varNode = mock(Node.class);
        Node nuggetsNode = mock(Node.class);
        when(varNode.addNode("nuggets", "sling:Folder")).thenReturn(nuggetsNode);
        when(rootNode.addNode("var", "sling:Folder")).thenReturn(varNode);
        when(session.getRootNode()).thenReturn(rootNode);
        Workspace workspace = mock(Workspace.class);
        ObservationManager observationManager = mock(ObservationManager.class);
        when(workspace.getObservationManager()).thenReturn(observationManager);
        when(session.getWorkspace()).thenReturn(workspace);
        when(session.hasPermission(any(String.class), eq(Session.ACTION_ADD_NODE))).thenReturn(true);

        SlingRepository repository = mock(SlingRepository.class);
        Scheduler scheduler = mock(Scheduler.class);
        ResourceResolverFactory resolverFactory  = mock(ResourceResolverFactory.class);


        when(repository.loginService(serviceName, null)).thenReturn(session);

        String path = "/some/path";
        PersistedJcrEventDistributionTrigger persistingJcrEventdistributionTrigger = new PersistedJcrEventDistributionTrigger(
                repository, scheduler, resolverFactory, path, serviceName, nuggetsPath);
        DistributionRequestHandler handler = mock(DistributionRequestHandler.class);
        persistingJcrEventdistributionTrigger.register(handler);

        Node eventNode = mock(Node.class);
        when(nuggetsNode.addNode(any(String.class), any(String.class))).thenReturn(eventNode);
        when(session.getNode(nuggetsPath)).thenReturn(nuggetsNode);
        Event event = mock(Event.class);
        when(event.getPath()).thenReturn("/some/path/generating/event");
        DistributionRequest distributionRequest = persistingJcrEventdistributionTrigger.processEvent(event);
        assertNotNull(distributionRequest);
    }
}