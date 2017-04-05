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
import java.util.Properties;

import org.apache.sling.commons.testing.integration.HttpTestBase;

/** Test the values returned by the HttpServletRequest object server-side,
 *  created for SLING-4804 */
public class RequestObjectTest extends ResolutionTestBase {

    private String path;
    private final String extension = ".TEST_SEL_1.txt";

    private static class TestItem {
        final String requestSuffix;
        final String expectedURISuffix;
        final String expectedURLSuffix;

        TestItem(String s,String uri,String url) {
            requestSuffix = s;
            expectedURISuffix = uri;
            expectedURLSuffix = url;
        }

        public String info(String msg) {
            return msg + ":TestItem with suffix [" + requestSuffix + "]";
        }
    };

    final TestItem [] TESTS = {
            new TestItem("","",""),
            new TestItem(";v=1.1",";v=1.1",";v=1.1"),
            new TestItem(";v=1.1?foo=bar",";v=1.1",";v=1.1"),
            new TestItem(";v=1.1?foo=bar&ga+bu=zo+meu",";v=1.1",";v=1.1")
    };

    @Override
    public void setUp() throws Exception {
        super.setUp();
        path = testNodeNORT.nodeUrl.substring(HttpTestBase.HTTP_BASE_URL.length()) + extension;
    }

    public void testRequestPathInfo() throws IOException {
        for(TestItem t : TESTS) {
            final String content = getContent(testNodeNORT.nodeUrl + ".TEST_SEL_1.txt" + t.requestSuffix, CONTENT_TYPE_PLAIN);
            final Properties props = getTestServletProperties(content);
            assertEquals(t.info("path"), path, props.get("http.request.pathInfo"));
            assertEquals(t.info("URI"), path + t.expectedURISuffix, props.get("http.request.requestURI"));
            assertEquals(t.info("URL"), testNodeNORT.nodeUrl + extension + t.expectedURLSuffix, props.get("http.request.requestURL"));
        }
    }
}
