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
package org.apache.sling.jackrabbit.usermanager.impl.post;

import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jackrabbit.usermanager.impl.resource.AuthorizableResourceProvider;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostConstants;

/**
 * Base class for servlets manipulating groups
 */
public abstract class AbstractGroupPostServlet extends
        AbstractAuthorizablePostServlet {
    private static final long serialVersionUID = 1159063041816944076L;

    /**
     * Update the group membership based on the ":member" request parameters. If
     * the ":member" value ends with @Delete it is removed from the group
     * membership, otherwise it is added to the group membership.
     * 
     * @param request
     * @param authorizable
     * @throws RepositoryException
     */
    protected void updateGroupMembership(Resource baseResource,
                                        Map<String, ?> properties,
                                        Authorizable authorizable, 
                                        List<Modification> changes)
            throws RepositoryException {
        if (authorizable.isGroup()) {
            Group group = ((Group) authorizable);
            String groupPath = AuthorizableResourceProvider.SYSTEM_USER_MANAGER_GROUP_PREFIX
                + group.getID();

            ResourceResolver resolver = baseResource.getResourceResolver();
            boolean changed = false;
            
            UserManager userManager = AccessControlUtil.getUserManager(resolver.adaptTo(Session.class));

            // first remove any members posted as ":member@Delete"
            String[] membersToDelete = convertToStringArray(properties.get(SlingPostConstants.RP_PREFIX
                + "member" + SlingPostConstants.SUFFIX_DELETE));
            if (membersToDelete != null) {
                for (String member : membersToDelete) {
                    
                    Authorizable memberAuthorizable = getAuthorizable(baseResource, member,userManager,resolver);
                    if (memberAuthorizable != null) {
                        group.removeMember(memberAuthorizable);
                        changed = true;
                    }

                }
            }

            // second add any members posted as ":member"
            String[] membersToAdd = convertToStringArray(properties.get(SlingPostConstants.RP_PREFIX
                + "member"));
            if (membersToAdd != null) {
                for (String member : membersToAdd) {
                    Authorizable memberAuthorizable = getAuthorizable(baseResource, member,userManager,resolver);
                    if (memberAuthorizable != null) {
                        group.addMember(memberAuthorizable);
                        changed = true;
                    }
                }
            }

            if (changed) {
                // add an entry to the changes list to record the membership
                // change
                changes.add(Modification.onModified(groupPath + "/members"));
            }
        }
    }

    /**
     * Gets the member, assuming its a principal name, failing that it assumes it a path to the resource.
     * @param member the token pointing to the member, either a name or a uri
     * @param userManager the user manager for this request.
     * @param resolver the resource resolver for this request.
     * @return the authorizable, or null if no authorizable was found.
     */
    private Authorizable getAuthorizable(Resource baseResource, 
                                    String member, 
                                    UserManager userManager,
                                    ResourceResolver resolver) {
        Authorizable memberAuthorizable = null;
        try {
            memberAuthorizable = userManager.getAuthorizable(member);
        } catch (RepositoryException e) {
            // if we can't find the members then it may be resolvable as a resource.
        }
        if ( memberAuthorizable == null ) {
            Resource res = resolver.getResource(baseResource, member);
            if (res != null) {
                memberAuthorizable = res.adaptTo(Authorizable.class);
            }
        }
        return memberAuthorizable;
    }

}
