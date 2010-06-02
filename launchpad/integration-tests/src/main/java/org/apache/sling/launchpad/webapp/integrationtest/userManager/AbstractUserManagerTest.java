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
import org.apache.sling.launchpad.webapp.integrationtest.AbstractAuthenticatedTest;

/**
 * Base class for UserManager tests.
 */
public abstract class AbstractUserManagerTest extends AbstractAuthenticatedTest {

	/**
	 * Helper to assist adding a user to a group
	 * @param testUserId the user
	 * @param testGroupId the group
	 */
	protected void addUserToGroup(String testUserId, String testGroupId) throws IOException {
        String postUrl = HTTP_BASE_URL + "/system/userManager/group/" + testGroupId + ".update.html";

		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair(":member", testUserId));
		
		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);
	}

	/**
	 * Helper to assist removing a user from a group
	 * @param testUserId the user
	 * @param testGroupId the group
	 */
	protected void removeUserFromGroup(String testUserId, String testGroupId) throws IOException {
        String postUrl = HTTP_BASE_URL + "/system/userManager/group/" + testGroupId + ".update.html";

		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair(":member@Delete", testUserId));
		
		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);
	}

	/**
	 * Add test user to the 'UserAdmin' group
	 * @param testUserId the user
	 */
	protected void addUserToUserAdminGroup(String testUserId) throws IOException {
		addUserToGroup(testUserId, "UserAdmin");
	}

	/**
	 * Add test user to the 'GroupAdmin' group
	 * @param testUserId the user
	 */
	protected void addUserToGroupAdminGroup(String testUserId) throws IOException {
		addUserToGroup(testUserId, "GroupAdmin");
	}
	
}
