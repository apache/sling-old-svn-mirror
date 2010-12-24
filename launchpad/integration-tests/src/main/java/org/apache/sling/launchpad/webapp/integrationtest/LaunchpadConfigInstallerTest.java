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

import org.apache.sling.commons.testing.integration.HttpTestBase;

/** Verify that the test config is installed */
public class LaunchpadConfigInstallerTest extends HttpTestBase {
    public void testConfigPresent() throws Exception {
        final String url = HTTP_BASE_URL + "/testing/GetConfigServlet.tidy.json/integrationTestsConfig";
        final String json = getContent(url, CONTENT_TYPE_JSON);
        final String expectMessage = "This test config should be loaded at startup";
        final String code = "out.print(data.properties.message)"; 
        assertJavascript(expectMessage, json, code);
    }
}
