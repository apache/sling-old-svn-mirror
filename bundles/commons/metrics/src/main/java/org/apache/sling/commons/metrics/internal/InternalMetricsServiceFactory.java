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

package org.apache.sling.commons.metrics.internal;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.sling.commons.metrics.Counter;
import org.apache.sling.commons.metrics.Histogram;
import org.apache.sling.commons.metrics.Meter;
import org.apache.sling.commons.metrics.MetricsService;
import org.apache.sling.commons.metrics.Timer;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

class InternalMetricsServiceFactory implements ServiceFactory<MetricsService> {
    private final MetricsService delegate;
    private final BundleMetricsMapper metricsMapper;

    public InternalMetricsServiceFactory(MetricsService delegate, BundleMetricsMapper metricsMapper) {
        this.delegate = delegate;
        this.metricsMapper = metricsMapper;
    }

    @Override
    public MetricsService getService(Bundle bundle, ServiceRegistration<MetricsService> registration) {
        return new BundleMetricService(bundle);
    }

    @Override
    public void ungetService(Bundle bundle, ServiceRegistration<MetricsService> registration, MetricsService service) {
        if (service instanceof BundleMetricService){
            ((BundleMetricService) service).unregister();
        }
    }

    private class BundleMetricService implements MetricsService {
        private final Bundle bundle;
        private Set<String> registeredNames = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

        public BundleMetricService(Bundle bundle) {
            this.bundle = bundle;
        }

        @Override
        public Timer timer(String name) {
            addMapping(name);
            return delegate.timer(name);
        }

        @Override
        public Histogram histogram(String name) {
            addMapping(name);
            return delegate.histogram(name);
        }

        @Override
        public Counter counter(String name) {
            addMapping(name);
            return delegate.counter(name);
        }

        @Override
        public Meter meter(String name) {
            addMapping(name);
            return delegate.meter(name);
        }

        @Override
        public <A> A adaptTo(Class<A> type) {
            return delegate.adaptTo(type);
        }

        void unregister(){
            metricsMapper.unregister(registeredNames);
        }

        private void addMapping(String name) {
            metricsMapper.addMapping(name, bundle);
            registeredNames.add(name);
        }
    }
}
