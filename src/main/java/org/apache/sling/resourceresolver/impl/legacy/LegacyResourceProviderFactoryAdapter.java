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
package org.apache.sling.resourceresolver.impl.legacy;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.commons.lang.ArrayUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProviderFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.spi.resource.provider.QueryLanguageProvider;
import org.apache.sling.spi.resource.provider.ResolverContext;
import org.apache.sling.spi.resource.provider.ResourceContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;

@SuppressWarnings({ "deprecation", "rawtypes", "unchecked" })
public class LegacyResourceProviderFactoryAdapter extends ResourceProvider<LegacyResourceProviderAdapter> {

    private final ResourceProviderFactory rpFactory;

    private final String[] languages;

    private final boolean ownsRoot;

    public LegacyResourceProviderFactoryAdapter(ResourceProviderFactory rpFactory, String[] languages, boolean ownsRoot) {
        this.rpFactory = rpFactory;
        this.languages = languages;
        this.ownsRoot = ownsRoot;
    }

    @Override
    @Nonnull
    public LegacyResourceProviderAdapter authenticate(final @Nonnull Map<String, Object> authenticationInfo)
            throws LoginException {
        return new LegacyResourceProviderAdapter(rpFactory.getResourceProvider(authenticationInfo), languages, ownsRoot);
    }

    @Override
    public void logout(final @Nonnull ResolverContext<LegacyResourceProviderAdapter> ctx) {
        ctx.getProviderState().logout(null);
    }

    @Override
    public Resource getResource(ResolverContext<LegacyResourceProviderAdapter> ctx, String path, ResourceContext resourceContext, Resource parent) {
        return ctx.getProviderState().getResource((ResolverContext) ctx, path, resourceContext, parent);
    }

    @Override
    public Iterator<Resource> listChildren(ResolverContext<LegacyResourceProviderAdapter> ctx, Resource parent) {
        return ctx.getProviderState().listChildren((ResolverContext) ctx, parent);
    }

    @Override
    public void refresh(final @Nonnull ResolverContext<LegacyResourceProviderAdapter> ctx) {
        ctx.getProviderState().refresh((ResolverContext) ctx);
    }

    @Override
    public @CheckForNull QueryLanguageProvider<LegacyResourceProviderAdapter> getQueryLanguageProvider() {
        if (ArrayUtils.isEmpty(languages)) {
            return super.getQueryLanguageProvider();
        } else {
            return new JCRQueryProviderAdapter(languages);
        }
    }

    @Override
    public Collection<String> getAttributeNames(final @Nonnull ResolverContext<LegacyResourceProviderAdapter> ctx) {
        return ctx.getProviderState().getAttributeNames((ResolverContext) ctx);
    }

    @Override
    public Object getAttribute(final @Nonnull ResolverContext<LegacyResourceProviderAdapter> ctx,
            final @Nonnull String name) {
        return ctx.getProviderState().getAttribute((ResolverContext) ctx, name);
    }

    @Override
    public boolean isLive(final @Nonnull ResolverContext<LegacyResourceProviderAdapter> ctx) {
        return ctx.getProviderState().isLive((ResolverContext) ctx);
    }

    @Override
    public Resource create(final @Nonnull ResolverContext<LegacyResourceProviderAdapter> ctx, final String path,
            final Map<String, Object> properties) throws PersistenceException {
        return ctx.getProviderState().create((ResolverContext) ctx, path, properties);
    }

    @Override
    public void delete(final @Nonnull ResolverContext<LegacyResourceProviderAdapter> ctx,
            final @Nonnull Resource resource) throws PersistenceException {
        ctx.getProviderState().delete((ResolverContext) ctx, resource);
    }

    @Override
    public void revert(final @Nonnull ResolverContext<LegacyResourceProviderAdapter> ctx) {
        ctx.getProviderState().revert((ResolverContext) ctx);
    }

    @Override
    public void commit(final @Nonnull ResolverContext<LegacyResourceProviderAdapter> ctx) throws PersistenceException {
        ctx.getProviderState().commit((ResolverContext) ctx);
    }

    @Override
    public boolean hasChanges(final @Nonnull ResolverContext<LegacyResourceProviderAdapter> ctx) {
        return ctx.getProviderState().hasChanges((ResolverContext) ctx);
    }

    private static class JCRQueryProviderAdapter implements QueryLanguageProvider<LegacyResourceProviderAdapter> {

        private final String[] languages;

        public JCRQueryProviderAdapter(String[] languages) {
            this.languages = languages;
        }

        @Override
        public String[] getSupportedLanguages(ResolverContext<LegacyResourceProviderAdapter> ctx) {
            return languages;
        }

        @Override
        public Iterator<Resource> findResources(ResolverContext<LegacyResourceProviderAdapter> ctx, String query,
                String language) {
            return ctx.getProviderState().getQueryLanguageProvider().findResources((ResolverContext) ctx, query, language);
        }

        @Override
        public Iterator<ValueMap> queryResources(ResolverContext<LegacyResourceProviderAdapter> ctx, String query,
                String language) {
            return ctx.getProviderState().getQueryLanguageProvider().queryResources((ResolverContext) ctx, query, language);
        }
    }
}
