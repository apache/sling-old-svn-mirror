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

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.resourceresolver.impl.helper.ResourceResolverControl;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderHandler;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderStorage;
import org.apache.sling.resourceresolver.impl.providers.tree.Node;
import org.apache.sling.spi.resource.provider.ResolverContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;

public class BasicResolveContext<T> implements ResolverContext<T> {

    private final String parentPath;

    private final ResourceResolver resourceResolver;

    private final T providerState;

    private final ResourceResolverControl combinedProvider;

    private volatile boolean parentLookupDone = false;

    private volatile ResourceProvider<Object> parentProvider;

    private volatile ResolverContext<Object> parentResolveContext;

    public BasicResolveContext(ResourceResolver resourceResolver,
            T providerState,
            String parentPath,
            ResourceResolverControl combinedProvider) {
        this.resourceResolver = resourceResolver;
        this.parentPath = parentPath;
        this.providerState = providerState;
        this.combinedProvider = combinedProvider;
    }

    public BasicResolveContext(ResourceResolver resourceResolver,
            T providerState, String parentPath) {
        this(resourceResolver, providerState, parentPath, null);
    }

    @Override
    public ResourceResolver getResourceResolver() {
        return resourceResolver;
    }

    @Override
    public T getProviderState() {
        return providerState;
    }

    @Override
    public ResolverContext<?> getParentResolveContext() {
        this.getParentResourceProvider();
        return parentResolveContext;
    }

    @Override
    public ResourceProvider<?> getParentResourceProvider() {
        if ( ! parentLookupDone ) {
            synchronized ( this ) {
                if ( this.parentPath != null ) {
                    final ResourceProviderStorage storage = this.combinedProvider.getResourceProviderStorage();
                    String path = this.parentPath;
                    while ( path != null && this.parentProvider != null ) {
                        final Node<ResourceProviderHandler> node = storage.getTree().getBestMatchingNode(this.parentPath);
                        if ( node != null ) {
                            try {
                                final StatefulResourceProvider srp = this.combinedProvider.getStatefulResourceProvider(node.getValue());
                                if ( srp != null ) {
                                    this.parentProvider = srp.getResourceProvider();
                                    this.parentResolveContext = srp.getContext();
                                }
                            } catch ( final LoginException se) {
                                // skip this, try next
                                this.parentProvider = null;
                            }
                            if ( this.parentProvider == null ) {
                                path = ResourceUtil.getParent(path);
                            }
                        } else {
                            path = null;
                        }
                    }
                }
                parentLookupDone = true;
            }
        }
        return this.parentProvider;
    }

}
