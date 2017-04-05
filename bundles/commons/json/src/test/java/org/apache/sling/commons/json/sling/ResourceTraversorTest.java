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

import java.util.Iterator;

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ResourceTraversorTest {
    private static final String RESOURCE_NAME = "R";
    private static final String ID = "id";

    // Before revision 1744381 the tests would fail with this set to 20'000
    private final int MANY = Integer.getInteger("sling.test.ResourceTraversor.count", 100000);
    
    private final int FEW = 5;
    private final int LEVELS = 3;
    private Resource root;
    
    @Rule
    public final SlingContext context = new SlingContext();
    
    private void addChildren(Resource parent, int resourcesPerLevel, int nLevels) throws PersistenceException {
        for(int i=0; i < resourcesPerLevel; i++) {
            final String id = makePath(nLevels, i);
            final Resource child = parent.getResourceResolver().create(parent, id, null);
            ModifiableValueMap vm = child.adaptTo(ModifiableValueMap.class);
            vm.put(ID,  id);
            child.getResourceResolver().commit();
            if(nLevels > 1) {
                addChildren(child, resourcesPerLevel, nLevels - 1);
            }
        }
    }
    
    private static String makePath(int nLevels, int index) {
        return RESOURCE_NAME + "_" + nLevels + "_" + index;
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
    
    public void createTree(int resourcesPerLevel, int depth) throws PersistenceException {
        root = context.resourceResolver().getResource("/");
        addChildren(root, resourcesPerLevel, depth);
    }
    
    @Test
    public void collectNLevelsNoLimit() throws JSONException, PersistenceException {
        createTree(FEW, LEVELS);
        ResourceTraversor traversor = new ResourceTraversor(-1, Integer.MAX_VALUE, root, true);
        traversor.collectResources();
        assertTraversalResult(root.getPath(), traversor.getJSONObject(), FEW, LEVELS);
    }
    
    @Test
    public void collectNLevelsWithLimit() throws JSONException, PersistenceException {
        createTree(FEW, LEVELS);
        final String [] ids = { "R_3_0", "R_3_1" };
        final int limit = ids.length;
        ResourceTraversor traversor = new ResourceTraversor(-1, limit, root, true);
        traversor.collectResources();
        final int expectedCount = limit + 1;
        assertEquals(expectedCount, traversor.getCount());
        final JSONObject jso = traversor.getJSONObject();
        for(String id : ids) {
            assertTrue("Expecting " + id + " on " + describe(jso), jso.has(id));
        }
    }
    
    @Test
    public void collectWithLimitInChildren() throws JSONException, PersistenceException {
        createTree(2, 2);
        final String [] ids = { "R_2_0", "R_2_1" };
        final int limit = ids.length;
        ResourceTraversor traversor = new ResourceTraversor(-1, limit, root, true);
        traversor.collectResources();
        final int expectedCount = limit + 1;
        assertEquals(expectedCount, traversor.getCount());
        final JSONObject jso = traversor.getJSONObject();
        for(String id : ids) {
            assertTrue("Expecting " + id + " on " + describe(jso), jso.has(id));
        }
    }
    
    @Test
    public void collectOneLevelNoLimit() throws JSONException, PersistenceException {
        createTree(MANY, 1);
        ResourceTraversor traversor = new ResourceTraversor(1, MANY * 10, root, true);
        traversor.collectResources();
        assertTraversalResult(root.getPath(), traversor.getJSONObject(), MANY, 1);
        assertEquals(MANY, traversor.getCount());
    }
    
    @Test
    public void collectOneLevelLimitIgnoredAtLevelOne() throws JSONException, PersistenceException {
        createTree(MANY, 1);
        ResourceTraversor traversor = new ResourceTraversor(1, 1, root, true);
        traversor.collectResources();
        assertTraversalResult(root.getPath(), traversor.getJSONObject(), MANY, 1);
        assertEquals(MANY, traversor.getCount());
    }
    
    void assertTraversalResult(String parentPath, JSONObject jso, int childrenPerLevel, int nLevels) throws JSONException {
        for(int i=0; i < childrenPerLevel; i++) {
            final String key = makePath(nLevels, i);
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
        // Sling mocks add an extra property
        assertEquals("Expecting " + childrenPerLevel + " keys on " + describe(jso), childrenPerLevel + 1, keysCount);
    }
}