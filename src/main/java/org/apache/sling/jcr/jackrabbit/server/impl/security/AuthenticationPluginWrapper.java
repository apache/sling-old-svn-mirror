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
package org.apache.sling.jcr.jackrabbit.server.impl.security;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.security.authentication.Authentication;
import org.apache.sling.jcr.jackrabbit.server.security.AuthenticationPlugin;
import org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin;

/**
 * Wraps a {@link AuthenticationPlugin} and a {@link LoginModulePlugin} with a 
 * {@link org.apache.jackrabbit.core.security.authentication.Authentication} object 
 *
 */
public class AuthenticationPluginWrapper implements Authentication {
	
	private AuthenticationPlugin auth;
	private LoginModulePlugin module;
	
	public AuthenticationPluginWrapper(AuthenticationPlugin pa, LoginModulePlugin plm) {
		this.auth = pa;
		this.module = plm;
	}
	
	/**
	 * Delegates to underlying {@link AuthenticationPlugin#authenticate(Credentials)} method
	 */
	public boolean authenticate(Credentials credentials)
			throws RepositoryException {
		return auth.authenticate(credentials);
	}
	
	/**
	 * Delegates to underlying {@link LoginModulePlugin#canHandle(Credentials)} method
	 */
	public boolean canHandle(Credentials credentials) {
		return module.canHandle(credentials);
	}
}
