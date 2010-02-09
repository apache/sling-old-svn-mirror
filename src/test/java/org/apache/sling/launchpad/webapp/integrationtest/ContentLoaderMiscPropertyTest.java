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
 * Test that name, path, and (in JCR 2, uri) properties are set correctly.
 */
public class ContentLoaderMiscPropertyTest extends HttpTestBase {

    /**
     * Verify that the test node's node type is set correctly.
     */
    public void testLoadedNodeType() throws IOException {
        final String expected = "sling:propertySetTestNodeType";
        final String content = getContent(
                HTTP_BASE_URL + "/sling-test/property-types-test/test-node.txt", CONTENT_TYPE_PLAIN);
        assertTrue("Content contains " + expected + " (" + content + ")", content.contains(expected));
    }

    /**
     * Verify that the URI-type property loaded correctly.
     *
     * This test is only really useful post-JCR 2 upgrade.
     */
    public void testLoadedURI() throws IOException {
        final String expected = "http://www.google.com/";
        final String content = getContent(
                HTTP_BASE_URL + "/sling-test/property-types-test/test-node/uri.txt", CONTENT_TYPE_PLAIN);
        assertTrue("Content contains " + expected + " (" + content + ")", content.contains(expected));
    }

    /**
     * Verify that the Name-type property loaded correctly.
     *
     * This test is only really useful post-JCR 2 upgrade.
     */

    public void testLoadedName() throws IOException {
        final String expected = "sling:test";
        final String content = getContent(
                HTTP_BASE_URL + "/sling-test/property-types-test/test-node/name.txt", CONTENT_TYPE_PLAIN);
        assertTrue("Content contains " + expected + " (" + content + ")", content.contains(expected));
    }

    /**
     * Verify that the Path-type property loaded correctly.
     *
     * This test is only really useful post-JCR 2 upgrade.
     */

    public void testLoadedPath() throws IOException {
        final String expected = "/sling-test/initial-content-folder/folder-content-test";
        final String content = getContent(
                HTTP_BASE_URL + "/sling-test/property-types-test/test-node/path.txt", CONTENT_TYPE_PLAIN);
        assertTrue("Content contains " + expected + " (" + content + ")", content.contains(expected));
    }

}
