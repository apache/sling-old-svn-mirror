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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.repoinit.parser.AclParsingException;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/** Various ACL-related tests */
public class GeneralAclTest {

    @Rule
    public final SlingContext context = new SlingContext(ResourceResolverType.JCR_OAK);
    
    private TestUtil U;
    private Session s;
    
    @Before
    public void setup() throws RepositoryException, AclParsingException {
        U = new TestUtil(context);
        U.parseAndExecute("create service user " + U.username);
        s = U.loginService(U.username);
    }

    @After
    public void cleanup() throws RepositoryException, AclParsingException {
        U.cleanupUser();
        s.logout();
    }
    
    @Test(expected=AccessDeniedException.class)
    public void getRootNodeIntiallyFails() throws Exception {
        s.getRootNode();
    }
    
    @Test
    public void readOnlyThenWriteThenDeny() throws Exception {
        final Node tmp = U.adminSession.getRootNode().addNode("tmp_" + U.id);
        U.adminSession.save();
        final String path = tmp.getPath();
        
        try {
            s.getNode(path);
            fail("Expected read access to be initially denied:" + path);
        } catch(PathNotFoundException ignore) {
        }
        
        final String allowRead =  
                "set ACL for " + U.username + "\n"
                + "allow jcr:read on " + path + "\n"
                + "end"
                ;
        U.parseAndExecute(allowRead);
        final Node n = s.getNode(path);
        
        try {
            n.setProperty("U.id", U.id);
            s.save();
            fail("Expected write access to be initially denied:" + path);
        } catch(AccessDeniedException ignore) {
        }
        s.refresh(false);
        
        final String allowWrite = 
                "set ACL for " + U.username + "\n"
                + "allow jcr:write on " + path + "\n"
                + "end"
                ;
        U.parseAndExecute(allowWrite);
        n.setProperty("U.id", U.id);
        s.save();
        
        final String deny = 
                "set ACL for " + U.username + "\n"
                + "deny jcr:all on " + path + "\n"
                + "end"
                ;
        U.parseAndExecute(deny);
        try {
            s.getNode(path);
            fail("Expected access to be denied again:" + path);
        } catch(PathNotFoundException ignore) {
        }
    }
    
    @Test
    public void addChildAtRoot() throws Exception {
        final String nodename = "test_" + U.id;
        final String path = "/" + nodename;
        
        final String aclSetup = 
            "set ACL for " + U.username + "\n"
            + "allow jcr:all on /\n"
            + "end"
            ;
        
        U.parseAndExecute(aclSetup);
        try {
            assertFalse(s.itemExists(path));
            s.getRootNode().addNode(nodename);
            s.save();
            assertTrue(s.nodeExists(path));
            s.getNode(path).remove();
            s.save();
            assertFalse(s.itemExists(path));
        } finally {
            s.logout();
        }
    }
}
