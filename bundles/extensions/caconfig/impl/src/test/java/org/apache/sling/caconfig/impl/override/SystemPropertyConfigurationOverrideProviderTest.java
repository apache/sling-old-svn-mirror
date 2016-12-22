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

import static org.apache.sling.caconfig.impl.override.SystemPropertyConfigurationOverrideProvider.SYSTEM_PROPERTY_PREFIX;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class SystemPropertyConfigurationOverrideProviderTest {

    @Rule
    public SlingContext context = new SlingContext();
    
    @Before
    public void setUp() {
        System.setProperty(SYSTEM_PROPERTY_PREFIX + "test/param1", "value1");
        System.setProperty(SYSTEM_PROPERTY_PREFIX + "[/content]test/param2", "value2");
    }

    @After
    public void tearDown() {
        System.clearProperty(SYSTEM_PROPERTY_PREFIX + "test/param1");
        System.clearProperty(SYSTEM_PROPERTY_PREFIX + "[/content]test/param2");
    }

    @Test
    public void testEnabled() {
        SystemPropertyConfigurationOverrideProvider provider = context.registerInjectActivateService(
                new SystemPropertyConfigurationOverrideProvider(),
                "enabled", true);

        Collection<String> overrides = provider.getOverrideStrings();
        assertTrue(overrides.contains("test/param1=value1"));
        assertTrue(overrides.contains("[/content]test/param2=value2"));
    }

    @Test
    public void testDisabled() {
        SystemPropertyConfigurationOverrideProvider provider = context.registerInjectActivateService(
                new SystemPropertyConfigurationOverrideProvider(),
                "enabled", false);

        Collection<String> overrides = provider.getOverrideStrings();
        assertTrue(overrides.isEmpty());
    }

}
