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
package org.apache.sling.jcr.jackrabbit.accessmanager.post;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.Item;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.servlets.post.Modification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Sling Post Servlet implementation for modifying the ACE for a principal on a JCR
 * resource.
 * </p>
 * <h2>Rest Service Description</h2>
 * <p>
 * Delete a set of Ace's from a node, the node is identified as a resource by the request
 * url &gt;resource&lt;.modifyAce.html
 * </p>
 * <h4>Methods</h4>
 * <ul>
 * <li>POST</li>
 * </ul>
 * <h4>Post Parameters</h4>
 * <dl>
 * <dt>principalId</dt>
 * <dd>The principal of the Ace to modify in the ACL specified by the path.</dd>
 * <dt>privilege@*</dt>
 * <dd>One of more privileges, either granted or denied, where set the permission in the
 * stored ACE is modified to match the request. Any permissions that are present in the
 * stored ACE, but are not in the request are left untouched.</dd>
 * </dl>
 *
 * <h4>Response</h4>
 * <dl>
 * <dt>200</dt>
 * <dd>Success.</dd>
 * <dt>404</dt>
 * <dd>The resource was not found.</dd>
 * <dt>500</dt>
 * <dd>Failure. HTML explains the failure.</dd>
 * </dl>
 *
 * <h4>Notes</h4>
 * <p>
 * The principalId is assumed to refer directly to an Authorizable, that comes direct from
 * the UserManager. This can be a group or a user, but if its a group, denied permissions
 * will not be added to the group. The group will only contain granted privileges.
 * </p>
 *
 * @scr.component immediate="true"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.resourceTypes" value="sling/servlet/default"
 * @scr.property name="sling.servlet.methods" value="POST"
 * @scr.property name="sling.servlet.selectors" value="modifyAce"
 */
public class ModifyAceServlet extends AbstractAccessPostServlet {
	private static final long serialVersionUID = -9182485466670280437L;

	/**
     * default log
     */
    private final Logger log = LoggerFactory.getLogger(getClass());

