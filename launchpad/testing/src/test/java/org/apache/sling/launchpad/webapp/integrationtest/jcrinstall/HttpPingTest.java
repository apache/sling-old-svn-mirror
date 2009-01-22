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
package org.apache.sling.launchpad.webapp.integrationtest.jcrinstall;

import org.apache.commons.httpclient.methods.GetMethod;

/** Ping the Sling server to verify that our integration test
 *  setup is ok.
 */
public class HttpPingTest extends JcrinstallTestBase {
    
    public void testWebServerRoot() throws Exception
    {
        final String url = HTTP_BASE_URL + "/";
        assertHttpStatus(url, 403, "Root should return 403 as no redirect is setup in the repository");
    }
    
    public void test404() throws Exception
    {
        assertHttpStatus(HTTP_BASE_URL + "/someNonExistentUrl", 404);
    }
    
}
