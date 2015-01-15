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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class JsonObjectCreatorTest {
    
    @Mock
    private Resource resource;
    
    @Mock
    private ResourceResolver resourceResolver;  
    
    @Before
    public void setup() {
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put("foo",  "bar");
        final ValueMap values = new ValueMapDecorator(props);
        
        when(resource.getResourceResolver()).thenReturn(resourceResolver);
        when(resource.adaptTo(ValueMap.class)).thenReturn(values);
        final List<Resource> children = new ArrayList<Resource>();
        when(resourceResolver.listChildren(any(Resource.class))).thenReturn(children.iterator());
    }
    
    @Test
    public void testBasicJSON() throws JSONException {
        final JSONObject j = JsonObjectCreator.create(resource, 1);
        assertEquals("bar", j.get("foo"));
    }
}
