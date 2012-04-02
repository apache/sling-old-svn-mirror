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

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.sling.commons.testing.integration.HttpAnyMethod;
import org.apache.sling.commons.testing.integration.HttpTestBase;

/** Test WebDAV upload of various file types */
public class WebdavOptionsTest extends HttpTestBase {
    
    public static final String WWW_Authenticate = "WWW-Authenticate";

    /** OPTIONS request at given path must return a WWW-Authenticate header */
    private void doTestOnPath(String path) throws Exception {
        final HttpClient noCredentialsClient = new HttpClient();
        final HttpAnyMethod opt = new HttpAnyMethod("OPTIONS", HTTP_BASE_URL + path);
        final int status = noCredentialsClient.executeMethod(opt);
        assertEquals("Expecting matching status on OPTIONS request at " + path, 401, status);
        final Header h = opt.getResponseHeader(WWW_Authenticate);
        assertNotNull("Expecting " + WWW_Authenticate + " header in response at " + path, h);
    }
    
    public void testAuthenticateHeaderOnRoot() throws Exception {
        doTestOnPath("/");
    }
    
    public void testAuthenticateHeaderOnDavPath() throws Exception {
        doTestOnPath("/dav/default");
    }
}
