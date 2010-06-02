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

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.sling.commons.testing.integration.HttpTestBase;
import org.apache.sling.servlets.post.SlingPostConstants;

/**
 * Test the "org.apache.sling.api.include.servlet" and
 * "org.apache.sling.api.include.resource" request attributes with the {link
 * ScriptHelper#include) functionality
 */
public class RequestAttributeTest extends HttpTestBase {

    private String nodeUrlA;

    private String nodeUrlB;

    private String scriptPath;

    private Set<String> toDelete = new HashSet<String>();

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // Create the test nodes under a path that's specific to this class to
        // allow collisions
        final String url = HTTP_BASE_URL + "/" + getClass().getSimpleName()
            + "/" + System.currentTimeMillis()
            + SlingPostConstants.DEFAULT_CREATE_SUFFIX;
        final Map<String, String> props = new HashMap<String, String>();

        // Create two test nodes and store their paths
        props.put("text", "texta");
        props.put("jcr:primaryType", "nt:unstructured");
        nodeUrlA = testClient.createNode(url, props);

        props.clear();
        props.put("text", "textb");
        props.put("jcr:primaryType", "nt:unstructured");
        nodeUrlB = testClient.createNode(nodeUrlA + "/child", props);

        // The main rendering script goes under /apps in the repository
        scriptPath = "/apps/nt/unstructured";
        testClient.mkdirs(WEBDAV_BASE_URL, scriptPath);
        toDelete.add(uploadTestScript(scriptPath, "request-attribute-test.esp",
            "txt.esp"));
        toDelete.add(uploadTestScript(scriptPath,
            "request-attribute-test-sel1.esp", "sel1.txt.esp"));
        toDelete.add(uploadTestScript(scriptPath,
            "request-attribute-test-sel2.esp", "sel2.txt.esp"));
        toDelete.add(uploadTestScript(scriptPath,
            "request-attribute-test-sel3.esp", "sel3.txt.esp"));
    }

    @Override
    protected void tearDown() throws Exception {
        for (String script : toDelete) {
            testClient.delete(script);
        }
        super.tearDown();
    }

    public void testRequestAttribute() throws Exception {
        final String content = getContent(nodeUrlA + ".txt", CONTENT_TYPE_PLAIN);

        final Properties props = new Properties();
        props.load(new ByteArrayInputStream(content.getBytes("ISO-8859-1")));

        final String pA = new URL(nodeUrlA).getPath();
        final String pB = new URL(nodeUrlB).getPath();

        // this is from txt.esp
        assertEquals("Request Servlet 0 is null", "null",
            props.get("servlet00"));
        assertEquals("Request Resource 0 is null", "null",
            props.get("resource00"));
        assertEquals("Request Servlet 1 is null", "null",
            props.get("servlet01"));
        assertEquals("Request Resource 1 is null", "null",
            props.get("resource01"));

        // this is from sel1.txt.esp, included by txt.esp
        assertEquals("Request Servlet 10", "/apps/nt/unstructured/txt.esp",
            props.get("servlet10"));
        assertEquals("Request Resource 10", pA, props.get("resource10"));
        assertEquals("Request Servlet 11", "/apps/nt/unstructured/txt.esp",
            props.get("servlet11"));
        assertEquals("Request Resource 11", pA, props.get("resource11"));

        // this is from sel2.txt.esp, included by txt.esp
        assertEquals("Request Servlet 20", "/apps/nt/unstructured/txt.esp",
            props.get("servlet20"));
        assertEquals("Request Resource 20", pA, props.get("resource20"));

        // this is from sel3.txt.esp, included by sel1.txt.esp
        assertEquals("Request Servlet 30",
            "/apps/nt/unstructured/sel1.txt.esp", props.get("servlet30"));
        assertEquals("Request Resource 30", pB, props.get("resource30"));
    }
}
