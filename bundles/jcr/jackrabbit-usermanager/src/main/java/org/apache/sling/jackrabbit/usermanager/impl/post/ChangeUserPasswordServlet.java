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
import javax.servlet.Servlet;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.jackrabbit.usermanager.ChangeUserPassword;
import org.apache.sling.jackrabbit.usermanager.impl.resource.AuthorizableResourceProvider;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.servlets.post.AbstractPostResponse;
import org.apache.sling.servlets.post.Modification;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Changes the password associated with a user. Maps on to nodes of resourceType <code>sling/user</code> like
 * <code>/rep:system/rep:userManager/rep:users/ae/fd/3e/ieb</code> mapped to a resource url
 * <code>/system/userManager/user/ieb</code>. This servlet responds at
 * <code>/system/userManager/user/ieb.changePassword.html</code>
 * </p>
 * <h3>Methods</h3>
 * <ul>
 * <li>POST</li>
 * </ul>
 * <h3>Post Parameters</h3>
 * <dl>
 * <dt>oldPwd</dt>
 * <dd>The current password for the user (required for non-administrators)</dd>
 * <dt>newPwd</dt>
 * <dd>The new password for the user (required)</dd>
 * <dt>newPwdConfirm</dt>
 * <dd>The confirm new password for the user (required)</dd>
 * </dl>
 * <h3>Response</h3>
 * <dl>
 * <dt>200</dt>
 * <dd>Success sent with no body</dd>
 * <dt>404</dt>
 * <dd>If the user was not found.</dd>
 * <dt>500</dt>
 * <dd>Failure, including password validation errors. HTML explains the failure.</dd>
 * </dl>
 * <h3>Example</h3>
 *
 * <code>
 * curl -FoldPwd=oldpassword -FnewPwd=newpassword -FnewPwdConfirm=newpassword http://localhost:8080/system/userManager/user/ieb.changePassword.html
 * </code>
 *
 * <h3>Notes</h3>
 */

@Component(service = {Servlet.class, ChangeUserPassword.class},
           property = {
        		   "sling.servlet.resourceTypes=sling/user",
        		   "sling.servlet.methods=POST",
        		   "sling.servlet.selectors=changePassword",
        		   AbstractAuthorizablePostServlet.PROP_DATE_FORMAT + "=EEE MMM dd yyyy HH:mm:ss 'GMT'Z", 
        		   AbstractAuthorizablePostServlet.PROP_DATE_FORMAT + "=yyyy-MM-dd'T'HH:mm:ss.SSSZ", 
        		   AbstractAuthorizablePostServlet.PROP_DATE_FORMAT + "=yyyy-MM-dd'T'HH:mm:ss", 
        		   AbstractAuthorizablePostServlet.PROP_DATE_FORMAT + "=yyyy-MM-dd", 
        		   AbstractAuthorizablePostServlet.PROP_DATE_FORMAT + "=dd.MM.yyyy HH:mm:ss", 
        		   AbstractAuthorizablePostServlet.PROP_DATE_FORMAT + "=dd.MM.yyyy",
        		   ChangeUserPasswordServlet.PAR_USER_ADMIN_GROUP_NAME + "=" + ChangeUserPasswordServlet.DEFAULT_USER_ADMIN_GROUP_NAME
           })
public class ChangeUserPasswordServlet extends AbstractAuthorizablePostServlet implements ChangeUserPassword {
    private static final long serialVersionUID = 1923614318474654502L;

    /**
     * default log
     */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * The default 'User administrator' group name
     *
     * @see #PAR_USER_ADMIN_GROUP_NAME
     */
    static final String DEFAULT_USER_ADMIN_GROUP_NAME = "UserAdmin";

    /**
     * The name of the configuration parameter providing the
     * name of the group whose members are allowed to reset the password
     * of a user without the 'oldPwd' value.
     */
    static final String PAR_USER_ADMIN_GROUP_NAME = "user.admin.group.name";

    private String userAdminGroupName = DEFAULT_USER_ADMIN_GROUP_NAME;

    // ---------- SCR integration ---------------------------------------------

    /**
     * Activates this component.
     *
     * @param props The component properties
     */
    @Override
    @Activate
    protected void activate(final Map<String, Object> props) {
        super.activate(props);

        this.userAdminGroupName = OsgiUtil.toString(props.get(PAR_USER_ADMIN_GROUP_NAME),
                DEFAULT_USER_ADMIN_GROUP_NAME);
        log.debug("User Admin Group Name {}", this.userAdminGroupName);
    }

    @Override
    @Deactivate
    protected void deactivate() {
        super.deactivate();
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

        Resource resource = request.getResource();
        Session session = request.getResourceResolver().adaptTo(Session.class);
        changePassword(session,
                resource.getName(),
                request.getParameter("oldPwd"),
                request.getParameter("newPwd"),
                request.getParameter("newPwdConfirm"),
                changes);
    }

    /* (non-Javadoc)
     * @see org.apache.sling.jackrabbit.usermanager.ChangeUserPassword#changePassword(javax.jcr.Session, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.util.List)
     */
    public User changePassword(Session jcrSession,
                                String name,
                                String oldPassword,
                                String newPassword,
                                String newPasswordConfirm,
                                List<Modification> changes)
                throws RepositoryException {

        if ("anonymous".equals(name)) {
            throw new RepositoryException(
                "Can not change the password of the anonymous user.");
        }

        User user;
        UserManager userManager = AccessControlUtil.getUserManager(jcrSession);
        Authorizable authorizable = userManager.getAuthorizable(name);
        if (authorizable instanceof User) {
            user = (User)authorizable;
        } else {
            throw new ResourceNotFoundException(
                "User to update could not be determined");
        }

        //SLING-2069: if the current user is an administrator, then a missing oldPwd is ok,
        // otherwise the oldPwd must be supplied.
        boolean administrator = false;

        // check that the submitted parameter values have valid values.
        if (oldPassword == null || oldPassword.length() == 0) {
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
            if (!administrator) {
                throw new RepositoryException("Old Password was not submitted");
            }
        }
        if (newPassword == null || newPassword.length() == 0) {
            throw new RepositoryException("New Password was not submitted");
        }
        if (!newPassword.equals(newPasswordConfirm)) {
            throw new RepositoryException(
                "New Password does not match the confirmation password");
        }

        try {
            if (oldPassword != null && oldPassword.length() > 0) {
                // verify old password
                user.changePassword(newPassword, oldPassword);
            } else {
                user.changePassword(newPassword);
            }

            final String passwordPath = AuthorizableResourceProvider.SYSTEM_USER_MANAGER_USER_PREFIX + user.getID() + "/rep:password";

            changes.add(Modification.onModified(passwordPath));
        } catch (RepositoryException re) {
            throw new RepositoryException("Failed to change user password.", re);
        }

        return user;
    }
}
