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

import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.UUID;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.junit.rules.TeleporterRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class ImmediateAclIT {
    
    @Rule
    public TeleporterRule teleporter = TeleporterRule.forClass(getClass(), "IT");
    
    private final static String FACTORY_PID = "org.apache.sling.acldef.jcr.AclSetupComponent";
    private final static String ACLDEF_PROP_PREFIX = "acldef.text.";

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
    
    @Before
    public void waitForServices() {
        WaitFor.services(teleporter, ConfigurationAdmin.class, SlingRepository.class);
    }
    
    @Test
    public void createServiceUser() throws IOException, InterruptedException, LoginException, RepositoryException {
        // Create a config for the AclSetupComponent and verify that the
        // service user eventually gets created 
        final String id = "user_" + UUID.randomUUID().toString();
        final ConfigurationAdmin ca = teleporter.getService(ConfigurationAdmin.class);
        final Configuration cfg = ca.createFactoryConfiguration(FACTORY_PID);
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(ACLDEF_PROP_PREFIX + "1", "create service user " + id);
        cfg.setBundleLocation(null);
        cfg.update(props);
        
        final long maxTimeMsec = 10000;
        final long timeout = System.currentTimeMillis() + maxTimeMsec;
        while(System.currentTimeMillis() < timeout) {
            if(userExists(id)) {
                return;
            }
            Thread.sleep(100L);
        }
        fail("User " + id + " was not created after " + maxTimeMsec + " msec");
    }
}