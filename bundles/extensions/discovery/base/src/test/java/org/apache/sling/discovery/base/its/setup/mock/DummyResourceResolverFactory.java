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
package org.apache.sling.discovery.base.its.setup.mock;

import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.jcr.api.SlingRepository;

/** Factored out of MockFactory: simple implementation
 * of a ResourceResolverFactory that uses MockedResourceResolver's
 * mechanism of auto-creating a repository via
 * RepositoryProvider.instance().getRepository()
 */
public class DummyResourceResolverFactory implements ResourceResolverFactory {

    private SlingRepository repository;
    private ArtificialDelay delay;

    public DummyResourceResolverFactory() {
        
    }
    
    public void setSlingRepository(SlingRepository repository) {
        this.repository = repository;
    }
    
    public void setArtificialDelay(ArtificialDelay delay) {
        this.delay = delay;
    }
    
    @Override
    public ResourceResolver getResourceResolver(Map<String, Object> authenticationInfo) throws LoginException {
        throw new IllegalStateException("not yet implemented");
    }

    @Override
    public ResourceResolver getAdministrativeResourceResolver(Map<String, Object> authenticationInfo) throws LoginException {
        try {
            MockedResourceResolver mockedResourceResolver = 
                    new MockedResourceResolver(repository, delay);
            repository = (SlingRepository) mockedResourceResolver.getRepository();
            return mockedResourceResolver;
        } catch (RepositoryException e) {
            throw new LoginException(e);
        }
    }

    public SlingRepository getSlingRepository() {
        return repository;
    }

}
