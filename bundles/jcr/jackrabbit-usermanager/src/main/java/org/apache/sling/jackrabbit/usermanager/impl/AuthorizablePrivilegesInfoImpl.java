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
package org.apache.sling.jackrabbit.usermanager.impl;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Dictionary;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Servlet;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.jackrabbit.usermanager.AuthorizablePrivilegesInfo;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class to assist in the usage of access control of users/groups from scripts.
 * 
 * The default access control policy defined by this provider has the following
 * characteristics:
 * <ul>
 * <li>everybody has READ permission to all items,</li>
 *
 * <li>every known user is allowed to modify it's own properties except for
 * her/his group membership,</li>
 *
 * <li>members of the 'User administrator' group are allowed to create, modify
 * and remove users,</li>
 *
 * <li>members of the 'Group administrator' group are allowed to create, modify
 * and remove groups,</li>
 *
 * <li>group membership can only be edited by members of the 'Group administrator'
 * and the 'User administrator' group.</li>
 * </ul>
 * 
 * @scr.component immediate="true" metatype="no"
 * @scr.service
 *
 * @scr.property name="service.description" value="User/Group Privileges Information"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 */
public class AuthorizablePrivilegesInfoImpl implements AuthorizablePrivilegesInfo {

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * The name of the configuration parameter providing the 
     * 'User administrator' group name.
     *
     * @scr.property valueRef="DEFAULT_USER_ADMIN_GROUP_NAME"
     */
    private static final String PAR_USER_ADMIN_GROUP_NAME = "user.admin.group.name";

    /**
     * The default 'User administrator' group name
     *
     * @see #PAR_USER_ADMIN_GROUP_NAME
     */
    private static final String DEFAULT_USER_ADMIN_GROUP_NAME = "UserAdmin";
 
    private String userAdminGroupName = DEFAULT_USER_ADMIN_GROUP_NAME;

    /**
     * The name of the configuration parameter providing the 
     * 'Group administrator' group name.
     *
     * @scr.property valueRef="DEFAULT_GROUP_ADMIN_GROUP_NAME"
     */
    private static final String PAR_GROUP_ADMIN_GROUP_NAME = "group.admin.group.name";

    /**
     * The default 'User administrator' group name
     *
     * @see #PAR_GROUP_ADMIN_GROUP_NAME
     */
    private static final String DEFAULT_GROUP_ADMIN_GROUP_NAME = "GroupAdmin";
 
