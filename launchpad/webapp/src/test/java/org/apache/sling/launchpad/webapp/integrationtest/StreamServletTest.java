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


/** Test the streaming of static files uploaded to the repository */
public class StreamServletTest extends RenderingTestBase {
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        scriptPath = TEST_PATH + "/StreamServletTest." + System.currentTimeMillis();
        displayUrl = HTTP_BASE_URL + scriptPath;
        testClient.mkdirs(WEBDAV_BASE_URL, scriptPath);
    }
    
    private void runTest(String filename, String expectedContentType, String expectedContent) throws Exception {
        final String toDelete = uploadTestScript(filename,filename);
        try {
            final String url = displayUrl + "/" + filename;
            final String content = getContent(url, expectedContentType);
            assertEquals(expectedContent, content);
        } finally {
            testClient.delete(toDelete);
        }
    }
    
    public void testPlainTextFile() throws Exception {
        runTest("testfile.txt", CONTENT_TYPE_PLAIN, "This is just some text in an ASCII file.");
    }

    public void testHtmlFile() throws Exception {
        runTest("testfile.html", CONTENT_TYPE_HTML, "This is <em>testfile.html</em>");
    }
    
    public void testJavascriptFile() throws Exception {
        runTest("testfile.js", CONTENT_TYPE_JS, "// This is testfile.js");
    }
    
    public void testJsonFile() throws Exception {
        runTest("testfile.json", CONTENT_TYPE_JSON, "This is testfile.json");
    }
}
