/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.jcr.jackrabbit.accessmanager.impl;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;

import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.jcr.jackrabbit.accessmanager.impl.PrivilegesHelper;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class PrivilegesHelperTest {

    @Rule
    public final SlingContext context = new SlingContext(ResourceResolverType.JCR_OAK);
    
    private Map<Privilege, Set<Privilege>> privilegeToAncestorMap;
    private Session session;
    private AccessControlManager acm;

    @Before
    public void buildPrivilegesMap() throws Exception {
        
        session = context.resourceResolver().adaptTo(Session.class);
        privilegeToAncestorMap = PrivilegesHelper.buildPrivilegeToAncestorMap(session, "/");
        acm = AccessControlUtil.getAccessControlManager(session);
    }
    
    @Test
    public void mergeAddsMissingPrivilege() throws Exception {
        
        Privilege write = priv(Privilege.JCR_WRITE);
        Privilege read = priv(Privilege.JCR_READ);
        
        Set<Privilege> allowed = new HashSet<>();
        allowed.add(read);
        Set<Privilege> denied = new HashSet<>();

        PrivilegesHelper.mergePrivilegeSets(write, privilegeToAncestorMap, allowed, denied);

        assertThat(allowed, hasItems(read, write));
        assertThat(allowed.size(), equalTo(2));
        
        assertThat(denied.size(), equalTo(0));
    }


    private Privilege priv(String privilegeName) throws RepositoryException {
        return acm.privilegeFromName(privilegeName);
    }

    @Test
    public void mergeRemovesExistingDeniedPrivilege() throws Exception {
        
        Privilege write = priv(Privilege.JCR_WRITE);
        
        Set<Privilege> allowed = new HashSet<>();
        Set<Privilege> denied = new HashSet<>();
        denied.add(write);

        PrivilegesHelper.mergePrivilegeSets(write, privilegeToAncestorMap, allowed, denied);

        assertThat(allowed, hasItem(write));
        assertThat(allowed.size(), equalTo(1));
        
        assertThat(denied.size(), equalTo(0));
    }    
    
    @Test
    public void mergeAggregateOverlappingPrivilegesOnBothSides() throws Exception {

        Privilege all = priv(Privilege.JCR_ALL);
        Privilege write = priv(Privilege.JCR_WRITE);
        
        Set<Privilege> allowed = new HashSet<>();
        Set<Privilege> denied = new HashSet<>();
        denied.add(all);

        PrivilegesHelper.mergePrivilegeSets(write, privilegeToAncestorMap, allowed, denied);

        assertThat(allowed, hasItem(write));
        assertThat(allowed.size(), equalTo(1));
        
        assertThat(denied, hasItem(priv(Privilege.JCR_READ)));
        assertThat(denied, not(hasItem(priv(Privilege.JCR_MODIFY_PROPERTIES))));
    }
    
    @Test
    public void mergeAggregateNonOverlappingPrivilegesOnBothSides() throws Exception {
        
        Privilege read = priv(Privilege.JCR_READ);
        Privilege write = priv(Privilege.JCR_WRITE);
        
        assertTrue(read.isAggregate());
        assertTrue(write.isAggregate());
        
        Set<Privilege> allowed = new HashSet<>();
        Set<Privilege> denied = new HashSet<>();
        denied.add(write);
        
        PrivilegesHelper.mergePrivilegeSets(read, privilegeToAncestorMap, allowed, denied);
        
        assertThat(allowed, hasItem(read));
        assertThat(allowed.size(), equalTo(1));
        
        assertThat(denied, hasItem(write));
        assertThat(denied.size(), equalTo(1));
    }
    
    /**
     * Validates that two identical privileges are merged
     */
    @Test
    public void mergeIdenticalPrivileges() throws Exception {
        
        Privilege read = priv(Privilege.JCR_READ);
        
        Set<Privilege> allowed = new HashSet<>();
        allowed.add(read);
        
        Set<Privilege> denied = new HashSet<>();
        
        PrivilegesHelper.mergePrivilegeSets(read, privilegeToAncestorMap, allowed, denied);
        
        assertThat(allowed, hasItem(read));
        assertThat(allowed.size(), equalTo(1));
        
        assertThat(denied.size(), equalTo(0));
    }
    
    /**
     * 
     * Validates that the <tt>jcr:modifyProperties</tt> is recognized as being aggregated into <tt>jcr:write</tt>
     */
    @Test
    public void mergeAggregatePrivileges() throws Exception {
        
        Privilege write = priv(Privilege.JCR_WRITE);
        Privilege modifyProps = priv(Privilege.JCR_MODIFY_PROPERTIES);
        
        Set<Privilege> allowed = new HashSet<>();
        allowed.add(write);
        
        Set<Privilege> denied = new HashSet<>();
        
        PrivilegesHelper.mergePrivilegeSets(modifyProps, privilegeToAncestorMap, allowed, denied);
        
        assertThat(allowed, hasItem(write));
        assertThat(allowed.size(), equalTo(1));
        
        assertThat(denied.size(), equalTo(0));
    }
    
    /**
     * Validates that when negating <tt>jcr:modifyProperties</tt> out of <tt>jcr:write</tt> the correct individual
     * privileges are reported
     */
    @Test
    public void mergeRemoveAggregatePrivileges() throws Exception {
        
        Privilege write = priv(Privilege.JCR_WRITE);
        Privilege modifyProps = priv(Privilege.JCR_MODIFY_PROPERTIES);
        
        Set<Privilege> denied = new HashSet<>();
        
        Set<Privilege> second = new HashSet<>();
        second.add(write);
        
        PrivilegesHelper.mergePrivilegeSets(modifyProps, privilegeToAncestorMap, denied, second);
        
        assertThat(denied, hasItem(modifyProps));
        assertThat(denied.size(), equalTo(1));
        
        assertThat(second, hasItems(priv(Privilege.JCR_ADD_CHILD_NODES), priv(Privilege.JCR_REMOVE_CHILD_NODES), priv(Privilege.JCR_REMOVE_NODE)));
        assertThat(second.size(), equalTo(3));
    }

}
