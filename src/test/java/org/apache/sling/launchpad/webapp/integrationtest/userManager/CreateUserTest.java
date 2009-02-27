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
 * Tests for the 'createUser' Sling Post Operation
 */
public class CreateUserTest extends AbstractUserManagerTest {

	String testUserId = null;
	
	@Override
	protected void tearDown() throws Exception {
		if (testUserId != null) {
			//remove the test user if it exists.
			String postUrl = HTTP_BASE_URL + "/system/userManager/user/" + testUserId;
			List<NameValuePair> postParams = new ArrayList<NameValuePair>();
			postParams.add(new NameValuePair(":operation", "deleteAuthorizable"));
			assertAuthenticatedAdminPostStatus(postUrl, HttpServletResponse.SC_OK, postParams, null);
		}
		super.tearDown();
	}

	/*
		<form action="/system/userManager/createUser.html" method="POST">
			<input type="hidden" name=":operation" value="creteUser" />
		   <div>Name: <input type="text" name=":name" value="testUser" /></div>
		   <div>Password: <input type="text" name="pwd" value="testUser" /></div>
		   <div>Password Confirm: <input type="text" name="pwdConfirm" value="testUser" /></div>
		   <input type="submit" value="Submit" />
		</form>
	 */
	public void testCreateUser() throws IOException, JSONException {
        String postUrl = HTTP_BASE_URL + "/system/userManager/user/";
        
		testUserId = "testUser" + (counter++);
		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair(":operation", "createUser"));
		postParams.add(new NameValuePair(":name", testUserId));
		postParams.add(new NameValuePair("pwd", "testPwd"));
		postParams.add(new NameValuePair("pwdConfirm", "testPwd"));
		assertPostStatus(postUrl, HttpServletResponse.SC_OK, postParams, null);
		
		//fetch the user profile json to verify the settings
		String getUrl = HTTP_BASE_URL + "/system/userManager/user/" + testUserId + ".json";
		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		String json = getAuthenticatedContent(creds, getUrl, CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json);
		JSONObject jsonObj = new JSONObject(json);
		assertEquals(testUserId, jsonObj.getString("rep:principalName"));
		assertFalse(jsonObj.has(":name"));
		assertFalse(jsonObj.has("pwd"));
		assertFalse(jsonObj.has("pwdConfirm"));
		assertFalse(jsonObj.has(":operation"));
	}

	public void testCreateUserMissingUserId() throws IOException {
        String postUrl = HTTP_BASE_URL + "/system/userManager/user/";

		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair(":operation", "createUser"));
		assertPostStatus(postUrl, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, postParams, null);
	}

	public void testCreateUserMissingPwd() throws IOException {
        String postUrl = HTTP_BASE_URL + "/system/userManager/user/";

        String userId = "testUser" + (counter++);
		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair(":operation", "createUser"));
		postParams.add(new NameValuePair(":name", userId));
		assertPostStatus(postUrl, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, postParams, null);
	}

	public void testCreateUserWrongConfirmPwd() throws IOException {
        String postUrl = HTTP_BASE_URL + "/system/userManager/user/";

        String userId = "testUser" + (counter++);
		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair(":operation", "createUser"));
		postParams.add(new NameValuePair(":name", userId));
		postParams.add(new NameValuePair("pwd", "testPwd"));
		postParams.add(new NameValuePair("pwdConfirm", "testPwd2"));
		assertPostStatus(postUrl, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, postParams, null);
	}

	public void testCreateUserUserAlreadyExists() throws IOException {
        String postUrl = HTTP_BASE_URL + "/system/userManager/user/";

		testUserId = "testUser" + (counter++);
		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair(":operation", "createUser"));
		postParams.add(new NameValuePair(":name", testUserId));
		postParams.add(new NameValuePair("pwd", "testPwd"));
		postParams.add(new NameValuePair("pwdConfirm", "testPwd"));
		assertPostStatus(postUrl, HttpServletResponse.SC_OK, postParams, null);
		
		//post the same info again, should fail
		assertPostStatus(postUrl, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, postParams, null);
	}
	
	/*
	<form action="/system/userManager/createUser.html" method="POST">
	    <input type="hidden" name=":operation" value="creteUser" />
	   <div>Name: <input type="text" name=":name" value="testUser" /></div>
	   <div>Password: <input type="text" name="pwd" value="testUser" /></div>
	   <div>Password Confirm: <input type="text" name="pwdConfirm" value="testUser" /></div>
	   <div>Extra Property #1: <input type="text" name="displayName" value="My Test User" /></div>
	   <div>Extra Property #2: <input type="text" name="url" value="http://www.apache.org" /></div>
	   <input type="submit" value="Submit" />
	</form>
	*/
	public void testCreateUserWithExtraProperties() throws IOException, JSONException {
        String postUrl = HTTP_BASE_URL + "/system/userManager/user/";

		testUserId = "testUser" + (counter++);
		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair(":operation", "createUser"));
		postParams.add(new NameValuePair(":name", testUserId));
		postParams.add(new NameValuePair("pwd", "testPwd"));
		postParams.add(new NameValuePair("pwdConfirm", "testPwd"));
		postParams.add(new NameValuePair("displayName", "My Test User"));
		postParams.add(new NameValuePair("url", "http://www.apache.org"));
		assertPostStatus(postUrl, HttpServletResponse.SC_OK, postParams, null);

		//fetch the user profile json to verify the settings
		String getUrl = HTTP_BASE_URL + "/system/userManager/user/" + testUserId + ".json";
		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		String json = getAuthenticatedContent(creds, getUrl, CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json);
		JSONObject jsonObj = new JSONObject(json);
		assertEquals(testUserId, jsonObj.getString("rep:principalName"));
		assertEquals("My Test User", jsonObj.getString("displayName"));
		assertEquals("http://www.apache.org", jsonObj.getString("url"));
		assertFalse(jsonObj.has(":name"));
		assertFalse(jsonObj.has("pwd"));
		assertFalse(jsonObj.has("pwdConfirm"));
		assertFalse(jsonObj.has(":operation"));
	}		
}
