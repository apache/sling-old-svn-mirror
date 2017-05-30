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
package org.apache.sling.testing.mock.caconfig;

import static org.apache.sling.testing.mock.caconfig.ContextPlugins.CACONFIG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.caconfig.ConfigurationBuilder;
import org.apache.sling.testing.mock.caconfig.example.ListConfig;
import org.apache.sling.testing.mock.caconfig.example.SimpleConfig;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.mock.sling.junit.SlingContextBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class MockContextAwareConfigTest {

    @Rule
    public SlingContext context = new SlingContextBuilder()
            .plugin(CACONFIG)
            .build();

    @Before
    public void setUp() {
        MockContextAwareConfig.registerAnnotationPackages(context, "org.apache.sling.testing.mock.caconfig.example");

        context.create().resource("/content/region/site", "sling:configRef", "/conf/region/site");

        context.currentResource(context.create().resource("/content/region/site/en"));

        MockContextAwareConfig.writeConfiguration(context, "/content/region/site", SimpleConfig.class,
                "stringParam", "value1");

        MockContextAwareConfig.writeConfigurationCollection(context, "/content/region/site", ListConfig.class,
                ImmutableList.of((Map<String, Object>) ImmutableMap.<String, Object> of("stringParam", "value1"),
                        (Map<String, Object>) ImmutableMap.<String, Object> of("stringParam", "value2")));
    }

    @Test
    public void testSingletonConfig() {
        Resource resource = context.request().getResource();
        SimpleConfig config = resource.adaptTo(ConfigurationBuilder.class).as(SimpleConfig.class);
        assertNotNull(config);
        assertEquals("value1", config.stringParam());
        assertEquals(5, config.intParam());
    }

    @Test
    public void testConfigCollection() {
        Resource resource = context.request().getResource();
        Collection<ListConfig> config = resource.adaptTo(ConfigurationBuilder.class).asCollection(ListConfig.class);
        assertEquals(2, config.size());
        Iterator<ListConfig> items = config.iterator();

        ListConfig item1 = items.next();
        assertEquals("value1", item1.stringParam());
        assertEquals(5, item1.intParam());

        ListConfig item2 = items.next();
        assertEquals("value2", item2.stringParam());
        assertEquals(5, item2.intParam());
    }

}
