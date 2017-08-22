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
package org.apache.sling.commons.metrics.rrd4j.impl;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;

import org.apache.felix.inventory.Format;
import org.apache.felix.inventory.InventoryPrinter;
import org.apache.felix.inventory.ZipAttachmentProvider;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.apache.sling.commons.metrics.rrd4j.impl.RRD4JReporter.DEFAULT_STEP;

@Component(
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        service = {InventoryPrinter.class, ZipAttachmentProvider.class},
        property = {
                InventoryPrinter.NAME + "=rrd4j-reporter",
                InventoryPrinter.TITLE + "=Sling Metrics RRD4J reporter",
                InventoryPrinter.FORMAT + "=TEXT"
        }
)
@Designate(ocd = CodahaleMetricsReporter.Configuration.class)
public class CodahaleMetricsReporter implements InventoryPrinter, ZipAttachmentProvider {

    private static final Logger LOG = LoggerFactory.getLogger(CodahaleMetricsReporter.class);

    private Configuration configuration;
    private File rrd;
    private ScheduledReporter reporter;

    private Map<String, CopyMetricRegistryListener> listeners = new ConcurrentHashMap<>();

    @ObjectClassDefinition(name = "Apache Sling Metrics reporter writing to RRD4J",
            description = "For syntax details on RRD data-source and round " +
                    "robin archive definitions see " +
                    "https://oss.oetiker.ch/rrdtool/doc/rrdcreate.en.html and " +
                    "https://github.com/rrd4j/rrd4j/wiki/Tutorial. Changing " +
                    "any attribute in this configuration will replace an " +
                    "existing RRD file with a empty one!")
    public @interface Configuration {

        @AttributeDefinition(
                name = "Data sources",
                description = "RRDTool data source definitions " +
                        "(e.g. 'DS:oak_SESSION_LOGIN_COUNTER:COUNTER:300:0:U'). " +
                        "Replace colon characters in the metric name with an " +
                        "underscore!"
        )
        String[] datasources() default {};

        @AttributeDefinition(
                name = "Step",
                description = "The base interval in seconds with which data " +
                        "will be fed into the RRD"
        )
        int step() default DEFAULT_STEP;

        @AttributeDefinition(
                name = "Archives",
                description = "RRDTool round robin archive definitions. The " +
                        "default configuration defines four archives based " +
                        "on a default step of five seconds: " +
                        "1) per minute averages for six hours, " +
                        "2) per five minute averages 48 hours, " +
                        "3) per hour averages for four weeks, " +
                        "4) per day averages for one year."
        )
        String[] archives() default {
            "RRA:AVERAGE:0.5:12:360", "RRA:AVERAGE:0.5:60:576", "RRA:AVERAGE:0.5:720:336", "RRA:AVERAGE:0.5:17280:365"
        };

        @AttributeDefinition(
                name = "Path",
                description = "Path of the RRD file where metrics are stored. " +
                        "If the path is relative, it is resolved relative to " +
                        "the value of the framework property 'sling.home' when " +
                        "available, otherwise relative to the current working " +
                        "directory."
        )
        String path() default "metrics/metrics.rrd";
    }

    private MetricRegistry metricRegistry = new MetricRegistry();

    @Activate
    void activate(BundleContext context, Configuration config) throws Exception {
        LOG.info("Starting RRD4J Metrics reporter");
        configuration = config;
        rrd = new File(config.path());
        if (!rrd.isAbsolute()) {
            String home = context.getProperty("sling.home");
            if (home != null) {
                rrd = new File(home, rrd.getPath());
            }
        }

        reporter = RRD4JReporter.forRegistry(metricRegistry)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MICROSECONDS)
                .withPath(rrd)
                .withDatasources(config.datasources())
                .withArchives(config.archives())
                .withStep(config.step())
                .build();
        reporter.start(config.step(), TimeUnit.SECONDS);
        LOG.info("Started RRD4J Metrics reporter. Writing to " + rrd);
    }

    @Deactivate
    void deactivate() {
        LOG.info("Stopping RRD4J Metrics reporter");
        reporter.stop();
        reporter = null;
        configuration = null;
        rrd = null;
        LOG.info("Stopped RRD4J Metrics reporter");
    }

    @Reference(
            service = MetricRegistry.class,
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC)
    synchronized void addMetricRegistry(MetricRegistry metricRegistry,
                                        Map<String, Object> properties) {
        String name = (String) properties.get("name");
        if (name == null) {
            name = metricRegistry.toString();
        }
        CopyMetricRegistryListener listener = new CopyMetricRegistryListener(this.metricRegistry, name);
        listener.start(metricRegistry);
        this.listeners.put(name, listener);
        LOG.info("Bound Metrics Registry {} ",name);
    }

    synchronized void removeMetricRegistry(MetricRegistry metricRegistry,
                                           Map<String, Object> properties) {
        String name = (String) properties.get("name");
        if (name == null) {
            name = metricRegistry.toString();
        }
        CopyMetricRegistryListener metricRegistryListener = listeners.get(name);
        if ( metricRegistryListener != null) {
            metricRegistryListener.stop(metricRegistry);
            this.listeners.remove(name);
        }
        LOG.info("Unbound Metrics Registry {} ",name);
    }

    //------------------------< InventoryPrinter >------------------------------

    @Override
    public void print(PrintWriter pw, Format format, boolean isZip) {
        if (format == Format.TEXT) {
            pw.println("Sling Metrics RRD4J reporter");
            pw.println("Path: " + rrd.getAbsolutePath());
            pw.println("Step: " + configuration.step());
            pw.println("Datasources: " + Arrays.asList(configuration.datasources()));
            pw.println("Archives: " + Arrays.asList(configuration.archives()));
        }
    }

    //----------------------< ZipAttachmentProvider >---------------------------

    @Override
    public void addAttachments(ZipOutputStream zos, String namePrefix)
            throws IOException {
        if (rrd.exists()) {
            appendFile(zos, rrd, namePrefix + configuration.path());
        }
        File props = new File(rrd.getParentFile(), rrd.getName() + ".properties");
        if (props.exists()) {
            appendFile(zos, props, namePrefix + configuration.path() + ".properties");
        }
    }

    private void appendFile(ZipOutputStream zos, File file, String name)
            throws IOException {
        ZipEntry entry = new ZipEntry(name);
        entry.setSize(file.length());
        zos.putNextEntry(entry);
        try {
            Files.copy(file.toPath(), zos);
            zos.flush();
        } finally {
            zos.closeEntry();
        }
    }
}
