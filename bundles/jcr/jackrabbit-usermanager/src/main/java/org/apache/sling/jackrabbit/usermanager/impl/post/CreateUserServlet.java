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
import javax.servlet.Servlet;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.jackrabbit.usermanager.CreateUser;
import org.apache.sling.jackrabbit.usermanager.impl.resource.AuthorizableResourceProvider;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.servlets.post.AbstractPostResponse;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.ModificationType;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.apache.sling.servlets.post.impl.helper.RequestProperty;
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
 */
@Component (immediate=true, metatype=true, inherit=true,
		label="%createUser.post.operation.name",
		description="%createUser.post.operation.description")
@Service(value={
		Servlet.class,
		CreateUser.class
})		
@Properties ({
	@Property (name="sling.servlet.resourceTypes",
			value="sling/users"),
	@Property (name="sling.servlet.methods",
			value="POST"),
	@Property (name="sling.servlet.selectors",
			value="create")
})
public class CreateUserServlet extends AbstractUserPostServlet implements CreateUser {
    private static final long serialVersionUID = 6871481922737658675L;

    /**
     * default log
     */
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final boolean DEFAULT_SELF_REGISTRATION_ENABLED = false;

    @Property (label="%self.registration.enabled.name",
    		description="%self.registration.enabled.description",
    		boolValue=DEFAULT_SELF_REGISTRATION_ENABLED)
    private static final String PROP_SELF_REGISTRATION_ENABLED = "self.registration.enabled";

    private Boolean selfRegistrationEnabled = DEFAULT_SELF_REGISTRATION_ENABLED;

    /**
     * The default 'User administrator' group name
     *
     * @see #PAR_USER_ADMIN_GROUP_NAME
     */
    private static final String DEFAULT_USER_ADMIN_GROUP_NAME = "UserAdmin";
 
    /**
     * The name of the configuration parameter providing the 
     * 'User administrator' group name.
     */
    @Property (value=DEFAULT_USER_ADMIN_GROUP_NAME)
    private static final String PAR_USER_ADMIN_GROUP_NAME = "user.admin.group.name";

    private String userAdminGroupName = DEFAULT_USER_ADMIN_GROUP_NAME;
    
    /**
     * The JCR Repository we access to resolve resources
     */
    @Reference
    private SlingRepository repository;

    /**
     * Returns an administrative session to the default workspace.
     */
    private Session getSession() throws RepositoryException {
        return repository.loginAdministrative(null);
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
        
        this.userAdminGroupName = OsgiUtil.toString(props.get(PAR_USER_ADMIN_GROUP_NAME),
                DEFAULT_USER_ADMIN_GROUP_NAME);
        log.info("User Admin Group Name {}", this.userAdminGroupName);
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
    		AbstractPostResponse response, List<Modification> changes)
            throws RepositoryException {
      

        Session session = request.getResourceResolver().adaptTo(Session.class);
        String principalName = request.getParameter(SlingPostConstants.RP_NODE_NAME);
        User user = createUser(session, 
                            principalName, 
                            request.getParameter("pwd"),
                            request.getParameter("pwdConfirm"),
                            request.getRequestParameterMap(), 
                            changes);
        
        String userPath = null;
        if (user == null) {
            if (changes.size() > 0) {
                Modification modification = changes.get(0);
                if (modification.getType() == ModificationType.CREATE) {
                    userPath = modification.getSource();
                }
            }
        } else {
            userPath = AuthorizableResourceProvider.SYSTEM_USER_MANAGER_USER_PREFIX
                    + user.getID();
        }
        
        if (userPath != null) {
            response.setPath(userPath);
            response.setLocation(externalizePath(request, userPath));
        }
        response.setParentLocation(externalizePath(request,
            AuthorizableResourceProvider.SYSTEM_USER_MANAGER_USER_PATH));
    }
    
    /* (non-Javadoc)
     * @see org.apache.sling.jackrabbit.usermanager.CreateUser#createUser(javax.jcr.Session, java.lang.String, java.lang.String, java.lang.String, java.util.Map, java.util.List)
     */
    public User createUser(Session jcrSession, 
                            String name, 
                            String password,
                            String passwordConfirm, 
                            Map<String, ?> properties,
                            List<Modification> changes) 
            throws RepositoryException {
        
        if (jcrSession == null) {
            throw new RepositoryException("JCR Session not found");
        }

        // check for an administrator
        boolean administrator = false;
        try {
            UserManager um = AccessControlUtil.getUserManager(jcrSession);
            User currentUser = (User) um.getAuthorizable(jcrSession.getUserID());
            administrator = currentUser.isAdmin();
            
            if (!administrator) {
                //check if the user is a member of the 'User administrator' group
                Authorizable userAdmin = um.getAuthorizable(this.userAdminGroupName);
                if (userAdmin instanceof Group) {
                    boolean isMember = ((Group)userAdmin).isMember(currentUser);
                    if (isMember) {
                        administrator = true;
                    }
                }
                
            }
        } catch ( Exception ex ) {
            log.warn("Failed to determine if the user is an admin, assuming not. Cause: "+ex.getMessage());
            administrator = false;
        }
            
      
        // make sure user self-registration is enabled
        if (!administrator && !selfRegistrationEnabled) {
            throw new RepositoryException(
                "Sorry, registration of new users is not currently enabled.  Please try again later.");
        }


        // check that the submitted parameter values have valid values.
        if (name == null || name.length() == 0) {
            throw new RepositoryException("User name was not submitted");
        }
        if (password == null) {
            throw new RepositoryException("Password was not submitted");
        }
        if (!password.equals(passwordConfirm)) {
            throw new RepositoryException(
                "Password value does not match the confirmation password");
        }
        
        User user = null;
        Session selfRegSession = jcrSession;
        boolean useAdminSession = !administrator && selfRegistrationEnabled;
        try {
            if (useAdminSession) {
                //the current user doesn't have permission to create the user,
                // but self-registration is enabled, so use an admin session
                // to do the work.
                selfRegSession = getSession();
            }           

            UserManager userManager = AccessControlUtil.getUserManager(selfRegSession);
            Authorizable authorizable = userManager.getAuthorizable(name);

            if (authorizable != null) {
                // user already exists!
                throw new RepositoryException(
                    "A principal already exists with the requested name: "
                        + name);
            } else {
                user = userManager.createUser(name, password);
                String userPath = AuthorizableResourceProvider.SYSTEM_USER_MANAGER_USER_PREFIX
                    + user.getID();
                
                Map<String, RequestProperty> reqProperties = collectContent(
                    properties, userPath);

                changes.add(Modification.onCreated(userPath));

                // write content from form
                writeContent(selfRegSession, user, reqProperties, changes);

                if (selfRegSession.hasPendingChanges()) {
                    selfRegSession.save();
                }
                
                if (useAdminSession) {
                    //lookup the user from the user session so we can return a live object
                    UserManager userManager2 = AccessControlUtil.getUserManager(jcrSession);
                    Authorizable authorizable2 = userManager2.getAuthorizable(user.getID());
                    if (authorizable2 instanceof User) {
                        user = (User)authorizable2;
                    } else {
                        user = null;
                    }
                }                
            }
        } finally {
            if (useAdminSession) {
                //done with the self-reg admin session, so clean it up
                ungetSession(selfRegSession);
            }           
        }
        
        return user;
    }
    
}
