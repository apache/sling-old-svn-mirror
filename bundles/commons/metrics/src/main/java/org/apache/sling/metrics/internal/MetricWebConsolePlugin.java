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

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
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
@Service(value = {InventoryPrinter.class, Servlet.class})
@Properties({
        @Property(name = "felix.webconsole.label", value = "slingmetrics"),
        @Property(name = "felix.webconsole.title", value = "Metrics"),
        @Property(name = "felix.webconsole.category", value = "Sling"),
        @Property(name = InventoryPrinter.FORMAT, value = {"TEXT" }),
        @Property(name = InventoryPrinter.NAME, value = "slingmetrics"),
        @Property(name = InventoryPrinter.TITLE, value = "Sling Metrics"),
        @Property(name = InventoryPrinter.WEBCONSOLE, boolValue = true)
})
public class MetricWebConsolePlugin extends HttpServlet implements
        InventoryPrinter, ServiceTrackerCustomizer<MetricRegistry, MetricRegistry>{
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

    private TimeUnit rateUnit = TimeUnit.SECONDS;
    private TimeUnit durationUnit = TimeUnit.MILLISECONDS;
    private Map<String, TimeUnit> specificDurationUnits = Collections.emptyMap();
    private Map<String, TimeUnit> specificRateUnits = Collections.emptyMap();
    private MetricTimeUnits timeUnit;

    @Activate
    private void activate(BundleContext context){
        this.context = context;
        this.timeUnit = new MetricTimeUnits(rateUnit, durationUnit, specificRateUnits, specificDurationUnits);
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

    //~----------------------------------------------< Servlet >

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        final PrintWriter pw = resp.getWriter();
        MetricRegistry registry = getConsolidatedRegistry();

        appendMetricStatus(pw, registry);
        addCounterDetails(pw, registry.getCounters());
        addGaugeDetails(pw, registry.getGauges());
        addMeterDetails(pw, registry.getMeters());
        addTimerDetails(pw, registry.getTimers());
        addHistogramDetails(pw, registry.getHistograms());
    }

    private static void appendMetricStatus(PrintWriter pw, MetricRegistry registry) {
        pw.printf(
                "<p class='statline'>Metrics: %d gauges, %d timers, %d meters, %d counters, %d histograms</p>%n",
                registry.getGauges().size(),
                registry.getTimers().size(),
                registry.getMeters().size(),
                registry.getCounters().size(),
                registry.getHistograms().size());
    }

    private void addMeterDetails(PrintWriter pw, SortedMap<String, Meter> meters) {
        if (meters.isEmpty()) {
            return;
        }
        pw.println("<br>");
        pw.println("<div class='table'>");
        pw.println("<div class='ui-widget-header ui-corner-top buttonGroup'>Meters</div>");
        pw.println("<table class='nicetable'>");
        pw.println("<thead>");
        pw.println("<tr>");
        pw.println("<th class='header'>Name</th>");
        pw.println("<th class='header'>Count</th>");
        pw.println("<th class='header'>Mean Rate</th>");
        pw.println("<th class='header'>OneMinuteRate</th>");
        pw.println("<th class='header'>FiveMinuteRate</th>");
        pw.println("<th class='header'>FifteenMinuteRate</ th>");
        pw.println("<th>RateUnit</th>");
        pw.println("</tr>");
        pw.println("</thead>");
        pw.println("<tbody>");

        String rowClass = "odd";
        for (Map.Entry<String, Meter> e : meters.entrySet()) {
            Meter m = e.getValue();
            String name = e.getKey();

            double rateFactor = timeUnit.rateFor(name).toSeconds(1);
            String rateUnit = "events/" + calculateRateUnit(timeUnit.rateFor(name));
            pw.printf("<tr class='%s ui-state-default'>%n", rowClass);

            pw.printf("<td>%s</td>", name);
            pw.printf("<td>%d</td>", m.getCount());
            pw.printf("<td>%f</td>", m.getMeanRate() * rateFactor);
            pw.printf("<td>%f</td>", m.getOneMinuteRate() * rateFactor);
            pw.printf("<td>%f</td>", m.getFiveMinuteRate() * rateFactor);
            pw.printf("<td>%f</td>", m.getFifteenMinuteRate() * rateFactor);
            pw.printf("<td>%s</td>", rateUnit);

            pw.println("</tr>");
            rowClass = "odd".equals(rowClass) ? "even" : "odd";
        }

        pw.println("</tbody>");
        pw.println("</table>");
        pw.println("</div>");
    }

    private void addTimerDetails(PrintWriter pw, SortedMap<String, Timer> timers) {
        if (timers.isEmpty()) {
            return;
        }

        pw.println("<br>");
        pw.println("<div class='table'>");
        pw.println("<div class='ui-widget-header ui-corner-top buttonGroup'>Timers</div>");
        pw.println("<table class='nicetable'>");
        pw.println("<thead>");
        pw.println("<tr>");
        pw.println("<th class='header'>Name</th>");
        pw.println("<th class='header'>Count</th>");
        pw.println("<th class='header'>Mean Rate</th>");
        pw.println("<th class='header'>1 min rate</th>");
        pw.println("<th class='header'>5 mins rate</th>");
        pw.println("<th class='header'>15 mins rate</th>");
        pw.println("<th class='header'>50%</th>");
        pw.println("<th class='header'>Min</th>");
        pw.println("<th class='header'>Max</th>");
        pw.println("<th class='header'>Mean</th>");
        pw.println("<th class='header'>StdDev</th>");
        pw.println("<th class='header'>75%</th>");
        pw.println("<th class='header'>95%</th>");
        pw.println("<th class='header'>98%</th>");
        pw.println("<th class='header'>99%</th>");
        pw.println("<th class='header'>999%</th>");
        pw.println("<th>Rate Unit</th>");
        pw.println("<th>Duration Unit</th>");
        pw.println("</tr>");
        pw.println("</thead>");
        pw.println("<tbody>");

        String rowClass = "odd";
        for (Map.Entry<String, Timer> e : timers.entrySet()) {
            Timer t = e.getValue();
            Snapshot s = t.getSnapshot();
            String name = e.getKey();

            double rateFactor = timeUnit.rateFor(name).toSeconds(1);
            String rateUnit = "events/" + calculateRateUnit(timeUnit.rateFor(name));

            double durationFactor = 1.0 / timeUnit.durationFor(name).toNanos(1);
            String durationUnit = timeUnit.durationFor(name).toString().toLowerCase(Locale.US);

            pw.printf("<tr class='%s ui-state-default'>%n", rowClass);

            pw.printf("<td>%s</td>", name);
            pw.printf("<td>%d</td>", t.getCount());
            pw.printf("<td>%f</td>", t.getMeanRate() * rateFactor);
            pw.printf("<td>%f</td>", t.getOneMinuteRate() * rateFactor);
            pw.printf("<td>%f</td>", t.getFiveMinuteRate() * rateFactor);
            pw.printf("<td>%f</td>", t.getFifteenMinuteRate() * rateFactor);

            pw.printf("<td>%f</td>", s.getMedian() * durationFactor);
            pw.printf("<td>%f</td>", s.getMin() * durationFactor);
            pw.printf("<td>%f</td>", s.getMax() * durationFactor);
            pw.printf("<td>%f</td>", s.getMean() * durationFactor);
            pw.printf("<td>%f</td>", s.getStdDev() * durationFactor);

            pw.printf("<td>%f</td>", s.get75thPercentile() * durationFactor);
            pw.printf("<td>%f</td>", s.get95thPercentile() * durationFactor);
            pw.printf("<td>%f</td>", s.get98thPercentile() * durationFactor);
            pw.printf("<td>%f</td>", s.get99thPercentile() * durationFactor);
            pw.printf("<td>%f</td>", s.get999thPercentile() * durationFactor);

            pw.printf("<td>%s</td>", rateUnit);
            pw.printf("<td>%s</td>", durationUnit);

            pw.println("</tr>");
            rowClass = "odd".equals(rowClass) ? "even" : "odd";
        }

        pw.println("</tbody>");
        pw.println("</table>");
        pw.println("</div>");
    }

    private void addHistogramDetails(PrintWriter pw, SortedMap<String, Histogram> histograms) {
        if (histograms.isEmpty()) {
            return;
        }

        pw.println("<br>");
        pw.println("<div class='table'>");
        pw.println("<div class='ui-widget-header ui-corner-top buttonGroup'>Histograms</div>");
        pw.println("<table class='nicetable'>");
        pw.println("<thead>");
        pw.println("<tr>");
        pw.println("<th class='header'>Name</th>");
        pw.println("<th class='header'>Count</th>");
        pw.println("<th class='header'>50%</th>");
        pw.println("<th class='header'>Min</th>");
        pw.println("<th class='header'>Max</th>");
        pw.println("<th class='header'>Mean</th>");
        pw.println("<th class='header'>StdDev</th>");
        pw.println("<th class='header'>75%</th>");
        pw.println("<th class='header'>95%</th>");
        pw.println("<th class='header'>98%</th>");
        pw.println("<th class='header'>99%</th>");
        pw.println("<th class='header'>999%</th>");
        pw.println("<th>Duration Unit</th>");
        pw.println("</tr>");
        pw.println("</thead>");
        pw.println("<tbody>");

        String rowClass = "odd";
        for (Map.Entry<String, Histogram> e : histograms.entrySet()) {
            Histogram h = e.getValue();
            Snapshot s = h.getSnapshot();
            String name = e.getKey();

            double durationFactor = 1.0 / timeUnit.durationFor(name).toNanos(1);
            String durationUnit = timeUnit.durationFor(name).toString().toLowerCase(Locale.US);
            pw.printf("<tr class='%s ui-state-default'>%n", rowClass);

            pw.printf("<td>%s</td>", name);
            pw.printf("<td>%d</td>", h.getCount());
            pw.printf("<td>%f</td>", s.getMedian() * durationFactor);
            pw.printf("<td>%f</td>", s.getMin() * durationFactor);
            pw.printf("<td>%f</td>", s.getMax() * durationFactor);
            pw.printf("<td>%f</td>", s.getMean() * durationFactor);
            pw.printf("<td>%f</td>", s.getStdDev() * durationFactor);

            pw.printf("<td>%f</td>", s.get75thPercentile() * durationFactor);
            pw.printf("<td>%f</td>", s.get95thPercentile() * durationFactor);
            pw.printf("<td>%f</td>", s.get98thPercentile() * durationFactor);
            pw.printf("<td>%f</td>", s.get99thPercentile() * durationFactor);
            pw.printf("<td>%f</td>", s.get999thPercentile() * durationFactor);

            pw.printf("<td>%s</td>", durationUnit);

            pw.println("</tr>");
            rowClass = "odd".equals(rowClass) ? "even" : "odd";
        }

        pw.println("</tbody>");
        pw.println("</table>");
        pw.println("</div>");
    }

    private void addCounterDetails(PrintWriter pw, SortedMap<String, Counter> counters) {
        if (counters.isEmpty()) {
            return;
        }
        pw.println("<br>");
        pw.println("<div class='table'>");
        pw.println("<div class='ui-widget-header ui-corner-top buttonGroup'>Counters</div>");
        pw.println("<table class='nicetable'>");
        pw.println("<thead>");
        pw.println("<tr>");
        pw.println("<th class='header'>Name</th>");
        pw.println("<th class='header'>Count</th>");
        pw.println("</tr>");
        pw.println("</thead>");
        pw.println("<tbody>");

        String rowClass = "odd";
        for (Map.Entry<String, Counter> e : counters.entrySet()) {
            Counter c = e.getValue();
            String name = e.getKey();

            pw.printf("<tr class='%s ui-state-default'>%n", rowClass);

            pw.printf("<td>%s</td>", name);
            pw.printf("<td>%d</td>", c.getCount());

            pw.println("</tr>");
            rowClass = "odd".equals(rowClass) ? "even" : "odd";
        }

        pw.println("</tbody>");
        pw.println("</table>");
        pw.println("</div>");
    }

    private void addGaugeDetails(PrintWriter pw, SortedMap<String, Gauge> gauges) {
        if (gauges.isEmpty()) {
            return;
        }

        pw.println("<br>");
        pw.println("<div class='table'>");
        pw.println("<div class='ui-widget-header ui-corner-top buttonGroup'>Guages</div>");
        pw.println("<table class='nicetable'>");
        pw.println("<thead>");
        pw.println("<tr>");
        pw.println("<th class='header'>Name</th>");
        pw.println("<th class='header'>Value</th>");
        pw.println("</tr>");
        pw.println("</thead>");
        pw.println("<tbody>");

        String rowClass = "odd";
        for (Map.Entry<String, Gauge> e : gauges.entrySet()) {
            Gauge g = e.getValue();
            String name = e.getKey();

            pw.printf("<tr class='%s ui-state-default'>%n", rowClass);

            pw.printf("<td>%s</td>", name);
            pw.printf("<td>%s</td>", g.getValue());

            pw.println("</tr>");
            rowClass = "odd".equals(rowClass) ? "even" : "odd";
        }

        pw.println("</tbody>");
        pw.println("</table>");
        pw.println("</div>");
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

    private static String calculateRateUnit(TimeUnit unit) {
        final String s = unit.toString().toLowerCase(Locale.US);
        return s.substring(0, s.length() - 1);
    }

    private static class MetricTimeUnits {
        private final TimeUnit defaultRate;
        private final TimeUnit defaultDuration;
        private final Map<String, TimeUnit> rateOverrides;
        private final Map<String, TimeUnit> durationOverrides;

        MetricTimeUnits(TimeUnit defaultRate,
                        TimeUnit defaultDuration,
                        Map<String, TimeUnit> rateOverrides,
                        Map<String, TimeUnit> durationOverrides) {
            this.defaultRate = defaultRate;
            this.defaultDuration = defaultDuration;
            this.rateOverrides = rateOverrides;
            this.durationOverrides = durationOverrides;
        }

        public TimeUnit durationFor(String name) {
            return durationOverrides.containsKey(name) ? durationOverrides.get(name) : defaultDuration;
        }

        public TimeUnit rateFor(String name) {
            return rateOverrides.containsKey(name) ? rateOverrides.get(name) : defaultRate;
        }
    }
}
