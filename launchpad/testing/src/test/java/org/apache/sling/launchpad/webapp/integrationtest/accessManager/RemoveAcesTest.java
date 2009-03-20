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
package org.apache.sling.launchpad.webapp.integrationtest.accessManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;

/**
 * Tests for the 'removeAce' Sling POST operation
 */
public class RemoveAcesTest extends AbstractAccessManagerTest {
	String testUserId = null;
	String testGroupId = null;
	String testFolderUrl = null;
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();

		Credentials creds = new UsernamePasswordCredentials("admin", "admin");

		if (testFolderUrl != null) {
			//remove the test user if it exists.
			String postUrl = testFolderUrl;
			List<NameValuePair> postParams = new ArrayList<NameValuePair>();
			postParams.add(new NameValuePair(":operation", "delete"));
			assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);
		}
		if (testGroupId != null) {
			//remove the test user if it exists.
			String postUrl = HTTP_BASE_URL + "/system/userManager/group/" + testGroupId + ".delete.html";
			List<NameValuePair> postParams = new ArrayList<NameValuePair>();
			assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);
		}
		if (testUserId != null) {
			//remove the test user if it exists.
			String postUrl = HTTP_BASE_URL + "/system/userManager/user/" + testUserId + ".delete.html";
			List<NameValuePair> postParams = new ArrayList<NameValuePair>();
			assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);
		}
		//todo delete test folder
	}
	
	private String createFolderWithAces(boolean addGroupAce) throws IOException, JSONException {
		testUserId = createTestUser();
		testFolderUrl = createTestFolder();

        String postUrl = testFolderUrl + ".modifyAce.html";

		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair("principalId", testUserId));
		postParams.add(new NameValuePair("privilege@jcr:read", "granted"));
		postParams.add(new NameValuePair("privilege@jcr:write", "denied"));
		
		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);

		if (addGroupAce) {
			testGroupId = createTestGroup();
			
			postParams = new ArrayList<NameValuePair>();
			postParams.add(new NameValuePair("principalId", testGroupId));
			postParams.add(new NameValuePair("privilege@jcr:read", "granted"));
			
			assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);
		}
		
		//fetch the JSON for the acl to verify the settings.
		String getUrl = testFolderUrl + ".acl.json";

		String json = getAuthenticatedContent(creds, getUrl, CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json);
		
		JSONObject jsonObj = new JSONObject(json);
		String aceString = jsonObj.getString(testUserId);
		assertNotNull(aceString);

		JSONObject aceObject = new JSONObject(aceString);
		assertNotNull(aceObject);
		
		JSONArray grantedArray = aceObject.getJSONArray("granted");
		assertNotNull(grantedArray);
		assertEquals("jcr:read", grantedArray.getString(0));

		JSONArray deniedArray = aceObject.getJSONArray("denied");
		assertNotNull(deniedArray);
		assertEquals("jcr:write", deniedArray.getString(0));

		if (addGroupAce) {
			aceString = jsonObj.getString(testGroupId);
			assertNotNull(aceString);

			aceObject = new JSONObject(aceString);
			assertNotNull(aceObject);

			grantedArray = aceObject.getJSONArray("granted");
			assertNotNull(grantedArray);
			assertEquals("jcr:read", grantedArray.getString(0));
		}
		
		return testFolderUrl;
	}
	
	//test removing a single ace
	public void testRemoveAce() throws IOException, JSONException {
		String folderUrl = createFolderWithAces(false);
		
		//remove the ace for the testUser principal
		String postUrl = folderUrl + ".deleteAce.html"; 
		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair(":applyTo", testUserId));
        Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);

		//fetch the JSON for the acl to verify the settings.
		String getUrl = folderUrl + ".acl.json";

		String json = getAuthenticatedContent(creds, getUrl, CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json);

		JSONObject jsonObj = new JSONObject(json);
		assertTrue(jsonObj.isNull(testUserId));
	}

	//test removing multiple aces
	public void testRemoveAces() throws IOException, JSONException {
		String folderUrl = createFolderWithAces(true);
		
		//remove the ace for the testUser principal
		String postUrl = folderUrl + ".deleteAce.html"; 
		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair(":applyTo", testUserId));
		postParams.add(new NameValuePair(":applyTo", testGroupId));
        Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);

		//fetch the JSON for the acl to verify the settings.
		String getUrl = folderUrl + ".acl.json";

		String json = getAuthenticatedContent(creds, getUrl, CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json);

		JSONObject jsonObj = new JSONObject(json);
		assertTrue(jsonObj.isNull(testUserId));
		assertTrue(jsonObj.isNull(testGroupId));
	}
}
