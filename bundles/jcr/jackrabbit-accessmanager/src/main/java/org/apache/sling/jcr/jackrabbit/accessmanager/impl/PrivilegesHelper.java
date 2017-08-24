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
package org.apache.sling.jcr.jackrabbit.accessmanager.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;

import org.apache.sling.jcr.base.util.AccessControlUtil;

/**
 * Contains utility methods related to handling privileges 
 *
 */
public abstract class PrivilegesHelper {
    
    /**
     * Builds a map of aggregate privileges to privileges they aggregate
     * 
     * @param jcrSession a JCR session
     * @param resourcePath the path used to look up the supported privileges
     * @return a map, never <code>null</code>
     * 
     * @throws RepositoryException error accessing the repository
     */
    public static Map<Privilege, Set<Privilege>> buildPrivilegeToAncestorMap(Session jcrSession, String resourcePath)
            throws RepositoryException {
        AccessControlManager accessControlManager = AccessControlUtil.getAccessControlManager(jcrSession);
        Map<Privilege, Set<Privilege>> privilegeToAncestorMap = new HashMap<Privilege, Set<Privilege>>();
        Privilege[] supportedPrivileges = accessControlManager.getSupportedPrivileges(resourcePath);
        for (Privilege privilege : supportedPrivileges) {
            if (privilege.isAggregate()) {
                Privilege[] ap = privilege.getAggregatePrivileges();
                for (Privilege privilege2 : ap) {
                    Set<Privilege> set = privilegeToAncestorMap.get(privilege2);
                    if (set == null) {
                        set = new HashSet<Privilege>();
                        privilegeToAncestorMap.put(privilege2, set);
                    }
                    set.add(privilege);
                }
            }
        }
        return privilegeToAncestorMap;
    }
    
    /**
     * Update the granted and denied privilege sets by merging the result of adding
     * the supplied privilege.
     * 
     * @param privilege the privilege to merge
     * @param privilegeToAncestorMap mapping created using {@linkplain #buildPrivilegeToAncestorMap(Session, String)}
     * @param add the first set to which the <tt>privilege</tt> should be added if missing
     * @param remove the second set from which the <tt>privilege</tt> should be removed if present
     */
    // visible for testing
    public static void mergePrivilegeSets(Privilege privilege,
            Map<Privilege, Set<Privilege>> privilegeToAncestorMap,
            Set<Privilege> add, Set<Privilege> remove) {
        //1. remove duplicates and invalid privileges from the list
        if (privilege.isAggregate()) {
            Privilege[] aggregatePrivileges = privilege.getAggregatePrivileges();
            //remove duplicates from the granted set
            List<Privilege> asList = Arrays.asList(aggregatePrivileges);
            add.removeAll(asList);
            
            // tentatively check for aggregate in the 'remov' set
            for ( Privilege removeCandidate : remove ) {
                if ( removeCandidate.isAggregate() && containsAny(removeCandidate.getAggregatePrivileges(), aggregatePrivileges) ) {
                    remove.remove(removeCandidate);
                    remove.addAll(Arrays.asList(removeCandidate.getAggregatePrivileges()));
                    remove.removeAll(Arrays.asList(aggregatePrivileges));
                }
            }
            

            //remove from the denied set
            remove.removeAll(asList);
        }
        remove.remove(privilege);

        //2. check if the privilege is already contained in another granted privilege
        boolean isAlreadyGranted = false;
        Set<Privilege> ancestorSet = privilegeToAncestorMap.get(privilege);
        if (ancestorSet != null) {
            for (Privilege privilege2 : ancestorSet) {
                if (add.contains(privilege2)) {
                    isAlreadyGranted = true;
                    break;
                }
            }
        }

        //3. add the privilege
        if (!isAlreadyGranted) {
            add.add(privilege);
        }

        //4. Deal with expanding existing aggregate privileges to remove the invalid
        //  items and add the valid ones.
        Set<Privilege> filterSet = privilegeToAncestorMap.get(privilege);
        if (filterSet != null) {
            //re-pack the denied set to compensate
            for (Privilege privilege2 : filterSet) {
                if (remove.contains(privilege2)) {
                    remove.remove(privilege2);
                    if (privilege2.isAggregate()) {
                        filterAndMergePrivilegesFromAggregate(privilege2,
                                add, remove, filterSet, privilege);
                    }
                }
            }
        }
    }

    private static boolean containsAny(Privilege[] aggregatePrivileges, Privilege[] aggregatePrivileges2) {
        List<Privilege> privs = Arrays.asList(aggregatePrivileges);
        for ( Privilege agg2 : aggregatePrivileges2) {
            if ( privs.contains(agg2))
                return true;
        }
        
        return false;
    }

    /**
     * Add all the declared aggregate privileges from the supplied privilege to the secondSet
     * unless the privilege is already in the firstSet and not contained in the supplied filterSet.
     */
    private static void filterAndMergePrivilegesFromAggregate(Privilege privilege, Set<Privilege> firstSet,
            Set<Privilege> secondSet, Set<Privilege> filterSet, Privilege ignorePrivilege) {
        Privilege[] declaredAggregatePrivileges = privilege.getDeclaredAggregatePrivileges();
        for (Privilege privilege3 : declaredAggregatePrivileges) {
            if (ignorePrivilege.equals(privilege3)) {
                continue; //skip it.
            }
            if (!firstSet.contains(privilege3) && !filterSet.contains(privilege3)) {
                secondSet.add(privilege3);
            }
            if (privilege3.isAggregate()) {
                Privilege[] declaredAggregatePrivileges2 = privilege3.getDeclaredAggregatePrivileges();
                for (Privilege privilege2 : declaredAggregatePrivileges2) {
                    if (!ignorePrivilege.equals(privilege2)) {
                        if (privilege2.isAggregate()) {
                            filterAndMergePrivilegesFromAggregate(privilege2,
                                    firstSet, secondSet, filterSet, ignorePrivilege);
                        }
                    }
                }
            }
        }
    }    

    private PrivilegesHelper() {
        
    }
}
