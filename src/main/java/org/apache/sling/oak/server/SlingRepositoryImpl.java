/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.oak.server;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.apache.jackrabbit.oak.spi.security.OpenSecurityProvider;
import org.apache.jackrabbit.oak.spi.security.SecurityProvider;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.AbstractNamespaceMappingRepository;
import org.osgi.service.component.ComponentContext;

/**
 * A Sling repository implementation that wraps the Oak OSGi repository
 * implementation from the Oak project.
 */
@Component(immediate = true, metatype = true)
@Service(value = { SlingRepository.class, Repository.class })
public class SlingRepositoryImpl extends AbstractNamespaceMappingRepository
        implements SlingRepository {

    private Repository oakRepository;
    
    @Activate
    protected void activate(ComponentContext ctx) {
        final SecurityProvider sp = new OpenSecurityProvider();
        // TODO barebones setup for now...might not provide much functionality.
        // TODO for a simple config (tar persistence) we could use the SegmentNodeStoreService
        oakRepository = new Jcr().with(sp).createRepository();
    }
    
    @Override
    public String getDescriptor(String key) {
        return oakRepository.getDescriptor(key);
    }

    @Override
    public String[] getDescriptorKeys() {
        return oakRepository.getDescriptorKeys();
    }

    @Override
    public Session login() throws LoginException, RepositoryException {
        return oakRepository.login();
    }

    @Override
    public Session login(Credentials creds, String workspace) throws LoginException,
            NoSuchWorkspaceException, RepositoryException {
        return oakRepository.login(creds, workspace);
    }

    @Override
    public Session login(Credentials creds) throws LoginException,
            RepositoryException {
        return oakRepository.login(creds);
    }

    @Override
    public Session login(String workspace) throws LoginException,
            NoSuchWorkspaceException, RepositoryException {
        return oakRepository.login(workspace);
    }

    @Override
    public String getDefaultWorkspace() {
        return null;
    }

    @Override
    public Session loginAdministrative(String workspace) throws RepositoryException {
        // TODO use configurable credentials
        final SimpleCredentials creds = new SimpleCredentials("admin", "admin".toCharArray());
        return login(creds, workspace);
    }
}