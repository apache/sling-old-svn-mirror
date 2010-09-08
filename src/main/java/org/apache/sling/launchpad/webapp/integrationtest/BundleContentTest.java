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

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.testing.integration.HttpTestBase;

public class BundleContentTest extends HttpTestBase {

    public void testBundleContentRetrieval() throws IOException {
        final String expected = "This is a text file provided by the bundle resource provider.";
        final String content = getContent(HTTP_BASE_URL + "/sling-test/sling/from-bundle/foo.txt", CONTENT_TYPE_PLAIN);
        assertTrue("Content contains " + expected + " (" + content + ")", content.contains(expected));
    }

    public void testBundleContentList() throws IOException {
        final String expected = "foo.txt";
        final String content = getContent(HTTP_BASE_URL + "/sling-test/sling/from-bundle.2.json", CONTENT_TYPE_JSON);
        assertTrue("Content contains " + expected + " (" + content + ")", content.contains(expected));
    }
    
    public void testBundleContentParentList() throws IOException {
        final String expected = "from-bundle";
        final String content = getContent(HTTP_BASE_URL + "/sling-test/sling.2.json", CONTENT_TYPE_JSON);
        assertTrue("Content contains " + expected + " (" + content + ")", content.contains(expected));
    }
    
    /**
     * Test fix for SLING-1733 - BundleResourceProvider fails to find resources when multiple bundles have the same Sling-Bundle-Resources path
     */
    public void testBundleContentParentFromMultipleBundles() throws IOException, JSONException {
        final String content = getContent(HTTP_BASE_URL + "/system.1.json", CONTENT_TYPE_JSON);
        JSONObject jsonObj = new JSONObject(content);

        //provided by the servlets.post bundle
        assertTrue("Expected sling.js in the /system folder", jsonObj.has("sling.js"));

        //provided by the launchpad.test-services bundle
        assertTrue("Expected sling-1733.txt in the /system folder", jsonObj.has("sling-1733.txt"));
    }
}
