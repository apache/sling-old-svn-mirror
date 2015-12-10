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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
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
    
    /** Assert that a file exists and verify its properties. */
    private void assertFile(String path, String mimeType, String expectedContent, Long lastModified) throws IOException {
        final Resource r = assertResource(fullPath(path));
        assertNotNull("Expecting resource to exist:" + path, r);
        
        // Files are stored according to the standard JCR structure
        final Resource jcrContent = r.getChild(ResourceBuilderImpl.JCR_CONTENT);
        assertNotNull("Expecting subresource:" + ResourceBuilderImpl.JCR_CONTENT, jcrContent);
        final ValueMap vm = jcrContent.adaptTo(ValueMap.class);
        assertNotNull("Expecting ValueMap for " + jcrContent.getPath(), vm);
        assertEquals("Expecting nt:Resource type for " + jcrContent.getPath(), 
                ResourceBuilderImpl.NT_RESOURCE, vm.get(ResourceBuilderImpl.JCR_PRIMARYTYPE));
        assertEquals("Expecting the correct mime-type", mimeType, vm.get(ResourceBuilderImpl.JCR_MIMETYPE));
        assertEquals("Expecting the correct last modified", lastModified, vm.get(ResourceBuilderImpl.JCR_LASTMODIFIED));
        
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final InputStream is = vm.get(ResourceBuilderImpl.JCR_DATA, InputStream.class);
        assertNotNull("Expecting InputStream property on nt:resource:" + ResourceBuilderImpl.JCR_DATA, is);
        IOUtils.copy(is, bos);
        try {
            final String content = new String(bos.toByteArray());
            assertTrue("Expecting content to contain " + expectedContent, content.contains(expectedContent));
        } finally {
            bos.close();
            is.close();
        }
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
    public void simpleTree() throws PersistenceException {
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
    
    @Test
    public void treeWithFiles() throws PersistenceException, IOException {
        getBuilder(testRootPath)
            .resource("apps/myapp/components/resource")
            .siblingsMode()
            .file("models.js", getClass().getResourceAsStream("/models.js"), "MT1", 42)
            .file("text.html", getClass().getResourceAsStream("/text.html"), "MT2", 43)
            .resetParent()
            .hierarchyMode()
            .resource("apps")
            .file("myapp.json", getClass().getResourceAsStream("/myapp.json"), "MT3", 44)
            .resetParent()
            .resource("apps/content/myapp/resource")
            .resetParent()
            .resource("apps/content", "title", "foo")
            .file("myapp.json", getClass().getResourceAsStream("/myapp.json"), "MT4", 45)
            .commit()
            ;
        
        assertResource("apps/content/myapp/resource");
        assertResource("apps/myapp/components/resource");
        assertProperties("apps/content", "title", "foo");
        
        assertFile("apps/myapp/components/resource/models.js", 
                "MT1", "function someJavascriptFunction()", 42L);
        assertFile("apps/myapp/components/resource/text.html", 
                "MT2", "This is an html file", 43L);
        assertFile("apps/myapp.json", 
                "MT3", "\"sling:resourceType\":\"its/resource/type\"", 44L);
        assertFile("apps/content/myapp.json", 
                "MT4", "\"sling:resourceType\":\"its/resource/type\"", 45L);
    }
}
