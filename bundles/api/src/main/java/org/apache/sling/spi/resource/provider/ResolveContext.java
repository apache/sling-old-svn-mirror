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
package org.apache.sling.spi.resource.provider;

import java.util.Map;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.sling.api.resource.ResourceResolver;

import aQute.bnd.annotation.ProviderType;

/**
 * The resolve context is passed to most of the methods of the {@link ResourceProvider}
 * to provide additional context information for the provider.
 *
 * If the provider requires authentication, the object returned by {@link ResourceProvider#authenticate(Map)}
 * is returned in {@link #getProviderState()}.
 *
 * @since 1.0.0 (Sling API Bundle 2.11.0)
 */
@ProviderType
public interface ResolveContext<T> {

    /**
     * Get the current resource resolver.
     * @return The resource resolver.
     */
    @Nonnull ResourceResolver getResourceResolver();

    /**
     * This is the object returned by {@link ResourceProvider#authenticate(Map)}
     * @return The data object or {@code null}
     */
    @CheckForNull T getProviderState();

    /**
     * Return a resolve context for the parent resource provider.
     * @return A resolve context or {@code null} if there is no parent.
     * @see #getParentResourceProvider()
     */
    @CheckForNull ResolveContext<?> getParentResolveContext();

    /**
     * Return the parent resource provider.
     * If the parent should be used for resolving, a context created with
     * {@link ResourceContext#getParentResolveContext()} should be passed to that
     * instance.
     * @return The parent provider or {@code null} if there is no parent.
     */
    @CheckForNull ResourceProvider<?> getParentResourceProvider();
}
