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
package org.apache.sling.contextaware.config.resource.impl.def;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.contextaware.config.resource.spi.ConfigurationResourcePersistenceStrategy;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;


public class DefaultConfigurationResourcePersistenceStrategyTest {

    @Rule
    public SlingContext context = new SlingContext();
    
    private Resource resource;
    
    @Before
    public void setUp() {
        resource = context.create().resource("/conf/test");
    }
    
    @Test
    public void testGetResource() {
        ConfigurationResourcePersistenceStrategy underTest = context.registerInjectActivateService(new DefaultConfigurationResourcePersistenceStrategy());
        
        Resource result = underTest.getResource(resource);
        assertSame(resource, result);
    }

    @Test
    public void testDisabled() {
        ConfigurationResourcePersistenceStrategy underTest = context.registerInjectActivateService(new DefaultConfigurationResourcePersistenceStrategy(),
                "enabled", false);
        
        assertNull(underTest.getResource(resource));
    }

}
