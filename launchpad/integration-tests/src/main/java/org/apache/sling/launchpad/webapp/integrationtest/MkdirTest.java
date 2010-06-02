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

import java.io.IOException;

import org.apache.sling.commons.testing.integration.HttpTestBase;

/** Test the MicroslingIntegrationTestClient's mkdir functions */ 
public class MkdirTest extends HttpTestBase {
    
    /** Try creating a directory via WebDAV */
    public void testMkdir() throws IOException {
        final String testDirUrl = WEBDAV_BASE_URL + TEST_PATH + System.currentTimeMillis();
        
        assertHttpStatus(testDirUrl, 404, testDirUrl);
        
        try {
            testClient.mkdir(testDirUrl);
        } catch(IOException ioe) {
            fail(ioe.getMessage());
        }
        
        assertHttpStatus(testDirUrl + DEFAULT_EXT, 200, testDirUrl);
        
        try {
            testClient.mkdir(testDirUrl);
        } catch(IOException ioe) {
            fail("mkdir must succeed on an existing directory, got IOException:" + ioe);
        }
        
        assertHttpStatus(testDirUrl + DEFAULT_EXT, 200, testDirUrl);
        
        testClient.delete(testDirUrl);
        assertHttpStatus(testDirUrl, 404, testDirUrl + " must be gone after DELETE");
    }
    
    /** Try creating a deep directory structure */
    public void testMkdirDeep() throws IOException {
        final String path = TEST_PATH + "/mkdir-test-" + System.currentTimeMillis() + "/something";
        final String url = WEBDAV_BASE_URL + path;
        assertHttpStatus(url,404,url + " must not exist before test");
        try {
            testClient.mkdirs(WEBDAV_BASE_URL, path);
        } catch(IOException ioe) {
            fail("mkdirs failed:" + ioe);
        }
        assertHttpStatus(url + DEFAULT_EXT,200,url + " must exist after test");
    }
}
