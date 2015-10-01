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
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.sling.api.SlingException;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.query.Query;
import org.apache.sling.api.resource.query.QueryInstructions;
import org.apache.sling.api.resource.runtime.dto.AuthType;
import org.apache.sling.resourceresolver.impl.BasicResolveContext;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderInfo;
import org.apache.sling.spi.resource.provider.JCRQueryProvider;
import org.apache.sling.spi.resource.provider.QueryProvider;
import org.apache.sling.spi.resource.provider.QueryResult;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;

/**
 * This {@link StatefulResourceProvider} implementation authenticates the
 * underlying {@link ResourceProvider}. The authentication can be done during
 * creation of the object (for {@link AuthType#required}) or before invoking the
 * first method (for {@link AuthType#lazy}).
 */
public class AuthenticatedResourceProvider implements StatefulResourceProvider {

    private static final String FORBIDDEN_ATTRIBUTE = ResourceResolverFactory.PASSWORD;

    private final ResourceProvider<Object> rp;

    private final ResourceProviderInfo info;

    private final Map<String, Object> authInfo;

    private final ResourceResolver resolver;

    private boolean authenticated;

    private Object contextData;

    private ResolveContext<Object> cachedContext;

    private QueryProvider<Object> cachedQueryProvider;

    private JCRQueryProvider<Object> cachedJcrQueryProvider;

    @SuppressWarnings("unchecked")
    public AuthenticatedResourceProvider(ResourceProvider<?> rp, ResourceProviderInfo info, ResourceResolver resolver,
            Map<String, Object> authInfo) throws LoginException {
        this.rp = (ResourceProvider<Object>) rp;
        this.info = info;
        this.authInfo = authInfo;
        this.resolver = resolver;
        if (info.getAuthType() == AuthType.required) {
            authenticate();
        }
    }

    private Object authenticate() throws LoginException {
        if (!authenticated && (info.getAuthType() == AuthType.required || info.getAuthType() == AuthType.lazy)) {
            contextData = rp.authenticate(authInfo);
            authenticated = true;
        }
        return contextData;
    }

    private ResolveContext<Object> getContext() {
        if (cachedContext != null) {
            return cachedContext;
        }
        return cachedContext = getContext(Collections.<String, String> emptyMap());
    }

    private ResolveContext<Object> getContext(Map<String, String> parameters) {
        try {
            return new BasicResolveContext(resolver, parameters, authenticate());
        } catch (LoginException e) {
            throw new SlingException("Can't authenticate", e);
        }
    }

    @Override
    public void logout() {
        if (authenticated) {
            rp.logout(getContext().getProviderState());
            authenticated = false;
            cachedContext = null;
        }
    }

    @Override
    public void refresh() {
        rp.refresh(getContext());
    }

    @Override
    public boolean isLive() {
        return rp.isLive(getContext());
    }

    @Override
    public Resource getParent(Resource child) {
        return rp.getParent(getContext(child.getResourceMetadata().getParameterMap()), child);
    }

    @Override
    public Resource getResource(String path, Resource parent, Map<String, String> parameters, boolean isResolve) {
        return rp.getResource(getContext(parameters), path, parent);

    }

    @Override
    public Iterator<Resource> listChildren(Resource parent) {
        return rp.listChildren(getContext(parent.getResourceMetadata().getParameterMap()), parent);
    }

    @Override
    public Collection<String> getAttributeNames() {
        Set<String> attributeNames = new LinkedHashSet<String>();
        Collection<String> rpAttributeNames = rp.getAttributeNames(getContext());
        if (rpAttributeNames != null) {
            attributeNames.addAll(rpAttributeNames);
        }
        if (authInfo != null) {
            attributeNames.addAll(authInfo.keySet());
        }
        attributeNames.remove(FORBIDDEN_ATTRIBUTE);
        return attributeNames;
    }

    @Override
    public Object getAttribute(String name) {
        if (FORBIDDEN_ATTRIBUTE.equals(name)) {
            return null;
        }
        Object attribute = rp.getAttribute(getContext(), name);
        if (attribute == null) {
            attribute = authInfo.get(name);
        }
        return attribute;
    }

    @Override
    public Resource create(String path, Map<String, Object> properties) throws PersistenceException {
        return rp.create(getContext(), path, properties);
    }

    @Override
    public void delete(Resource resource) throws PersistenceException {
        rp.delete(getContext(resource.getResourceMetadata().getParameterMap()), resource);
    }

    @Override
    public void revert() {
        rp.revert(getContext());
    }

    @Override
    public void commit() throws PersistenceException {
        rp.commit(getContext());
    }

    @Override
    public boolean hasChanges() {
        return rp.hasChanges(getContext());
    }

    private QueryProvider<Object> getQueryProvider() {
        if (cachedQueryProvider == null) {
            cachedQueryProvider = rp.getQueryProvider();
        }
        return cachedQueryProvider;
    }

    private JCRQueryProvider<Object> getJcrQueryProvider() {
        if (cachedJcrQueryProvider == null) {
            cachedJcrQueryProvider = rp.getJCRQueryProvider();
        }
        return cachedJcrQueryProvider;
    }

    @Override
    public QueryResult find(Query q, QueryInstructions qi) {
        final QueryProvider<Object> provider = getQueryProvider();
        if (provider == null) {
            return null;
        }
        return provider.find(getContext(), q, qi);
    }

    @Override
    public String[] getSupportedLanguages() {
        final JCRQueryProvider<Object> jcrQueryProvider = getJcrQueryProvider();
        if (jcrQueryProvider == null) {
            return null;
        }
        return jcrQueryProvider.getSupportedLanguages(getContext());
    }

    @Override
    public Iterator<Resource> findResources(String query, String language) {
        final JCRQueryProvider<Object> jcrQueryProvider = getJcrQueryProvider();
        if (jcrQueryProvider == null) {
            return null;
        }
        return jcrQueryProvider.findResources(getContext(), query, language);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Iterator<Map<String, Object>> queryResources(String query, String language) {
        final JCRQueryProvider<Object> jcrQueryProvider = getJcrQueryProvider();
        if (jcrQueryProvider == null) {
            return null;
        }
        return (Iterator) jcrQueryProvider.queryResources(getContext(), query, language);
    }

    @Override
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        return rp.adaptTo(getContext(), type);
    }

    @Override
    public boolean copy(String srcAbsPath, String destAbsPath) throws PersistenceException {
        return rp.copy(getContext(), srcAbsPath, destAbsPath);
    }

    @Override
    public boolean move(String srcAbsPath, String destAbsPath) throws PersistenceException {
        return rp.move(getContext(), srcAbsPath, destAbsPath);
    }

    @Override
    public ResourceResolver getResourceResolver() {
        return resolver;
    }
}
