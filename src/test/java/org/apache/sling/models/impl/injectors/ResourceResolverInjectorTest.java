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
package org.apache.sling.models.impl.injectors;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Test;

public class ResourceResolverInjectorTest {

    private ResourceResolverInjector injector = new ResourceResolverInjector();

    @Test
    public void testFromResource() {
        Resource resource = mock(Resource.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        when(resource.getResourceResolver()).thenReturn(resourceResolver);

        Object result = injector.getValue(resource, "resourceResolver", null, null, null);
        assertEquals(resourceResolver, result);
    }

    @Test
    public void testFromRequest() {
        SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        when(request.getResourceResolver()).thenReturn(resourceResolver);

        Object result = injector.getValue(request, "resourceResolver", null, null, null);
        assertEquals(resourceResolver, result);
    }

    @Test
    public void testFromSomethingElse() {
        SlingHttpServletResponse response = mock(SlingHttpServletResponse.class);

        Object result = injector.getValue(response, "resourceResolver", null, null, null);
        assertNull(result);
    }

}
