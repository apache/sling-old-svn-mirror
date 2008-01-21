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

import java.io.File;
import java.io.IOException;

import org.apache.commons.httpclient.methods.GetMethod;

/**
 * Test uploading files using the ujax post servlet (SLING-168)
 */
public class UploadFileTest extends UslingHttpTestBase {

    private static final String TEST_FILE = "src/test/resources/integration-test/file-to-upload.txt";
    
    public void testDistinctResource() throws IOException {
        String folderPath = "/UploadFileTest_1_" + System.currentTimeMillis();
        final String url = HTTP_BASE_URL + folderPath;

        // create new node
        String urlOfNewNode = null;
        try {
            urlOfNewNode = testClient.createNode(url, null);
        } catch(IOException ioe) {
            fail("createNode failed: " + ioe);
        }

        // upload local file
        File localFile = new File(TEST_FILE);
        testClient.uploadToFileNode(urlOfNewNode, localFile, "./file", null);

        // get and check URL of created file
        String urlOfFileNode = urlOfNewNode + "/file";
        final GetMethod get = new GetMethod(urlOfFileNode);
        final int status = httpClient.executeMethod(get);
        assertEquals(urlOfFileNode + " must be accessible after createNode",200,status);

        /*
        We should check the data, but nt:resources are not handled yet
        // compare data with local file (just length)
        final byte[] data = get.getResponseBody();
        assertEquals("size of file must be same", localFile.length(), data.length);
        */
        String data = get.getResponseBodyAsString();
        assertTrue("checking for content", data.contains("http://www.apache.org/licenses/LICENSE-2.0"));

        // download structure
        String json = getContent(urlOfFileNode + ".json", CONTENT_TYPE_JSON);
        // just check for some strings
        assertTrue("checking primary type", json.contains("\"jcr:primaryType\":\"nt:resource\""));
        assertTrue("checking mime type", json.contains("\"jcr:mimeType\":\"text/plain\""));

    }

    public void testDistinctResourceWithType() throws IOException {
        String folderPath = "/UploadFileTest_1_" + System.currentTimeMillis();
        final String url = HTTP_BASE_URL + folderPath;

        // create new node
        String urlOfNewNode = null;
        try {
            urlOfNewNode = testClient.createNode(url, null);
        } catch(IOException ioe) {
            fail("createNode failed: " + ioe);
        }

        // upload local file
        File localFile = new File(TEST_FILE);
        testClient.uploadToFileNode(urlOfNewNode, localFile, "./file", "nt:unstructured");

        // get and check URL of created file
        String urlOfFileNode = urlOfNewNode + "/file";
        final GetMethod get = new GetMethod(urlOfFileNode);
        final int status = httpClient.executeMethod(get);
        assertEquals(urlOfFileNode + " must be accessible after createNode",200,status);

        /*
        We should check the data, but nt:resources are not handled yet
        // compare data with local file (just length)
        final byte[] data = get.getResponseBody();
        assertEquals("size of file must be same", localFile.length(), data.length);
        */
        String data = get.getResponseBodyAsString();
        assertTrue("checking for content", data.contains("http://www.apache.org/licenses/LICENSE-2.0"));

        // download structure
        String json = getContent(urlOfFileNode + ".json", CONTENT_TYPE_JSON);
        // just check for some strings
        assertTrue("checking primary type", json.contains("\"jcr:primaryType\":\"nt:unstructured\""));
        assertTrue("checking mime type", json.contains("\"jcr:mimeType\":\"text/plain\""));
    }

    public void testDistinctFile() throws IOException {
        String folderPath = "/UploadFileTest_1_" + System.currentTimeMillis();
        testClient.mkdirs(WEBDAV_BASE_URL, folderPath);
        final String url = HTTP_BASE_URL + folderPath;


        // upload local file
        File localFile = new File(TEST_FILE);
        testClient.uploadToFileNode(url, localFile, "./file", null);

        // get and check URL of created file
        String urlOfFileNode = url + "/file";
        
        /*
        TODO: does not work, since no nt:file resource type handler present ???

        final GetMethod get = new GetMethod(urlOfFileNode);
        final int status = httpClient.executeMethod(get);
        assertEquals(urlOfFileNode + " must be accessible after createNode",200,status);

        // compare data with local file (just length)
        final byte[] data = get.getResponseBody();
        assertEquals("size of file must be same", localFile.length(), data.length);
        */
        
        String webdavUrl = WEBDAV_BASE_URL + folderPath + "/file";
        final GetMethod get = new GetMethod(webdavUrl);
        final int status = httpClient.executeMethod(get);
        assertEquals(urlOfFileNode + " must be accessible after createNode",200,status);

        // compare data with local file (just length)
        final byte[] data = get.getResponseBody();
        assertEquals("size of file must be same", localFile.length(), data.length);

        // download structure
        String json = getContent(urlOfFileNode + ".json", CONTENT_TYPE_JSON);
        // just check for some strings
        assertTrue("checking primary type", json.contains("\"jcr:primaryType\":\"nt:file\""));
    }

}