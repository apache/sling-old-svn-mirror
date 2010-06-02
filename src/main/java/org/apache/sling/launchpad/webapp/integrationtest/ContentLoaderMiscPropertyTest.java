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

import org.apache.sling.commons.testing.integration.HttpTestBase;

/**
 * Test that property types are supported in initial content.
 */
public class ContentLoaderMiscPropertyTest extends HttpTestBase {

    /**
     * Verify that the test node's properties are set correctly.
     */
    public void testLoaded() throws IOException {
        final String content = getContent(
                HTTP_BASE_URL + "/sling-test/property-types-test/test-node.txt", CONTENT_TYPE_PLAIN);

        assertPropertyValue(content, "Resource type", "sling:propertySetTestNodeType");
        assertPropertyValue(content, "string", "Sling");
        assertPropertyValue(content, "strings", "[Apache, Sling]");
        assertPropertyValue(content, "long", "42");
        assertPropertyValue(content, "longs", "[4, 8, 15, 16, 23, 42]");
        assertPropertyValue(content, "boolean", "true");
        assertPropertyValue(content, "booleans", "[true, false]");
        assertPropertyValue(content, "uri", "http://www.google.com/");
        assertPropertyValue(content, "uris", "[http://sling.apache.org/, http://www.google.com/]");
        assertPropertyValue(content, "name", "sling:test");
        assertPropertyValue(content, "names", "[jcr:base, sling:test]");
        assertPropertyValue(content, "path", "/sling-test/initial-content-folder/folder-content-test");
        assertPropertyValue(content, "paths", "[/sling-test/initial-content-folder/folder-content-test, /apps]");
    }

    private void assertPropertyValue(String content, String name, String value) {
        final String expected = String.format("%s: %s", name, value);
        assertTrue("Content contains " + expected + " (" + content + ")", content.contains(expected));
    }

}
