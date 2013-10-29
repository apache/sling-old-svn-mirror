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
package org.apache.sling.launchpad.webapp.integrationtest.issues;

import java.util.HashMap;
import java.util.Map;
import org.apache.sling.launchpad.webapp.integrationtest.RenderingTestBase;

public class SLING457Test extends RenderingTestBase {

    private String testRootUrl;

    private String contentUrl;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        final String testRootPathBare = "/" + getClass().getSimpleName() + "/"
            + System.currentTimeMillis();
        final String testRootPath = HTTP_BASE_URL + testRootPathBare;

        // the root for our tests
        testRootUrl = testClient.createNode(testRootPath, null);

        // create the Resource Type "a" location
        String rtA = testRootPathBare + "/a";
        testClient.createNode(HTTP_BASE_URL + rtA, null);

        // create the Resource Type "b" location
        String rtB = testRootPathBare + "/b";
        Map<String, String> props = new HashMap<String, String>();
        props.put("sling:resourceSuperType", rtA);
        testClient.createNode(HTTP_BASE_URL + rtB, props);

        // create content node "x" of type rtB
        String rtX = testRootPathBare + "/x";
        props.clear();
        props.put("sling:resourceType", rtB);
        contentUrl = testClient.createNode(HTTP_BASE_URL + rtX, props);

        // create content node "x/image" of type rtB
        String rtXImage = rtX + "/image";
        props.clear();
        props.put("sling:resourceType", "nt:unstructured");
        testClient.createNode(HTTP_BASE_URL + rtXImage, props);

        uploadTestScript(rtA, "issues/sling457/a-foo.html.jsp", "foo.html.jsp");
        uploadTestScript(rtB, "issues/sling457/b-b.jsp", "b.jsp");

    }

    @Override
    protected void tearDown() throws Exception {
        testClient.delete(testRootUrl);
        super.tearDown();
    }

    public void testCallFooHtml() throws Exception {
        String url = contentUrl + ".foo.html";
        String content = getContent(url, CONTENT_TYPE_HTML);
        assertTrue("Expected 'foo.html.jsp' in content",
            content.indexOf("foo.html.jsp") >= 0);
    }

    public void testCallHtml() throws Exception {
        String url = contentUrl + ".html";
        String content = getContent(url, CONTENT_TYPE_HTML);
        assertTrue("Expected 'foo.html.jsp' in content",
            content.indexOf("foo.html.jsp") >= 0);
    }
}
