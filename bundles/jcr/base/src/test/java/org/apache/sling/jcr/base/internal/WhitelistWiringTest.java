/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.jcr.base.internal;

import static org.apache.sling.jcr.base.MockSlingRepositoryManager.WHITELIST_ALL;
import static org.apache.sling.jcr.base.MockSlingRepositoryManager.WHITELIST_NONE;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.AbstractSlingRepository2;
import org.apache.sling.jcr.base.MockSlingRepositoryManager;
import org.apache.sling.testing.mock.jcr.MockJcr;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/** Verify that the AbstractSlingRepository2 uses the login admin whitelist,
 *  as well as its combination with the global "disable login admin" flag
 */
@RunWith(Parameterized.class)
public class WhitelistWiringTest {

    private SlingRepository repository;

    private final boolean managerAllowsLoginAdmin;
    private final boolean whitelistAllowsLoginAdmin;
    private final boolean loginAdminExpected;
 
    @Parameters(name="manager {0}, whitelist {1} -> {2}")
    public static Collection<Object[]> data() {
        final List<Object[]> result = new ArrayList<Object[]>();
        result.add(new Object[] { false, false, false });
        result.add(new Object[] { false, true, false });
        result.add(new Object[] { true, false, false });
        result.add(new Object[] { true, true, true});
        return result;
    }

    public WhitelistWiringTest(boolean managerAllowsLoginAdmin, boolean whitelistAllowsLoginAdmin, boolean loginAdminExpected) {
        this.managerAllowsLoginAdmin = managerAllowsLoginAdmin;
        this.whitelistAllowsLoginAdmin = whitelistAllowsLoginAdmin;
        this.loginAdminExpected = loginAdminExpected;
    }
    
    @Before
    public void setup() throws Exception  {
        BundleContext bundleContext = MockOsgi.newBundleContext();
        Bundle bundle = bundleContext.getBundle();

        String whitelist = whitelistAllowsLoginAdmin ? WHITELIST_ALL : WHITELIST_NONE;

        final MockSlingRepositoryManager repoMgr =
                new MockSlingRepositoryManager(MockJcr.newRepository(), !managerAllowsLoginAdmin, whitelist);

        repoMgr.activate(bundleContext);
        
        repository = new AbstractSlingRepository2(repoMgr, bundle) {
            @Override
            protected Session createAdministrativeSession(String workspace) throws RepositoryException {
                return Mockito.mock(Session.class);
            }
        };
    }
    
    @Test
    @SuppressWarnings("deprecation")
    public void testLoginAdmin() throws Exception {
        boolean allowed = false;
        try {
            repository.loginAdministrative(null);
            allowed = true;
        } catch(LoginException ignored) {
            allowed = false;
        }
        
        assertEquals(loginAdminExpected, allowed);
    }
}