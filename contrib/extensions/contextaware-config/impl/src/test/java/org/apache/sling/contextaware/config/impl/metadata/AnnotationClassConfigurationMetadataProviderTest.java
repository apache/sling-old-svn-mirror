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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.apache.sling.contextaware.config.annotation.Configuration;
import org.apache.sling.contextaware.config.example.AllTypesConfig;
import org.apache.sling.contextaware.config.example.MetadataSimpleConfig;
import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.Bundle;

public class AnnotationClassConfigurationMetadataProviderTest {
    
    @Rule
    public OsgiContext context = new OsgiContext();
    
    private AnnotationClassConfigurationMetadataProvider underTest;
    
    @Before
    public void setUp() {
        underTest = context.registerInjectActivateService(new AnnotationClassConfigurationMetadataProvider()); 
    }

    @Test
    public void testGetConfigurationMetadata() {
        
        // no configuration metadata present
        assertTrue(underTest.getConfigurationNames().isEmpty());
        
        // simulate bundle deployment with annotation classes
        Bundle dummyBundle = BundleEventUtil.startDummyBundle(context.bundleContext(),
                MetadataSimpleConfig.class, AllTypesConfig.class);
        
        // validate config metadata is available
        Set<String> configNames = underTest.getConfigurationNames();
        assertEquals(2, configNames.size());
        assertTrue(configNames.contains("simpleConfig"));
        assertTrue(configNames.contains(AllTypesConfig.class.getName()));
        assertEquals("simpleConfig", underTest.getConfigurationMetadata("simpleConfig").getName());
        assertEquals(AllTypesConfig.class.getName(), underTest.getConfigurationMetadata(AllTypesConfig.class.getName()).getName());
        
        // simulate bundle undeployment
        BundleEventUtil.stopDummyBundle(dummyBundle);

        // no configuration metadata present
        assertTrue(underTest.getConfigurationNames().isEmpty());
    }


    @Test
    public void testUnmappedConfigName() {
        assertNull(underTest.getConfigurationMetadata("unkonwn"));
    }

    // Define configuration annotation class mapped to name 'simpleConfig' already used by another class
    @Configuration(name = "simpleConfig")
    private @interface NameConflictMetadataSimpleConfig {
        String stringParam();
        int intParam() default 5;
        boolean boolParam();
    }

    @Test
    public void testNameConflictSingleBundle() {
        
        // simulate bundle deployment with annotation classes
        Bundle dummyBundle = BundleEventUtil.startDummyBundle(context.bundleContext(),
                MetadataSimpleConfig.class, NameConflictMetadataSimpleConfig.class);
        
        // validate config metadata is available
        Set<String> configNames = underTest.getConfigurationNames();
        assertEquals(1, configNames.size());  // only 1 - annotation with conflicting name is ignored
        assertTrue(configNames.contains("simpleConfig"));
        assertEquals("simpleConfig", underTest.getConfigurationMapping("simpleConfig").getConfigName());
        
        // simulate bundle undeployment
        BundleEventUtil.stopDummyBundle(dummyBundle);

        // no configuration metadata present
        assertTrue(underTest.getConfigurationNames().isEmpty());
    }

    @Test
    public void testNameConflictAccrossBundles() {
        
        // simulate bundle deployment with annotation classes
        Bundle dummyBundle1 = BundleEventUtil.startDummyBundle(context.bundleContext(),
                MetadataSimpleConfig.class);
        Bundle dummyBundle2 = BundleEventUtil.startDummyBundle(context.bundleContext(),
                NameConflictMetadataSimpleConfig.class);
        
        // validate config metadata is available
        Set<String> configNames = underTest.getConfigurationNames();
        assertEquals(1, configNames.size());  // only 1 - annotation with conflicting name is ignored
        assertTrue(configNames.contains("simpleConfig"));
        assertEquals("simpleConfig", underTest.getConfigurationMapping("simpleConfig").getConfigName());
        
        // simulate bundle undeployment
        BundleEventUtil.stopDummyBundle(dummyBundle1);
        BundleEventUtil.stopDummyBundle(dummyBundle2);

        // no configuration metadata present
        assertTrue(underTest.getConfigurationNames().isEmpty());
    }
   
    
}
