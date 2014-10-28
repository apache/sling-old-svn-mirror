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

import java.util.LinkedHashMap;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Mock {@link Repository} implementation. The data is stored inside the mocked
 * session, not the repository - so it is not possible to open multiple session
 * to access the same data in this mock implementation.
 */
class MockRepository implements Repository {

    // Use linked hashmap to ensure ordering when adding items is preserved.
    private final Map<String, ItemData> items = new LinkedHashMap<String, ItemData>();
    
    public MockRepository() {
        this.items.put("/", ItemData.newNode("/", MockNodeTypes.NT_UNSTRUCTURED));
    }
    
    @Override
    public Session login() {
        return login(null, null);
    }

    @Override
    public Session login(final String workspaceName) {
        return login(null, workspaceName);
    }

    @Override
    public Session login(final Credentials credentials) {
        return login(credentials, null);
    }

    @Override
    public Session login(final Credentials credentials, final String workspaceName) {
        String userId = null;
        if (credentials instanceof SimpleCredentials) {
            userId = ((SimpleCredentials)credentials).getUserID();
        }
        return new MockSession(this, items,
                StringUtils.defaultString(userId, MockJcr.DEFAULT_USER_ID),
                StringUtils.defaultString(workspaceName, MockJcr.DEFAULT_WORKSPACE));
    }

    @Override
    public String[] getDescriptorKeys() {
        return ArrayUtils.EMPTY_STRING_ARRAY;
    }

    @Override
    public boolean isStandardDescriptor(final String key) {
        return false;
    }

    @Override
    public boolean isSingleValueDescriptor(final String key) {
        return false;
    }

    @Override
    public Value getDescriptorValue(final String key) {
        return null;
    }

    @Override
    public Value[] getDescriptorValues(final String key) { // NOPMD
        return null; // NOPMD
    }

    @Override
    public String getDescriptor(final String key) {
        return null;
    }

}
