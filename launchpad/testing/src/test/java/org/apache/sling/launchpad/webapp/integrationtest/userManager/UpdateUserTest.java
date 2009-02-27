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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;

/**
 * Tests for the 'updateAuthorizable' and 'changePassword' Sling Post 
 * Operations on a user resource.
 */
public class UpdateUserTest extends AbstractUserManagerTest {

	String testUserId = null;
	
	@Override
	protected void tearDown() throws Exception {
		if (testUserId != null) {
			//remove the test user if it exists.
			String postUrl = HTTP_BASE_URL + "/system/userManager/user/" + testUserId;
			List<NameValuePair> postParams = new ArrayList<NameValuePair>();
			postParams.add(new NameValuePair(":operation", "updateAuthorizable"));
			assertAuthenticatedAdminPostStatus(postUrl, HttpServletResponse.SC_OK, postParams, null);
		}

		super.tearDown();
	}

	public void testUpdateUser() throws IOException, JSONException {
		testUserId = createTestUser();
		
        String postUrl = HTTP_BASE_URL + "/system/userManager/user/" + testUserId;

		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair(":operation", "updateAuthorizable"));
		postParams.add(new NameValuePair("displayName", "My Updated Test User"));
		postParams.add(new NameValuePair("url", "http://www.apache.org/updated"));
		Credentials creds = new UsernamePasswordCredentials(testUserId, "testPwd");
		assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);
		
		//fetch the user profile json to verify the settings
		String getUrl = HTTP_BASE_URL + "/system/userManager/user/" + testUserId + ".json";
		assertAuthenticatedHttpStatus(creds, getUrl, HttpServletResponse.SC_OK, null); //make sure the profile request returns some data
		String json = getAuthenticatedContent(creds, getUrl, CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json);
		JSONObject jsonObj = new JSONObject(json);
		assertEquals("My Updated Test User", jsonObj.getString("displayName"));
		assertEquals("http://www.apache.org/updated", jsonObj.getString("url"));
		assertFalse(jsonObj.has(":operation"));
	}
	
	public void testChangeUserPassword() throws IOException {
		testUserId = createTestUser();
		
        String postUrl = HTTP_BASE_URL + "/system/userManager/user/" + testUserId;

		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair(":operation", "changePassword"));
		postParams.add(new NameValuePair("oldPwd", "testPwd"));
		postParams.add(new NameValuePair("newPwd", "testNewPwd"));
		postParams.add(new NameValuePair("newPwdConfirm", "testNewPwd"));

		Credentials creds = new UsernamePasswordCredentials(testUserId, "testPwd");
		assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);
	}
	
	public void testChangeUserPasswordWrongOldPwd() throws IOException {
		testUserId = createTestUser();
		
        String postUrl = HTTP_BASE_URL + "/system/userManager/user/" + testUserId;

		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair(":operation", "changePassword"));
		postParams.add(new NameValuePair("oldPwd", "wrongTestPwd"));
		postParams.add(new NameValuePair("newPwd", "testNewPwd"));
		postParams.add(new NameValuePair("newPwdConfirm", "testNewPwd"));
		
		//Credentials creds = new UsernamePasswordCredentials(testUserId, "testPwd");
		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, postParams, null);
	}

	public void testChangeUserPasswordWrongConfirmPwd() throws IOException {
		testUserId = createTestUser();
		
        String postUrl = HTTP_BASE_URL + "/system/userManager/user/" + testUserId;

		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair(":operation", "changePassword"));
		postParams.add(new NameValuePair("oldPwd", "testPwd"));
		postParams.add(new NameValuePair("newPwd", "testNewPwd"));
		postParams.add(new NameValuePair("newPwdConfirm", "wrongTestNewPwd"));
		
		//Credentials creds = new UsernamePasswordCredentials(testUserId, "testPwd");
		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, postParams, null);
	}
	
}