    private String groupAdminGroupName = DEFAULT_GROUP_ADMIN_GROUP_NAME;
    
    
	/* (non-Javadoc)
	 * @see org.apache.sling.jackrabbit.usermanager.AuthorizablePrivilegesInfo#canAddGroup(javax.jcr.Session)
	 */
	public boolean canAddGroup(Session jcrSession) {
		try {
			UserManager userManager = AccessControlUtil.getUserManager(jcrSession);
			Authorizable currentUser = userManager.getAuthorizable(jcrSession.getUserID());

			if (currentUser != null) {
				if (((User)currentUser).isAdmin()) {
					return true; //admin user has full control
				}
				
				//check if the user is a member of the 'Group administrator' group
				Authorizable groupAdmin = userManager.getAuthorizable(this.groupAdminGroupName);
				if (groupAdmin instanceof Group) {
					boolean isMember = ((Group)groupAdmin).isMember(currentUser);
					if (isMember) {
						return true;
					}
				}
			}
		} catch (RepositoryException e) {
			log.warn("Failed to determine if {} can add a new group", jcrSession.getUserID());
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.apache.sling.jackrabbit.usermanager.AuthorizablePrivilegesInfo#canAddUser(javax.jcr.Session)
	 */
	public boolean canAddUser(Session jcrSession) {
		try {
			//if self-registration is enabled, then anyone can create a user
			if (componentContext != null) {
				String filter = "(&(sling.servlet.resourceTypes=sling/users)(|(sling.servlet.methods=POST)(sling.servlet.selectors=create)))";
				BundleContext bundleContext = componentContext.getBundleContext();
				ServiceReference[] serviceReferences = bundleContext.getServiceReferences(Servlet.class.getName(), filter);
				if (serviceReferences != null) {
					String propName = "self.registration.enabled";
					for (ServiceReference serviceReference : serviceReferences) {
						Object propValue = serviceReference.getProperty(propName);
						if (propValue != null) {
							boolean selfRegEnabled = Boolean.TRUE.equals(propValue);
							if (selfRegEnabled) {
								return true;
							}
							break;
						}
					}
				}
			}

			UserManager userManager = AccessControlUtil.getUserManager(jcrSession);
			Authorizable currentUser = userManager.getAuthorizable(jcrSession.getUserID());
			if (currentUser != null) {
				if (((User)currentUser).isAdmin()) {
					return true; //admin user has full control
				}
				
				//check if the user is a member of the 'User administrator' group
				Authorizable userAdmin = userManager.getAuthorizable(this.userAdminGroupName);
				if (userAdmin instanceof Group) {
					boolean isMember = ((Group)userAdmin).isMember(currentUser);
					if (isMember) {
						return true;
					}
				}
			}
		} catch (RepositoryException e) {
			log.warn("Failed to determine if {} can add a new user", jcrSession.getUserID());
		} catch (InvalidSyntaxException e) {
			log.warn("Failed to determine if {} can add a new user", jcrSession.getUserID());
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.apache.sling.jackrabbit.usermanager.AuthorizablePrivilegesInfo#canRemove(javax.jcr.Session, java.lang.String)
	 */
	public boolean canRemove(Session jcrSession, String principalId) {
		try {
			UserManager userManager = AccessControlUtil.getUserManager(jcrSession);
			Authorizable currentUser = userManager.getAuthorizable(jcrSession.getUserID());

			if (((User)currentUser).isAdmin()) {
				return true; //admin user has full control
			}

			Authorizable authorizable = userManager.getAuthorizable(principalId);
			if (authorizable instanceof User) {
				//check if the user is a member of the 'User administrator' group
				Authorizable userAdmin = userManager.getAuthorizable(this.userAdminGroupName);
				if (userAdmin instanceof Group) {
					boolean isMember = ((Group)userAdmin).isMember(currentUser);
					if (isMember) {
						return true;
					}
				}
			} else if (authorizable instanceof Group) {
				//check if the user is a member of the 'Group administrator' group
				Authorizable groupAdmin = userManager.getAuthorizable(this.groupAdminGroupName);
				if (groupAdmin instanceof Group) {
					boolean isMember = ((Group)groupAdmin).isMember(currentUser);
					if (isMember) {
						return true;
					}
				}
			}
		} catch (RepositoryException e) {
			log.warn("Failed to determine if {} can remove authorizable {}", jcrSession.getUserID(), principalId);
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.apache.sling.jackrabbit.usermanager.AuthorizablePrivilegesInfo#canUpdateGroupMembers(javax.jcr.Session, java.lang.String)
	 */
	public boolean canUpdateGroupMembers(Session jcrSession, String groupId) {
		try {
			UserManager userManager = AccessControlUtil.getUserManager(jcrSession);
			Authorizable currentUser = userManager.getAuthorizable(jcrSession.getUserID());

			if (((User)currentUser).isAdmin()) {
				return true; //admin user has full control
			}

			Authorizable authorizable = userManager.getAuthorizable(groupId);
			if (authorizable instanceof Group) {
				//check if the user is a member of the 'Group administrator' group
				Authorizable groupAdmin = userManager.getAuthorizable(this.groupAdminGroupName);
				if (groupAdmin instanceof Group) {
					boolean isMember = ((Group)groupAdmin).isMember(currentUser);
					if (isMember) {
						return true;
					}
				}
			}
		} catch (RepositoryException e) {
			log.warn("Failed to determine if {} can remove authorizable {}", jcrSession.getUserID(), groupId);
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.apache.sling.jackrabbit.usermanager.AuthorizablePrivilegesInfo#canUpdateProperties(javax.jcr.Session, java.lang.String)
	 */
	public boolean canUpdateProperties(Session jcrSession, String principalId) {
		try {
			if (jcrSession.getUserID().equals(principalId)) {
				//user is allowed to update it's own properties
				return true;
			}
			
			UserManager userManager = AccessControlUtil.getUserManager(jcrSession);
			Authorizable currentUser = userManager.getAuthorizable(jcrSession.getUserID());

			if (((User)currentUser).isAdmin()) {
				return true; //admin user has full control
			}

			Authorizable authorizable = userManager.getAuthorizable(principalId);
			if (authorizable instanceof User) {
				//check if the user is a member of the 'User administrator' group
				Authorizable userAdmin = userManager.getAuthorizable(this.userAdminGroupName);
				if (userAdmin instanceof Group) {
					boolean isMember = ((Group)userAdmin).isMember(currentUser);
					if (isMember) {
						return true;
					}
				}
			} else if (authorizable instanceof Group) {
				//check if the user is a member of the 'Group administrator' group
				Authorizable groupAdmin = userManager.getAuthorizable(this.groupAdminGroupName);
				if (groupAdmin instanceof Group) {
					boolean isMember = ((Group)groupAdmin).isMember(currentUser);
					if (isMember) {
						return true;
					}
				}
			}
		} catch (RepositoryException e) {
			log.warn("Failed to determine if {} can remove authorizable {}", jcrSession.getUserID(), principalId);
		}
		return false;
	}


	// ---------- SCR Integration ----------------------------------------------

	//keep track of the bundle context
	private ComponentContext componentContext;

    /**
     * Called by SCR to activate the component.
     *
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     * @throws IllegalStateException
     * @throws UnsupportedEncodingException
     */
    protected void activate(ComponentContext componentContext)
            throws InvalidKeyException, NoSuchAlgorithmException,
            IllegalStateException, UnsupportedEncodingException {

    	this.componentContext = componentContext;
    	
        Dictionary<?, ?> properties = componentContext.getProperties();

        this.userAdminGroupName = OsgiUtil.toString(properties.get(PAR_USER_ADMIN_GROUP_NAME),
        		DEFAULT_USER_ADMIN_GROUP_NAME);
        log.info("User Admin Group Name {}", this.userAdminGroupName);

        this.groupAdminGroupName = OsgiUtil.toString(properties.get(PAR_GROUP_ADMIN_GROUP_NAME), 
        		DEFAULT_GROUP_ADMIN_GROUP_NAME);
        log.info("Group Admin Group Name {}", this.groupAdminGroupName);
    }

    protected void deactivate(ComponentContext componentContext) {
    	this.userAdminGroupName = DEFAULT_USER_ADMIN_GROUP_NAME;
    	this.groupAdminGroupName = DEFAULT_GROUP_ADMIN_GROUP_NAME;
    }
}
