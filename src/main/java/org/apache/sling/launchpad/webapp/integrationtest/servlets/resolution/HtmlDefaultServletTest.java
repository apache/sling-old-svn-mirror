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
package org.apache.sling.launchpad.webapp.integrationtest.servlets.resolution;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.testing.tools.retry.RetryLoop;
import org.apache.sling.testing.tools.retry.RetryLoop.Condition;

/** Test for SLING-1069 */
public class HtmlDefaultServletTest extends ResolutionTestBase {

    public static final String CONFIG_SERVLET = HTTP_BASE_URL + "/system/console/configMgr/org.apache.sling.launchpad.testservices.servlets.HtmlDefaultServlet";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        // enable the HtmlDefaultServlet before testing
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("apply", "true");
        properties.put("GET", "sling.servlet.methods");
        properties.put("propertylist", "sling.servlet.methods");
        assertEquals(302, testClient.post(CONFIG_SERVLET, properties));
    }

    @Override
    protected void tearDown() throws Exception {
        // disable the HtmlDefaultServlet after testing
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("apply", "true");
        properties.put("delete", "true");
        assertEquals(200, testClient.post(CONFIG_SERVLET, properties));
        
        // Verify that our test servlet is gone
        final String url = testNodeNORT.nodeUrl + ".html"; 
        final Condition c = new Condition() {
            public String getDescription() {
                return url + " is not served by a test servlet";
            }

            public boolean isTrue() throws Exception {
                assertNotTestServlet(getContent(url, CONTENT_TYPE_HTML));
                return true;
            }
        };
        new RetryLoop(c, 10, 100);
        
        // we need to wait a little bit as starting with version 1.1.12 of
        // org.apache.sling.resourceresolver, unregistering a resource provider
        // forces an async reregistration of the resource resolver factory
        try {
            Thread.sleep(3000);
        } catch ( final InterruptedException ie ) {
            // ignore
        }
        super.tearDown();
    }

    public void testHtmlExtension() throws IOException {
        final String url = testNodeNORT.nodeUrl + ".html"; 
        final Condition c = new Condition() {
            public String getDescription() {
                return url + " returns plain text";
            }

            public boolean isTrue() throws Exception {
                assertServlet(getContent(url, CONTENT_TYPE_PLAIN), HTML_DEFAULT_SERVLET_SUFFIX);
                return true;
            }
        };
        new RetryLoop(c, 10, 100);
    }

    public void testJsonExtension() throws IOException {
        final String url = testNodeNORT.nodeUrl + ".json"; 
        final Condition c = new Condition() {
            public String getDescription() {
                return url + " returns JSON";
            }

            public boolean isTrue() throws Exception {
                assertNotTestServlet(getContent(url,CONTENT_TYPE_DONTCARE));
                return true;
            }
        };
        new RetryLoop(c, 10, 100);
    }
}