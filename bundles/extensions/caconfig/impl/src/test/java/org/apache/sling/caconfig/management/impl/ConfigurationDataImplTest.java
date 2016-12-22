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
package org.apache.sling.caconfig.management.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.caconfig.impl.def.DefaultConfigurationPersistenceStrategy;
import org.apache.sling.caconfig.impl.override.ConfigurationOverrideManager;
import org.apache.sling.caconfig.management.ConfigurationData;
import org.apache.sling.caconfig.management.ConfigurationManager;
import org.apache.sling.caconfig.management.ValueInfo;
import org.apache.sling.caconfig.spi.ConfigurationPersistenceStrategy;
import org.apache.sling.caconfig.spi.metadata.ConfigurationMetadata;
import org.apache.sling.caconfig.spi.metadata.PropertyMetadata;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationDataImplTest {
    
    @Rule
    public SlingContext context = new SlingContext();
    @Mock
    private Resource contextResource;
    @Mock
    private ConfigurationOverrideManager configurationOverrideManager;
    @Mock
    private ConfigurationManager configurationManager;
    private ConfigurationPersistenceStrategy configurationPersistenceStrategy;
    
    private Resource configResource;
    private ConfigurationMetadata configMetadata;
    
    @Before
    public void setUp() {
        configurationPersistenceStrategy = context.registerInjectActivateService(new DefaultConfigurationPersistenceStrategy());
        
        configResource = context.create().resource("/conf/test",
                "prop1", "value1",
                "prop4", true);
        configMetadata = new ConfigurationMetadata("testName", ImmutableList.<PropertyMetadata<?>>of(
                new PropertyMetadata<>("prop1", "defValue"),
                new PropertyMetadata<>("prop2", String.class),
                new PropertyMetadata<>("prop3", 5),
                new PropertyMetadata<>("propIntArray", new Integer[] { 1,2,3 })),
                false);
    }

    @Test
    public void testWithResourceMetadata() {
        ConfigurationData underTest = new ConfigurationDataImpl(configMetadata, configResource, configResource, null,
                contextResource, "test", configurationManager, configurationOverrideManager, configurationPersistenceStrategy,
                true, "item1");
        
        assertEquals("test", underTest.getConfigName());
        assertEquals("item1", underTest.getCollectionItemName());
        
        assertEquals(configResource.getPath(), underTest.getResourcePath());
        assertEquals(ImmutableSet.of("prop1", "prop2", "prop3", "prop4", "propIntArray"), underTest.getPropertyNames());
        
        ValueMap values = underTest.getValues();
        assertEquals("value1", values.get("prop1", String.class));
        assertNull(values.get("prop2", String.class));
        assertNull(values.get("prop3", Integer.class));
        assertEquals(true, values.get("prop4", Boolean.class));

        ValueMap effectiveValues = underTest.getEffectiveValues();
        assertEquals("value1", effectiveValues.get("prop1", String.class));
        assertNull(effectiveValues.get("prop2", String.class));
        assertEquals((Integer)5, effectiveValues.get("prop3", Integer.class));
        assertEquals(true, effectiveValues.get("prop4", Boolean.class));
        
        ValueInfo<?> prop1 = underTest.getValueInfo("prop1");
        assertEquals("prop1", prop1.getPropertyMetadata().getName());
        assertEquals("value1", prop1.getValue());
        assertEquals("value1", prop1.getEffectiveValue());

        ValueInfo<?> prop3 = underTest.getValueInfo("prop3");
        assertEquals("prop3", prop3.getPropertyMetadata().getName());
        assertNull(prop3.getValue());
        assertEquals((Integer)5, prop3.getEffectiveValue());

        ValueInfo<?> prop4 = underTest.getValueInfo("prop4");
        assertNull("prop4", prop4.getPropertyMetadata());
        assertEquals(true, prop4.getValue());
        assertEquals(true, prop4.getEffectiveValue());

        ValueInfo<?> propIntArray = underTest.getValueInfo("propIntArray");
        assertNull(propIntArray.getValue());
        assertArrayEquals(new Integer[] {1,2,3}, (Integer[])propIntArray.getEffectiveValue());
    }

    @Test
    public void testWithResourceOnly() {
        ConfigurationData underTest = new ConfigurationDataImpl(null, configResource, configResource, null,
                contextResource, "test", configurationManager, configurationOverrideManager, configurationPersistenceStrategy,
                false, null);
        
        assertEquals("test", underTest.getConfigName());
        assertNull(underTest.getCollectionItemName());
        
        assertEquals(ImmutableSet.of("prop1", "prop4"), underTest.getPropertyNames());
        
        ValueMap values = underTest.getValues();
        assertEquals("value1", values.get("prop1", String.class));
        assertEquals(true, values.get("prop4", Boolean.class));

        ValueMap effectiveValues = underTest.getEffectiveValues();
        assertEquals("value1", effectiveValues.get("prop1", String.class));
        assertEquals(true, effectiveValues.get("prop4", Boolean.class));
        
        ValueInfo<?> prop1 = underTest.getValueInfo("prop1");
        assertNull(prop1.getPropertyMetadata());
        assertEquals("value1", prop1.getValue());
        assertEquals("value1", prop1.getEffectiveValue());

        ValueInfo<?> prop4 = underTest.getValueInfo("prop4");
        assertNull("prop4", prop4.getPropertyMetadata());
        assertEquals(true, prop4.getValue());
        assertEquals(true, prop4.getEffectiveValue());
    }

    @Test
    public void testMetadataOnly() {
        ConfigurationData underTest = new ConfigurationDataImpl(configMetadata,
                contextResource, "test", configurationManager, configurationOverrideManager, configurationPersistenceStrategy,
                false);
        
        assertEquals("test", underTest.getConfigName());
        assertNull(underTest.getCollectionItemName());
        
        assertEquals(ImmutableSet.of("prop1", "prop2", "prop3", "propIntArray"), underTest.getPropertyNames());
        
        ValueMap values = underTest.getValues();
        assertTrue(values.isEmpty());

        ValueMap effectiveValues = underTest.getEffectiveValues();
        assertEquals("defValue", effectiveValues.get("prop1", String.class));
        assertEquals((Integer)5, effectiveValues.get("prop3", Integer.class));
        
        ValueInfo<?> prop1 = underTest.getValueInfo("prop1");
        assertEquals("prop1", prop1.getPropertyMetadata().getName());
        assertNull(prop1.getValue());
        assertEquals("defValue", prop1.getEffectiveValue());

        ValueInfo<?> prop3 = underTest.getValueInfo("prop3");
        assertEquals("prop3", prop3.getPropertyMetadata().getName());
        assertNull(prop3.getValue());
        assertEquals((Integer)5, prop3.getEffectiveValue());
    }

    @Test
    public void testIgnoreProperties() {
        Resource resource = context.create().resource("/conf/testIgnoreProps",
                "prop1", "value1",
                "prop4", true,
                "jcr:primaryType", "myType");

        ConfigurationData underTest = new ConfigurationDataImpl(null, resource, resource, null,
                contextResource, "test", configurationManager, configurationOverrideManager, configurationPersistenceStrategy,
                false, null);
        
        assertEquals(ImmutableSet.of("prop1", "prop4"), underTest.getPropertyNames());
        
        assertNull(underTest.getValues().get("jcr:primaryType"));
        assertNull(underTest.getEffectiveValues().get("jcr:primaryType"));
    }

}
