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

import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.sling.launchpad.webapp.integrationtest.util.JsonUtil;

/**
 * Tests for the 'updateAuthorizable' Sling Post Operation on
 * a group resource.
 */
public class UpdateGroupTest extends UserManagerTestUtil {

	String testGroupId = null;

	String testUserId = null;

	@Override
	public void tearDown() throws Exception {
        if (testUserId != null) {
            //remove the test user if it exists.
            String postUrl = HTTP_BASE_URL + "/system/userManager/user/" + testUserId + ".delete.html";
            List<NameValuePair> postParams = new ArrayList<NameValuePair>();
            assertAuthenticatedAdminPostStatus(postUrl, HttpServletResponse.SC_OK, postParams, null);
        }

		if (testGroupId != null) {
			//remove the test group if it exists.
			String postUrl = HTTP_BASE_URL + "/system/userManager/group/" + testGroupId + ".delete.html";
			List<NameValuePair> postParams = new ArrayList<NameValuePair>();
			assertAuthenticatedAdminPostStatus(postUrl, HttpServletResponse.SC_OK, postParams, null);
		}

		super.tearDown();
	}

	public void testUpdateGroup() throws IOException, JsonException {
		testGroupId = createTestGroup();

        String postUrl = HTTP_BASE_URL + "/system/userManager/group/" + testGroupId + ".update.html";

		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair("displayName", "My Updated Test Group"));
		postParams.add(new NameValuePair("url", "http://www.apache.org/updated"));

		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);

		//fetch the user profile json to verify the settings
		String getUrl = HTTP_BASE_URL + "/system/userManager/group/" + testGroupId + ".json";
		assertAuthenticatedHttpStatus(creds, getUrl, HttpServletResponse.SC_OK, null); //make sure the profile request returns some data
		String json = getAuthenticatedContent(creds, getUrl, CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
		assertNotNull(json);
		JsonObject jsonObj = JsonUtil.parseObject(json);
		assertEquals("My Updated Test Group", jsonObj.getString("displayName"));
		assertEquals("http://www.apache.org/updated", jsonObj.getString("url"));
	}

	public void testUpdateGroupMembers() throws IOException, JsonException {
		testGroupId = createTestGroup();
		testUserId = createTestUser();

        Credentials creds = new UsernamePasswordCredentials("admin", "admin");

		// verify that the members array exists, but is empty
		JsonArray members = getTestGroupMembers(creds);
        assertEquals(0, members.size());

        JsonArray memberships = getTestUserMemberships(creds);
        assertEquals(0, memberships.size());

        String postUrl = HTTP_BASE_URL + "/system/userManager/group/" + testGroupId + ".update.html";

        // add a group member
		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair(":member", testUserId));
        assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);

        members = getTestGroupMembers(creds);
        assertEquals(1, members.size());
        assertEquals("/system/userManager/user/" + testUserId, members.getString(0));

        memberships = getTestUserMemberships(creds);
        assertEquals(1, memberships.size());
        assertEquals("/system/userManager/group/" + testGroupId, memberships.getString(0));

        // delete a group member
		postParams.clear();
		postParams.add(new NameValuePair(":member@Delete", testUserId));
        assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);

        members = getTestGroupMembers(creds);
        assertEquals(0, members.size());

        memberships = getTestUserMemberships(creds);
        assertEquals(0, memberships.size());

	}

	JsonArray getTestUserMemberships(Credentials creds) throws IOException, JsonException {
	    String getUrl = HTTP_BASE_URL + "/system/userManager/user/" + testUserId + ".json";
        assertAuthenticatedHttpStatus(creds, getUrl, HttpServletResponse.SC_OK, null); //make sure the profile request returns some data
        String json = getAuthenticatedContent(creds, getUrl, CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
        assertNotNull(json);
        JsonObject jsonObj = JsonUtil.parseObject(json);
        JsonArray memberships = jsonObj.getJsonArray("memberOf");
        return memberships;
    }

    JsonArray getTestGroupMembers(Credentials creds) throws IOException, JsonException {
        String getUrl = HTTP_BASE_URL + "/system/userManager/group/" + testGroupId + ".json";
		assertAuthenticatedHttpStatus(creds, getUrl, HttpServletResponse.SC_OK, null); //make sure the profile request returns some data
        String json = getAuthenticatedContent(creds, getUrl, CONTENT_TYPE_JSON, null, HttpServletResponse.SC_OK);
        assertNotNull(json);
        JsonObject jsonObj = JsonUtil.parseObject(json);
        JsonArray members = jsonObj.getJsonArray("members");
        return members;
    }

	/**
	 * Test for SLING-1677
	 */
	public void testUpdateGroupResponseAsJSON() throws IOException, JsonException {
		testGroupId = createTestGroup();

        String postUrl = HTTP_BASE_URL + "/system/userManager/group/" + testGroupId + ".update.json";

		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair("displayName", "My Updated Test Group"));
		postParams.add(new NameValuePair("url", "http://www.apache.org/updated"));

		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		String json = getAuthenticatedPostContent(creds, postUrl, CONTENT_TYPE_JSON, postParams, HttpServletResponse.SC_OK);

		//make sure the json response can be parsed as a JSON object
		JsonObject jsonObj = JsonUtil.parseObject(json);
		assertNotNull(jsonObj);
	}	
}