	/* (non-Javadoc)
	 * @see org.apache.sling.jackrabbit.accessmanager.post.AbstractAccessPostServlet#handleOperation(org.apache.sling.api.SlingHttpServletRequest, org.apache.sling.api.servlets.HtmlResponse, java.util.List)
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected void handleOperation(SlingHttpServletRequest request,
			HtmlResponse htmlResponse, List<Modification> changes)
			throws RepositoryException {
		Session session = request.getResourceResolver().adaptTo(Session.class);
		if (session == null) {
			throw new RepositoryException("JCR Session not found");
		}

		String principalId = request.getParameter("principalId");
		if (principalId == null) {
			throw new RepositoryException("principalId was not submitted.");
		}
		UserManager userManager = AccessControlUtil.getUserManager(session);
		Authorizable authorizable = userManager.getAuthorizable(principalId);
		if (authorizable == null) {
			throw new RepositoryException("No principal found for id: " + principalId);
		}

    	String resourcePath = null;
    	Resource resource = request.getResource();
    	if (resource == null) {
			throw new ResourceNotFoundException("Resource not found.");
    	} else {
    		Item item = resource.adaptTo(Item.class);
    		if (item != null) {
    			resourcePath = item.getPath();
    		} else {
    			throw new ResourceNotFoundException("Resource is not a JCR Node");
    		}
    	}


		List<String> grantedPrivilegeNames = new ArrayList<String>();
		List<String> deniedPrivilegeNames = new ArrayList<String>();
		//SLING-997: keep track of the privilege names that were posted, so the others can be preserved
		Set<String> postedPrivilegeNames = new HashSet<String>();
		Enumeration parameterNames = request.getParameterNames();
		while (parameterNames.hasMoreElements()) {
			Object nextElement = parameterNames.nextElement();
			if (nextElement instanceof String) {
				String paramName = (String)nextElement;
				if (paramName.startsWith("privilege@")) {
					String privilegeName = paramName.substring(10);
					//keep track of which privileges should be changed
					postedPrivilegeNames.add(privilegeName); 
					
					String parameterValue = request.getParameter(paramName);
					if (parameterValue != null && parameterValue.length() > 0) {
						if ("granted".equals(parameterValue)) {
							grantedPrivilegeNames.add(privilegeName);
						} else if ("denied".equals(parameterValue)) {
							deniedPrivilegeNames.add(privilegeName);
						}
					}
				}
			}
		}

		try {
			AccessControlManager accessControlManager = AccessControlUtil.getAccessControlManager(session);
			AccessControlList updatedAcl = getAccessControlList(accessControlManager, resourcePath, true);

			StringBuilder oldPrivileges = null;
			StringBuilder newPrivileges = null;
			if (log.isDebugEnabled()) {
				oldPrivileges = new StringBuilder();
				newPrivileges = new StringBuilder();
			}

			List<Privilege> preserveGrantedPrivileges = new ArrayList<Privilege>();
			List<Privilege> preserveDeniedPrivileges = new ArrayList<Privilege>();
			
			//keep track of the existing Aces for the target principal
			AccessControlEntry[] accessControlEntries = updatedAcl.getAccessControlEntries();
			List<AccessControlEntry> oldAces = new ArrayList<AccessControlEntry>();
			for (AccessControlEntry ace : accessControlEntries) {
				if (principalId.equals(ace.getPrincipal().getName())) {
					if (log.isDebugEnabled()) {
						log.debug("Found Existing ACE for principal {0} on resource: ", new Object[] {principalId, resourcePath});
					}
					oldAces.add(ace);

					boolean isAllow = AccessControlUtil.isAllow(ace);
					Privilege[] privileges = ace.getPrivileges();
					for (Privilege privilege : privileges) {
						String privilegeName = privilege.getName();
						if (!postedPrivilegeNames.contains(privilegeName)) {
							//this privilege was not posted, so record the existing state to be 
							// preserved when the ACE is re-created below
							if (isAllow) {
								preserveGrantedPrivileges.add(privilege);
							} else {
								preserveDeniedPrivileges.add(privilege);
							}
						}

						if (log.isDebugEnabled()) {
							//collect the information for debug logging
							if (oldPrivileges.length() > 0) {
								oldPrivileges.append(", "); //separate entries by commas
							}
						}
					}
				}
			}

			//remove the old aces
			if (!oldAces.isEmpty()) {
				for (AccessControlEntry ace : oldAces) {
					updatedAcl.removeAccessControlEntry(ace);
				}
			}

			//add a fresh ACE with the granted privileges
			List<Privilege> grantedPrivilegeList = new ArrayList<Privilege>();
			for (String name : grantedPrivilegeNames) {
				if (name.length() == 0) {
					continue; //empty, skip it.
				}
				Privilege privilege = accessControlManager.privilegeFromName(name);
				grantedPrivilegeList.add(privilege);
			}
			//add the privileges that should be preserved
			grantedPrivilegeList.addAll(preserveGrantedPrivileges);

			if (log.isDebugEnabled()) {
				for (Privilege privilege : grantedPrivilegeList) {
					if (newPrivileges.length() > 0) {
						newPrivileges.append(", "); //separate entries by commas
					}
					newPrivileges.append("granted=");
					newPrivileges.append(privilege.getName());
				}				
			}

			if (grantedPrivilegeList.size() > 0) {
				Principal principal = authorizable.getPrincipal();
				updatedAcl.addAccessControlEntry(principal, grantedPrivilegeList.toArray(new Privilege[grantedPrivilegeList.size()]));
			}

			//if the authorizable is a user (not a group) process any denied privileges
			if (!authorizable.isGroup()) {
				//add a fresh ACE with the denied privileges
				List<Privilege> deniedPrivilegeList = new ArrayList<Privilege>();
				for (String name : deniedPrivilegeNames) {
					if (name.length() == 0) {
						continue; //empty, skip it.
					}
					Privilege privilege = accessControlManager.privilegeFromName(name);
					deniedPrivilegeList.add(privilege);
				}
				//add the privileges that should be preserved
				deniedPrivilegeList.addAll(preserveDeniedPrivileges);
				
				if (log.isDebugEnabled()) {
					for (Privilege privilege : deniedPrivilegeList) {
						if (newPrivileges.length() > 0) {
							newPrivileges.append(", "); //separate entries by commas
						}
						newPrivileges.append("denied=");
						newPrivileges.append(privilege.getName());
					}
				}
				
				if (deniedPrivilegeList.size() > 0) {
					Principal principal = authorizable.getPrincipal();
					AccessControlUtil.addEntry(updatedAcl, principal, deniedPrivilegeList.toArray(new Privilege[deniedPrivilegeList.size()]), false);
				}
			}

			accessControlManager.setPolicy(resourcePath, updatedAcl);
			if (session.hasPendingChanges()) {
				session.save();
			}

			if (log.isDebugEnabled()) {
				log.debug("Updated ACE for principalId {0} for resource {1) from {2} to {3}", new Object [] {
						authorizable.getID(), resourcePath, oldPrivileges.toString(), newPrivileges.toString()
				});
			}
		} catch (RepositoryException re) {
			throw new RepositoryException("Failed to create ace.", re);
		}
	}
}
