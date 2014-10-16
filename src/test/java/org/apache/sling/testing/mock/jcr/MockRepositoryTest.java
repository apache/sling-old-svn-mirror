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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.junit.Before;
import org.junit.Test;

public class MockRepositoryTest {

    private static final String USER_NAME = "user";
    private static final char[] PASSWORD = "pwd".toCharArray();

    private Repository repository;

    @Before
    public void setUp() {
        repository = MockJcr.newRepository();
    }

    @Test
    public void testLogin() throws RepositoryException {
        assertNotNull(repository.login());
        assertNotNull(repository.login(new SimpleCredentials(USER_NAME, PASSWORD)));
        assertNotNull(repository.login(MockJcr.DEFAULT_WORKSPACE));
        assertNotNull(repository.login(new SimpleCredentials(USER_NAME, PASSWORD), MockJcr.DEFAULT_WORKSPACE));
    }

    @Test
    public void testDescriptor() {
        assertEquals(0, repository.getDescriptorKeys().length);
        assertNull(repository.getDescriptor("test"));
        assertNull(repository.getDescriptorValue("test"));
        assertNull(repository.getDescriptorValues("test"));
        assertFalse(repository.isStandardDescriptor("test"));
        assertFalse(repository.isSingleValueDescriptor("test"));
    }
    
    @Test
    public void testMultipleSessions() throws RepositoryException {
        Session session1 = repository.login();
        Session session2 = repository.login();

        // add a node in session 1
        Node root = session1.getRootNode();
        root.addNode("test");
        session1.save();
        
        // try to get node in session 2
        Node testNode2 = session2.getNode("/test");
        assertNotNull(testNode2);
        
        // delete node and make sure it is removed in session 1 as well
        testNode2.remove();
        session2.save();
        
        try {
            session1.getNode("/test");
            fail("Node was not removed");
        }
        catch (PathNotFoundException ex) {
            // expected
        }
    }

}
