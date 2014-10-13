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

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;

import org.junit.Before;
import org.junit.Test;

public class MockRepositoryTest {

    private static final String USER_NAME = "user";
    private static final char[] PASSWORD = "pwd".toCharArray();

    private Repository repository;

    @Before
    public void setUp() {
        this.repository = MockJcr.newRepository();
    }

    @Test
    public void testLogin() throws RepositoryException {
        assertNotNull(this.repository.login());
        assertNotNull(this.repository.login(new SimpleCredentials(USER_NAME, PASSWORD)));
        assertNotNull(this.repository.login(MockJcr.DEFAULT_WORKSPACE));
        assertNotNull(this.repository.login(new SimpleCredentials(USER_NAME, PASSWORD), MockJcr.DEFAULT_WORKSPACE));
    }

    @Test
    public void testDescriptor() {
        assertEquals(0, this.repository.getDescriptorKeys().length);
        assertNull(this.repository.getDescriptor("test"));
        assertNull(this.repository.getDescriptorValue("test"));
        assertNull(this.repository.getDescriptorValues("test"));
        assertFalse(this.repository.isStandardDescriptor("test"));
        assertFalse(this.repository.isSingleValueDescriptor("test"));
    }

}
