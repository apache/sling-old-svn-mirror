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
package org.apache.sling.replication.agent.impl;

import java.lang.reflect.Field;
import java.util.Properties;

import org.junit.Test;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testcase for {@link DefaultReplicationAgentConfigurationManager}
 */
public class DefaultReplicationAgentConfigurationManagerTest {

    @Test
    public void testRetrievalWithNoConfigurationAvailable() throws Exception {
        ConfigurationAdmin configAdmin = mock(ConfigurationAdmin.class);

        DefaultReplicationAgentConfigurationManager defaultReplicationAgentConfigurationManager = new DefaultReplicationAgentConfigurationManager();
        Field configAdminField = defaultReplicationAgentConfigurationManager.getClass().getDeclaredField("configAdmin");
        configAdminField.setAccessible(true);
        configAdminField.set(defaultReplicationAgentConfigurationManager, configAdmin);
        
        try {
            defaultReplicationAgentConfigurationManager.getConfiguration("fake");
            fail("an exception should be thrown when no configuration is available");
        } catch (Exception e) {
            // failure is expected
        }
    }

    @Test
    public void testRetrievalWithMultipleConfigurationsAvailable() throws Exception {
        ConfigurationAdmin configAdmin = mock(ConfigurationAdmin.class);

        when(configAdmin.listConfigurations("(name=publish)")).thenReturn(new Configuration[]{null, null});
        DefaultReplicationAgentConfigurationManager defaultReplicationAgentConfigurationManager = new DefaultReplicationAgentConfigurationManager();
        Field configAdminField = defaultReplicationAgentConfigurationManager.getClass().getDeclaredField("configAdmin");
        configAdminField.setAccessible(true);
        configAdminField.set(defaultReplicationAgentConfigurationManager, configAdmin);
        
        try {
            defaultReplicationAgentConfigurationManager.getConfiguration("publish");
            fail("an exception should be thrown when multiple configurations are available");
        } catch (Exception e) {
            // failure is expected
        }
    }

    @Test
    public void testRetrievalWithOneConfigurationAvailable() throws Exception {
        Configuration configuration = mock(Configuration.class);
        when(configuration.getProperties()).thenReturn(new Properties());
        ConfigurationAdmin configAdmin = mock(ConfigurationAdmin.class);

        when(configAdmin.listConfigurations("(name=publish)")).thenReturn(new Configuration[]{configuration});
        DefaultReplicationAgentConfigurationManager defaultReplicationAgentConfigurationManager = new DefaultReplicationAgentConfigurationManager();
        Field configAdminField = defaultReplicationAgentConfigurationManager.getClass().getDeclaredField("configAdmin");
        configAdminField.setAccessible(true);
        configAdminField.set(defaultReplicationAgentConfigurationManager, configAdmin);
        defaultReplicationAgentConfigurationManager.getConfiguration("publish");
    }
}
