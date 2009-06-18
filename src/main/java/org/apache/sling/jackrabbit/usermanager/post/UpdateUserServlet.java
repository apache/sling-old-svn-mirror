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
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.jackrabbit.usermanager.post.impl.RequestProperty;
import org.apache.sling.servlets.post.Modification;

/**
 * Sling Post Operation implementation for updating a user in the 
 * jackrabbit UserManager.
 * 
 * @scr.component metatype="no" immediate="true"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.resourceTypes" value="sling/user"
 * @scr.property name="sling.servlet.methods" value="POST" 
 * @scr.property name="sling.servlet.selectors" value="update" 
 */
public class UpdateUserServlet extends AbstractUserPostServlet {
	private static final long serialVersionUID = 5874621724096106496L;

	/* (non-Javadoc)
	 * @see org.apache.sling.jackrabbit.usermanager.post.AbstractAuthorizablePostServlet#handleOperation(org.apache.sling.api.SlingHttpServletRequest, org.apache.sling.api.servlets.HtmlResponse, java.util.List)
	 */
	@Override
	protected void handleOperation(SlingHttpServletRequest request,
			HtmlResponse htmlResponse, List<Modification> changes)
			throws RepositoryException {
		Authorizable authorizable = null;
		Resource resource = request.getResource();
		if (resource != null) {
			authorizable = resource.adaptTo(Authorizable.class);
		}
		
		//check that the group was located.
		if (authorizable == null) {
			throw new ResourceNotFoundException("User to update could not be determined");
		}

		Session session = request.getResourceResolver().adaptTo(Session.class);
		if (session == null) {
			throw new RepositoryException("JCR Session not found");
		}

		Map<String, RequestProperty> reqProperties = collectContent(request, htmlResponse);
		try {
	        // cleanup any old content (@Delete parameters)
	        processDeletes(authorizable, reqProperties, changes);
				
	        // write content from form
	        writeContent(session, authorizable, reqProperties, changes);

		} catch (RepositoryException re) {
			throw new RepositoryException("Failed to update user.", re);
		}
	}
}
