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

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.resourceresolver.impl.helper.ResourceResolverControl;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderHandler;
import org.apache.sling.resourceresolver.impl.providers.tree.Node;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;

/**
 * Resolve context implementation for a resource provider.
 *
 * This class is not thread safe (same as the resource resolver).
 */
public class BasicResolveContext<T> implements ResolveContext<T> {

    private final ResourceResolver resolver;

    private final ProviderManager resolveContextManager;

    private final ResourceResolverControl control;

    private final T providerState;

    private final String parentPath;

    public BasicResolveContext(@Nonnull final ResourceResolver resolver,
            @Nonnull final ProviderManager resolveContextManager,
            @Nonnull final ResourceResolverControl control,
            @CheckForNull final T providerState,
            @Nonnull final String parentPath) {
        this.resolver = resolver;
        this.resolveContextManager = resolveContextManager;
        this.parentPath = parentPath;
        this.providerState = providerState;
        this.control = control;
    }

    @Override
    public ResourceResolver getResourceResolver() {
        return this.resolver;
    }

    @Override
    public T getProviderState() {
        return providerState;
    }

    @Override
    public ResolveContext<?> getParentResolveContext() {
        final Object[] providerAndContext = getParentProviderAndContext();
        return (ResolveContext<?>) (providerAndContext != null ? providerAndContext[1] : null);
    }

    @Override
    public ResourceProvider<?> getParentResourceProvider() {
        final Object[] providerAndContext = getParentProviderAndContext();
        return (ResourceProvider<?>) (providerAndContext != null ? providerAndContext[0] : null);
    }

    private Object[] getParentProviderAndContext() {
        ResourceProvider<?> parentProvider = null;
        ResolveContext<?> parentResolveContext = null;
        if ( this.parentPath != null ) {
            String path = this.parentPath;
            while ( path != null && parentProvider == null ) {
                final Node<ResourceProviderHandler> node = this.control.getResourceProviderStorage().getTree().getBestMatchingNode(path);
                if ( node != null ) {
                    final ResourceProviderHandler handler = node.getValue();
                    try {
                       parentResolveContext = this.resolveContextManager.getOrCreateResolveContext(handler, this.control);
                       if ( parentResolveContext != null ) {
                           parentProvider = handler.getResourceProvider();
                        }
                    } catch ( final LoginException se) {
                        // skip this, try next
                    }
                    if ( parentProvider == null ) {
                        parentResolveContext = null;
                        path = ResourceUtil.getParent(path);
                    }
                } else {
                    path = null;
                }
            }
        }
        return parentProvider != null ? new Object[] {parentProvider, parentResolveContext} : null;
    }
}
