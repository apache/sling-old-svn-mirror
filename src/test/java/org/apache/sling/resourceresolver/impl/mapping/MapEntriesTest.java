/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.resourceresolver.impl.mapping;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.EventAdmin;

public class MapEntriesTest {

    private MapEntries mapEntries;

    @Mock
    private MapConfigurationProvider resourceResolverFactory;

    @Mock
    private BundleContext bundleContext;

    @Mock
    private ResourceResolver resourceResolver;

    @Mock
    private EventAdmin eventAdmin;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(resourceResolverFactory.getAdministrativeResourceResolver(null)).thenReturn(resourceResolver);
        when(resourceResolver.findResources(anyString(), eq("sql"))).thenReturn(
                Collections.<Resource> emptySet().iterator());

        mapEntries = new MapEntries(resourceResolverFactory, bundleContext, eventAdmin);

    }

    @Test
    public void test_simple_alias_support() {
        Resource parent = mock(Resource.class);
        when(parent.getPath()).thenReturn("/parent");

        final Resource result = mock(Resource.class);
        when(result.getParent()).thenReturn(parent);
        when(result.getPath()).thenReturn("/parent/child");
        when(result.getName()).thenReturn("child");
        when(result.adaptTo(ValueMap.class)).thenReturn(singletonValueMap("sling:alias", "alias"));

        when(resourceResolver.findResources(anyString(), eq("sql"))).thenAnswer(new Answer<Iterator<Resource>>() {

            public Iterator<Resource> answer(InvocationOnMock invocation) throws Throwable {
                if (invocation.getArguments()[0].toString().contains("sling:alias")) {
                    return Collections.singleton(result).iterator();
                } else {
                    return Collections.<Resource> emptySet().iterator();
                }
            }
        });

        mapEntries.doInit();

        Map<String, String> aliasMap = mapEntries.getAliasMap("/parent");
        assertNotNull(aliasMap);
        assertTrue(aliasMap.containsKey("alias"));
        assertEquals("child", aliasMap.get("alias"));
    }

    @Test
    public void test_that_duplicate_alias_doesnt_replace_first_alias() {
        Resource parent = mock(Resource.class);
        when(parent.getPath()).thenReturn("/parent");

        final Resource result = mock(Resource.class);
        when(result.getParent()).thenReturn(parent);
        when(result.getPath()).thenReturn("/parent/child");
        when(result.getName()).thenReturn("child");
        when(result.adaptTo(ValueMap.class)).thenReturn(singletonValueMap("sling:alias", "alias"));

        final Resource secondResult = mock(Resource.class);
        when(secondResult.getParent()).thenReturn(parent);
        when(secondResult.getPath()).thenReturn("/parent/child2");
        when(secondResult.getName()).thenReturn("child2");
        when(secondResult.adaptTo(ValueMap.class)).thenReturn(singletonValueMap("sling:alias", "alias"));

        when(resourceResolver.findResources(anyString(), eq("sql"))).thenAnswer(new Answer<Iterator<Resource>>() {

            public Iterator<Resource> answer(InvocationOnMock invocation) throws Throwable {
                if (invocation.getArguments()[0].toString().contains("sling:alias")) {
                    return Arrays.asList(result, secondResult).iterator();
                } else {
                    return Collections.<Resource> emptySet().iterator();
                }
            }
        });

        mapEntries.doInit();

        Map<String, String> aliasMap = mapEntries.getAliasMap("/parent");
        assertNotNull(aliasMap);
        assertTrue(aliasMap.containsKey("alias"));
        assertEquals("child", aliasMap.get("alias"));
    }

    private ValueMap singletonValueMap(String key, String value) {
        return new ValueMapDecorator(Collections.<String, Object> singletonMap(key, value));
    }

}
