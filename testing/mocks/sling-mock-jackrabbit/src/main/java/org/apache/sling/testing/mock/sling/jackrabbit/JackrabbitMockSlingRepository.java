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
package org.apache.sling.testing.mock.sling.jackrabbit;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.testing.jcr.RepositoryProvider;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.ComponentContext;

@Component
@Service(SlingRepository.class)
public final class JackrabbitMockSlingRepository implements SlingRepository {

    private SlingRepository delegate;

    @Activate
    protected void activate(ComponentContext componentContext) {
        try {
            this.delegate = RepositoryProvider.instance().getRepository();
        }
        catch (RepositoryException ex) {
            throw new RuntimeException("Unable to get jackrabbit SlingRepository instance.", ex);
        }
    }
    
    public String getDefaultWorkspace() {
        return delegate.getDefaultWorkspace();
    }

    @SuppressWarnings("deprecation")
    public Session loginAdministrative(String workspace) throws LoginException, RepositoryException {
        return delegate.loginAdministrative(workspace);
    }

    public Session loginService(String subServiceName, String workspace) throws LoginException, RepositoryException {
        // fallback to loginAdministrative
        return loginAdministrative(workspace);
    }

    public String[] getDescriptorKeys() {
        return delegate.getDescriptorKeys();
    }

    public boolean isStandardDescriptor(String key) {
        return delegate.isStandardDescriptor(key);
    }

    public boolean isSingleValueDescriptor(String key) {
        return delegate.isSingleValueDescriptor(key);
    }

    public Value getDescriptorValue(String key) {
        return delegate.getDescriptorValue(key);
    }

    public Value[] getDescriptorValues(String key) {
        return delegate.getDescriptorValues(key);
    }

    public String getDescriptor(String key) {
        return delegate.getDescriptor(key);
    }

    public Session login(Credentials credentials, String workspaceName) throws LoginException,
            NoSuchWorkspaceException, RepositoryException {
        return delegate.login(credentials, workspaceName);
    }

    public Session login(Credentials credentials) throws LoginException, RepositoryException {
        return delegate.login(credentials);
    }

    public Session login(String workspaceName) throws LoginException, NoSuchWorkspaceException, RepositoryException {
        return delegate.login(workspaceName);
    }

    public Session login() throws LoginException, RepositoryException {
        return delegate.login();
    }
    
}
