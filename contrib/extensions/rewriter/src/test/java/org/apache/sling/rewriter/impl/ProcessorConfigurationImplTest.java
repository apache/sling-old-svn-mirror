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
package org.apache.sling.rewriter.impl;

import static org.apache.sling.rewriter.impl.ProcessorConfigurationImpl.PROPERTY_CONTENT_TYPES;
import static org.apache.sling.rewriter.impl.ProcessorConfigurationImpl.PROPERTY_EXTENSIONS;
import static org.apache.sling.rewriter.impl.ProcessorConfigurationImpl.PROPERTY_PATHS;
import static org.apache.sling.rewriter.impl.ProcessorConfigurationImpl.PROPERTY_RESOURCE_TYPES;
import static org.apache.sling.rewriter.impl.ProcessorConfigurationImpl.PROPERTY_SELECTORS;
import static org.apache.sling.rewriter.impl.ProcessorConfigurationImpl.PROPERTY_UNWRAP_RESOURCES;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceWrapper;
import org.apache.sling.rewriter.ProcessingContext;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableMap;

@RunWith(MockitoJUnitRunner.class)
public class ProcessorConfigurationImplTest {
    
    @Rule
    public SlingContext context = new SlingContext();
    
    @Mock
    private ProcessingContext processingContext;

    @Before
    public void setup() {
        when(processingContext.getContentType()).thenReturn("text/html");
        when(processingContext.getRequest()).thenReturn(context.request());
        when(processingContext.getResponse()).thenReturn(context.response());
    }
    
    private ProcessorConfigurationImpl buildConfig(Map<String,Object> configProps) {
        Resource configResoruce = context.create().resource("/apps/myapp/rewriter/config", configProps);
        return new ProcessorConfigurationImpl(configResoruce);
    }
    
    private void assertMatch(Map<String,Object> configProps) {
        assertTrue(buildConfig(configProps).match(processingContext));
    }

    private void assertNoMatch(Map<String,Object> configProps) {
        assertFalse(buildConfig(configProps).match(processingContext));
    }

    @Test
    public void testMatchContentTypeMismatch() {
        assertNoMatch(ImmutableMap.<String,Object>of(PROPERTY_CONTENT_TYPES, new String[] {"text/xml", "text/plain"}));
    }

    @Test
    public void testMatchAtLeastOneContentType() {
        assertMatch(ImmutableMap.<String,Object>of(PROPERTY_CONTENT_TYPES, new String[] {"text/html", "text/xml"}));
    }

    @Test
    public void testMatchAnyContentType() {
        assertMatch(ImmutableMap.<String,Object>of(PROPERTY_CONTENT_TYPES, new String[] {"text/xml", "*"}));
    }

    @Test
    public void testMatchExtensionMismatch() {
        context.requestPathInfo().setExtension("html");
        assertNoMatch(ImmutableMap.<String,Object>of(PROPERTY_EXTENSIONS, new String[] {"xml","txt"}));
    }

    @Test
    public void testMatchAtLeastOneExtension() {
        context.requestPathInfo().setExtension("html");
        assertMatch(ImmutableMap.<String,Object>of(PROPERTY_EXTENSIONS, new String[] {"xml","html"}));
    }

    @Test
    public void testMatchResourceTypeMismatch() {
        context.currentResource(context.create().resource("/content/test",
                ImmutableMap.<String, Object>of("sling:resourceType", "type/1")));
        assertNoMatch(ImmutableMap.<String,Object>of(PROPERTY_RESOURCE_TYPES, new String[] {"type/2","type/3"}));
    }

    @Test
    public void testMatchAtLeastOneResourceType() {
        context.currentResource(context.create().resource("/content/test",
                ImmutableMap.<String, Object>of("sling:resourceType", "type/1")));
        assertMatch(ImmutableMap.<String,Object>of(PROPERTY_RESOURCE_TYPES, new String[] {"type/1","type/2"}));
    }

    @Test
    public void testMatchAtLeastOneResourceTypeWithResourceWrapper_UnwrapDisabled() {
        Resource resource = context.create().resource("/content/test", ImmutableMap.<String, Object>of("sling:resourceType", "type/1"));
        
        // overwrite resource type via ResourceWrapper
        Resource resourceWrapper = new ResourceWrapper(resource) {
            @Override
            public String getResourceType() { return "/type/override/1"; }
        };
        context.currentResource(resourceWrapper);
        
        assertNoMatch(ImmutableMap.<String,Object>of(PROPERTY_RESOURCE_TYPES, new String[] {"type/1","type/2"}));
    }

    @Test
    public void testMatchAtLeastOneResourceTypeWithResourceWrapper_UnwrapEnabled() {
        Resource resource = context.create().resource("/content/test", ImmutableMap.<String, Object>of("sling:resourceType", "type/1"));
        
        // overwrite resource type via ResourceWrapper
        Resource resourceWrapper = new ResourceWrapper(resource) {
            @Override
            public String getResourceType() { return "/type/override/1"; }
        };
        context.currentResource(resourceWrapper);
        
        assertMatch(ImmutableMap.<String,Object>of(PROPERTY_RESOURCE_TYPES, new String[] {"type/1","type/2"},
                PROPERTY_UNWRAP_RESOURCES, true));
    }

    @Test
    public void testMatchPathMismatch() {
        context.requestPathInfo().setResourcePath("/content/test");
        assertNoMatch(ImmutableMap.<String,Object>of(PROPERTY_PATHS, new String[] {"/apps","/var"}));
    }

    @Test
    public void testMatchAtLeastOnePath() {
        context.requestPathInfo().setResourcePath("/content/test");
        assertMatch(ImmutableMap.<String,Object>of(PROPERTY_PATHS, new String[] {"/apps","/content"}));
    }

    @Test
    public void testMatchAnyPath() {
        context.requestPathInfo().setResourcePath("/content/test");
        assertMatch(ImmutableMap.<String,Object>of(PROPERTY_PATHS, new String[] {"/apps","*"}));
    }

    @Test
    public void testMatchSelectorRequired() {
        assertNoMatch(ImmutableMap.<String,Object>of(PROPERTY_SELECTORS, new String[] {"sel"}));
    }

    @Test
    public void testMatchSelectorMismatch() {
        context.requestPathInfo().setSelectorString("sel1.sel2");
        assertNoMatch(ImmutableMap.<String,Object>of(PROPERTY_SELECTORS, new String[] {"sel3"}));
    }

    @Test
    public void testMatchAtLeastOneSelector() {
        context.requestPathInfo().setSelectorString("sel1.sel2");
        assertMatch(ImmutableMap.<String,Object>of(PROPERTY_SELECTORS, new String[] {"sel1"}));
    }

}
