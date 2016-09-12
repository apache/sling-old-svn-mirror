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
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.commons.mime.MimeTypeService;
import org.apache.sling.resourcebuilder.api.ResourceBuilder;
import org.apache.sling.resourcebuilder.test.ResourceAssertions;
import org.apache.sling.testing.resourceresolver.MockResourceResolverFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ResourceBuilderImplTest {
    
    private String testRootPath;
    private long lastModified;
    private Random random = new Random(System.currentTimeMillis());
    private ResourceAssertions A;
    
    @Mock
    private MimeTypeService mimeTypeService;
    
    private ResourceResolver resourceResolver;
    
    private Resource getTestRoot(String path) throws PersistenceException {
        final Resource root = resourceResolver.resolve("/");
        assertNotNull("Expecting non-null root", root);
        return resourceResolver.create(root, ResourceUtil.getName(path), null);
    }
    
    private ResourceBuilderImpl getBuilder(String path) throws Exception {
        lastModified = random.nextLong();
        
        final Resource parent = getTestRoot(path);
        final ResourceBuilderImpl result = new ResourceBuilderImpl(parent, mimeTypeService) {
            @Override
            protected long getLastModified(long userSuppliedValue) {
                final long now = System.currentTimeMillis();
                final long superValue = super.getLastModified(-1);
                final long maxDelta = 60 * 1000L;
                if(superValue < now || superValue - now > maxDelta) {
                    fail("getLastModified does not seem to use current time as its default value");
                }
                
                if(userSuppliedValue >= 0) {
                    return super.getLastModified(userSuppliedValue);
                }
                return lastModified;
            }
        };
        return result;
    }
    
    @Before
    public void setup() throws Exception {
        when(mimeTypeService.getMimeType(anyString())).thenReturn("application/javascript");
        resourceResolver = new MockResourceResolverFactory().getResourceResolver(null);
        
        testRootPath = "/" + UUID.randomUUID().toString();
        A = new ResourceAssertions(testRootPath, resourceResolver);
    }
    
    @After
    public void cleanup() throws PersistenceException {
        final Resource r = resourceResolver.getResource(testRootPath);
        if(r != null) {
            resourceResolver.delete(r);
            resourceResolver.commit();
        }
    }
    
    @Test
    public void basicResource() throws Exception {
        getBuilder(testRootPath)
            .resource("child", "title", "foo")
            .commit();
        
        A.assertProperties("child", "title", "foo");
        assertEquals(A.fullPath("child"), A.assertResource("child").getPath());
    }
    
    @Test
    public void basicResourceWithMap() throws Exception {
        Map<String,Object> props = new HashMap<String,Object>();
        props.put("title", "foo");
        getBuilder(testRootPath)
            .resource("child", props)
            .commit();
        
        A.assertProperties("child", "title", "foo");
        assertEquals(A.fullPath("child"), A.assertResource("child").getPath());
    }
    
    @Test
    public void ensureResourceExists() throws Exception {
        
        class MyResourceBuilder extends ResourceBuilderImpl {
            MyResourceBuilder() {
                super(resourceResolver.getResource("/"), null);
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
    public void deepResource() throws Exception {
        getBuilder(testRootPath)
            .resource("a/b/c", "title", "foo")
            .commit();
        
        A.assertProperties("a/b/c", "title", "foo");
        assertEquals(A.fullPath("a/b/c"), A.assertResource("a/b/c").getPath());
        A.assertResource("a/b");
        A.assertResource("a");
    }
    
    @Test
    public void intermediatePrimaryTypes() throws Exception {
        getBuilder(testRootPath)
            .resource("a/b/c")
            .withIntermediatePrimaryType("foo")
            .resource("d/e")
            .withIntermediatePrimaryType(null)
            .resource("f/g")
            .commit();
        
        A.assertProperties("a/b", ResourceBuilderImpl.JCR_PRIMARYTYPE, "nt:unstructured");
        A.assertProperties("a/b/c/d", ResourceBuilderImpl.JCR_PRIMARYTYPE, "foo");
        A.assertProperties("a/b/c/d/e/f", ResourceBuilderImpl.JCR_PRIMARYTYPE, "nt:unstructured");
    }
    
    @Test
    public void resetParent() throws Exception {
        getBuilder(testRootPath)
            .resource("a/b/c")
            .siblingsMode()
            .resource("one")
            .resource("two")
            .atParent()  // also sets hierarchyMode
            .resource("d/e")
            .resource("f/g")
            .siblingsMode()
            .resource("three")
            .resource("four")
            .commit();
        
        A.assertResource("a/b/c");
        A.assertResource("a/b/c/one");
        A.assertResource("a/b/c/two");
        A.assertResource("d/e");
        A.assertResource("d/e/f/g");
        A.assertResource("d/e/f/g/three");
        A.assertResource("d/e/f/g/four");
    }
    
    @Test
    public void noResetParent() throws Exception {
        getBuilder(testRootPath)
            .resource("a/b/c")
            .resource("d/e")
            .commit();
        
        A.assertResource("a/b/c");
        A.assertResource("a/b/c/d/e");
    }
    
    @Test
    public void getParent() throws Exception {
        final Resource parent = getBuilder(testRootPath).getCurrentParent();
        assertNotNull(parent);
        assertEquals(testRootPath, parent.getPath());
    }
    
    @Test(expected=RuntimeException.class)
    public void missingParentFails() throws Exception {
        new ResourceBuilderImpl(null, null).resource("foo");
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void aboveParentFails() throws Exception {
        getBuilder(testRootPath).resource("../foo");
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void aboveParentFailsFile() throws Exception {
        getBuilder(testRootPath).file("../foo.js", null);
    }
    
    @Test
    public void simpleTree() throws Exception {
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
        
        A.assertProperties("a/b/c", "count", 21, "title", "foo");
        A.assertProperties("a/b/c/with/more/here", "it", "worked");
        A.assertResource("a/b/c/with/more/here/deepest");
        A.assertResource("a/b/c/1");
        A.assertResource("a/b/c/2");
        A.assertResource("a/b/c/3");
    }
    
    @Test
    public void treeWithFiles() throws Exception {
        getBuilder(testRootPath)
            .resource("apps/myapp/components/resource")
            .siblingsMode()
            .file("models.js", getClass().getResourceAsStream("/files/models.js"), "MT1", 42)
            .file("text.html", getClass().getResourceAsStream("/files/text.html"), "MT2", 43)
            .atParent()
            .file("apps/myapp.json", getClass().getResourceAsStream("/files/myapp.json"), "MT3", 44)
            .atParent()
            .resource("apps/content/myapp/resource")
            .atParent()
            .resource("apps/content", "title", "foo")
            .file("myapp.json", getClass().getResourceAsStream("/files/myapp.json"), "MT4", 45)
            .commit()
            ;
        
        A.assertResource("apps/content/myapp/resource");
        A.assertResource("apps/myapp/components/resource");
        A.assertProperties("apps/content", "title", "foo");
        
        A.assertFile("apps/myapp/components/resource/models.js", 
                "MT1", "function someJavascriptFunction()", 42L);
        A.assertFile("apps/myapp/components/resource/text.html", 
                "MT2", "This is an html file", 43L);
        A.assertFile("apps/myapp.json", 
                "MT3", "\"sling:resourceType\":\"its/resource/type\"", 44L);
        A.assertFile("apps/content/myapp.json", 
                "MT4", "\"sling:resourceType\":\"its/resource/type\"", 45L);
    }
    
    @Test
    public void autoMimetype() throws Exception {
        getBuilder(testRootPath)
            .file("models.js", getClass().getResourceAsStream("/files/models.js"), null, 42)
            .commit()
            ;
        A.assertFile("models.js", 
                "application/javascript", "function someJavascriptFunction()", 42L);
    }
    
    @Test
    public void autoLastModified() throws Exception {
        getBuilder(testRootPath)
            .file("models.js", getClass().getResourceAsStream("/files/models.js"), "MT1", -1)
            .commit()
            ;
        A.assertFile("models.js", 
                "MT1", "function someJavascriptFunction()", lastModified);
    }
    
    @Test
    public void autoEverything() throws Exception {
        getBuilder(testRootPath)
            .file("a/b/c/models.js", getClass().getResourceAsStream("/files/models.js"))
            .commit()
            ;
        A.assertFile("a/b/c/models.js", 
                "application/javascript", "function someJavascriptFunction()", lastModified);
    }
    
    @Test(expected=IllegalStateException.class)
    public void duplicatedFileFails() throws Exception {
        getBuilder(testRootPath)
            .siblingsMode()
            .file("models.js", getClass().getResourceAsStream("/files/models.js"), null, 42)
            .file("models.js", getClass().getResourceAsStream("/files/models.js"), null, 42)
            ;
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void nullDataFails() throws Exception {
        getBuilder(testRootPath)
            .file("models.js", null, null, 42)
            ;
    }
    
    @Test
    public void forParent() throws PersistenceException {
        new ResourceBuilderFactoryService()
            .forParent(getTestRoot(testRootPath))
            .resource("a/b/c")
            .commit();
        A.assertResource("a/b/c");
    }
    
    @Test
    public void forResolver() throws PersistenceException {
        new ResourceBuilderFactoryService()
            .forResolver(resourceResolver)
            .resource("d/e/f")
            .commit();
        
        // Resource is created at root in this case
        A.assertResource("/d/e/f");
    }

    @Test
    public void absolutePath() throws Exception {
        new ResourceBuilderFactoryService()
            .forResolver(resourceResolver)
            .resource("/a/b/c")
            .resource("/a/b/f")
            .resource("/g/h/i")
            .resource("j/l/m")
            .resource("/o/p/q");
        
        // absolute paths are supported and can be mixed with relative paths
        A.assertResource("/a/b/c");
        A.assertResource("/a/b/f");
        A.assertResource("/g/h/i");
        A.assertResource("/g/h/i/j/l/m");
        A.assertResource("/o/p/q");
    }

    @Test
    public void reuseInstance() throws Exception {
        ResourceBuilder content = new ResourceBuilderFactoryService()
                .forResolver(resourceResolver)
                .resource("/content");
        content.resource("a");
        content.resource("b/c");
        content.resource("/test")
                .siblingsMode()
                .resource("1")
                .resource("2");

        A.assertResource("/content/a");
        A.assertResource("/content/b/c");
        A.assertResource("/test");
        A.assertResource("/test/1");
        A.assertResource("/test/2");
    }

}
