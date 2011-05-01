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

import java.lang.reflect.Method;
import java.util.List;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.servlets.post.Modification;

/**
 * <p>
 * Changes the password associated with a user. Maps on to nodes of resourceType <code>sling/user</code> like
 * <code>/rep:system/rep:userManager/rep:users/ae/fd/3e/ieb</code> mapped to a resource url
 * <code>/system/userManager/user/ieb</code>. This servlet responds at
 * <code>/system/userManager/user/ieb.changePassword.html</code>
 * </p>
 * <h4>Methods</h4>
 * <ul>
 * <li>POST</li>
 * </ul>
 * <h4>Post Parameters</h4>
 * <dl>
 * <dt>oldPwd</dt>
 * <dd>The current password for the user (required)</dd>
 * <dt>newPwd</dt>
 * <dd>The new password for the user (required)</dd>
 * <dt>newPwdConfirm</dt>
 * <dd>The confirm new password for the user (required)</dd>
 * </dl>
 * <h4>Response</h4>
 * <dl>
 * <dt>200</dt>
 * <dd>Success sent with no body</dd>
 * <dt>404</dt>
 * <dd>If the user was not found.</dd>
 * <dt>500</dt>
 * <dd>Failure, including password validation errors. HTML explains the failure.</dd>
 * </dl>
 * <h4>Example</h4>
 *
 * <code>
 * curl -FoldPwd=oldpassword -FnewPwd=newpassword =FnewPwdConfirm=newpassword http://localhost:8080/system/userManager/user/ieb.changePassword.html
 * </code>
 *
 * <h4>Notes</h4>
 *
 *
 * @scr.component metatype="no" immediate="true"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.resourceTypes" value="sling/user"
 * @scr.property name="sling.servlet.methods" value="POST"
 * @scr.property name="sling.servlet.selectors" value="changePassword"
 */
public class ChangeUserPasswordServlet extends AbstractUserPostServlet {
    private static final long serialVersionUID = 1923614318474654502L;

    /*
     * (non-Javadoc)
     * @see
     * org.apache.sling.jackrabbit.usermanager.post.AbstractAuthorizablePostServlet
     * #handleOperation(org.apache.sling.api.SlingHttpServletRequest,
     * org.apache.sling.api.servlets.HtmlResponse, java.util.List)
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

        // check that the user was located.
        if (authorizable == null || authorizable.isGroup()) {
            throw new ResourceNotFoundException(
                "User to update could not be determined.");
        }

        if ("anonymous".equals(authorizable.getID())) {
            throw new RepositoryException(
                "Can not change the password of the anonymous user.");
        }

        Session session = request.getResourceResolver().adaptTo(Session.class);
        if (session == null) {
            throw new RepositoryException("JCR Session not found");
        }

        // check that the submitted parameter values have valid values.
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
            throw new RepositoryException(
                "New Password does not match the confirmation password");
        }

        // verify old password
        checkPassword(authorizable, oldPwd);

        try {
            ((User) authorizable).changePassword(digestPassword(newPwd));

            changes.add(Modification.onModified(resource.getPath()
                + "/rep:password"));
        } catch (RepositoryException re) {
            throw new RepositoryException("Failed to change user password.", re);
        }
    }

    private void checkPassword(Authorizable authorizable, String oldPassword)
            throws RepositoryException {
        Credentials oldCreds = ((User) authorizable).getCredentials();
        if (oldCreds instanceof SimpleCredentials) {
            char[] oldCredsPwd = ((SimpleCredentials) oldCreds).getPassword();
            if (oldPassword.equals(String.valueOf(oldCredsPwd))) {
                return;
            }
        } else {
            try {
                // CryptSimpleCredentials.matches(SimpleCredentials credentials)
                Class<?> oldCredsClass = oldCreds.getClass();
                Method matcher = oldCredsClass.getMethod("matches",
                    SimpleCredentials.class);
                SimpleCredentials newCreds = new SimpleCredentials(
                    authorizable.getPrincipal().getName(),
                    oldPassword.toCharArray());
                boolean match = (Boolean) matcher.invoke(oldCreds, newCreds);
                if (match) {
                    return;
                }
            } catch (Throwable t) {
                // failure here, fall back to password check failure below
            }
        }

        throw new RepositoryException("Old Password does not match");
    }
}
