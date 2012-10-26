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
package org.apache.sling.commons.log.internal.config;

import java.util.Hashtable;

import org.apache.sling.commons.log.internal.LogManager;
import org.apache.sling.commons.log.internal.slf4j.LogConfigManager;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.osgi.service.cm.ConfigurationException;

public class GlobalConfigurationTest extends AbstractSlingConfigTest {
    
    public GlobalConfigurationTest() {
        MockitoAnnotations.initMocks(this);
    }    
    
    @Test
    public void testGlobalConfigorator() throws ConfigurationException {
        GlobalConfigurator g = new GlobalConfigurator();
        LogConfigManager lcm = LogConfigManager.getInstance();
        g.setLogConfigManager(lcm);
        Assert.assertSame(lcm, g.getLogConfigManager());
        Hashtable<String, Object> config = getGoodConfiguration();
        g.updated(config);
        Assert.assertTrue(config.containsKey(LogManager.LOG_LOGGERS));
    }

    @Test
    public void testGlobalConfigoratorFail() throws ConfigurationException {
        GlobalConfigurator g = new GlobalConfigurator();
        g.setLogConfigManager(LogConfigManager.getInstance());
        try {
            g.updated(getBadConfiguration());
            Assert.fail("Should have failed with a LOG_LEVEL not set");
        } catch ( ConfigurationException e ) {
            // good
        }
    }
 
}
