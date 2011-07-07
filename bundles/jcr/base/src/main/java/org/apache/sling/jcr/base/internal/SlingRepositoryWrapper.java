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
package org.apache.sling.jcr.base.internal;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;

import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.AbstractNamespaceMappingRepository;
import org.osgi.framework.BundleContext;

/**
 *
 */
public class SlingRepositoryWrapper
    extends AbstractNamespaceMappingRepository
    implements SlingRepository {

    private final Repository delegatee;

    public SlingRepositoryWrapper(final Repository delegatee, final BundleContext bundleContext) {
        this.delegatee = delegatee;
        this.setup(bundleContext);
    }

    public void dispose() {
        this.tearDown();
    }

    /**
     * Return <code>null</code> to indicate the default workspace
     * of the repository is used.
     * @see org.apache.sling.jcr.api.SlingRepository#getDefaultWorkspace()
     */
    public String getDefaultWorkspace() {
        return null;
    }

    /**
     * @see org.apache.sling.jcr.api.SlingRepository#loginAdministrative(java.lang.String)
     */
    public Session loginAdministrative(String workspace)
    throws RepositoryException {
        return this.login(new SimpleCredentials("admin", "admin".toCharArray()), workspace);
    }

    /**
     * @see javax.jcr.Repository#getDescriptorKeys()
     */
    public String[] getDescriptorKeys() {
        return delegatee.getDescriptorKeys();
    }

    /**
     * @see javax.jcr.Repository#isStandardDescriptor(java.lang.String)
     */
    public boolean isStandardDescriptor(String key) {
        return delegatee.isStandardDescriptor(key);
    }

    /**
     * @see javax.jcr.Repository#isSingleValueDescriptor(java.lang.String)
     */
    public boolean isSingleValueDescriptor(String key) {
        return delegatee.isSingleValueDescriptor(key);
    }

    /**
     * @see javax.jcr.Repository#getDescriptorValue(java.lang.String)
     */
    public Value getDescriptorValue(String key) {
        return delegatee.getDescriptorValue(key);
    }

    /**
     * @see javax.jcr.Repository#getDescriptorValues(java.lang.String)
     */
    public Value[] getDescriptorValues(String key) {
        return delegatee.getDescriptorValues(key);
    }

    /**
     * @see javax.jcr.Repository#getDescriptor(java.lang.String)
     */
    public String getDescriptor(String key) {
        return delegatee.getDescriptor(key);
    }

    /**
     * @see javax.jcr.Repository#login(javax.jcr.Credentials, java.lang.String)
     */
    public Session login(Credentials credentials, String workspaceName)
            throws LoginException, NoSuchWorkspaceException,
            RepositoryException {
        return this.getNamespaceAwareSession(delegatee.login(credentials, workspaceName));
    }

    /**
     * @see javax.jcr.Repository#login(javax.jcr.Credentials)
     */
    public Session login(Credentials credentials) throws LoginException,
            RepositoryException {
        return this.getNamespaceAwareSession(delegatee.login(credentials));
    }

    /**
     * @see javax.jcr.Repository#login(java.lang.String)
     */
    public Session login(String workspaceName) throws LoginException,
            NoSuchWorkspaceException, RepositoryException {
        return this.getNamespaceAwareSession(delegatee.login(workspaceName));
    }

    /**
     * @see javax.jcr.Repository#login()
     */
    public Session login() throws LoginException, RepositoryException {
        return this.getNamespaceAwareSession(delegatee.login());
    }
}
