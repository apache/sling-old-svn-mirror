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

import org.apache.sling.caconfig.management.ConfigurationManagementSettings;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;

public class ConfigurationManagementSettingsImplTest {

    @Rule
    public SlingContext context = new SlingContext();
    
    @Before
    public void setUp() {
        
    }
    
    @Test
    public void testDefault() {
        ConfigurationManagementSettings underTest = context.registerInjectActivateService(new ConfigurationManagementSettingsImpl());
        
        assertEquals(ImmutableSet.<String>of(), underTest.getIgnoredPropertyNames(ImmutableSet.<String>of()));
        assertEquals(ImmutableSet.<String>of(), underTest.getIgnoredPropertyNames(ImmutableSet.<String>of("abc", "def")));
        assertEquals(ImmutableSet.<String>of("jcr:xyz", "jcr:def"), underTest.getIgnoredPropertyNames(ImmutableSet.<String>of("abc", "jcr:xyz", "jcr:def")));
    }

    @Test
    public void testCustomConfig() {
        ConfigurationManagementSettings underTest = context.registerInjectActivateService(new ConfigurationManagementSettingsImpl(),
                "ignorePropertyNameRegex", new String[] { "^.*e.*$", "^.*b.*$" });
        
        assertEquals(ImmutableSet.<String>of(), underTest.getIgnoredPropertyNames(ImmutableSet.<String>of()));
        assertEquals(ImmutableSet.<String>of("abc", "def"), underTest.getIgnoredPropertyNames(ImmutableSet.<String>of("abc", "def")));
        assertEquals(ImmutableSet.<String>of("abc", "jcr:def"), underTest.getIgnoredPropertyNames(ImmutableSet.<String>of("abc", "jcr:xyz", "jcr:def")));
    }

}
