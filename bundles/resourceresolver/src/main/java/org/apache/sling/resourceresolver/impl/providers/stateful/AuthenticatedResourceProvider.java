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

import org.apache.commons.lang.ArrayUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.runtime.dto.AuthType;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderInfo;
import org.apache.sling.spi.resource.provider.QueryLanguageProvider;
import org.apache.sling.spi.resource.provider.ResolverContext;
import org.apache.sling.spi.resource.provider.ResourceContext;
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

    private ResolverContext<Object> cachedContext;

    private final CombinedResourceProvider combinedProvider;

    @SuppressWarnings("unchecked")
    public AuthenticatedResourceProvider(ResourceProvider<?> rp,
            ResourceProviderInfo info,
            ResourceResolver resolver,
            Map<String, Object> authInfo,
            CombinedResourceProvider combinedProvider) throws LoginException {
        this.rp = (ResourceProvider<Object>) rp;
        this.info = info;
        this.authInfo = authInfo;
        this.resolver = resolver;
        this.combinedProvider = combinedProvider;
        if (info.getAuthType() == AuthType.required) {
            authenticate();
        }
    }

    private Object authenticate() throws LoginException {
        if (!authenticated && (info.getAuthType() == AuthType.required || info.getAuthType() == AuthType.lazy)) {
            try {
                contextData = rp.authenticate(authInfo);
            } catch ( final LoginException le ) {
                logger.debug("Unable to login into resource provider " + rp, le);
                throw le;
            }
            authenticated = true;
        }
        return contextData;
    }

    @Override
    public ResolverContext<Object> getContext() throws LoginException {
        if (cachedContext == null) {
            cachedContext = new BasicResolveContext<Object>(resolver, authenticate(), ResourceUtil.getParent(info.getPath()), this.combinedProvider);
        }
        return cachedContext;
    }

    @Override
    public void logout() {
        if (authenticated) {
            try {
                rp.logout(getContext());
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
            rp.refresh(getContext());
        } catch (LoginException e) {
            logger.error("Can't create context", e);
        }
    }

    @Override
    public boolean isLive() {
        try {
            return rp.isLive(getContext());
        } catch (LoginException e) {
            logger.error("Can't create context", e);
            return false;
        }
    }

    @Override
    public Resource getParent(Resource child) {
        try {
            return rp.getParent(getContext(), child);
        } catch (LoginException e) {
            logger.error("Can't create context", e);
            return null;
        }
    }

    @Override
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
        try {
            return rp.getResource(getContext(), path, resourceContext, parent);
        } catch (LoginException e) {
            logger.error("Can't create context", e);
            return null;
        }

    }

    @Override
    public Iterator<Resource> listChildren(Resource parent) {
        try {
            return rp.listChildren(getContext(), parent);
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
            rpAttributeNames = rp.getAttributeNames(getContext());
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
            attribute = rp.getAttribute(getContext(), name);
        } catch (LoginException e) {
            logger.error("Can't create context", e);
        }
        if (attribute == null) {
            attribute = authInfo.get(name);
        }
        return attribute;
    }

    @Override
    public Resource create(final ResourceResolver resolver, String path, Map<String, Object> properties) throws PersistenceException {
        try {
            return rp.create(getContext(), path, properties);
        } catch (LoginException e) {
            logger.error("Can't create context", e);
            return null;
        }
    }

    @Override
    public void delete(Resource resource) throws PersistenceException {
        try {
            rp.delete(getContext(), resource);
        } catch (LoginException e) {
            logger.error("Can't create context", e);
        }
    }

    @Override
    public void revert() {
        try {
            rp.revert(getContext());
        } catch (LoginException e) {
            logger.error("Can't create context", e);
        }
    }

    @Override
    public void commit() throws PersistenceException {
        try {
            rp.commit(getContext());
        } catch (LoginException e) {
            logger.error("Can't create context", e);
        }
    }

    @Override
    public boolean hasChanges() {
        try {
            return rp.hasChanges(getContext());
        } catch (LoginException e) {
            logger.error("Can't create context", e);
            return false;
        }
    }

    private QueryLanguageProvider<Object> getQueryLanguageProvider() {
        return rp.getQueryLanguageProvider();
    }

    @Override
    public String[] getSupportedLanguages() {
        final QueryLanguageProvider<Object> jcrQueryProvider = getQueryLanguageProvider();
        if (jcrQueryProvider == null) {
            return null;
        }
        try {
            return jcrQueryProvider.getSupportedLanguages(getContext());
        } catch (LoginException e) {
            logger.error("Can't create context", e);
            return ArrayUtils.EMPTY_STRING_ARRAY;
        }
    }

    @Override
    public Iterator<Resource> findResources(String query, String language) {
        final QueryLanguageProvider<Object> jcrQueryProvider = getQueryLanguageProvider();
        if (jcrQueryProvider == null) {
            return null;
        }
        try {
            return jcrQueryProvider.findResources(getContext(), query, language);
        } catch (LoginException e) {
            logger.error("Can't create context", e);
            return null;
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Iterator<Map<String, Object>> queryResources(String query, String language) {
        final QueryLanguageProvider<Object> jcrQueryProvider = getQueryLanguageProvider();
        if (jcrQueryProvider == null) {
            return null;
        }
        try {
            return (Iterator) jcrQueryProvider.queryResources(getContext(), query, language);
        } catch (LoginException e) {
            logger.error("Can't create context", e);
            return null;
        }
    }

    @Override
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        try {
            return rp.adaptTo(getContext(), type);
        } catch (LoginException e) {
            logger.error("Can't create context", e);
            return null;
        }
    }

    @Override
    public boolean copy(String srcAbsPath, String destAbsPath) throws PersistenceException {
        try {
            return rp.copy(getContext(), srcAbsPath, destAbsPath);
        } catch (LoginException e) {
            throw new PersistenceException("Unable to create context.", e);
        }
    }

    @Override
    public boolean move(String srcAbsPath, String destAbsPath) throws PersistenceException {
        try {
            return rp.move(getContext(), srcAbsPath, destAbsPath);
        } catch (LoginException e) {
            throw new PersistenceException("Unable to create context.", e);
        }
    }

    @Override
    public ResourceProvider<Object> getResourceProvider() {
        return rp;
    }

    @Override
    public String toString() {
        return "[" + getClass().getSimpleName() + "# rp: " + rp + ", authenticated: " + authenticated + "]";
    }
}
