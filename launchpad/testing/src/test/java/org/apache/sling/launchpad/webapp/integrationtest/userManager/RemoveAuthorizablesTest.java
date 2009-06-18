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

/**
 * Tests for the 'removeAuthorizable' Sling Post Operation
 */
public class RemoveAuthorizablesTest extends AbstractUserManagerTest {

	public void testRemoveUser() throws IOException {
		String userId = createTestUser();
		
        Credentials creds = new UsernamePasswordCredentials("admin", "admin");

		String getUrl = HTTP_BASE_URL + "/system/userManager/user/" + userId + ".json";
		assertAuthenticatedHttpStatus(creds, getUrl, HttpServletResponse.SC_OK, null); //make sure the profile request returns some data

		String postUrl = HTTP_BASE_URL + "/system/userManager/user/" + userId + ".delete.html";
		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);
		
		getUrl = HTTP_BASE_URL + "/system/userManager/user/" + userId + ".json";
		assertAuthenticatedHttpStatus(creds, getUrl, HttpServletResponse.SC_NOT_FOUND, null); //make sure the profile request returns some data
	}
	
	public void testRemoveGroup() throws IOException {
		String groupId = createTestGroup();
		
        Credentials creds = new UsernamePasswordCredentials("admin", "admin");

		String getUrl = HTTP_BASE_URL + "/system/userManager/group/" + groupId + ".json";
		assertAuthenticatedHttpStatus(creds, getUrl, HttpServletResponse.SC_OK, null); //make sure the profile request returns some data

		String postUrl = HTTP_BASE_URL + "/system/userManager/group/" + groupId + ".delete.html";
		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);
		
		getUrl = HTTP_BASE_URL + "/system/userManager/group/" + groupId + ".json";
		assertAuthenticatedHttpStatus(creds, getUrl, HttpServletResponse.SC_NOT_FOUND, null); //make sure the profile request returns some data
	}

	public void testRemoveAuthorizables() throws IOException {
		String userId = createTestUser();
		String groupId = createTestGroup();
		
        Credentials creds = new UsernamePasswordCredentials("admin", "admin");

		String getUrl = HTTP_BASE_URL + "/system/userManager/user/" + userId + ".json";
		assertAuthenticatedHttpStatus(creds, getUrl, HttpServletResponse.SC_OK, null); //make sure the profile request returns some data

		getUrl = HTTP_BASE_URL + "/system/userManager/group/" + groupId + ".json";
		assertAuthenticatedHttpStatus(creds, getUrl, HttpServletResponse.SC_OK, null); //make sure the profile request returns some data
		
		String postUrl = HTTP_BASE_URL + "/system/userManager.delete.html";
		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair(":applyTo", "group/" + groupId));
		postParams.add(new NameValuePair(":applyTo", "user/" + userId));
		assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);
		
		getUrl = HTTP_BASE_URL + "/system/userManager/user/" + userId + ".json";
		assertAuthenticatedHttpStatus(creds, getUrl, HttpServletResponse.SC_NOT_FOUND, null); //make sure the profile request returns some data

		getUrl = HTTP_BASE_URL + "/system/userManager/group/" + groupId + ".json";
		assertAuthenticatedHttpStatus(creds, getUrl, HttpServletResponse.SC_NOT_FOUND, null); //make sure the profile request returns some data
	}
}
