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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.jcr.Session;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.testing.jcr.RepositoryUtil;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.internal.helper.jcr.JcrTestNodeResource;
import org.junit.After;
import org.junit.Before;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Test of JcrResourceListener.
 */
public class JcrResourceListenerTest extends AbstractListenerTest {

    private SynchronousJcrResourceListener listener;

    private Session adminSession;

    @After
    public void tearDown() throws Exception {
        if ( adminSession != null ) {
            adminSession.logout();
            adminSession = null;
        }
        RepositoryUtil.stopRepository();
        if ( listener != null ) {
            listener.dispose();
            listener = null;
        }
    }

    @Before
    public void setUp() throws Exception {
        RepositoryUtil.startRepository();
        this.adminSession = RepositoryUtil.getRepository().loginAdministrative(null);
        RepositoryUtil.registerSlingNodeTypes(adminSession);
        final ResourceResolver resolver = Mockito.mock(ResourceResolver.class);
        Mockito.when(resolver.adaptTo(Mockito.any(Class.class))).thenReturn(this.adminSession);
        Mockito.when(resolver.getResource(Mockito.anyString())).thenReturn(new JcrTestNodeResource(resolver, this.adminSession.getNode("/"), null));

        final ResourceResolverFactory factory = Mockito.mock(ResourceResolverFactory.class);
        Mockito.when(factory.getAdministrativeResourceResolver(Mockito.anyMap())).thenReturn(resolver);

        final EventAdmin mockEA = new EventAdmin() {

            public void postEvent(final Event event) {
                addEvent(event);
            }

            public void sendEvent(final Event event) {
                addEvent(event);
            }
        };

        final ServiceTracker tracker = mock(ServiceTracker.class);
        when(tracker.getService()).thenReturn(mockEA);

        final BundleContext bundleContext = mock(BundleContext.class);
        when(bundleContext.createFilter(any(String.class))).thenReturn(null);
        when(bundleContext.getServiceReference(any(String.class))).thenReturn(null);
        when(bundleContext.getService(null)).thenReturn(mockEA);

        this.listener = new SynchronousJcrResourceListener(RepositoryUtil.getRepository(),
                        bundleContext, resolver, tracker);
    }

    @Override
    public SlingRepository getRepository() {
        return RepositoryUtil.getRepository();
    }
}
