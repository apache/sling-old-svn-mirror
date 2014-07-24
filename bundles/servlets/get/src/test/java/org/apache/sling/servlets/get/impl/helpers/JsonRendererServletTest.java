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
package org.apache.sling.servlets.get.impl.helpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class JsonRendererServletTest {
    
    private SlingHttpServletRequest request;
    private SlingHttpServletResponse response;
    private String [] selectors;
    private JsonRendererServlet jrs;
    
    @Before
    public void setup() {
        request = Mockito.mock(SlingHttpServletRequest.class);
        
        final RequestPathInfo rpi = Mockito.mock(RequestPathInfo.class);
        Mockito.when(request.getRequestPathInfo()).thenReturn(rpi);
        Mockito.when(rpi.getSelectors()).thenAnswer(new Answer<String[]>(){
            public String [] answer(InvocationOnMock invocation) {
                return selectors;
            }
        });
        
        final Resource resource = Mockito.mock(Resource.class);
        Mockito.when(request.getResource()).thenReturn(resource);
        
        response = Mockito.mock(SlingHttpServletResponse.class);
        
        jrs = new JsonRendererServlet(42);
    }
    
    @Test
    public void testRecursionLevelA() {
        selectors = new String[] { "12" };
        assertEquals(12, jrs.getMaxRecursionLevel(request));
        assertFalse(jrs.isTidy(request));
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testRecursionLevelB() {
        selectors = new String[] { "42", "more" };
        jrs.getMaxRecursionLevel(request);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testRecursionLevelC() {
        selectors = new String[] { "more" };
        jrs.getMaxRecursionLevel(request);
    }
    
    @Test
    public void testRecursionLevelD() {
        selectors = new String[] { "tidy" };
        assertEquals(0, jrs.getMaxRecursionLevel(request));
    }

    @Test
    public void testRecursionLevelE() {
        // Level must be the last selector but
        // if the last selector is "tidy" there's
        // no error. "for historical reasons"
        selectors = new String[] { "46", "tidy" };
        assertEquals(0, jrs.getMaxRecursionLevel(request));
    }

    @Test
    public void testRecursionLevelF() {
        selectors = new String[] { "tidy", "45" };
        assertEquals(45, jrs.getMaxRecursionLevel(request));
        assertTrue(jrs.isTidy(request));
    }
    
    @Test
    public void testBadRequest() throws IOException {
        selectors = new String[] { "bad", "selectors" };
        jrs.doGet(request, response);
        Mockito.verify(response, Mockito.times(1)).sendError(Matchers.anyInt(), Matchers.anyString());
    }
}