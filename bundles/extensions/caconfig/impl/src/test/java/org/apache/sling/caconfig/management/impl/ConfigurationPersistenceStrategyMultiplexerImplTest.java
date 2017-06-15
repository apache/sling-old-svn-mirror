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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.caconfig.impl.ConfigurationPersistenceStrategyBridge;
import org.apache.sling.caconfig.impl.def.DefaultConfigurationPersistenceStrategy;
import org.apache.sling.caconfig.management.multiplexer.ConfigurationPersistenceStrategyMultiplexer;
import org.apache.sling.caconfig.spi.ConfigurationCollectionPersistData;
import org.apache.sling.caconfig.spi.ConfigurationPersistData;
import org.apache.sling.caconfig.spi.ConfigurationPersistenceStrategy2;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.Constants;

import com.google.common.collect.ImmutableList;

public class ConfigurationPersistenceStrategyMultiplexerImplTest {

    @Rule
    public SlingContext context = new SlingContext();
    
    private ConfigurationPersistenceStrategyMultiplexer underTest;
    
    private Resource resource1;
    private Resource resource2;
    
    @Before
    public void setUp() {
        context.registerInjectActivateService(new ConfigurationManagementSettingsImpl());
        underTest = context.registerInjectActivateService(new ConfigurationPersistenceStrategyMultiplexerImpl());
        context.registerInjectActivateService(new ConfigurationPersistenceStrategyBridge());
        resource1 = context.create().resource("/conf/test1");
        resource2 = context.create().resource("/conf/test2");
    }
    
    @Test
    public void testWithNoStrategies() {
        assertNull(underTest.getResource(resource1));
        assertNull(underTest.getResourcePath(resource1.getPath()));
        assertFalse(underTest.persistConfiguration(context.resourceResolver(), "/conf/test1", new ConfigurationPersistData(resource1.getValueMap())));
        assertFalse(underTest.persistConfigurationCollection(context.resourceResolver(), "/conf/testCol",
                new ConfigurationCollectionPersistData(ImmutableList.of(
                        new ConfigurationPersistData(resource1.getValueMap()).collectionItemName(resource1.getName()),
                        new ConfigurationPersistData(resource2.getValueMap()).collectionItemName(resource2.getName())))));
        assertFalse(underTest.deleteConfiguration(context.resourceResolver(), "/conf/test1"));
    }

    @Test
    public void testWithDefaultStrategy() {
        context.registerInjectActivateService(new DefaultConfigurationPersistenceStrategy());

        assertSame(resource1, underTest.getResource(resource1));
        assertEquals(resource1.getPath(), underTest.getResourcePath(resource1.getPath()));
        assertTrue(underTest.persistConfiguration(context.resourceResolver(), "/conf/test1", new ConfigurationPersistData(resource1.getValueMap())));
        assertTrue(underTest.persistConfigurationCollection(context.resourceResolver(), "/conf/testCol",
                new ConfigurationCollectionPersistData(ImmutableList.of(
                        new ConfigurationPersistData(resource1.getValueMap()).collectionItemName(resource1.getName()),
                        new ConfigurationPersistData(resource2.getValueMap()).collectionItemName(resource2.getName())))));
        assertTrue(underTest.deleteConfiguration(context.resourceResolver(), "/conf/test1"));
    }
    
