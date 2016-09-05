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

import org.apache.sling.contextaware.config.it.example.SimpleConfig;
import org.apache.sling.contextaware.config.it.util.WaitFor;
import org.apache.sling.contextaware.config.spi.ConfigurationMetadataProvider;
import org.apache.sling.contextaware.config.spi.metadata.ConfigurationMetadata;
import org.apache.sling.junit.rules.TeleporterRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ConfigurationMetadataProviderIT {
    
    @Rule
    public TeleporterRule teleporter = TeleporterRule.forClass(getClass(), "IT");
    
    @Before
    public void setUp() throws Exception {
        WaitFor.services(teleporter, ConfigurationMetadataProvider.class);
    }
    
    @Test
    public void testConfigurationMetadata() {
        ConfigurationMetadataProvider underTest = teleporter.getService(ConfigurationMetadataProvider.class);
        
        ConfigurationMetadata configMetadata = underTest.getConfigurationMetadata(SimpleConfig.class.getName());
        assertNotNull(configMetadata);
        assertEquals(SimpleConfig.class.getName(), configMetadata.getName());
    }
    
}
