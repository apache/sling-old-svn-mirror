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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.management.MBeanServer;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.commons.metrics.Meter;
import org.apache.sling.commons.metrics.MetricsService;
import org.apache.sling.commons.metrics.Timer;
import org.apache.sling.commons.metrics.Counter;
import org.apache.sling.commons.metrics.Histogram;
import org.apache.sling.commons.metrics.Metric;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

@Component
public class MetricsServiceImpl implements MetricsService {
    private final List<ServiceRegistration> regs = new ArrayList<ServiceRegistration>();
    private final ConcurrentMap<String, Metric> metrics = new ConcurrentHashMap<String, Metric>();
    private final MetricRegistry registry = new MetricRegistry();

    @Reference
    private MBeanServer server;

    private JmxReporter reporter;

    @Activate
    private void activate(BundleContext context, Map<String, Object> config) {
        //TODO Domain name should be based on calling bundle
        //For that we can register ServiceFactory and make use of calling
        //bundle symbolic name to determine the mapping

        reporter = JmxReporter.forRegistry(registry)
                .inDomain("org.apache.sling")
                .registerWith(server)
                .build();

        final Dictionary<String, String> svcProps = new Hashtable<String, String>();
        svcProps.put(Constants.SERVICE_DESCRIPTION, "Apache Sling Metrics Service");
        svcProps.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        regs.add(context.registerService(MetricsService.class.getName(), this, svcProps));

        final Dictionary<String, String> regProps = new Hashtable<String, String>();
        regProps.put(Constants.SERVICE_DESCRIPTION, "Apache Sling Metrics Registry");
        regProps.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        regProps.put("name", "sling");
        regs.add(context.registerService(MetricRegistry.class.getName(), registry, regProps));
    }

    @Deactivate
    private void deactivate() throws IOException {
        for (ServiceRegistration reg : regs) {
            reg.unregister();
        }
        regs.clear();

        metrics.clear();

        if (reporter != null) {
            reporter.close();
        }
    }

    @Override
    public Timer timer(String name) {
        return getOrAdd(name, MetricBuilder.TIMERS);
    }

    @Override
    public Histogram histogram(String name) {
        return getOrAdd(name, MetricBuilder.HISTOGRAMS);
    }

    @Override
    public Counter counter(String name) {
        return getOrAdd(name, MetricBuilder.COUNTERS);
    }

    @Override
    public Meter meter(String name) {
        return getOrAdd(name, MetricBuilder.METERS);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <A> A adaptTo(Class<A> type) {
        if (type == MetricRegistry.class){
            return (A) registry;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private <T extends Metric> T getOrAdd(String name, MetricBuilder<T> builder) {
        final Metric metric = metrics.get(name);
        if (builder.isInstance(metric)) {
            return (T) metric;
        } else if (metric == null) {
            try {
                return register(name, builder.newMetric(registry, name));
            } catch (IllegalArgumentException e) {
                final Metric added = metrics.get(name);
                if (builder.isInstance(added)) {
                    return (T) added;
                }
            }
        }
        throw new IllegalArgumentException(name + " is already used for a different type of metric");
    }

    private <T extends Metric> T register(String name, T metric) throws IllegalArgumentException {
        final Metric existing = metrics.putIfAbsent(name, metric);
        if (existing != null) {
            throw new IllegalArgumentException("A metric named " + name + " already exists");
        }
        return metric;
    }

    /**
     * A quick and easy way of capturing the notion of default metrics.
     */
    private interface MetricBuilder<T extends Metric> {
        MetricBuilder<Counter> COUNTERS = new MetricBuilder<Counter>() {
            @Override
            public Counter newMetric(MetricRegistry registry, String name) {
                return new CounterImpl(registry.counter(name));
            }

            @Override
            public boolean isInstance(Metric metric) {
                return Counter.class.isInstance(metric);
            }
        };

        MetricBuilder<Histogram> HISTOGRAMS = new MetricBuilder<Histogram>() {
            @Override
            public Histogram newMetric(MetricRegistry registry, String name) {
                return new HistogramImpl(registry.histogram(name));
            }

            @Override
            public boolean isInstance(Metric metric) {
                return Histogram.class.isInstance(metric);
            }
        };

        MetricBuilder<Meter> METERS = new MetricBuilder<Meter>() {
            @Override
            public Meter newMetric(MetricRegistry registry, String name) {
                return new MeterImpl(registry.meter(name));
            }

            @Override
            public boolean isInstance(Metric metric) {
                return Meter.class.isInstance(metric);
            }
        };

        MetricBuilder<Timer> TIMERS = new MetricBuilder<Timer>() {
            @Override
            public Timer newMetric(MetricRegistry registry, String name) {
                return new TimerImpl(registry.timer(name));
            }

            @Override
            public boolean isInstance(Metric metric) {
                return Timer.class.isInstance(metric);
            }
        };

        T newMetric(MetricRegistry registry, String name);

        boolean isInstance(Metric metric);
    }
}
