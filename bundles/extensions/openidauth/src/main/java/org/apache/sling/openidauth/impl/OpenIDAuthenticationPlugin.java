/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.openidauth.impl;

import java.security.Principal;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;

import org.apache.sling.jcr.jackrabbit.server.security.AuthenticationPlugin;
import org.apache.sling.openidauth.OpenIDUserUtil;

import com.dyuproject.openid.OpenIdUser;

public class OpenIDAuthenticationPlugin implements AuthenticationPlugin {

	private Principal principal;
	
	public OpenIDAuthenticationPlugin(Principal p) {
		this.principal = p;
	}
	
	public boolean authenticate(Credentials credentials)
			throws RepositoryException {
		if(credentials instanceof SimpleCredentials) {
			OpenIdUser user = (OpenIdUser)((SimpleCredentials)credentials)
				.getAttribute(OpenIDAuthenticationHandler.class.getName());
			if(user != null) {
				return principal.getName().equals(
						OpenIDUserUtil.getPrincipalName(
								user.getIdentity())) && 
						user.isAuthenticated();
			}
		}
		throw new RepositoryException("Can't authenticate credentials of type: " + credentials.getClass());
	}

}
