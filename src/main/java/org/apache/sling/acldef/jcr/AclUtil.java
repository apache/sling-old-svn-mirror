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
package org.apache.sling.acldef.jcr;

import java.security.Principal;
import java.util.List;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlManager;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.commons.jackrabbit.authorization.AccessControlUtils;

/** Utilities for ACL management */
public class AclUtil {

    public static JackrabbitAccessControlManager getJACM(Session s) throws UnsupportedRepositoryOperationException, RepositoryException {
        final AccessControlManager acm = s.getAccessControlManager();
        if(!(acm instanceof JackrabbitAccessControlManager)) {
            throw new IllegalStateException(
                "AccessControlManager is not a JackrabbitAccessControlManager:" 
                + acm.getClass().getName());
        }
        return (JackrabbitAccessControlManager) acm;
    }
    
    public static void setAcl(Session s, List<String> principals, List<String> paths, List<String> privileges, boolean isAllow) 
            throws UnsupportedRepositoryOperationException, RepositoryException {
        
        final String [] privArray = privileges.toArray(new String[privileges.size()]);
        final Privilege[] jcrPriv = AccessControlUtils.privilegesFromNames(s, privArray);

        
        for(String path : paths) {
            if(!s.nodeExists(path)) {
                throw new PathNotFoundException("Cannot set ACL on non-existent path " + path);
            }
            JackrabbitAccessControlList acl = AccessControlUtils.getAccessControlList(s, path);
            for(String principal : principals) {
                final Authorizable a = ServiceUserUtil.getAuthorizable(s, principal);
                if(a == null) {
                    throw new IllegalStateException("Principal not found:" + principal);
                }
                final Principal p = a.getPrincipal(); 
                acl.addEntry(p, jcrPriv, isAllow);
            }
            getJACM(s).setPolicy(path, acl);
        }
    }
}
