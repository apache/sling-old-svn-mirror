package org.apache.sling.muppet.demo.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
 *  the existence of some resources. One of the tests is designed to fail.
 */
@RunWith(SlingAnnotationsTestRunner.class)
public class ResourcesExistTest {
    @TestReference
    private SlingRepository repository;
    
    private void assertResource(String path) throws Exception {
        Session s = null;
        try {
            s = repository.loginAdministrative(null); 
            assertNotNull(s);
            assertTrue("Expecting path " + path + " to exist", s.itemExists(path));
        } finally {
            if(s != null) {
                s.logout();
            }
        }
    }
    
    @Test
    public void checkAppsExist() throws Exception {
        assertResource("/apps");
    }
    
    @Test
    public void checkNonExistentPath() throws Exception {
        assertResource("/NON_EXISTENT/" + System.currentTimeMillis());
    }
}
