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

import java.io.Closeable;
import java.util.Collections;

import com.codahale.metrics.MetricRegistry;
import org.apache.sling.commons.metrics.Gauge;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class GaugeManager implements ServiceTrackerCustomizer<Gauge, GaugeManager.GaugeImpl>, Closeable {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final MetricRegistry registry;
    private final BundleMetricsMapper mapper;
    private final BundleContext bundleContext;
    private ServiceTracker<Gauge, GaugeImpl> tracker;

    public GaugeManager(BundleContext context, MetricRegistry registry, BundleMetricsMapper mapper) {
        this.registry = registry;
        this.mapper = mapper;
        this.bundleContext = context;
        this.tracker = new ServiceTracker<>(context, Gauge.class, this);
        tracker.open();
    }

    //~-------------------------------------< ServiceTrackerCustomizer >

    @Override
    public GaugeImpl addingService(ServiceReference<Gauge> reference) {
        String name = (String) reference.getProperty(Gauge.NAME);
        if (name == null){
            log.warn("A {} service is registered without [{}] property. This Gauge would not be " +
                    "registered with MetricsRegistry", reference, Gauge.NAME);
            return null;
        }

        Gauge gauge = bundleContext.getService(reference);
        GaugeImpl gaugeImpl = new GaugeImpl(name, gauge);
        register(reference, gaugeImpl);
        return gaugeImpl;
    }

    @Override
    public void modifiedService(ServiceReference<Gauge> reference, GaugeImpl service) {
        String name = (String) reference.getProperty(Gauge.NAME);
        if (name == null){
            return;
        }

        if (!name.equals(service.name)){
            unregister(service);
            service.name = name;
            register(reference, service);
        }
    }

    @Override
    public void removedService(ServiceReference<Gauge> reference, GaugeImpl service) {
        unregister(service);
    }

    //~------------------------------------< Closeable >

    @Override
    public void close() {
        tracker.close();
    }

    //~-------------------------------------< Internal >

    private void unregister(GaugeImpl service) {
        mapper.unregister(Collections.singleton(service.name));
    }

    private void register(ServiceReference<Gauge> reference, GaugeImpl gaugeImpl) {
        mapper.addMapping(gaugeImpl.name, reference.getBundle());
        registry.register(gaugeImpl.name, gaugeImpl);
    }

    //~--------------------------------------< GaugeImpl >

    public static class GaugeImpl implements com.codahale.metrics.Gauge {
        String name;
        final Gauge gauge;

        public GaugeImpl(String name, Gauge gauge) {
            this.name = name;
            this.gauge = gauge;
        }

        @Override
        public Object getValue() {
            return gauge.getValue();
        }
    }
}
