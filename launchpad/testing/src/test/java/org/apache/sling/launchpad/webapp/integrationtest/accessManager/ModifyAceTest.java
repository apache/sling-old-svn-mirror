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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;

/**
 * Tests for the 'modifyAce' Sling Post Operation
 */
public class ModifyAceTest extends AbstractAccessManagerTest {

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
	}

	public void testModifyAceForUser() throws IOException, JSONException {
		testUserId = createTestUser();
		
		testFolderUrl = createTestFolder();
		
        String postUrl = testFolderUrl + ".modifyAce.html";

		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair("principalId", testUserId));
		postParams.add(new NameValuePair("privilege@jcr:read", "granted"));
		postParams.add(new NameValuePair("privilege@jcr:write", "denied"));
		
		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);
		
		
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
	}

	public void testModifyAceForGroup() throws IOException, JSONException {
		testGroupId = createTestGroup();

		testFolderUrl = createTestFolder();

        String postUrl = testFolderUrl + ".modifyAce.html";

		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair("principalId", testGroupId));
		postParams.add(new NameValuePair("privilege@jcr:read", "granted"));
		postParams.add(new NameValuePair("privilege@jcr:write", "denied"));
		
		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);
		
		
		//fetch the JSON for the acl to verify the settings.
		String getUrl = testFolderUrl + ".acl.json";

		String json = getAuthenticatedContent(creds, getUrl, CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json);
		JSONObject jsonObj = new JSONObject(json);
		String aceString = jsonObj.getString(testGroupId);
		assertNotNull(aceString);

		JSONObject aceObject = new JSONObject(aceString);
		assertNotNull(aceObject);
		
		JSONArray grantedArray = aceObject.getJSONArray("granted");
		assertNotNull(grantedArray);
		assertEquals("jcr:read", grantedArray.getString(0));

		//denied rights are not applied for groups, so make sure it is not there
		assertTrue(aceObject.isNull("denied"));
	}
	
	/**
	 * Test for SLING-997, preserve privileges that were not posted with the modifyAce 
	 * request.
	 */
	public void testMergeAceForUser() throws IOException, JSONException {
		testUserId = createTestUser();
		testFolderUrl = createTestFolder();
		
        String postUrl = testFolderUrl + ".modifyAce.html";

        //1. create an initial set of privileges
		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair("principalId", testUserId));
		postParams.add(new NameValuePair("privilege@jcr:read", "granted"));
		postParams.add(new NameValuePair("privilege@jcr:readAccessControl", "granted"));
		postParams.add(new NameValuePair("privilege@jcr:addChildNodes", "granted"));
		postParams.add(new NameValuePair("privilege@jcr:modifyAccessControl", "denied"));
		postParams.add(new NameValuePair("privilege@jcr:removeChildNodes", "denied"));
		
		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);
		
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
		assertEquals(3, grantedArray.length());
		Set<String> grantedPrivilegeNames = new HashSet<String>();
		for (int i=0; i < grantedArray.length(); i++) {
			grantedPrivilegeNames.add(grantedArray.getString(i));
		}
		assertTrue(grantedPrivilegeNames.contains("jcr:read"));
		assertTrue(grantedPrivilegeNames.contains("jcr:readAccessControl"));
		assertTrue(grantedPrivilegeNames.contains("jcr:addChildNodes"));

		JSONArray deniedArray = aceObject.getJSONArray("denied");
		assertNotNull(deniedArray);
		assertEquals(2, deniedArray.length());
		Set<String> deniedPrivilegeNames = new HashSet<String>();
		for (int i=0; i < deniedArray.length(); i++) {
			deniedPrivilegeNames.add(deniedArray.getString(i));
		}
		assertTrue(deniedPrivilegeNames.contains("jcr:modifyAccessControl"));
		assertTrue(deniedPrivilegeNames.contains("jcr:removeChildNodes"));
		
		
		
        //2. post a new set of privileges to merge with the existing privileges
		List<NameValuePair> postParams2 = new ArrayList<NameValuePair>();
		postParams2.add(new NameValuePair("principalId", testUserId));
		//jcr:read and jcr:addChildNodes are not posted, so they should remain in the granted ACE
		postParams2.add(new NameValuePair("privilege@jcr:readAccessControl", "none")); //clear the existing privilege
		postParams2.add(new NameValuePair("privilege@jcr:modifyProperties", "granted")); //add a new privilege
		//jcr:modifyAccessControl is not posted, so it should remain in the denied ACE
		postParams2.add(new NameValuePair("privilege@jcr:modifyAccessControl", "denied")); //deny the modifyAccessControl privilege
		postParams2.add(new NameValuePair("privilege@jcr:removeChildNodes", "none")); //clear the existing privilege
		postParams2.add(new NameValuePair("privilege@jcr:removeNode", "denied")); //deny a new privilege
		
		assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams2, null);
		
		
		//fetch the JSON for the acl to verify the settings.
		String json2 = getAuthenticatedContent(creds, getUrl, CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		
		assertNotNull(json2);
		JSONObject jsonObj2 = new JSONObject(json2);
		String aceString2 = jsonObj2.getString(testUserId);
		assertNotNull(aceString2);
		
		JSONObject aceObject2 = new JSONObject(aceString2); 
		assertNotNull(aceObject2);
		
		JSONArray grantedArray2 = aceObject2.getJSONArray("granted");
		assertNotNull(grantedArray2);
		assertEquals(3, grantedArray2.length());
		Set<String> grantedPrivilegeNames2 = new HashSet<String>();
		for (int i=0; i < grantedArray2.length(); i++) {
			grantedPrivilegeNames2.add(grantedArray2.getString(i));
		}
		assertTrue(grantedPrivilegeNames2.contains("jcr:read"));
		assertTrue(grantedPrivilegeNames2.contains("jcr:addChildNodes"));
		assertTrue(grantedPrivilegeNames2.contains("jcr:modifyProperties"));

		JSONArray deniedArray2 = aceObject2.getJSONArray("denied");
		assertNotNull(deniedArray2);
		assertEquals(2, deniedArray2.length());
		Set<String> deniedPrivilegeNames2 = new HashSet<String>();
		for (int i=0; i < deniedArray2.length(); i++) {
			deniedPrivilegeNames2.add(deniedArray2.getString(i));
		}
		assertTrue(deniedPrivilegeNames2.contains("jcr:modifyAccessControl"));
		assertTrue(deniedPrivilegeNames2.contains("jcr:removeNode"));
	}
	
}
