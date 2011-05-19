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
package org.apache.sling.jcr.jackrabbit.accessmanager;

import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * The <code>ModifyAce</code> service api.
 * <p>
 * This interface is not intended to be implemented by bundles. It is
 * implemented by this bundle and may be used by client bundles.
 * </p>
 */
public interface ModifyAce {

	/**
	 * Add or modify the access control entry for the specified user 
	 * or group.
	 * 
	 * @param jcrSession the JCR session of the user updating the user
	 * @param resourcePath The absolute path of the resource to apply the ACE to (required)
	 * @param principalId The name of the user/group to provision (required)
	 * @param privileges Map of privileges to apply. (optional)
     * @param changes The list of changes for this operation (optional)
     * @return the user that was updated or null if not found 
	 * @throws RepositoryException
	 */
	public void modifyAce(Session jcrSession,
							String resourcePath,
							String principalId,
							Map<String, String> privileges,
							String order
				) throws RepositoryException;
	
}
