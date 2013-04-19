package org.apache.sling.hc.demo.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import javax.jcr.Session;

import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.junit.annotations.SlingAnnotationsTestRunner;
import org.apache.sling.junit.annotations.TestReference;
import org.junit.Test;
import org.junit.runner.RunWith;

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

/** Trivial test used to demonstrate the Muppet junit rules - verify
 *  that the SlingRepository is functional.
 */
@RunWith(SlingAnnotationsTestRunner.class)
public class RepositoryPresentTest {
    @TestReference
    private SlingRepository repository;
    
    @Test
    public void checkRepositoryPresent() {
        assertNotNull(repository);
    }
    
    @Test
    public void checkAdminLogin() {
        Session s = null;
        try {
            s = repository.loginAdministrative(null); 
            assertNotNull(s);
        } catch(Exception e) {
            fail("SlingRepository login failed: " + e);
        } finally {
            if(s != null) {
                s.logout();
            }
        }
    }
}
