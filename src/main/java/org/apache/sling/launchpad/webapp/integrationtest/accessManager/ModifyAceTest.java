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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertFalse;

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
import org.apache.sling.commons.testing.integration.HttpTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Tests for the 'modifyAce' Sling Post Operation
 */
public class ModifyAceTest {

	String testUserId = null;
	String testUserId2 = null;
	String testGroupId = null;
	String testFolderUrl = null;
	
	private final AccessManagerTestUtil H = new AccessManagerTestUtil();  
	
	@Before
    public void setup() throws Exception {
	    H.setUp();
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
	}

	@Test 
	public void testModifyAceForUser() throws IOException, JSONException {
		testUserId = H.createTestUser();
		
		testFolderUrl = H.createTestFolder();
		
        String postUrl = testFolderUrl + ".modifyAce.html";

		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair("principalId", testUserId));
		postParams.add(new NameValuePair("privilege@jcr:read", "granted"));
		postParams.add(new NameValuePair("privilege@jcr:write", "denied"));
		postParams.add(new NameValuePair("privilege@jcr:modifyAccessControl", "bogus")); //invalid value should be ignored.
		
		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		H.assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);
		
		
		//fetch the JSON for the acl to verify the settings.
		String getUrl = testFolderUrl + ".acl.json";

		String json = H.getAuthenticatedContent(creds, getUrl, HttpTest.CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json);
		JSONObject jsonObject = new JSONObject(json);
		assertEquals(1, jsonObject.length());
		
		JSONObject aceObject = jsonObject.optJSONObject(testUserId);
		assertNotNull(aceObject);
		
		String principalString = aceObject.optString("principal");
		assertEquals(testUserId, principalString);

	        int order = aceObject.optInt("order");
	        assertEquals(0, order);

		JSONArray grantedArray = aceObject.optJSONArray("granted");
		assertNotNull(grantedArray);
		assertEquals(1, grantedArray.length());
		assertEquals("jcr:read", grantedArray.getString(0));

		JSONArray deniedArray = aceObject.optJSONArray("denied");
		assertNotNull(deniedArray);
		assertEquals(1, deniedArray.length());
		assertEquals("jcr:write", deniedArray.getString(0));
	}

	@Test 
	public void testModifyAceForGroup() throws IOException, JSONException {
		testGroupId = H.createTestGroup();

		testFolderUrl = H.createTestFolder();

        String postUrl = testFolderUrl + ".modifyAce.html";

		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair("principalId", testGroupId));
		postParams.add(new NameValuePair("privilege@jcr:read", "granted"));
		postParams.add(new NameValuePair("privilege@jcr:write", "denied"));
		postParams.add(new NameValuePair("privilege@jcr:modifyAccessControl", "bogus")); //invalid value should be ignored.
		
		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		H.assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);
		
		
		//fetch the JSON for the acl to verify the settings.
		String getUrl = testFolderUrl + ".acl.json";

		String json = H.getAuthenticatedContent(creds, getUrl, HttpTest.CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json);
		JSONObject jsonObject = new JSONObject(json);
		assertEquals(1, jsonObject.length());
		
		JSONObject aceObject = jsonObject.optJSONObject(testGroupId);
		assertNotNull(aceObject);

	        int order = aceObject.optInt("order");
	        assertEquals(0, order);

		String principalString = aceObject.optString("principal");
		assertEquals(testGroupId, principalString);
		
		JSONArray grantedArray = aceObject.optJSONArray("granted");
		assertNotNull(grantedArray);
		assertEquals(1, grantedArray.length());
		assertEquals("jcr:read", grantedArray.getString(0));

		JSONArray deniedArray = aceObject.optJSONArray("denied");
		assertNotNull(deniedArray);
		assertEquals("jcr:write", deniedArray.getString(0));
	}
	
	/**
	 * Test for SLING-997, preserve privileges that were not posted with the modifyAce 
	 * request.
	 */
	@Test 
	public void testMergeAceForUser() throws IOException, JSONException {
		testUserId = H.createTestUser();
		testFolderUrl = H.createTestFolder();
		
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
		H.assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);
		
		//fetch the JSON for the acl to verify the settings.
		String getUrl = testFolderUrl + ".acl.json";

		String json = H.getAuthenticatedContent(creds, getUrl, HttpTest.CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json);
		JSONObject jsonObject = new JSONObject(json);
		assertEquals(1, jsonObject.length());
		
		JSONObject aceObject = jsonObject.optJSONObject(testUserId);
		assertNotNull(aceObject);

		String principalString = aceObject.optString("principal");
		assertEquals(testUserId, principalString);
		
	        int order = aceObject.optInt("order");
	        assertEquals(0, order);

		JSONArray grantedArray = aceObject.optJSONArray("granted");
		assertNotNull(grantedArray);
		assertEquals(3, grantedArray.length());
		Set<String> grantedPrivilegeNames = new HashSet<String>();
		for (int i=0; i < grantedArray.length(); i++) {
			grantedPrivilegeNames.add(grantedArray.getString(i));
		}
		H.assertPrivilege(grantedPrivilegeNames, true, "jcr:read");
		H.assertPrivilege(grantedPrivilegeNames, true, "jcr:readAccessControl");
		H.assertPrivilege(grantedPrivilegeNames, true, "jcr:addChildNodes");

		JSONArray deniedArray = aceObject.optJSONArray("denied");
		assertNotNull(deniedArray);
		assertEquals(2, deniedArray.length());
		Set<String> deniedPrivilegeNames = new HashSet<String>();
		for (int i=0; i < deniedArray.length(); i++) {
			deniedPrivilegeNames.add(deniedArray.getString(i));
		}
		H.assertPrivilege(deniedPrivilegeNames, true, "jcr:modifyAccessControl");
		H.assertPrivilege(deniedPrivilegeNames, true, "jcr:removeChildNodes");
		
		
		
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
		
		H.assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams2, null);
		
		
		//fetch the JSON for the acl to verify the settings.
		String json2 = H.getAuthenticatedContent(creds, getUrl, HttpTest.CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json2);
		JSONObject jsonObject2 = new JSONObject(json2);
		assertEquals(1, jsonObject2.length());
		
		JSONObject aceObject2 = jsonObject2.optJSONObject(testUserId);
		assertNotNull(aceObject2);

		String principalString2 = aceObject2.optString("principal");
		assertEquals(testUserId, principalString2);
		
		JSONArray grantedArray2 = aceObject2.optJSONArray("granted");
		assertNotNull(grantedArray2);
		assertEquals(3, grantedArray2.length());
		Set<String> grantedPrivilegeNames2 = new HashSet<String>();
		for (int i=0; i < grantedArray2.length(); i++) {
			grantedPrivilegeNames2.add(grantedArray2.getString(i));
		}
		H.assertPrivilege(grantedPrivilegeNames2, true, "jcr:read");
		H.assertPrivilege(grantedPrivilegeNames2, true, "jcr:addChildNodes");
		H.assertPrivilege(grantedPrivilegeNames2, true, "jcr:modifyProperties");

		JSONArray deniedArray2 = aceObject2.optJSONArray("denied");
		assertNotNull(deniedArray2);
		assertEquals(2, deniedArray2.length());
		Set<String> deniedPrivilegeNames2 = new HashSet<String>();
		for (int i=0; i < deniedArray2.length(); i++) {
			deniedPrivilegeNames2.add(deniedArray2.getString(i));
		}
		H.assertPrivilege(deniedPrivilegeNames2, true, "jcr:modifyAccessControl");
		H.assertPrivilege(deniedPrivilegeNames2, true, "jcr:removeNode");
	}

	
	/**
	 * Test for SLING-997, preserve privileges that were not posted with the modifyAce 
	 * request.
	 */
	@Test 
	public void testMergeAceForUserSplitAggregatePrincipal() throws IOException, JSONException {
		testUserId = H.createTestUser();
		testFolderUrl = H.createTestFolder();
		
        String postUrl = testFolderUrl + ".modifyAce.html";

        //1. create an initial set of privileges
		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair("principalId", testUserId));
		postParams.add(new NameValuePair("privilege@jcr:read", "granted"));
		postParams.add(new NameValuePair("privilege@jcr:write", "denied"));
		
		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		H.assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);
		
		//fetch the JSON for the acl to verify the settings.
		String getUrl = testFolderUrl + ".acl.json";

		String json = H.getAuthenticatedContent(creds, getUrl, HttpTest.CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json);
		
		JSONObject jsonObject = new JSONObject(json);
		assertEquals(1, jsonObject.length());
		
		JSONObject aceObject = jsonObject.optJSONObject(testUserId);
		assertNotNull(aceObject);
		
		assertEquals(testUserId, aceObject.optString("principal"));
		
		JSONArray grantedArray = aceObject.optJSONArray("granted");
		assertNotNull(grantedArray);
		assertEquals(1, grantedArray.length());
		Set<String> grantedPrivilegeNames = new HashSet<String>();
		for (int i=0; i < grantedArray.length(); i++) {
			grantedPrivilegeNames.add(grantedArray.getString(i));
		}
		H.assertPrivilege(grantedPrivilegeNames, true, "jcr:read");

		JSONArray deniedArray = aceObject.optJSONArray("denied");
		assertNotNull(deniedArray);
		assertEquals(1, deniedArray.length());
		Set<String> deniedPrivilegeNames = new HashSet<String>();
		for (int i=0; i < deniedArray.length(); i++) {
			deniedPrivilegeNames.add(deniedArray.getString(i));
		}
		H.assertPrivilege(deniedPrivilegeNames, true, "jcr:write");
		
		
		
        //2. post a new set of privileges to merge with the existing privileges
		List<NameValuePair> postParams2 = new ArrayList<NameValuePair>();
		postParams2.add(new NameValuePair("principalId", testUserId));
		//jcr:read is not posted, so it should remain in the granted ACE
		postParams2.add(new NameValuePair("privilege@jcr:modifyProperties", "granted")); //add a new privilege
		//jcr:write is not posted, but one of the aggregate privileges is now granted, so the aggregate priviledge should be disagreaged into
		//  the remaining denied privileges in the denied ACE
		
		H.assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams2, null);
		
		
		//fetch the JSON for the acl to verify the settings.
		String json2 = H.getAuthenticatedContent(creds, getUrl, HttpTest.CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json2);
		
		JSONObject jsonObject2 = new JSONObject(json2);
		assertEquals(1, jsonObject2.length());
		
		JSONObject aceObject2 = jsonObject2.optJSONObject(testUserId);
		assertNotNull(aceObject2);
		
		assertEquals(testUserId, aceObject2.optString("principal"));
		
		JSONArray grantedArray2 = aceObject2.optJSONArray("granted");
		assertNotNull(grantedArray2);
		assertEquals(2, grantedArray2.length());
		Set<String> grantedPrivilegeNames2 = new HashSet<String>();
		for (int i=0; i < grantedArray2.length(); i++) {
			grantedPrivilegeNames2.add(grantedArray2.getString(i));
		}
		H.assertPrivilege(grantedPrivilegeNames2, true, "jcr:read");
		H.assertPrivilege(grantedPrivilegeNames2, true, "jcr:modifyProperties");

		JSONArray deniedArray2 = aceObject2.optJSONArray("denied");
		assertNotNull(deniedArray2);
		assertEquals(3, deniedArray2.length());
		Set<String> deniedPrivilegeNames2 = new HashSet<String>();
		for (int i=0; i < deniedArray2.length(); i++) {
			deniedPrivilegeNames2.add(deniedArray2.getString(i));
		}
		H.assertPrivilege(deniedPrivilegeNames2, false, "jcr:write");
		//only the remaining privileges from the disaggregated jcr:write collection should remain.
		H.assertPrivilege(deniedPrivilegeNames2, true, "jcr:addChildNodes");
		H.assertPrivilege(deniedPrivilegeNames2, true, "jcr:removeNode");
		H.assertPrivilege(deniedPrivilegeNames2, true, "jcr:removeChildNodes");
	}

	/**
	 * Test for SLING-997, preserve privileges that were not posted with the modifyAce 
	 * request.
	 */
	@Test 
	public void testMergeAceForUserCombineAggregatePrivilege() throws IOException, JSONException {
		testUserId = H.createTestUser();
		testFolderUrl = H.createTestFolder();
		
        String postUrl = testFolderUrl + ".modifyAce.html";

        //1. create an initial set of privileges
		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair("principalId", testUserId));
		postParams.add(new NameValuePair("privilege@jcr:read", "granted"));
		postParams.add(new NameValuePair("privilege@jcr:removeNode", "denied"));
		
		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		H.assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);
		
		//fetch the JSON for the acl to verify the settings.
		String getUrl = testFolderUrl + ".acl.json";

		String json = H.getAuthenticatedContent(creds, getUrl, HttpTest.CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json);
		
		JSONObject jsonObject = new JSONObject(json);
		assertEquals(1, jsonObject.length());
		
		JSONObject aceObject = jsonObject.optJSONObject(testUserId);
		assertNotNull(aceObject);
		
		assertEquals(testUserId, aceObject.optString("principal"));
		
		JSONArray grantedArray = aceObject.getJSONArray("granted");
		assertNotNull(grantedArray);
		assertEquals(1, grantedArray.length());
		Set<String> grantedPrivilegeNames = new HashSet<String>();
		for (int i=0; i < grantedArray.length(); i++) {
			grantedPrivilegeNames.add(grantedArray.getString(i));
		}
		H.assertPrivilege(grantedPrivilegeNames, true, "jcr:read");

		JSONArray deniedArray = aceObject.getJSONArray("denied");
		assertNotNull(deniedArray);
		assertEquals(1, deniedArray.length());
		Set<String> deniedPrivilegeNames = new HashSet<String>();
		for (int i=0; i < deniedArray.length(); i++) {
			deniedPrivilegeNames.add(deniedArray.getString(i));
		}
		H.assertPrivilege(deniedPrivilegeNames, true, "jcr:removeNode");
		
		
		
        //2. post a new set of privileges to merge with the existing privileges
		List<NameValuePair> postParams2 = new ArrayList<NameValuePair>();
		postParams2.add(new NameValuePair("principalId", testUserId));
		//jcr:read is not posted, so it should remain in the granted ACE
		
		//deny the full jcr:write aggregate privilege, which should merge with the
		//existing part.
		postParams2.add(new NameValuePair("privilege@jcr:write", "denied")); //add a new privilege
		
		H.assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams2, null);
		
		
		//fetch the JSON for the acl to verify the settings.
		String json2 = H.getAuthenticatedContent(creds, getUrl, HttpTest.CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json2);
		
		JSONObject jsonObject2 = new JSONObject(json2);
		assertEquals(1, jsonObject2.length());
		
		JSONObject aceObject2 = jsonObject2.optJSONObject(testUserId);
		assertNotNull(aceObject2);
		
		assertEquals(testUserId, aceObject.optString("principal"));
		
		JSONArray grantedArray2 = aceObject2.optJSONArray("granted");
		assertNotNull(grantedArray2);
		assertEquals(1, grantedArray2.length());
		Set<String> grantedPrivilegeNames2 = new HashSet<String>();
		for (int i=0; i < grantedArray2.length(); i++) {
			grantedPrivilegeNames2.add(grantedArray2.getString(i));
		}
		H.assertPrivilege(grantedPrivilegeNames2, true, "jcr:read");

		JSONArray deniedArray2 = aceObject2.optJSONArray("denied");
		assertNotNull(deniedArray2);
		assertEquals(1, deniedArray2.length());
		Set<String> deniedPrivilegeNames2 = new HashSet<String>();
		for (int i=0; i < deniedArray2.length(); i++) {
			deniedPrivilegeNames2.add(deniedArray2.getString(i));
		}
		H.assertPrivilege(deniedPrivilegeNames2, true, "jcr:write");
	}

	
	/**
	 * Test ACE update with a deny privilege for an ACE that already contains
	 * a grant privilege 
	 */
	@Test 
	public void testMergeAceForUserDenyPrivilegeAfterGrantPrivilege() throws IOException, JSONException {
		testUserId = H.createTestUser();
		testFolderUrl = H.createTestFolder();
		
        String postUrl = testFolderUrl + ".modifyAce.html";

        //1. create an initial set of privileges
		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair("principalId", testUserId));
		postParams.add(new NameValuePair("privilege@jcr:write", "granted"));
		
		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		H.assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);
		
		//fetch the JSON for the acl to verify the settings.
		String getUrl = testFolderUrl + ".acl.json";

		String json = H.getAuthenticatedContent(creds, getUrl, HttpTest.CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json);
		
		JSONObject jsonObject = new JSONObject(json);
		assertEquals(1, jsonObject.length());

		JSONObject aceObject = jsonObject.optJSONObject(testUserId);
		assertNotNull(aceObject);
		
		assertEquals(testUserId, aceObject.optString("principal"));
		
		JSONArray grantedArray = aceObject.optJSONArray("granted");
		assertNotNull(grantedArray);
		assertEquals(1, grantedArray.length());
		Set<String> grantedPrivilegeNames = new HashSet<String>();
		for (int i=0; i < grantedArray.length(); i++) {
			grantedPrivilegeNames.add(grantedArray.getString(i));
		}
		H.assertPrivilege(grantedPrivilegeNames, true, "jcr:write");

		assertFalse(aceObject.has("denied"));
		
		
        //2. post a new set of privileges to merge with the existing privileges
		List<NameValuePair> postParams2 = new ArrayList<NameValuePair>();
		postParams2.add(new NameValuePair("principalId", testUserId));
		//jcr:write is not posted, so it should remain in the granted ACE
		
		//deny the jcr:nodeTypeManagement privilege, which should merge with the
		//existing ACE.
		postParams2.add(new NameValuePair("privilege@jcr:nodeTypeManagement", "denied")); //add a new privilege
		
		H.assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams2, null);
		
		
		//fetch the JSON for the acl to verify the settings.
		String json2 = H.getAuthenticatedContent(creds, getUrl, HttpTest.CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json2);

		JSONObject jsonObject2 = new JSONObject(json2);
		assertEquals(1, jsonObject2.length());
		
		JSONObject aceObject2 = jsonObject2.optJSONObject(testUserId);
		assertNotNull(aceObject2);
		
		assertEquals(testUserId, aceObject2.optString("principal"));
		
		JSONArray grantedArray2 = aceObject2.optJSONArray("granted");
		assertNotNull(grantedArray2);
		assertEquals(1, grantedArray2.length());
		Set<String> grantedPrivilegeNames2 = new HashSet<String>();
		for (int i=0; i < grantedArray2.length(); i++) {
			grantedPrivilegeNames2.add(grantedArray2.getString(i));
		}
		H.assertPrivilege(grantedPrivilegeNames2, true, "jcr:write");

		JSONArray deniedArray2 = aceObject2.optJSONArray("denied");
		assertNotNull(deniedArray2);
		assertEquals(1, deniedArray2.length());
		Set<String> deniedPrivilegeNames2 = new HashSet<String>();
		for (int i=0; i < deniedArray2.length(); i++) {
			deniedPrivilegeNames2.add(deniedArray2.getString(i));
		}
		H.assertPrivilege(deniedPrivilegeNames2, true, "jcr:nodeTypeManagement");
	}


	
	/**
	 * Test to verify adding an ACE in the first position of 
	 * the ACL
	 */
	@Test 
	public void testAddAceOrderByFirst() throws IOException, JSONException {
		createAceOrderTestFolderWithOneAce();
		
		testGroupId = H.createTestGroup();

		addOrUpdateAce(testFolderUrl, testGroupId, true, "first");

		//fetch the JSON for the acl to verify the settings.
		String getUrl = testFolderUrl + ".acl.json";

		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		String json = H.getAuthenticatedContent(creds, getUrl, HttpTest.CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json);

		JSONObject jsonObject = new JSONObject(json);
		assertEquals(2, jsonObject.length());

		JSONObject group = jsonObject.getJSONObject(testGroupId);
		assertNotNull(group);
		assertEquals(testGroupId, group.getString("principal"));
                assertEquals(0, group.getInt("order"));
		JSONObject user =  jsonObject.getJSONObject(testUserId);
                assertNotNull(user);
                assertEquals(testUserId, user.getString("principal"));
                assertEquals(1, user.getInt("order"));
	}	

	/**
	 * Test to verify adding an ACE at the end 
	 * the ACL
	 */
	@Test 
	public void testAddAceOrderByLast() throws IOException, JSONException {
		createAceOrderTestFolderWithOneAce();
		
		testGroupId = H.createTestGroup();

		addOrUpdateAce(testFolderUrl, testGroupId, true, "last");

		//fetch the JSON for the acl to verify the settings.
		String getUrl = testFolderUrl + ".acl.json";

		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		String json = H.getAuthenticatedContent(creds, getUrl, HttpTest.CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json);

		JSONObject jsonObject = new JSONObject(json);
		assertEquals(2, jsonObject.length());
		
                JSONObject user =  jsonObject.getJSONObject(testUserId);
                assertNotNull(user);
                assertEquals(testUserId, user.getString("principal"));
                assertEquals(0, user.getInt("order"));
                JSONObject group = jsonObject.getJSONObject(testGroupId);
                assertNotNull(group);
                assertEquals(testGroupId, group.getString("principal"));
                assertEquals(1, group.getInt("order"));

	}	

	/**
	 * Test to verify adding an ACE before an existing ACE 
	 * the ACL
	 */
	@Test 
	public void testAddAceOrderByBefore() throws IOException, JSONException {
		createAceOrderTestFolderWithOneAce();
		
		testGroupId = H.createTestGroup();

		addOrUpdateAce(testFolderUrl, testGroupId, true, "before " + testUserId);

		//fetch the JSON for the acl to verify the settings.
		String getUrl = testFolderUrl + ".acl.json";

		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		String json = H.getAuthenticatedContent(creds, getUrl, HttpTest.CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json);

		
                JSONObject jsonObject = new JSONObject(json);
                assertEquals(2, jsonObject.length());


                JSONObject group = jsonObject.getJSONObject(testGroupId);
                assertNotNull(group);
                assertEquals(testGroupId, group.getString("principal"));
                assertEquals(0, group.getInt("order"));
                JSONObject user =  jsonObject.getJSONObject(testUserId);
                assertNotNull(user);
                assertEquals(testUserId, user.getString("principal"));
                assertEquals(1, user.getInt("order"));

	}	

	/**
	 * Test to verify adding an ACE after an existing ACE 
	 * the ACL
	 */
	@Test 
	public void testAddAceOrderByAfter() throws IOException, JSONException {
		createAceOrderTestFolderWithOneAce();
		
		testGroupId = H.createTestGroup();

		addOrUpdateAce(testFolderUrl, testGroupId, true, "after " + testUserId);

		//fetch the JSON for the acl to verify the settings.
		String getUrl = testFolderUrl + ".acl.json";

		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		String json = H.getAuthenticatedContent(creds, getUrl, HttpTest.CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json);

                JSONObject jsonObject = new JSONObject(json);
                assertEquals(2, jsonObject.length());

                JSONObject user =  jsonObject.getJSONObject(testUserId);
                assertNotNull(user);
                assertEquals(testUserId, user.getString("principal"));
                assertEquals(0, user.getInt("order"));
                JSONObject group = jsonObject.getJSONObject(testGroupId);
                assertNotNull(group);
                assertEquals(testGroupId, group.getString("principal"));
                assertEquals(1, group.getInt("order"));

	}	

	/**
	 * Test to verify adding an ACE at a specific index inside 
	 * the ACL
	 */
	@Test 
	public void testAddAceOrderByNumeric() throws IOException, JSONException {
		createAceOrderTestFolderWithOneAce();
		
		testGroupId = H.createTestGroup();
		addOrUpdateAce(testFolderUrl, testGroupId, true, "0");

		//fetch the JSON for the acl to verify the settings.
		String getUrl = testFolderUrl + ".acl.json";

		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		String json = H.getAuthenticatedContent(creds, getUrl, HttpTest.CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json);

		
                JSONObject jsonObject = new JSONObject(json);
                assertEquals(2, jsonObject.length());

                JSONObject group = jsonObject.getJSONObject(testGroupId);
                assertNotNull(group);
                assertEquals(testGroupId, group.getString("principal"));
                assertEquals(0, group.getInt("order"));

                JSONObject user =  jsonObject.getJSONObject(testUserId);
                assertNotNull(user);
                assertEquals(testUserId, user.getString("principal"));
                assertEquals(1, user.getInt("order"));



		//add another principal between the testGroupId and testUserId
		testUserId2 = H.createTestUser();
		addOrUpdateAce(testFolderUrl, testUserId2, true, "1");

		String json2 = H.getAuthenticatedContent(creds, getUrl, HttpTest.CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json2);

                JSONObject jsonObject2 = new JSONObject(json2);
                assertEquals(3, jsonObject2.length());

                JSONObject group2 = jsonObject2.getJSONObject(testGroupId);
                assertNotNull(group2);
                assertEquals(testGroupId, group2.getString("principal"));
                assertEquals(0, group2.getInt("order"));

                JSONObject user3 =  jsonObject2.getJSONObject(testUserId2);
                assertNotNull(user3);
                assertEquals(testUserId2, user3.getString("principal"));
                assertEquals(1, user3.getInt("order"));

                JSONObject user2 =  jsonObject2.getJSONObject(testUserId);
                assertNotNull(user2);
                assertEquals(testUserId, user2.getString("principal"));
                assertEquals(2, user2.getInt("order"));

	}	

	/**
	 * Test to make sure modifying an existing ace without changing the order 
	 * leaves the ACE in the same position in the ACL
	 */
	@Test 
	public void testUpdateAcePreservePosition() throws IOException, JSONException {
		createAceOrderTestFolderWithOneAce();
		
		testGroupId = H.createTestGroup();

		addOrUpdateAce(testFolderUrl, testGroupId, true, "first");

		//update the ace to make sure the update does not change the ACE order
		addOrUpdateAce(testFolderUrl, testGroupId, false, null);
		
		
		//fetch the JSON for the acl to verify the settings.
		String getUrl = testFolderUrl + ".acl.json";

		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		String json = H.getAuthenticatedContent(creds, getUrl, HttpTest.CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json);
		
                JSONObject jsonObject = new JSONObject(json);
                assertEquals(2, jsonObject.length());

                JSONObject group = jsonObject.getJSONObject(testGroupId);
                assertNotNull(group);
                assertEquals(testGroupId, group.getString("principal"));
                assertEquals(0, group.getInt("order"));
                JSONObject user =  jsonObject.getJSONObject(testUserId);
                assertNotNull(user);
                assertEquals(testUserId, user.getString("principal"));
                assertEquals(1, user.getInt("order"));

	}	

	
	/**
	 * Helper to create a test folder with a single ACE pre-created
	 */
	private void createAceOrderTestFolderWithOneAce() throws IOException, JSONException {
		testUserId = H.createTestUser();
		
		testFolderUrl = H.createTestFolder();

		addOrUpdateAce(testFolderUrl, testUserId, true, null);

		//fetch the JSON for the acl to verify the settings.
		String getUrl = testFolderUrl + ".acl.json";

		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		String json = H.getAuthenticatedContent(creds, getUrl, HttpTest.CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json);
		
                JSONObject jsonObject = new JSONObject(json);
                assertEquals(1, jsonObject.length());

                JSONObject user = jsonObject.getJSONObject(testUserId);
                assertNotNull(user);
                assertEquals(testUserId, user.getString("principal"));
                assertEquals(0, user.getInt("order"));

	}
	
	/**
	 * Helper to add or update an ace for testing
	 */
	private void addOrUpdateAce(String folderUrl, String principalId, boolean readGranted, String order) throws IOException, JSONException {
        String postUrl = folderUrl + ".modifyAce.html";

		//1. create an initial set of privileges
		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair("principalId", principalId));
		postParams.add(new NameValuePair("privilege@jcr:read", readGranted ? "granted" : "denied"));
		postParams.add(new NameValuePair("privilege@jcr:write", "denied"));
		if (order != null) {
			postParams.add(new NameValuePair("order", order));
		}
		
		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		H.assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);
	}
	
	/**
	 * Test for SLING-1677
	 */
	@Test 
	public void testModifyAceResponseAsJSON() throws IOException, JSONException {
		testUserId = H.createTestUser();
		
		testFolderUrl = H.createTestFolder();
		
        String postUrl = testFolderUrl + ".modifyAce.json";

		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair("principalId", testUserId));
		postParams.add(new NameValuePair("privilege@jcr:read", "granted"));
		postParams.add(new NameValuePair("privilege@jcr:write", "denied"));
		postParams.add(new NameValuePair("privilege@jcr:modifyAccessControl", "bogus")); //invalid value should be ignored.
		
		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		String json = H.getAuthenticatedPostContent(creds, postUrl, HttpTest.CONTENT_TYPE_JSON, postParams, HttpServletResponse.SC_OK);

        //make sure the json response can be parsed as a JSON object
        JSONObject jsonObject = new JSONObject(json);
		assertNotNull(jsonObject);
	}
	
	
	/**
	 * Test for SLING-3010
	 */
	@Test 
	public void testMergeAceForUserGrantNestedAggregatePrivilegeAfterDenySuperAggregatePrivilege() throws IOException, JSONException {
		testUserId = H.createTestUser();
		
		testFolderUrl = H.createTestFolder();
		
        String postUrl = testFolderUrl + ".modifyAce.json";

        //1. setup an initial set of denied privileges for the test user
        List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair("principalId", testUserId));
		postParams.add(new NameValuePair("privilege@jcr:versionManagement", "denied"));
		postParams.add(new NameValuePair("privilege@jcr:read", "denied"));
		postParams.add(new NameValuePair("privilege@jcr:modifyAccessControl", "denied")); 
		postParams.add(new NameValuePair("privilege@rep:write", "denied")); 
		
		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		/*String json = */H.getAuthenticatedPostContent(creds, postUrl, HttpTest.CONTENT_TYPE_JSON, postParams, HttpServletResponse.SC_OK);

		
        //2. now grant the jcr:write subset from the rep:write aggregate privilege
		postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair("principalId", testUserId));
		postParams.add(new NameValuePair("privilege@jcr:versionManagement", "granted"));
		postParams.add(new NameValuePair("privilege@jcr:read", "granted"));
		postParams.add(new NameValuePair("privilege@jcr:modifyAccessControl", "granted")); 
		postParams.add(new NameValuePair("privilege@jcr:write", "granted")); //sub-aggregate of rep:write  
		
		/*String json = */H.getAuthenticatedPostContent(creds, postUrl, HttpTest.CONTENT_TYPE_JSON, postParams, HttpServletResponse.SC_OK);
		
		//3. verify that the acl has the correct values
		//fetch the JSON for the acl to verify the settings.
		String getUrl = testFolderUrl + ".acl.json";

		String json = H.getAuthenticatedContent(creds, getUrl, HttpTest.CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json);
		
		JSONObject jsonObject = new JSONObject(json);
		assertEquals(1, jsonObject.length());
		
		JSONObject aceObject = jsonObject.optJSONObject(testUserId);
		assertNotNull(aceObject);
		
		assertEquals(testUserId, aceObject.optString("principal"));
		
		JSONArray grantedArray = aceObject.getJSONArray("granted");
		assertNotNull(grantedArray);
		assertEquals(4, grantedArray.length());
		Set<String> grantedPrivilegeNames = new HashSet<String>();
		for (int i=0; i < grantedArray.length(); i++) {
			grantedPrivilegeNames.add(grantedArray.getString(i));
		}
		H.assertPrivilege(grantedPrivilegeNames, true, "jcr:versionManagement");
		H.assertPrivilege(grantedPrivilegeNames, true, "jcr:read");
		H.assertPrivilege(grantedPrivilegeNames, true, "jcr:modifyAccessControl");
		H.assertPrivilege(grantedPrivilegeNames, true, "jcr:write");

		JSONArray deniedArray = aceObject.getJSONArray("denied");
		assertNotNull(deniedArray);
		assertEquals(1, deniedArray.length());
		Set<String> deniedPrivilegeNames = new HashSet<String>();
		for (int i=0; i < deniedArray.length(); i++) {
			deniedPrivilegeNames.add(deniedArray.getString(i));
		}
		//the leftovers from the denied rep:write that were not granted with jcr:write
		H.assertPrivilege(deniedPrivilegeNames, true, "jcr:nodeTypeManagement"); 
	}

	/**
	 * Test for SLING-3010
	 */
	@Test 
	public void testMergeAceForUserGrantAggregatePrivilegePartsAfterDenyAggregatePrivilege() throws IOException, JSONException {
		testUserId = H.createTestUser();
		
		testFolderUrl = H.createTestFolder();
		
        String postUrl = testFolderUrl + ".modifyAce.json";

        //1. setup an initial set of denied privileges for the test user
        List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair("principalId", testUserId));
		postParams.add(new NameValuePair("privilege@jcr:versionManagement", "denied"));
		postParams.add(new NameValuePair("privilege@jcr:read", "denied"));
		postParams.add(new NameValuePair("privilege@jcr:modifyAccessControl", "denied")); 
		postParams.add(new NameValuePair("privilege@rep:write", "denied")); 
		
		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		/*String json = */H.getAuthenticatedPostContent(creds, postUrl, HttpTest.CONTENT_TYPE_JSON, postParams, HttpServletResponse.SC_OK);

        //2. now grant the all the privileges contained in the rep:write privilege
		postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair("principalId", testUserId));
		postParams.add(new NameValuePair("privilege@jcr:versionManagement", "granted"));
		postParams.add(new NameValuePair("privilege@jcr:read", "granted"));
		postParams.add(new NameValuePair("privilege@jcr:modifyAccessControl", "granted")); 
		postParams.add(new NameValuePair("privilege@jcr:nodeTypeManagement", "granted")); //sub-privilege of rep:write  
		postParams.add(new NameValuePair("privilege@jcr:write", "granted")); //sub-aggregate of rep:write  
		
		/*String json = */H.getAuthenticatedPostContent(creds, postUrl, HttpTest.CONTENT_TYPE_JSON, postParams, HttpServletResponse.SC_OK);
		
		//3. verify that the acl has the correct values
		//fetch the JSON for the acl to verify the settings.
		String getUrl = testFolderUrl + ".acl.json";

		String json = H.getAuthenticatedContent(creds, getUrl, HttpTest.CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json);
		
		JSONObject jsonObject = new JSONObject(json);
		assertEquals(1, jsonObject.length());
		
		JSONObject aceObject = jsonObject.optJSONObject(testUserId);
		assertNotNull(aceObject);
		
		assertEquals(testUserId, aceObject.optString("principal"));
		
		JSONArray grantedArray = aceObject.getJSONArray("granted");
		assertNotNull(grantedArray);
		Set<String> grantedPrivilegeNames = new HashSet<String>();
		for (int i=0; i < grantedArray.length(); i++) {
			grantedPrivilegeNames.add(grantedArray.getString(i));
		}
		H.assertPrivilege(grantedPrivilegeNames, true, "jcr:versionManagement");
		H.assertPrivilege(grantedPrivilegeNames, true, "jcr:read");
		H.assertPrivilege(grantedPrivilegeNames, true, "jcr:modifyAccessControl");
		H.assertPrivilege(grantedPrivilegeNames, true, "rep:write"); //jcr:nodeTypeManagement + jcr:write
        assertEquals("Expecting the correct number of privileges in " + grantedPrivilegeNames, 4, grantedPrivilegeNames.size());

		//should be nothing left in the denied set.
		JSONArray deniedArray = aceObject.optJSONArray("denied");
		assertNull(deniedArray);
	}
	
}
