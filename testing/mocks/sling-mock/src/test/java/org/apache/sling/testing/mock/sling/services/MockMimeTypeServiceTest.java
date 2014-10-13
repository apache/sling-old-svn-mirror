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
package org.apache.sling.testing.mock.sling.services;

import static org.junit.Assert.assertEquals;

import org.apache.sling.commons.mime.MimeTypeService;
import org.junit.Before;
import org.junit.Test;

public class MockMimeTypeServiceTest {

    private MimeTypeService mimeTypeService;

    @Before
    public void setUp() throws Exception {
        this.mimeTypeService = new MockMimeTypeService();
    }

    @Test
    public void testGetMimeType() {
        assertEquals("text/html", this.mimeTypeService.getMimeType("html"));
        assertEquals("application/json", this.mimeTypeService.getMimeType("json"));
        assertEquals("image/jpeg", this.mimeTypeService.getMimeType("jpg"));
        assertEquals("image/jpeg", this.mimeTypeService.getMimeType("jpeg"));
    }

    @Test
    public void testGetExtension() {
        assertEquals("html", this.mimeTypeService.getExtension("text/html"));
        assertEquals("json", this.mimeTypeService.getExtension("application/json"));
        assertEquals("jpeg", this.mimeTypeService.getExtension("image/jpeg"));
    }

}
