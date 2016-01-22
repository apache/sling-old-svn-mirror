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
package org.apache.sling.repoinit.jcr;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;

/** Utilities for Service Users management */
public class ServiceUserUtil {

    public static UserManager getUserManager(Session session) throws RepositoryException {
        if(!(session instanceof JackrabbitSession)) {
            throw new IllegalArgumentException("Session is not a JackrabbitSession");
        }
        return ((JackrabbitSession)session).getUserManager();
    }
    
    public static Authorizable getAuthorizable(Session session, String username) throws RepositoryException {
        return getUserManager(session).getAuthorizable(username);
    }
    
    /** Create a service user - fails if it already exists */
    public static void createServiceUser(Session s, String username) throws RepositoryException {
        getUserManager(s).createSystemUser(username, null);
    }
    
    /** True if specified service user exists */
    public static boolean serviceUserExists(Session session, String username) throws RepositoryException {
        boolean result = false;
        final Authorizable a = getAuthorizable(session, username);
        if(a != null) {
            final User u = (User)a;
            result = u.isSystemUser();
        }
        return result;
    }
    
    public static void deleteServiceUser(Session s, String username) throws RepositoryException {
        final Authorizable a = getUserManager(s).getAuthorizable(username);
        if(a == null) {
            throw new IllegalStateException("Authorizable not found:" + username);
        }
        a.remove();
    }
    
}
