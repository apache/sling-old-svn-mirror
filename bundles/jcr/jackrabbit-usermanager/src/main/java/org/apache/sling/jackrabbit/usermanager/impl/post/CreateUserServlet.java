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
import org.apache.sling.servlets.post.impl.helper.RequestProperty;
import org.apache.sling.jackrabbit.usermanager.impl.resource.AuthorizableResourceProvider;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Sling Post Servlet implementation for creating a user in the jackrabbit UserManager.
 * </p>
 * <h2>Rest Service Description</h2>
 * <p>
 * Creates a new user. Maps on to nodes of resourceType <code>sling/users</code> like
 * <code>/rep:system/rep:userManager/rep:users</code> mapped to a resource url
 * <code>/system/userManager/user</code>. This servlet responds at <code>/system/userManager/user.create.html</code>
 * </p>
 * <h4>Methods</h4>
 * <ul>
 * <li>POST</li>
 * </ul>
 * <h4>Post Parameters</h4>
 * <dl>
 * <dt>:name</dt>
 * <dd>The name of the new user (required)</dd>
 * <dt>:pwd</dt>
 * <dd>The password of the new user (required)</dd>
 * <dt>:pwdConfirm</dt>
 * <dd>The password of the new user (required)</dd>
 * <dt>*</dt>
 * <dd>Any additional parameters become properties of the user node (optional)</dd>
 * </dl>
 * <h4>Response</h4>
 * <dl>
 * <dt>200</dt>
 * <dd>Success, a redirect is sent to the users resource locator. The redirect comes with
 * HTML describing the status.</dd>
 * <dt>500</dt>
 * <dd>Failure, including user already exists. HTML explains the failure.</dd>
 * </dl>
 * <h4>Example</h4>
 *
 * <code>
 * curl -F:name=ieb -Fpwd=password -FpwdConfirm=password -Fproperty1=value1 http://localhost:8080/system/userManager/user.create.html
 * </code>
 *
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

    /**
     * @scr.property label="%self.registration.enabled.name"
     *               description="%self.registration.enabled.description"
     *               valueRef="DEFAULT_SELF_REGISTRATION_ENABLED"
     */
    private static final String PROP_SELF_REGISTRATION_ENABLED = "self.registration.enabled";

    private static final Boolean DEFAULT_SELF_REGISTRATION_ENABLED = Boolean.FALSE;

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
        if (session != null) {
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
     *            component.
     */
    protected void activate(ComponentContext componentContext) {
        super.activate(componentContext);
        Dictionary<?, ?> props = componentContext.getProperties();
        Object propValue = props.get(PROP_SELF_REGISTRATION_ENABLED);
        if (propValue instanceof Boolean) {
        	selfRegistrationEnabled = (Boolean)propValue;
        } else if (propValue instanceof String) {
            selfRegistrationEnabled = Boolean.parseBoolean((String) propValue);
        } else {
            selfRegistrationEnabled = DEFAULT_SELF_REGISTRATION_ENABLED;
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * org.apache.sling.jackrabbit.usermanager.post.AbstractAuthorizablePostServlet
     * #handleOperation(org.apache.sling.api.SlingHttpServletRequest,
     * org.apache.sling.api.servlets.HtmlResponse, java.util.List)
     */
    @Override
    protected void handleOperation(SlingHttpServletRequest request,
            HtmlResponse response, List<Modification> changes)
            throws RepositoryException {
      
        // check for an administrator
        boolean administrator = false;
        try {
            Session currentSession = request.getResourceResolver().adaptTo(Session.class);
            UserManager um = AccessControlUtil.getUserManager(currentSession);
            User currentUser = (User) um.getAuthorizable(currentSession.getUserID());
            administrator = currentUser.isAdmin();
        } catch ( Exception ex ) {
            log.warn("Failed to determin if the user is an admin, assuming not. Cause: "+ex.getMessage());
            administrator = false;
        }
            
      
        // make sure user self-registration is enabled
        if (!administrator && !selfRegistrationEnabled) {
            throw new RepositoryException(
                "Sorry, registration of new users is not currently enabled.  Please try again later.");
        }

        Session session = request.getResourceResolver().adaptTo(Session.class);
        if (session == null) {
            throw new RepositoryException("JCR Session not found");
        }

        // check that the submitted parameter values have valid values.
        String principalName = request.getParameter(SlingPostConstants.RP_NODE_NAME);
        if (principalName == null || principalName.length() == 0) {
            throw new RepositoryException("User name was not submitted");
        }
        String pwd = request.getParameter("pwd");
        if (pwd == null) {
            throw new RepositoryException("Password was not submitted");
        }
        String pwdConfirm = request.getParameter("pwdConfirm");
        if (!pwd.equals(pwdConfirm)) {
            throw new RepositoryException(
                "Password value does not match the confirmation password");
        }

        Session selfRegSession = null;
        try {
            selfRegSession = getSession();

            UserManager userManager = AccessControlUtil.getUserManager(selfRegSession);
            Authorizable authorizable = userManager.getAuthorizable(principalName);

            if (authorizable != null) {
                // user already exists!
                throw new RepositoryException(
                    "A principal already exists with the requested name: "
                        + principalName);
            } else {
                User user = userManager.createUser(principalName,
                    digestPassword(pwd));
                String userPath = AuthorizableResourceProvider.SYSTEM_USER_MANAGER_USER_PREFIX
                    + user.getID();
                
                Map<String, RequestProperty> reqProperties = collectContent(
                    request, response, userPath);

                response.setPath(userPath);
                response.setLocation(externalizePath(request, userPath));
                response.setParentLocation(externalizePath(request,
                    AuthorizableResourceProvider.SYSTEM_USER_MANAGER_USER_PATH));
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
