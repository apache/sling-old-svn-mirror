/* Licensed to the Apache Software Foundation (ASF) under one or more
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

import org.apache.sling.commons.testing.integration.HttpTestBase;

public class InitialContentTest extends HttpTestBase {

    public void testIndex() throws IOException {
        final String expected = "Do not remove this comment, used for Launchpad integration tests";
        final String content = getContent(HTTP_BASE_URL + "/index.html", CONTENT_TYPE_HTML);
        assertTrue("Content contains expected marker (" + content + ")",content.contains(expected));
    }

    public void testRootRedirectProperty() throws IOException {
    	final String expected = "\"sling:resourceType\":\"sling:redirect\"";
    	final String content = getContent(HTTP_BASE_URL + "/.json", CONTENT_TYPE_JSON);
    	assertTrue("Content contains " + expected + " (" + content + ")", content.contains(expected));
    }

    public void testInitialContentA() throws IOException {
    	final String expected = "42";
    	final String content = getContent(
    			HTTP_BASE_URL + "/sling-test/sling/initial-content-test/marker.txt", CONTENT_TYPE_PLAIN);
    	assertTrue("Content contains " + expected + " (" + content + ")", content.contains(expected));
    }

    public void testInitialContentB() throws IOException {
    	final String expected = "46";
    	final String content = getContent(
    			HTTP_BASE_URL + "/sling-test/initial-content-folder/folder-content-test/marker.txt", CONTENT_TYPE_PLAIN);
    	assertTrue("Content contains " + expected + " (" + content + ")", content.contains(expected));
    }

    public void testInitialContentInSubNodeRoot() throws IOException {
        final String expected = "\"testProperty\":\"value\"";
        final String content = getContent(
                HTTP_BASE_URL + "/sling-test.json", CONTENT_TYPE_JSON);
        assertTrue("Content contains " + expected + " (" + content + ")", content.contains(expected));
    }
}
