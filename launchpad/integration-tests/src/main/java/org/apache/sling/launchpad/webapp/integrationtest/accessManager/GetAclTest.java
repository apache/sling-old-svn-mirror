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
import static org.junit.Assert.assertTrue;

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
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests for the 'acl' and 'eacl' Sling Get Operation
 */
public class GetAclTest {

	String testUserId = null;
	String testUserId2 = null;
	
	private final AccessManagerTestUtil H = new AccessManagerTestUtil();  
	
	@Before
	public void setup() throws Exception {
	    H.setUp();
	}
	
	@After
	public void cleanup() throws Exception {
		H.tearDown();

		Credentials creds = new UsernamePasswordCredentials("admin", "admin");

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
	
	/**
	 * Test for SLING-2600, Effective ACL servlet returns incorrect information
	 */
	@Test 
	public void testEffectiveAclForUser() throws IOException, JsonException {
		testUserId = H.createTestUser();
		testUserId2 = H.createTestUser();
		
		String testFolderUrl = H.createTestFolder("{ \"jcr:primaryType\": \"nt:unstructured\", \"propOne\" : \"propOneValue\", \"child\" : { \"childPropOne\" : true } }");
		
        String postUrl = testFolderUrl + ".modifyAce.html";

        //1. create an initial set of privileges
		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair("principalId", testUserId));
		postParams.add(new NameValuePair("privilege@jcr:write", "granted"));
		
		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		H.assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);
		
		postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair("principalId", testUserId2));
		postParams.add(new NameValuePair("privilege@jcr:write", "granted"));
		
		H.assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);
		
		postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair("principalId", testUserId2));
		postParams.add(new NameValuePair("privilege@jcr:lockManagement", "granted"));
		
        postUrl = testFolderUrl + "/child.modifyAce.html";
		H.assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);

		
		//fetch the JSON for the eacl to verify the settings.
		String getUrl = testFolderUrl + "/child.eacl.json";

		String json = H.getAuthenticatedContent(creds, getUrl, HttpTest.CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json);
		JsonObject jsonObject = JsonUtil.parseObject(json);
		
		JsonObject aceObject = jsonObject.getJsonObject(testUserId);
		assertNotNull(aceObject);

		String principalString = aceObject.getString("principal");
		assertEquals(testUserId, principalString);
		
		JsonArray grantedArray = aceObject.getJsonArray("granted");
		assertNotNull(grantedArray);
		assertEquals(1, grantedArray.size());
		Set<String> grantedPrivilegeNames = new HashSet<String>();
		for (int i=0; i < grantedArray.size(); i++) {
			grantedPrivilegeNames.add(grantedArray.getString(i));
		}
		H.assertPrivilege(grantedPrivilegeNames,true,"jcr:write");

		Object deniedArray = aceObject.get("denied");
		assertNull(deniedArray);

		JsonObject aceObject2 = jsonObject.getJsonObject(testUserId2);
		assertNotNull(aceObject2);

		String principalString2 = aceObject2.getString("principal");
		assertEquals(testUserId2, principalString2);
		
		JsonArray grantedArray2 = aceObject2.getJsonArray("granted");
		assertNotNull(grantedArray2);
		assertEquals(2, grantedArray2.size());
		Set<String> grantedPrivilegeNames2 = new HashSet<String>();
		for (int i=0; i < grantedArray2.size(); i++) {
			grantedPrivilegeNames2.add(grantedArray2.getString(i));
		}
		H.assertPrivilege(grantedPrivilegeNames2, true, "jcr:write");
		H.assertPrivilege(grantedPrivilegeNames2, true, "jcr:lockManagement");

		Object deniedArray2 = aceObject2.get("denied");
		assertNull(deniedArray2);
	
	}

	/**
	 * Test for SLING-2600, Effective ACL servlet returns incorrect information
	 */
	@Test 
	public void testEffectiveAclMergeForUser_ReplacePrivilegeOnChild() throws IOException, JsonException {
		testUserId = H.createTestUser();
		
		String testFolderUrl = H.createTestFolder("{ \"jcr:primaryType\": \"nt:unstructured\", \"propOne\" : \"propOneValue\", \"child\" : { \"childPropOne\" : true } }");
		
        String postUrl = testFolderUrl + ".modifyAce.html";

        //1. create an initial set of privileges
		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair("principalId", testUserId));
		postParams.add(new NameValuePair("privilege@jcr:write", "denied"));
		
		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		H.assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);
		
		H.assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);
		
		postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair("principalId", testUserId));
		postParams.add(new NameValuePair("privilege@jcr:write", "granted"));
		
        postUrl = testFolderUrl + "/child.modifyAce.html";
		H.assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);

		
		//fetch the JSON for the eacl to verify the settings.
		String getUrl = testFolderUrl + "/child.eacl.json";

		String json = H.getAuthenticatedContent(creds, getUrl, HttpTest.CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json);
		JsonObject jsonObject = JsonUtil.parseObject(json);
		
		JsonObject aceObject = jsonObject.getJsonObject(testUserId);
		assertNotNull(aceObject);

		String principalString = aceObject.getString("principal");
		assertEquals(testUserId, principalString);
		
		JsonArray grantedArray = aceObject.getJsonArray("granted");
		assertNotNull(grantedArray);
		assertEquals(1, grantedArray.size());
		Set<String> grantedPrivilegeNames = new HashSet<String>();
		for (int i=0; i < grantedArray.size(); i++) {
			grantedPrivilegeNames.add(grantedArray.getString(i));
		}
		H.assertPrivilege(grantedPrivilegeNames,true,"jcr:write");

		Object deniedArray = aceObject.get("denied");
		assertNull(deniedArray);
	}
	
	/**
	 * Test for SLING-2600, Effective ACL servlet returns incorrect information
	 */
	@Test 
	public void testEffectiveAclMergeForUser_FewerPrivilegesGrantedOnChild() throws IOException, JsonException {
		testUserId = H.createTestUser();
		
		String testFolderUrl = H.createTestFolder("{ \"jcr:primaryType\": \"nt:unstructured\", \"propOne\" : \"propOneValue\", \"child\" : { \"childPropOne\" : true } }");
		
        String postUrl = testFolderUrl + ".modifyAce.html";

        //1. create an initial set of privileges
		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair("principalId", testUserId));
		postParams.add(new NameValuePair("privilege@jcr:all", "granted"));
		
		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		H.assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);
		
		H.assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);
		
		postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair("principalId", testUserId));
		postParams.add(new NameValuePair("privilege@jcr:write", "granted"));
		
        postUrl = testFolderUrl + "/child.modifyAce.html";
		H.assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);

		
		//fetch the JSON for the eacl to verify the settings.
		String getUrl = testFolderUrl + "/child.eacl.json";

		String json = H.getAuthenticatedContent(creds, getUrl, HttpTest.CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json);
		JsonObject jsonObject = JsonUtil.parseObject(json);
		
		JsonObject aceObject = jsonObject.getJsonObject(testUserId);
		assertNotNull(aceObject);

		String principalString = aceObject.getString("principal");
		assertEquals(testUserId, principalString);
		
		JsonArray grantedArray = aceObject.getJsonArray("granted");
		assertNotNull(grantedArray);
		assertEquals(1, grantedArray.size());
		Set<String> grantedPrivilegeNames = new HashSet<String>();
		for (int i=0; i < grantedArray.size(); i++) {
			grantedPrivilegeNames.add(grantedArray.getString(i));
		}
		H.assertPrivilege(grantedPrivilegeNames,true,"jcr:all");

		Object deniedArray = aceObject.get("denied");
		assertNull(deniedArray);
	}

	/**
	 * Test for SLING-2600, Effective ACL servlet returns incorrect information
	 */
	@Test 
	public void testEffectiveAclMergeForUser_MorePrivilegesGrantedOnChild() throws IOException, JsonException {
		testUserId = H.createTestUser();
		
		String testFolderUrl = H.createTestFolder("{ \"jcr:primaryType\": \"nt:unstructured\", \"propOne\" : \"propOneValue\", \"child\" : { \"childPropOne\" : true } }");
		
        String postUrl = testFolderUrl + ".modifyAce.html";

        //1. create an initial set of privileges
		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair("principalId", testUserId));
		postParams.add(new NameValuePair("privilege@jcr:write", "granted"));
		
		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		H.assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);
		
		H.assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);
		
		postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair("principalId", testUserId));
		postParams.add(new NameValuePair("privilege@jcr:all", "granted"));
		
        postUrl = testFolderUrl + "/child.modifyAce.html";
		H.assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);

		
		//fetch the JSON for the eacl to verify the settings.
		String getUrl = testFolderUrl + "/child.eacl.json";

		String json = H.getAuthenticatedContent(creds, getUrl, HttpTest.CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json);
		JsonObject jsonObject = JsonUtil.parseObject(json);
		
		JsonObject aceObject = jsonObject.getJsonObject(testUserId);
		assertNotNull(aceObject);

		String principalString = aceObject.getString("principal");
		assertEquals(testUserId, principalString);
		
		JsonArray grantedArray = aceObject.getJsonArray("granted");
		assertNotNull(grantedArray);
		assertEquals(1, grantedArray.size());
		Set<String> grantedPrivilegeNames = new HashSet<String>();
		for (int i=0; i < grantedArray.size(); i++) {
			grantedPrivilegeNames.add(grantedArray.getString(i));
		}
		H.assertPrivilege(grantedPrivilegeNames,true,"jcr:all");

		Object deniedArray = aceObject.get("denied");
		assertNull(deniedArray);
	}

	/**
	 * Test for SLING-2600, Effective ACL servlet returns incorrect information
	 */
	@Test
	@Ignore // TODO: fails on Oak
	public void testEffectiveAclMergeForUser_SubsetOfPrivilegesDeniedOnChild() throws IOException, JsonException {
		testUserId = H.createTestUser();
		
		String testFolderUrl = H.createTestFolder("{ \"jcr:primaryType\": \"nt:unstructured\", \"propOne\" : \"propOneValue\", \"child\" : { \"childPropOne\" : true } }");
		
        String postUrl = testFolderUrl + ".modifyAce.html";

        //1. create an initial set of privileges
		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair("principalId", testUserId));
		postParams.add(new NameValuePair("privilege@jcr:all", "granted"));
		
		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		H.assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);
		
		H.assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);
		
		postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair("principalId", testUserId));
		postParams.add(new NameValuePair("privilege@jcr:write", "denied"));
		
        postUrl = testFolderUrl + "/child.modifyAce.html";
		H.assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);

		
		//fetch the JSON for the eacl to verify the settings.
		String getUrl = testFolderUrl + "/child.eacl.json";

		String json = H.getAuthenticatedContent(creds, getUrl, HttpTest.CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json);
		JsonObject jsonObject = JsonUtil.parseObject(json);
		
		JsonObject aceObject = jsonObject.getJsonObject(testUserId);
		assertNotNull(aceObject);

		String principalString = aceObject.getString("principal");
		assertEquals(testUserId, principalString);
		
		JsonArray grantedArray = aceObject.getJsonArray("granted");
		assertNotNull(grantedArray);
		assertTrue(grantedArray.size() >= 8);
		Set<String> grantedPrivilegeNames = new HashSet<String>();
		for (int i=0; i < grantedArray.size(); i++) {
			grantedPrivilegeNames.add(grantedArray.getString(i));
		}
		H.assertPrivilege(grantedPrivilegeNames,false,"jcr:all");
		H.assertPrivilege(grantedPrivilegeNames,false,"jcr:write");
		H.assertPrivilege(grantedPrivilegeNames,true,"jcr:read");
		H.assertPrivilege(grantedPrivilegeNames,true,"jcr:readAccessControl");
		H.assertPrivilege(grantedPrivilegeNames,true,"jcr:modifyAccessControl");
		H.assertPrivilege(grantedPrivilegeNames,true,"jcr:lockManagement");
		H.assertPrivilege(grantedPrivilegeNames,true,"jcr:versionManagement");
		H.assertPrivilege(grantedPrivilegeNames,true,"jcr:nodeTypeManagement");
		H.assertPrivilege(grantedPrivilegeNames,true,"jcr:retentionManagement");
		H.assertPrivilege(grantedPrivilegeNames,true,"jcr:lifecycleManagement");
		//jcr:write aggregate privileges should be denied
		H.assertPrivilege(grantedPrivilegeNames,false,"jcr:modifyProperties");
		H.assertPrivilege(grantedPrivilegeNames,false,"jcr:addChildNodes");
		H.assertPrivilege(grantedPrivilegeNames,false,"jcr:removeNode");
		H.assertPrivilege(grantedPrivilegeNames,false,"jcr:removeChildNodes");
		
		
		JsonArray deniedArray = aceObject.getJsonArray("denied");
		assertNotNull(deniedArray);
		assertEquals(1, deniedArray.size());
		Set<String> deniedPrivilegeNames = new HashSet<String>();
		for (int i=0; i < deniedArray.size(); i++) {
			deniedPrivilegeNames.add(deniedArray.getString(i));
		}
		H.assertPrivilege(deniedPrivilegeNames, true, "jcr:write");
	}

	/**
	 * Test for SLING-2600, Effective ACL servlet returns incorrect information
	 */
	@Test 
	public void testEffectiveAclMergeForUser_SubsetOfPrivilegesDeniedOnChild2() throws IOException, JsonException {
		testUserId = H.createTestUser();
		
		String testFolderUrl = H.createTestFolder("{ \"jcr:primaryType\": \"nt:unstructured\", \"propOne\" : \"propOneValue\", \"child\" : { \"childPropOne\" : true } }");
		
        String postUrl = testFolderUrl + ".modifyAce.html";

        //1. create an initial set of privileges
		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair("principalId", testUserId));
		postParams.add(new NameValuePair("privilege@jcr:all", "granted"));
		
		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		H.assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);
		
		H.assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);
		
		postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair("principalId", testUserId));
		postParams.add(new NameValuePair("privilege@jcr:removeNode", "denied"));
		
        postUrl = testFolderUrl + "/child.modifyAce.html";
		H.assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);

		
		//fetch the JSON for the eacl to verify the settings.
		String getUrl = testFolderUrl + "/child.eacl.json";

		String json = H.getAuthenticatedContent(creds, getUrl, HttpTest.CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json);
		JsonObject jsonObject = JsonUtil.parseObject(json);
		
		JsonObject aceObject = jsonObject.getJsonObject(testUserId);
		assertNotNull(aceObject);

		String principalString = aceObject.getString("principal");
		assertEquals(testUserId, principalString);
		
		JsonArray grantedArray = aceObject.getJsonArray("granted");
		assertNotNull(grantedArray);
		assertTrue(grantedArray.size() >= 11);
		Set<String> grantedPrivilegeNames = new HashSet<String>();
		for (int i=0; i < grantedArray.size(); i++) {
			grantedPrivilegeNames.add(grantedArray.getString(i));
		}
		H.assertPrivilege(grantedPrivilegeNames,false,"jcr:all");
		H.assertPrivilege(grantedPrivilegeNames,false,"jcr:write");
		H.assertPrivilege(grantedPrivilegeNames,true,"jcr:read");
		H.assertPrivilege(grantedPrivilegeNames,true,"jcr:readAccessControl");
		H.assertPrivilege(grantedPrivilegeNames,true,"jcr:modifyAccessControl");
		H.assertPrivilege(grantedPrivilegeNames,true,"jcr:lockManagement");
		H.assertPrivilege(grantedPrivilegeNames,true,"jcr:versionManagement");
		H.assertPrivilege(grantedPrivilegeNames,true,"jcr:nodeTypeManagement");
		H.assertPrivilege(grantedPrivilegeNames,true,"jcr:retentionManagement");
		H.assertPrivilege(grantedPrivilegeNames,true,"jcr:lifecycleManagement");
		H.assertPrivilege(grantedPrivilegeNames,true,"jcr:modifyProperties");
		H.assertPrivilege(grantedPrivilegeNames,true,"jcr:addChildNodes");
		H.assertPrivilege(grantedPrivilegeNames,true,"jcr:removeChildNodes");

		JsonArray deniedArray = aceObject.getJsonArray("denied");
		assertNotNull(deniedArray);
		assertEquals(1, deniedArray.size());
		Set<String> deniedPrivilegeNames = new HashSet<String>();
		for (int i=0; i < deniedArray.size(); i++) {
			deniedPrivilegeNames.add(deniedArray.getString(i));
		}
		H.assertPrivilege(deniedPrivilegeNames, true, "jcr:removeNode");
	}

	/**
	 * Test for SLING-2600, Effective ACL servlet returns incorrect information
	 */
	@Test 
	public void testEffectiveAclMergeForUser_SupersetOfPrivilegesDeniedOnChild() throws IOException, JsonException {
		testUserId = H.createTestUser();
		
		String testFolderUrl = H.createTestFolder("{ \"jcr:primaryType\": \"nt:unstructured\", \"propOne\" : \"propOneValue\", \"child\" : { \"childPropOne\" : true } }");
		
        String postUrl = testFolderUrl + ".modifyAce.html";

        //1. create an initial set of privileges
		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair("principalId", testUserId));
		postParams.add(new NameValuePair("privilege@jcr:write", "granted"));
		
		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		H.assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);
		
		H.assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);
		
		postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair("principalId", testUserId));
		postParams.add(new NameValuePair("privilege@jcr:all", "denied"));
		
        postUrl = testFolderUrl + "/child.modifyAce.html";
		H.assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);

		
		//fetch the JSON for the eacl to verify the settings.
		String getUrl = testFolderUrl + "/child.eacl.json";

		String json = H.getAuthenticatedContent(creds, getUrl, HttpTest.CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json);
		JsonObject jsonObject = JsonUtil.parseObject(json);
		
		JsonObject aceObject = jsonObject.getJsonObject(testUserId);
		assertNotNull(aceObject);

		String principalString = aceObject.getString("principal");
		assertEquals(testUserId, principalString);
		
		Object grantedArray = aceObject.get("granted");
		assertNull(grantedArray);
		
		JsonArray deniedArray = aceObject.getJsonArray("denied");
		assertNotNull(deniedArray);
		assertEquals(1, deniedArray.size());
		Set<String> deniedPrivilegeNames = new HashSet<String>();
		for (int i=0; i < deniedArray.size(); i++) {
			deniedPrivilegeNames.add(deniedArray.getString(i));
		}
		H.assertPrivilege(deniedPrivilegeNames, true, "jcr:all");
	}

	/**
	 * Test for SLING-2600, Effective ACL servlet returns incorrect information
	 */
	@Test 
	public void testEffectiveAclMergeForUser_SupersetOfPrivilegesDeniedOnChild2() throws IOException, JsonException {
		testUserId = H.createTestUser();
		
		String testFolderUrl = H.createTestFolder("{ \"jcr:primaryType\": \"nt:unstructured\", \"propOne\" : \"propOneValue\", \"child\" : { \"childPropOne\" : true } }");
		
        String postUrl = testFolderUrl + ".modifyAce.html";

        //1. create an initial set of privileges
		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair("principalId", testUserId));
		postParams.add(new NameValuePair("privilege@jcr:modifyProperties", "granted"));
		
		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		H.assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);
		
		H.assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);
		
		postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair("principalId", testUserId));
		postParams.add(new NameValuePair("privilege@jcr:all", "denied"));
		
        postUrl = testFolderUrl + "/child.modifyAce.html";
		H.assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);

		
		//fetch the JSON for the eacl to verify the settings.
		String getUrl = testFolderUrl + "/child.eacl.json";

		String json = H.getAuthenticatedContent(creds, getUrl, HttpTest.CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json);
		JsonObject jsonObject = JsonUtil.parseObject(json);
		
		JsonObject aceObject = jsonObject.getJsonObject(testUserId);
		assertNotNull(aceObject);

		String principalString = aceObject.getString("principal");
		assertEquals(testUserId, principalString);
		
		Object grantedArray = aceObject.get("granted");
		assertNull(grantedArray);
		
		JsonArray deniedArray = aceObject.getJsonArray("denied");
		assertNotNull(deniedArray);
		assertEquals(1, deniedArray.size());
		Set<String> deniedPrivilegeNames = new HashSet<String>();
		for (int i=0; i < deniedArray.size(); i++) {
			deniedPrivilegeNames.add(deniedArray.getString(i));
		}
		H.assertPrivilege(deniedPrivilegeNames, true, "jcr:all");
	}
}
