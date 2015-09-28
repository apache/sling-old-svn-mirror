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
package org.apache.sling.testing.mock.sling.oak;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;

import org.apache.sling.jcr.api.SlingRepository;

public final class RepositoryWrapper implements SlingRepository {

    private static final String ADMIN_NAME = "admin";
    private static final String ADMIN_PASSWORD = "admin";

    protected final Repository wrapped;

    public RepositoryWrapper(Repository r) {
        wrapped = r;
    }

    public String getDescriptor(String key) {
        return wrapped.getDescriptor(key);
    }

    public String[] getDescriptorKeys() {
        return wrapped.getDescriptorKeys();
    }

    public String getDefaultWorkspace() {
        return "default";
    }

    public Session login() throws LoginException, RepositoryException {
        return wrapped.login();
    }

    public Session login(Credentials credentials, String workspaceName) 
            throws LoginException, NoSuchWorkspaceException, RepositoryException {
        return wrapped.login(credentials, (workspaceName == null ? getDefaultWorkspace() : workspaceName));
    }

    public Session login(Credentials credentials) 
            throws LoginException, RepositoryException {
        return wrapped.login(credentials);
    }

    public Session login(String workspaceName) 
            throws LoginException, NoSuchWorkspaceException, RepositoryException {
        return wrapped.login((workspaceName == null ? getDefaultWorkspace() : workspaceName));
    }

    public Session loginAdministrative(String workspaceName) 
            throws RepositoryException {
        final Credentials credentials = new SimpleCredentials(ADMIN_NAME, ADMIN_PASSWORD.toCharArray());
        return this.login(credentials, (workspaceName == null ? getDefaultWorkspace() : workspaceName));
    }

    @Override
    public Session loginService(String subServiceName, String workspaceName) 
            throws LoginException, RepositoryException {
        return loginAdministrative(workspaceName);
    }
    
    public Value getDescriptorValue(String key) {
        return wrapped.getDescriptorValue(key);
    }

    public Value[] getDescriptorValues(String key) {
        return wrapped.getDescriptorValues(key);
    }

    public boolean isSingleValueDescriptor(String key) {
        return wrapped.isSingleValueDescriptor(key);
    }

    public boolean isStandardDescriptor(String key) {
        return wrapped.isStandardDescriptor(key);
    }

}