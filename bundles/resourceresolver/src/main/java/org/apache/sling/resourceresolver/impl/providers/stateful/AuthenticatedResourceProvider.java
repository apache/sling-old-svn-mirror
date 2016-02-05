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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.runtime.dto.AuthType;
import org.apache.sling.spi.resource.provider.QueryLanguageProvider;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;

/**
 * This {@link StatefulResourceProvider} implementation authenticates the
 * underlying {@link ResourceProvider}. The authentication can be done during
 * creation of the object (for {@link AuthType#required}) or before invoking the
 * first method (for {@link AuthType#lazy}).
 */
public class AuthenticatedResourceProvider {

    private static final String FORBIDDEN_ATTRIBUTE = ResourceResolverFactory.PASSWORD;

    public static final AuthenticatedResourceProvider UNAUTHENTICATED_PROVIDER = new AuthenticatedResourceProvider(null, null);

    private final ResourceProvider<Object> provider;

    private final ResolveContext<Object> cachedContext;

    private volatile boolean authenticated;


    public AuthenticatedResourceProvider(final ResourceProvider<Object> provider,
            final ResolveContext<Object> resolveContext) {
        this.provider = provider;
        this.cachedContext = resolveContext;
    }

    public ResolveContext<Object> getResolveContext() {
        return this.cachedContext;
    }

    public void refresh() {
        this.provider.refresh(this.cachedContext);
    }

    public boolean isLive() {
        return this.provider.isLive(this.cachedContext);
    }

    public Resource getParent(final Resource child) {
        return this.provider.getParent(this.cachedContext, child);
    }

    public Resource getResource(String path, Resource parent, final Map<String, String> parameters, boolean isResolve) {
        ResourceContext resourceContext = ResourceContext.EMPTY_CONTEXT;
        if ( parameters != null ) {
            resourceContext = new ResourceContext() {

                @Override
                public Map<String, String> getResolveParameters() {
                    return parameters;
                }
            };
        }
        return this.provider.getResource(this.cachedContext, path, resourceContext, parent);
    }

    public Iterator<Resource> listChildren(Resource parent) {
        return this.provider.listChildren(this.cachedContext, parent);
    }

    public Collection<String> getAttributeNames(final Map<String, Object> authInfo) {
        Set<String> attributeNames = new LinkedHashSet<String>();
        Collection<String> rpAttributeNames = this.provider.getAttributeNames(this.cachedContext);
        if (rpAttributeNames != null) {
            attributeNames.addAll(rpAttributeNames);
        }
        if (authInfo != null) {
            attributeNames.addAll(authInfo.keySet());
        }
        attributeNames.remove(FORBIDDEN_ATTRIBUTE);
        return attributeNames;
    }

    public Object getAttribute(final String name, final Map<String, Object> authInfo) {
        if (FORBIDDEN_ATTRIBUTE.equals(name)) {
            return null;
        }
        Object attribute = this.provider.getAttribute(this.cachedContext, name);
        if (attribute == null) {
            attribute = authInfo.get(name);
        }
        return attribute;
    }

    public Resource create(final ResourceResolver resolver, String path, Map<String, Object> properties) throws PersistenceException {
        return this.provider.create(this.cachedContext, path, properties);
    }

    public void delete(Resource resource) throws PersistenceException {
        this.provider.delete(this.cachedContext, resource);
    }

    public void revert() {
        this.provider.revert(this.cachedContext);
    }

    public void commit() throws PersistenceException {
        this.provider.commit(this.cachedContext);
    }

    public boolean hasChanges() {
        return this.provider.hasChanges(this.cachedContext);
    }

    private QueryLanguageProvider<Object> getQueryLanguageProvider() {
        return this.provider.getQueryLanguageProvider();
    }

    public String[] getSupportedLanguages() {
        final QueryLanguageProvider<Object> jcrQueryProvider = getQueryLanguageProvider();
        if (jcrQueryProvider == null) {
            return null;
        }
        return jcrQueryProvider.getSupportedLanguages(this.cachedContext);
    }

    public Iterator<Resource> findResources(String query, String language) {
        final QueryLanguageProvider<Object> jcrQueryProvider = getQueryLanguageProvider();
        if (jcrQueryProvider == null) {
            return null;
        }
        return jcrQueryProvider.findResources(this.cachedContext, query, language);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Iterator<Map<String, Object>> queryResources(String query, String language) {
        final QueryLanguageProvider<Object> jcrQueryProvider = getQueryLanguageProvider();
        if (jcrQueryProvider == null) {
            return null;
        }
        return (Iterator) jcrQueryProvider.queryResources(this.cachedContext, query, language);
    }

    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        return this.provider.adaptTo(this.cachedContext, type);
    }

    public boolean copy(String srcAbsPath, String destAbsPath) throws PersistenceException {
        return this.provider.copy(this.cachedContext, srcAbsPath, destAbsPath);
    }

    public boolean move(String srcAbsPath, String destAbsPath) throws PersistenceException {
        return this.provider.move(this.cachedContext, srcAbsPath, destAbsPath);
    }

    @Override
    public String toString() {
        return "[" + getClass().getSimpleName() + "# rp: " + this.provider + ", authenticated: " + authenticated + "]";
    }
}