    @SuppressWarnings("deprecation")
    @Test
    public void testMultipleStrategies() {
        
        // strategy 1 (using old ConfigurationPersistenceStrategy with bridge to  ConfigurationPersistenceStrategy2)
        context.registerService(org.apache.sling.caconfig.spi.ConfigurationPersistenceStrategy.class,
                new org.apache.sling.caconfig.spi.ConfigurationPersistenceStrategy() {
            @Override
            public Resource getResource(Resource resource) {
                return resource2;
            }
            @Override
            public String getResourcePath(String resourcePath) {
                return resource2.getPath();
            }
            @Override
            public boolean persistConfiguration(ResourceResolver resourceResolver, String configResourcePath,
                    ConfigurationPersistData data) {
                return true;
            }
            @Override
            public boolean persistConfigurationCollection(ResourceResolver resourceResolver,
                    String configResourceCollectionParentPath, ConfigurationCollectionPersistData data) {
                return false;
            }
            @Override
            public boolean deleteConfiguration(ResourceResolver resourceResolver, String configResourcePath) {
                return false;
            }
        }, Constants.SERVICE_RANKING, 2000);
        
        // strategy 2
        context.registerService(ConfigurationPersistenceStrategy2.class, new ConfigurationPersistenceStrategy2() {
            @Override
            public Resource getResource(Resource resource) {
                return resource1;
            }
            @Override
            public Resource getCollectionParentResource(Resource resource) {
                return resource1;
            }
            @Override
            public Resource getCollectionItemResource(Resource resource) {
                return resource1;
            }
            @Override
            public String getResourcePath(String resourcePath) {
                return resource1.getPath();
            }
            @Override
            public String getCollectionParentResourcePath(String resourcePath) {
                return resource1.getPath();
            }
            @Override
            public String getCollectionItemResourcePath(String resourcePath) {
                return resource1.getPath();
            }
            @Override
            public String getConfigName(String configName, String relatedConfigPath) {
                return resource1.getPath();
            }
            @Override
            public String getCollectionParentConfigName(String configName, String relatedConfigPath) {
                return resource1.getPath();
            }
            @Override
            public String getCollectionItemConfigName(String configName, String relatedConfigPath) {
                return resource1.getPath();
            }
            @Override
            public boolean persistConfiguration(ResourceResolver resourceResolver, String configResourcePath,
                    ConfigurationPersistData data) {
                return false;
            }
            @Override
            public boolean persistConfigurationCollection(ResourceResolver resourceResolver,
                    String configResourceCollectionParentPath, ConfigurationCollectionPersistData data) {
                return true;
            }
            @Override
            public boolean deleteConfiguration(ResourceResolver resourceResolver, String configResourcePath) {
                return true;
            }
        }, Constants.SERVICE_RANKING, 1000);
        
        assertSame(resource2, underTest.getResource(resource1));
        assertSame(resource1, underTest.getCollectionParentResource(resource1));
        assertSame(resource2, underTest.getCollectionItemResource(resource1));
        assertEquals(resource2.getPath(), underTest.getResourcePath(resource1.getPath()));
        assertEquals(resource1.getPath(), underTest.getCollectionParentResourcePath(resource1.getPath()));
        assertEquals(resource2.getPath(), underTest.getCollectionItemResourcePath(resource1.getPath()));
        assertEquals(resource2.getPath(), underTest.getConfigName(resource1.getPath(), null));
        assertEquals(resource1.getPath(), underTest.getCollectionParentConfigName(resource1.getPath(), null));
        assertEquals(resource2.getPath(), underTest.getCollectionItemConfigName(resource1.getPath(), null));
        assertEquals(ImmutableList.of(resource2.getPath(), resource1.getPath()), ImmutableList.copyOf(underTest.getAllConfigNames(resource1.getPath())));
        assertEquals(ImmutableList.of(resource1.getPath()), ImmutableList.copyOf(underTest.getAllCollectionParentConfigNames(resource1.getPath())));
        assertEquals(ImmutableList.of(resource2.getPath(), resource1.getPath()), ImmutableList.copyOf(underTest.getAllCollectionItemConfigNames(resource1.getPath())));
        assertTrue(underTest.persistConfiguration(context.resourceResolver(), "/conf/test1", new ConfigurationPersistData(resource1.getValueMap())));
        assertTrue(underTest.persistConfigurationCollection(context.resourceResolver(), "/conf/testCol",
                new ConfigurationCollectionPersistData(ImmutableList.of(
                        new ConfigurationPersistData(resource1.getValueMap()).collectionItemName(resource1.getName()),
                        new ConfigurationPersistData(resource2.getValueMap()).collectionItemName(resource2.getName())))));
        assertTrue(underTest.deleteConfiguration(context.resourceResolver(), "/conf/test1"));
    }

}
