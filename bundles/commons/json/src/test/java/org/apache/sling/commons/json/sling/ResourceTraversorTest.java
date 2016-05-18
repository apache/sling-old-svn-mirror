/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.sling.commons.json.sling;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.junit.Test;

public class ResourceTraversorTest {
    private static final String RESOURCE_NAME = "testResource";
    private static final String PATH = "/" + RESOURCE_NAME;
    private static final String ID = "id";

    @Test
    public void testCollectResources() throws JSONException {
        // Currently this fails with -Dsling.test.ResourceTraversor.N = 20000
        final int N = Integer.getInteger("sling.test.ResourceTraversor.N", 1000);
        final int LEVELS = 10;
        Resource r = createResource(N);
        ResourceTraversor traversor = new ResourceTraversor(LEVELS, N * 10, r, true);
        traversor.collectResources();
        assertTraversalResult(traversor.getJSONObject(), N);
    }
    
    void assertTraversalResult(JSONObject jso, int nChildren) throws JSONException {
        for(int i=0; i < nChildren; i++) {
            final String key = "child" + i;
            assertTrue("Expecting " + key, jso.has(key));
            final JSONObject child = jso.getJSONObject(key);
            assertTrue("Expecting property " + ID, child.has(ID));
            assertEquals("Expecting value " + key, key, child.get(ID));
        }
    }

    Resource createResource(int numberOfChildren) {
        Resource resource = mock(Resource.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        ResourceResolver childResourceResolver = mock(ResourceResolver.class);

        final List<Resource> empty = new ArrayList<Resource>();
        when(childResourceResolver.listChildren(any(Resource.class))).thenReturn(empty.iterator());
        when(resource.getResourceResolver()).thenReturn(resourceResolver);
        when(resource.getPath()).thenReturn(PATH);

        final List<Resource> children = new ArrayList<Resource>();
        for(int i=0; i < numberOfChildren; i++) {
            final Map<String, Object> childProps = new HashMap<String, Object>();
            final String id = "child" + i;
            childProps.put(ID, id);
            final Resource r = mock(Resource.class);
            when(r.adaptTo(ValueMap.class)).thenReturn(new ValueMapDecorator(childProps));
            when(r.getResourceResolver()).thenReturn(childResourceResolver);
            when(r.getPath()).thenReturn(PATH + "/" + id);
            children.add(r);
        }
        when(resourceResolver.listChildren(any(Resource.class))).thenReturn(children.iterator());
        return resource;
    }
}
