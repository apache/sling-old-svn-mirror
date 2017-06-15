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

import javax.management.ObjectName;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableMap;
import org.apache.sling.testing.mock.osgi.MockBundle;
import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;

public class BundleMetricsMapperTest {
    @Rule
    public final OsgiContext context = new OsgiContext();
    private MetricRegistry registry = new MetricRegistry();

    private BundleMetricsMapper mapper = new BundleMetricsMapper(registry);

    @Test
    public void defaultDomainName() throws Exception{
        ObjectName name = mapper.createName("counter", "foo", "bar");
        assertEquals("foo", name.getDomain());
    }

    @Test
    public void mappedName_SymbolicName() throws Exception{
        MockBundle bundle = new MockBundle(context.bundleContext());
        bundle.setSymbolicName("com.example");

        mapper.addMapping("bar", bundle);

        ObjectName name = mapper.createName("counter", "foo", "bar");
        assertEquals("com.example", name.getDomain());
    }

    @Test
    public void mappedName_Header() throws Exception{
        MockBundle bundle = new MockBundle(context.bundleContext());
        bundle.setSymbolicName("com.example");
        bundle.setHeaders(ImmutableMap.of(BundleMetricsMapper.HEADER_DOMAIN_NAME, "com.test"));

        mapper.addMapping("bar", bundle);

        ObjectName name = mapper.createName("counter", "foo", "bar");
        assertEquals("com.test", name.getDomain());
    }


}