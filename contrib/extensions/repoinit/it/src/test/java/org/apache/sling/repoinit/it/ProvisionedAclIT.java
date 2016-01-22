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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Dictionary;
import java.util.Hashtable;
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
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.junit.rules.TeleporterRule;
import org.apache.sling.repoinit.jcr.AclOperationVisitor;
import org.apache.sling.repoinit.parser.AclDefinitionsParser;
import org.apache.sling.repoinit.parser.operations.Operation;
import org.apache.sling.repoinit.parser.operations.OperationVisitor;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/** Test service users and ACLs set from a text file. */
public class ProvisionedAclIT {

    private Session session;
    private static final String FRED_WILMA = "fredWilmaService";
    private static final String ANOTHER = "anotherService";
    
    public static final String REPO_INIT_FILE = "/repoinit.txt";
    
    @Rule
    public TeleporterRule teleporter = TeleporterRule
        .forClass(getClass(), "IT")
        .withResources(REPO_INIT_FILE);
    
    @Before
    public void setup() throws Exception {
        WaitFor.services(teleporter, SlingRepository.class, AclDefinitionsParser.class);
        session = teleporter.getService(SlingRepository.class).loginAdministrative(null);
        
        // TODO this should be done by the repoinit language
        try {
            session.getRootNode().addNode("acltest").addNode("A").addNode("B").save();;
        } catch(RepositoryException ignore) {
        }
        assertTrue("Expecting test nodes to be created", session.itemExists("/acltest/A/B"));
        
        // Execute some repoinit statements
        final InputStream is = getClass().getResourceAsStream(REPO_INIT_FILE);
        assertNotNull("Expecting " + REPO_INIT_FILE, is);
        try {
            final AclDefinitionsParser parser = teleporter.getService(AclDefinitionsParser.class);
            final OperationVisitor v = new AclOperationVisitor(session);
            for(Operation op : parser.parse(new InputStreamReader(is, "UTF-8"))) {
                op.accept(v);
            }
            session.save();
        } finally {
            is.close();
        }
        
    }
    
    @After
    public void cleanup() {
        if(session != null) {
            session.logout();
        }
    }
    
    private boolean userExists(String id) throws LoginException, RepositoryException, InterruptedException {
        final Authorizable a = ((JackrabbitSession)session).getUserManager().getAuthorizable(id);
        return a != null;
    }
    
    private Session getServiceSession(String serviceId) throws LoginException, RepositoryException {
        return session.impersonate(new SimpleCredentials(serviceId, new char[0]));
    }
    
    /** True if user can write to specified path. 
     *  @throws PathNotFoundException if the path doesn't exist */ 
    private boolean canWrite(String userId, String path) throws PathNotFoundException,RepositoryException {
        if(!session.itemExists(path)) {
            throw new PathNotFoundException(path);
        }
        
        final Session serviceSession = getServiceSession(userId);
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
    
    @Test
    public void serviceUserCreated() throws Exception {
        new Retry() {
            @Override
            public Void call() throws Exception {
                assertTrue("Expecting user " + FRED_WILMA, userExists(FRED_WILMA));
                return null;
            }
        };
    }
    
    @Test
    public void fredWilmaAcl() throws Exception {
        new Retry() {
            @Override
            public Void call() throws Exception {
                assertFalse("Expecting no write access to A", canWrite(FRED_WILMA, "/acltest/A"));
                assertTrue("Expecting write access to A/B", canWrite(FRED_WILMA, "/acltest/A/B"));
                return null;
            }
        };
    }
    
    @Test
    public void anotherUserAcl() throws Exception {
        new Retry() {
            @Override
            public Void call() throws Exception {
                assertTrue("Expecting write access to A", canWrite(ANOTHER, "/acltest/A"));
                assertFalse("Expecting no write access to B", canWrite(ANOTHER, "/acltest/A/B"));
                return null;
            }
        };
    }
}
