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

import java.util.Collections;

import javax.management.ObjectName;

import com.codahale.metrics.MetricRegistry;
import org.apache.sling.commons.metrics.MetricsService;
import org.apache.sling.testing.mock.osgi.MockBundle;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class MetricsServiceFactoryTest {
    @Rule
    public final OsgiContext context = new OsgiContext();
    private MetricsServiceImpl serviceImpl = new MetricsServiceImpl();
    private MetricRegistry registry = serviceImpl.getRegistry();
    private BundleMetricsMapper mapper = new BundleMetricsMapper(registry);
    private MetricsServiceFactory srvFactory = new MetricsServiceFactory(serviceImpl, mapper);
    private ServiceRegistration<MetricsService> reg = mock(ServiceRegistration.class);

    @Test
    public void basicWorking() throws Exception{
        MetricsService service = srvFactory.getService(cb("foo"), reg);
        service.meter("m1");
        service.timer("t1");
        service.histogram("h1");
        service.counter("c1");
        assertTrue(registry.getMeters().containsKey("m1"));
        assertTrue(registry.getTimers().containsKey("t1"));
        assertTrue(registry.getHistograms().containsKey("h1"));
        assertTrue(registry.getCounters().containsKey("c1"));

        ObjectName name = mapper.createName("meter", "com.foo", "m1");

        //Domain name should be bundle symbolic name
        assertEquals("foo", name.getDomain());
    }

    @Test
    public void unRegistration() throws Exception{
        Bundle foo = cb("foo");
        Bundle bar = cb("bar");
        MetricsService srv1 = srvFactory.getService(foo, reg);
        MetricsService srv2 = srvFactory.getService(bar, reg);

        srv1.meter("m1");
        srv1.counter("c1");

        srv2.meter("m2");
        assertTrue(registry.getMeters().containsKey("m1"));
        assertTrue(registry.getMeters().containsKey("m2"));
        assertTrue(registry.getCounters().containsKey("c1"));

        srvFactory.ungetService(foo, reg, srv1);

        //Metrics from 'foo' bundle i.e. m1 and c1 must be removed
        assertFalse(registry.getMeters().containsKey("m1"));
        assertFalse(registry.getCounters().containsKey("c1"));

        //Metrics from 'bar' bundle should be present
        assertTrue(registry.getMeters().containsKey("m2"));
    }

    private Bundle cb(String name){
        MockBundle bundle = new MockBundle(context.bundleContext());
        bundle.setSymbolicName(name);
        return bundle;
    }


}