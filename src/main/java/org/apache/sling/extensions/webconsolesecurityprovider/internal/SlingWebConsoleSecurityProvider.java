/*
 * Copyright 1997-2010 Day Management AG
 * Barfuesserplatz 6, 4001 Basel, Switzerland
 * All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * Day Management AG, ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Day.
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

@Component(specVersion = "1.1", metatype = true)
@Service(WebConsoleSecurityProvider.class)
public class SlingWebConsoleSecurityProvider implements
        WebConsoleSecurityProvider {

    private static final String PROP_USERS = "users";

    private static final String PROP_GROUPS_DEFAULT_USER = "admin";

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
