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
package org.apache.sling.testing.mock.sling;

import static org.junit.Assert.assertNotNull;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.testing.mock.jcr.MockJcr;
import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class MockJcrSlingRepositoryTest {

    @Rule
    public OsgiContext context = new OsgiContext();
    
    private SlingRepository repository;

    @Before
    public void setUp() {
        this.repository = context.registerInjectActivateService(new MockJcrSlingRepository());
    }
    
    @SuppressWarnings("deprecation")
    @Test
    public void testLoginAdministrative() throws RepositoryException {
        Session session = this.repository.loginAdministrative(MockJcr.DEFAULT_WORKSPACE);
        assertNotNull(session);
        session.logout();
    }

    @Test
    public void testLoginService() throws RepositoryException {
        Session session = this.repository.loginService("test", MockJcr.DEFAULT_WORKSPACE);
        assertNotNull(session);
        session.logout();
    }

}
