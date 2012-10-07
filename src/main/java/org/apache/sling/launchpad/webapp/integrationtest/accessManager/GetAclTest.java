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
 * Tests for the 'acl' and 'eacl' Sling Get Operation
 */
public class GetAclTest extends AbstractAccessManagerTest {

	String testUserId = null;
	String testUserId2 = null;
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();

		Credentials creds = new UsernamePasswordCredentials("admin", "admin");

		if (testUserId != null) {
			//remove the test user if it exists.
			String postUrl = HTTP_BASE_URL + "/system/userManager/user/" + testUserId + ".delete.html";
			List<NameValuePair> postParams = new ArrayList<NameValuePair>();
			assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);
		}
		if (testUserId2 != null) {
			//remove the test user if it exists.
			String postUrl = HTTP_BASE_URL + "/system/userManager/user/" + testUserId2 + ".delete.html";
			List<NameValuePair> postParams = new ArrayList<NameValuePair>();
			assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);
		}
	}
	
	/**
	 * Test for SLING-2600, Effective ACL servlet returns incorrect information
	 */
	public void testEffectiveAclForUser() throws IOException, JSONException {
		testUserId = createTestUser();
		testUserId2 = createTestUser();
		
		String testFolderUrl = createTestFolder("{ 'jcr:primaryType': 'nt:unstructured', 'propOne' : 'propOneValue', 'child' : { 'childPropOne' : true } }");
		
        String postUrl = testFolderUrl + ".modifyAce.html";

        //1. create an initial set of privileges
		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair("principalId", testUserId));
		postParams.add(new NameValuePair("privilege@jcr:write", "granted"));
		
		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);
		
		postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair("principalId", testUserId2));
		postParams.add(new NameValuePair("privilege@jcr:write", "granted"));
		
		assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);
		
		postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair("principalId", testUserId2));
		postParams.add(new NameValuePair("privilege@jcr:lockManagement", "granted"));
		
        postUrl = testFolderUrl + "/child.modifyAce.html";
		assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);

		
		//fetch the JSON for the eacl to verify the settings.
		String getUrl = testFolderUrl + "/child.eacl.json";

		String json = getAuthenticatedContent(creds, getUrl, CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json);
		JSONObject jsonObject = new JSONObject(json);
		
		JSONObject aceObject = jsonObject.optJSONObject(testUserId);
		assertNotNull(aceObject);

		String principalString = aceObject.optString("principal");
		assertEquals(testUserId, principalString);
		
		JSONArray grantedArray = aceObject.optJSONArray("granted");
		assertNotNull(grantedArray);
		assertEquals(1, grantedArray.length());
		Set<String> grantedPrivilegeNames = new HashSet<String>();
		for (int i=0; i < grantedArray.length(); i++) {
			grantedPrivilegeNames.add(grantedArray.getString(i));
		}
		assertTrue(grantedPrivilegeNames.contains("jcr:write"));

		JSONArray deniedArray = aceObject.optJSONArray("denied");
		assertNull(deniedArray);

		JSONObject aceObject2 = jsonObject.optJSONObject(testUserId2);
		assertNotNull(aceObject2);

		String principalString2 = aceObject2.optString("principal");
		assertEquals(testUserId2, principalString2);
		
		JSONArray grantedArray2 = aceObject2.optJSONArray("granted");
		assertNotNull(grantedArray2);
		assertEquals(2, grantedArray2.length());
		Set<String> grantedPrivilegeNames2 = new HashSet<String>();
		for (int i=0; i < grantedArray2.length(); i++) {
			grantedPrivilegeNames2.add(grantedArray2.getString(i));
		}
		assertTrue(grantedPrivilegeNames2.contains("jcr:write"));
		assertTrue(grantedPrivilegeNames2.contains("jcr:lockManagement"));

		JSONArray deniedArray2 = aceObject2.optJSONArray("denied");
		assertNull(deniedArray2);
	
	}
}
