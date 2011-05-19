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

/**
 * The <code>DeleteAces</code> service api.
 * <p>
 * This interface is not intended to be implemented by bundles. It is
 * implemented by this bundle and may be used by client bundles.
 * </p>
 */
public interface DeleteAces {

	/**
	 * Deletes one or more ACEs from the access control list of a resource.
	 * 
	 * @param jcrSession the JCR session of the user updating the user
	 * @param resourcePath The path of the resource to update the ACL for (required)
	 * @param principalNamesToDelete An array of ace principal names to delete.. (required)
	 * @throws RepositoryException
	 */
	public void deleteAces(Session jcrSession,
							String resourcePath,
							String [] principalNamesToDelete
				) throws RepositoryException;
	
}
