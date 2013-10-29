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
package org.apache.sling.launchpad.webapp.integrationtest.resourceprovider;

import org.apache.sling.commons.testing.integration.HttpTestBase;

/**
 * Test the PlanetsResourceProvider that the test-services bundles provides */ 
public class PlanetsResourceProviderTest extends HttpTestBase {

    private void assertStrings(String url, String [] what) throws Exception {
        final String content = getContent(url, CONTENT_TYPE_JSON);
        for(String expected : what) {
            if(expected.startsWith("!")) {
                assertFalse("NOT expecting '" + expected + "' in " + url, content.contains(expected.substring(1)));
            } else {
                assertTrue("Expecting '" + expected + "' in " + url, content.contains(expected));
            }
        }
    }
    
    public void testRootResource() throws Exception {
        assertStrings(
            HTTP_BASE_URL + "/planets.tidy.-1.json", 
            new String [] {
                    "earth",
                    "moon",
                    "Moon",
                    "Resources can have different sets of properties",
                    "\"name\": \"Uranus\""
            });
    }
    
    public void testEarthResource() throws Exception {
        assertStrings(
                HTTP_BASE_URL + "/planets.tidy.-1.json", 
                new String [] {
                        "earth",
                        "moon",
                        "Moon",
                        "Resources can have different sets of properties",
                        "\"name\": \"Uranus\"",
                        "384"
                });
    }
    
    public void testMoonResource() throws Exception {
        assertStrings(
                HTTP_BASE_URL + "/planets/earth/moon.tidy.json", 
                new String [] {
                        "!earth",
                        "!moon",
                        "Moon",
                        "!Resources can have different sets of properties",
                        "!\"name\": \"Uranus\"",
                        "384"
                });
    }
    
    public void testMoonHtml() throws Exception {
        final String content = getContent(HTTP_BASE_URL + "/planets/earth/moon.html", CONTENT_TYPE_HTML);
        final String expect = "<title>Planet at /planets/earth/moon</title>";
        assertTrue("Expecting content to contain " + expect, content.contains(expect));
    }
}
