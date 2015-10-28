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
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderHandler;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceProviderAuthenticator {

    private static final Logger logger = LoggerFactory.getLogger(ResourceProviderAuthenticator.class);

    private final Map<ResourceProviderHandler, StatefulResourceProvider> stateful;

    private final List<StatefulResourceProvider> authenticated;

    private final List<StatefulResourceProvider> authenticatedModifiable;

    private final ResourceResolver resolver;

    private final Map<String, Object> authInfo;

    private final ResourceAccessSecurityTracker securityTracker;

    boolean allProvidersAuthenticated;

    public ResourceProviderAuthenticator(ResourceResolver resolver, Map<String, Object> authInfo,
            ResourceAccessSecurityTracker securityTracker) throws LoginException {
        this.stateful = new IdentityHashMap<ResourceProviderHandler, StatefulResourceProvider>();
        this.authenticated = new ArrayList<StatefulResourceProvider>();
        this.authenticatedModifiable = new ArrayList<StatefulResourceProvider>();
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
                                final CombinedResourceProvider combinedProvider)
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

    private StatefulResourceProvider authenticate(final ResourceProviderHandler handler,
            CombinedResourceProvider combinedProvider) throws LoginException {
        StatefulResourceProvider rp = stateful.get(handler);
        if (rp == null) {
            rp = createStateful(handler, combinedProvider);
            stateful.put(handler, rp);
            if (handler.getInfo().getAuthType() != AuthType.no) {
                authenticated.add(rp);
            }
            if (handler.getInfo().getModifiable()) {
                authenticatedModifiable.add(rp);
            }
        }
        return rp;
    }

    public Collection<StatefulResourceProvider> getAllUsed() {
        return stateful.values();
    }

    public StatefulResourceProvider getStateful(ResourceProviderHandler handler, CombinedResourceProvider combinedProvider)
    throws LoginException {
        return authenticate(handler, combinedProvider);
    }

    public Collection<StatefulResourceProvider> getAllUsedAuthenticated() {
        return authenticated;
    }

    public Collection<StatefulResourceProvider> getAllUsedModifiable() {
        return authenticatedModifiable;
    }

    public Collection<StatefulResourceProvider> getAll(List<ResourceProviderHandler> handlers,
            CombinedResourceProvider combinedProvider) throws LoginException {
        List<StatefulResourceProvider> result = new ArrayList<StatefulResourceProvider>(handlers.size());
        for (ResourceProviderHandler h : handlers) {
            result.add(getStateful(h, combinedProvider));
        }
        return result;
    }

    private @Nonnull StatefulResourceProvider createStateful(ResourceProviderHandler handler,
            CombinedResourceProvider combinedProvider) throws LoginException {
        final ResourceProvider<?> rp = handler.getResourceProvider();
        StatefulResourceProvider authenticated;
        authenticated = new AuthenticatedResourceProvider(rp, handler.getInfo(), resolver, authInfo, combinedProvider);
        if (handler.getInfo().getUseResourceAccessSecurity()) {
            authenticated = new SecureResourceProvider(authenticated, securityTracker);
        }
        return authenticated;
    }

}