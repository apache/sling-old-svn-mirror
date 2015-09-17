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

import org.apache.sling.api.resource.AttributableResourceProvider;
import org.apache.sling.api.resource.DynamicResourceProvider;
import org.apache.sling.api.resource.ModifyingResourceProvider;
import org.apache.sling.api.resource.ParametrizableResourceProvider;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.QueriableResourceProvider;
import org.apache.sling.api.resource.RefreshableResourceProvider;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.spi.resource.provider.JCRQueryProvider;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;

@SuppressWarnings("deprecation")
public class LegacyResourceProviderAdapter extends ResourceProvider<Object> {

    private final org.apache.sling.api.resource.ResourceProvider rp;

    private final String[] languages;

    public LegacyResourceProviderAdapter(org.apache.sling.api.resource.ResourceProvider rp, String[] languages) {
        this.rp = rp;
        this.languages = languages;
    }

    @Override
    public Resource getResource(ResolveContext<Object> ctx, String path, Resource parent) {
        if (rp instanceof ParametrizableResourceProvider) {
            return ((ParametrizableResourceProvider) rp).getResource(ctx.getResourceResolver(), path,
                    ctx.getResolveParameters());
        } else {
            return rp.getResource(ctx.getResourceResolver(), path);
        }
    }

    @Override
    public Iterator<Resource> listChildren(ResolveContext<Object> ctx, Resource parent) {
        return rp.listChildren(parent);
    }

    @Override
    public void refresh(final @Nonnull ResolveContext<Object> ctx) {
        if (rp instanceof RefreshableResourceProvider) {
            ((RefreshableResourceProvider) rp).refresh();
        }
    }

    @Override
    public @CheckForNull JCRQueryProvider<Object> getJCRQueryProvider() {
        if (rp instanceof QueriableResourceProvider) {
            return new JCRQueryProviderAdapter((QueriableResourceProvider) rp, languages);
        } else {
            return super.getJCRQueryProvider();
        }
    }

    @Override
    public Collection<String> getAttributeNames(final @Nonnull ResolveContext<Object> ctx) {
        if (rp instanceof AttributableResourceProvider) {
            return ((AttributableResourceProvider) rp).getAttributeNames(ctx.getResourceResolver());
        } else {
            return super.getAttributeNames(ctx);
        }
    }

    @Override
    public Object getAttribute(final @Nonnull ResolveContext<Object> ctx, final @Nonnull String name) {
        if (rp instanceof AttributableResourceProvider) {
            return ((AttributableResourceProvider) rp).getAttribute(ctx.getResourceResolver(), name);
        } else {
            return super.getAttribute(ctx, name);
        }
    }

    @Override
    public boolean isLive(final @Nonnull ResolveContext<Object> ctx) {
        if (rp instanceof DynamicResourceProvider) {
            return ((DynamicResourceProvider) rp).isLive();
        } else {
            return super.isLive(ctx);
        }
    }

    @Override
    public void logout(final @Nonnull Object state) {
        if (rp instanceof DynamicResourceProvider) {
            ((DynamicResourceProvider) rp).close();
        }
    }

    @Override
    public Resource create(final @Nonnull ResolveContext<Object> ctx, final String path,
            final Map<String, Object> properties) throws PersistenceException {
        if (rp instanceof ModifyingResourceProvider) {
            return ((ModifyingResourceProvider) rp).create(ctx.getResourceResolver(), path, properties);
        } else {
            return super.create(ctx, path, properties);
        }
    }

    @Override
    public void delete(final @Nonnull ResolveContext<Object> ctx, final @Nonnull Resource resource)
            throws PersistenceException {
        if (rp instanceof ModifyingResourceProvider) {
            ((ModifyingResourceProvider) rp).delete(ctx.getResourceResolver(), resource.getPath());
        } else {
            super.delete(ctx, resource);
        }
    }

    @Override
    public void revert(final @Nonnull ResolveContext<Object> ctx) {
        if (rp instanceof ModifyingResourceProvider) {
            ((ModifyingResourceProvider) rp).revert(ctx.getResourceResolver());
        } else {
            super.revert(ctx);
        }
    }

    @Override
    public void commit(final @Nonnull ResolveContext<Object> ctx) throws PersistenceException {
        if (rp instanceof ModifyingResourceProvider) {
            ((ModifyingResourceProvider) rp).commit(ctx.getResourceResolver());
        } else {
            super.commit(ctx);
        }
    }

    @Override
    public boolean hasChanges(final @Nonnull ResolveContext<Object> ctx) {
        if (rp instanceof ModifyingResourceProvider) {
            return ((ModifyingResourceProvider) rp).hasChanges(ctx.getResourceResolver());
        } else {
            return super.hasChanges(ctx);
        }
    }

    private static class JCRQueryProviderAdapter implements JCRQueryProvider<Object> {

        private final QueriableResourceProvider rp;
        
        private final String[] languages;
        
        public JCRQueryProviderAdapter(QueriableResourceProvider rp, String[] languages) {
            this.rp = rp;
            this.languages = languages;
        }

        @Override
        public String[] getSupportedLanguages(ResolveContext<Object> ctx) {
            return languages;
        }

        @Override
        public Iterator<Resource> findResources(ResolveContext<Object> ctx, String query, String language) {
            return rp.findResources(ctx.getResourceResolver(), query, language);
        }

        @Override
        public Iterator<ValueMap> queryResources(ResolveContext<Object> ctx, String query, String language) {
            return rp.queryResources(ctx.getResourceResolver(), query, language);
        }
    }
}
