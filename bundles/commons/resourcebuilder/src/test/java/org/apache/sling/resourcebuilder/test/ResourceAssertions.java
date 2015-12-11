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
package org.apache.sling.resourcebuilder.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.resourcebuilder.impl.MapArgsConverter;
import org.apache.sling.resourcebuilder.impl.ResourceBuilderImpl;

/** Utilities for asserting Resources and their properties */
public class ResourceAssertions {
    
    private final ResourceResolver resourceResolver;
    private final String testRootPath;
    
    public ResourceAssertions(String testRootPath, ResourceResolver r) {
        this.testRootPath = testRootPath;
        this.resourceResolver = r;
    }
    
    public String fullPath(String path) {
        return path.startsWith("/") ? path : testRootPath + "/" + path;
    }
    
    public Resource assertResource(String path) {
        final Resource result =  resourceResolver.resolve(fullPath(path));
        assertNotNull("Expecting resource to exist:" + path, result);
        return result;
    }
    
    /** Assert that a file exists and verify its properties. */
    public Resource assertFile(String path, String mimeType, String expectedContent, Long lastModified) throws IOException {
        final Comparator<Long> defaultComparator = new Comparator<Long>() {
            @Override
            public int compare(Long expected, Long fromResource) {
                if(expected == -1) {
                    return 0;
                }
                return expected.compareTo(fromResource);
            }
        };
        return assertFile(path, mimeType, expectedContent, lastModified, defaultComparator);
    }
    
    public String readFully(InputStream is) throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            IOUtils.copy(is, bos);
            return new String(bos.toByteArray());
        } finally {
            bos.close();
            is.close();
        }
    }
    
    /** Assert that a file exists and verify its properties. */
    public Resource assertFile(String path, String mimeType, String expectedContent, Long lastModified, Comparator<Long> lastModifiedComparator) throws IOException {
        final Resource r = assertResource(path);
        assertNotNull("Expecting resource to exist:" + path, r);
        
        // Files are stored according to the standard JCR structure
        final ValueMap fileVm = r.adaptTo(ValueMap.class);
        assertNotNull("Expecting ValueMap for " + r.getPath(), fileVm);
        assertEquals("Expecting an nt:file at " + r.getPath(), 
                ResourceBuilderImpl.NT_FILE, fileVm.get(ResourceBuilderImpl.JCR_PRIMARYTYPE));
        final Resource jcrContent = r.getChild(ResourceBuilderImpl.JCR_CONTENT);
        assertNotNull("Expecting subresource:" + ResourceBuilderImpl.JCR_CONTENT, jcrContent);
        final ValueMap vm = jcrContent.adaptTo(ValueMap.class);
        assertNotNull("Expecting ValueMap for " + jcrContent.getPath(), vm);
        assertEquals("Expecting nt:Resource type for " + jcrContent.getPath(), 
                ResourceBuilderImpl.NT_RESOURCE, vm.get(ResourceBuilderImpl.JCR_PRIMARYTYPE));
        assertEquals("Expecting the correct mime-type", mimeType, vm.get(ResourceBuilderImpl.JCR_MIMETYPE));
        assertEquals("Expecting the correct last modified", 
                0, lastModifiedComparator.compare(lastModified, getLastModified(vm)));
        
        final InputStream is = vm.get(ResourceBuilderImpl.JCR_DATA, InputStream.class);
        assertNotNull("Expecting InputStream property on nt:resource:" + ResourceBuilderImpl.JCR_DATA, is);
        final String content = readFully(is);
        assertTrue("Expecting content to contain " + expectedContent, content.contains(expectedContent));
        
        return r;
    }
    
    private Long getLastModified(ValueMap vm) {
        final Object o = vm.get(ResourceBuilderImpl.JCR_LASTMODIFIED);
        if(o instanceof Long) {
            return (Long)o;
        } else if(o instanceof Calendar) {
            return ((Calendar)o).getTimeInMillis();
        }
        throw new IllegalArgumentException("Unexpected type " + o.getClass().getName());
    }
    
    public void assertProperties(String path, Object ...props) {
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
}