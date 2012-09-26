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
package org.apache.sling.resourceresolver.impl.helper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.sling.api.resource.DynamicResourceProvider;
import org.apache.sling.api.resource.ModifyingResourceProvider;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * This class keeps track of the used resource providers for a
 * resource resolver.
 * Like a resource resolver itself, this class is not thread safe.
 */
public class ResourceResolverContext {

    /** A map of all used providers created by a factory. */
    private final Map<Long, ResourceProvider> providers = new HashMap<Long, ResourceProvider>();

    /** A set of all dynamic providers (for closing them later on) */
    private final Set<DynamicResourceProvider> dynamicProviders = new HashSet<DynamicResourceProvider>();

    /** A set of all modifying providers */
    private final Set<ModifyingResourceProvider> modifyingProviders = new HashSet<ModifyingResourceProvider>();

    /** Is this a resource resolver for an admin? */
    private final boolean isAdmin;

    /**
     * The original authentication information - this is used for cloning and lazy logins.
     */
    private final Map<String, Object> originalAuthInfo;

    /**
     * Create a new resource resolver context.
     */
    public ResourceResolverContext(final boolean isAdmin, final Map<String, Object> originalAuthInfo) {
        this.isAdmin = isAdmin;
        this.originalAuthInfo = originalAuthInfo;
    }

    /**
     * Is this an admin resource resolver.
     */
    public boolean isAdmin() {
        return this.isAdmin;
    }

    /**
     * Return the authentication info.
     */
    public Map<String, Object> getAuthenticationInfo() {
        return this.originalAuthInfo;
    }

    /**
     * Add a new resource provider
     * @param key      The unique key of the provider
     * @param provider The provider.
     */
    public void addFactoryResourceProvider(final Long key, final ResourceProvider provider) {
        this.providers.put(key, provider);
        if (provider instanceof DynamicResourceProvider) {
            this.dynamicProviders.add((DynamicResourceProvider) provider);
        }
        if (provider instanceof ModifyingResourceProvider) {
            this.modifyingProviders.add((ModifyingResourceProvider) provider);
        }
    }

    /**
     * Return a resource provider for a given key
     * @param key The unique key of a provider
     * @return The resource provider or <code>null</code>
     */
    public ResourceProvider getFactoryResourceProvider(final Long key) {
        return this.providers.get(key);
    }

    /**
     * Check all active dynamic resource providers.
     */
    public boolean isLive() {
        boolean result = true;
        for (final DynamicResourceProvider provider : this.dynamicProviders) {
            if (!provider.isLive()) {
                result = false;
                break;
            }
        }
        return result;
    }

    /**
     * Close all dynamic resource providers.
     */
    public void close() {
        for (final DynamicResourceProvider provider : this.dynamicProviders) {
            provider.close();
        }
        this.dynamicProviders.clear();
        this.providers.clear();
    }

    /**
     * Revert all transient changes.
     */
    public void revert(final ResourceResolver resolver) {
        for(final ModifyingResourceProvider provider : this.modifyingProviders) {
            provider.revert(resolver);
        }
    }

    /**
     * Commit all transient changes
     */
    public void commit(final ResourceResolver resolver) throws PersistenceException {
        for(final ModifyingResourceProvider provider : this.modifyingProviders) {
            provider.commit(resolver);
        }
    }

    /**
     * Do we have changes?
     */
    public boolean hasChanges(final ResourceResolver resolver) {
        for(final ModifyingResourceProvider provider : this.modifyingProviders) {
            if ( provider.hasChanges(resolver) ) {
                return true;
            }
        }
        return false;
    }
}
