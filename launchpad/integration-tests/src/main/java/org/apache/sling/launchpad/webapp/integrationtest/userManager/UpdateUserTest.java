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
package org.apache.sling.launchpad.webapp.integrationtest.userManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.testing.integration.HttpTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for the 'updateAuthorizable' and 'changePassword' Sling Post 
 * Operations on a user resource.
 */
public class UpdateUserTest {

	String testUserId = null;
	private final UserManagerTestUtil H = new UserManagerTestUtil();
	
    @Before
    public void setup() throws Exception {
        H.setUp();
    }
    
	@After
	public void cleanup() throws Exception {
		if (testUserId != null) {
			//remove the test user if it exists.
			String postUrl = HttpTest.HTTP_BASE_URL + "/system/userManager/user/" + testUserId + ".delete.html";
			List<NameValuePair> postParams = new ArrayList<NameValuePair>();
			H.assertAuthenticatedAdminPostStatus(postUrl, HttpServletResponse.SC_OK, postParams, null);
		}

		H.tearDown();
	}

	@Test 
	public void testUpdateUser() throws IOException, JSONException {
		testUserId = H.createTestUser();
		
        String postUrl = HttpTest.HTTP_BASE_URL + "/system/userManager/user/" + testUserId + ".update.html";

		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair("displayName", "My Updated Test User"));
		postParams.add(new NameValuePair("url", "http://www.apache.org/updated"));
		Credentials creds = new UsernamePasswordCredentials(testUserId, "testPwd");
		H.assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);
		
		//fetch the user profile json to verify the settings
		String getUrl = HttpTest.HTTP_BASE_URL + "/system/userManager/user/" + testUserId + ".json";
		H.assertAuthenticatedHttpStatus(creds, getUrl, HttpServletResponse.SC_OK, null); //make sure the profile request returns some data
		String json = H.getAuthenticatedContent(creds, getUrl, HttpTest.CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json);
		JSONObject jsonObj = new JSONObject(json);
		assertEquals("My Updated Test User", jsonObj.getString("displayName"));
		assertEquals("http://www.apache.org/updated", jsonObj.getString("url"));
	}
	
	@Test 
	public void testChangeUserPassword() throws IOException {
		testUserId = H.createTestUser();
		
        String postUrl = HttpTest.HTTP_BASE_URL + "/system/userManager/user/" + testUserId + ".changePassword.html";

		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair("oldPwd", "testPwd"));
		postParams.add(new NameValuePair("newPwd", "testNewPwd"));
		postParams.add(new NameValuePair("newPwdConfirm", "testNewPwd"));

		Credentials creds = new UsernamePasswordCredentials(testUserId, "testPwd");
		H.assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);
	}
	
	@Test 
	public void testChangeUserPasswordWrongOldPwd() throws IOException {
		testUserId = H.createTestUser();
		
        String postUrl = HttpTest.HTTP_BASE_URL + "/system/userManager/user/" + testUserId + ".changePassword.html";

		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair("oldPwd", "wrongTestPwd"));
		postParams.add(new NameValuePair("newPwd", "testNewPwd"));
		postParams.add(new NameValuePair("newPwdConfirm", "testNewPwd"));
		
		//Credentials creds = new UsernamePasswordCredentials(testUserId, "testPwd");
		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		H.assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, postParams, null);
	}

	@Test 
	public void testChangeUserPasswordWrongConfirmPwd() throws IOException {
		testUserId = H.createTestUser();
		
        String postUrl = HttpTest.HTTP_BASE_URL + "/system/userManager/user/" + testUserId + ".changePassword.html";

		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair("oldPwd", "testPwd"));
		postParams.add(new NameValuePair("newPwd", "testNewPwd"));
		postParams.add(new NameValuePair("newPwdConfirm", "wrongTestNewPwd"));
		
		//Credentials creds = new UsernamePasswordCredentials(testUserId, "testPwd");
		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		H.assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, postParams, null);
	}

	/**
	 * Test for SLING-1677
	 */
	@Test 
	public void testUpdateUserResponseAsJSON() throws IOException, JSONException {
		testUserId = H.createTestUser();
		
        String postUrl = HttpTest.HTTP_BASE_URL + "/system/userManager/user/" + testUserId + ".update.json";

		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair("displayName", "My Updated Test User"));
		postParams.add(new NameValuePair("url", "http://www.apache.org/updated"));
		Credentials creds = new UsernamePasswordCredentials(testUserId, "testPwd");
		String json = H.getAuthenticatedPostContent(creds, postUrl, HttpTest.CONTENT_TYPE_JSON, postParams, HttpServletResponse.SC_OK);

		//make sure the json response can be parsed as a JSON object
		JSONObject jsonObj = new JSONObject(json);
		assertNotNull(jsonObj);
	}	
	

	/**
	 * Test for SLING-2069
	 * @throws IOException
	 */
	@Test 
	public void testChangeUserPasswordAsAdministratorWithoutOldPwd() throws IOException {
		testUserId = H.createTestUser();
		
        String postUrl = HttpTest.HTTP_BASE_URL + "/system/userManager/user/" + testUserId + ".changePassword.html";

		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair("newPwd", "testNewPwd"));
		postParams.add(new NameValuePair("newPwdConfirm", "testNewPwd"));
		
		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		H.assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);
	}

	/**
	 * Test for SLING-2072
	 * @throws IOException
	 */
	@Test 
	public void testDisableUser() throws IOException {
		testUserId = H.createTestUser();

		//login before the user is disabled, so login should work
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new NameValuePair("j_username", testUserId));
        params.add(new NameValuePair("j_password", "testPwd"));
        params.add(new NameValuePair("j_validate", "true"));
        HttpMethod post = H.assertPostStatus(HttpTest.HTTP_BASE_URL + "/j_security_check", HttpServletResponse.SC_OK, params, null);
        assertNull(post.getResponseHeader("X-Reason"));
		H.getHttpClient().getState().clearCredentials();
		H.getHttpClient().getState().clearCookies();

        //update the user to disable it
        String postUrl = HttpTest.HTTP_BASE_URL + "/system/userManager/user/" + testUserId + ".update.html";
		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair(":disabled", "true"));
		postParams.add(new NameValuePair(":disabledReason", "Just Testing"));
		H.assertAuthenticatedAdminPostStatus(postUrl, HttpServletResponse.SC_OK, postParams, null);
		
		//the user is now disabled, so login should fail
        post = H.assertPostStatus(HttpTest.HTTP_BASE_URL + "/j_security_check", HttpServletResponse.SC_FORBIDDEN, params, null);
        assertNotNull(post.getResponseHeader("X-Reason"));
		H.getHttpClient().getState().clearCredentials();
		H.getHttpClient().getState().clearCookies();

		//enable the user again
		postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair(":disabled", "false"));
		H.assertAuthenticatedAdminPostStatus(postUrl, HttpServletResponse.SC_OK, postParams, null);

		//login after the user is enabled, so login should work
        post = H.assertPostStatus(HttpTest.HTTP_BASE_URL + "/j_security_check", HttpServletResponse.SC_OK, params, null);
        assertNull(post.getResponseHeader("X-Reason"));
		H.getHttpClient().getState().clearCredentials();
		H.getHttpClient().getState().clearCookies();
	}
}
