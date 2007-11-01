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
package org.apache.sling.microsling.integration;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.methods.GetMethod;

/** Base class for rendering tests
 */
class RenderingTestBase extends MicroslingHttpTestBase {

    protected String scriptPath;
    protected String testText;
    protected String displayUrl;

    /** upload rendering test script, and return its URL for future deletion */
    protected String uploadTestScript(String localFilename,String filenameOnServer) throws IOException {
        final String url = WEBDAV_BASE_URL + scriptPath + "/" + filenameOnServer;
        final String testFile = "/integration-test/" + localFilename;
        final InputStream data = getClass().getResourceAsStream(testFile);
        try {
            System.out.println();
            System.out.println();
            System.out.println("Path " + testFile);
            System.out.println("Loader " + getClass().getClassLoader());
            System.out.println("Uploading " + data);
            System.out.println();
            System.out.println();
            testClient.upload(url, data);
        } finally {
            if(data!=null) {
                data.close();
            }
        }
        return url;
    }

    /** retrieve the contents of given URL and assert its content type
     * @throws IOException
     * @throws HttpException */
    protected String getContent(String url, String expectedContentType) throws IOException {
        final GetMethod get = new GetMethod(url);
        final int status = httpClient.executeMethod(get);
        assertEquals("Expected status 200 for " + url,200,status);
        final Header h = get.getResponseHeader("Content-Type");
        if(expectedContentType == null) {
            if(h!=null) {
                fail("Expected null Content-Type, got " + h.getValue());
            }
        } else {
            assertTrue(
                "Expected Content-Type '" + expectedContentType + "' for " + url,
                h.getValue().startsWith(expectedContentType)
            );
        }
        return get.getResponseBodyAsString();
    }

}