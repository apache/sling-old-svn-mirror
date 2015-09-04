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

package org.apache.sling.commons.contentdetection.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import junitx.util.PrivateAccessor;

import org.apache.sling.commons.mime.MimeTypeService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ContentAwareMimeTypeServiceImplTest {

    private ContentAwareMimeTypeServiceImpl contentAwareMimeTypeService = null;
    private int counterA;
    private int counterB;

    final MimeTypeService mimeTypeService = new MimeTypeService() {

        @Override
        public String getMimeType(String name) {
            return "MT_" + name;
        }

        @Override
        public String getExtension(String mimeType) {
            return "EXT_" + mimeType;
        }

        @Override
        public void registerMimeType(String mimeType, String... extensions) {
            counterA++;
        }

        @Override
        public void registerMimeType(InputStream mimeTabStream) throws IOException {
            counterB++;
        }
    };
    
    @Before
    public void setup() throws NoSuchFieldException {
        contentAwareMimeTypeService = new ContentAwareMimeTypeServiceImpl();
        PrivateAccessor.setField(contentAwareMimeTypeService, "mimeTypeService", mimeTypeService);
    }
    
    @Test
    public void testGetMimeTypeByString(){
        String mimeTypeName = "testName.txt";
        final String mimeType = contentAwareMimeTypeService.getMimeType(mimeTypeName);
        Assert.assertEquals("MT_testName.txt", mimeType);
    }
    
    @Test
    public void testGetExtension() {
        final String ext = contentAwareMimeTypeService.getExtension("foo");
        Assert.assertEquals("EXT_foo", ext);
    }

    @Test
    public void testGetMimeTypeWithNullContent() throws IOException {
        final String filename = "test.txt";
        final String mimeType = contentAwareMimeTypeService.getMimeType(filename, null);
        Assert.assertEquals("MT_test.txt", mimeType);
    }

    @Test
    public void testRegisterMimeTypeIsDelegatedA() {
        final int before = counterA;
        contentAwareMimeTypeService.registerMimeType("foo", new String[] {});
        Assert.assertEquals("Expecting 1 call to registerMimeType(A)", before + 1, counterA);
    }

    @Test
    public void testRegisterMimeTypeIsDelegatedB() throws IOException {
        final int before = counterB;
        final InputStream is = new ByteArrayInputStream("x".getBytes());
        try {
            contentAwareMimeTypeService.registerMimeType(is);
        } finally {
            is.close();
        }
        Assert.assertEquals("Expecting 1 call to registerMimeType(B)", before + 1, counterB);
    }
}