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
package org.apache.sling.superimposing.impl;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.apache.sling.api.adapter.AdapterManager;
import org.apache.sling.api.adapter.SlingAdaptable;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SuperimposingResourceTest {

    private Resource underTest;

    @Mock
    private Resource originalResource;
    @Mock
    private ResourceResolver resourceResolver;
    private ResourceMetadata resourceMetadata = new ResourceMetadata();

    private static final String ORIGINAL_PATH = "/root/path1";
    private static final String SUPERIMPOSED_PATH = "/root/path2";
    private static final String RESOURCE_TYPE = "/resourceType1";

    @Before
    public void setUp() {
        when(originalResource.getPath()).thenReturn(ORIGINAL_PATH);
        when(originalResource.getResourceType()).thenReturn(RESOURCE_TYPE);
        when(originalResource.getResourceSuperType()).thenReturn(null);
        when(originalResource.getResourceMetadata()).thenReturn(this.resourceMetadata);
        when(originalResource.getResourceResolver()).thenReturn(this.resourceResolver);
        resourceMetadata.setResolutionPath(ORIGINAL_PATH);
        underTest = new SuperimposingResource(this.originalResource, SUPERIMPOSED_PATH);
    }

    @Test
    public void testGetter() {
        assertEquals(SUPERIMPOSED_PATH, underTest.getPath());
        assertEquals(RESOURCE_TYPE, underTest.getResourceType());
        assertNull(underTest.getResourceSuperType());
        assertEquals(ORIGINAL_PATH, underTest.getResourceMetadata().getResolutionPath());
        assertSame(this.resourceResolver, underTest.getResourceResolver());
    }

    /**
     * Make sure adaptions are inherited from source resource, but can be overridden by superimposing resource instance.
     */
    @Test
    public void testAdaptTo() {
        SlingAdaptable.setAdapterManager(new AdapterManager() {
            @SuppressWarnings("unchecked")
            public <AdapterType> AdapterType getAdapter(Object adaptable, Class<AdapterType> type) {
                if (adaptable instanceof SuperimposingResource && type==String.class) {
                    return (AdapterType)"mystring";
                }
                return null;
            }
        });
        when(this.originalResource.adaptTo(String.class)).thenReturn("myoriginalstring");
        when(this.originalResource.adaptTo(Integer.class)).thenReturn(12345);

        assertEquals("mystring", underTest.adaptTo(String.class));
        assertEquals((Integer)12345, underTest.adaptTo(Integer.class));
        assertNull(underTest.adaptTo(Boolean.class));
    }

}
