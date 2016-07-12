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
package org.apache.sling.resourceresolver.impl.providers;

import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.path.Path;
import org.apache.sling.api.resource.path.PathSet;
import org.apache.sling.api.resource.runtime.dto.AuthType;
import org.apache.sling.api.resource.runtime.dto.RuntimeDTO;
import org.apache.sling.resourceresolver.impl.Fixture;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderTracker.ChangeListener;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderTracker.ObservationReporterGenerator;
import org.apache.sling.spi.resource.provider.ObservationReporter;
import org.apache.sling.spi.resource.provider.ObserverConfiguration;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.EventAdmin;

public class ResourceProviderTrackerTest {

    private ResourceProviderInfo rp2Info;
    private ResourceProviderTracker tracker;
    private Fixture fixture;

    @Before
    public void prepare() throws Exception {
        BundleContext bundleContext = MockOsgi.newBundleContext();

        fixture = new Fixture(bundleContext);

        EventAdmin eventAdmin = mock(EventAdmin.class);

        @SuppressWarnings("unchecked")
        ResourceProvider<Object> rp = mock(ResourceProvider.class);
        @SuppressWarnings("unchecked")
        ResourceProvider<Object> rp2 = mock(ResourceProvider.class);
        @SuppressWarnings("unchecked")
        ResourceProvider<Object> rp3 = mock(ResourceProvider.class);

        fixture.registerResourceProvider(rp, "/", AuthType.no);
        rp2Info = fixture.registerResourceProvider(rp2, "/path", AuthType.lazy);
        fixture.registerResourceProvider(rp3, "invalid", AuthType.no);

        tracker = new ResourceProviderTracker();

        tracker.setObservationReporterGenerator(new SimpleObservationReporterGenerator(new NoDothingObservationReporter()));
        tracker.activate(bundleContext, eventAdmin, new DoNothingChangeListener());
    }

    @Test
    public void activate() {

        // since the OSGi mocks are asynchronous we don't have to wait for the changes to propagate

        assertThat(tracker.getResourceProviderStorage().getAllHandlers().size(), equalTo(2));

        fixture.unregisterResourceProvider(rp2Info);

        assertThat(tracker.getResourceProviderStorage().getAllHandlers().size(), equalTo(1));
    }

    @Test
    public void deactivate() {

        tracker.deactivate();

        assertThat(tracker.getResourceProviderStorage().getAllHandlers(), hasSize(0));
    }

    @Test
    public void testActivationDeactivation() throws Exception {
        final BundleContext bundleContext = MockOsgi.newBundleContext();
        final EventAdmin eventAdmin = mock(EventAdmin.class);

        final Fixture fixture = new Fixture(bundleContext);
        final ResourceProviderTracker tracker = new ResourceProviderTracker();
        tracker.setObservationReporterGenerator(new SimpleObservationReporterGenerator(new NoDothingObservationReporter()));

        // create boolean markers for the listener
        final AtomicBoolean addedCalled = new AtomicBoolean(false);
        final AtomicBoolean removedCalled = new AtomicBoolean(false);

        final ChangeListener listener = new ChangeListener() {

            @Override
            public void providerAdded() {
                addedCalled.set(true);
            }

            @Override
            public void providerRemoved(String name, String pid, boolean stateful, boolean used) {
                removedCalled.set(true);
            }

        };
        // activate and check that no listener is called yet
        tracker.activate(bundleContext, eventAdmin, listener);
        assertFalse(addedCalled.get());
        assertFalse(removedCalled.get());

        // add a new resource provider
        @SuppressWarnings("unchecked")
        ResourceProvider<Object> rp = mock(ResourceProvider.class);
        final ResourceProviderInfo info = fixture.registerResourceProvider(rp, "/", AuthType.no);

        // check added is called but not removed
        assertTrue(addedCalled.get());
        assertFalse(removedCalled.get());

        // verify a single provider
        assertThat(tracker.getResourceProviderStorage().getAllHandlers().size(), equalTo(1));

        // reset boolean markers
        addedCalled.set(false);
        removedCalled.set(false);

        // remove provider
        fixture.unregisterResourceProvider(info);

        // verify removed is called but not added
        assertTrue(removedCalled.get());
        assertFalse(addedCalled.get());

        // no provider anymore
        assertThat(tracker.getResourceProviderStorage().getAllHandlers().size(), equalTo(0));
    }

