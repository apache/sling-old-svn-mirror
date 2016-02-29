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
package org.apache.sling.launchpad.webapp.integrationtest.repository;

import static org.junit.Assert.fail;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.junit.rules.TeleporterRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

/** Verify that required system users have been created */
@Ignore("TODO reactivate once jcr.base 2.3.2 is released")
public class SystemUsersTest {

    @Rule
    public final TeleporterRule teleporter = TeleporterRule.forClass(getClass(), "Launchpad");
    
    private void assertSystemUser(String name) throws RepositoryException {
        final SlingRepository repo = teleporter.getService(SlingRepository.class);
        final Session s = repo.loginAdministrative(null);
        try {
            final Credentials creds = new SimpleCredentials(name, new char[] {});
            try {
                s.impersonate(creds);
            } catch(RepositoryException rex) {
                fail("Impersonation as " + name + " failed: " + rex.toString());
            }
        } finally { 
            s.logout();
        }
    }
    
    @Test
    public void launchpadTestingUser() throws RepositoryException {
        // This user is created by a RepositoryInitalizer in our
        // test-services bundle
        assertSystemUser("launchpad_testing");
    }
}