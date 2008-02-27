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
package org.apache.sling.core.impl.request;

import junit.framework.TestCase;

import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;

/** Test the SlingRequestPathInfo class */
public class SlingRequestPathInfoTest extends TestCase {

    public void testNullResource() {
        try {
            new SlingRequestPathInfo(null, "dontcare");
            fail("Expected NullPointerException");
        } catch (NullPointerException npe) {
            // required for a null resource
        }
    }

    public void testTrailingDot() {
        RequestPathInfo p = new SlingRequestPathInfo(
            new MockResource("/some/path"), "/some/path.");
        assertEquals("/some/path", p.getResourcePath());
        assertNull(p.getSelectorString());
        assertEquals(0, p.getSelectors().length);
        assertNull(p.getExtension());
        assertNull(p.getSuffix());
    }

    public void testTrailingDotWithSuffix() {
        RequestPathInfo p = new SlingRequestPathInfo(
            new MockResource("/some/path"), "/some/path./suffix");
        assertEquals("/some/path", p.getResourcePath());
        assertNull(p.getSelectorString());
        assertEquals(0, p.getSelectors().length);
        assertNull(p.getExtension());
        assertEquals("/suffix", p.getSuffix());
    }

    public void testTrailingDotDot() {
        RequestPathInfo p = new SlingRequestPathInfo(
            new MockResource("/some/path"), "/some/path..");
        assertEquals("/some/path", p.getResourcePath());
        assertNull(p.getSelectorString());
        assertEquals(0, p.getSelectors().length);
        assertNull(p.getExtension());
        assertNull(p.getSuffix());
    }

    public void testTrailingDotDotWithSuffix() {
        RequestPathInfo p = new SlingRequestPathInfo(
            new MockResource("/some/path"), "/some/path../suffix");
        assertEquals("/some/path", p.getResourcePath());
        assertNull(p.getSelectorString());
        assertEquals(0, p.getSelectors().length);
        assertNull(p.getExtension());
        assertEquals("/suffix", p.getSuffix());
    }

    public void testTrailingDotDotDot() {
        RequestPathInfo p = new SlingRequestPathInfo(
            new MockResource("/some/path"), "/some/path...");
        assertEquals("/some/path", p.getResourcePath());
        assertNull(p.getSelectorString());
        assertEquals(0, p.getSelectors().length);
        assertNull(p.getExtension());
        assertNull(p.getSuffix());
    }

    public void testTrailingDotDotDotWithSuffix() {
        RequestPathInfo p = new SlingRequestPathInfo(
            new MockResource("/some/path"), "/some/path.../suffix");
        assertEquals("/some/path", p.getResourcePath());
        assertNull(p.getSelectorString());
        assertEquals(0, p.getSelectors().length);
        assertNull(p.getExtension());
        assertEquals("/suffix", p.getSuffix());
    }

    public void testAllOptions() {
        RequestPathInfo p = new SlingRequestPathInfo(
            new MockResource("/some/path"), "/some/path.print.a4.html/some/suffix");
        assertEquals("/some/path", p.getResourcePath());
        assertEquals("print.a4", p.getSelectorString());
        assertEquals(2, p.getSelectors().length);
        assertEquals("print", p.getSelectors()[0]);
        assertEquals("a4", p.getSelectors()[1]);
        assertEquals("html", p.getExtension());
        assertEquals("/some/suffix", p.getSuffix());
    }

    public void testAllEmpty() {
        RequestPathInfo p = new SlingRequestPathInfo(
            new MockResource("/"), null);
        assertEquals("/", p.getResourcePath());
        assertNull(p.getSelectorString());
        assertEquals(0, p.getSelectors().length);
        assertNull(p.getExtension());
        assertNull(p.getSuffix());
    }

    public void testPathOnly() {
        RequestPathInfo p = new SlingRequestPathInfo(new MockResource(
        "/some/path/here"), "/some/path/here");
        assertEquals("/some/path/here", p.getResourcePath());
        assertNull(p.getSelectorString());
        assertEquals(0, p.getSelectors().length);
        assertNull(p.getExtension());
        assertNull(p.getSuffix());
    }
    
    public void testPathWithExtensionOnly() {
        RequestPathInfo p = new SlingRequestPathInfo(new MockResource(
            "/some/path/here.html"), "/some/path/here.html");
        assertEquals("/some/path/here.html", p.getResourcePath());
        assertNull(p.getSelectorString());
        assertEquals(0, p.getSelectors().length);
        assertNull(p.getExtension());
        assertNull(p.getSuffix());
    }

    public void testPathAndExtensionOnly() {
        RequestPathInfo p = new SlingRequestPathInfo(new MockResource(
            "/some/path/here"), "/some/path/here.html");
        assertEquals("/some/path/here", p.getResourcePath());
        assertNull(p.getSelectorString());
        assertEquals(0, p.getSelectors().length);
        assertEquals("html", p.getExtension());
        assertNull(p.getSuffix());
    }

