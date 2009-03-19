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

import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jackrabbit.usermanager.resource.AuthorizableResourceProvider;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostConstants;

/**
 * Base class for servlets manipulating groups
 */
public abstract class AbstractGroupPostServlet extends AbstractAuthorizablePostServlet {
	private static final long serialVersionUID = 1159063041816944076L;

	/**
     * Update the group membership based on the ":member" request
     * parameters.  If the ":member" value ends with @Delete it is removed
     * from the group membership, otherwise it is added to the group membership.
     * 
     * @param request
     * @param authorizable
     * @throws RepositoryException
     */
	protected void updateGroupMembership(SlingHttpServletRequest request,
			Authorizable authorizable, List<Modification> changes) throws RepositoryException {
		if (authorizable.isGroup()) {
			Group group = ((Group)authorizable);
    		String groupPath = AuthorizableResourceProvider.SYSTEM_USER_MANAGER_GROUP_PREFIX + group.getID(); 

	    	ResourceResolver resolver = request.getResourceResolver();
	    	Resource baseResource = request.getResource();
	    	boolean changed = false;

	    	//first remove any members posted as ":member@Delete"
	    	String[] membersToDelete = request.getParameterValues(SlingPostConstants.RP_PREFIX + "member" + SlingPostConstants.SUFFIX_DELETE);
	    	if (membersToDelete != null) {
				for (String member : membersToDelete) {
	                Resource res = resolver.getResource(baseResource, member);
	                if (res != null) {
	                	Authorizable memberAuthorizable = res.adaptTo(Authorizable.class);
	                	if (memberAuthorizable != null) {
	                		group.removeMember(memberAuthorizable);
	                		changed = true;
	                	}
	                }
					
				}
	    	}
	    	
	    	//second add any members posted as ":member"
	    	String[] membersToAdd = request.getParameterValues(SlingPostConstants.RP_PREFIX + "member");
	    	if (membersToAdd != null) {
				for (String member : membersToAdd) {
	                Resource res = resolver.getResource(baseResource, member);
	                if (res != null) {
	                	Authorizable memberAuthorizable = res.adaptTo(Authorizable.class);
	                	if (memberAuthorizable != null) {
	                		group.addMember(memberAuthorizable);
	                		changed = true;
	                	}
	                }
				}
	    	}

	    	if (changed) {
        		//add an entry to the changes list to record the membership change
        		changes.add(Modification.onModified(groupPath + "/members"));
	    	}
		}
	}
	
}
