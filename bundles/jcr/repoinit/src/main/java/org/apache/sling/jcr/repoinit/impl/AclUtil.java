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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.api.security.JackrabbitAccessControlEntry;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlManager;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.commons.jackrabbit.authorization.AccessControlUtils;
import org.apache.jackrabbit.oak.spi.security.principal.EveryonePrincipal;
import org.apache.sling.repoinit.parser.operations.RestrictionClause;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utilities for ACL management */
public class AclUtil {

    private static final Logger LOG = LoggerFactory.getLogger(AclUtil.class);
    public static JackrabbitAccessControlManager getJACM(Session s) throws RepositoryException {
        final AccessControlManager acm = s.getAccessControlManager();
        if(!(acm instanceof JackrabbitAccessControlManager)) {
            throw new IllegalStateException(
                "AccessControlManager is not a JackrabbitAccessControlManager:"
                + acm.getClass().getName());
        }
        return (JackrabbitAccessControlManager) acm;
    }

    /**
     * Converts RestrictionClauses to structure consumable by
     * jackrabbit
     * @param list
     * @param jacl
     * @param s
     * @return
     * @throws RepositoryException
     */
    private static LocalRestrictions createLocalRestrictions(List<RestrictionClause> list, JackrabbitAccessControlList jacl, Session s) throws RepositoryException {
        Map<String,Value> restrictions = new HashMap<>();
        Map<String,Value[]> mvrestrictions = new HashMap<>();

        if(list != null && !list.isEmpty()){
            ValueFactory vf = s.getValueFactory();

           for(RestrictionClause rc : list){
               String restrictionName = rc.getName();
               int type = jacl.getRestrictionType(restrictionName);
               Value[] values = new Value[rc.getValues().size()];
               for(int i=0;i<values.length;i++) {
                   values[i] = vf.createValue(rc.getValues().get(i),type);
               }

               if(values.length == 1) {
                   restrictions.put(restrictionName, values[0]);
               } else {
                   mvrestrictions.put(restrictionName, values);
               }
           }
        }
        return new LocalRestrictions(restrictions,mvrestrictions);

    }


    public static void setAcl(Session session, List<String> principals, List<String> paths, List<String> privileges, boolean isAllow)
            throws RepositoryException {
        setAcl(session,principals,paths,privileges,isAllow,Arrays.asList(new RestrictionClause[]{}));
    }

