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
package org.apache.sling.servlet.resolver.helper;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.servlet.resolver.mock.MockResource;

public class LocationUtilGetServletsTest extends LocationTestBase {

    private String label;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        label = ResourceUtil.getName(resourceTypePath);
    }
    
    public void testGetServlets0() {
        String[] names = { "/" + label + ".esp", // 0
                "/GET.esp", // 1
                "/" + label + ".html.esp", // 2
                "/html.esp", // 3
                "/print.esp", // 4
                "/print/a4.esp", // 5
                "/print.html.esp", // 6
                "/print/a4.html.esp", // 7
        };
        
        int[] baseIdxs = { 0, 1, 1, 0, 0, 1, 0, 1 };
        int[] indices  = { 7, 5, 6, 4, 2, 3, 0, 1 };
        
        effectiveTest(names, baseIdxs, indices);
    }
    
    public void testGetServlets1() {
        String[] names = { "/" + label + ".esp", // 0
                "/GET.esp", // 1
                "/" + label + ".html.esp", // 2
                "/print.esp", // 3
                "/print.other.esp", // 4
                "/print/other.esp", // 5
                "/print.html.esp", // 6
                "/print/a4.html.esp", // 7
        };
        
        int[] baseIdxs = { 0, 1, 0, 1, 0, 1, 0, 1, 0, 1 };
        int[] indices = { 7, 6, 3, 2, 0, 1 };
        
        effectiveTest(names, baseIdxs, indices);
    }
    
    public void testGetServlets2() {
        String[] names = { "/" + label + ".esp", // 0
                "/GET.esp", // 1
                "/" + label + ".html.esp", // 2
                "/html.esp", // 3
                "/image.esp", // 4
                "/print/other.esp", // 5
                "/print.other.esp", // 6
                "/print.html.esp", // 7
                "/print/a4.html.esp", // 8
        };
        
        int[] baseIdxs = { 0, 1, 0, 1, 0, 1, 0, 1, 0, 1 };
        int[] indices = { 8, 7, 2, 3, 0, 1 };
        
        effectiveTest(names, baseIdxs, indices);
    }
    
    public void testGetServlets3() {
        String[] names = { ".servlet", // 0
                "/" + label + ".esp", // 1
                "/GET.esp", // 2
                "/" + label + ".html.esp", // 3
                "/html.esp", // 4
                "/image.esp", // 5
                "/print/other.esp", // 6
                "/print.other.esp", // 7
                "/print.html.esp", // 8
                "/print/a4.html.esp", // 9
        };
        
        int[] baseIdxs = { 0, 1, 0, 1, 0, 1, 0, 1, 0, 1 };
        int[] indices = { 9, 8, 3, 4, 1, 2, 0 };
        
        effectiveTest(names, baseIdxs, indices);
    }
    
    public void testGetServlets4() {
        String[] names = { ".servlet", // 0
                "/" + label + ".esp", // 1
                "/GET.esp", // 2
                "/" + label + ".html.esp", // 3
                "/html.esp", // 4
                ".esp", // 5
                "/image.esp", // 6
                "/print/other.esp", // 7
                "/print.other.esp", // 8
                "/print.html.esp", // 9
                "/print/a4.html.esp", // 10
        };
        
        int[] baseIdxs = { 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0 };
        int[] indices = { 10, 9, 3, 4, 1, 2, 0 };
        
        effectiveTest(names, baseIdxs, indices);
    }
    
    protected void effectiveTest(String[] names, int[] baseIdxs, int[] indices) {

        String[] base = { "/apps/" + resourceTypePath,
            "/libs/" + resourceTypePath };

        @SuppressWarnings("unchecked")
        Map<String, String> pathMap = new HashMap<String, String>();

        for (int i=0; i < names.length; i++) {
            String name = names[i];
            int baseIdx = baseIdxs[i];
            String path = base[baseIdx] + name;
            createScriptResource(path, "nt:file");
            pathMap.put(name, path);
        }
        
        LocationUtil lu = LocationUtil.create(request);
        Collection<LocationResource> res = lu.getScripts(request);
        Iterator<LocationResource> rIter = res.iterator();

        for (int index : indices) {
            assertTrue(rIter.hasNext());

            LocationResource lr = rIter.next();

            String name = names[index];
            String path = pathMap.get(name);

            assertEquals(path, lr.getResource().getPath());
        }

        assertFalse(rIter.hasNext());
    }

    protected MockResource createScriptResource(String path, String type) {
        MockResource res = new MockResource(resourceResolver, path, type);
        resourceResolver.addResource(res);
        return res;
    }
}
