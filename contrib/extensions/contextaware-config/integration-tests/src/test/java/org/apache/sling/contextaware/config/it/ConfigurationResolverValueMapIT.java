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
package org.apache.sling.contextaware.config.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.contextaware.config.ConfigurationResolver;
import org.apache.sling.junit.rules.TeleporterRule;
import org.apache.sling.resourcebuilder.api.ResourceBuilder;
import org.apache.sling.resourcebuilder.api.ResourceBuilderFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ConfigurationResolverValueMapIT {
    
    @Rule
    public TeleporterRule teleporter = TeleporterRule.forClass(getClass(), "IT");
    
    private ResourceResolver resourceResolver;
    private ResourceBuilder resourceBuilder;
    
    @SuppressWarnings("deprecation")
    @Before
    public void setUp() throws Exception {
        resourceResolver = teleporter.getService(ResourceResolverFactory.class).getAdministrativeResourceResolver(null);
        resourceBuilder = teleporter.getService(ResourceBuilderFactory.class).forResolver(resourceResolver);
    }
    
    @After
    public void tearDown() {
        resourceResolver.close();
    }
    
    @Test
    public void testNonExistingConfig() throws Exception {
        resourceBuilder.resource("content/page1");
        Resource resourcePage1 = resourceResolver.getResource("/content/page1");
        
        ConfigurationResolver configResolver = teleporter.getService(ConfigurationResolver.class);
        ValueMap props = configResolver.get(resourcePage1).name("test").asValueMap();
        assertNotNull(props);

        assertNull(props.get("stringParam", String.class));
        assertEquals(0, (int)props.get("intParam", 0));
        assertEquals(false, props.get("boolParam", false));
    }
    
    @Test
    public void testExistingConfig() throws Exception {
        resourceBuilder.resource("conf/content/page1/sling:configs/test",
                "stringParam", "value1",
                "intParam", 123,
                "boolParam", true)
            .atParent()
            .resource("content/page1",
                    "sling:config-ref", "/conf/content/page1");
        
        Resource resourcePage1 = resourceResolver.getResource("/content/page1");
        
        ConfigurationResolver configResolver = teleporter.getService(ConfigurationResolver.class);
        ValueMap props = configResolver.get(resourcePage1).name("test").asValueMap();
        assertNotNull(props);
        
        assertEquals("value1", props.get("stringParam", String.class));
        assertEquals(123, (int)props.get("intParam", 0));
        assertEquals(true, props.get("boolParam", false));
    }
    
}
