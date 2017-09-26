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

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricRegistryListener;
import com.codahale.metrics.Timer;

class CopyMetricRegistryListener implements MetricRegistryListener {

    private final MetricRegistry parent;
    private final String name;

    CopyMetricRegistryListener(MetricRegistry parent, String name) {
        this.parent = parent;
        this.name = name;
    }

    void start(MetricRegistry metricRegistry) {
        metricRegistry.addListener(this);
    }

    void stop(MetricRegistry metricRegistry) {
        metricRegistry.removeListener(this);
        for(String name : metricRegistry.getMetrics().keySet()) {
            removeMetric(name);
        }
    }

    private void addMetric(String metricName, Metric m) {
        parent.register(getMetricName(metricName), m);
    }

    private void removeMetric(String metricName) {
        parent.remove(getMetricName(metricName));
    }

    private String getMetricName(String metricName) {
        return name + "_" + metricName;
    }

    @Override
    public void onGaugeAdded(String s, Gauge<?> gauge) {
        addMetric(s, gauge);
    }

    @Override
    public void onGaugeRemoved(String s) {
        removeMetric(s);
    }

    @Override
    public void onCounterAdded(String s, Counter counter) {
        addMetric(s, counter);
    }

    @Override
    public void onCounterRemoved(String s) {
        removeMetric(s);
    }

    @Override
    public void onHistogramAdded(String s, Histogram histogram) {
        addMetric(s, histogram);
    }

    @Override
    public void onHistogramRemoved(String s) {
        removeMetric(s);
    }

    @Override
    public void onMeterAdded(String s, Meter meter) {
        addMetric(s, meter);
    }

    @Override
    public void onMeterRemoved(String s) {
        removeMetric(s);
    }

    @Override
    public void onTimerAdded(String s, Timer timer) {
        addMetric(s, timer);
    }

    @Override
    public void onTimerRemoved(String s) {
        removeMetric(s);
    }

}
