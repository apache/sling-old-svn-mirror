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
package org.apache.sling.launchpad.webapp.integrationtest.servlets.post;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.sling.commons.testing.integration.HttpTestBase;

/** Test the sling:sessionInfo resource */

public class SlingSessionInfoTest extends HttpTestBase {

    public void testSessionInfo() throws IOException {
        final String content = getContent(HTTP_BASE_URL + "/system/sling/info.sessionInfo.json", CONTENT_TYPE_JSON);

        // assume workspace name contains "default", might not
        // always be the case as the default workspace is selected
        // by the JCR implementation due to SLING-256
        assertJavascript("admin.string.string", content, "out.println(data.userID + '.' + typeof data.workspace + '.' + typeof data.authType)");
    }

    public void testNonexistentSlingUrl() throws IOException {
        assertHttpStatus(HTTP_BASE_URL + "/sling.nothing", HttpServletResponse.SC_NOT_FOUND);
    }
}
