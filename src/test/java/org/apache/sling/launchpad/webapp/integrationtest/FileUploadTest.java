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
import java.io.InputStream;

import org.apache.sling.commons.testing.integration.HttpTestBase;

/** Test uploading files to the Sling repository
 *  via WebDAV, as a first step towards testing the
 *  complete content creation and rendering scenario. 
 */
public class FileUploadTest extends HttpTestBase {
    
    /** This only tests the WebDAV interface. We know it works, so
     *  we're mostly testing our test code here ;-)
     */
    public void testUploadAndDelete() throws IOException {
        final String testFile = "/integration-test/testfile.txt";
        final InputStream data = getClass().getResourceAsStream(testFile);
        try {
            assertNotNull("Local test file " + testFile + " must be found",data);
            
            final String webdavUrl = WEBDAV_BASE_URL + "/FileUploadTest." + System.currentTimeMillis() + ".txt";
            
            // Upload a file via WebDAV, verify, delete and verify
            assertHttpStatus(webdavUrl, 404, "Resource " + webdavUrl + " must not exist before test");
            int status = testClient.upload(webdavUrl, data);
            assertEquals("upload must return status code 201",201,status);
            assertHttpStatus(webdavUrl, 200, "Resource " + webdavUrl + " must exist after upload");
            testClient.delete(webdavUrl);
            assertHttpStatus(webdavUrl, 404, "Resource " + webdavUrl + " must not exist anymore after deleting");
        } finally {
            if(data!=null) {
                data.close();
            }
        }
    }
}
