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
package org.apache.sling.acldef.it;

import static org.junit.Assert.assertTrue;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.junit.rules.TeleporterRule;
import org.junit.Rule;
import org.junit.Test;

/** Test service users and ACLs set from
 *  our provisioning model. 
 *  TODO test /var ACLs and use @Retry rule
 */
public class ProvisionedAclIT {
    
    @Rule
    public TeleporterRule teleporter = TeleporterRule.forClass(getClass(), "IT");
    
    private boolean userExists(String id) throws LoginException, RepositoryException, InterruptedException {
        final Session s = teleporter.getService(SlingRepository.class).loginAdministrative(null);
        
        try {
            final Authorizable a = ((JackrabbitSession)s).getUserManager().getAuthorizable(id);
            if(a != null) {
                return true;
            }
        } finally {
            s.logout();
        }
        return false;
    }
    
    @Test
    public void serviceUserCreated() throws Exception {
        final String id = "fredWilma";
        new Retry() {
            @Override
            public Void call() throws Exception {
                assertTrue("Expecting user " + id, userExists(id));
                return null;
            }
        };
    }
}