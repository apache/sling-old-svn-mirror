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

import java.util.Dictionary;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.jackrabbit.usermanager.post.impl.RequestProperty;
import org.apache.sling.jackrabbit.usermanager.resource.AuthorizableResourceProvider;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sling Post Servlet implementation for creating a user in the jackrabbit
 * UserManager.
 * 
 * @scr.component immediate="true" label="%createUser.post.operation.name"
 *                description="%createUser.post.operation.description"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.resourceTypes" value="sling/users"
 * @scr.property name="sling.servlet.methods" value="POST" 
 * @scr.property name="sling.servlet.selectors" value="create" 
 */
public class CreateUserServlet extends AbstractUserPostServlet {
	private static final long serialVersionUID = 6871481922737658675L;

	/**
     * default log
     */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /** @scr.property label="%self.registration.enabled.name" 
     * 					description="%self.registration.enabled.description" 
     * 					valueRef="DEFAULT_SELF_REGISTRATION_ENABLED" 
     */
    private static final String PROP_SELF_REGISTRATION_ENABLED = "self.registration.enabled";
    private static final Boolean DEFAULT_SELF_REGISTRATION_ENABLED = Boolean.TRUE;

    private Boolean selfRegistrationEnabled = DEFAULT_SELF_REGISTRATION_ENABLED;

    /**
     * The JCR Repository we access to resolve resources
     *
     * @scr.reference
     */
    private SlingRepository repository;

    /** Returns the JCR repository used by this service. */
    protected SlingRepository getRepository() {
        return repository;
    }

    /**
     * Returns an administrative session to the default workspace.
     */
    private Session getSession() throws RepositoryException {
        return getRepository().loginAdministrative(null);
    }

    /**
     * Return the administrative session and close it.
     */
    private void ungetSession(final Session session) {
        if ( session != null ) {
            try {
                session.logout();
            } catch (Throwable t) {
                log.error("Unable to log out of session: " + t.getMessage(), t);
            }
        }
    }

    // ---------- SCR integration ---------------------------------------------

    /**
     * Activates this component.
     *
     * @param componentContext The OSGi <code>ComponentContext</code> of this
     *      component.
     */
    protected void activate(ComponentContext componentContext) {
    	super.activate(componentContext);
        Dictionary<?, ?> props = componentContext.getProperties();
        Object propValue = props.get(PROP_SELF_REGISTRATION_ENABLED);
        if (propValue instanceof String) {
        	selfRegistrationEnabled = Boolean.parseBoolean((String)propValue);
        } else {
        	selfRegistrationEnabled = DEFAULT_SELF_REGISTRATION_ENABLED;
        }
    }

    

	/* (non-Javadoc)
	 * @see org.apache.sling.jackrabbit.usermanager.post.AbstractAuthorizablePostServlet#handleOperation(org.apache.sling.api.SlingHttpServletRequest, org.apache.sling.api.servlets.HtmlResponse, java.util.List)
	 */
	@Override
	protected void handleOperation(SlingHttpServletRequest request,
			HtmlResponse response, List<Modification> changes) throws RepositoryException {
		//make sure user self-registration is enabled
		if (!selfRegistrationEnabled) {
			throw new RepositoryException("Sorry, registration of new users is not currently enabled.  Please try again later.");
		}

		Session session = request.getResourceResolver().adaptTo(Session.class);
		if (session == null) {
			throw new RepositoryException("JCR Session not found");
		}
		
		//check that the submitted parameter values have valid values.
		String principalName = request.getParameter(SlingPostConstants.RP_NODE_NAME);
		if (principalName == null) {
			throw new RepositoryException("User name was not submitted");
		}
		String pwd = request.getParameter("pwd");
		if (pwd == null) {
			throw new RepositoryException("Password was not submitted");
		}
		String pwdConfirm = request.getParameter("pwdConfirm");
		if (!pwd.equals(pwdConfirm)) {
			throw new RepositoryException("Password value does not match the confirmation password");
		}
		
		Session selfRegSession = null;
		try {
			selfRegSession = getSession();

			UserManager userManager = AccessControlUtil.getUserManager(selfRegSession);
			Authorizable authorizable = userManager.getAuthorizable(principalName);
			
			if (authorizable != null) {
				//user already exists!
				throw new RepositoryException("A principal already exists with the requested name: " + principalName);
			} else {
				Map<String, RequestProperty> reqProperties = collectContent(request, response);

				User user = userManager.createUser(principalName, digestPassword(pwd));
				String userPath = AuthorizableResourceProvider.SYSTEM_USER_MANAGER_USER_PREFIX + user.getID();
				
				response.setPath(userPath);
				response.setLocation(externalizePath(request, userPath));
				response.setParentLocation(externalizePath(request, AuthorizableResourceProvider.SYSTEM_USER_MANAGER_USER_PATH));
				changes.add(Modification.onCreated(userPath));
				
		        // write content from form
		        writeContent(selfRegSession, user, reqProperties, changes);
				
				if (selfRegSession.hasPendingChanges()) {
					selfRegSession.save();
				}
			}
		} finally {
			ungetSession(selfRegSession);
		}
	}
}
