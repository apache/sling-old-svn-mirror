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

    private final Session session;
    private final NamespaceRegistry namespaceRegistry = new MockNamespaceRegistry();
    private final ObservationManager observationManager = new MockObservationManager();
    private final NodeTypeManager nodeTypeManager = new MockNodeTypeManager();

    /**
     * @param session JCR session
     */
    public MockWorkspace(final Session session) {
        this.session = session;
    }

    @Override
    public Session getSession() {
        return this.session;
    }

    @Override
    public String getName() {
        return MockJcr.DEFAULT_WORKSPACE;
    }

    @Override
    public NamespaceRegistry getNamespaceRegistry() {
        return this.namespaceRegistry;
    }

    @Override
    public ObservationManager getObservationManager() {
        return this.observationManager;
    }

    @Override
    public NodeTypeManager getNodeTypeManager() {
        return this.nodeTypeManager;
    }

    // --- unsupported operations ---
    @Override
    public void copy(final String srcAbsPath, final String destAbsPath) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void copy(final String srcWorkspace, final String srcAbsPath, final String destAbsPath) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clone(final String srcWorkspace, final String srcAbsPath, final String destAbsPath,
            final boolean removeExisting) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void move(final String srcAbsPath, final String destAbsPath) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void restore(final Version[] versions, final boolean removeExisting) {
        throw new UnsupportedOperationException();
    }

    @Override
    public LockManager getLockManager() {
        throw new UnsupportedOperationException();
    }

    @Override
    public QueryManager getQueryManager() {
        throw new UnsupportedOperationException();
    }

    @Override
    public VersionManager getVersionManager() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] getAccessibleWorkspaceNames() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ContentHandler getImportContentHandler(final String parentAbsPath, final int uuidBehavior) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void importXML(final String parentAbsPath, final InputStream in, final int uuidBehavior) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void createWorkspace(final String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void createWorkspace(final String name, final String srcWorkspace) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteWorkspace(final String name) {
        throw new UnsupportedOperationException();
    }

}
