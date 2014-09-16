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
package org.apache.sling.testing.resourceresolver;

import static org.apache.sling.testing.resourceresolver.MockResource.JCR_CONTENT;
import static org.apache.sling.testing.resourceresolver.MockResource.JCR_DATA;
import static org.apache.sling.testing.resourceresolver.MockResource.JCR_PRIMARYTYPE;
import static org.apache.sling.testing.resourceresolver.MockResource.NT_FILE;
import static org.apache.sling.testing.resourceresolver.MockResource.NT_RESOURCE;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

/**
 * Implements simple write and read resource and values test.
 * Sling CRUD API is used to create the test data.
 */
public class NtFileResourceTest {

    private static final byte[] BINARY_VALUE = new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06 };

    private ResourceResolver resourceResolver;
    private Resource testRoot;

    @Before
    public final void setUp() throws IOException, LoginException {
        resourceResolver = new MockResourceResolverFactory().getResourceResolver(null);
        Resource root = resourceResolver.getResource("/");
        testRoot = resourceResolver.create(root, "test", ValueMap.EMPTY);
    }

    @Test
    public void testNtFile() throws IOException {
        Resource file = resourceResolver.create(testRoot, "ntFile", ImmutableMap.<String, Object>builder()
                .put(JCR_PRIMARYTYPE, NT_FILE)
                .build());
        resourceResolver.create(file, JCR_CONTENT, ImmutableMap.<String, Object>builder() 
            .put(JCR_PRIMARYTYPE, NT_RESOURCE)
            .put(JCR_DATA, new ByteArrayInputStream(BINARY_VALUE))
            .build());

        String path = testRoot.getPath() + "/ntFile";
        Resource resource = resourceResolver.getResource(path);
        InputStream is = resource.adaptTo(InputStream.class);
        assertNotNull(is);
        
        assertArrayEquals(BINARY_VALUE, IOUtils.toByteArray(is));
        is.close();
    }

    @Test
    public void testNtResource() throws IOException {
        resourceResolver.create(testRoot, "ntResource", ImmutableMap.<String, Object>builder()
                .put(JCR_PRIMARYTYPE, NT_RESOURCE)
                .put(JCR_DATA, new ByteArrayInputStream(BINARY_VALUE))
                .build());

        String path = testRoot.getPath() + "/ntResource";
        Resource resource = resourceResolver.getResource(path);
        InputStream is = resource.adaptTo(InputStream.class);
        assertNotNull(is);
        
        assertArrayEquals(BINARY_VALUE, IOUtils.toByteArray(is));
        is.close();
    }

}
