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
package org.apache.sling.contextaware.config.management.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.contextaware.config.impl.def.DefaultConfigurationPersistenceStrategy;
import org.apache.sling.contextaware.config.spi.ConfigurationPersistenceStrategy;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.Constants;

import com.google.common.collect.ImmutableList;

public class ConfigurationPersistenceStrategyMultiplexerTest {

    @Rule
    public SlingContext context = new SlingContext();
    
    private ConfigurationPersistenceStrategyMultiplexer underTest;
    
    private Resource resource1;
    private Resource resource2;
    
    @Before
    public void setUp() {
        underTest = context.registerInjectActivateService(new ConfigurationPersistenceStrategyMultiplexer());
        resource1 = context.create().resource("/conf/test1");
        resource2 = context.create().resource("/conf/test2");
    }
    
    @Test
    public void testWithNoStrategies() {
        assertNull(underTest.getResource(resource1));
        assertFalse(underTest.persist(context.resourceResolver(), "/conf/test1", resource1.getValueMap()));
        assertFalse(underTest.persistCollection(context.resourceResolver(), "/conf/testCol", 
                ImmutableList.<Map<String,Object>>of(resource1.getValueMap(), resource2.getValueMap())));
    }

    @Test
    public void testWithDefaultStrategy() {
        context.registerInjectActivateService(new DefaultConfigurationPersistenceStrategy());

        Resource result = underTest.getResource(resource1);
        assertSame(resource1, result);
        assertTrue(underTest.persist(context.resourceResolver(), "/conf/test1", resource1.getValueMap()));
        assertTrue(underTest.persistCollection(context.resourceResolver(), "/conf/testCol", 
                ImmutableList.<Map<String,Object>>of(resource1.getValueMap(), resource2.getValueMap())));
    }
    
    @Test
    public void testMultipleStrategies() {
        
        // strategy 1
        context.registerService(ConfigurationPersistenceStrategy.class, new ConfigurationPersistenceStrategy() {
            @Override
            public Resource getResource(Resource resource) {
                return resource2;
            }
            @Override
            public boolean persist(ResourceResolver resourceResolver, String configResourcePath, Map<String,Object> properties) {
                return true;
            }
            @Override
            public boolean persistCollection(ResourceResolver resourceResolver, String configResourceCollectionParentPath,
                    Collection<Map<String,Object>> propertiesCollection) {
                return false;
            }
        }, Constants.SERVICE_RANKING, 2000);
        
        // strategy 2
        context.registerService(ConfigurationPersistenceStrategy.class, new ConfigurationPersistenceStrategy() {
            @Override
            public Resource getResource(Resource resource) {
                return resource1;
            }
            @Override
            public boolean persist(ResourceResolver resourceResolver, String configResourcePath, Map<String,Object> properties) {
                return false;
            }
            @Override
            public boolean persistCollection(ResourceResolver resourceResolver, String configResourceCollectionParentPath,
                    Collection<Map<String,Object>> propertiesCollection) {
                return true;
            }

        }, Constants.SERVICE_RANKING, 1000);
        
        Resource result = underTest.getResource(resource1);
        assertSame(resource2, result);
        assertTrue(underTest.persist(context.resourceResolver(), "/conf/test1", resource1.getValueMap()));
        assertTrue(underTest.persistCollection(context.resourceResolver(), "/conf/testCol", 
                ImmutableList.<Map<String,Object>>of(resource1.getValueMap(), resource2.getValueMap())));
    }
    

}
