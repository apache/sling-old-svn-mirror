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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.xss.XSSAPI;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class HtmlRendererServletTest {

    private SlingHttpServletRequest request;
    private SlingHttpServletResponse response;

    @Before
    public void setup() throws IOException {
        request = Mockito.mock(SlingHttpServletRequest.class);

        final ResourceResolver resolver = Mockito.mock(ResourceResolver.class);
        Mockito.when(request.getResourceResolver()).thenReturn(resolver);

        final Resource resource = Mockito.mock(Resource.class);
        Mockito.when(request.getResource()).thenReturn(resource);
        Mockito.when(resource.getResourceResolver()).thenReturn(resolver);

        final Map<String, Object> props = new HashMap<>();
        props.put("key", "<script>alert(1);</script>");
        Mockito.when(resource.adaptTo(Map.class)).thenReturn(props);

        response = Mockito.mock(SlingHttpServletResponse.class);

        Mockito.when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
    }

    @Test
    public void testEscaping() throws ServletException, IOException {
        XSSAPI xss = Mockito.mock(XSSAPI.class);

        final HtmlRendererServlet servlet = new HtmlRendererServlet(xss);

        servlet.doGet(request, response);

        Mockito.verify(xss).encodeForHTML("<script>alert(1);</script>");
    }
}