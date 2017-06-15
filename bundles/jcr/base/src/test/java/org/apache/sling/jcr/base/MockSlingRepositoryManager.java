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
package org.apache.sling.jcr.base;

import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;

import org.apache.sling.serviceusermapping.ServiceUserMapper;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/** Minimal AbstractSlingRepositoryManager used for testing */
public class MockSlingRepositoryManager extends AbstractSlingRepositoryManager {

    public static final String WHITELIST_ALL = "*";

    public static final String WHITELIST_NONE = "";

    private final Repository repository;

    private boolean loginAdminDisabled;

    private Set<String> loginAdminWhitelist;

    public MockSlingRepositoryManager(Repository repository) {
        this(repository, false, WHITELIST_ALL);
    }

    public MockSlingRepositoryManager(Repository repository, boolean loginAdminDisabled, String... loginAdminWhitelist) {
        this.repository = repository;
        this.loginAdminDisabled = loginAdminDisabled;
        this.loginAdminWhitelist = new HashSet<>(Arrays.asList(loginAdminWhitelist));
        this.loginAdminWhitelist.remove(WHITELIST_NONE);
    }

    @Override
    protected ServiceUserMapper getServiceUserMapper() {
        return null;
    }

    @Override
    protected Repository acquireRepository() {
        return repository;
    }

    @Override
    protected Dictionary<String, Object> getServiceRegistrationProperties() {
        return new Hashtable<String, Object>();
    }

    @Override
    protected AbstractSlingRepository2 create(Bundle usingBundle) {
        if(repository != null) {
            try {
                return new MockSlingRepository2(this, usingBundle, repository.login());
            } catch(RepositoryException rex) {
                fail(rex.toString());
            }
        }
        return null;
    }

    @Override
    protected void destroy(AbstractSlingRepository2 repositoryServiceInstance) {
    }

    @Override
    protected void disposeRepository(Repository repository) {
    }

    @Override
    protected boolean allowLoginAdministrativeForBundle(final Bundle bundle) {
        return loginAdminWhitelist.contains("*") || loginAdminWhitelist.contains(bundle.getSymbolicName());
    }

    public void activate(BundleContext context) {
        start(context, new Config(null, loginAdminDisabled));
    }
}