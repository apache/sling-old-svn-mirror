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

/** Test the MicroslingRequestPathInfo */
public class SlingRequestPathInfoTest extends TestCase {

    public void testSimplePath() {
        RequestPathInfo p = new SlingRequestPathInfo(
            new MockResource("/"), "/some/path.print.a4.html/some/suffix");
        assertEquals("/", p.getResourcePath());
        assertNull(p.getSelectorString());
        assertEquals(0, p.getSelectors().length);
        assertNull(p.getExtension());
        assertEquals("/some/path.print.a4.html/some/suffix", p.getSuffix());
    }

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

    public void testSimpleSuffix() {
        RequestPathInfo p = new SlingRequestPathInfo(
            new MockResource("/"), "/some/path.print.a4.html/some/suffix");
        assertEquals("/", p.getResourcePath());
        assertNull(p.getSelectorString());
        assertEquals(0, p.getSelectors().length);
        assertNull(p.getExtension());
        assertEquals("/some/path.print.a4.html/some/suffix", p.getSuffix());
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

    public void testPartialResolutionA() {
        RequestPathInfo p = new SlingRequestPathInfo(new MockResource(
            "/some"), "/some/path.print.a4.html/some/suffix");
        assertEquals("/some", p.getResourcePath());
        assertNull(p.getSelectorString());
        assertEquals(0, p.getSelectors().length);
        assertNull(p.getExtension());
        assertEquals("/path.print.a4.html/some/suffix", p.getSuffix());
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

    public void testPartialResolutionE() {
        RequestPathInfo p = new SlingRequestPathInfo(new MockResource(
            "/some/path.print.a4.html"), "/some/path.print.a4.html/some/suffix");
        assertEquals("/some/path.print.a4.html", p.getResourcePath());
        assertNull(p.getSelectorString());
        assertEquals(0, p.getSelectors().length);
        assertNull(p.getExtension());
        assertEquals("/some/suffix", p.getSuffix());
    }

    static class MockResource implements Resource {

        private final ResourceMetadata metadata;

        MockResource(String resolutionPath) {
            metadata = new ResourceMetadata();
            metadata.put(ResourceMetadata.RESOLUTION_PATH, resolutionPath);
        }

        public String getResourceType() {
            throw new Error("MockResource does not implement this method");
        }

        public String getPath() {
            throw new Error("MockResource does not implement this method");
        }

        public ResourceMetadata getResourceMetadata() {
            return metadata;
        }

        public <Type> Type adaptTo(Class<Type> type) {
            return null;
        }

    }

}
