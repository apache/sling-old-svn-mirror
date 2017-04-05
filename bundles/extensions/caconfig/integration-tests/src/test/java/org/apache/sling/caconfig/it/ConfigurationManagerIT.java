/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.caconfig.it;

import static org.apache.sling.caconfig.it.TestUtils.CONFIG_ROOT_PATH;
import static org.apache.sling.caconfig.it.TestUtils.CONTENT_ROOT_PATH;
import static org.apache.sling.caconfig.it.TestUtils.cleanUp;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.caconfig.ConfigurationResolver;
import org.apache.sling.caconfig.it.example.SimpleConfig;
import org.apache.sling.caconfig.management.ConfigurationData;
import org.apache.sling.caconfig.management.ConfigurationManager;
import org.apache.sling.caconfig.spi.ConfigurationCollectionPersistData;
import org.apache.sling.caconfig.spi.ConfigurationPersistData;
import org.apache.sling.junit.rules.TeleporterRule;
import org.apache.sling.resourcebuilder.api.ResourceBuilder;
import org.apache.sling.resourcebuilder.api.ResourceBuilderFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ConfigurationManagerIT {
    
    @Rule
    public TeleporterRule teleporter = TeleporterRule.forClass(getClass(), "IT");
    
    private ResourceResolver resourceResolver;
    private ResourceBuilder resourceBuilder;
    private ConfigurationManager configManager;
    private ConfigurationResolver configResolver;
    
    private static final String PAGE_PATH = CONTENT_ROOT_PATH + "/page1";
    private static final String CONFIG_PATH = CONFIG_ROOT_PATH + "/page1";
    private static final String CONFIG_NAME = SimpleConfig.class.getName();
    
    private Resource resourcePage1;
    
    @SuppressWarnings("deprecation")
    @Before
    public void setUp() throws Exception {
        resourceResolver = teleporter.getService(ResourceResolverFactory.class).getAdministrativeResourceResolver(null);
        resourceBuilder = teleporter.getService(ResourceBuilderFactory.class).forResolver(resourceResolver);
        configManager = teleporter.getService(ConfigurationManager.class);
        configResolver = teleporter.getService(ConfigurationResolver.class);
        
        resourcePage1 = resourceBuilder.resource(PAGE_PATH, "sling:configRef", CONFIG_PATH).getCurrentParent();
    }
    
    @After
    public void tearDown() {
        cleanUp(resourceResolver);
        resourceResolver.close();
    }
    
    @Test
    public void testNonExistingConfig() throws Exception {
        ConfigurationData config = configManager.getConfiguration(resourcePage1, CONFIG_NAME);
        assertNotNull(config);

        ValueMap props = config.getEffectiveValues();
        assertNull(props.get("stringParam", String.class));
        assertEquals("defValue", props.get("stringParamDefault", String.class));
        assertEquals(0, (int)props.get("intParam", 0));
        assertEquals(false, props.get("boolParam", false));
    }
    
    @Test
    public void testExistingConfig() throws Exception {
        resourceBuilder.resource(CONFIG_PATH + "/sling:configs/" + CONFIG_NAME,
                "stringParam", "value1",
                "intParam", 123,
                "boolParam", true);
        
        ConfigurationData config = configManager.getConfiguration(resourcePage1, CONFIG_NAME);
        assertNotNull(config);
        
        ValueMap props = config.getEffectiveValues();
        assertEquals("value1", props.get("stringParam", String.class));
        assertEquals("defValue", props.get("stringParamDefault", String.class));
        assertEquals(123, (int)props.get("intParam", 0));
        assertEquals(true, props.get("boolParam", false));
    }
    
    @Test
    public void testWriteConfig() throws Exception {
        // write configuration data via configuration manager
        Map<String,Object> values = new HashMap<>();
        values.put("stringParam", "valueA");
        values.put("stringParamDefault", "valueB");
        values.put("intParam", 55);
        values.put("boolParam", true);
        configManager.persistConfiguration(resourcePage1, CONFIG_NAME, new ConfigurationPersistData(values));
        resourceResolver.commit();
        
        // read config via configuration resolver
        SimpleConfig config = configResolver.get(resourcePage1).as(SimpleConfig.class);
        assertNotNull(config);
        
        assertEquals("valueA", config.stringParam());
        assertEquals("valueB", config.stringParamDefault());
        assertEquals(55, (int)config.intParam());
        assertEquals(true, config.boolParam());
    }
    
    @Test
    public void testWriteConfigCollection() throws Exception {
        // write configuration data via configuration manager
        Map<String,Object> values1 = new HashMap<>();
        values1.put("stringParam", "valueA");
        values1.put("stringParamDefault", "valueB");
        Map<String,Object> values2 = new HashMap<>();
        values2.put("intParam", 55);
        values2.put("boolParam", true);
        List<ConfigurationPersistData> items = new ArrayList<>();
        items.add(new ConfigurationPersistData(values1).collectionItemName("item1"));
        items.add(new ConfigurationPersistData(values2).collectionItemName("item2"));
        configManager.persistConfigurationCollection(resourcePage1, CONFIG_NAME, new ConfigurationCollectionPersistData(items));
        resourceResolver.commit();
        
        // read config via configuration resolver
        Collection<SimpleConfig> config = configResolver.get(resourcePage1).asCollection(SimpleConfig.class);
        assertEquals(2, config.size());
        
        Iterator<SimpleConfig> configIterator = config.iterator();
        SimpleConfig config1 = configIterator.next();
        SimpleConfig config2 = configIterator.next();
        
        assertEquals("valueA", config1.stringParam());
        assertEquals("valueB", config1.stringParamDefault());
        assertEquals(55, (int)config2.intParam());
        assertEquals(true, config2.boolParam());
    }
    
}
