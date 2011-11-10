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
package org.apache.sling.commons.mime.internal;

import junit.framework.TestCase;

import org.apache.sling.commons.mime.MimeTypeProvider;

/**
 * Unit tests for the {@link TikaMimeTypeProvider} class.
 */
public class TikaMimeTypeProviderTest extends TestCase {

    public void testGetMimeType() {
        MimeTypeProvider provider = new TikaMimeTypeProvider();
        assertEquals("text/plain", provider.getMimeType("test.txt"));
        assertEquals("application/pdf", provider.getMimeType("test.pdf"));
        assertEquals("image/jpeg", provider.getMimeType("test.jpg"));
    }

    public void testGetExtension() {
        MimeTypeProvider provider = new TikaMimeTypeProvider();
        assertEquals("txt", provider.getExtension("text/plain"));
        assertEquals("pdf", provider.getExtension("application/pdf"));
        assertEquals("jpg", provider.getExtension("image/jpeg"));
    }

}