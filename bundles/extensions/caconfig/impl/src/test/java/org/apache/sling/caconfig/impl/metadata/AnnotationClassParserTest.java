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
package org.apache.sling.caconfig.impl.metadata;

import static org.apache.sling.caconfig.impl.metadata.AnnotationClassParser.buildConfigurationMetadata;
import static org.apache.sling.caconfig.impl.metadata.AnnotationClassParser.getConfigurationName;
import static org.apache.sling.caconfig.impl.metadata.AnnotationClassParser.getPropertyName;
import static org.apache.sling.caconfig.impl.metadata.AnnotationClassParser.isContextAwareConfig;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.caconfig.example.AllTypesConfig;
import org.apache.sling.caconfig.example.ListConfig;
import org.apache.sling.caconfig.example.MetadataSimpleConfig;
import org.apache.sling.caconfig.example.NestedConfig;
import org.apache.sling.caconfig.example.SimpleConfig;
import org.apache.sling.caconfig.example.WithoutAnnotationConfig;
import org.apache.sling.caconfig.spi.metadata.ConfigurationMetadata;
import org.apache.sling.caconfig.spi.metadata.PropertyMetadata;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
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
        assertFalse(metadata.isCollection());
        
        List<PropertyMetadata<?>> propertyMetadataList = ImmutableList.copyOf(metadata.getPropertyMetadata().values());
        assertEquals(3, propertyMetadataList.size());
        
        PropertyMetadata<?> stringParam = propertyMetadataList.get(0);
        assertEquals("String Param", stringParam.getLabel());
        assertEquals("Enter strings here.", stringParam.getDescription());
        assertEquals(ImmutableMap.of("p1", "v1"), stringParam.getProperties());
        assertNull(stringParam.getDefaultValue());

        PropertyMetadata<?> intParam = propertyMetadataList.get(1);
        assertEquals("Integer Param", intParam.getLabel());
        assertNull(intParam.getDescription());
        assertTrue(intParam.getProperties().isEmpty());
        assertEquals(5, intParam.getDefaultValue());

        PropertyMetadata<?> boolParam = propertyMetadataList.get(2);
        assertNull(boolParam.getLabel());
        assertNull(boolParam.getDescription());
        assertTrue(boolParam.getProperties().isEmpty());
        assertNull(boolParam.getDefaultValue());
    }
    
    @Test
    public void testBuildConfigurationMetadata_List() {
        ConfigurationMetadata metadata = buildConfigurationMetadata(ListConfig.class);
        
        assertEquals(ListConfig.class.getName(), metadata.getName());
        assertTrue(metadata.isCollection());
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
    
    @Test
    public void testBuildConfigurationMetadata_Nested() {
        ConfigurationMetadata metadata = buildConfigurationMetadata(NestedConfig.class);
        
        assertEquals(NestedConfig.class.getName(), metadata.getName());

        Collection<PropertyMetadata<?>> propertyMetadataList = metadata.getPropertyMetadata().values();
        assertEquals(4, propertyMetadataList.size());
        
        for (PropertyMetadata<?> propertyMetadata : propertyMetadataList) {
            if (StringUtils.equals(propertyMetadata.getName(), "stringParam")) {
                assertEquals(String.class, propertyMetadata.getType());
            }
            else if (StringUtils.equals(propertyMetadata.getName(), "subConfig")) {
                assertEquals(ConfigurationMetadata.class, propertyMetadata.getType());
                
                ConfigurationMetadata subConfigMetadata = propertyMetadata.getConfigurationMetadata();
                assertEquals("subConfig", subConfigMetadata.getName());
                assertEquals(3, subConfigMetadata.getPropertyMetadata().size());
            }
            else if (StringUtils.equals(propertyMetadata.getName(), "subListConfig")) {
                assertEquals(ConfigurationMetadata[].class, propertyMetadata.getType());

                ConfigurationMetadata subListConfigMetadata = propertyMetadata.getConfigurationMetadata(); 
                assertEquals("subListConfig", subListConfigMetadata.getName());
                assertEquals(2, subListConfigMetadata.getPropertyMetadata().size());
            }
            else if (StringUtils.equals(propertyMetadata.getName(), "subConfigWithoutAnnotation")) {
                assertEquals(ConfigurationMetadata.class, propertyMetadata.getType());

                ConfigurationMetadata subConfigWithoutAnnotationMetadata = propertyMetadata.getConfigurationMetadata(); 
                assertEquals("subConfigWithoutAnnotation", subConfigWithoutAnnotationMetadata.getName());
                assertEquals(1, subConfigWithoutAnnotationMetadata.getPropertyMetadata().size());
            }
            else {
                fail("Unexpected property name: " + propertyMetadata.getName());
            }
        }
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testBuildConfigurationMetadata_IllegalClass() {
        buildConfigurationMetadata(WithoutAnnotationConfig.class);
    }
    
}
