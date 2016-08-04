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
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Reporter;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import org.json.JSONException;
import org.json.JSONWriter;

class JSONReporter implements Reporter, Closeable {

    public static JSONReporter.Builder forRegistry(MetricRegistry registry) {
        return new JSONReporter.Builder(registry);
    }

    public static class Builder {
        private final MetricRegistry registry;
        private PrintStream output;
        private MetricFilter filter;
        private TimeUnit rateUnit;
        private TimeUnit durationUnit;

        private Builder(MetricRegistry registry) {
            this.registry = registry;
            this.output = System.out;
            this.filter = MetricFilter.ALL;
            this.rateUnit = TimeUnit.SECONDS;
            this.durationUnit = TimeUnit.MILLISECONDS;
        }

        /**
         * Write to the given {@link PrintStream}.
         *
         * @param output a {@link PrintStream} instance.
         * @return {@code this}
         */
        public Builder outputTo(PrintStream output) {
            this.output = output;
            return this;
        }

        /**
         * Convert rates to the given time unit.
         *
         * @param rateUnit a unit of time
         * @return {@code this}
         */
        public Builder convertRatesTo(TimeUnit rateUnit) {
            this.rateUnit = rateUnit;
            return this;
        }

        /**
         * Convert durations to the given time unit.
         *
         * @param durationUnit a unit of time
         * @return {@code this}
         */
        public Builder convertDurationsTo(TimeUnit durationUnit) {
            this.durationUnit = durationUnit;
            return this;
        }

        /**
         * Only report metrics which match the given filter.
         *
         * @param filter a {@link MetricFilter}
         * @return {@code this}
         */
        public Builder filter(MetricFilter filter) {
            this.filter = filter;
            return this;
        }

        /**
         * Builds a {@link ConsoleReporter} with the given properties.
         *
         * @return a {@link ConsoleReporter}
         */
        public JSONReporter build() {
            return new JSONReporter(registry,
                    output,
                    rateUnit,
                    durationUnit,
                    filter);
        }
    }

    private final MetricRegistry registry;
    private final MetricFilter filter;
    private final double durationFactor;
    private final String durationUnit;
    private final double rateFactor;
    private final String rateUnit;
    private final JSONWriter json;
    private final PrintWriter pw;

    private JSONReporter(MetricRegistry registry,
                         PrintStream output, TimeUnit rateUnit, TimeUnit durationUnit, MetricFilter filter){
        this.registry = registry;
        this.filter = filter;
        this.pw = new PrintWriter(output);
        this.json = new JSONWriter(pw);
        this.rateFactor = rateUnit.toSeconds(1);
        this.rateUnit = calculateRateUnit(rateUnit);
        this.durationFactor = 1.0 / durationUnit.toNanos(1);
        this.durationUnit = durationUnit.toString().toLowerCase(Locale.US);
    }

