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
import org.apache.sling.launchpad.webapp.integrationtest.AuthenticatedTestUtil;

/**
 * Base class for UserManager tests. - it's called "Util" now
 * as we're moving tests to JUnit4-style which won't extend
 * this anymore - but right now some still do.
 */
public class UserManagerTestUtil extends AuthenticatedTestUtil {

	/**
	 * Helper to assist adding a user to a group
	 * @param testUserId the user
	 * @param testGroupId the group
	 */
	public void addUserToGroup(String testUserId, String testGroupId) throws IOException {
        String postUrl = HTTP_BASE_URL + "/system/userManager/group/" + testGroupId + ".update.html";

		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair(":member", testUserId));
		
		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		final String info = "Adding user " + testUserId + " to group via " + postUrl;
		assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, info);
	}

	/**
	 * Helper to assist removing a user from a group
	 * @param testUserId the user
	 * @param testGroupId the group
	 */
	public void removeUserFromGroup(String testUserId, String testGroupId) throws IOException {
        String postUrl = HTTP_BASE_URL + "/system/userManager/group/" + testGroupId + ".update.html";

		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair(":member@Delete", testUserId));
		
		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
        final String info = "Removing user " + testUserId + " from group via " + postUrl;
		assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, info);
	}

	/**
	 * Add test user to the 'UserAdmin' group
	 * @param testUserId the user
	 */
	public void addUserToUserAdminGroup(String testUserId) throws IOException {
		addUserToGroup(testUserId, "UserAdmin");
	}

	/**
	 * Add test user to the 'GroupAdmin' group
	 * @param testUserId the user
	 */
	public void addUserToGroupAdminGroup(String testUserId) throws IOException {
		addUserToGroup(testUserId, "GroupAdmin");
	}
	
}
