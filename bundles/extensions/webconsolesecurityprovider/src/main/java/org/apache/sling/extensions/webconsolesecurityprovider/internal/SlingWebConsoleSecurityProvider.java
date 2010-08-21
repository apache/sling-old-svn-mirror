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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.webconsole.WebConsoleSecurityProvider;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
@Component(specVersion = "1.1", metatype = true)
@Service(WebConsoleSecurityProvider.class)
public class SlingWebConsoleSecurityProvider implements
        WebConsoleSecurityProvider {

    // name of the property providing list of authorized users
    private static final String PROP_USERS = "users";

    // default user being authorized
    private static final String PROP_GROUPS_DEFAULT_USER = "admin";

    // name of the property providing list of groups whose members are
    // authorized
    private static final String PROP_GROUPS = "groups";

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    private Repository repository;

    @Property(name = PROP_USERS, cardinality = 20, value = PROP_GROUPS_DEFAULT_USER)
    private Set<String> users;

    @Property(name = PROP_GROUPS, cardinality = 20)
    private Set<String> groups;

    // ---------- SCR integration

    @SuppressWarnings("unused")
    @Activate
    @Modified
    private void configure(Map<String, Object> config) {
        this.users = toSet(config.get(PROP_USERS));
        this.groups = toSet(config.get(PROP_GROUPS));
    }

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

                    log.info(
                        "authenticate: User {} is granted Web Console access",
                        userName);
                } else {
                    log.error(
                        "authenticate: Expected user ID {} to refer to a user",
                        userId);
                }
            } else {
                log.info(
                    "authenticate: Jackrabbit Session required to grant access to the Web Console for {}; got {}",
                    userName, session.getClass());
            }
        } catch (LoginException re) {
            log.info(
                "authenticate: User "
                    + userName
                    + " failed to authenticate with the repository for Web Console access",
                re);
        } catch (RepositoryException re) {
            log.info("authenticate: Generic problem trying grant User "
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
        log.info("authorize: Grant user {} access for role {}", user, role);
        return true;
    }

    private Set<String> toSet(final Object configObj) {
        final HashSet<String> groups = new HashSet<String>();
        if (configObj instanceof String) {
            groups.add((String) configObj);
        } else if (configObj instanceof Collection<?>) {
            for (Object obj : ((Collection<?>) configObj)) {
                if (obj instanceof String) {
                    groups.add((String) obj);
                }
            }
        } else if (configObj instanceof String[]) {
            for (String string : ((String[]) configObj)) {
                if (string != null) {
                    groups.add(string);
                }
            }
        }
        return groups;
    }
}
