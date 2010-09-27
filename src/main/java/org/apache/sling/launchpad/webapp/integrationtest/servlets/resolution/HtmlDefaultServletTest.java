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
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.NameValuePair;

/** Test for SLING-1069 */
public class HtmlDefaultServletTest extends ResolutionTestBase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // enable the HtmlDefaultServlet before testing
        changeComponent(
            "org.apache.sling.launchpad.testservices.servlets.HtmlDefaultServlet",
            "enable");
    }

    @Override
    protected void tearDown() throws Exception {
        // disable the HtmlDefaultServlet before testing
        changeComponent(
            "org.apache.sling.launchpad.testservices.servlets.HtmlDefaultServlet",
            "disable");
        super.tearDown();
    }

    public void testHtmlExtension() throws IOException {
        assertServlet(
            getContent(testNodeNORT.nodeUrl + ".html", CONTENT_TYPE_PLAIN),
            HTML_DEFAULT_SERVLET_SUFFIX);
    }

    public void testJsonExtension() throws IOException {
        assertNotTestServlet(getContent(testNodeNORT.nodeUrl + ".json",
            CONTENT_TYPE_DONTCARE));
    }

    protected void changeComponent(final String component, final String action)
            throws IOException {
        List<NameValuePair> pars = new ArrayList<NameValuePair>();
        pars.add(new NameValuePair("action", action));
        assertPostStatus(HTTP_BASE_URL + "/system/console/components/"
            + component, HttpServletResponse.SC_OK, pars, null);
    }
}
