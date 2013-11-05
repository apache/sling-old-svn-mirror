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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.testing.integration.HttpTest;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AccessPrivilegesInfoTest {
    private static Random random = new Random(System.currentTimeMillis());
	
	String testUserId = null;
	String testGroupId = null;
	String testFolderUrl = null;
    Set<String> toDelete = new HashSet<String>();
    
    private final AccessManagerTestUtil H = new AccessManagerTestUtil();
	
	@Before
	public void setup() throws Exception {
		H.setUp();

        // Script for server-side PrivilegeInfo calculations
        String scriptPath = "/apps/nt/unstructured";
        H.getTestClient().mkdirs(HttpTest.WEBDAV_BASE_URL, scriptPath);
        toDelete.add(H.uploadTestScript(scriptPath,
        				"accessmanager/privileges-info.json.esp",
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
		
        for(String script : toDelete) {
            H.getTestClient().delete(script);
        }
	}
	
	/*
	 * testuser granted read / denied write
	 */
	@Test 
	public void testDeniedWriteForUser() throws IOException, JSONException {
		testUserId = H.createTestUser();
		testFolderUrl = H.createTestFolder();
		
		//assign some privileges
        String postUrl = testFolderUrl + ".modifyAce.html";

		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair("principalId", testUserId));
		postParams.add(new NameValuePair("privilege@jcr:read", "granted"));
		postParams.add(new NameValuePair("privilege@jcr:readAccessControl", "granted"));
		postParams.add(new NameValuePair("privilege@jcr:write", "denied"));
		
		Credentials adminCreds = new UsernamePasswordCredentials("admin", "admin");
		H.assertAuthenticatedPostStatus(adminCreds, postUrl, HttpServletResponse.SC_OK, postParams, null);
		
		String getUrl = testFolderUrl + ".privileges-info.json";

		//fetch the JSON for the test page to verify the settings.
		Credentials testUserCreds = new UsernamePasswordCredentials(testUserId, "testPwd");

		String json = H.getAuthenticatedContent(testUserCreds, getUrl, HttpTest.CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json);
		JSONObject jsonObj = new JSONObject(json);
		
		assertEquals(false, jsonObj.getBoolean("canAddChildren"));
		assertEquals(false, jsonObj.getBoolean("canDeleteChildren"));
		assertEquals(false, jsonObj.getBoolean("canDelete"));
		assertEquals(false, jsonObj.getBoolean("canModifyProperties"));
		assertEquals(true, jsonObj.getBoolean("canReadAccessControl"));
		assertEquals(false, jsonObj.getBoolean("canModifyAccessControl"));
	}

	/*
	 * testuser granted read / granted write
	 */
	@Test 
	public void testGrantedWriteForUser() throws IOException, JSONException {
		testUserId = H.createTestUser();
		testFolderUrl = H.createTestFolder();
		
		//assign some privileges
        String postUrl = testFolderUrl + ".modifyAce.html";

		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair("principalId", testUserId));
		postParams.add(new NameValuePair("privilege@jcr:read", "granted"));
		postParams.add(new NameValuePair("privilege@jcr:write", "granted"));
		postParams.add(new NameValuePair("privilege@jcr:readAccessControl", "granted"));
		postParams.add(new NameValuePair("privilege@jcr:modifyAccessControl", "granted"));
		
		Credentials adminCreds = new UsernamePasswordCredentials("admin", "admin");
		H.assertAuthenticatedPostStatus(adminCreds, postUrl, HttpServletResponse.SC_OK, postParams, null);

		String getUrl = testFolderUrl + ".privileges-info.json";

		//fetch the JSON for the test page to verify the settings.
		Credentials testUserCreds = new UsernamePasswordCredentials(testUserId, "testPwd");

		String json = H.getAuthenticatedContent(testUserCreds, getUrl, HttpTest.CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json);
		JSONObject jsonObj = new JSONObject(json);
		
		assertEquals(true, jsonObj.getBoolean("canAddChildren"));
		assertEquals(true, jsonObj.getBoolean("canDeleteChildren"));
		//the parent node must also have jcr:removeChildren granted for 'canDelete' to be true
		assertEquals(false, jsonObj.getBoolean("canDelete"));  
		assertEquals(true, jsonObj.getBoolean("canModifyProperties"));
		assertEquals(true, jsonObj.getBoolean("canReadAccessControl"));
		assertEquals(true, jsonObj.getBoolean("canModifyAccessControl"));
		
		//add a child node to verify the 'canDelete' use case
        String childFolderUrl = H.getTestClient().createNode(testFolderUrl + "/testFolder" + random.nextInt() + SlingPostConstants.DEFAULT_CREATE_SUFFIX, null);
        String childPostUrl = childFolderUrl + ".modifyAce.html";

		postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair("principalId", testUserId));
		postParams.add(new NameValuePair("privilege@jcr:read", "granted"));
		postParams.add(new NameValuePair("privilege@jcr:removeNode", "granted"));
		H.assertAuthenticatedPostStatus(adminCreds, childPostUrl, HttpServletResponse.SC_OK, postParams, null);
		
		String childGetUrl = childFolderUrl + ".privileges-info.json";
		String childJson = H.getAuthenticatedContent(testUserCreds, childGetUrl, HttpTest.CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(childJson);
		JSONObject childJsonObj = new JSONObject(childJson);
		assertEquals(true, childJsonObj.getBoolean("canDelete"));
	}

	
	
	/*
	 * group testuser granted read / denied write
	 */
	@Test 
	public void testDeniedWriteForGroup() throws IOException, JSONException {
		testGroupId = H.createTestGroup();
		testUserId = H.createTestUser();
		testFolderUrl = H.createTestFolder();

		Credentials adminCreds = new UsernamePasswordCredentials("admin", "admin");

		//add testUserId to testGroup
        String groupPostUrl = HttpTest.HTTP_BASE_URL + "/system/userManager/group/" + testGroupId + ".update.html";
		List<NameValuePair> groupPostParams = new ArrayList<NameValuePair>();
		groupPostParams.add(new NameValuePair(":member", testUserId));
		H.assertAuthenticatedPostStatus(adminCreds, groupPostUrl, HttpServletResponse.SC_OK, groupPostParams, null);
		
		//assign some privileges
        String postUrl = testFolderUrl + ".modifyAce.html";

		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair("principalId", testGroupId));
		postParams.add(new NameValuePair("privilege@jcr:read", "granted"));
		postParams.add(new NameValuePair("privilege@jcr:readAccessControl", "granted"));
		postParams.add(new NameValuePair("privilege@jcr:write", "denied"));
		
		H.assertAuthenticatedPostStatus(adminCreds, postUrl, HttpServletResponse.SC_OK, postParams, null);
		
		String getUrl = testFolderUrl + ".privileges-info.json";

		//fetch the JSON for the test page to verify the settings.
		Credentials testUserCreds = new UsernamePasswordCredentials(testUserId, "testPwd");

		String json = H.getAuthenticatedContent(testUserCreds, getUrl, HttpTest.CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json);
		JSONObject jsonObj = new JSONObject(json);
		
		assertEquals(false, jsonObj.getBoolean("canAddChildren"));
		assertEquals(false, jsonObj.getBoolean("canDeleteChildren"));
		assertEquals(false, jsonObj.getBoolean("canDelete"));
		assertEquals(false, jsonObj.getBoolean("canModifyProperties"));
		assertEquals(true, jsonObj.getBoolean("canReadAccessControl"));
		assertEquals(false, jsonObj.getBoolean("canModifyAccessControl"));
	}

	/*
	 * group testuser granted read / granted write
	 */
	@Test 
	public void testGrantedWriteForGroup() throws IOException, JSONException {
		testGroupId = H.createTestGroup();
		testUserId = H.createTestUser();
		testFolderUrl = H.createTestFolder();

		Credentials adminCreds = new UsernamePasswordCredentials("admin", "admin");

		//add testUserId to testGroup
        String groupPostUrl = HttpTest.HTTP_BASE_URL + "/system/userManager/group/" + testGroupId + ".update.html";
		List<NameValuePair> groupPostParams = new ArrayList<NameValuePair>();
		groupPostParams.add(new NameValuePair(":member", testUserId));
		H.assertAuthenticatedPostStatus(adminCreds, groupPostUrl, HttpServletResponse.SC_OK, groupPostParams, null);

		//assign some privileges
        String postUrl = testFolderUrl + ".modifyAce.html";

		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair("principalId", testGroupId));
		postParams.add(new NameValuePair("privilege@jcr:read", "granted"));
		postParams.add(new NameValuePair("privilege@jcr:write", "granted"));
		postParams.add(new NameValuePair("privilege@jcr:readAccessControl", "granted"));
		postParams.add(new NameValuePair("privilege@jcr:modifyAccessControl", "granted"));
		
		H.assertAuthenticatedPostStatus(adminCreds, postUrl, HttpServletResponse.SC_OK, postParams, null);

		String getUrl = testFolderUrl + ".privileges-info.json";

		//fetch the JSON for the test page to verify the settings.
		Credentials testUserCreds = new UsernamePasswordCredentials(testUserId, "testPwd");

		String json = H.getAuthenticatedContent(testUserCreds, getUrl, HttpTest.CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json);
		JSONObject jsonObj = new JSONObject(json);
		
		assertEquals(true, jsonObj.getBoolean("canAddChildren"));
		assertEquals(true, jsonObj.getBoolean("canDeleteChildren"));
		//the parent node must also have jcr:removeChildren granted for 'canDelete' to be true
		assertEquals(false, jsonObj.getBoolean("canDelete"));
		assertEquals(true, jsonObj.getBoolean("canModifyProperties"));
		assertEquals(true, jsonObj.getBoolean("canReadAccessControl"));
		assertEquals(true, jsonObj.getBoolean("canModifyAccessControl"));
		

		//add a child node to verify the 'canDelete' use case
        String childFolderUrl = H.getTestClient().createNode(testFolderUrl + "/testFolder" + random.nextInt() + SlingPostConstants.DEFAULT_CREATE_SUFFIX, null);
        String childPostUrl = childFolderUrl + ".modifyAce.html";

		postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair("principalId", testGroupId));
		postParams.add(new NameValuePair("privilege@jcr:read", "granted"));
		postParams.add(new NameValuePair("privilege@jcr:removeNode", "granted"));
		H.assertAuthenticatedPostStatus(adminCreds, childPostUrl, HttpServletResponse.SC_OK, postParams, null);
		
		String childGetUrl = childFolderUrl + ".privileges-info.json";
		String childJson = H.getAuthenticatedContent(testUserCreds, childGetUrl, HttpTest.CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(childJson);
		JSONObject childJsonObj = new JSONObject(childJson);
		assertEquals(true, childJsonObj.getBoolean("canDelete"));
	}
	

	/**
	 * Test the fix for SLING-1090
	 */
	@Test 
	public void testSLING_1090() throws Exception {
		testUserId = H.createTestUser();

        //grant jcr: removeChildNodes to the root node
        ArrayList<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair("principalId", testUserId));
		postParams.add(new NameValuePair("privilege@jcr:read", "granted"));
		postParams.add(new NameValuePair("privilege@jcr:removeChildNodes", "granted"));
		Credentials adminCreds = new UsernamePasswordCredentials("admin", "admin");
		H.assertAuthenticatedPostStatus(adminCreds, HttpTest.HTTP_BASE_URL + "/.modifyAce.html", HttpServletResponse.SC_OK, postParams, null);

		//create a node as a child of the root folder
		testFolderUrl = H.getTestClient().createNode(HttpTest.HTTP_BASE_URL + "/testFolder" + random.nextInt() + SlingPostConstants.DEFAULT_CREATE_SUFFIX, null);
        String postUrl = testFolderUrl + ".modifyAce.html";
        
        //grant jcr:removeNode to the test node
        postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair("principalId", testUserId));
		postParams.add(new NameValuePair("privilege@jcr:read", "granted"));
		postParams.add(new NameValuePair("privilege@jcr:removeNode", "granted"));
		H.assertAuthenticatedPostStatus(adminCreds, postUrl, HttpServletResponse.SC_OK, postParams, null);
		
		//fetch the JSON for the test page to verify the settings.
		String getUrl = testFolderUrl + ".privileges-info.json";
		Credentials testUserCreds = new UsernamePasswordCredentials(testUserId, "testPwd");
		String json = H.getAuthenticatedContent(testUserCreds, getUrl, HttpTest.CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json);
		JSONObject jsonObj = new JSONObject(json);
		assertEquals(true, jsonObj.getBoolean("canDelete"));
	}
}
