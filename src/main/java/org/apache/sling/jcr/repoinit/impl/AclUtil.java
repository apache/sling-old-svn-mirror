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

import java.security.Principal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.api.security.JackrabbitAccessControlEntry;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlManager;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.commons.jackrabbit.authorization.AccessControlUtils;
import org.apache.jackrabbit.oak.spi.security.principal.EveryonePrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utilities for ACL management */
public class AclUtil {

    private static final Logger LOG = LoggerFactory.getLogger(AclUtil.class);
    
    public static JackrabbitAccessControlManager getJACM(Session s) throws UnsupportedRepositoryOperationException, RepositoryException {
        final AccessControlManager acm = s.getAccessControlManager();
        if(!(acm instanceof JackrabbitAccessControlManager)) {
            throw new IllegalStateException(
                "AccessControlManager is not a JackrabbitAccessControlManager:"
                + acm.getClass().getName());
        }
        return (JackrabbitAccessControlManager) acm;
    }

    public static void setAcl(Session session, List<String> principals, List<String> paths, List<String> privileges, boolean isAllow)
            throws UnsupportedRepositoryOperationException, RepositoryException {

        final String [] privArray = privileges.toArray(new String[privileges.size()]);
        final Privilege[] jcrPriv = AccessControlUtils.privilegesFromNames(session, privArray);

        for(String path : paths) {
            if(!session.nodeExists(path)) {
                throw new PathNotFoundException("Cannot set ACL on non-existent path " + path);
            }
            JackrabbitAccessControlList acl = AccessControlUtils.getAccessControlList(session, path);
            AccessControlEntry[] existingAces = acl.getAccessControlEntries();
            boolean changed = false;
            for (String name : principals) {
                final Principal principal;
                if (EveryonePrincipal.NAME.equals(name)) {
                    principal = AccessControlUtils.getPrincipal(session, name);
                } else {
                    final Authorizable authorizable = UserUtil.getAuthorizable(session, name);
                    if (authorizable == null) {
                        throw new IllegalStateException("Authorizable not found:" + name);
                    }
                    principal = authorizable.getPrincipal();
                }
                if (principal == null) {
                    throw new IllegalStateException("Principal not found: " + name);
                }
                LocalAccessControlEntry newAce = new LocalAccessControlEntry(principal, jcrPriv, isAllow);
                if (contains(existingAces, newAce)) {
                    LOG.info("Not adding {} to path {} since an equivalent access control entry already exists", newAce, path);
                    continue;
                }
                acl.addEntry(newAce.principal, newAce.privileges, newAce.isAllow);
                changed = true;
            }
            if ( changed ) {
                getJACM(session).setPolicy(path, acl);
            }
            
        }
    }

    // visible for testing
    static boolean contains(AccessControlEntry[] existingAces, LocalAccessControlEntry newAce) throws RepositoryException {
        for (int i = 0 ; i < existingAces.length; i++) {
            JackrabbitAccessControlEntry existingEntry = (JackrabbitAccessControlEntry) existingAces[i];
            LOG.debug("Comparing {} with {}", newAce, toString(existingEntry));
            if (newAce.isContainedIn(existingEntry)) {
                return true;
            }
        }
        return false;
    }

    private static String toString(JackrabbitAccessControlEntry entry) throws RepositoryException {
        return "[" + entry.getClass().getSimpleName() + "# principal: "
                + "" + entry.getPrincipal() + ", privileges: " + Arrays.toString(entry.getPrivileges()) +
                ", isAllow: " + entry.isAllow() + ", restrictionNames: " + entry.getRestrictionNames()  + "]";
    }

    /**
     * Helper class which allows easy comparison of a local (proposed) access control entry with an existing one
     */
    static class LocalAccessControlEntry {
        
        private final Principal principal;
        private final Privilege[] privileges;
        private final boolean isAllow;
        
        LocalAccessControlEntry(Principal principal, Privilege[] privileges, boolean isAllow) {
            this.principal = principal;
            this.privileges = privileges;
            this.isAllow = isAllow;
        }
        
        public boolean isContainedIn(JackrabbitAccessControlEntry other) throws RepositoryException {
            return other.getPrincipal().equals(principal) &&
                    contains(other.getPrivileges(), privileges) &&
                    other.isAllow() == isAllow &&
                    ( other.getRestrictionNames() == null || 
                        other.getRestrictionNames().length == 0 );
        }
        
        private boolean contains(Privilege[] first, Privilege[] second) {
            // we need to ensure that the privilege order is not taken into account, so we use sets
            Set<Privilege> set1 = new HashSet<Privilege>();
            set1.addAll(Arrays.asList(first));
            
            Set<Privilege> set2 = new HashSet<Privilege>();
            set2.addAll(Arrays.asList(second));
            
            return set1.containsAll(set2);
        }
        
        @Override
        public String toString() {
            return "[" + getClass().getSimpleName() + "# principal " + principal+ ", privileges: " + Arrays.toString(privileges) + ", isAllow : " + isAllow + "]";
        }
    }
}
