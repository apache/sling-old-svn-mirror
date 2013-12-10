/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.extensions.webconsolesecurityprovider.internal;

import java.util.Iterator;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.webconsole.WebConsoleSecurityProvider2;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.auth.Authenticator;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.auth.core.AuthenticationSupport;

/**
 * The <code>SlingWebConsoleSecurityProvider</code> is security provider for the
 * Apache Felix Web Console which validates the user name and password by loging
 * into the repository and the checking whether the user is allowed access.
 * Access granted by the {@link #authenticate(String, String)} method applies to
 * all of the Web Console since the {@link #authorize(Object, String)} method
 * always returns <code>true</code>.
 * <p>
 * This security provider requires a JCR Repository to operate. Therefore it is
 * only registered as a security provider service once such a JCR Repository is
 * available.
 */
public class SlingWebConsoleSecurityProvider2
    extends AbstractWebConsoleSecurityProvider
    implements WebConsoleSecurityProvider2 {

    private final AuthenticationSupport authentiationSupport;

    private final Authenticator authenticator;

    public SlingWebConsoleSecurityProvider2(final Object support, final Object authenticator) {
        this.authentiationSupport = (AuthenticationSupport)support;
        this.authenticator = (Authenticator)authenticator;
    }

    /**
     * @see org.apache.felix.webconsole.WebConsoleSecurityProvider2#authenticate(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public boolean authenticate(final HttpServletRequest request,
            final HttpServletResponse response) {
        if ( this.authentiationSupport.handleSecurity(request, response) ) {
            // get ResourceResolver (set by AuthenticationSupport)
            Object resolverObject = request.getAttribute(AuthenticationSupport.REQUEST_ATTRIBUTE_RESOLVER);
            final ResourceResolver resolver = (resolverObject instanceof ResourceResolver)
                    ? (ResourceResolver) resolverObject
                    : null;
            if ( resolver != null ) {
                final Session session = resolver.adaptTo(Session.class);
                if ( session != null ) {
                    try {
                        final User u = this.authenticate(session);
                        if ( u != null ) {
                            request.setAttribute(USER_ATTRIBUTE, u);
                            return true;
                        }
                    } catch (final Exception re) {
                        logger.info("authenticate: Generic problem trying grant User "
                            + " access to the Web Console", re);
                    }
                }
            }

            this.authenticator.login(request, response);
        }
        return false;
    }

    public User authenticate(String userName, String password) {
        return null; // this method is never invoked
    }

    private User authenticate(final Session session) throws RepositoryException {
        String userId = session.getUserID();
        if (session instanceof JackrabbitSession) {
            UserManager umgr = ((JackrabbitSession) session).getUserManager();
            Authorizable a = umgr.getAuthorizable(userId);
            if (a instanceof User) {

                // check users
                if (users.contains(userId)) {
                    return (User)a;
                }

                // check groups
                @SuppressWarnings("unchecked")
                Iterator<Group> gi = a.memberOf();
                while (gi.hasNext()) {
                    if (groups.contains(gi.next().getID())) {
                        return (User)a;
                    }
                }

                logger.info(
                    "authenticate: User {} is denied Web Console access",
                    userId);
            } else {
                logger.error(
                    "authenticate: Expected user ID {} to refer to a user",
                    userId);
            }
        } else {
            logger.info(
                "authenticate: Jackrabbit Session required to grant access to the Web Console for {}; got {}",
                userId, session.getClass());
        }
        return null;
    }

    /**
     * All users authenticated with the repository and being a member of the
     * authorized groups are granted access for all roles in the Web Console.
     */
    public boolean authorize(Object user, String role) {
        logger.debug("authorize: Grant user {} access for role {}", user, role);
        return true;
    }
}