    public void testPathAndOneSelectorOnly() {
        RequestPathInfo p = new SlingRequestPathInfo(new MockResource(
            "/some/path/here"), "/some/path/here.print.html");
        assertEquals("/some/path/here", p.getResourcePath());
        assertEquals("print", p.getSelectorString());
        assertEquals(1, p.getSelectors().length);
        assertEquals("print", p.getSelectors()[0]);
        assertEquals("html", p.getExtension());
        assertNull(p.getSuffix());
    }

    public void testPathExtAndSuffix() {
        RequestPathInfo p = new SlingRequestPathInfo(new MockResource(
            "/some/path/here"), "/some/path/here.html/something");
        assertEquals("/some/path/here", p.getResourcePath());
        assertNull(p.getSelectorString());
        assertEquals(0, p.getSelectors().length);
        assertEquals("html", p.getExtension());
        assertEquals("/something", p.getSuffix());
    }

    public void testSelectorsSplit() {
        RequestPathInfo p = new SlingRequestPathInfo(new MockResource(
            "/some/path"), "/some/path.print.a4.html/some/suffix");
        assertEquals("/some/path", p.getResourcePath());
        assertEquals(2, p.getSelectors().length);
        assertEquals("print", p.getSelectors()[0]);
        assertEquals("a4", p.getSelectors()[1]);
        assertEquals("html", p.getExtension());
        assertEquals("/some/suffix", p.getSuffix());
    }

    public void testPartialResolutionB() {
        RequestPathInfo p = new SlingRequestPathInfo(new MockResource(
            "/some/path"), "/some/path.print.a4.html/some/suffix");
        assertEquals("/some/path", p.getResourcePath());
        assertEquals("print.a4", p.getSelectorString());
        assertEquals(2, p.getSelectors().length);
        assertEquals("print", p.getSelectors()[0]);
        assertEquals("a4", p.getSelectors()[1]);
        assertEquals("html", p.getExtension());
        assertEquals("/some/suffix", p.getSuffix());
    }

    public void testPartialResolutionC() {
        RequestPathInfo p = new SlingRequestPathInfo(new MockResource(
            "/some/path.print"), "/some/path.print.a4.html/some/suffix");
        assertEquals("/some/path.print", p.getResourcePath());
        assertEquals("a4", p.getSelectorString());
        assertEquals(1, p.getSelectors().length);
        assertEquals("a4", p.getSelectors()[0]);
        assertEquals("html", p.getExtension());
        assertEquals("/some/suffix", p.getSuffix());
    }

    public void testPartialResolutionD() {
        RequestPathInfo p = new SlingRequestPathInfo(new MockResource(
            "/some/path.print.a4"), "/some/path.print.a4.html/some/suffix");
        assertEquals("/some/path.print.a4", p.getResourcePath());
        assertNull(p.getSelectorString());
        assertEquals(0, p.getSelectors().length);
        assertEquals("html", p.getExtension());
        assertEquals("/some/suffix", p.getSuffix());
    }

    public void testJIRA_250_a() {
    	RequestPathInfo p = 
            new SlingRequestPathInfo(
                    new MockResource("/bunkai"), 
                    "/bunkai.1.json"
            );
        assertEquals("/bunkai", p.getResourcePath());
        assertEquals("json", p.getExtension());
        assertEquals("1", p.getSelectorString());
    }
    
    public void testJIRA_250_b() {
    	RequestPathInfo p = 
            new SlingRequestPathInfo(
                    new MockResource("/"), 
                    "/.1.json"
            );
        assertEquals("/", p.getResourcePath());
        assertEquals("json", p.getExtension());
        assertNull(p.getSuffix());
        assertEquals("Selector string must not be null", "1", p.getSelectorString());
    }
    
    public void testJIRA_250_c() {
    	RequestPathInfo p = 
            new SlingRequestPathInfo(
                    new MockResource("/"), 
                    "/.1.json/my/suffix"
            );
        assertEquals("/", p.getResourcePath());
        assertEquals("json", p.getExtension());
        assertEquals("/my/suffix",p.getSuffix());
        assertEquals("Selector string must not be null", "1", p.getSelectorString());
    }
    
    public void testJIRA_250_d() {
    	RequestPathInfo p = 
            new SlingRequestPathInfo(
                    new MockResource("/"), 
                    "/.json"
            );
        assertEquals("/", p.getResourcePath());
        assertEquals("json", p.getExtension());
        assertNull(p.getSuffix());
        assertNull(p.getSelectorString());
    }
    
    static class MockResource implements Resource {

        private final ResourceMetadata metadata;

        MockResource(String resolutionPath) {
            metadata = new ResourceMetadata();
            metadata.setResolutionPath(resolutionPath);
        }

        public String getResourceType() {
            throw new Error("MockResource does not implement this method");
        }

        public String getResourceSuperType() {
            throw new Error("MockResource does not implement this method");
        }
        
        public String getPath() {
            throw new Error("MockResource does not implement this method");
        }

        public ResourceMetadata getResourceMetadata() {
            return metadata;
        }

        public ResourceResolver getResourceResolver() {
            return null;
        }
        
        public <Type> Type adaptTo(Class<Type> type) {
            return null;
        }

    }

}
