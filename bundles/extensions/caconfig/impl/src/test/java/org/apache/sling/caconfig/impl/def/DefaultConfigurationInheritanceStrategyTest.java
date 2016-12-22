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

import static org.apache.sling.caconfig.impl.def.ConfigurationDefNameConstants.PROPERTY_CONFIG_PROPERTY_INHERIT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Iterator;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.caconfig.spi.ConfigurationInheritanceStrategy;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class DefaultConfigurationInheritanceStrategyTest {
    
    private static final String PROPERTY_CONFIG_PROPERTY_INHERIT_CUSTOM = "custom:configPropertyInherit";
    
    @Rule
    public SlingContext context = new SlingContext();
    
    private ConfigurationInheritanceStrategy underTest;
    
    @Test
    public void testWithoutPropertyMerging() {
        underTest = context.registerInjectActivateService(new DefaultConfigurationInheritanceStrategy());

        Iterator<Resource> resources = ImmutableList.of(
                context.create().resource("/conf/resource1", "prop1", "value1a", "prop2", "value2a"),
                context.create().resource("/conf/resource2", "prop2", "value2b", "prop3", "value3b"),
                context.create().resource("/conf/resource3", "prop4", "value4b")
                ).iterator();
        
        Resource inherited = underTest.getResource(resources);
        ValueMap props = inherited.getValueMap();
        
        assertEquals("value1a", props.get("prop1", String.class));
        assertEquals("value2a", props.get("prop2", String.class));
        assertNull(props.get("prop3", String.class));
        assertNull(props.get("prop4", String.class));        
    }

    @Test
    public void testWithPropertyMerging() {
        underTest = context.registerInjectActivateService(new DefaultConfigurationInheritanceStrategy(),
                "configPropertyInheritancePropertyNames", PROPERTY_CONFIG_PROPERTY_INHERIT_CUSTOM);
        Iterator<Resource> resources = ImmutableList.of(
                context.create().resource("/conf/resource1", "prop1", "value1a", "prop2", "value2a", PROPERTY_CONFIG_PROPERTY_INHERIT, true),
                context.create().resource("/conf/resource2", "prop2", "value2b", "prop3", "value3b", PROPERTY_CONFIG_PROPERTY_INHERIT_CUSTOM, true),
                context.create().resource("/conf/resource3", "prop4", "value4b")
                ).iterator();
        
        Resource inherited = underTest.getResource(resources);
        ValueMap props = inherited.getValueMap();
        
        assertEquals("value1a", props.get("prop1", String.class));
        assertEquals("value2a", props.get("prop2", String.class));
        assertEquals("value3b", props.get("prop3", String.class));
        assertEquals("value4b", props.get("prop4", String.class));        
    }

    @Test
    public void testWithPartialPropertyMerging() {
        underTest = context.registerInjectActivateService(new DefaultConfigurationInheritanceStrategy());

        Iterator<Resource> resources = ImmutableList.of(
                context.create().resource("/conf/resource1", "prop1", "value1a", "prop2", "value2a", PROPERTY_CONFIG_PROPERTY_INHERIT, true),
                context.create().resource("/conf/resource2", "prop2", "value2b", "prop3", "value3b"),
                context.create().resource("/conf/resource3", "prop4", "value4b")
                ).iterator();
        
        Resource inherited = underTest.getResource(resources);
        ValueMap props = inherited.getValueMap();
        
        assertEquals("value1a", props.get("prop1", String.class));
        assertEquals("value2a", props.get("prop2", String.class));
        assertEquals("value3b", props.get("prop3", String.class));
        assertNull(props.get("prop4", String.class));        
    }

    @Test
    public void testDisabled() {
        underTest = context.registerInjectActivateService(new DefaultConfigurationInheritanceStrategy(),
                "enabled", false);

        Iterator<Resource> resources = ImmutableList.of(
                context.create().resource("/conf/resource1", "prop1", "value1a", "prop2", "value2a", PROPERTY_CONFIG_PROPERTY_INHERIT, true),
                context.create().resource("/conf/resource2", "prop2", "value2b", "prop3", "value3b", PROPERTY_CONFIG_PROPERTY_INHERIT, true),
                context.create().resource("/conf/resource3", "prop4", "value4b")
                ).iterator();
        
        Resource inherited = underTest.getResource(resources);
        assertNull(inherited);
    }

}
