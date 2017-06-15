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

import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.sling.commons.testing.integration.HttpTest;
import org.apache.sling.launchpad.webapp.integrationtest.util.JsonUtil;
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
	public void testModifyAceForUser() throws IOException, JsonException {
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
		JsonObject jsonObject = JsonUtil.parseObject(json);
		assertEquals(1, jsonObject.size());
		
		JsonObject aceObject = jsonObject.getJsonObject(testUserId);
		assertNotNull(aceObject);
		
		String principalString = aceObject.getString("principal");
		assertEquals(testUserId, principalString);

	        int order = aceObject.getInt("order");
	        assertEquals(0, order);

		JsonArray grantedArray = aceObject.getJsonArray("granted");
		assertNotNull(grantedArray);
		assertEquals(1, grantedArray.size());
		assertEquals("jcr:read", grantedArray.getString(0));

		JsonArray deniedArray = aceObject.getJsonArray("denied");
		assertNotNull(deniedArray);
		assertEquals(1, deniedArray.size());
		assertEquals("jcr:write", deniedArray.getString(0));
	}

	@Test 
	public void testModifyAceForGroup() throws IOException, JsonException {
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
		JsonObject jsonObject = JsonUtil.parseObject(json);
		assertEquals(1, jsonObject.size());
		
		JsonObject aceObject = jsonObject.getJsonObject(testGroupId);
		assertNotNull(aceObject);

	        int order = aceObject.getInt("order");
	        assertEquals(0, order);

		String principalString = aceObject.getString("principal");
		assertEquals(testGroupId, principalString);
		
		JsonArray grantedArray = aceObject.getJsonArray("granted");
		assertNotNull(grantedArray);
		assertEquals(1, grantedArray.size());
		assertEquals("jcr:read", grantedArray.getString(0));

		JsonArray deniedArray = aceObject.getJsonArray("denied");
		assertNotNull(deniedArray);
		assertEquals("jcr:write", deniedArray.getString(0));
	}
	
	/**
	 * Test for SLING-997, preserve privileges that were not posted with the modifyAce 
	 * request.
	 */
	@Test 
	public void testMergeAceForUser() throws IOException, JsonException {
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
		JsonObject jsonObject = JsonUtil.parseObject(json);
		assertEquals(1, jsonObject.size());
		
		JsonObject aceObject = jsonObject.getJsonObject(testUserId);
		assertNotNull(aceObject);

		String principalString = aceObject.getString("principal");
		assertEquals(testUserId, principalString);
		
	        int order = aceObject.getInt("order");
	        assertEquals(0, order);

		JsonArray grantedArray = aceObject.getJsonArray("granted");
		assertNotNull(grantedArray);
		assertEquals(3, grantedArray.size());
		Set<String> grantedPrivilegeNames = new HashSet<String>();
		for (int i=0; i < grantedArray.size(); i++) {
			grantedPrivilegeNames.add(grantedArray.getString(i));
		}
		H.assertPrivilege(grantedPrivilegeNames, true, "jcr:read");
		H.assertPrivilege(grantedPrivilegeNames, true, "jcr:readAccessControl");
		H.assertPrivilege(grantedPrivilegeNames, true, "jcr:addChildNodes");

		JsonArray deniedArray = aceObject.getJsonArray("denied");
		assertNotNull(deniedArray);
		assertEquals(2, deniedArray.size());
		Set<String> deniedPrivilegeNames = new HashSet<String>();
		for (int i=0; i < deniedArray.size(); i++) {
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
		JsonObject jsonObject2 = JsonUtil.parseObject(json2);
		assertEquals(1, jsonObject2.size());
		
		JsonObject aceObject2 = jsonObject2.getJsonObject(testUserId);
		assertNotNull(aceObject2);

		String principalString2 = aceObject2.getString("principal");
		assertEquals(testUserId, principalString2);
		
		JsonArray grantedArray2 = aceObject2.getJsonArray("granted");
		assertNotNull(grantedArray2);
		assertEquals(3, grantedArray2.size());
		Set<String> grantedPrivilegeNames2 = new HashSet<String>();
		for (int i=0; i < grantedArray2.size(); i++) {
			grantedPrivilegeNames2.add(grantedArray2.getString(i));
		}
		H.assertPrivilege(grantedPrivilegeNames2, true, "jcr:read");
		H.assertPrivilege(grantedPrivilegeNames2, true, "jcr:addChildNodes");
		H.assertPrivilege(grantedPrivilegeNames2, true, "jcr:modifyProperties");

		JsonArray deniedArray2 = aceObject2.getJsonArray("denied");
		assertNotNull(deniedArray2);
		assertEquals(2, deniedArray2.size());
		Set<String> deniedPrivilegeNames2 = new HashSet<String>();
		for (int i=0; i < deniedArray2.size(); i++) {
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
	public void testMergeAceForUserSplitAggregatePrincipal() throws IOException, JsonException {
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
		
		JsonObject jsonObject = JsonUtil.parseObject(json);
		assertEquals(1, jsonObject.size());
		
		JsonObject aceObject = jsonObject.getJsonObject(testUserId);
		assertNotNull(aceObject);
		
		assertEquals(testUserId, aceObject.getString("principal"));
		
		JsonArray grantedArray = aceObject.getJsonArray("granted");
		assertNotNull(grantedArray);
		assertEquals(1, grantedArray.size());
		Set<String> grantedPrivilegeNames = new HashSet<String>();
		for (int i=0; i < grantedArray.size(); i++) {
			grantedPrivilegeNames.add(grantedArray.getString(i));
		}
		H.assertPrivilege(grantedPrivilegeNames, true, "jcr:read");

		JsonArray deniedArray = aceObject.getJsonArray("denied");
		assertNotNull(deniedArray);
		assertEquals(1, deniedArray.size());
		Set<String> deniedPrivilegeNames = new HashSet<String>();
		for (int i=0; i < deniedArray.size(); i++) {
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
		
		JsonObject jsonObject2 = JsonUtil.parseObject(json2);
		assertEquals(1, jsonObject2.size());
		
		JsonObject aceObject2 = jsonObject2.getJsonObject(testUserId);
		assertNotNull(aceObject2);
		
		assertEquals(testUserId, aceObject2.getString("principal"));
		
		JsonArray grantedArray2 = aceObject2.getJsonArray("granted");
		assertNotNull(grantedArray2);
		assertEquals(2, grantedArray2.size());
		Set<String> grantedPrivilegeNames2 = new HashSet<String>();
		for (int i=0; i < grantedArray2.size(); i++) {
			grantedPrivilegeNames2.add(grantedArray2.getString(i));
		}
		H.assertPrivilege(grantedPrivilegeNames2, true, "jcr:read");
		H.assertPrivilege(grantedPrivilegeNames2, true, "jcr:modifyProperties");

		JsonArray deniedArray2 = aceObject2.getJsonArray("denied");
		assertNotNull(deniedArray2);
		assertEquals(3, deniedArray2.size());
		Set<String> deniedPrivilegeNames2 = new HashSet<String>();
		for (int i=0; i < deniedArray2.size(); i++) {
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
	public void testMergeAceForUserCombineAggregatePrivilege() throws IOException, JsonException {
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
		
		JsonObject jsonObject = JsonUtil.parseObject(json);
		assertEquals(1, jsonObject.size());
		
		JsonObject aceObject = jsonObject.getJsonObject(testUserId);
		assertNotNull(aceObject);
		
		assertEquals(testUserId, aceObject.getString("principal"));
		
		JsonArray grantedArray = aceObject.getJsonArray("granted");
		assertNotNull(grantedArray);
		assertEquals(1, grantedArray.size());
		Set<String> grantedPrivilegeNames = new HashSet<String>();
		for (int i=0; i < grantedArray.size(); i++) {
			grantedPrivilegeNames.add(grantedArray.getString(i));
		}
		H.assertPrivilege(grantedPrivilegeNames, true, "jcr:read");

		JsonArray deniedArray = aceObject.getJsonArray("denied");
		assertNotNull(deniedArray);
		assertEquals(1, deniedArray.size());
		Set<String> deniedPrivilegeNames = new HashSet<String>();
		for (int i=0; i < deniedArray.size(); i++) {
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
		
		JsonObject jsonObject2 = JsonUtil.parseObject(json2);
		assertEquals(1, jsonObject2.size());
		
		JsonObject aceObject2 = jsonObject2.getJsonObject(testUserId);
		assertNotNull(aceObject2);
		
		assertEquals(testUserId, aceObject.getString("principal"));
		
		JsonArray grantedArray2 = aceObject2.getJsonArray("granted");
		assertNotNull(grantedArray2);
		assertEquals(1, grantedArray2.size());
		Set<String> grantedPrivilegeNames2 = new HashSet<String>();
		for (int i=0; i < grantedArray2.size(); i++) {
			grantedPrivilegeNames2.add(grantedArray2.getString(i));
		}
		H.assertPrivilege(grantedPrivilegeNames2, true, "jcr:read");

		JsonArray deniedArray2 = aceObject2.getJsonArray("denied");
		assertNotNull(deniedArray2);
		assertEquals(1, deniedArray2.size());
		Set<String> deniedPrivilegeNames2 = new HashSet<String>();
		for (int i=0; i < deniedArray2.size(); i++) {
			deniedPrivilegeNames2.add(deniedArray2.getString(i));
		}
		H.assertPrivilege(deniedPrivilegeNames2, true, "jcr:write");
	}

	
	/**
	 * Test ACE update with a deny privilege for an ACE that already contains
	 * a grant privilege 
	 */
	@Test 
	public void testMergeAceForUserDenyPrivilegeAfterGrantPrivilege() throws IOException, JsonException {
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
		
		JsonObject jsonObject = JsonUtil.parseObject(json);
		assertEquals(1, jsonObject.size());

		JsonObject aceObject = jsonObject.getJsonObject(testUserId);
		assertNotNull(aceObject);
		
		assertEquals(testUserId, aceObject.getString("principal"));
		
		JsonArray grantedArray = aceObject.getJsonArray("granted");
		assertNotNull(grantedArray);
		assertEquals(1, grantedArray.size());
		Set<String> grantedPrivilegeNames = new HashSet<String>();
		for (int i=0; i < grantedArray.size(); i++) {
			grantedPrivilegeNames.add(grantedArray.getString(i));
		}
		H.assertPrivilege(grantedPrivilegeNames, true, "jcr:write");

		assertFalse(aceObject.containsKey("denied"));
		
		
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

		JsonObject jsonObject2 = JsonUtil.parseObject(json2);
		assertEquals(1, jsonObject2.size());
		
		JsonObject aceObject2 = jsonObject2.getJsonObject(testUserId);
		assertNotNull(aceObject2);
		
		assertEquals(testUserId, aceObject2.getString("principal"));
		
		JsonArray grantedArray2 = aceObject2.getJsonArray("granted");
		assertNotNull(grantedArray2);
		assertEquals(1, grantedArray2.size());
		Set<String> grantedPrivilegeNames2 = new HashSet<String>();
		for (int i=0; i < grantedArray2.size(); i++) {
			grantedPrivilegeNames2.add(grantedArray2.getString(i));
		}
		H.assertPrivilege(grantedPrivilegeNames2, true, "jcr:write");

		JsonArray deniedArray2 = aceObject2.getJsonArray("denied");
		assertNotNull(deniedArray2);
		assertEquals(1, deniedArray2.size());
		Set<String> deniedPrivilegeNames2 = new HashSet<String>();
		for (int i=0; i < deniedArray2.size(); i++) {
			deniedPrivilegeNames2.add(deniedArray2.getString(i));
		}
		H.assertPrivilege(deniedPrivilegeNames2, true, "jcr:nodeTypeManagement");
	}


	
	/**
	 * Test to verify adding an ACE in the first position of 
	 * the ACL
	 */
	@Test 
	public void testAddAceOrderByFirst() throws IOException, JsonException {
		createAceOrderTestFolderWithOneAce();
		
		testGroupId = H.createTestGroup();

		addOrUpdateAce(testFolderUrl, testGroupId, true, "first");

		//fetch the JSON for the acl to verify the settings.
		String getUrl = testFolderUrl + ".acl.json";

		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		String json = H.getAuthenticatedContent(creds, getUrl, HttpTest.CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json);

		JsonObject jsonObject = JsonUtil.parseObject(json);
		assertEquals(2, jsonObject.size());

		JsonObject group = jsonObject.getJsonObject(testGroupId);
		assertNotNull(group);
		assertEquals(testGroupId, group.getString("principal"));
                assertEquals(0, group.getInt("order"));
		JsonObject user =  jsonObject.getJsonObject(testUserId);
                assertNotNull(user);
                assertEquals(testUserId, user.getString("principal"));
                assertEquals(1, user.getInt("order"));
	}	

	/**
	 * Test to verify adding an ACE at the end 
	 * the ACL
	 */
	@Test 
	public void testAddAceOrderByLast() throws IOException, JsonException {
		createAceOrderTestFolderWithOneAce();
		
		testGroupId = H.createTestGroup();

		addOrUpdateAce(testFolderUrl, testGroupId, true, "last");

		//fetch the JSON for the acl to verify the settings.
		String getUrl = testFolderUrl + ".acl.json";

		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		String json = H.getAuthenticatedContent(creds, getUrl, HttpTest.CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json);

		JsonObject jsonObject = JsonUtil.parseObject(json);
		assertEquals(2, jsonObject.size());
		
                JsonObject user =  jsonObject.getJsonObject(testUserId);
                assertNotNull(user);
                assertEquals(testUserId, user.getString("principal"));
                assertEquals(0, user.getInt("order"));
                JsonObject group = jsonObject.getJsonObject(testGroupId);
                assertNotNull(group);
                assertEquals(testGroupId, group.getString("principal"));
                assertEquals(1, group.getInt("order"));

	}	

	/**
	 * Test to verify adding an ACE before an existing ACE 
	 * the ACL
	 */
	@Test 
	public void testAddAceOrderByBefore() throws IOException, JsonException {
		createAceOrderTestFolderWithOneAce();
		
		testGroupId = H.createTestGroup();

		addOrUpdateAce(testFolderUrl, testGroupId, true, "before " + testUserId);

		//fetch the JSON for the acl to verify the settings.
		String getUrl = testFolderUrl + ".acl.json";

		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		String json = H.getAuthenticatedContent(creds, getUrl, HttpTest.CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json);

		
                JsonObject jsonObject = JsonUtil.parseObject(json);
                assertEquals(2, jsonObject.size());


                JsonObject group = jsonObject.getJsonObject(testGroupId);
                assertNotNull(group);
                assertEquals(testGroupId, group.getString("principal"));
                assertEquals(0, group.getInt("order"));
                JsonObject user =  jsonObject.getJsonObject(testUserId);
                assertNotNull(user);
                assertEquals(testUserId, user.getString("principal"));
                assertEquals(1, user.getInt("order"));

	}	

	/**
	 * Test to verify adding an ACE after an existing ACE 
	 * the ACL
	 */
	@Test 
	public void testAddAceOrderByAfter() throws IOException, JsonException {
		createAceOrderTestFolderWithOneAce();
		
		testGroupId = H.createTestGroup();

		addOrUpdateAce(testFolderUrl, testGroupId, true, "after " + testUserId);

		//fetch the JSON for the acl to verify the settings.
		String getUrl = testFolderUrl + ".acl.json";

		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		String json = H.getAuthenticatedContent(creds, getUrl, HttpTest.CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json);

                JsonObject jsonObject = JsonUtil.parseObject(json);
                assertEquals(2, jsonObject.size());

                JsonObject user =  jsonObject.getJsonObject(testUserId);
                assertNotNull(user);
                assertEquals(testUserId, user.getString("principal"));
                assertEquals(0, user.getInt("order"));
                JsonObject group = jsonObject.getJsonObject(testGroupId);
                assertNotNull(group);
                assertEquals(testGroupId, group.getString("principal"));
                assertEquals(1, group.getInt("order"));

	}	

	/**
	 * Test to verify adding an ACE at a specific index inside 
	 * the ACL
	 */
	@Test 
	public void testAddAceOrderByNumeric() throws IOException, JsonException {
		createAceOrderTestFolderWithOneAce();
		
		testGroupId = H.createTestGroup();
		addOrUpdateAce(testFolderUrl, testGroupId, true, "0");

		//fetch the JSON for the acl to verify the settings.
		String getUrl = testFolderUrl + ".acl.json";

		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		String json = H.getAuthenticatedContent(creds, getUrl, HttpTest.CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json);

		
                JsonObject jsonObject = JsonUtil.parseObject(json);
                assertEquals(2, jsonObject.size());

                JsonObject group = jsonObject.getJsonObject(testGroupId);
                assertNotNull(group);
                assertEquals(testGroupId, group.getString("principal"));
                assertEquals(0, group.getInt("order"));

                JsonObject user =  jsonObject.getJsonObject(testUserId);
                assertNotNull(user);
                assertEquals(testUserId, user.getString("principal"));
                assertEquals(1, user.getInt("order"));



		//add another principal between the testGroupId and testUserId
		testUserId2 = H.createTestUser();
		addOrUpdateAce(testFolderUrl, testUserId2, true, "1");

		String json2 = H.getAuthenticatedContent(creds, getUrl, HttpTest.CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json2);

                JsonObject jsonObject2 = JsonUtil.parseObject(json2);
                assertEquals(3, jsonObject2.size());

                JsonObject group2 = jsonObject2.getJsonObject(testGroupId);
                assertNotNull(group2);
                assertEquals(testGroupId, group2.getString("principal"));
                assertEquals(0, group2.getInt("order"));

                JsonObject user3 =  jsonObject2.getJsonObject(testUserId2);
                assertNotNull(user3);
                assertEquals(testUserId2, user3.getString("principal"));
                assertEquals(1, user3.getInt("order"));

                JsonObject user2 =  jsonObject2.getJsonObject(testUserId);
                assertNotNull(user2);
                assertEquals(testUserId, user2.getString("principal"));
                assertEquals(2, user2.getInt("order"));

	}	

	/**
	 * Test to make sure modifying an existing ace without changing the order 
	 * leaves the ACE in the same position in the ACL
	 */
	@Test 
	public void testUpdateAcePreservePosition() throws IOException, JsonException {
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
		
                JsonObject jsonObject = JsonUtil.parseObject(json);
                assertEquals(2, jsonObject.size());

                JsonObject group = jsonObject.getJsonObject(testGroupId);
                assertNotNull(group);
                assertEquals(testGroupId, group.getString("principal"));
                assertEquals(0, group.getInt("order"));
                JsonObject user =  jsonObject.getJsonObject(testUserId);
                assertNotNull(user);
                assertEquals(testUserId, user.getString("principal"));
                assertEquals(1, user.getInt("order"));

	}	

	
	/**
	 * Helper to create a test folder with a single ACE pre-created
	 */
	private void createAceOrderTestFolderWithOneAce() throws IOException, JsonException {
		testUserId = H.createTestUser();
		
		testFolderUrl = H.createTestFolder();

		addOrUpdateAce(testFolderUrl, testUserId, true, null);

		//fetch the JSON for the acl to verify the settings.
		String getUrl = testFolderUrl + ".acl.json";

		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		String json = H.getAuthenticatedContent(creds, getUrl, HttpTest.CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json);
		
                JsonObject jsonObject = JsonUtil.parseObject(json);
                assertEquals(1, jsonObject.size());

                JsonObject user = jsonObject.getJsonObject(testUserId);
                assertNotNull(user);
                assertEquals(testUserId, user.getString("principal"));
                assertEquals(0, user.getInt("order"));

	}
	
	/**
	 * Helper to add or update an ace for testing
	 */
	private void addOrUpdateAce(String folderUrl, String principalId, boolean readGranted, String order) throws IOException, JsonException {
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
	public void testModifyAceResponseAsJSON() throws IOException, JsonException {
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
        JsonObject jsonObject = JsonUtil.parseObject(json);
		assertNotNull(jsonObject);
	}
	
	
	/**
	 * Test for SLING-3010
	 */
	@Test 
	public void testMergeAceForUserGrantNestedAggregatePrivilegeAfterDenySuperAggregatePrivilege() throws IOException, JsonException {
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
		
		JsonObject jsonObject = JsonUtil.parseObject(json);
		assertEquals(1, jsonObject.size());
		
		JsonObject aceObject = jsonObject.getJsonObject(testUserId);
		assertNotNull(aceObject);
		
		assertEquals(testUserId, aceObject.getString("principal"));
		
		JsonArray grantedArray = aceObject.getJsonArray("granted");
		assertNotNull(grantedArray);
		assertEquals(4, grantedArray.size());
		Set<String> grantedPrivilegeNames = new HashSet<String>();
		for (int i=0; i < grantedArray.size(); i++) {
			grantedPrivilegeNames.add(grantedArray.getString(i));
		}
		H.assertPrivilege(grantedPrivilegeNames, true, "jcr:versionManagement");
		H.assertPrivilege(grantedPrivilegeNames, true, "jcr:read");
		H.assertPrivilege(grantedPrivilegeNames, true, "jcr:modifyAccessControl");
		H.assertPrivilege(grantedPrivilegeNames, true, "jcr:write");

		JsonArray deniedArray = aceObject.getJsonArray("denied");
		assertNotNull(deniedArray);
		assertEquals(1, deniedArray.size());
		Set<String> deniedPrivilegeNames = new HashSet<String>();
		for (int i=0; i < deniedArray.size(); i++) {
			deniedPrivilegeNames.add(deniedArray.getString(i));
		}
		//the leftovers from the denied rep:write that were not granted with jcr:write
		H.assertPrivilege(deniedPrivilegeNames, true, "jcr:nodeTypeManagement"); 
	}

	/**
	 * Test for SLING-3010
	 */
	@Test 
	public void testMergeAceForUserGrantAggregatePrivilegePartsAfterDenyAggregatePrivilege() throws IOException, JsonException {
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
		
		JsonObject jsonObject = JsonUtil.parseObject(json);
		assertEquals(1, jsonObject.size());
		
		JsonObject aceObject = jsonObject.getJsonObject(testUserId);
		assertNotNull(aceObject);
		
		assertEquals(testUserId, aceObject.getString("principal"));
		
		JsonArray grantedArray = aceObject.getJsonArray("granted");
		assertNotNull(grantedArray);
		Set<String> grantedPrivilegeNames = new HashSet<String>();
		for (int i=0; i < grantedArray.size(); i++) {
			grantedPrivilegeNames.add(grantedArray.getString(i));
		}
		H.assertPrivilege(grantedPrivilegeNames, true, "jcr:versionManagement");
		H.assertPrivilege(grantedPrivilegeNames, true, "jcr:read");
		H.assertPrivilege(grantedPrivilegeNames, true, "jcr:modifyAccessControl");
		H.assertPrivilege(grantedPrivilegeNames, true, "rep:write"); //jcr:nodeTypeManagement + jcr:write
        assertEquals("Expecting the correct number of privileges in " + grantedPrivilegeNames, 4, grantedPrivilegeNames.size());

		//should be nothing left in the denied set.
		Object deniedArray = aceObject.get("denied");
		assertNull(deniedArray);
	}
	
}
