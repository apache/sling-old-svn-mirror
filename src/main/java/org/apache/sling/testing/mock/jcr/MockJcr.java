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

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Factory for mock JCR objects.
 */
public final class MockJcr {

    /**
     * Default workspace name
     */
    public static final String DEFAULT_WORKSPACE = "mockedWorkspace";

    private MockJcr() {
        // static methods only
    }

    /**
     * Create a new mocked in-memory JCR repository. Beware: each session has
     * its own data store.
     * @return JCR repository
     */
    public static Repository newRepository() {
        return new MockRepository();
    }

    /**
     * Create a new mocked in-memory JCR session. It contains only the root
     * node. All data of the session is thrown away if it gets garbage
     * collected.
     * @return JCR session
     */
    public static Session newSession() {
        try {
            return newRepository().login();
        } catch (RepositoryException ex) {
            throw new RuntimeException("Creating mocked JCR session failed.", ex);
        }
    }

}
