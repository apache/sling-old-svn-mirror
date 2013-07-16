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
package org.apache.sling.launchpad.testservices.serversidetests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.HashMap;

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.junit.annotations.SlingAnnotationsTestRunner;
import org.apache.sling.junit.annotations.TestReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Test (and demonstrate) the Sling Resource CRUD functionality */
@RunWith(SlingAnnotationsTestRunner.class)
public class WriteableResourcesTest {
    @TestReference
    private ResourceResolverFactory resourceResolverFactory;
    
    private ResourceResolver resolver;
    private Resource testRoot;
    
    @SuppressWarnings("serial")
    private static class Props extends HashMap<String, Object> {
        Props(String ... keyValue) {
            for(int i=0 ; i< keyValue.length; i+=2) {
                put(keyValue[i], keyValue[i+1]);
            }
        }
    }
    
    @Before
    public void setup() throws Exception {
        resolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
        assertNotNull("Expecting non-null ResourceResolver", resolver);
        final Resource root = resolver.getResource("/");
        assertNotNull("Expecting non-null root Resource", root);
        final String path = getClass().getSimpleName() + "_" + System.currentTimeMillis();
        testRoot = resolver.create(root, path, null);
    }

    @After
    public void cleanup() throws Exception {
        resolver.delete(testRoot);
        resolver.close();
    }
    
    private void assertValueMap(ValueMap m, String ... keyValue) {
        assertNotNull("Expecting non-null ValueMap", m);
        for(int i=0 ; i< keyValue.length; i+=2) {
            final String key = keyValue[i];
            final String value = keyValue[i+1];
            assertEquals("Expecting " + key + "=" + value, value, m.get(key, String.class));
        }
    }

    @Test
    public void testSimpleCRUD() throws Exception {
        
        // Create a child resource of testRoot, with title and text properties
        final Props props = new Props("title", "hello", "text", "world");
        final String fullPath = resolver.create(testRoot, "child_" + System.currentTimeMillis(), props).getPath();
        resolver.commit();
        
        {
            // Retrieve and check child resource
            final Resource r = resolver.getResource(fullPath);
            assertNotNull("Expecting Resource at " + fullPath, r);
            final ModifiableValueMap m = r.adaptTo(ModifiableValueMap.class);
            assertValueMap(m, "title", "hello", "text", "world");
            
            // Update child resource
            m.put("more", "fun");
            m.put("title", "changed");
            resolver.commit();
        }
        
        {
            // Retrieve and check updated resource
            final Resource r = resolver.getResource(fullPath);
            assertNotNull("Expecting modified Resource at " + fullPath, r);
            assertValueMap(r.adaptTo(ValueMap.class), "title", "changed", "more", "fun", "text", "world");
        }
        
        {
            // Delete test resource and check that it's gone
            final Resource r = resolver.getResource(fullPath);
            assertNotNull("Expecting non-null resource to delete, at " + fullPath, r);
            resolver.delete(r);
            resolver.commit();
            assertNull("Expecting " + fullPath + " to be deleted", resolver.getResource(fullPath));
        }
    }
}
