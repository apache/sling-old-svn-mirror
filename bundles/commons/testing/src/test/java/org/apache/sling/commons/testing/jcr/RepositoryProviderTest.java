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
package org.apache.sling.commons.testing.jcr;

import static org.junit.Assert.assertNotNull;

import javax.jcr.Session;

import org.apache.sling.jcr.api.SlingRepository;
import org.junit.Before;
import org.junit.Test;

/** JUnit 4 style RepositoryProvider test */
public class RepositoryProviderTest {
    private SlingRepository repo;
    
    @Before
    public void getRepo() throws Exception {
        repo = RepositoryProvider.instance().getRepository();
    }
    
    @Test 
    public void testRepository() throws Exception {
        assertNotNull("Expecting SlingRepository to be setup", repo);
    }
    
    @Test 
    public void testRootNode() throws Exception {
        final Session s = repo.loginAdministrative(null);
        try {
            assertNotNull("Expecting a non-null Session", s);
        } finally {
            s.logout();
        }
    }
}
