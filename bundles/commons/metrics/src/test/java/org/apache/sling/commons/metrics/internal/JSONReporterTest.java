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

import java.io.PrintStream;
import java.io.StringWriter;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.JvmAttributeGaugeSet;
import com.codahale.metrics.MetricRegistry;
import org.apache.commons.io.output.WriterOutputStream;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class JSONReporterTest {

    @Test
    public void jsonOutput() throws Exception {
        MetricRegistry registry = new MetricRegistry();
        registry.meter("test1").mark(5);
        registry.timer("test2").time().close();
        registry.histogram("test3").update(743);
        registry.counter("test4").inc(9);
        registry.registerAll(new JvmAttributeGaugeSet());

        JSONObject json = getJSON(registry);

        assertTrue(json.has("meters"));
        assertTrue(json.has("guages"));
        assertTrue(json.has("timers"));
        assertTrue(json.has("counters"));
        assertTrue(json.has("histograms"));
        assertTrue(json.has("meters"));

        assertTrue(json.getJSONObject("meters").has("test1"));
        assertTrue(json.getJSONObject("timers").has("test2"));
        assertTrue(json.getJSONObject("counters").has("test4"));
        assertTrue(json.getJSONObject("histograms").has("test3"));
    }

    @Test
    public void nan_value() throws Exception{
        MetricRegistry registry = new MetricRegistry();

        registry.register("test", new Gauge<Double>() {
            @Override
            public Double getValue() {
                return Double.POSITIVE_INFINITY;
            }
        });


        JSONObject json = getJSON(registry);
        assertTrue(json.getJSONObject("guages").has("test"));
    }

    private static JSONObject getJSON(MetricRegistry registry) throws JSONException {
        StringWriter sw = new StringWriter();
        JSONReporter reporter = JSONReporter.forRegistry(registry)
                .outputTo(new PrintStream(new WriterOutputStream(sw)))
                .build();
        reporter.report();
        reporter.close();
        return new JSONObject(sw.toString());
    }

}