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

import static org.junit.Assert.assertTrue;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.junit.rules.TeleporterRule;
import org.junit.Rule;
import org.junit.Test;

/** Verify that a path is created by repoinit statements
 *  from our provisioning model.
 */
public class RepoinitPathTest {

    @Rule
    public final TeleporterRule teleporter = TeleporterRule.forClass(getClass(), "Launchpad");

    @Test
    public void pathExists() throws RepositoryException {
        final SlingRepository repo = teleporter.getService(SlingRepository.class);
        final Session s = repo.loginAdministrative(null);
        try {
            assertTrue(s.nodeExists("/repoinit/provisioningModelTest"));
        } finally { 
            s.logout();
        }
    }
}