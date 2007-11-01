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


/** Test the StreamServlet by reading an uploaded file with a GET
 */
public class StreamServletTest extends RenderingTestBase {
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        scriptPath = TEST_PATH + "/StreamServletTest." + System.currentTimeMillis();
        displayUrl = HTTP_BASE_URL + scriptPath;
        testClient.mkdirs(WEBDAV_BASE_URL, scriptPath);
    }

    public void testPlainTextFile() throws Exception {
        final String filename = "testfile.txt";
        final String toDelete = uploadTestScript(filename,filename);
        try {
            final String url = displayUrl + "/" + filename;
            // TODO why don't we get a content-type here?
            final String content = getContent(url, null);
            assertTrue(
                    "Content at " + url + " must include expected marker, got " + content,
                    content.contains("This is just some text in an ASCII file.")
            );
        } finally {
            // TODO testClient.delete(toDelete);
        }
    }

    public void testHtmlTextFile() throws Exception {
        final String filename = "testfile.html";
        final String toDelete = uploadTestScript(filename,filename);
        try {
            // TODO this should really be text/html, not sure why it is not
            final String url = displayUrl + "/" + filename;
            // TODO why don't we get a content-type here?
            final String content = getContent(url, null);
            assertTrue(
                    "Content at " + url + " must include expected marker, got " + content,
                    content.contains("This is <em>testfile.html</em>.")
            );
        } finally {
            // TODO testClient.delete(toDelete);
        }
    }
}
