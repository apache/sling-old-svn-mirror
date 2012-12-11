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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.jcr.Item;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Servlet;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.jcr.jackrabbit.accessmanager.ModifyAce;
import org.apache.sling.servlets.post.AbstractPostResponse;
import org.apache.sling.servlets.post.Modification;

/**
 * <p>
 * Sling Post Servlet implementation for modifying the ACEs for a principal on a JCR
 * resource.
 * </p>
 * <h2>Rest Service Description</h2>
 * <p>
 * Modify a principal's ACEs for the node identified as a resource by the request
 * URL &gt;resource&lt;.modifyAce.html
 * </p>
 * <h4>Methods</h4>
 * <ul>
 * <li>POST</li>
 * </ul>
 * <h4>Post Parameters</h4>
 * <dl>
 * <dt>principalId</dt>
 * <dd>The principal of the ACEs to modify in the ACL specified by the path.</dd>
 * <dt>privilege@*</dt>
 * <dd>One or more privileges, either granted or denied or none, which will be applied
 * to (or removed from) the node ACL. Any permissions that are present in an
 * existing ACE for the principal but not in the request are left untouched.</dd>
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
 */
@Component (immediate=true,
		label="%modifyAce.post.operation.name",
		description="%modifyAce.post.operation.description"
)
@Service (value={
		Servlet.class,
		ModifyAce.class
})
@Properties ({
	@Property (name="sling.servlet.resourceTypes", 
			value="sling/servlet/default"),
	@Property (name="sling.servlet.methods", 
			value="POST"),
	@Property (name="sling.servlet.selectors", 
			value="modifyAce")
})
public class ModifyAceServlet extends AbstractAccessPostServlet implements ModifyAce {
	private static final long serialVersionUID = -9182485466670280437L;

	/* (non-Javadoc)
	 * @see org.apache.sling.jackrabbit.accessmanager.post.AbstractAccessPostServlet#handleOperation(org.apache.sling.api.SlingHttpServletRequest, org.apache.sling.api.servlets.HtmlResponse, java.util.List)
	 */
	@Override
	protected void handleOperation(SlingHttpServletRequest request,
			AbstractPostResponse response, List<Modification> changes)
			throws RepositoryException {
		Session session = request.getResourceResolver().adaptTo(Session.class);
    	String resourcePath = request.getResource().getPath();
		String principalId = request.getParameter("principalId");
		Map<String, String> privileges = new HashMap<String, String>();
		Enumeration<?> parameterNames = request.getParameterNames();
		while (parameterNames.hasMoreElements()) {
			Object nextElement = parameterNames.nextElement();
			if (nextElement instanceof String) {
				String paramName = (String)nextElement;
				if (paramName.startsWith("privilege@")) {
					String privilegeName = paramName.substring(10);
					String parameterValue = request.getParameter(paramName);
					privileges.put(privilegeName, parameterValue);
				}
			}
		}
		String order = request.getParameter("order");
    	modifyAce(session, resourcePath, principalId, privileges, order);
	}
	
	/* (non-Javadoc)
	 * @see org.apache.sling.jcr.jackrabbit.accessmanager.ModifyAce#modifyAce(javax.jcr.Session, java.lang.String, java.lang.String, java.util.Map, java.lang.String)
	 */
	public void modifyAce(Session jcrSession, String resourcePath,
			String principalId, Map<String, String> privileges, String order)
			throws RepositoryException {
		if (jcrSession == null) {
			throw new RepositoryException("JCR Session not found");
		}

		if (principalId == null) {
			throw new RepositoryException("principalId was not submitted.");
		}
		PrincipalManager principalManager = AccessControlUtil.getPrincipalManager(jcrSession);
		Principal principal = principalManager.getPrincipal(principalId);
		
    	if (resourcePath == null) {
			throw new ResourceNotFoundException("Resource path was not supplied.");
    	}

		Item item = jcrSession.getItem(resourcePath);
		if (item != null) {
			resourcePath = item.getPath();
		} else {
			throw new ResourceNotFoundException("Resource is not a JCR Node");
		}
		
		// Collect the modified privileges from the request.
		Set<String> grantedPrivilegeNames = new HashSet<String>();
		Set<String> deniedPrivilegeNames = new HashSet<String>();
		Set<String> removedPrivilegeNames = new HashSet<String>();
		Set<Entry<String, String>> entrySet = privileges.entrySet();
		for (Entry<String, String> entry : entrySet) {
			String privilegeName = entry.getKey();
			if (privilegeName.startsWith("privilege@")) {
				privilegeName = privilegeName.substring(10);
			}
			String parameterValue = entry.getValue();
			if (parameterValue != null && parameterValue.length() > 0) {
				if ("granted".equals(parameterValue)) {
					grantedPrivilegeNames.add(privilegeName);
				} else if ("denied".equals(parameterValue)) {
					deniedPrivilegeNames.add(privilegeName);
				} else if ("none".equals(parameterValue)){
					removedPrivilegeNames.add(privilegeName);
				}
			}
		}

		// Make the actual changes.
		try {
			AccessControlUtil.replaceAccessControlEntry(jcrSession, resourcePath, principal,
					grantedPrivilegeNames.toArray(new String[grantedPrivilegeNames.size()]),
					deniedPrivilegeNames.toArray(new String[deniedPrivilegeNames.size()]),
					removedPrivilegeNames.toArray(new String[removedPrivilegeNames.size()]),
					order);
			if (jcrSession.hasPendingChanges()) {
				jcrSession.save();
			}
		} catch (RepositoryException re) {
			throw new RepositoryException("Failed to create ace.", re);
		}
	}
	
}
