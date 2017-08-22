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

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;

import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.rrd4j.core.RrdDb;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ReporterTest {

    private static final File RRD = new File(new File("target", "metrics"), "metrics.rrd");
    private static final File RRD_0 = new File(new File("target", "metrics"), "metrics.rrd.0");

    private static final long TEST_VALUE = 42;

    @Rule
    public final OsgiContext context = new OsgiContext();

    private MetricRegistry registry = new MetricRegistry();

    private CodahaleMetricsReporter reporter = new CodahaleMetricsReporter();

    @Before
    public void before() throws Exception {
        RRD.delete();
        RRD_0.delete();
        context.registerService(MetricRegistry.class, registry, "name", "sling");

        Map<String, Object> properties = newConfig();
        context.registerInjectActivateService(reporter, properties);

        registry.register("myMetric", new TestGauge(TEST_VALUE));
    }

    @Test
    public void writeRRD() throws Exception {
        assertTrue(RRD.exists());
        for (int i = 0; i < 10; i++) {
            RrdDb db = new RrdDb(RRD.getPath(), true);
            try {
                double lastValue = db.getDatasource("0").getLastValue();
                if (lastValue == (double) TEST_VALUE) {
                    return;
                }
            } finally {
                db.close();
            }
            Thread.sleep(1000);
        }
        fail("RRD4J reporter did not update database in time");
    }

    @Test
    public void reconfigure() throws Exception {
        assertFalse(RRD_0.exists());
        MockOsgi.deactivate(reporter, context.bundleContext());

        // re-activate with changed configuration
        Map<String, Object> properties = newConfig();
        properties.put("step", 5L);
        MockOsgi.activate(reporter, context.bundleContext(), properties);

        assertTrue(RRD_0.exists());
    }

    private static final class TestGauge implements Gauge<Long> {

        private final long value;

        TestGauge(long value) {
            this.value = value;
        }

        @Override
        public Long getValue() {
            return value;
        }
    }

    private static Map<String, Object> newConfig() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("step", 1L);
        properties.put("datasources", new String[]{"DS:sling_myMetric:GAUGE:300:0:U"});
        properties.put("archives", new String[]{"RRA:AVERAGE:0.5:1:60"});
        properties.put("path", RRD.getPath());
        return properties;
    }
}
