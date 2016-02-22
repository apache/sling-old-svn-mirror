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
package org.apache.sling.testing.mock.sling;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.testing.mock.jcr.MockJcr;
import org.osgi.service.component.ComponentContext;

/**
 * Mock {@link SlingRepository} implementation.
 */
@Component
@Service(SlingRepository.class)
public final class MockJcrSlingRepository implements SlingRepository {

    private Repository repository;
    
    @Activate
    protected void activate(ComponentContext componentContext) {
        repository = MockJcr.newRepository();
    }

    @Override
    public Session loginAdministrative(final String workspaceName) throws RepositoryException {
        return login(workspaceName);
    }

    @Override
    public Session loginService(final String subServiceName, final String workspaceName)
            throws LoginException, RepositoryException {
        return login(workspaceName);
    }

    @Override
    public String getDefaultWorkspace() {
        return MockJcr.DEFAULT_WORKSPACE;
    }

    // delegated methods
    @Override
    public String[] getDescriptorKeys() {
        return this.repository.getDescriptorKeys();
    }

    @Override
    public boolean isStandardDescriptor(final String key) {
        return this.repository.isStandardDescriptor(key);
    }

    @Override
    public boolean isSingleValueDescriptor(final String key) {
        return this.repository.isSingleValueDescriptor(key);
    }

    @Override
    public Value getDescriptorValue(final String key) {
        return this.repository.getDescriptorValue(key);
    }

    @Override
    public Value[] getDescriptorValues(final String key) {
        return this.repository.getDescriptorValues(key);
    }

    @Override
    public String getDescriptor(final String key) {
        return this.repository.getDescriptor(key);
    }

    @Override
    public Session login(final Credentials credentials, final String workspaceName) throws LoginException,
            NoSuchWorkspaceException, RepositoryException {
        return this.repository.login(credentials, workspaceName);
    }

    @Override
    public Session login(final Credentials credentials) throws LoginException, RepositoryException {
        return this.repository.login(credentials);
    }

    @Override
    public Session login(final String workspaceName) throws LoginException, NoSuchWorkspaceException,
            RepositoryException {
        return this.repository.login(workspaceName);
    }

    @Override
    public Session login() throws LoginException, RepositoryException {
        return this.repository.login();
    }

}
