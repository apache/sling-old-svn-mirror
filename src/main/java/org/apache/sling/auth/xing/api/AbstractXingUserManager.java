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
package org.apache.sling.auth.xing.api;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.jcr.api.SlingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractXingUserManager implements XingUserManager {

    protected boolean autoCreateUser;

    protected boolean autoUpdateUser;

    protected Session session;

    public static final boolean DEFAULT_AUTO_CREATE_USER = true;

    public static final boolean DEFAULT_AUTO_UPDATE_USER = false;

    private final Logger logger = LoggerFactory.getLogger(AbstractXingUserManager.class);

    protected abstract SlingRepository getSlingRepository();

    @Override
    public boolean autoCreate() {
        return autoCreateUser;
    }

    @Override
    public boolean autoUpdate() {
        return autoUpdateUser;
    }

    @Override
    public User getUser(final Credentials credentials) {
        if (credentials instanceof SimpleCredentials) {
            final SimpleCredentials simpleCredentials = (SimpleCredentials) credentials;
            final String userId = simpleCredentials.getUserID();
            return getUser(userId);
        }
        return null;
    }

    protected User getUser(final String userId) {
        logger.info("getting user with id '{}'", userId);
        try {
            final Session session = getSession();
            final UserManager userManager = getUserManager(session);
            final Authorizable authorizable = userManager.getAuthorizable(userId);
            if (authorizable != null) {
                if (authorizable instanceof User) {
                    final User user = (User) authorizable;
                    logger.debug("user for id '{}' found", userId);
                    return user;
                } else {
                    logger.debug("found authorizable with id '{}' is not an user", authorizable.getID());
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    protected synchronized Session getSession() throws RepositoryException {
        if (session == null || !session.isLive()) {
            session = getSlingRepository().loginService(null, null);
        }
        return session;
    }

    protected UserManager getUserManager(final Session session) throws RepositoryException {
        if (session instanceof JackrabbitSession) {
            final JackrabbitSession jackrabbitSession = (JackrabbitSession) session;
            return jackrabbitSession.getUserManager();
        } else {
            logger.error("Cannot get UserManager from session: not a Jackrabbit session");
            return null;
        }
    }

}
