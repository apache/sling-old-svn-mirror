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

package org.apache.sling.metrics.internal;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import org.apache.commons.io.output.WriterOutputStream;
import org.apache.felix.inventory.Format;
import org.apache.felix.inventory.InventoryPrinter;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Service(value = InventoryPrinter.class)
@Properties({
        @Property(name = InventoryPrinter.FORMAT, value = {"TEXT" }),
        @Property(name = InventoryPrinter.NAME, value = "slingmetrics"),
        @Property(name = InventoryPrinter.TITLE, value = "Sling Metrics"),
        @Property(name = InventoryPrinter.WEBCONSOLE, boolValue = true)
})
public class MetricPrinter implements InventoryPrinter, ServiceTrackerCustomizer<MetricRegistry, MetricRegistry>{
    /**
     * Service property name which stores the MetricRegistry name as a given OSGi
     * ServiceRegistry might have multiple instances of MetricRegistry
     */
    public static final String METRIC_REGISTRY_NAME = "name";
    private final Logger log = LoggerFactory.getLogger(getClass());
    private BundleContext context;
    private ServiceTracker<MetricRegistry, MetricRegistry> tracker;
    private ConcurrentMap<ServiceReference, MetricRegistry> registries
            = new ConcurrentHashMap<ServiceReference, MetricRegistry>();

    @Activate
    private void activate(BundleContext context){
        this.context = context;
        tracker = new ServiceTracker<MetricRegistry, MetricRegistry>(context, MetricRegistry.class, this);
        tracker.open();
    }

    @Deactivate
    private void deactivate(BundleContext context){
        tracker.close();
    }

    //~--------------------------------------------< InventoryPrinter >

    @Override
    public void print(PrintWriter printWriter, Format format, boolean isZip) {
        if (format == Format.TEXT) {
            MetricRegistry registry = getConsolidatedRegistry();
            ConsoleReporter reporter = ConsoleReporter.forRegistry(registry)
                    .outputTo(new PrintStream(new WriterOutputStream(printWriter)))
                    .build();
            reporter.report();
            reporter.close();
        }
    }


    //~---------------------------------------------< ServiceTracker >

    @Override
    public MetricRegistry addingService(ServiceReference<MetricRegistry> serviceReference) {
        MetricRegistry registry = context.getService(serviceReference);
        registries.put(serviceReference, registry);
        return registry;
    }

    @Override
    public void modifiedService(ServiceReference<MetricRegistry> serviceReference, MetricRegistry registry) {
        registries.put(serviceReference, registry);
    }

    @Override
    public void removedService(ServiceReference<MetricRegistry> serviceReference, MetricRegistry registry) {
        registries.remove(serviceReference);
    }

    //~----------------------------------------------< internal >

    private MetricRegistry getConsolidatedRegistry() {
        MetricRegistry registry = new MetricRegistry();
        for (Map.Entry<ServiceReference, MetricRegistry> registryEntry : registries.entrySet()){
            String metricRegistryName = (String) registryEntry.getKey().getProperty(METRIC_REGISTRY_NAME);
            for (Map.Entry<String, Metric> metricEntry : registryEntry.getValue().getMetrics().entrySet()){
                String metricName = metricEntry.getKey();
                try{
                    if (metricRegistryName != null){
                        metricName = metricRegistryName + ":" + metricName;
                    }
                    registry.register(metricName, metricEntry.getValue());
                }catch (IllegalArgumentException ex){
                    log.warn("Duplicate Metric name found {}", metricName, ex);
                }
            }
        }
        return registry;
    }
}
