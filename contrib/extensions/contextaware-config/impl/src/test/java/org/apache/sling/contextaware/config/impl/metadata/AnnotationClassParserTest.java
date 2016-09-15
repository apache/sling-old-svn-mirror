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
package org.apache.sling.contextaware.config.impl.metadata;

import static org.apache.sling.contextaware.config.impl.metadata.AnnotationClassParser.buildConfigurationMetadata;
import static org.apache.sling.contextaware.config.impl.metadata.AnnotationClassParser.getConfigurationName;
import static org.apache.sling.contextaware.config.impl.metadata.AnnotationClassParser.getPropertyName;
import static org.apache.sling.contextaware.config.impl.metadata.AnnotationClassParser.isContextAwareConfig;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.contextaware.config.example.AllTypesConfig;
import org.apache.sling.contextaware.config.example.MetadataSimpleConfig;
import org.apache.sling.contextaware.config.example.SimpleConfig;
import org.apache.sling.contextaware.config.example.WithoutAnnotationConfig;
import org.apache.sling.contextaware.config.spi.metadata.ConfigurationMetadata;
import org.apache.sling.contextaware.config.spi.metadata.PropertyMetadata;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class AnnotationClassParserTest {

    @Test
    public void testIsContextAwareConfig() {
        assertTrue(isContextAwareConfig(SimpleConfig.class));
        assertFalse(isContextAwareConfig(WithoutAnnotationConfig.class));
        assertFalse(isContextAwareConfig(Object.class));
    }

    @Test
    public void testGetConfigurationName() {
        assertEquals(SimpleConfig.class.getName(), getConfigurationName(SimpleConfig.class));
        assertEquals("simpleConfig", getConfigurationName(MetadataSimpleConfig.class));
        assertNull(getConfigurationName(WithoutAnnotationConfig.class));
        assertNull(getConfigurationName(Object.class));
    }

    @Test
    public void testGetPropertyName() {
        // test all variants defined in OSGi spec as example
        assertEquals("myProperty143", getPropertyName("myProperty143"));
        assertEquals("new", getPropertyName("$new"));
        assertEquals("my$prop", getPropertyName("my$$prop"));
        assertEquals("dot.prop", getPropertyName("dot_prop"));
        assertEquals(".secret", getPropertyName("_secret"));
        assertEquals("another_prop", getPropertyName("another__prop"));
        assertEquals("three_.prop", getPropertyName("three___prop"));
        assertEquals("four._prop", getPropertyName("four_$__prop"));
        assertEquals("five..prop", getPropertyName("five_$_prop"));
    }

    @Test
    public void testBuildConfigurationMetadata_Simple() {
        ConfigurationMetadata metadata = buildConfigurationMetadata(MetadataSimpleConfig.class);
        
        assertEquals("simpleConfig", metadata.getName());
        assertEquals("Simple configuration", metadata.getLabel());
        assertEquals("This is a configuration example with additional metadata.", metadata.getDescription());
        assertEquals(ImmutableMap.of("param1", "value1", "param2", "123"), metadata.getProperties());
        
        Collection<PropertyMetadata<?>> propertyMetadataList = metadata.getPropertyMetadata().values();
        assertEquals(3, propertyMetadataList.size());
        
        for (PropertyMetadata<?> propertyMetadata : propertyMetadataList) {
            if (StringUtils.equals(propertyMetadata.getName(), "stringParam")) {
                assertEquals("String Param", propertyMetadata.getLabel());
                assertEquals("Enter strings here.", propertyMetadata.getDescription());
                assertEquals(ImmutableMap.of("p1", "v1"), propertyMetadata.getProperties());
                assertNull(propertyMetadata.getDefaultValue());
            }
            else if (StringUtils.equals(propertyMetadata.getName(), "intParam")) {
                assertEquals("Integer Param", propertyMetadata.getLabel());
                assertNull(propertyMetadata.getDescription());
                assertTrue(propertyMetadata.getProperties().isEmpty());
                assertEquals(5, propertyMetadata.getDefaultValue());
            }
            else if (StringUtils.equals(propertyMetadata.getName(), "booleanParam")) {
                assertNull(propertyMetadata.getLabel());
                assertNull(propertyMetadata.getDescription());
                assertTrue(propertyMetadata.getProperties().isEmpty());
                assertFalse((Boolean)propertyMetadata.getDefaultValue());
            }
        }
    }
    
    @Test
    public void testBuildConfigurationMetadata_AllTypes() {
        ConfigurationMetadata metadata = buildConfigurationMetadata(AllTypesConfig.class);
        
        assertEquals(AllTypesConfig.class.getName(), metadata.getName());
        assertNull(metadata.getLabel());
        assertNull(metadata.getDescription());
        assertTrue(metadata.getProperties().isEmpty());
        assertEquals(20, metadata.getPropertyMetadata().size());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testBuildConfigurationMetadata_IllegalClass() {
        buildConfigurationMetadata(WithoutAnnotationConfig.class);
    }
    
}
