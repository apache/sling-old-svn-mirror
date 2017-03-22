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
package org.apache.sling.caconfig.impl.override;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Map;

import org.apache.sling.caconfig.spi.ConfigurationOverrideProvider;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.Constants;

import com.google.common.collect.ImmutableMap;

public class ConfigurationOverrideMultiplexerImplTest {
    
    @Rule
    public SlingContext context = new SlingContext();
    
    private ConfigurationOverrideMultiplexerImpl underTest;
    
    @Before
    public void setUp() {
        underTest = context.registerInjectActivateService(new ConfigurationOverrideMultiplexerImpl());
    }

    @Test
    public void testWithNoProviders() {
        assertOverride("/a/b", "test",
                ImmutableMap.<String,Object>of("param1", "initialValue"),
                null);
    }

    @Test
    public void testWithMultipleProviders() {

        // 1st provider
        context.registerService(ConfigurationOverrideProvider.class, new DummyConfigurationOverrideProvider(
                "test/globalParam1=\"globalValue1\"",
                "[/a/b]test/param1=\"value1\""), Constants.SERVICE_RANKING, 200);

        // 2nd provider (may overwrite 1st one)
        context.registerService(ConfigurationOverrideProvider.class, new DummyConfigurationOverrideProvider(
                "test/globalParam1=\"globalValue1a\"",
                "[/a/b/c]test={\"param1\":\"value2\"}"), Constants.SERVICE_RANKING, 100);

        assertOverride("/a", "test",
                ImmutableMap.<String,Object>of("param1", "initialValue"),
                ImmutableMap.<String,Object>of("param1", "initialValue", "globalParam1", "globalValue1a"));

        assertOverride("/a/b", "test",
                ImmutableMap.<String,Object>of("param1", "initialValue"),
                ImmutableMap.<String,Object>of("param1", "value1", "globalParam1", "globalValue1a"));

        assertOverride("/a/b/c", "test",
                ImmutableMap.<String,Object>of("param1", "initialValue"),
                ImmutableMap.<String,Object>of("param1", "value2"));

        assertOverride("/a/b/c/d", "test",
                ImmutableMap.<String,Object>of("param1", "initialValue"),
                ImmutableMap.<String,Object>of("param1", "value2"));

        assertOverride("/a/b", "test2",
                ImmutableMap.<String,Object>of("param1", "initialValue"),
                null);
    }
    
    private void assertOverride(String path, String configName, Map<String,Object> input, Map<String,Object> output) {
        if (output == null) {
            assertNull(underTest.overrideProperties(path, configName, input));
        }
        else {
            assertEquals(output, underTest.overrideProperties(path, configName, input));
        }
    }

}
