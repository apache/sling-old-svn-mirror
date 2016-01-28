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

import java.util.Dictionary;
import java.util.Hashtable;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;

import org.apache.sling.jcr.api.NamespaceMapper;
import org.apache.sling.serviceusermapping.ServiceUserMapper;
import org.osgi.framework.Bundle;

/** Minimal AbstractSlingRepositoryManager used for testing */ 
class MockSlingRepositoryManager extends AbstractSlingRepositoryManager {
    
    private final Repository repository;
    
    MockSlingRepositoryManager(Repository repository) {
        this.repository = repository;
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
    protected NamespaceMapper[] getNamespaceMapperServices() {
        return new NamespaceMapper[0];
    }
}