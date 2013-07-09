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
import org.apache.sling.servlets.post.SlingPostConstants;

/** Test the generation of node names for POST requests to URLS
 *  ending in / *
 */
public class GeneratedNodeNameTest extends HttpTestBase {

    private final String postFolder = HTTP_BASE_URL + "/" + getClass().getSimpleName() ;
    private final String postUrl = postFolder + "/" + System.currentTimeMillis() + SlingPostConstants.DEFAULT_CREATE_SUFFIX;

    protected void setUp() throws Exception {

        super.setUp();
        testClient.delete(postFolder);
    }

    protected void tearDown() throws Exception {

        testClient.delete(postFolder);
        super.tearDown();
    }

    public void testTitle() throws IOException {
        final Map<String,String> props = new HashMap<String,String>();
        props.put("title", "Hello There");
        final String location = testClient.createNode(postUrl, props);
        final String expect = "hello_there";
        assertTrue("Location " + location + " ends with " + expect,location.endsWith(expect));
    }

    public void testSlingPostNodeNameParam() throws IOException {
        final Map<String,String> props = new HashMap<String,String>();
        props.put(":name", "MyNodeName");
        final String location = testClient.createNode(postUrl, props);
        final String expect = "MyNodeName";
        assertTrue("Location " + location + " ends with " + expect,location.endsWith(expect));
    }

    public void testSlingPostNodeNameHintParam() throws IOException {
        final Map<String,String> props = new HashMap<String,String>();
        props.put(":nameHint", "AnotherNodeName");
        final String location = testClient.createNode(postUrl, props);
        final String expect = "AnotherNodeName".toLowerCase();
        assertTrue("Location " + location + " ends with " + expect,location.endsWith(expect));
    }

    public void testTitleWithSavePrefix() throws IOException {
        final Map<String,String> props = new HashMap<String,String>();
        props.put("./title", "Hello There 2");
        props.put("title", "not this one");
        final String location = testClient.createNode(postUrl, props);
        final String expect = "hello_there_2";
        assertTrue("Location " + location + " ends with " + expect,location.endsWith(expect));
    }

    public void testCollision() throws IOException {
        final Map<String,String> props = new HashMap<String,String>();
        props.put("title", "Hello There");

        // posting twice with the same title must work, and return different locations
        final String locationA = testClient.createNode(postUrl, props);
        final String locationB = testClient.createNode(postUrl, props);

        assertFalse("Locations A and B must be different (" + locationA + "," + locationB + ")",
                locationA.equals(locationB));
    }

    public void testNoParams() throws IOException {
        final String location = testClient.createNode(postUrl, null);
        assertTrue("Location end with a digit",Character.isDigit(location.charAt(location.length() - 1)));
    }
}
