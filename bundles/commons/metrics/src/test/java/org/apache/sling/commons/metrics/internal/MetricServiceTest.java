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

import java.lang.management.ManagementFactory;
import java.util.Collections;

import javax.management.MBeanServer;

import com.codahale.metrics.MetricRegistry;
import org.apache.sling.commons.metrics.Counter;
import org.apache.sling.commons.metrics.Histogram;
import org.apache.sling.commons.metrics.Meter;
import org.apache.sling.commons.metrics.MetricsService;
import org.apache.sling.commons.metrics.Timer;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class MetricServiceTest {
    @Rule
    public final OsgiContext context = new OsgiContext();

    private MetricsServiceImpl service = new MetricsServiceImpl();

    @After
    public void registerMBeanServer() {
        context.registerService(MBeanServer.class, ManagementFactory.getPlatformMBeanServer());
    }

    @Test
    public void defaultSetup() throws Exception{
        activate();

        assertNotNull(context.getService(MetricRegistry.class));
        assertNotNull(context.getService(MetricsService.class));

        MockOsgi.deactivate(service);

        assertNull(context.getService(MetricRegistry.class));
        assertNull(context.getService(MetricsService.class));
    }

    @Test
    public void meter() throws Exception{
        activate();
        Meter meter = service.meter("test");

        assertNotNull(meter);
        assertTrue(getRegistry().getMeters().containsKey("test"));

        assertSame(meter, service.meter("test"));
    }

    @Test
    public void counter() throws Exception{
        activate();
        Counter counter = service.counter("test");

        assertNotNull(counter);
        assertTrue(getRegistry().getCounters().containsKey("test"));

        assertSame(counter, service.counter("test"));
    }

    @Test
    public void timer() throws Exception{
        activate();
        Timer timer = service.timer("test");

        assertNotNull(timer);
        assertTrue(getRegistry().getTimers().containsKey("test"));

        assertSame(timer, service.timer("test"));
    }

    @Test
    public void histogram() throws Exception{
        activate();
        Histogram histo = service.histogram("test");

        assertNotNull(histo);
        assertTrue(getRegistry().getHistograms().containsKey("test"));

        assertSame(histo, service.histogram("test"));
    }

    private MetricRegistry getRegistry(){
        return context.getService(MetricRegistry.class);
    }

    private void activate() {
        MockOsgi.activate(service, context.bundleContext(), Collections.<String, Object>emptyMap());
    }

}
