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

import java.util.Collections;
import java.util.Map;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;

public class BasicResolveContext<T> implements ResolveContext<T> {

    private final ResourceResolver resourceResolver;

    private final Map<String, String> resolveParameters;

    private final T providerState;

    private final ResolveContext<Object> parentResolveContext;

    private final ResourceProvider<Object> parentResourceProvider;

    public BasicResolveContext(ResourceResolver resourceResolver, Map<String, String> resolveParameters,
            T providerState, ResourceProvider<Object> parentResourceProvider, ResolveContext<Object> parentResolveContext) {
        this.resourceResolver = resourceResolver;
        this.resolveParameters = resolveParameters == null ? Collections.<String, String> emptyMap()
                : resolveParameters;
        this.providerState = providerState;
        this.parentResolveContext = parentResolveContext;
        this.parentResourceProvider = parentResourceProvider;
    }

    public BasicResolveContext(ResourceResolver resourceResolver, Map<String, String> resolveParameters,
            T providerState) {
        this(resourceResolver, resolveParameters, providerState, null, null);
    }

    @Override
    public ResourceResolver getResourceResolver() {
        return resourceResolver;
    }

    @Override
    public Map<String, String> getResolveParameters() {
        return resolveParameters;
    }

    @Override
    public T getProviderState() {
        return providerState;
    }

    @Override
    public ResolveContext<?> getParentResolveContext() {
        return parentResolveContext;
    }

    @Override
    public ResourceProvider<?> getParentResourceProvider() {
        return parentResourceProvider;
    }

}
