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

import javax.jcr.Session;

import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.repoinit.JcrRepoInitOpsProcessor;
import org.apache.sling.junit.rules.TeleporterRule;
import org.apache.sling.repoinit.parser.RepoInitParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/** Test service users and ACLs set from a text file. */
public class RepoInitIT {

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
        WaitFor.services(teleporter, SlingRepository.class, RepoInitParser.class);
        session = teleporter.getService(SlingRepository.class).loginAdministrative(null);
        
        // Execute some repoinit statements
        final InputStream is = getClass().getResourceAsStream(REPO_INIT_FILE);
        assertNotNull("Expecting " + REPO_INIT_FILE, is);
        try {
            final RepoInitParser  parser = teleporter.getService(RepoInitParser.class);
            final JcrRepoInitOpsProcessor processor = teleporter.getService(JcrRepoInitOpsProcessor.class);
            processor.apply(session, parser.parse(new InputStreamReader(is, "UTF-8")));
            session.save();
        } finally {
            is.close();
        }
        
        // The repoinit file causes those nodes to be created
        assertTrue("Expecting test nodes to be created", session.itemExists("/acltest/A/B"));
    }
    
    @After
    public void cleanup() {
        if(session != null) {
            session.logout();
        }
    }
    
    @Test
    public void serviceUserCreated() throws Exception {
        new Retry() {
            @Override
            public Void call() throws Exception {
                assertTrue("Expecting user " + FRED_WILMA, U.userExists(session, FRED_WILMA));
                return null;
            }
        };
    }
    
    @Test
    public void fredWilmaAcl() throws Exception {
        new Retry() {
            @Override
            public Void call() throws Exception {
                assertFalse("Expecting no write access to A", U.canWrite(session, FRED_WILMA, "/acltest/A"));
                assertTrue("Expecting write access to A/B", U.canWrite(session, FRED_WILMA, "/acltest/A/B"));
                return null;
            }
        };
    }
    
    @Test
    public void anotherUserAcl() throws Exception {
        new Retry() {
            @Override
            public Void call() throws Exception {
                assertTrue("Expecting write access to A", U.canWrite(session, ANOTHER, "/acltest/A"));
                assertFalse("Expecting no write access to B", U.canWrite(session, ANOTHER, "/acltest/A/B"));
                return null;
            }
        };
    }
}
