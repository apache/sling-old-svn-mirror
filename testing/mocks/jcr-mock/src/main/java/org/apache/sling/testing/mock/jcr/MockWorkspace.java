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

import java.io.InputStream;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.lock.LockManager;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.observation.ObservationManager;
import javax.jcr.query.QueryManager;
import javax.jcr.version.Version;
import javax.jcr.version.VersionManager;

import org.xml.sax.ContentHandler;

/**
 * Mock {@link Workspace} implementation
 */
class MockWorkspace implements Workspace {

    private final MockRepository repository;
    private final Session session;
    private final String workspaceName;
    private final QueryManager queryManager = new MockQueryManager();

    /**
     * @param session JCR session
     */
    public MockWorkspace(MockRepository repository, Session session, String workspaceName) {
        this.repository = repository;
        this.session = session;
        this.workspaceName = workspaceName;
    }

    @Override
    public Session getSession() {
        return this.session;
    }

    @Override
    public String getName() {
        return this.workspaceName;
    }

    @Override
    public NamespaceRegistry getNamespaceRegistry() throws RepositoryException {
        return repository.getNamespaceRegistry();
    }

    @Override
    public ObservationManager getObservationManager() throws RepositoryException {
        return repository.getObservationManager();
    }

    @Override
    public NodeTypeManager getNodeTypeManager() throws RepositoryException {
        return repository.getNodeTypeManager();
    }

    @Override
    public QueryManager getQueryManager() throws RepositoryException {
        return this.queryManager;
    }
    
    // --- unsupported operations ---
    @Override
    public void copy(final String srcAbsPath, final String destAbsPath) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void copy(final String srcWorkspace, final String srcAbsPath, final String destAbsPath)
            throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clone(final String srcWorkspace, final String srcAbsPath, final String destAbsPath,
            final boolean removeExisting) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void move(final String srcAbsPath, final String destAbsPath) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void restore(final Version[] versions, final boolean removeExisting) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public LockManager getLockManager() throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public VersionManager getVersionManager() throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] getAccessibleWorkspaceNames() throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ContentHandler getImportContentHandler(final String parentAbsPath, final int uuidBehavior) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void importXML(final String parentAbsPath, final InputStream in, final int uuidBehavior) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void createWorkspace(final String name) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void createWorkspace(final String name, final String srcWorkspace) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteWorkspace(final String name) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

}
