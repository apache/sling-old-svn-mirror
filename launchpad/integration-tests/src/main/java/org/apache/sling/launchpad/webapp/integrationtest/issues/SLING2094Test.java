/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.launchpad.webapp.integrationtest.issues;

import java.util.HashMap;
import java.util.Map;
import org.apache.sling.launchpad.webapp.integrationtest.RenderingTestBase;

/** Test the SLING-2094 JSP errorpage statement */
public class SLING2094Test extends RenderingTestBase {
    public final static String TEST_ROOT = "/apps/sling2094";
    private String testNodePath;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        scriptPath = TEST_ROOT;
        
        testClient.mkdirs(HTTP_BASE_URL, TEST_ROOT);
        for(String file : new String[] { "custom-error-page.jsp", "sling2094.jsp" }) {
            uploadTestScript("issues/sling2094/" + file, file);
        }
        
        final Map<String, String> props = new HashMap<String, String>();
        props.put(SLING_RESOURCE_TYPE, TEST_ROOT);
        testNodePath = testClient.createNode(HTTP_BASE_URL + TEST_ROOT, props);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        testClient.delete(HTTP_BASE_URL + TEST_ROOT);
    }
    
    public void testWithoutError() throws Exception {
        final String expected = "All good, no exception";
        final String url = testNodePath + ".html";
        assertContains(getContent(url, CONTENT_TYPE_HTML), expected);
    }
    
    public void testWithError() throws Exception {
        final String expected = "witherror selector was specified";
        final String url = testNodePath + ".witherror.html";
        assertContains(getContent(url, CONTENT_TYPE_HTML), expected);
    }
}