    @Test
    public void testReactivation() throws Exception {
        final BundleContext bundleContext = MockOsgi.newBundleContext();
        final EventAdmin eventAdmin = mock(EventAdmin.class);

        final Fixture fixture = new Fixture(bundleContext);
        final ResourceProviderTracker tracker = new ResourceProviderTracker();
        tracker.setObservationReporterGenerator(new SimpleObservationReporterGenerator(new NoDothingObservationReporter()));

        // create boolean markers for the listener
        final AtomicBoolean addedCalled = new AtomicBoolean(false);
        final AtomicBoolean removedCalled = new AtomicBoolean(false);

        final ChangeListener listener = new ChangeListener() {

            @Override
            public void providerAdded() {
                addedCalled.set(true);
            }

            @Override
            public void providerRemoved(String name, String pid, boolean stateful, boolean used) {
                removedCalled.set(true);
            }

        };
        // activate and check that no listener is called yet
        tracker.activate(bundleContext, eventAdmin, listener);
        assertFalse(addedCalled.get());
        assertFalse(removedCalled.get());

        // activate and check that no listener is called yet
        @SuppressWarnings("unchecked")
        ResourceProvider<Object> rp = mock(ResourceProvider.class);
        final ResourceProviderInfo info = fixture.registerResourceProvider(rp, "/", AuthType.no);

        // check added is called but not removed
        assertTrue(addedCalled.get());
        assertFalse(removedCalled.get());

        // verify a single provider
        assertThat(tracker.getResourceProviderStorage().getAllHandlers().size(), equalTo(1));

        // reset boolean markers
        addedCalled.set(false);
        removedCalled.set(false);

        // add overlay provider
        final ResourceProviderInfo infoOverlay = fixture.registerResourceProvider(rp, "/", AuthType.no);

        // check added and removed is called
        assertTrue(addedCalled.get());
        assertTrue(removedCalled.get());

        // verify a single provider
        assertThat(tracker.getResourceProviderStorage().getAllHandlers().size(), equalTo(1));

        // reset boolean markers
        addedCalled.set(false);
        removedCalled.set(false);

        // unregister overlay provider
        fixture.unregisterResourceProvider(infoOverlay);

        // check added and removed is called
        assertTrue(addedCalled.get());
        assertTrue(removedCalled.get());

        // verify a single provider
        assertThat(tracker.getResourceProviderStorage().getAllHandlers().size(), equalTo(1));

        // reset boolean markers
        addedCalled.set(false);
        removedCalled.set(false);

        // unregister first provider
        fixture.unregisterResourceProvider(info);

        // check removed is called but not added
        assertTrue(removedCalled.get());
        assertFalse(addedCalled.get());

        // verify no provider
        assertThat(tracker.getResourceProviderStorage().getAllHandlers().size(), equalTo(0));
    }

    @Test
    public void fillDto() throws Exception {

        RuntimeDTO dto = new RuntimeDTO();

        tracker.fill(dto);

        assertThat( dto.providers, arrayWithSize(2));
        assertThat( dto.failedProviders, arrayWithSize(1));
    }

    static final class NoDothingObservationReporter implements ObservationReporter {
        @Override
        public void reportChanges(Iterable<ResourceChange> changes, boolean distribute) {
        }

        @Override
        public List<ObserverConfiguration> getObserverConfigurations() {
            return Collections.emptyList();
        }
    }

    static final class SimpleObservationReporterGenerator implements ObservationReporterGenerator {
        private final ObservationReporter reporter;

        SimpleObservationReporterGenerator(ObservationReporter reporter) {
            this.reporter = reporter;
        }

        @Override
        public ObservationReporter createProviderReporter() {
            return reporter;
        }

        @Override
        public ObservationReporter create(Path path, PathSet excludes) {
            return reporter;
        }
    }

    static final class DoNothingChangeListener implements ChangeListener {

        @Override
        public void providerAdded() {
            // TODO Auto-generated method stub

        }

        @Override
        public void providerRemoved(String name, String pid, boolean stateful, boolean used) {
            // TODO Auto-generated method stub

        }
    }
}