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
package org.apache.sling.caconfig.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.SortedSet;

import javax.script.Bindings;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.caconfig.ConfigurationBuilder;
import org.apache.sling.caconfig.impl.metadata.ConfigurationMetadataProviderMultiplexer;
import org.apache.sling.caconfig.spi.ConfigurationMetadataProvider;
import org.apache.sling.caconfig.spi.metadata.ConfigurationMetadata;
import org.apache.sling.caconfig.spi.metadata.PropertyMetadata;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("unchecked")
public class ConfigurationBindingsValueProviderTest {

    private final static ValueMap VALUEMAP = new ValueMapDecorator(
            ImmutableMap.<String, Object> of("param1", "value1"));
    
    private static final SortedSet<String> CONFIG_NAMES = ImmutableSortedSet.of("name1", "name.2");

    @Rule
    public SlingContext context = new SlingContext();

    @Mock
    private SlingHttpServletRequest request;
    @Mock
    private Resource resource;
    @Mock
    private Bindings bindings;
    @Mock
    private ConfigurationBuilder configBuilder;
    @Mock
    private ConfigurationMetadataProvider configMetadataProvider;

    private ConfigurationBindingsValueProvider underTest;

    @Before
    public void setUp() {
        context.registerInjectActivateService(new ConfigurationMetadataProviderMultiplexer());
        context.registerService(ConfigurationMetadataProvider.class, configMetadataProvider);
        when(configMetadataProvider.getConfigurationNames()).thenReturn(CONFIG_NAMES);
        
        when(bindings.containsKey(SlingBindings.REQUEST)).thenReturn(true);
        when(bindings.get(SlingBindings.REQUEST)).thenReturn(request);
        when(request.getResource()).thenReturn(resource);
        when(resource.adaptTo(ConfigurationBuilder.class)).thenReturn(configBuilder);
        when(configBuilder.name(anyString())).thenReturn(configBuilder);
        when(configBuilder.asValueMap()).thenReturn(VALUEMAP);
        when(configBuilder.asValueMapCollection()).thenReturn(ImmutableList.of(VALUEMAP));
        
        when(configMetadataProvider.getConfigurationMetadata("name1")).thenReturn(
                new ConfigurationMetadata("name1", ImmutableList.<PropertyMetadata<?>>of(), false));
        when(configMetadataProvider.getConfigurationMetadata("name.2")).thenReturn(
                new ConfigurationMetadata("name.2", ImmutableList.<PropertyMetadata<?>>of(), true));
    }

    @Test
    public void testWithConfig() {
        underTest = context.registerInjectActivateService(new ConfigurationBindingsValueProvider(), "enabled", true);
        underTest.addBindings(bindings);

        ArgumentCaptor<Map<String, ValueMap>> configMapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(bindings).put(eq(ConfigurationBindingsValueProvider.BINDING_VARIABLE), configMapCaptor.capture());
        
        Map<String, ValueMap> configMap = configMapCaptor.getValue();
        assertEquals(CONFIG_NAMES, configMap.keySet());
        assertEquals(VALUEMAP, configMap.get("name1"));
        assertEquals(ImmutableList.of(VALUEMAP), configMap.get("name.2"));
    }

    @Test
    public void testNoResource() {
        when(request.getResource()).thenReturn(null);
        underTest = context.registerInjectActivateService(new ConfigurationBindingsValueProvider(), "enabled", true);
        underTest.addBindings(bindings);
        verify(bindings, never()).put(anyString(), any(Object.class));
    }

    @Test
    public void testNoRequest() {
        underTest = context.registerInjectActivateService(new ConfigurationBindingsValueProvider(), "enabled", true);
        when(bindings.containsKey(SlingBindings.REQUEST)).thenReturn(false);
        underTest.addBindings(bindings);
        verify(bindings, never()).put(anyString(), any(Object.class));
    }

    @Test
    public void testDisabled() {
        underTest = context.registerInjectActivateService(new ConfigurationBindingsValueProvider(), "enabled", false);
        underTest.addBindings(bindings);
        verify(bindings, never()).put(anyString(), any(Object.class));
    }

}
