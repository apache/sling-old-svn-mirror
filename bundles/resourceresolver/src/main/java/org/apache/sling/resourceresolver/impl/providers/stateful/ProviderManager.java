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
package org.apache.sling.resourceresolver.impl.providers.stateful;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.runtime.dto.AuthType;
import org.apache.sling.resourceresolver.impl.ResourceAccessSecurityTracker;
import org.apache.sling.resourceresolver.impl.helper.AbstractIterator;
import org.apache.sling.resourceresolver.impl.helper.ResourceResolverControl;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderHandler;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages resolve contexts for each resource provider including
 * authentication.
 *
 * This class is not thread safe (same as the resource resolver).
 */
public class ProviderManager {

    private static final Logger logger = LoggerFactory.getLogger(ProviderManager.class);

    private final ResourceResolver resolver;

    private final Map<ResourceProviderHandler, AuthenticatedResourceProvider> contextMap;

    /** Set of authenticated resource providers. */
    private final List<AuthenticatedResourceProvider> authenticated = new ArrayList<AuthenticatedResourceProvider>();

    /** Set of modifiable resource providers. */
    private final List<AuthenticatedResourceProvider> modifiable = new ArrayList<AuthenticatedResourceProvider>();

    /** Set of refreshable resource providers. */
    private final List<AuthenticatedResourceProvider> refreshable = new ArrayList<AuthenticatedResourceProvider>();

    private final ResourceAccessSecurityTracker tracker;

    public ProviderManager(@Nonnull final ResourceResolver resolver, @Nonnull final ResourceAccessSecurityTracker tracker) {
        this.contextMap = new IdentityHashMap<ResourceProviderHandler, AuthenticatedResourceProvider>();
        this.resolver = resolver;
        this.tracker = tracker;
    }

    /**
     * Get the context
     * @param handler The resource handler
     * @return The resource context or {@code null} if authentication failed previously.
     */
    public @CheckForNull AuthenticatedResourceProvider getOrCreateProvider(@Nonnull final ResourceProviderHandler handler,
            @Nonnull final ResourceResolverControl control)
    throws LoginException {
        AuthenticatedResourceProvider provider = this.contextMap.get(handler);
        if (provider == null) {
            try {
                provider = authenticate(handler, control);
                this.contextMap.put(handler, provider);
                if ( handler.getInfo().getAuthType() == AuthType.lazy || handler.getInfo().getAuthType() == AuthType.required ) {
                    control.registerAuthenticatedProvider(handler, provider.getResolveContext().getProviderState());
                }
            } catch ( final LoginException le) {
                logger.debug("Authentication to resource provider " + handler.getResourceProvider() + " failed: " + le.getMessage(), le);
                this.contextMap.put(handler, AuthenticatedResourceProvider.UNAUTHENTICATED_PROVIDER);

                throw le;
            }
        }

        return provider == AuthenticatedResourceProvider.UNAUTHENTICATED_PROVIDER ? null : provider;
    }

    /**
     * Get the context
     * @param handler The resource handler
     * @return The resource context or {@code null}.
     */
    public @CheckForNull ResolveContext<Object> getOrCreateResolveContext(@Nonnull final ResourceProviderHandler handler,
            @Nonnull final ResourceResolverControl control)
    throws LoginException {
        AuthenticatedResourceProvider provider = this.getOrCreateProvider(handler, control);
        return provider == null ? null : provider.getResolveContext();
    }

    /**
     * Authenticate all handlers
     * @param handlers List of handlers
     * @param control the resource resolver control
     * @throws LoginException If authentication fails to one provider
     */
    public void authenticateAll(@Nonnull final List<ResourceProviderHandler> handlers,
            @Nonnull final ResourceResolverControl control)
    throws LoginException {
        for (final ResourceProviderHandler h : handlers) {
            try {
                this.getOrCreateProvider(h, control);
            } catch ( final LoginException le ) {
                // authentication failed, logout from all successful handlers
                for(final Map.Entry<ResourceProviderHandler, AuthenticatedResourceProvider> entry : this.contextMap.entrySet()) {
                    if ( entry.getValue() != AuthenticatedResourceProvider.UNAUTHENTICATED_PROVIDER ) {
                        final ResourceProvider<Object> provider = entry.getKey().getResourceProvider();
                        if ( provider != null ) {
                            provider.logout(entry.getValue().getResolveContext().getProviderState());
                        }
                    }
                }
                this.contextMap.clear();
                control.clearAuthenticatedProviders();
                throw le;
            }
        }
    }

    /**
     * Authenticate a single resource provider (handler)
     * @param handler The resource provider handler
     * @param control The resource control
     * @return The resolve context
     * @throws LoginException If authentication fails
     */
    private @Nonnull AuthenticatedResourceProvider authenticate(@Nonnull final ResourceProviderHandler handler,
            @Nonnull final ResourceResolverControl control) throws LoginException {
        final ResourceProvider<Object> provider = handler.getResourceProvider();
        boolean isAuthenticated = false;
        Object contextData = null;
        if ( (handler.getInfo().getAuthType() == AuthType.required || handler.getInfo().getAuthType() == AuthType.lazy) ) {
            try {
                contextData = provider.authenticate(control.getAuthenticationInfo());
                isAuthenticated = true;
            } catch ( final LoginException le ) {
                logger.debug("Unable to login into resource provider " + provider, le);
                throw le;
            }
        }

        final ResolveContext<Object> context = new BasicResolveContext<Object>(this.resolver,
                this,
                control,
                contextData,
                ResourceUtil.getParent(handler.getInfo().getPath()));
        final AuthenticatedResourceProvider rp = new AuthenticatedResourceProvider(handler,
                handler.getInfo().getUseResourceAccessSecurity(),
                context,
                this.tracker);
        if ( isAuthenticated ) {
            this.authenticated.add(rp);
        }
        if ( handler.getInfo().isModifiable() ) {
            this.modifiable.add(rp);
        }
        if ( handler.getInfo().isRefreshable() ) {
            this.refreshable.add(rp);
        }

        return rp;
    }

    public Collection<AuthenticatedResourceProvider> getAllAuthenticated() {
        return new ArrayList<AuthenticatedResourceProvider>(this.authenticated);
    }

    public Collection<AuthenticatedResourceProvider> getAllUsedModifiable() {
        return new ArrayList<AuthenticatedResourceProvider>(modifiable);
    }

    public Collection<AuthenticatedResourceProvider> getAllUsedRefreshable() {
        return new ArrayList<AuthenticatedResourceProvider>(refreshable);
    }

    public Iterable<AuthenticatedResourceProvider> getAllBestEffort(@Nonnull final List<ResourceProviderHandler> handlers,
            @Nonnull final ResourceResolverControl control) {
        final Iterator<ResourceProviderHandler> handlerIter = handlers.iterator();
        return new Iterable<AuthenticatedResourceProvider>() {

            @Override
            public Iterator<AuthenticatedResourceProvider> iterator() {
                return new AbstractIterator<AuthenticatedResourceProvider>() {

                    @Override
                    protected AuthenticatedResourceProvider seek() {
                        AuthenticatedResourceProvider result = null;
                        while ( result == null && handlerIter.hasNext() ) {
                            final ResourceProviderHandler h = handlerIter.next();
                            try {
                                result = getOrCreateProvider(h, control);
                            } catch ( final LoginException le) {
                                // ignore
                            }
                        }
                        return result;
                    }
                };
            }
        };
    }
}
