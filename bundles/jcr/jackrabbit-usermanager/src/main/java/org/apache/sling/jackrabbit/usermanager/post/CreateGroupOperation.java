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
package org.apache.sling.jackrabbit.usermanager.post;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.jackrabbit.usermanager.post.impl.RequestProperty;
import org.apache.sling.jackrabbit.usermanager.resource.AuthorizableResourceProvider;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostConstants;

/**
 * Sling Post Operation implementation for creating a group in the jackrabbit
 * UserManager.
 *
 * @scr.component metatype="no" immediate="true"
 * @scr.service interface="org.apache.sling.servlets.post.SlingPostOperation"
 * @scr.property name="sling.post.operation" value="createGroup"
 */
public class CreateGroupOperation extends AbstractAuthorizableOperation {

	/* (non-Javadoc)
	 * @see org.apache.sling.servlets.post.AbstractSlingPostOperation#doRun(org.apache.sling.api.SlingHttpServletRequest, org.apache.sling.api.servlets.HtmlResponse, java.util.List)
	 */
	@Override
	protected void doRun(SlingHttpServletRequest request,
			HtmlResponse response, List<Modification> changes)
			throws RepositoryException {
		Session session = request.getResourceResolver().adaptTo(Session.class);
		if (session == null) {
			throw new RepositoryException("JCR Session not found");
		}
		
		//check that the submitted parameter values have valid values.
		final String principalName = request.getParameter(SlingPostConstants.RP_NODE_NAME);
		if (principalName == null) {
			throw new RepositoryException("Group name was not submitted");
		}
		
		try {
			UserManager userManager = AccessControlUtil.getUserManager(session);
			Authorizable authorizable = userManager.getAuthorizable(principalName);
			
			if (authorizable != null) {
				//principal already exists!
				throw new RepositoryException("A principal already exists with the requested name: " + principalName);
			} else {
				Map<String, RequestProperty> reqProperties = collectContent(request, response);

				Group group = userManager.createGroup(new Principal() {
					public String getName() {
						return principalName;
					}
				});

				String groupPath = AuthorizableResourceProvider.SYSTEM_USER_MANAGER_GROUP_PREFIX + group.getID();
				response.setPath(groupPath);
				response.setLocation(externalizePath(request, groupPath));
				response.setParentLocation(externalizePath(request, AuthorizableResourceProvider.SYSTEM_USER_MANAGER_GROUP_PATH));
				changes.add(Modification.onCreated(groupPath));
				
		        // write content from form
		        writeContent(session, group, reqProperties, changes);
		        
		        //update the group memberships
		        updateGroupMembership(request, group, changes);
			}
		} catch (RepositoryException re) {
			throw new RepositoryException("Failed to create new group.", re);
		}
	}
}
