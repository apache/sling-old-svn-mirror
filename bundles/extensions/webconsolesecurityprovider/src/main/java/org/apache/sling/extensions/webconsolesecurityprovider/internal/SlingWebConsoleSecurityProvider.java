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

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;

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
@Component(ds=false, metatype=true,
           label="Apache Sling Web Console Security Provider",
           description="Configuration for the security provider used to verfiy user " +
                       "credentials and grant access to the Apache Felix Web Console " +
                       "based on registered JCR Repository users.")
@Properties({
    @Property(name = "users", value=AbstractWebConsoleSecurityProvider.PROP_GROUPS_DEFAULT_USER, cardinality=20,
              label="User Names",
              description="Names of users granted full access to the Apache Felix " +
                          "Web Console. By default this lists the \"admin\" user. A maximum of 20 users" +
                          " may be configured. Administrators are encouraged to create a group whose" +
                          " members are to be granted access to Web Console instead of allowing access" +
                          " to individual users."),
    @Property(name = "groups", cardinality=20,
             label="Group Names",
             description="Names of groups whose members are granted full access to the Apache Felix " +
                         "Web Console. The default lists no groups. Administrators are encouraged to " +
                         "create a group whose members are to be granted access to the Web Console." +
                         " A maximum of 20 groups may be configured. Using groups to control" +
                         " access requires a Jackrabbit based repository.")
})
public class SlingWebConsoleSecurityProvider extends AbstractWebConsoleSecurityProvider {

    private Repository repository;

    public SlingWebConsoleSecurityProvider(final Object repository) {
        this.repository = (Repository)repository;
    }

    // ---------- SCR integration

    /**
     * Authenticates and authorizes the user identified by the user name and
     * password. The check applied to authorize access consists of the following
     * steps:
     * <ol>
     * <li>User name and password are able to create a JCR session with the
     * default repository workspace. If such a session cannot be created, the
     * user is denied access.</li>
     * <li>If the user is listed in the configured set of granted users, access
     * is granted to all of the Web Console.</li>
     * <li>If the user is a member of one of the groups configured to grant
     * access to their members, access is granted to all of the Web Console.</li>
     * </ol>
     * <p>
     * If the user name and password cannot be used to login to the default
     * workspace of the repository or if the user neither one of the configured
     * set of granted users or is not a member of the configured set of groups
     * access is denied to the Web Console.
     *
     * @param userName The name of the user to grant access for
     * @param password The password to authenticate the user. This may be
     *            <code>null</code> to assume an empty password.
     * @return The <code>userName</code> is currently returned to indicate
     *         successfull authentication.
     * @throws NullPointerException if <code>userName</code> is
     *             <code>null</code>.
     */
    public Object authenticate(String userName, String password) {
        final Credentials creds = new SimpleCredentials(userName,
            (password == null) ? new char[0] : password.toCharArray());
        Session session = null;
        try {
            session = repository.login(creds);
            if (session instanceof JackrabbitSession) {
                UserManager umgr = ((JackrabbitSession) session).getUserManager();
                String userId = session.getUserID();
                Authorizable a = umgr.getAuthorizable(userId);
                if (a instanceof User) {

                    // check users
                    if (users.contains(userId)) {
                        return true;
                    }

                    // check groups
                    @SuppressWarnings("unchecked")
                    Iterator<Group> gi = a.memberOf();
                    while (gi.hasNext()) {
                        if (groups.contains(gi.next().getID())) {
                            return userName;
                        }
                    }

                    logger.debug(
                        "authenticate: User {} is denied Web Console access",
                        userName);
                } else {
                    logger.error(
                        "authenticate: Expected user ID {} to refer to a user",
                        userId);
                }
            } else {
                logger.info(
                    "authenticate: Jackrabbit Session required to grant access to the Web Console for {}; got {}",
                    userName, session.getClass());
            }
        } catch (final LoginException re) {
            logger.info(
                "authenticate: User "
                    + userName
                    + " failed to authenticate with the repository for Web Console access",
                re);
        } catch (final Exception re) {
            logger.info("authenticate: Generic problem trying grant User "
                + userName + " access to the Web Console", re);
        } finally {
            if (session != null) {
                session.logout();
            }
        }

        // no success (see log)
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
