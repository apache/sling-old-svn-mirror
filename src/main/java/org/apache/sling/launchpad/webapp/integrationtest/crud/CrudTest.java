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
package org.apache.sling.launchpad.webapp.integrationtest.crud;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.NameValuePair;
import org.apache.sling.commons.testing.integration.NameValuePairList;
import org.apache.sling.launchpad.webapp.integrationtest.RenderingTestBase;
import org.apache.sling.servlets.post.SlingPostConstants;

public class CrudTest extends RenderingTestBase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        final String slingResourceType = getClass().getName();

        this.scriptPath = "/apps/" + slingResourceType;
        this.testClient.mkdirs(WEBDAV_BASE_URL, scriptPath);

        // create the test node, under a path that's specific to this class to allow collisions
        final String url = HTTP_BASE_URL + "/" + getClass().getSimpleName() + "/" + System.currentTimeMillis() + SlingPostConstants.DEFAULT_CREATE_SUFFIX;

        final NameValuePairList list = new NameValuePairList();
        list.add("sling:resourceType", slingResourceType);

        this.displayUrl = testClient.createNode(url, list, null, true);

    }

    @Override
    protected void tearDown() throws Exception {
        testClient.delete(this.displayUrl);
        super.tearDown();
    }

    public void testCreate() throws Exception {
        final String testScriptPath = this.uploadTestScript("crud/crud-test.jsp", "html.jsp");

        final String name = getClass().getSimpleName() + System.currentTimeMillis();
        final List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new NameValuePair("name", name));
        params.add(new NameValuePair("a", "100"));
        params.add(new NameValuePair("b", "200"));
        try {
            final String content = getContent(displayUrl + ".html", CONTENT_TYPE_PLAIN, params);
            assertTrue("Content should included created marker: " + content, content.contains("created"));

            final String json = getContent(HTTP_BASE_URL + "/" + name + ".json", CONTENT_TYPE_JSON);
            assertJavascript("100", json, "out.print(data.a)");
            assertJavascript("200", json, "out.print(data.b)");

        } finally {
            testClient.delete(testScriptPath);
        }

    }
}
