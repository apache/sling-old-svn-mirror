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
package org.apache.sling.jcr.repoinit.impl;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;

/** Utilities for User management */
public class UserUtil {

    public static UserManager getUserManager(Session session) throws RepositoryException {
        if(!(session instanceof JackrabbitSession)) {
            throw new IllegalArgumentException("Session is not a JackrabbitSession");
        }
        return ((JackrabbitSession)session).getUserManager();
    }

    public static Authorizable getAuthorizable(Session session, String id) throws RepositoryException {
        return getUserManager(session).getAuthorizable(id);
    }

    /** Create a service user - fails if it already exists */
    public static void createServiceUser(Session session, String username) throws RepositoryException {
        getUserManager(session).createSystemUser(username, null);
    }

    /** True if specified service user exists */
    public static boolean isServiceUser(Session session, String id) throws RepositoryException {
        boolean result = false;
        final Authorizable authorizable = getAuthorizable(session, id);
        if (authorizable != null && !authorizable.isGroup()) {
            final User user = (User) authorizable;
            result = user.isSystemUser();
        }
        return result;
    }

    public static void deleteUser(Session session, String id) throws RepositoryException {
        final Authorizable authorizable = getUserManager(session).getAuthorizable(id);
        if(authorizable == null) {
            throw new IllegalStateException("Authorizable not found:" + id);
        }
        authorizable.remove();
    }

    /** Create a user - fails if it already exists */
    public static void createUser(Session session, String username, String password) throws RepositoryException {
        getUserManager(session).createUser(username, password);
    }

    /** True if specified user exists */
    public static boolean userExists(Session session, String id) throws RepositoryException {
        boolean result = false;
        final Authorizable authorizable = getAuthorizable(session, id);
        if (authorizable != null) {
            result = !authorizable.isGroup();
        }
        return result;
    }

}
