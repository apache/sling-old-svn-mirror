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
package org.apache.sling.testing.mock.jcr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.jcr.RepositoryException;
import javax.jcr.Workspace;
import javax.jcr.observation.ObservationManager;

import org.junit.Before;
import org.junit.Test;

public class MockWorkspaceTest {

    private Workspace underTest;

    @Before
    public void setUp() {
        underTest = MockJcr.newSession().getWorkspace();
    }

    @Test
    public void testName() {
        assertEquals(MockJcr.DEFAULT_WORKSPACE, underTest.getName());
    }

    @Test
    public void testNameSpaceRegistry() throws RepositoryException {
        assertNotNull(underTest.getNamespaceRegistry());
    }

    @Test
    public void testObservationManager() throws RepositoryException {
        // just mage sure listener methods can be called, although they do
        // nothing
        ObservationManager observationManager = underTest.getObservationManager();
        observationManager.addEventListener(null, 0, null, false, null, null, false);
        observationManager.removeEventListener(null);
    }

    @Test
    public void testNodeTypeManager() throws RepositoryException {
        assertNotNull(underTest.getNodeTypeManager());
    }

}
