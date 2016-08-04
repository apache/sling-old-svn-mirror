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

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.sling.commons.testing.integration.HttpTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Verify that redirect to resource after logout works */
public class RedirectOnLogoutTest {
    
    private final HttpTest H = new HttpTest();
    
    @Before
    public void setup() throws Exception {
        H.setUp();
    }
    
    @After
    public void cleanup() throws Exception {
        H.tearDown();
    }
    
    /**
     * Test SLING-1847
     * @throws Exception
     */
    @Test 
    public void testRedirectToResourceAfterLogout() throws Exception {
    	//login
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new NameValuePair("j_username", "admin"));
        params.add(new NameValuePair("j_password", "admin"));
        H.assertPostStatus(HttpTest.HTTP_BASE_URL + "/j_security_check", HttpServletResponse.SC_MOVED_TEMPORARILY, params, null);
        
        //...and then...logout with a resource redirect
        String locationAfterLogout = HttpTest.SERVLET_CONTEXT + "/system/sling/info.sessionInfo.json";
        final GetMethod get = new GetMethod(HttpTest.HTTP_BASE_URL + "/system/sling/logout");
        NameValuePair [] logoutParams = new NameValuePair[1];
        logoutParams[0] = new NameValuePair("resource", locationAfterLogout);
        get.setQueryString(logoutParams);
        
        get.setFollowRedirects(false);
        final int status = H.getHttpClient().executeMethod(get);
        assertEquals("Expected redirect", HttpServletResponse.SC_MOVED_TEMPORARILY, status);
        Header location = get.getResponseHeader("Location");
        assertEquals(HttpTest.HTTP_BASE_URL + locationAfterLogout, location.getValue());
    }
}