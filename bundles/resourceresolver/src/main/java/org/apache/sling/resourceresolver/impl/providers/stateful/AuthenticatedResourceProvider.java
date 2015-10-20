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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This {@link StatefulResourceProvider} implementation authenticates the
 * underlying {@link ResourceProvider}. The authentication can be done during
 * creation of the object (for {@link AuthType#required}) or before invoking the
 * first method (for {@link AuthType#lazy}).
 */
public class AuthenticatedResourceProvider implements StatefulResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticatedResourceProvider.class);

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

    private ResolveContext<Object> getBasicContext() throws LoginException {
        if (cachedContext != null) {
            return cachedContext;
        }
        return cachedContext = getContext(null, null);
    }

    @Override
    public ResolveContext<Object> getContext(Map<String, String> parameters) throws LoginException {
        if (parameters == null || parameters.isEmpty()) {
            return getBasicContext();
        } else {
            return getContext(parameters, null);
        }
    }

    private ResolveContext<Object> getContext(Map<String, String> parameters, List<StatefulResourceProvider> parentProviders) throws LoginException {
        ResourceProvider<Object> parentProvider = null;
        ResolveContext<Object> parentContext = null;
        try {
            if (parentProviders != null && !parentProviders.isEmpty()) {
                StatefulResourceProvider statefulParentProvider = parentProviders.get(0);
                parentProvider = statefulParentProvider.getResourceProvider();
                parentContext = statefulParentProvider.getContext(parameters);
            }
        } catch (LoginException e) {
            logger.warn("Can't authenticate the parent resource provider", e);
        }
        return new BasicResolveContext<Object>(resolver, parameters, authenticate(), parentProvider, parentContext);
    }

    @Override
    public void logout() {
        if (authenticated) {
            try {
                rp.logout(getBasicContext().getProviderState());
            } catch (LoginException e) {
                logger.error("Can't create context", e);
            }
            authenticated = false;
            cachedContext = null;
        }
    }

    @Override
    public void refresh() {
        try {
            rp.refresh(getBasicContext());
        } catch (LoginException e) {
            logger.error("Can't create context", e);
        }
    }

    @Override
    public boolean isLive() {
        try {
            return rp.isLive(getBasicContext());
        } catch (LoginException e) {
            logger.error("Can't create context", e);
            return false;
        }
    }

    @Override
    public Resource getParent(Resource child, List<StatefulResourceProvider> parentProviders) {
        try {
            return rp.getParent(getContext(child.getResourceMetadata().getParameterMap(), parentProviders), child);
        } catch (LoginException e) {
            logger.error("Can't create context", e);
            return null;
        }
    }

    @Override
    public Resource getResource(String path, Resource parent, Map<String, String> parameters, boolean isResolve, List<StatefulResourceProvider> parentProviders) {
        try {
            return rp.getResource(getContext(parameters, parentProviders), path, parent);
        } catch (LoginException e) {
            logger.error("Can't create context", e);
            return null;
        }

    }

    @Override
    public Iterator<Resource> listChildren(Resource parent, List<StatefulResourceProvider> parentProviders) {
        try {
            return rp.listChildren(getContext(parent.getResourceMetadata().getParameterMap(), parentProviders), parent);
        } catch (LoginException e) {
            logger.error("Can't create context", e);
            return null;
        }
    }

    @Override
    public Collection<String> getAttributeNames() {
        Set<String> attributeNames = new LinkedHashSet<String>();
        Collection<String> rpAttributeNames = null;
        try {
            rpAttributeNames = rp.getAttributeNames(getBasicContext());
        } catch (LoginException e) {
            logger.error("Can't create context", e);
        }
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
        Object attribute = null;
        try {
            attribute = rp.getAttribute(getBasicContext(), name);
        } catch (LoginException e) {
            logger.error("Can't create context", e);
        }
        if (attribute == null) {
            attribute = authInfo.get(name);
        }
        return attribute;
    }

    @Override
    public Resource create(String path, Map<String, Object> properties, List<StatefulResourceProvider> parentProviders) throws PersistenceException {
        try {
            return rp.create(getContext(null, parentProviders), path, properties);
        } catch (LoginException e) {
            logger.error("Can't create context", e);
            return null;
        }
    }

    @Override
    public void delete(Resource resource, List<StatefulResourceProvider> parentProviders) throws PersistenceException {
        try {
            rp.delete(getContext(resource.getResourceMetadata().getParameterMap(), parentProviders), resource);
        } catch (LoginException e) {
            logger.error("Can't create context", e);
        }
    }

    @Override
    public void revert() {
        try {
            rp.revert(getBasicContext());
        } catch (LoginException e) {
            logger.error("Can't create context", e);
        }
    }

    @Override
    public void commit() throws PersistenceException {
        try {
            rp.commit(getBasicContext());
        } catch (LoginException e) {
            logger.error("Can't create context", e);
        }
    }

    @Override
    public boolean hasChanges() {
        try {
            return rp.hasChanges(getBasicContext());
        } catch (LoginException e) {
            logger.error("Can't create context", e);
            return false;
        }
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
        try {
            return provider.find(getBasicContext(), q, qi);
        } catch (LoginException e) {
            logger.error("Can't create context", e);
            return null;
        }
    }

    @Override
    public String[] getSupportedLanguages() {
        final JCRQueryProvider<Object> jcrQueryProvider = getJcrQueryProvider();
        if (jcrQueryProvider == null) {
            return null;
        }
        try {
            return jcrQueryProvider.getSupportedLanguages(getBasicContext());
        } catch (LoginException e) {
            logger.error("Can't create context", e);
            return ArrayUtils.EMPTY_STRING_ARRAY;
        }
    }

    @Override
    public Iterator<Resource> findResources(String query, String language) {
        final JCRQueryProvider<Object> jcrQueryProvider = getJcrQueryProvider();
        if (jcrQueryProvider == null) {
            return null;
        }
        try {
            return jcrQueryProvider.findResources(getBasicContext(), query, language);
        } catch (LoginException e) {
            logger.error("Can't create context", e);
            return null;
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Iterator<Map<String, Object>> queryResources(String query, String language) {
        final JCRQueryProvider<Object> jcrQueryProvider = getJcrQueryProvider();
        if (jcrQueryProvider == null) {
            return null;
        }
        try {
            return (Iterator) jcrQueryProvider.queryResources(getBasicContext(), query, language);
        } catch (LoginException e) {
            logger.error("Can't create context", e);
            return null;
        }
    }

    @Override
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        try {
            return rp.adaptTo(getBasicContext(), type);
        } catch (LoginException e) {
            logger.error("Can't create context", e);
            return null;
        }
    }

    @Override
    public boolean copy(String srcAbsPath, String destAbsPath, List<StatefulResourceProvider> parentProviders) throws PersistenceException {
        try {
            return rp.copy(getContext(null, parentProviders), srcAbsPath, destAbsPath);
        } catch (LoginException e) {
            logger.error("Can't create context", e);
            return false;
        }
    }

    @Override
    public boolean move(String srcAbsPath, String destAbsPath, List<StatefulResourceProvider> parentProviders) throws PersistenceException {
        try {
            return rp.move(getContext(null, parentProviders), srcAbsPath, destAbsPath);
        } catch (LoginException e) {
            logger.error("Can't create context", e);
            return false;
        }
    }

    @Override
    public ResourceResolver getResourceResolver() {
        return resolver;
    }

    @Override
    public ResourceProvider<Object> getResourceProvider() {
        return rp;
    }
}
