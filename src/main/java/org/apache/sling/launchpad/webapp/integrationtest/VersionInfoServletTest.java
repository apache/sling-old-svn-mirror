/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.launchpad.webapp.integrationtest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.json.JsonException;
import javax.json.JsonObject;

import org.apache.commons.httpclient.HttpException;
import org.apache.sling.commons.testing.integration.HttpTestBase;
import org.apache.sling.launchpad.webapp.integrationtest.util.JsonUtil;
import org.apache.sling.servlets.post.SlingPostConstants;

public class VersionInfoServletTest extends HttpTestBase {

    public static final String TEST_BASE_PATH = "/sling-tests";

    public static final String CONFIG_SERVLET = HTTP_BASE_URL + "/system/console/configMgr/org.apache.sling.servlets.get.impl.version.VersionInfoServlet";

    private String postUrl;

    private Map<String,String> params;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        postUrl = HTTP_BASE_URL + TEST_BASE_PATH + "/" + System.currentTimeMillis();
        params = new HashMap<String,String>();
        params.put("jcr:mixinTypes", "mix:versionable");
    }

    private String createVersionableNode() throws IOException {
        params.put(":checkinNewVersionableNodes", "true");
        final String location = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, params);

        final String content = getContent(location + ".txt", CONTENT_TYPE_PLAIN);
        assertTrue("Node (" + location + ") should be checked in.", content.contains("jcr:isCheckedOut: false"));
        return location;
    }

    public void testDisabledServlet() throws IOException, JsonException, InterruptedException {
        deleteConfiguration();
        waitUntilSlingIsStable();
        getContent(createVersionableNode() + ".V.json", CONTENT_TYPE_HTML, null, 400);
    }

    public void testStandardVersionsList() throws IOException, JsonException, InterruptedException {
        createConfiguration("V");
        waitUntilSlingIsStable();
        final JsonObject versions = JsonUtil.parseObject(getContent(createVersionableNode() + ".V.json", CONTENT_TYPE_JSON));
        assertTrue("Expecting versions subtree", versions.containsKey("versions"));
        assertTrue("Expecting root version", versions.getJsonObject("versions").containsKey("jcr:rootVersion"));
        assertTrue("Expecting version 1.0", versions.getJsonObject("versions").containsKey("1.0"));

        final JsonObject oneZero = versions.getJsonObject("versions").getJsonObject("1.0");
        assertTrue("Expecting non-empty creation date", oneZero.getString("created").length() > 0);
        assertEquals("Expecting no successors", 0, oneZero.getJsonArray("successors").size());
        assertEquals("Expecting root version predecessor", "jcr:rootVersion", oneZero.getJsonArray("predecessors").getString(0));
        assertEquals("Expecting no labels", 0, oneZero.getJsonArray("labels").size());
        assertEquals("Expecting true baseVersion", "true", oneZero.getString("baseVersion"));
    }

    public void testHarrayVersionsList() throws Exception {
        // No need to test everything in detail, just verify that the output
        // is reformatted with the harray option
        createConfiguration("V");
        waitForSlingStartup();
        waitUntilSlingIsStable();
        final JsonObject versions = JsonUtil.parseObject(getContent(createVersionableNode() + ".V.harray.json", CONTENT_TYPE_JSON));
        assertTrue("Expecting children array", versions.containsKey("__children__"));
        assertEquals("Expecting versions object", "versions",
                versions.getJsonArray("__children__").getJsonObject(0).getString("__name__"));
    }

    private void createConfiguration(final String selector) throws IOException {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("apply", "true");
        properties.put("sling.servlet.selectors", selector);
        properties.put("propertylist", "sling.servlet.selectors");
        assertEquals(302, testClient.post(CONFIG_SERVLET, properties));
    }

    private void deleteConfiguration() throws IOException {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("apply", "true");
        properties.put("delete", "true");
        assertEquals(200, testClient.post(CONFIG_SERVLET, properties));
    }

    private void waitUntilSlingIsStable() throws HttpException, IOException,
            InterruptedException {
        for (int i = 0; i < 10; i++) {
            Thread.sleep(1000);
            if (testClient.get(HTTP_BASE_URL  + "/.json") == 200) {
                return;
            }
        }
        fail("Sling instance fails to respond with status 200");
    }
}
