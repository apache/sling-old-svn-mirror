/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.hc.sling.impl;

import java.util.Arrays;
import java.util.List;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ServletTagsSelectionTest {
    
    @Mock
    private SlingHttpServletRequest request;
    
    @Mock
    private RequestPathInfo requestPathInfo;
    
    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        Mockito.when(request.getRequestPathInfo()).thenReturn(requestPathInfo);
    }

    private void assertTags(String [] actual, String ... expected) {
        assertEquals("Expecting " + expected.length + " tags", expected.length, actual.length);
        final List<String> actualList = Arrays.asList(actual);
        for(String tag : expected) {
            assertTrue("Expecting " + actualList + " to contain " + tag, actualList.contains(tag));
        }
    }
    
    @Test
    public void testNoSelectors() {
        Mockito.when(requestPathInfo.getSelectors()).thenReturn(new String [] {});
        assertTags(SlingHealthCheckServlet.getRuleTagsFromRequest(request), new String[] {});
    }
    
    @Test
    public void testNoHcSelector() {
        final String [] selectors = new String [] { "foo", "bar" };
        Mockito.when(requestPathInfo.getSelectors()).thenReturn(selectors);
        assertTags(SlingHealthCheckServlet.getRuleTagsFromRequest(request), selectors);
    }
    
    @Test
    public void testWithHcSelector() {
        final String [] selectors = new String [] { "foo", "bar", SlingHealthCheckServlet.HC_SELECTOR };
        Mockito.when(requestPathInfo.getSelectors()).thenReturn(selectors);
        final String [] expected = new String [] { "foo", "bar" };
        assertTags(SlingHealthCheckServlet.getRuleTagsFromRequest(request), expected);
    }
    
}
