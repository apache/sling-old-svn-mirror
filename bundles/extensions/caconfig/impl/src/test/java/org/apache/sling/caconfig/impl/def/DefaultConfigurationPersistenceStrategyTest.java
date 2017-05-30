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
package org.apache.sling.caconfig.impl.def;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.caconfig.management.impl.ConfigurationManagementSettingsImpl;
import org.apache.sling.caconfig.spi.ConfigurationCollectionPersistData;
import org.apache.sling.caconfig.spi.ConfigurationPersistData;
import org.apache.sling.caconfig.spi.ConfigurationPersistenceStrategy2;
import org.apache.sling.hamcrest.ResourceMatchers;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class DefaultConfigurationPersistenceStrategyTest {

    @Rule
    public SlingContext context = new SlingContext();
    
    @Before
    public void setUp() {
        context.registerInjectActivateService(new ConfigurationManagementSettingsImpl());
    }
    
    @Test
    public void testGetResource() {
        ConfigurationPersistenceStrategy2 underTest = context.registerInjectActivateService(new DefaultConfigurationPersistenceStrategy());
        
        Resource resource = context.create().resource("/conf/test");
        assertSame(resource, underTest.getResource(resource));
        assertSame(resource, underTest.getCollectionParentResource(resource));
        assertSame(resource, underTest.getCollectionItemResource(resource));
    }

    @Test
    public void testGetResourcePath() {
        ConfigurationPersistenceStrategy2 underTest = context.registerInjectActivateService(new DefaultConfigurationPersistenceStrategy());
        
        String path = "/conf/test";
        assertEquals(path, underTest.getResourcePath(path));
        assertEquals(path, underTest.getCollectionParentResourcePath(path));
        assertEquals(path, underTest.getCollectionItemResourcePath(path));
    }
    
    @Test
    public void testGetConfigName() throws Exception {
        ConfigurationPersistenceStrategy2 underTest = context.registerInjectActivateService(new DefaultConfigurationPersistenceStrategy());
        
        String path = "test";
        assertEquals(path, underTest.getConfigName(path, null));
        assertEquals(path, underTest.getCollectionParentConfigName(path, null));
        assertEquals(path, underTest.getCollectionItemConfigName(path, null));
    }

    @Test
    public void testPersistConfiguration() throws Exception {
        ConfigurationPersistenceStrategy2 underTest = context.registerInjectActivateService(new DefaultConfigurationPersistenceStrategy());
        
        // store config data
        assertTrue(underTest.persistConfiguration(context.resourceResolver(), "/conf/test",
                new ConfigurationPersistData(ImmutableMap.<String,Object>of("prop1", "value1", "prop2", 5))));
        context.resourceResolver().commit();
        
        ValueMap props = context.resourceResolver().getResource("/conf/test").getValueMap();
        assertEquals("value1", props.get("prop1", String.class));
        assertEquals((Integer)5, props.get("prop2", Integer.class));

        // remove config data
        assertTrue(underTest.persistConfiguration(context.resourceResolver(), "/conf/test",
                new ConfigurationPersistData(ImmutableMap.<String,Object>of())));
        context.resourceResolver().commit();

        props = context.resourceResolver().getResource("/conf/test").getValueMap();
        assertNull(props.get("prop1", String.class));
        assertNull(props.get("prop2", Integer.class));
        
        underTest.deleteConfiguration(context.resourceResolver(), "/conf/test");
        assertNull(context.resourceResolver().getResource("/conf/test"));
    }

    @Test
    public void testPersistConfigurationCollection() throws Exception {
        ConfigurationPersistenceStrategy2 underTest = context.registerInjectActivateService(new DefaultConfigurationPersistenceStrategy());
        
        // store new config collection items
        assertTrue(underTest.persistConfigurationCollection(context.resourceResolver(), "/conf/test",
                new ConfigurationCollectionPersistData(ImmutableList.of(
                new ConfigurationPersistData(ImmutableMap.<String,Object>of("prop1", "value1")).collectionItemName("0"),
                new ConfigurationPersistData(ImmutableMap.<String,Object>of("prop2", 5)).collectionItemName("1"))
                ).properties(ImmutableMap.<String, Object>of("prop1", "abc", "sling:resourceType", "/a/b/c"))
        ));
        context.resourceResolver().commit();
        
        Resource resource = context.resourceResolver().getResource("/conf/test");
        assertEquals(2, ImmutableList.copyOf(resource.getChildren()).size());
        ValueMap props0 = context.resourceResolver().getResource("/conf/test/0").getValueMap();
        assertEquals("value1", props0.get("prop1", String.class));
        ValueMap props1 = context.resourceResolver().getResource("/conf/test/1").getValueMap();
        assertEquals((Integer)5, props1.get("prop2", Integer.class));
        
        assertThat(resource, ResourceMatchers.props(
                "prop1", "abc",
                "sling:resourceType", "/a/b/c"));

        // remove config collection items
        assertTrue(underTest.persistConfigurationCollection(context.resourceResolver(), "/conf/test",
                new ConfigurationCollectionPersistData(ImmutableList.<ConfigurationPersistData>of())));
        context.resourceResolver().commit();

        resource = context.resourceResolver().getResource("/conf/test");
        assertEquals(0, ImmutableList.copyOf(resource.getChildren()).size());

        underTest.deleteConfiguration(context.resourceResolver(), "/conf/test");
        assertNull(context.resourceResolver().getResource("/conf/test"));
    }

    @Test
    public void testDisabled() {
        ConfigurationPersistenceStrategy2 underTest = context.registerInjectActivateService(new DefaultConfigurationPersistenceStrategy(),
                "enabled", false);
        
        Resource resource = context.create().resource("/conf/test");
        assertNull(underTest.getResource(resource));
        assertNull(underTest.getResourcePath(resource.getPath()));

        assertFalse(underTest.persistConfiguration(context.resourceResolver(), "/conf/test",
                new ConfigurationPersistData(ImmutableMap.<String,Object>of())));
        assertFalse(underTest.persistConfigurationCollection(context.resourceResolver(), "/conf/test",
                new ConfigurationCollectionPersistData(ImmutableList.<ConfigurationPersistData>of())));
        assertFalse(underTest.deleteConfiguration(context.resourceResolver(), "/conf/test"));
    }

}
