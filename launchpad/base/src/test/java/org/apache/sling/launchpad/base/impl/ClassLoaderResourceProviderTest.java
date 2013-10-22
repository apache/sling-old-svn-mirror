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
package org.apache.sling.launchpad.base.impl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.io.InputStream;

import org.junit.After;
import org.junit.Test;

public class ClassLoaderResourceProviderTest {
    
    public static final String TEST_RESOURCE_PATH = "holaworld-invalid.jar"; 
    private ClassLoaderResourceProvider provider = new ClassLoaderResourceProvider(getClass().getClassLoader());
    private InputStream toClose;

    @After
    public void cleanup() throws IOException {
        if(toClose != null) {
            toClose.close();
        }
    }
    
    @Test
    public void testGetResourceFound() {
        assertNotNull(provider.getResource(TEST_RESOURCE_PATH));
    }
    
    @Test
    public void testGetResourceLeadingSlash() {
        assertNotNull(provider.getResource("/" + TEST_RESOURCE_PATH));
    }
    
    @Test
    public void testGetResourceNotFound() {
        assertNull(provider.getResource("NONEXISTENT"));
    }
    
    @Test
    public void testGetResourceNull() {
        assertNull(provider.getResource(null));
    }
    
    @Test
    public void testGetResourceStreamFound() throws IOException {
        assertNotNull(toClose = provider.getResourceAsStream(TEST_RESOURCE_PATH));
    }
    
    @Test
    public void testGetResourceStreamLeadingSlash() throws IOException {
        assertNotNull(toClose = provider.getResourceAsStream("/" + TEST_RESOURCE_PATH));
    }
    
    @Test
    public void testGetResourceStreamNotFound() throws IOException {
        assertNull(provider.getResourceAsStream("NONEXISTENT"));
    }
    
}
