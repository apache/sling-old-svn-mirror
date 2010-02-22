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

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.sling.commons.testing.integration.HttpTestBase;

/** Verify that the sling:authRequestLogin parameter forces login */
public class AuthRequestLoginTest extends HttpTestBase {
    private final static String SESSION_INFO_PATH = "/system/sling/info.sessionInfo.json";
    
    public void testForcedLogin() throws Exception {
    	// disable credentials -> anonymous session
        final URL url = new URL(HTTP_BASE_URL);
    	final AuthScope scope = new AuthScope(url.getHost(), url.getPort(), AuthScope.ANY_REALM);
    	httpClient.getParams().setAuthenticationPreemptive(false);
        httpClient.getState().setCredentials(scope, null);
    	{
            final String content = getContent(HTTP_BASE_URL + SESSION_INFO_PATH, CONTENT_TYPE_JSON);
            assertJavascript("anonymous", content, "out.println(data.userID)");
    	}
    	
    	// root must return 20x or 30x
        final GetMethod get = new GetMethod(HTTP_BASE_URL + "/");
        final int status = httpClient.executeMethod(get);
        final int status10 = status / 10;
        if(status10 != 20 && status10 != 30) {
        	fail("Expected 20x or 30x status, got " + status);
        }
        
        // root with sling:authRequestLogin=true must return 401
    	assertHttpStatus(HTTP_BASE_URL + "/?sling:authRequestLogin=true", HttpServletResponse.SC_UNAUTHORIZED);
    	
    	// re-enable credentials -> admin session
        httpClient.getParams().setAuthenticationPreemptive(true);
        Credentials defaultcreds = new UsernamePasswordCredentials("admin", "admin");
        httpClient.getState().setCredentials(scope, defaultcreds);
    	{
            final String content = getContent(HTTP_BASE_URL + SESSION_INFO_PATH, CONTENT_TYPE_JSON);
            assertJavascript("admin", content, "out.println(data.userID)");
    	}
    }

}
