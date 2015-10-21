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
import org.apache.sling.spi.resource.provider.JCRQueryProvider;
import org.apache.sling.spi.resource.provider.ResolveContext;
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

    @Nonnull
    public LegacyResourceProviderAdapter authenticate(final @Nonnull Map<String, Object> authenticationInfo)
            throws LoginException {
        return new LegacyResourceProviderAdapter(rpFactory.getResourceProvider(authenticationInfo), languages, ownsRoot);
    }

    public void logout(final @Nonnull LegacyResourceProviderAdapter state) {
        state.logout(null);
    }

    @Override
    public Resource getResource(ResolveContext<LegacyResourceProviderAdapter> ctx, String path, Resource parent) {
        return ctx.getProviderState().getResource((ResolveContext) ctx, path, parent);
    }

    @Override
    public Iterator<Resource> listChildren(ResolveContext<LegacyResourceProviderAdapter> ctx, Resource parent) {
        return ctx.getProviderState().listChildren((ResolveContext) ctx, parent);
    }

    @Override
    public void refresh(final @Nonnull ResolveContext<LegacyResourceProviderAdapter> ctx) {
        ctx.getProviderState().refresh((ResolveContext) ctx);
    }

    @Override
    public @CheckForNull JCRQueryProvider<LegacyResourceProviderAdapter> getJCRQueryProvider() {
        if (ArrayUtils.isEmpty(languages)) {
            return super.getJCRQueryProvider();
        } else {
            return new JCRQueryProviderAdapter(languages);
        }
    }

    @Override
    public Collection<String> getAttributeNames(final @Nonnull ResolveContext<LegacyResourceProviderAdapter> ctx) {
        return ctx.getProviderState().getAttributeNames((ResolveContext) ctx);
    }

    @Override
    public Object getAttribute(final @Nonnull ResolveContext<LegacyResourceProviderAdapter> ctx,
            final @Nonnull String name) {
        return ctx.getProviderState().getAttribute((ResolveContext) ctx, name);
    }

    @Override
    public boolean isLive(final @Nonnull ResolveContext<LegacyResourceProviderAdapter> ctx) {
        return ctx.getProviderState().isLive((ResolveContext) ctx);
    }

    @Override
    public Resource create(final @Nonnull ResolveContext<LegacyResourceProviderAdapter> ctx, final String path,
            final Map<String, Object> properties) throws PersistenceException {
        return ctx.getProviderState().create((ResolveContext) ctx, path, properties);
    }

    @Override
    public void delete(final @Nonnull ResolveContext<LegacyResourceProviderAdapter> ctx,
            final @Nonnull Resource resource) throws PersistenceException {
        ctx.getProviderState().delete((ResolveContext) ctx, resource);
    }

    @Override
    public void revert(final @Nonnull ResolveContext<LegacyResourceProviderAdapter> ctx) {
        ctx.getProviderState().revert((ResolveContext) ctx);
    }

    @Override
    public void commit(final @Nonnull ResolveContext<LegacyResourceProviderAdapter> ctx) throws PersistenceException {
        ctx.getProviderState().commit((ResolveContext) ctx);
    }

    @Override
    public boolean hasChanges(final @Nonnull ResolveContext<LegacyResourceProviderAdapter> ctx) {
        return ctx.getProviderState().hasChanges((ResolveContext) ctx);
    }

    private static class JCRQueryProviderAdapter implements JCRQueryProvider<LegacyResourceProviderAdapter> {

        private final String[] languages;

        public JCRQueryProviderAdapter(String[] languages) {
            this.languages = languages;
        }

        @Override
        public String[] getSupportedLanguages(ResolveContext<LegacyResourceProviderAdapter> ctx) {
            return languages;
        }

        @Override
        public Iterator<Resource> findResources(ResolveContext<LegacyResourceProviderAdapter> ctx, String query,
                String language) {
            return ctx.getProviderState().getJCRQueryProvider().findResources((ResolveContext) ctx, query, language);
        }

        @Override
        public Iterator<ValueMap> queryResources(ResolveContext<LegacyResourceProviderAdapter> ctx, String query,
                String language) {
            return ctx.getProviderState().getJCRQueryProvider().queryResources((ResolveContext) ctx, query, language);
        }
    }
}
