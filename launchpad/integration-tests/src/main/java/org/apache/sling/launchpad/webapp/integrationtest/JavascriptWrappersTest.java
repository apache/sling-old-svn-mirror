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
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.commons.testing.integration.HttpTestBase;

/** Test Scriptable objects */
public class JavascriptWrappersTest extends HttpTestBase {

    private TestNode testRootNode;
    private String basePath;

    private void createNodes(TestNode n, String prefix, int levels) throws Exception {
        String url = n.nodeUrl;
        while(levels >= 1) {
            url += "/" + prefix + levels;
            testClient.createNode(url, null);
            levels--;
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        basePath = "/" + getClass().getSimpleName() + "_" + System.currentTimeMillis();

        final Map<String, String> props = new HashMap<String, String>();
        props.put(SLING_RESOURCE_TYPE, getClass().getSimpleName());
        props.put("title", "testnode");

        testRootNode = new TestNode(HTTP_BASE_URL + basePath, props);
        createNodes(testRootNode, "a", 3);
        createNodes(testRootNode, "b", 1);
        createNodes(testRootNode, "c", 2);
    }

    public void testRecursiveDump() throws IOException {
        final String toDelete = uploadTestScript(testRootNode.scriptPath, "dump-resource.ecma", "html.ecma");
        try {
            final String content = getContent(testRootNode.nodeUrl + ".html", CONTENT_TYPE_HTML);

            final String expected =
                "1 " + basePath + "/testnode\n"
                + "2 " + basePath + "/testnode/a3\n"
                + "3 " + basePath + "/testnode/a3/a2\n"
                + "4 " + basePath + "/testnode/a3/a2/a1\n"
                + "2 " + basePath + "/testnode/b1\n"
                + "2 " + basePath + "/testnode/c2\n"
                + "3 " + basePath + "/testnode/c2/c1\n"
                ;
            assertEquals(expected, content);
        } finally {
            testClient.delete(toDelete);
        }

    }
}
