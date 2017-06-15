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

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;
import java.util.Map;

import org.apache.commons.io.output.WriterOutputStream;
import org.apache.felix.utils.json.JSONParser;
import org.junit.Test;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.JvmAttributeGaugeSet;
import com.codahale.metrics.MetricRegistry;

public class JSONReporterTest {

    @SuppressWarnings("unchecked")
    @Test
    public void jsonOutput() throws Exception {
        MetricRegistry registry = new MetricRegistry();
        registry.meter("test1").mark(5);
        registry.timer("test2").time().close();
        registry.histogram("test3").update(743);
        registry.counter("test4").inc(9);
        registry.registerAll(new JvmAttributeGaugeSet());

        Map<String, Object> json = getJSON(registry);

        assertTrue(json.containsKey("meters"));
        assertTrue(json.containsKey("gauges"));
        assertTrue(json.containsKey("timers"));
        assertTrue(json.containsKey("counters"));
        assertTrue(json.containsKey("histograms"));
        assertTrue(json.containsKey("meters"));

        assertTrue(((Map<String, Object>)json.get("meters")).containsKey("test1"));
        assertTrue(((Map<String, Object>)json.get("timers")).containsKey("test2"));
        assertTrue(((Map<String, Object>)json.get("counters")).containsKey("test4"));
        assertTrue(((Map<String, Object>)json.get("histograms")).containsKey("test3"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void nan_value() throws Exception{
        MetricRegistry registry = new MetricRegistry();

        registry.register("test", new Gauge<Double>() {
            @Override
            public Double getValue() {
                return Double.POSITIVE_INFINITY;
            }
        });


        Map<String, Object> json = getJSON(registry);
        assertTrue(((Map<String, Object>)json.get("gauges")).containsKey("test"));
    }

    private static Map<String, Object> getJSON(MetricRegistry registry) throws IOException {
        StringWriter sw = new StringWriter();
        JSONReporter reporter = JSONReporter.forRegistry(registry)
                .outputTo(new PrintStream(new WriterOutputStream(sw)))
                .build();
        reporter.report();
        reporter.close();
        return new JSONParser(sw.toString()).getParsed();
    }

}
