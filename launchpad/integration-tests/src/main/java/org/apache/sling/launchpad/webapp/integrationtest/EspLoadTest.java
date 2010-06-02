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

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.sling.commons.testing.integration.HttpTestBase;

/** Test the SLING-428 esp load function */
public class EspLoadTest extends HttpTestBase {
    
    private String basePath;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        basePath = "/" + getClass().getSimpleName() + "_" + System.currentTimeMillis();
    }
    
    public void testNestedInclude() throws Exception {
        final Map<String,String> props = new HashMap<String,String>();
        props.put("scriptToInclude", "included-a.esp");
        props.put(SLING_RESOURCE_TYPE, getClass().getSimpleName());
        final TestNode tn = new TestNode(HTTP_BASE_URL + basePath, props);
        final String subfolder = tn.scriptPath + "/subfolder";
        testClient.mkdirs(WEBDAV_BASE_URL, subfolder);
        final String [] toDelete = {
                uploadTestScript(tn.scriptPath, "esp-load/main.esp", "html.esp"),
                uploadTestScript(tn.scriptPath, "esp-load/included-a.esp", "included-a.esp"),
                uploadTestScript(subfolder, 
                        "esp-load/subfolder/included-b.esp", "included-b.esp")
        };

        try {
            final String content = getContent(tn.nodeUrl + ".html", CONTENT_TYPE_HTML);
            
            final String [] expectedStringsInOrder = {
                    "main.esp before load",
                    "included-a.esp before load",
                    "included-b.esp",
                    "included-a.esp after load",
                    "main.esp after load",
                    "Here's more from included-a"
            };
            
            int pos = 0;
            for(String expected : expectedStringsInOrder) {
                final int newPos = content.indexOf(expected);
                assertTrue("Content (" + content + ") must contain '" + expected + "'", newPos >= 0);
                assertTrue("String '" + expected + "' must come after previous expected string", newPos > pos);
                pos = newPos;
            }
        } finally {
            for(String s : toDelete) {
                testClient.delete(s);
            }
        }
    }
    
    public void testNonExistentInclude() throws Exception {
        final Map<String,String> props = new HashMap<String,String>();
        final String badScript = "nonexistent.esp";
        props.put("scriptToInclude", badScript);
        props.put(SLING_RESOURCE_TYPE, getClass().getSimpleName());
        final TestNode tn = new TestNode(HTTP_BASE_URL + basePath, props);
        final String toDelete = uploadTestScript(tn.scriptPath, "esp-load/main.esp", "html.esp");
        try {
            assertHttpStatus(tn.nodeUrl + ".html", HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Including " + badScript + " must fail");
        } finally {
            testClient.delete(toDelete);
        }
    }
}
