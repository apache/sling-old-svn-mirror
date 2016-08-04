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

import static org.junit.Assert.assertEquals;

import org.apache.sling.commons.testing.integration.HttpTest;
import org.apache.sling.launchpad.webapp.integrationtest.util.RepositoryTestUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Verify the repository name when running on Oak.
 */
public class RepositoryNameTest {

    private final HttpTest H = new HttpTest();
    
    @Before
    public void setup() throws Exception {
        H.setUp();
    }
    
    @After
    public void cleanup() throws Exception {
        H.tearDown();
    }
    
    @Test
    public void checkOakName() throws Exception {
        RepositoryTestUtil.logDescriptors(H,  "jcr.repository.name", "jcr.repository.version");
        assertEquals("Apache Jackrabbit Oak", RepositoryTestUtil.getDescriptor(H, "jcr.repository.name"));
    }
}