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
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import javax.jcr.Session;

import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.junit.rules.TeleporterRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/** Test repoinit statements supplied by our provisioning model */
public class ProvisioningModelIT {

    private Session session;
    private static final String TEST_PATH = "/repoinit/fromProvisioningModel";
    private static final String TEST_USER = "userFromProvisioningModel";
    private static final String SECOND_TEST_USER = "secondUserFromProvisioningModel";
    private final String uniqueID = UUID.randomUUID().toString();
    
    @Rule
    public TeleporterRule teleporter = TeleporterRule.forClass(getClass(), "IT");
    
    @Before
    public void setup() throws Exception {
        session = teleporter.getService(SlingRepository.class).loginAdministrative(null);
    }
    
    @After
    public void cleanup() {
        if(session != null) {
            session.logout();
        }
    }
    
    @Test
    public void usersCreated() throws Exception {
        assertTrue("Expecting user " + TEST_USER, U.userExists(session, TEST_USER));
        assertTrue("Expecting user " + SECOND_TEST_USER, U.userExists(session, SECOND_TEST_USER));
    }
    
    @Test
    public void userAclSet() throws Exception {
        assertTrue("Expecting read access", U.canRead(session, TEST_USER, TEST_PATH));
        assertFalse("Expecting no write access",  U.canWrite(session, TEST_USER, TEST_PATH));
    }
    
    @Test
    public void namespaceAndCndRegistered() throws Exception {
        final String nodeName = "ns-" + uniqueID;
        session.getRootNode().addNode(nodeName, "slingtest:unstructured");
        session.save();
    }
}
