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

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.runtime.dto.AuthType;
import org.apache.sling.resourceresolver.impl.ResourceAccessSecurityTracker;
import org.apache.sling.resourceresolver.impl.helper.ResourceResolverContext;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderHandler;
import org.apache.sling.spi.resource.provider.ResourceProvider;

/**
 * The authenticator is a per resource resolver instance.
 * It keeps track of the used providers, especially the authenticated providers.
 *
 * This class is not thread safe (same as the resource resolver).
 */
public class ResourceProviderAuthenticator {

    /** Set of authenticated resource providers. */
    private final List<StatefulResourceProvider> authenticated = new ArrayList<StatefulResourceProvider>();

    /** Set of modifiable resource providers. */
    private final List<StatefulResourceProvider> modifiable = new ArrayList<StatefulResourceProvider>();

    /** Set of refreshable resource providers. */
    private final List<StatefulResourceProvider> refreshable = new ArrayList<StatefulResourceProvider>();

    private final Map<ResourceProviderHandler, StatefulResourceProvider> stateful;

    private final ResourceResolver resolver;

    private final Map<String, Object> authInfo;

    private final ResourceAccessSecurityTracker securityTracker;

    public ResourceProviderAuthenticator(ResourceResolver resolver, Map<String, Object> authInfo,
            ResourceAccessSecurityTracker securityTracker) throws LoginException {
        this.stateful = new IdentityHashMap<ResourceProviderHandler, StatefulResourceProvider>();
        this.resolver = resolver;
        this.authInfo = authInfo;
        this.securityTracker = securityTracker;
    }

    /**
     * Authenticate all handlers
     * @param handlers
     * @param combinedProvider
     * @throws LoginException
     */
    public void authenticateAll(final List<ResourceProviderHandler> handlers,
                                final ResourceResolverContext combinedProvider)
    throws LoginException {
        final List<StatefulResourceProvider> successfulHandlers = new ArrayList<StatefulResourceProvider>();
        for (final ResourceProviderHandler h : handlers) {
            try {
                successfulHandlers.add(authenticate(h, combinedProvider));
            } catch ( final LoginException le ) {
                // logout from all successful handlers
                for(final StatefulResourceProvider handler : successfulHandlers) {
                    handler.logout();
                }
                throw le;
            }
        }
    }

    private @Nonnull StatefulResourceProvider authenticate(final ResourceProviderHandler handler,
            ResourceResolverContext combinedProvider) throws LoginException {
        StatefulResourceProvider rp = stateful.get(handler);
        if (rp == null) {
            rp = createStateful(handler, combinedProvider);
            stateful.put(handler, rp);
            if (handler.getInfo().getAuthType() != AuthType.no) {
                authenticated.add(rp);
            }
            if (handler.getInfo().isModifiable()) {
                modifiable.add(rp);
            }
            if (handler.getInfo().isRefreshable()) {
                refreshable.add(rp);
            }
        }
        return rp;
    }

    public Collection<StatefulResourceProvider> getAllUsed() {
        return stateful.values();
    }

    public @Nonnull StatefulResourceProvider getStateful(ResourceProviderHandler handler, ResourceResolverContext combinedProvider)
    throws LoginException {
        return authenticate(handler, combinedProvider);
    }

    public Collection<StatefulResourceProvider> getAllUsedAuthenticated() {
        return authenticated;
    }

    public Collection<StatefulResourceProvider> getAllUsedModifiable() {
        return modifiable;
    }

    public Collection<StatefulResourceProvider> getAllUsedRefreshable() {
        return refreshable;
    }

    public Collection<StatefulResourceProvider> getAllBestEffort(List<ResourceProviderHandler> handlers,
            ResourceResolverContext combinedProvider) {
        List<StatefulResourceProvider> result = new ArrayList<StatefulResourceProvider>(handlers.size());
        for (ResourceProviderHandler h : handlers) {
            try {
                result.add(getStateful(h, combinedProvider));
            } catch ( final LoginException le) {
                // ignore
            }
        }
        return result;
    }

    /**
     * Create a stateful resource provider
     * @param handler Resource provider handler
     * @param combinedProvider Combined resource provider
     * @return The stateful resource provider
     * @throws LoginException
     */
    private @Nonnull StatefulResourceProvider createStateful(
            final ResourceProviderHandler handler,
            final ResourceResolverContext combinedProvider)
    throws LoginException {
        final ResourceProvider<?> rp = handler.getResourceProvider();
        StatefulResourceProvider authenticated;
        authenticated = new AuthenticatedResourceProvider(rp, handler.getInfo(), resolver, authInfo, combinedProvider);
        if (handler.getInfo().getUseResourceAccessSecurity()) {
            authenticated = new SecureResourceProviderDecorator(authenticated, securityTracker);
        }
        return authenticated;
    }

}