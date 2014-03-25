/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.jcr.resource.internal;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.ObservationManager;

import org.apache.sling.jcr.api.SlingRepository;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.EventAdmin;

/**
 * This test case asserts that JcrResourceListener scales to an
 * arbitrary number of events.
 */
public class JcrResourceListenerScalabilityTest {

    private JcrResourceListener jcrResourceListener;
    private EventIterator events;

    @Before
    public void setUp() throws RepositoryException, InvalidSyntaxException {
        ObservationManager observationManager = mock(ObservationManager.class);

        Workspace workspace = mock(Workspace.class);
        when(workspace.getObservationManager()).thenReturn(observationManager);

        Session session = mock(Session.class);
        when(session.getWorkspace()).thenReturn(workspace);

        SlingRepository repository = mock(SlingRepository.class);
        when(repository.loginAdministrative(null)).thenReturn(session);

        EventAdmin eventAdmin = mock(EventAdmin.class);
        ServiceReference serviceRef = mock(ServiceReference.class);
        ServiceReference[] serviceRefs = new ServiceReference[]{serviceRef};
        BundleContext bundleContext = mock(BundleContext.class);
        when(bundleContext.getServiceReferences(anyString(), anyString())).thenReturn(serviceRefs);
        when(bundleContext.getService(serviceRef)).thenReturn(eventAdmin);

        jcrResourceListener = new JcrResourceListener("/", new ObservationListenerSupport(bundleContext, repository));

        Event event = mock(MockEvent.class);
        events = mock(EventIterator.class);
        when(events.hasNext()).thenReturn(true);
        when(event.getPath()).thenCallRealMethod();
        when(event.getType()).thenReturn(Event.NODE_ADDED);
        when(events.nextEvent()).thenReturn(event);
    }

    @Ignore("SLING-3399")  // FIXME SLING-3399
    @Test
    public void testManyEvents() throws RepositoryException, InterruptedException, InvalidSyntaxException {
        jcrResourceListener.onEvent(events);
    }

    private abstract static class MockEvent implements Event {
        int count;

        public String getPath() throws RepositoryException {
            return "path-" + count++;
        }
    }
}
