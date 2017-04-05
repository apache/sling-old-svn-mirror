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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.testing.integration.HttpTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class UserPrivilegesInfoTest {
	
	String testUserId = null;
	String testUserId2 = null;
	String testGroupId = null;
	String testFolderUrl = null;
    Set<String> toDelete = new HashSet<String>();
	
    private final UserManagerTestUtil H = new UserManagerTestUtil();
    
	@Before
	public void setup() throws Exception {
		H.setUp();

        // Script for server-side PrivilegeInfo calculations
        String scriptPath = "/apps/sling/servlet/default";
        H.getTestClient().mkdirs(HttpTest.WEBDAV_BASE_URL, scriptPath);
        toDelete.add(H.uploadTestScript(scriptPath,
        				"usermanager/privileges-info.json.esp",
        				"privileges-info.json.esp"));
	}

	@After
	public void cleanup() throws Exception {
		H.tearDown();

		Credentials creds = new UsernamePasswordCredentials("admin", "admin");

		if (testFolderUrl != null) {
			//remove the test user if it exists.
			String postUrl = testFolderUrl;
			List<NameValuePair> postParams = new ArrayList<NameValuePair>();
			postParams.add(new NameValuePair(":operation", "delete"));
			H.assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);
		}
		if (testGroupId != null) {
			//remove the test user if it exists.
			String postUrl = HttpTest.HTTP_BASE_URL + "/system/userManager/group/" + testGroupId + ".delete.html";
			List<NameValuePair> postParams = new ArrayList<NameValuePair>();
			H.assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);
		}
		if (testUserId != null) {
			//remove the test user if it exists.
			String postUrl = HttpTest.HTTP_BASE_URL + "/system/userManager/user/" + testUserId + ".delete.html";
			List<NameValuePair> postParams = new ArrayList<NameValuePair>();
			H.assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);
		}
		if (testUserId2 != null) {
			//remove the test user if it exists.
			String postUrl = HttpTest.HTTP_BASE_URL + "/system/userManager/user/" + testUserId2 + ".delete.html";
			List<NameValuePair> postParams = new ArrayList<NameValuePair>();
			H.assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);
		}
		
        for(String script : toDelete) {
            H.getTestClient().delete(script);
        }
	}
	
	
	/**
	 * Checks whether the current user has been granted privileges
	 * to add a new user.
	 */
	@Test 
	public void testCanAddUser() throws JSONException, IOException {
		testUserId = H.createTestUser();

		String getUrl = HttpTest.HTTP_BASE_URL + "/system/userManager/user/" + testUserId + ".privileges-info.json";

		//fetch the JSON for the test page to verify the settings.
		Credentials testUserCreds = new UsernamePasswordCredentials(testUserId, "testPwd");

		String json = H.getAuthenticatedContent(testUserCreds, getUrl, HttpTest.CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json);
		JSONObject jsonObj = new JSONObject(json);
		
		assertEquals(false, jsonObj.getBoolean("canAddUser"));
	}

	/**
	 * Checks whether the current user has been granted privileges
	 * to add a new group.
	 */
	@Test 
	public void testCanAddGroup() throws IOException, JSONException {
		testUserId = H.createTestUser();

		String getUrl = HttpTest.HTTP_BASE_URL + "/system/userManager/user/" + testUserId + ".privileges-info.json";

		//fetch the JSON for the test page to verify the settings.
		Credentials testUserCreds = new UsernamePasswordCredentials(testUserId, "testPwd");

		String json = H.getAuthenticatedContent(testUserCreds, getUrl, HttpTest.CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json);
		JSONObject jsonObj = new JSONObject(json);
		
		assertEquals(false, jsonObj.getBoolean("canAddGroup"));
	}
	
	/**
	 * Checks whether the current user has been granted privileges
	 * to update the properties of the specified user.
	 */
	@Test 
	public void testCanUpdateUserProperties() throws IOException, JSONException {
		testUserId = H.createTestUser();

		//1. verify user can update thier own properties
		String getUrl = HttpTest.HTTP_BASE_URL + "/system/userManager/user/" + testUserId + ".privileges-info.json";

		//fetch the JSON for the test page to verify the settings.
		Credentials testUserCreds = new UsernamePasswordCredentials(testUserId, "testPwd");

		String json = H.getAuthenticatedContent(testUserCreds, getUrl, HttpTest.CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json);
		JSONObject jsonObj = new JSONObject(json);
		
		//user can update their own properties
		assertEquals(true, jsonObj.getBoolean("canUpdateProperties"));
		
		
		//2. now try another user 
		testUserId2 = H.createTestUser();

		//fetch the JSON for the test page to verify the settings.
		Credentials testUser2Creds = new UsernamePasswordCredentials(testUserId2, "testPwd");

		String json2 = H.getAuthenticatedContent(testUser2Creds, getUrl, HttpTest.CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json2);
		JSONObject jsonObj2 = new JSONObject(json2);
		
		//user can not update other users properties
		assertEquals(false, jsonObj2.getBoolean("canUpdateProperties"));
	}

	/**
	 * Checks whether the current user has been granted privileges
	 * to update the properties of the specified group.
	 */
	@Test 
	public void testCanUpdateGroupProperties() throws IOException, JSONException {
		testGroupId = H.createTestGroup();
		testUserId = H.createTestUser();

		//1. Verify non admin user can not update group properties
		String getUrl = HttpTest.HTTP_BASE_URL + "/system/userManager/group/" + testGroupId + ".privileges-info.json";

		//fetch the JSON for the test page to verify the settings.
		Credentials testUserCreds = new UsernamePasswordCredentials(testUserId, "testPwd");

		String json = H.getAuthenticatedContent(testUserCreds, getUrl, HttpTest.CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json);
		JSONObject jsonObj = new JSONObject(json);
		
		//normal user can not update group properties
		assertEquals(false, jsonObj.getBoolean("canUpdateProperties"));
	}
	
	/**
	 * Checks whether the current user has been granted privileges
	 * to remove the specified user.
	 */
	@Test 
	public void testCanRemoveUser() throws IOException, JSONException {
		testUserId = H.createTestUser();

		//1. verify user can not remove themselves
		String getUrl = HttpTest.HTTP_BASE_URL + "/system/userManager/user/" + testUserId + ".privileges-info.json";

		//fetch the JSON for the test page to verify the settings.
		Credentials testUserCreds = new UsernamePasswordCredentials(testUserId, "testPwd");

		String json = H.getAuthenticatedContent(testUserCreds, getUrl, HttpTest.CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json);
		JSONObject jsonObj = new JSONObject(json);
		
		//user can not remove themselves
		assertEquals(false, jsonObj.getBoolean("canRemove"));
		
		
		//2. now try another user 
		testUserId2 = H.createTestUser();

		//fetch the JSON for the test page to verify the settings.
		Credentials testUser2Creds = new UsernamePasswordCredentials(testUserId2, "testPwd");

		String json2 = H.getAuthenticatedContent(testUser2Creds, getUrl, HttpTest.CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json2);
		JSONObject jsonObj2 = new JSONObject(json2);
		
		//user can not delete other users
		assertEquals(false, jsonObj2.getBoolean("canRemove"));
	}

	/**
	 * Checks whether the current user has been granted privileges
	 * to remove the specified group.
	 */
	@Test 
	public void testCanRemoveGroup() throws IOException, JSONException {
		testGroupId = H.createTestGroup();
		testUserId = H.createTestUser();

		//1. Verify non admin user can not remove group
		String getUrl = HttpTest.HTTP_BASE_URL + "/system/userManager/group/" + testGroupId + ".privileges-info.json";

		//fetch the JSON for the test page to verify the settings.
		Credentials testUserCreds = new UsernamePasswordCredentials(testUserId, "testPwd");

		String json = H.getAuthenticatedContent(testUserCreds, getUrl, HttpTest.CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json);
		JSONObject jsonObj = new JSONObject(json);
		
		//normal user can not remove group
		assertEquals(false, jsonObj.getBoolean("canRemove"));
	}
	
	/**
	 * Checks whether the current user has been granted privileges
	 * to update the membership of the specified group.
	 */
	@Test 
	public void testCanUpdateGroupMembers() throws IOException, JSONException {
		testGroupId = H.createTestGroup();
		testUserId = H.createTestUser();

		//1. Verify non admin user can not update group membership
		String getUrl = HttpTest.HTTP_BASE_URL + "/system/userManager/group/" + testGroupId + ".privileges-info.json";

		//fetch the JSON for the test page to verify the settings.
		Credentials testUserCreds = new UsernamePasswordCredentials(testUserId, "testPwd");

		String json = H.getAuthenticatedContent(testUserCreds, getUrl, HttpTest.CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json);
		JSONObject jsonObj = new JSONObject(json);
		
		//normal user can not remove group
		assertEquals(false, jsonObj.getBoolean("canUpdateGroupMembers"));
	}
}
