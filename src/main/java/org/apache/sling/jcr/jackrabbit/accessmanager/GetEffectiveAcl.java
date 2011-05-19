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

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;

/**
 * The <code>GetAcl</code> service api.
 * <p>
 * This interface is not intended to be implemented by bundles. It is
 * implemented by this bundle and may be used by client bundles.
 * </p>
 */
public interface GetEffectiveAcl {

	/**
	 * Gets the effective access control list for a resource.
	 * 
	 * @param jcrSession the JCR session of the user updating the user
	 * @param resourcePath The path of the resource to get the ACL for (required)
     * @return the ACL as a JSON object 
	 * @throws RepositoryException
	 */
	public JSONObject getEffectiveAcl(Session jcrSession,
							String resourcePath
				) throws RepositoryException, JSONException;
	
}
