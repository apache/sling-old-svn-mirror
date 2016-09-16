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
package org.apache.sling.contextaware.config.impl.def;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.contextaware.config.spi.ConfigurationPersistenceStrategy;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;


public class DefaultConfigurationPersistenceStrategyTest {

    @Rule
    public SlingContext context = new SlingContext();
    
    @Test
    public void testGetResource() {
        ConfigurationPersistenceStrategy underTest = context.registerInjectActivateService(new DefaultConfigurationPersistenceStrategy());
        
        Resource resource = context.create().resource("/conf/test");
        Resource result = underTest.getResource(resource);
        assertSame(resource, result);
    }

    @Test
    public void testPersist() throws Exception {
        ConfigurationPersistenceStrategy underTest = context.registerInjectActivateService(new DefaultConfigurationPersistenceStrategy());
        
        // store config data
        assertTrue(underTest.persist(context.resourceResolver(), "/conf/test",
                ImmutableMap.<String,Object>of("prop1", "value1", "prop2", 5)));
        context.resourceResolver().commit();
        
        ValueMap props = context.resourceResolver().getResource("/conf/test").getValueMap();
        assertEquals("value1", props.get("prop1", String.class));
        assertEquals((Integer)5, props.get("prop2", Integer.class));

        // remove config data
        assertTrue(underTest.persist(context.resourceResolver(), "/conf/test", ImmutableMap.<String,Object>of()));
        context.resourceResolver().commit();

        props = context.resourceResolver().getResource("/conf/test").getValueMap();
        assertNull(props.get("prop1", String.class));
        assertNull(props.get("prop2", Integer.class));
        
    }

    @Test
    public void testPersistCollection() throws Exception {
        ConfigurationPersistenceStrategy underTest = context.registerInjectActivateService(new DefaultConfigurationPersistenceStrategy());
        
        // store new config collection items
        assertTrue(underTest.persistCollection(context.resourceResolver(), "/conf/test", ImmutableList.<Map<String,Object>>of(
                ImmutableMap.<String,Object>of("prop1", "value1"),
                ImmutableMap.<String,Object>of("prop2", 5)
        )));
        context.resourceResolver().commit();
        
        Resource resource = context.resourceResolver().getResource("/conf/test");
        assertEquals(2, ImmutableList.copyOf(resource.getChildren()).size());
        ValueMap props0 = context.resourceResolver().getResource("/conf/test/0").getValueMap();
        assertEquals("value1", props0.get("prop1", String.class));
        ValueMap props1 = context.resourceResolver().getResource("/conf/test/1").getValueMap();
        assertEquals((Integer)5, props1.get("prop2", Integer.class));

        // remove config collection items
        assertTrue(underTest.persistCollection(context.resourceResolver(), "/conf/test", ImmutableList.<Map<String,Object>>of()));
        context.resourceResolver().commit();

        resource = context.resourceResolver().getResource("/conf/test");
        assertEquals(0, ImmutableList.copyOf(resource.getChildren()).size());
    }

    @Test
    public void testDisabled() {
        ConfigurationPersistenceStrategy underTest = context.registerInjectActivateService(new DefaultConfigurationPersistenceStrategy(),
                "enabled", false);
        
        Resource resource = context.create().resource("/conf/test");
        assertNull(underTest.getResource(resource));

        assertFalse(underTest.persist(context.resourceResolver(), "/conf/test", ImmutableMap.<String,Object>of()));
        assertFalse(underTest.persistCollection(context.resourceResolver(), "/conf/test", ImmutableList.<Map<String,Object>>of()));
    }

}