    public static void setAcl(Session session, List<String> principals, List<String> paths, List<String> privileges, boolean isAllow, List<RestrictionClause> restrictionClauses)
            throws RepositoryException {

        final String [] privArray = privileges.toArray(new String[privileges.size()]);
        final Privilege[] jcrPriv = AccessControlUtils.privilegesFromNames(session, privArray);

        for(String path : paths) {
            if(!session.nodeExists(path)) {
                throw new PathNotFoundException("Cannot set ACL on non-existent path " + path);
            }

            JackrabbitAccessControlList acl = AccessControlUtils.getAccessControlList(session, path);

            LocalRestrictions localRestrictions = createLocalRestrictions(restrictionClauses, acl, session);

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
                LocalAccessControlEntry newAce = new LocalAccessControlEntry(principal, jcrPriv, isAllow, localRestrictions);
                if (contains(existingAces, newAce)) {
                    LOG.info("Not adding {} to path {} since an equivalent access control entry already exists", newAce, path);
                    continue;
                }
                acl.addEntry(newAce.principal, newAce.privileges, newAce.isAllow,
                        newAce.restrictions.getRestrictions(), newAce.restrictions.getMVRestrictions());
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

    /** Compare arrays a and b, which do not need to be ordered
     *  but are expected to be small.
     *  @param a might be sorted by this method
     *  @param b might be sorted by this method
     *  @return true if both arrays contain the same elements,
     *      in whatever order. Also true if both arrays are null
     *      or empty.
     */
    static boolean compareArrays(Object[] a, Object[] b) {
        if(a== null && b == null){
            return true;
        }
        if(a== null  || b == null){
            return false;
        }
        if(a.length != b.length){
            return false;
        }
        Arrays.sort(a);
        Arrays.sort(b);
        for(int i=0;i<a.length;i++){
            if(!a[i].equals(b[i])){
                return false;
            }
        }
        return true;
    }

    /**
     * Helper class which allows easy comparison of a local (proposed) access control entry with an existing one
     */
    static class LocalAccessControlEntry {

        private final Principal principal;
        private final Privilege[] privileges;
        private final boolean isAllow;
        private final LocalRestrictions restrictions;
        LocalAccessControlEntry(Principal principal, Privilege[] privileges, boolean isAllow) {
            this(principal, privileges, isAllow, null);
        }

        LocalAccessControlEntry(Principal principal, Privilege[] privileges,
                                boolean isAllow, LocalRestrictions restrictions) {
            this.principal = principal;
            this.privileges = privileges;
            this.isAllow = isAllow;
            this.restrictions = restrictions != null ? restrictions : new LocalRestrictions();
        }

        public boolean isContainedIn(JackrabbitAccessControlEntry other) throws RepositoryException {
            return other.getPrincipal().equals(principal) &&
                    contains(other.getPrivileges(), privileges) &&
                    other.isAllow() == isAllow &&
                    sameRestrictions(other);
        }
        private Set<Privilege> expandPrivileges(Privilege[] privileges){
            Set<Privilege> expandedSet = new HashSet<>();

            if(privileges != null){
                for(Privilege privilege : privileges){
                    if(privilege.isAggregate()){
                        expandedSet.addAll(Arrays.asList(privilege.getAggregatePrivileges()));
                    } else {
                        expandedSet.add(privilege);
                    }
                }
            }

            return expandedSet;
        }

        /**
         * compares if restrictions present in jackrabbit access control entry
         * is same as specified restrictions in repo init
         * @param jace
         * @return
         * @throws RepositoryException
         */
        private boolean sameRestrictions(JackrabbitAccessControlEntry jace) throws RepositoryException {
            // total (multivalue and simple)  number of restrictions should be same
            if(jace.getRestrictionNames().length == (restrictions.size())){
                for(String rn : jace.getRestrictionNames()){
                    Value[] oldValues = jace.getRestrictions(rn);
                    Value[] newValues = restrictions.getRestrictions().get(rn) != null
                                        ? new Value[]{restrictions.getRestrictions().get(rn)}
                                        : restrictions.getMVRestrictions().get(rn);
                    if((newValues == null || newValues.length == 0) && (oldValues == null || oldValues.length == 0)){
                        continue;
                    }

                    if(!compareArrays(newValues, oldValues)) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }

        private boolean contains(Privilege[] first, Privilege[] second) {
            // we need to ensure that the privilege order is not taken into account, so we use sets
            Set<Privilege> set1 = expandPrivileges(first);

            Set<Privilege> set2 = expandPrivileges(second);

            return set1.containsAll(set2);
        }

        @Override
        public String toString() {
            return "[" + getClass().getSimpleName() + "# principal " + principal+ ", privileges: " + Arrays.toString(privileges) + ", isAllow : " + isAllow + "]";
        }
    }

    /**
     * Helper class to store both restrictions and multi value restrictions
     * in ready to consume structure expected by jackrabbit
     */
    private static class LocalRestrictions {
        private Map<String,Value> restrictions;
        private Map<String,Value[]> mvRestrictions;
        public LocalRestrictions(){
            restrictions = new HashMap<>();
            mvRestrictions = new HashMap<>();
        }
        public LocalRestrictions(Map<String,Value> restrictions,Map<String,Value[]> mvRestrictions){
            this.restrictions = restrictions != null ? restrictions : new HashMap<String,Value>();
            this.mvRestrictions = mvRestrictions != null ? mvRestrictions : new HashMap<String,Value[]>();
        }

        public Map<String,Value> getRestrictions(){
            return this.restrictions;
        }

        public Map<String,Value[]> getMVRestrictions(){
            return this.mvRestrictions;
        }

        public int size(){
            return this.restrictions.size() + this.mvRestrictions.size();
        }
    }
}
