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
package org.apache.sling.resourcebuilder.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Map;
import java.util.UUID;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ResourceBuilderImplTest {
    
    private String testRootPath;
    private ResourceResolver resourceResolver;
    
    @Rule
    public SlingContext context = new SlingContext(ResourceResolverType.RESOURCERESOLVER_MOCK);
    
    private ResourceBuilderImpl getBuilder(String path) throws PersistenceException {
        final Resource root = context.resourceResolver().resolve("/");
        assertNotNull("Expecting non-null root", root);
        return new ResourceBuilderImpl(resourceResolver.create(root, ResourceUtil.getName(path), null));
    }
    
    @Before
    public void setup() {
        testRootPath = "/" + UUID.randomUUID().toString();
        resourceResolver = context.resourceResolver();
    }
    
    private Resource assertResource(String path) {
        final Resource result =  resourceResolver.resolve(fullPath(path));
        assertNotNull("Expecting resource to exist:" + path, result);
        return result;
    }
    
    private String fullPath(String path) {
        return path.startsWith("/") ? path : testRootPath + "/" + path;
    }
    
    private void assertProperties(String path, Object ...props) {
        final Map<String, Object> expected = MapArgsConverter.toMap(props);
        final Resource r = assertResource(path);
        final ValueMap vm = r.adaptTo(ValueMap.class);
        for(Map.Entry<String, Object> e : expected.entrySet()) {
            final Object value = vm.get(e.getKey());
            assertNotNull("Expecting property " + e.getKey() + " for resource " + r.getPath());
            assertEquals(
                    "Expecting value " + e.getValue() 
                    + " for property " + e.getKey() + " of resource " + r.getPath()
                    , e.getValue(), value);
        }
    }
    
    @Test
    public void basicResource() throws PersistenceException {
        getBuilder(testRootPath)
            .resource("child", "title", "foo")
            .commit();
        
        assertProperties("child", "title", "foo");
        assertEquals(fullPath("child"), assertResource("child").getPath());
    }
    
    @Test
    public void ensureResourceExists() throws PersistenceException {
        
        class MyResourceBuilder extends ResourceBuilderImpl {
            MyResourceBuilder() {
                super(resourceResolver.getResource("/"));
            }
            
            Resource r(String path) {
                return ensureResourceExists(path);
            }
        };
        final MyResourceBuilder b = new MyResourceBuilder();
        
        assertEquals("/", b.r(null).getPath());
        assertEquals("/", b.r("").getPath());
        assertEquals("/", b.r("/").getPath());
    }
    
    @Test
    public void deepResource() throws PersistenceException {
        getBuilder(testRootPath)
            .resource("a/b/c", "title", "foo")
            .commit();
        
        assertProperties("a/b/c", "title", "foo");
        assertEquals(fullPath("a/b/c"), assertResource("a/b/c").getPath());
        assertResource("a/b");
        assertResource("a");
    }
    
    @Test
    public void intermediatePrimaryTypes() throws PersistenceException {
        getBuilder(testRootPath)
            .resource("a/b/c")
            .withIntermediatePrimaryType("foo")
            .resource("d/e")
            .withIntermediatePrimaryType(null)
            .resource("f/g")
            .commit();
        
        assertProperties("a/b", ResourceBuilderImpl.JCR_PRIMARYTYPE, "nt:unstructured");
        assertProperties("a/b/c/d", ResourceBuilderImpl.JCR_PRIMARYTYPE, "foo");
        assertProperties("a/b/c/d/e/f", ResourceBuilderImpl.JCR_PRIMARYTYPE, "nt:unstructured");
    }
    
    @Test
    public void resetParent() throws PersistenceException {
        getBuilder(testRootPath)
            .resource("a/b/c")
            .resetParent()
            .resource("d/e")
            .commit();
        
        assertResource("a/b/c");
        assertResource("d/e");
    }
    
    @Test
    public void noResetParent() throws PersistenceException {
        getBuilder(testRootPath)
            .resource("a/b/c")
            .resource("d/e")
            .commit();
        
        assertResource("a/b/c");
        assertResource("a/b/c/d/e");
    }
    
    @Test
    public void getParent() throws PersistenceException {
        final Resource parent = getBuilder(testRootPath).getCurrentParent();
        assertNotNull(parent);
        assertEquals(testRootPath, parent.getPath());
    }
    
    @Test(expected=RuntimeException.class)
    public void missingParentFails() throws PersistenceException {
        new ResourceBuilderImpl(null).resource("foo");
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void absolutePathFails() throws PersistenceException {
        getBuilder(testRootPath).resource("/absolute");
    }
    
    @Test
    public void buildATree() throws PersistenceException {
        getBuilder(testRootPath)
            .resource("a/b/c", "title", "foo", "count", 21)
            .siblingsMode()
            .resource("1")
            .resource("2")
            .resource("3")
            .hierarchyMode()
            .resource("with")
            .resource("more/here", "it", "worked")
            .resource("deepest", "it", "worked")
            .commit();
        
        assertProperties("a/b/c", "count", 21, "title", "foo");
        assertProperties("a/b/c/with/more/here", "it", "worked");
        assertResource("a/b/c/with/more/here/deepest");
        assertResource("a/b/c/1");
        assertResource("a/b/c/2");
        assertResource("a/b/c/3");
    }
}
