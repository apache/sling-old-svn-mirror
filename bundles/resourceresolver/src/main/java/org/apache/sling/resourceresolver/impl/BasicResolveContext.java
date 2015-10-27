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
package org.apache.sling.resourceresolver.impl;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.spi.resource.provider.ResolverContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;

public class BasicResolveContext<T> implements ResolverContext<T> {

    private final ResourceResolver resourceResolver;

    private final T providerState;

    private final ResolverContext<Object> parentResolveContext;

    private final ResourceProvider<Object> parentResourceProvider;

    public BasicResolveContext(ResourceResolver resourceResolver,
            T providerState, ResourceProvider<Object> parentResourceProvider, ResolverContext<Object> parentResolveContext) {
        this.resourceResolver = resourceResolver;
        this.providerState = providerState;
        this.parentResolveContext = parentResolveContext;
        this.parentResourceProvider = parentResourceProvider;
    }

    public BasicResolveContext(ResourceResolver resourceResolver,
            T providerState) {
        this(resourceResolver, providerState, null, null);
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
        return parentResolveContext;
    }

    @Override
    public ResourceProvider<?> getParentResourceProvider() {
        return parentResourceProvider;
    }

}
