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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.query.Query;
import org.apache.sling.api.resource.query.QueryInstructions;
import org.apache.sling.api.resource.query.QueryManager;
import org.apache.sling.spi.resource.provider.JCRQueryProvider;
import org.apache.sling.spi.resource.provider.QueryResult;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;

/**
 * This interface represents a {@link ResourceProvider} and its authentication
 * info. It's meant to be used only in the o.a.s.resourceresolver.impl bundle to
 * avoid passing authentication state with every call to any resource provider
 * method.
 */
public interface StatefulResourceProvider {

    /**
     * Returns the resource resolver associated to this {@link StatefulResourceProvider}.
     */
    ResourceResolver getResourceResolver();

    /**
     * @see ResourceProvider#logout(Object)
     */
    void logout();

    /**
     * @see ResourceProvider#refresh(org.apache.sling.spi.resource.provider.ResolveContext)
     */
    void refresh();

    /**
     * @see ResourceProvider#isLive(org.apache.sling.spi.resource.provider.ResolveContext)
     */
    boolean isLive();

    /**
     * @see ResourceProvider#getParent(org.apache.sling.spi.resource.provider.ResolveContext, Resource)
     */
    @CheckForNull
    Resource getParent(final @Nonnull Resource child, final List<StatefulResourceProvider> parentProviders);

    /**
     * @see ResourceProvider#getResource(org.apache.sling.spi.resource.provider.ResolveContext, String, Resource)
     */
    @CheckForNull
    Resource getResource(@Nonnull final String path, @CheckForNull final Resource parent,
            final Map<String, String> parameters, final boolean isResolve,
            final List<StatefulResourceProvider> parentProviders);

    /**
     * @see ResourceProvider#listChildren(org.apache.sling.spi.resource.provider.ResolveContext, Resource)
     */
    @CheckForNull
    Iterator<Resource> listChildren(final @Nonnull Resource parent, final List<StatefulResourceProvider> parentProviders);

    /**
     * @see ResourceProvider#getAttributeNames(org.apache.sling.spi.resource.provider.ResolveContext)
     */
    Collection<String> getAttributeNames();

    /**
     * @see ResourceProvider#getAttribute(org.apache.sling.spi.resource.provider.ResolveContext, String)
     */
    Object getAttribute(final @Nonnull String name);

    /**
     * @see ResourceProvider#create(org.apache.sling.spi.resource.provider.ResolveContext, String, Map)
     */
    Resource create(final String path, final Map<String, Object> properties, final List<StatefulResourceProvider> parentProviders) throws PersistenceException;

    /**
     * @see ResourceProvider#delete(org.apache.sling.spi.resource.provider.ResolveContext, Resource)
     */
    void delete(final @Nonnull Resource resource, final List<StatefulResourceProvider> parentProviders) throws PersistenceException;

    /**
     * @see ResourceProvider#revert(org.apache.sling.spi.resource.provider.ResolveContext)
     */
    void revert();

    /**
     * @see ResourceProvider#commit(org.apache.sling.spi.resource.provider.ResolveContext)
     */
    void commit() throws PersistenceException;

    /**
     * @see ResourceProvider#hasChanges(org.apache.sling.spi.resource.provider.ResolveContext)
     */
    boolean hasChanges();

    /**
     * @see QueryManager#find(ResourceResolver, Query, QueryInstructions)
     */
    @CheckForNull
    QueryResult find(@Nonnull Query q, @Nonnull QueryInstructions qi);

    /**
     * @see JCRQueryProvider#getSupportedLanguages(org.apache.sling.spi.resource.provider.ResolveContext)
     */
    @CheckForNull
    String[] getSupportedLanguages();

    /**
     * @see JCRQueryProvider#findResources(org.apache.sling.spi.resource.provider.ResolveContext, String, String)
     */
    @CheckForNull
    Iterator<Resource> findResources(String query, String language);

    /**
     * @see JCRQueryProvider#queryResources(org.apache.sling.spi.resource.provider.ResolveContext, String, String)
     */
    @CheckForNull
    Iterator<Map<String, Object>> queryResources(String query, String language);

    /**
     * @see ResourceProvider#adaptTo(org.apache.sling.spi.resource.provider.ResolveContext, Class)
     */
    @CheckForNull
    <AdapterType> AdapterType adaptTo(final @Nonnull Class<AdapterType> type);

    /**
     * @see ResourceProvider#copy(org.apache.sling.spi.resource.provider.ResolveContext, String, String)
     */
    boolean copy(final String srcAbsPath, final String destAbsPath, final List<StatefulResourceProvider> parentProviders) throws PersistenceException;

    /**
     * @see ResourceProvider#move(org.apache.sling.spi.resource.provider.ResolveContext, String, String)
     */
    boolean move(final String srcAbsPath, final String destAbsPath, final List<StatefulResourceProvider> parentProviders) throws PersistenceException;

    /**
     * @return Wrapped ResourceProvider
     */
    ResourceProvider<Object> getResourceProvider();

    /**
     * @return Context for the wrapped ResourceProvider
     * @throws LoginException 
     */
    ResolveContext<Object> getContext(Map<String, String> parameters) throws LoginException;
}
