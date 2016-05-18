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
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class ResourceTraversorTest {
    private static final String RESOURCE_NAME = "R";
    private static final String PATH = "/" + RESOURCE_NAME;
    private static final String ID = "id";

    // Several tests currently fail with stack overflow with -Dsling.test.ResourceTraversor.count=20000
    private final int MANY = Integer.getInteger("sling.test.ResourceTraversor.count", 10);
    private final int FEW = 5;
    private final int LEVELS = 3;
    private Resource root;
    
    private void addChildren(Resource parent, int resourcesPerLevel, int nLevels) {
        final List<Resource> empty = new ArrayList<Resource>();
        final List<Resource> children = new CopyOnWriteArrayList<Resource>();
        ResourceResolver childResourceResolver = mock(ResourceResolver.class);
        for(int i=0; i < resourcesPerLevel; i++) {
            final Map<String, Object> childProps = new HashMap<String, Object>();
            final String id = makePath(parent.getPath(), i);
            childProps.put(ID, id);
            final Resource r = mock(Resource.class);
            when(r.adaptTo(ValueMap.class)).thenReturn(new ValueMapDecorator(childProps));
            when(r.getResourceResolver()).thenReturn(childResourceResolver);
            when(r.getPath()).thenReturn(PATH + "/" + id);
            children.add(r);
            when(r.getResourceResolver().listChildren(any(Resource.class))).thenReturn(empty.iterator());
        }
        when(parent.getResourceResolver().listChildren(any(Resource.class))).thenReturn(children.iterator());
    }
    
    private static String makePath(String parentPath, int index) {
        return parentPath.replaceAll("/", "_") + "_" + index;
    }
    
    private String describe(JSONObject o) {
        int maxKeys = 5;
        final StringBuilder b = new StringBuilder();
        b.append("JSONOBject having ");
        final Iterator<String> k = o.keys();
        int count = 0;
        if(!k.hasNext()) {
            b.append("no properties");
        }
        while(k.hasNext()) {
            b.append(k.next()).append(",");
            if(++count >= maxKeys) {
                b.append("...");
                break;
            }
        }
        return b.toString();
    }
    
    @Before
    public void setup() {
        root = null;
    }
    
    public void createTree(int resourcesPerLevel, int depth) {
        root = mock(Resource.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        when(root.getResourceResolver()).thenReturn(resourceResolver);
        when(root.getPath()).thenReturn(PATH);
        addChildren(root, resourcesPerLevel, depth);
    }
    
    @Test
    @Ignore("Need to generate a tree of resources")
    public void collectNLevelsNoLimit() throws JSONException {
        createTree(FEW, LEVELS);
        ResourceTraversor traversor = new ResourceTraversor(-1, Integer.MAX_VALUE, root, true);
        traversor.collectResources();
        assertTraversalResult(root.getPath(), traversor.getJSONObject(), FEW, LEVELS);
    }
    
    @Test
    public void collectOneLevelNoLimit() throws JSONException {
        createTree(MANY, 1);
        ResourceTraversor traversor = new ResourceTraversor(1, MANY * 10, root, true);
        traversor.collectResources();
        assertTraversalResult(root.getPath(), traversor.getJSONObject(), MANY, 1);
    }
    
    @Test
    public void collectOneLevelLimitIgnoredAtLevelOne() throws JSONException {
        createTree(MANY, 1);
        ResourceTraversor traversor = new ResourceTraversor(1, 1, root, true);
        traversor.collectResources();
        assertTraversalResult(root.getPath(), traversor.getJSONObject(), MANY, 1);
    }
    
    void assertTraversalResult(String parentPath, JSONObject jso, int childrenPerLevel, int nLevels) throws JSONException {
        for(int i=0; i < childrenPerLevel; i++) {
            final String key = makePath(parentPath, i);
            assertTrue("Expecting " + key + " on " + describe(jso), jso.has(key));
            final JSONObject child = jso.getJSONObject(key);
            assertTrue("Expecting property " + ID, child.has(ID));
            assertEquals("Expecting value " + key, key, child.get(ID));
            if(nLevels > 1) {
                assertTraversalResult(key, child, childrenPerLevel, nLevels - 1);
            }
        }
        int keysCount = 0;
        final Iterator<String> k = jso.keys();
        while(k.hasNext()) {
            k.next();
            keysCount++;
        }
        assertEquals("Expecting " + childrenPerLevel + " keys on " + describe(jso), childrenPerLevel, keysCount);
    }
}