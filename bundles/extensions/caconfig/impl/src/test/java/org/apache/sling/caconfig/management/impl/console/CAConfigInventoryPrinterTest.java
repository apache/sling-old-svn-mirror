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
package org.apache.sling.caconfig.management.impl.console;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.inventory.Format;
import org.apache.sling.caconfig.impl.ConfigurationTestUtils;
import org.apache.sling.caconfig.impl.def.DefaultConfigurationInheritanceStrategy;
import org.apache.sling.caconfig.impl.def.DefaultConfigurationPersistenceStrategy;
import org.apache.sling.caconfig.spi.ConfigurationMetadataProvider;
import org.apache.sling.caconfig.spi.ConfigurationOverrideProvider;
import org.apache.sling.caconfig.spi.metadata.ConfigurationMetadata;
import org.apache.sling.caconfig.spi.metadata.PropertyMetadata;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;

@RunWith(MockitoJUnitRunner.class)
public class CAConfigInventoryPrinterTest {
    
    private static final String SAMPLE_CONFIG_NAME = "sample.config.Name";
    private static final String SAMPLE_OVERRIDE_STRING = "[/sample]override/string='abc'";
    
    @Rule
    public SlingContext context = new SlingContext();
    
    @Mock
    private ConfigurationMetadataProvider configurationMetadataProvider;
    @Mock
    private ConfigurationOverrideProvider configurationOverrideProvider;
    
    private CAConfigInventoryPrinter underTest;
    
    @Before
    public void setUp() {
        context.registerService(ConfigurationMetadataProvider.class, configurationMetadataProvider);
        context.registerService(ConfigurationOverrideProvider.class, configurationOverrideProvider);
        ConfigurationTestUtils.registerConfigurationResolver(context);
        underTest = context.registerInjectActivateService(new CAConfigInventoryPrinter());
    
        ConfigurationMetadata configMetadata = new ConfigurationMetadata(SAMPLE_CONFIG_NAME, ImmutableList.<PropertyMetadata<?>>of(
                new PropertyMetadata<>("prop1", "defValue"),
                new PropertyMetadata<>("prop2", String.class),
                new PropertyMetadata<>("prop3", 5)),
                false);
        when(configurationMetadataProvider.getConfigurationMetadata(SAMPLE_CONFIG_NAME)).thenReturn(configMetadata);
        when(configurationMetadataProvider.getConfigurationNames()).thenReturn(ImmutableSortedSet.of(SAMPLE_CONFIG_NAME));
        
        when(configurationOverrideProvider.getOverrideStrings()).thenReturn(ImmutableList.of(SAMPLE_OVERRIDE_STRING));
    }

    @Test
    public void testPrintConfiguration() {
        StringWriter sw = new StringWriter();
        underTest.print(new PrintWriter(sw), Format.TEXT, false);
        String result = sw.toString();
        
        // test existance of some strategy names
        assertTrue(StringUtils.contains(result, DefaultConfigurationInheritanceStrategy.class.getName()));
        assertTrue(StringUtils.contains(result, DefaultConfigurationPersistenceStrategy.class.getName()));
        
        // ensure config metadata
        assertTrue(StringUtils.contains(result, SAMPLE_CONFIG_NAME));
        
        // ensure overrides strings
        assertTrue(StringUtils.contains(result, SAMPLE_OVERRIDE_STRING));
    }

}
