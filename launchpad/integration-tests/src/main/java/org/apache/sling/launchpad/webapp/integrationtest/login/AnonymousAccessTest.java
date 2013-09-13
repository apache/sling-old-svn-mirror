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
package org.apache.sling.launchpad.webapp.integrationtest.login;

import java.net.URL;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.sling.commons.testing.integration.HttpTestBase;
import org.apache.sling.commons.testing.integration.NameValuePairList;
import org.apache.sling.servlets.post.SlingPostConstants;

/** Verify that anonymous has read access via HTTP */
public class AnonymousAccessTest extends HttpTestBase {

    private String displayUrl;
    private String testText;
    
    @Override
    public void setUp() throws Exception {
        super.setUp();
        
        // create test node under a unique path
        final String url = HTTP_BASE_URL + "/" + getClass().getSimpleName() + "/" + System.currentTimeMillis() + SlingPostConstants.DEFAULT_CREATE_SUFFIX;
        testText = "Test text " + System.currentTimeMillis();
        final NameValuePairList list = new NameValuePairList();
        list.add("text", testText);
        displayUrl = testClient.createNode(url, list, null, true);
    }
    
    private void assertContent() throws Exception {
        final String content = getContent(displayUrl + ".txt", CONTENT_TYPE_PLAIN);
        assertTrue(content.contains(testText));
    }
    
    public void testAnonymousContent() throws Exception {
        // disable credentials -> anonymous session
        final URL url = new URL(HTTP_BASE_URL);
        final AuthScope scope = new AuthScope(url.getHost(), url.getPort(), AuthScope.ANY_REALM);
        httpClient.getParams().setAuthenticationPreemptive(false);
        httpClient.getState().setCredentials(scope, null);
        
        try {
            assertContent();
        } finally {
            // re-enable credentials -> admin session
            httpClient.getParams().setAuthenticationPreemptive(true);
            Credentials defaultcreds = new UsernamePasswordCredentials("admin", "admin");
            httpClient.getState().setCredentials(scope, defaultcreds);
        }
    }
    
    public void testAdminContent() throws Exception {
        // HTTP test client has credentials by default
        assertContent();
    }
}
