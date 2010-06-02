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

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.methods.GetMethod;

public class PathsServletTest extends ResolutionTestBase {

    private final static String[] TEST_PATH = { 
        "/testing/PathsServlet/foo",
        "/testing/PathsServlet/bar/more/foo.html" 
    };
    
    public void testGetCorrectPaths() throws Exception {
        for (String path : TEST_PATH) {
            assertServlet(getContent(HTTP_BASE_URL + path, CONTENT_TYPE_PLAIN),
                    PATHS_SERVLET_SUFFIX);
        }
    }
    
    public void testSubpath() throws Exception {
        final String extra = "/something";
        for (String p : TEST_PATH) {
            String path = p + extra;
            GetMethod get = new GetMethod(HTTP_BASE_URL + path);
            int status = httpClient.executeMethod(get);
            assertFalse("Expected non-200 status for path " + path, HttpServletResponse.SC_OK == status);
        }
    }
}
