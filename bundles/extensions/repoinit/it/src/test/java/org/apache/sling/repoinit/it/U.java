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
package org.apache.sling.repoinit.it;

import java.util.UUID;

import javax.jcr.AccessDeniedException;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;

/** Test utilities */
public class U {
    public static boolean userExists(Session session, String id) throws LoginException, RepositoryException, InterruptedException {
        final Authorizable a = ((JackrabbitSession)session).getUserManager().getAuthorizable(id);
        return a != null;
    }
    
    public static Session getServiceSession(Session session, String serviceId) throws LoginException, RepositoryException {
        return session.impersonate(new SimpleCredentials(serviceId, new char[0]));
    }
    
    /** True if user can write to specified path. 
     *  @throws PathNotFoundException if the path doesn't exist */ 
    public static boolean canWrite(Session session, String userId, String path) throws PathNotFoundException,RepositoryException {
        if(!session.itemExists(path)) {
            throw new PathNotFoundException(path);
        }
        
        final Session serviceSession = getServiceSession(session, userId);
        final String testNodeName = "test_" + UUID.randomUUID().toString();
        try {
            ((Node)serviceSession.getItem(path)).addNode(testNodeName);
            serviceSession.save();
        } catch(AccessDeniedException ade) {
            return false;
        } finally {
            serviceSession.logout();
        }
        return true;
    }
    
    /** True if user can read specified path. 
     *  @throws PathNotFoundException if the path doesn't exist */ 
    public static boolean canRead(Session session, String userId, String path) throws PathNotFoundException,RepositoryException {
        if(!session.itemExists(path)) {
            throw new PathNotFoundException(path);
        }
        
        final Session serviceSession = getServiceSession(session, userId);
        try {
            serviceSession.getItem(path);
        } catch(AccessDeniedException ade) {
            return false;
        } finally {
            serviceSession.logout();
        }
        return true;
    }
}