    public void report() {
        try {
            report(registry.getGauges(filter),
                    registry.getCounters(filter),
                    registry.getHistograms(filter),
                    registry.getMeters(filter),
                    registry.getTimers(filter));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close(){
        pw.close();
    }

    private void report(SortedMap<String, Gauge> gauges, SortedMap<String, Counter> counters,
                        SortedMap<String, Histogram> histograms, SortedMap<String, Meter> meters,
                        SortedMap<String, Timer> timers) throws JSONException {
        json.object();
        if (!gauges.isEmpty()) {
            json.key("guages").object();
            for (Map.Entry<String, Gauge> entry : gauges.entrySet()) {
                printGauge(entry);
            }
            json.endObject();
        }

        if (!counters.isEmpty()) {
            json.key("counters").object();
            for (Map.Entry<String, Counter> entry : counters.entrySet()) {
                printCounter(entry);
            }
            json.endObject();
        }

        if (!histograms.isEmpty()) {
            json.key("histograms").object();
            for (Map.Entry<String, Histogram> entry : histograms.entrySet()) {
                printHistogram(entry);
            }
            json.endObject();
        }

        if (!meters.isEmpty()) {
            json.key("meters").object();
            for (Map.Entry<String, Meter> entry : meters.entrySet()) {
                printMeter(entry);
            }
            json.endObject();
        }

        if (!timers.isEmpty()) {
            json.key("timers").object();
            for (Map.Entry<String, Timer> entry : timers.entrySet()) {
                printTimer(entry);
            }
            json.endObject();
        }

        json.endObject();

    }

    private void printTimer(Map.Entry<String, Timer> e) throws JSONException {
        json.key(e.getKey()).object();
        Timer timer = e.getValue();
        Snapshot snapshot = timer.getSnapshot();

        json.key("count").value(timer.getCount());
        json.key("max").value(snapshot.getMax() * durationFactor);
        json.key("mean").value(snapshot.getMean() * durationFactor);
        json.key("min").value(snapshot.getMin() * durationFactor);

        json.key("p50").value(snapshot.getMedian() * durationFactor);
        json.key("p75").value(snapshot.get75thPercentile() * durationFactor);
        json.key("p95").value(snapshot.get95thPercentile() * durationFactor);
        json.key("p98").value(snapshot.get98thPercentile() * durationFactor);
        json.key("p99").value(snapshot.get99thPercentile() * durationFactor);
        json.key("p999").value(snapshot.get999thPercentile() * durationFactor);

        json.key("stddev").value(snapshot.getStdDev() * durationFactor);
        json.key("m1_rate").value(timer.getOneMinuteRate() * rateFactor);
        json.key("m5_rate").value(timer.getFiveMinuteRate() * rateFactor);
        json.key("m15_rate").value(timer.getFifteenMinuteRate() * rateFactor);
        json.key("mean_rate").value(timer.getMeanRate() * rateFactor);
        json.key("duration_units").value(durationUnit);
        json.key("rate_units").value(rateUnit);

        json.endObject();
    }

    private void printMeter(Map.Entry<String, Meter> e) throws JSONException {
        json.key(e.getKey()).object();
        Meter meter = e.getValue();
        json.key("count").value(e.getValue().getCount());
        json.key("m1_rate").value(meter.getOneMinuteRate() * rateFactor);
        json.key("m5_rate").value(meter.getFiveMinuteRate() * rateFactor);
        json.key("m15_rate").value(meter.getFifteenMinuteRate() * rateFactor);
        json.key("mean_rate").value(meter.getMeanRate() * rateFactor);
        json.key("units").value(rateUnit);
        json.endObject();
    }

    private void printHistogram(Map.Entry<String, Histogram> e) throws JSONException {
        json.key(e.getKey()).object();
        json.key("count").value(e.getValue().getCount());

        Snapshot snapshot = e.getValue().getSnapshot();
        json.key("max").value(snapshot.getMax());
        json.key("mean").value(snapshot.getMean());
        json.key("min").value(snapshot.getMin());
        json.key("p50").value(snapshot.getMedian());
        json.key("p75").value(snapshot.get75thPercentile());
        json.key("p95").value(snapshot.get95thPercentile());
        json.key("p98").value(snapshot.get98thPercentile());
        json.key("p99").value(snapshot.get99thPercentile());
        json.key("p999").value(snapshot.get999thPercentile());
        json.key("stddev").value(snapshot.getStdDev());

        json.endObject();
    }

    private void printCounter(Map.Entry<String, Counter> e) throws JSONException {
        json.key(e.getKey()).object();
        json.key("count").value(e.getValue().getCount());
        json.endObject();
    }

    private void printGauge(Map.Entry<String, Gauge> e) throws JSONException {
        json.key(e.getKey()).object();
        Object v = e.getValue().getValue();
        json.key("value").value(jsonSafeValue(v));
        json.endObject();
    }

    private static Object jsonSafeValue(Object v){
        //Json does not allow NaN or infinite doubles. So take care of that
        if (v instanceof Number){
            if (v instanceof Double){
                Double d = (Double) v;
                if (d.isInfinite() || d.isNaN()){
                    return d.toString();
                }
            }
        }
        return v;
    }

    private static String calculateRateUnit(TimeUnit unit) {
        final String s = unit.toString().toLowerCase(Locale.US);
        return s.substring(0, s.length() - 1);
    }

}
