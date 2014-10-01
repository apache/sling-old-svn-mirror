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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.testing.sling.MockResourceResolver;
import org.apache.sling.commons.testing.sling.MockSlingHttpServletRequest;
import org.apache.sling.rewriter.ProcessingContext;
import org.apache.sling.rewriter.ProcessorConfiguration;
import org.junit.Before;
import org.junit.Test;

/**
 * @author diru
 */
public class ProcessorConfigurationImplTest {

    private MockSlingHttpServletRequest mockRequestWithSelector;
    private MockSlingHttpServletRequest mockRequestWithoutSelector;

    @Before
    public void setup() {
        this.mockRequestWithSelector = new MockSlingHttpServletRequest("/content/path", "sel1.sel2", "xml", null, null);
        this.mockRequestWithoutSelector = new MockSlingHttpServletRequest("/content/path", null, "xml", null, null);
        // mock the resource resolver and create also a mocked resource to prevent NPE in ResourceUtil.
        final MockResourceResolver mockResourceResolver = new MockResourceResolver();
        mockResourceResolver.setSearchPath("/libs");
        this.mockRequestWithSelector.setResourceResolver(mockResourceResolver);
        this.mockRequestWithoutSelector.setResourceResolver(mockResourceResolver);
    }

    @Test
    public void testMatchContentTypeMismatch() {
        this.doTestAgainstProcessingContextWithSelector(new ProcessorConfigurationImpl(new String[] {"text/html",
            "text/plain" }, null, null, null, null), true);
    }

    @Test
    public void testMatchAtLeastOneContentType() {
        this.doTestAgainstProcessingContextWithSelector(new ProcessorConfigurationImpl(new String[] {"text/html",
                "text/xml" }, null, null, null, null));
    }

    @Test
    public void testMatchAnyContentType() {
        this.doTestAgainstProcessingContextWithSelector(new ProcessorConfigurationImpl(
                new String[] {"text/html", "*" }, null, null, null, null));
    }

    @Test
    public void testMatchExtensionMismatch() {
        this.doTestAgainstProcessingContextWithSelector(new ProcessorConfigurationImpl(null, null, new String[] {
            "html", "txt" }, null, null), true);
    }

    @Test
    public void testMatchAtLeastOneExtension() {
        this.doTestAgainstProcessingContextWithSelector(new ProcessorConfigurationImpl(null, null, new String[] {
                "html", "xml" }, null, null));
    }

    @Test
    public void testMatchResourceTypeMismatch() {
        this.doTestAgainstProcessingContextWithSelector(new ProcessorConfigurationImpl(null, null, null, new String[] {
            "a/b/c" }, null), true);
    }

    @Test
    public void testMatchAtLeastOneResourceType() {
        this.doTestAgainstProcessingContextWithSelector(new ProcessorConfigurationImpl(null, null, null, new String[] {
                "a/b/c", MockSlingHttpServletRequest.RESOURCE_TYPE }, null));
    }

    @Test
    public void testMatchPathMismatch() {
        this.doTestAgainstProcessingContextWithSelector(new ProcessorConfigurationImpl(null, new String[] {"/apps",
            "/var" }, null, null, null), true);
    }

    @Test
    public void testMatchAtLeastOnePath() {
        this.doTestAgainstProcessingContextWithSelector(new ProcessorConfigurationImpl(null, new String[] {"/libs",
                "/content" }, null, null, null));
    }

    @Test
    public void testMatchAnyPath() {
        this.doTestAgainstProcessingContextWithSelector(new ProcessorConfigurationImpl(null, new String[] {"/libs",
                "*" }, null, null, null));
    }

    @Test
    public void testMatchSelectorRequired() {
        this.doTestAgainstProcessingContextWithoutSelector(new ProcessorConfigurationImpl(null, null, null, null,
                new String[] {"sel" }), true);
    }

    @Test
    public void testMatchSelectorMismatch() {
        this.doTestAgainstProcessingContextWithSelector(new ProcessorConfigurationImpl(null, null, null, null,
            new String[] {"sel3" }), true);
    }

    @Test
    public void testMatchAtLeastOneSelector() {
        this.doTestAgainstProcessingContextWithSelector(new ProcessorConfigurationImpl(null, null, null, null,
            new String[] {"sel1" }));
    }

    private void doTestAgainstProcessingContextWithSelector(ProcessorConfiguration configuration) {
        this.doTestAgainstProcessingContextWithSelector(configuration, false);
    }

    private void doTestAgainstProcessingContextWithSelector(ProcessorConfiguration configuration, boolean negate) {
        // setup processing context
        ProcessingContext context = createProcessingContext(this.mockRequestWithSelector);
        // test the given configuration
        doTest(configuration, context, negate);
    }

    private void doTestAgainstProcessingContextWithoutSelector(ProcessorConfiguration configuration, boolean negate) {
        // setup processing context
        ProcessingContext context = createProcessingContext(this.mockRequestWithoutSelector);
        // test the given configuration
        doTest(configuration, context, negate);
    }

    private void doTest(ProcessorConfiguration configuration, ProcessingContext context, boolean negate) {
        if (!negate) {
            assertTrue(configuration.match(context));
        } else {
            assertFalse(configuration.match(context));
        }
    }

    private ProcessingContext createProcessingContext(SlingHttpServletRequest request) {
        final ProcessingContext context = mock(ProcessingContext.class);
        when(context.getContentType()).thenReturn("text/xml");
        when(context.getRequest()).thenReturn(request);

        return context;
    }
}
