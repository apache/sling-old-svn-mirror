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
package org.apache.sling.resourceresolver.impl.providers.stateful;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.security.ResourceAccessSecurity;
import org.apache.sling.resourceresolver.impl.ResourceAccessSecurityTracker;
import org.apache.sling.resourceresolver.impl.ResourceResolverImpl;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This wrappers add the Sling security layer (see
 * {@link ResourceAccessSecurity}) to the underlying {@link ResourceProvider}.
 */
public class SecureResourceProvider extends StatefulResourceProviderWrapper {

    private static final Logger logger = LoggerFactory.getLogger(ResourceResolverImpl.class);

    private final ResourceAccessSecurityTracker tracker;

    public SecureResourceProvider(StatefulResourceProvider rp, ResourceAccessSecurityTracker tracker) {
        super(rp);
        if (tracker == null) {
            logger.warn("ResourceAccessSecurityTracker is null. Resource-level security will be disabled.");
        }
        this.tracker = tracker;
    }

    @Override
    public Resource create(final String path, Map<String, Object> properties, List<StatefulResourceProvider> parentProviders) throws PersistenceException {
        if (isAllowed(new SecurityTest() {
            @Override
            public boolean isAllowed(ResourceAccessSecurity security) {
                return security.canCreate(path, getResourceResolver());
            }
        })) {
            return rp.create(path, properties, parentProviders);
        } else {
            return null;
        }
    }

    @Override
    public void delete(final Resource resource, List<StatefulResourceProvider> parentProviders) throws PersistenceException {
        if (isAllowed(new SecurityTest() {
            @Override
            public boolean isAllowed(ResourceAccessSecurity security) {
                return security.canDelete(resource);
            }
        })) {
            rp.delete(resource, parentProviders);
        }
    }

    @Override
    public Iterator<Resource> findResources(String query, String language) {
        return wrapIterator(rp.findResources(query, language));
    }

    private Iterator<Resource> wrapIterator(Iterator<Resource> iterator) {
        if (tracker == null) {
            return iterator;
        } else {
            return new SecureIterator(iterator);
        }
    }

    private boolean isAllowed(SecurityTest test) {
        if (tracker == null) {
            return true;
        }
        boolean allowed = true;
        if (tracker.getProviderResourceAccessSecurity() != null) {
            allowed = allowed && test.isAllowed(tracker.getProviderResourceAccessSecurity());
        }
        if (tracker.getApplicationResourceAccessSecurity() != null) {
            allowed = allowed && test.isAllowed(tracker.getApplicationResourceAccessSecurity());
        }
        return allowed;
    }

    private static interface SecurityTest {
        boolean isAllowed(ResourceAccessSecurity security);
    }

    private class SecureIterator extends AbstractIterator<Resource> {

        private final Iterator<Resource> iterator;

        public SecureIterator(Iterator<Resource> iterator) {
            this.iterator = iterator;
        }

        @Override
        protected Resource seek() {
            while (iterator.hasNext()) {
                Resource resource = iterator.next();
                if (resource != null && tracker.getProviderResourceAccessSecurity() != null) {
                    resource = tracker.getProviderResourceAccessSecurity().getReadableResource(resource);
                }
                if (resource != null && tracker.getApplicationResourceAccessSecurity() != null) {
                    resource = tracker.getApplicationResourceAccessSecurity().getReadableResource(resource);
                }
                if (resource != null) {
                    return resource;
                }
            }
            return null;
        }
    }
}
