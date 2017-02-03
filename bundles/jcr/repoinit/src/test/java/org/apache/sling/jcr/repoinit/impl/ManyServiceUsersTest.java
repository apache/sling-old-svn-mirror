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

import java.util.Arrays;
import java.util.UUID;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.apache.sling.repoinit.parser.RepoInitParsingException;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.fail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Created for SLING-6182 to verify that setAcl works right after
 *  creating a service user.
 */
public class ManyServiceUsersTest {

    @Rule
    public final SlingContext context = new SlingContext(ResourceResolverType.JCR_OAK);
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private static final String N_USERS_PROP = ManyServiceUsersTest.class.getSimpleName() + ".N_USERS";
    
    private TestUtil U;
    private static final int N_USERS = Integer.getInteger(N_USERS_PROP, 50);
    private String uniqueId;
    
    @Before
    public void setup() throws RepositoryException, RepoInitParsingException {
        U = new TestUtil(context);
        uniqueId = UUID.randomUUID().toString();
    }
    
    @After
    public void cleanup() {
        U.cleanup();
    }
    
    @Test
    public void testSyncServiceUserCreation() throws Exception {
        
        log.info("Creating {} service users (as set by the {} system property) and setting ACLs on them", 
                N_USERS, N_USERS_PROP);
        
        final Session s = U.getAdminSession();
        final Node testRoot = s.getRootNode().addNode(getClass().getSimpleName() + "_" + uniqueId);
        final String path = testRoot.getPath();
        s.save();
        
        try {
            for(int i=1; i < N_USERS; i++) {
                final String username = getClass().getSimpleName() + "_" + uniqueId + "_" + i;
                UserUtil.createServiceUser(s, username);
                U.assertServiceUser("Right after creation at index " + i, username, true);
                
                // Required for setAcl to work
                s.save();
                
                try {
                    AclUtil.setAcl(s, Arrays.asList(username), Arrays.asList(path), Arrays.asList("jcr:read"), true);
                } catch(Exception e) {
                    fail("SetAcl failed at index " + i + ": " + e);
                }
            }
        } finally {
            testRoot.remove();
            s.save();
        }
    }
}