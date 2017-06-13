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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.security.Principal;

import javax.jcr.RepositoryException;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.commons.jackrabbit.authorization.AccessControlUtils;
import org.apache.sling.repoinit.parser.RepoInitParsingException;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class AclUtilTest {

    @Rule
    public final SlingContext context = new SlingContext(ResourceResolverType.JCR_OAK);
    
    private TestUtil U;

    private JackrabbitAccessControlList acl;
    
    @Before
    public void setup() throws RepositoryException, RepoInitParsingException {
        U = new TestUtil(context);
        U.parseAndExecute("create service user " + U.username);
        
        final String aclSetup = 
                "set ACL for " + U.username + "\n"
                + "allow jcr:read on /\n"
                + "allow jcr:write on /\n"                    
                + "end"
                ;
            
        U.parseAndExecute(aclSetup);
        acl = AccessControlUtils.getAccessControlList(U.adminSession, "/");
    }

    @After
    public void cleanup() throws RepositoryException, RepoInitParsingException {
        U.cleanupUser();
    }

    @Test
    public void entryIsContained() throws Exception {
        
        // validates that an exact match of the username, privilege list and isAllow is contained
        
        assertIsContained(acl, U.username, new String[]{ Privilege.JCR_READ, Privilege.JCR_WRITE }, true);
    }

    @Test
    public void testDeclaredAggregatePrivilegesAreContained() throws Exception {
        String [] privileges = new String[]{
          //JCR_READ
                "rep:readNodes","rep:readProperties",
          // JCR_WRITE
                Privilege.JCR_ADD_CHILD_NODES,Privilege.JCR_MODIFY_PROPERTIES,
                Privilege.JCR_REMOVE_CHILD_NODES, Privilege.JCR_REMOVE_NODE
        };
        assertIsContained(acl, U.username, privileges, true);
    }

    @Test
    public void testAllAggregatePrivilegesAreContained() throws Exception {
        String [] privileges = new String[]{
                //JCR_READ
                "rep:readNodes","rep:readProperties",
                // JCR_WRITE
                Privilege.JCR_ADD_CHILD_NODES,"rep:addProperties",
                "rep:alterProperties","rep:removeProperties",
                Privilege.JCR_REMOVE_CHILD_NODES, Privilege.JCR_REMOVE_NODE
        };
        assertIsContained(acl, U.username, privileges, true);
    }

    
    @Test
    public void entryWithFewerPrivilegesIsContained() throws Exception {
        
        // validates that an exact match of the username and isAllow but with fewer privileges is contained
        assertIsContained(acl, U.username, new String[]{ Privilege.JCR_WRITE }, true);
    }

    @Test
    public void entryWithDifferentPrivilegeIsNotContained() throws Exception {
        // validates that an exact match of the username and isAllow but with different privileges is contained
        assertIsNotContained(acl, U.username, new String[]{ Privilege.JCR_ALL }, true);
    }
    
    @Test
    public void entryWithPartiallyMatchingPrivilegesIsContained() throws Exception {
        // validates that an exact match of the username and isAllow but with privileges partially overlapping is contained
        // existing: JCR_READ, JCR_WRITE 
        // new: JCR_READ, JCR_MODIFY_PROPERTIES
        assertIsContained(acl, U.username, new String[]{Privilege.JCR_READ, Privilege.JCR_MODIFY_PROPERTIES }, true);
    }    
    
    @Test
    public void entryWithDifferentUserIsNotContained() throws Exception {

        // validates that an exact match of the privileges and isAllow but with different username is not contained
        String otherPrincipalName = U.username + "_";
        try {
            U.parseAndExecute("create service user " + otherPrincipalName);
            assertIsNotContained(acl, otherPrincipalName, new String[]{ Privilege.JCR_READ, Privilege.JCR_WRITE }, true);
        } finally {
            U.cleanupServiceUser(otherPrincipalName);
        }
    }
    
    @Test
    public void entryWithDifferentIsAllowIsNotContained() throws Exception {
        // validates that an exact match of the username and privileges but with different is allow is not contained
        assertIsNotContained(acl, U.username, new String[]{ Privilege.JCR_READ, Privilege.JCR_WRITE }, false);
    }
    
    private void assertArrayCompare(Object[] a, Object[] b, boolean expected) {
        final boolean actual = AclUtil.compareArrays(a, b);
        assertEquals("Expecting compareArrays to return " + expected, actual, expected);
    }

    @Test
    public void compareArraysTest() {
        final String[] a1 = { "a", "b" };
        final String[] a2 = { "a", "b" };
        final String[] a3 = { "b", "c" };
        final String[] a4 = { "a", "b", "c" };
        final String[] a5 = { "b", "a" };
        final String[] a6 = { "b", "a", "c" };
        final String[] emptyA = {};
        final String[] emptyB = {};

        assertArrayCompare(null, null, true);
        assertArrayCompare(null, a1, false);
        assertArrayCompare(a2, null, false);
        assertArrayCompare(a1, a2, true);
        assertArrayCompare(a1, a3, false);
        assertArrayCompare(a3, a1, false);
        assertArrayCompare(a1, a3, false);
        assertArrayCompare(a1, a4, false);
        assertArrayCompare(a4, a4, true);
        assertArrayCompare(a1, a5, true);
        assertArrayCompare(a4, a6, true);
        assertArrayCompare(emptyA, emptyB, true);
    }

    private void assertIsContained(JackrabbitAccessControlList acl, String username, String[] privilegeNames, boolean isAllow) throws RepositoryException {
        assertIsContained0(acl, username, privilegeNames, isAllow, true);
    }
    
    private void assertIsNotContained(JackrabbitAccessControlList acl, String username, String[] privilegeNames, boolean isAllow) throws RepositoryException {
        assertIsContained0(acl, username, privilegeNames, isAllow, false);
    }
    
    private void assertIsContained0(JackrabbitAccessControlList acl, String username, String[] privilegeNames, boolean isAllow, boolean contained) throws RepositoryException {
        AclUtil.LocalAccessControlEntry localAce = new AclUtil.LocalAccessControlEntry(principal(username), privileges(privilegeNames), isAllow);
        
        if ( contained ) {
            assertTrue("ACL does not contain an entry for " + localAce, AclUtil.contains(acl.getAccessControlEntries(), localAce));    
        } else {
            assertFalse("ACL contains an entry for " + localAce, AclUtil.contains(acl.getAccessControlEntries(), localAce));
        }
        
    }

    private Principal principal(String principalName) throws RepositoryException {
        return AccessControlUtils.getPrincipal(U.adminSession, principalName);
    }

    private Privilege[] privileges(String... privilegeNames) throws RepositoryException {
        return AccessControlUtils.privilegesFromNames(U.adminSession, privilegeNames);
    }
}