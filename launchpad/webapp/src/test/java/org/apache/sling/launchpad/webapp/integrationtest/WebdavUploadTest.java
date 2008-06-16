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
package org.apache.sling.launchpad.webapp.integrationtest;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.sling.commons.testing.integration.HttpTestBase;

/** Test WebDAV upload of various file types */
public class WebdavUploadTest extends HttpTestBase {
    
    private final String testDir = "/sling-test/" + getClass().getSimpleName() + System.currentTimeMillis();
    private final String testDirUrl = WEBDAV_BASE_URL + testDir;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        testClient.mkdirs(WEBDAV_BASE_URL, testDir);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        testClient.delete(testDirUrl);
    }

    protected byte [] readStream(InputStream is) throws IOException {
        if(is == null) {
            fail("Null InputStream");
        }
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            final byte [] buffer = new byte[4096];
            int n = 0;
            while( (n = is.read(buffer, 0, buffer.length)) > 0) {
                bos.write(buffer, 0, n);
            }
        } finally {
            is.close();
            bos.flush();
            bos.close();
        }
        return bos.toByteArray();
    }
    
    protected void compareData(String slingDataUrl, String localResourcePath) throws IOException {
        
        // Get Sling content as a binary stream
        final GetMethod m = new GetMethod(slingDataUrl);
        final int httpStatus = httpClient.executeMethod(m);
        assertEquals("GET " + slingDataUrl + " must return status 200", 200, httpStatus);
        final byte [] local = readStream(getClass().getResourceAsStream(localResourcePath));
        final byte [] remote = readStream(m.getResponseBodyAsStream());
        
        assertEquals("Local and remote files have the same length", local.length, remote.length);
        
        for(int i=0 ; i < local.length; i++) {
            assertEquals("Content must match at index " + i, local[i], remote[i]);
        }
    }
    
    protected void uploadAndCheck(String localResourcePath) throws IOException {
        final InputStream data = getClass().getResourceAsStream(localResourcePath);
        if(data==null) {
            fail("Local resource not found: " + localResourcePath);
        }
        
        try {
            final String url = testDirUrl + "/" + new File(localResourcePath).getName();
            testClient.upload(url, data); 
            compareData(url, localResourcePath);
        } finally {
            if(data!=null) {
                data.close();
            }
        }
        
    }
    
    public void testTextUpload() throws IOException {
        uploadAndCheck("/integration-test/testfile.txt");
    }
    
    public void testXmlUpload() throws IOException {
        uploadAndCheck("/integration-test/testfile.xml");
    }
    
    public void testZipUpload() throws IOException {
        uploadAndCheck("/integration-test/testfile.zip");
    }
    
    public void testPngUpload() throws IOException {
        uploadAndCheck("/integration-test/sling-logo.png");
    }
}
