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
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.apache.jackrabbit.oak.plugins.commit.ConflictValidatorProvider;
import org.apache.jackrabbit.oak.plugins.commit.JcrConflictHandler;
import org.apache.jackrabbit.oak.plugins.index.lucene.LuceneIndexEditorProvider;
import org.apache.jackrabbit.oak.plugins.index.lucene.LuceneIndexProvider;
import org.apache.jackrabbit.oak.plugins.index.nodetype.NodeTypeIndexProvider;
import org.apache.jackrabbit.oak.plugins.index.property.PropertyIndexEditorProvider;
import org.apache.jackrabbit.oak.plugins.index.property.PropertyIndexProvider;
import org.apache.jackrabbit.oak.plugins.name.NameValidatorProvider;
import org.apache.jackrabbit.oak.plugins.name.NamespaceValidatorProvider;
import org.apache.jackrabbit.oak.plugins.nodetype.RegistrationEditorProvider;
import org.apache.jackrabbit.oak.plugins.nodetype.TypeEditorProvider;
import org.apache.jackrabbit.oak.plugins.nodetype.write.InitialContent;
import org.apache.jackrabbit.oak.plugins.version.VersionEditorProvider;
import org.apache.jackrabbit.oak.spi.commit.EditorHook;
import org.apache.jackrabbit.oak.spi.security.OpenSecurityProvider;
import org.apache.jackrabbit.oak.spi.security.SecurityProvider;
import org.apache.jackrabbit.oak.spi.state.NodeStore;
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
    
    @Reference
    private NodeStore nodeStore;
    
    @Activate
    protected void activate(ComponentContext ctx) {
        // TODO OpenSecurityProvider does not check anything, should use 
        // at least a SecurityProviderImpl, but that doesn't work with oak 0.8
        // (LoginModule class not found)
        final SecurityProvider sp = new OpenSecurityProvider();
        
        oakRepository = new Jcr(new Oak(nodeStore))
        .with(sp)
        .with(new InitialContent())

        .with(JcrConflictHandler.JCR_CONFLICT_HANDLER)
        .with(new EditorHook(new VersionEditorProvider()))

        .with(new NameValidatorProvider())
        .with(new NamespaceValidatorProvider())
        .with(new TypeEditorProvider())
        .with(new RegistrationEditorProvider())
        .with(new ConflictValidatorProvider())

        // index stuff
        .with(new PropertyIndexEditorProvider())

        .with(new PropertyIndexProvider())
        .with(new NodeTypeIndexProvider())

        .with(new LuceneIndexEditorProvider())
        .with(new LuceneIndexProvider())

        //.withAsyncIndexing() // TODO oak 0.9?
        .createRepository();
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