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
package org.apache.sling.resourceresolver.impl.observation;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.apache.sling.api.resource.path.Path;
import org.apache.sling.api.resource.path.PathSet;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderTracker;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderTracker.ObservationReporterGenerator;
import org.apache.sling.spi.resource.provider.ObservationReporter;
import org.apache.sling.spi.resource.provider.ObserverConfiguration;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracker component for the resource change listeners.
 */
public class ResourceChangeListenerWhiteboard implements ResourceProviderTracker.ObservationReporterGenerator {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Map<ServiceReference<ResourceChangeListener>, ResourceChangeListenerInfo> listeners = new ConcurrentHashMap<>();

    private volatile ResourceProviderTracker resourceProviderTracker;

    private volatile ServiceTracker<ResourceChangeListener, ServiceReference<ResourceChangeListener>> tracker;

    private volatile String[] searchPath;

    public void activate(final BundleContext bundleContext,
            final ResourceProviderTracker resourceProviderTracker,
            final String[] searchPath) {
        this.searchPath = searchPath;
        this.resourceProviderTracker = resourceProviderTracker;
        this.resourceProviderTracker.setObservationReporterGenerator(this);
        this.tracker = new ServiceTracker<>(bundleContext,
                ResourceChangeListener.class,
                new ServiceTrackerCustomizer<ResourceChangeListener, ServiceReference<ResourceChangeListener>>() {

            @Override
            public void removedService(final ServiceReference<ResourceChangeListener> reference, final ServiceReference<ResourceChangeListener> service) {
                final ResourceChangeListenerInfo info = listeners.remove(reference);
                if ( info != null ) {
                    updateProviderTracker();
                }
            }

            @Override
            public void modifiedService(final ServiceReference<ResourceChangeListener> reference, final ServiceReference<ResourceChangeListener> service) {
                removedService(reference, service);
                addingService(reference);
            }

            @Override
            public ServiceReference<ResourceChangeListener> addingService(final ServiceReference<ResourceChangeListener> reference) {
                final ResourceChangeListenerInfo info = new ResourceChangeListenerInfo(reference, searchPath);
                if ( info.isValid() ) {
                    final ResourceChangeListener listener = bundleContext.getService(reference);
                    if ( listener != null ) {
                        info.setListener(listener);
                        listeners.put(reference, info);
                        updateProviderTracker();
                    }
                } else {
                    logger.warn("Ignoring invalid resource change listenr {}", reference);
                }
                return reference;
            }
        });
        this.tracker.open();
    }

    public void deactivate() {
        if ( this.tracker != null ) {
            this.tracker.close();
            this.tracker = null;
        }
        this.resourceProviderTracker.setObservationReporterGenerator(NOP_GENERATOR);
        this.resourceProviderTracker = null;
    }

    private void updateProviderTracker() {
        this.resourceProviderTracker.setObservationReporterGenerator(this);
    }

    @Override
    public ObservationReporter create(final Path path, final PathSet excludes) {
        return new BasicObservationReporter(this.searchPath, this.listeners.values(), path, excludes);
    }

    @Override
    public ObservationReporter createProviderReporter() {
        return new BasicObservationReporter(this.searchPath, this.listeners.values());
    }

    private static final ObservationReporter EMPTY_REPORTER = new ObservationReporter() {

        @Override
        public void reportChanges(Iterable<ResourceChange> changes, boolean distribute) {
            // ignore
        }

        @Override
        public void reportChanges(ObserverConfiguration config, Iterable<ResourceChange> changes, boolean distribute) {
            // ignore
        }

        @Override
        public List<ObserverConfiguration> getObserverConfigurations() {
            return Collections.emptyList();
        }
    };

    private static final ObservationReporterGenerator NOP_GENERATOR = new ObservationReporterGenerator() {

        @Override
        public ObservationReporter create(Path path, PathSet excludes) {
            return EMPTY_REPORTER;
        }

        @Override
        public ObservationReporter createProviderReporter() {
            return EMPTY_REPORTER;
        }
    };
}
