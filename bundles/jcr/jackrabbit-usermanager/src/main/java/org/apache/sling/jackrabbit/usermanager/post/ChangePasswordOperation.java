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
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.servlets.post.Modification;

/**
 * Sling Post Operation implementation for updating the password of
 * a user in the jackrabbit UserManager.
 * 
 * @scr.component metatype="no" immediate="true"
 * @scr.service interface="org.apache.sling.servlets.post.SlingPostOperation"
 * @scr.property name="sling.post.operation" value="changePassword"
 */
public class ChangePasswordOperation extends AbstractAuthorizableOperation {

	/* (non-Javadoc)
	 * @see org.apache.sling.servlets.post.AbstractSlingPostOperation#doRun(org.apache.sling.api.SlingHttpServletRequest, org.apache.sling.api.servlets.HtmlResponse, java.util.List)
	 */
	@Override
	protected void doRun(SlingHttpServletRequest request,
			HtmlResponse response, List<Modification> changes)
			throws RepositoryException {
		Authorizable authorizable = null;
		Resource resource = request.getResource();
		if (resource != null) {
			authorizable = resource.adaptTo(Authorizable.class);
		}
		
		//check that the user was located.
		if (authorizable == null || authorizable.isGroup()) {
			throw new RepositoryException("User to update could not be determined.");
		}

		if ("anonymous".equals(authorizable.getID())) {
			throw new RepositoryException("Can not change the password of the anonymous user.");
		}

		Session session = request.getResourceResolver().adaptTo(Session.class);
		if (session == null) {
			throw new RepositoryException("JCR Session not found");
		}

		//check that the submitted parameter values have valid values.
		String oldPwd = request.getParameter("oldPwd");
		if (oldPwd == null || oldPwd.length() == 0) {
			throw new RepositoryException("Old Password was not submitted");
		}
		String newPwd = request.getParameter("newPwd");
		if (newPwd == null || newPwd.length() == 0) {
			throw new RepositoryException("New Password was not submitted");
		}
		String newPwdConfirm = request.getParameter("newPwdConfirm");
		if (!newPwd.equals(newPwdConfirm)) {
			throw new RepositoryException("New Password does not match the confirmation password");
		}
		
		try {
			String digestedOldPwd = digestPassword(oldPwd);
			Value[] pwdProperty = ((User)authorizable).getProperty("rep:password");
			if (pwdProperty != null && pwdProperty.length > 0) {
				String repPasswordValue = pwdProperty[0].getString();
				if (!digestedOldPwd.equals(repPasswordValue)) {
					//submitted oldPwd value is not correct.
					throw new RepositoryException("Old Password does not match");
				}
			}
				
			((User)authorizable).changePassword(digestPassword(newPwd));
			
            changes.add(Modification.onModified(
                	resource.getPath() + "/rep:password"
                ));
		} catch (RepositoryException re) {
			throw new RepositoryException("Failed to change user password.", re);
		}
	}
}
