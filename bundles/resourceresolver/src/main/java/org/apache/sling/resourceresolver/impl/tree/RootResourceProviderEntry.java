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
package org.apache.sling.resourceresolver.impl.tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.adapter.Adaptable;
import org.apache.sling.api.resource.AttributableResourceProvider;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.QueriableResourceProvider;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.resource.runtime.dto.AuthType;
import org.apache.sling.resourceresolver.impl.helper.ResourceResolverContext;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderHandler;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderTracker;
import org.apache.sling.spi.resource.provider.JCRQueryProvider;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the root resource provider entry which keeps track of the resource
 * providers.
 */
@Component
@Service(RootResourceProviderEntry.class)
public class RootResourceProviderEntry extends ResourceProviderEntry {

    /** Default logger */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /** Event admin. */
    @Reference
    private EventAdmin eventAdmin;

    @Reference
    private ResourceProviderTracker resourceProviderTracker;

    /**
     * Login into all required factories
     * @throws LoginException If login fails.
     */
    public void loginToRequiredFactories(final ResourceResolverContext ctx) throws LoginException {
        try {
            for (ResourceProviderHandler h : ctx.getProviders()) {
                if (h.getInfo().getAuthType() == AuthType.required) {
                    h.getResourceProvider().authenticate(ctx.getAuthenticationInfo());
                }
            }
        } catch (final LoginException le) {
            // login failed, so logout if already logged in providers
            ctx.close();
            throw le;
        }
    }

    /**
     * Invoke all resource providers and find an adaption
     * @see Adaptable
     */
    public <AdapterType> AdapterType adaptTo(final ResourceResolver resolver, final ResourceResolverContext ctx, final Class<AdapterType> type) {
        for (ResourceProviderHandler h :ctx.getProviders()) {
            ResolveContext resolveContext = ctx.getResolveContext(resolver, h);
            AdapterType adaptee = (AdapterType) h.getResourceProvider().adaptTo(resolveContext, type);
            if (adaptee != null) {
                return adaptee;
            }
        }
        return null;
    }

    /**
     * Invoke all queriable resource providers.
     * @see QueriableResourceProvider#findResources(ResourceResolver, String, String)
     */
    public Iterator<Resource> findResources(final ResourceResolverContext ctx,
                    final ResourceResolver resolver, final String query, final String language) {
        final Iterator<ResourceProviderHandler> i = getQueriableProviders(ctx, resolver, language);

        return new Iterator<Resource>() {

            private Resource nextObject = this.seek();

            private Iterator<Resource> nextResourceIter;
            
            private ProviderHandler actProviderHandler;
            
            private Resource seek() {
                Resource result = null;
                if ( nextResourceIter == null || !nextResourceIter.hasNext() ) {
                    nextResourceIter = null;
                    while ( i.hasNext() && nextResourceIter == null ) {
                        ResourceProviderHandler h = i.next();
                        ResolveContext resolveContext = ctx.getResolveContext(resolver, h);
                        nextResourceIter = h.getResourceProvider().getJCRQueryProvider().findResources(resolveContext, query, language);
                    }
                }
                if ( nextResourceIter != null ) {
                    while ( nextResourceIter.hasNext() && result == null ) {
                        result = nextResourceIter.next();
                        if ( actProviderHandler != null ) {
                            result = actProviderHandler.getReadableResource(ctx, result);
                        }
                    }
                    if ( result == null ) {
                        result = seek();
                    }
                }
                return result;
            }

            /**
             * @see java.util.Iterator#hasNext()
             */
            public boolean hasNext() {
                return this.nextObject != null;
            }

            /**
             * @see java.util.Iterator#next()
             */
            public Resource next() {
                if ( this.nextObject == null ) {
                    throw new NoSuchElementException();
                }
                final Resource result = this.nextObject;
                this.nextObject = this.seek();
                return result;
            }

            /**
             * @see java.util.Iterator#remove()
             */
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    private Iterator<ResourceProviderHandler> getQueriableProviders(final ResourceResolverContext ctx,
            final ResourceResolver resolver, final String language) {
        List<ResourceProviderHandler> queriableProviderList = new ArrayList<ResourceProviderHandler>();
        for (ResourceProviderHandler h : ctx.getProviders()) {
            ResolveContext resolveContext = ctx.getResolveContext(resolver, h);
            JCRQueryProvider jcrQueryProvider = h.getResourceProvider().getJCRQueryProvider();
            if (jcrQueryProvider == null) {
                continue;
            }
            String[] supported = jcrQueryProvider.getSupportedLanguages(resolveContext);
            if (ArrayUtils.contains(supported, language)) {
                queriableProviderList.add(h);
            }
        }
        final Iterator<ResourceProviderHandler> i = queriableProviderList.iterator();
        return i;
    }

    /**
     * Invoke all queriable resource providers.
     * @see QueriableResourceProvider#queryResources(ResourceResolver, String, String)
     */
    public Iterator<Map<String, Object>> queryResources(final ResourceResolverContext ctx,
                    final ResourceResolver resolver, final String query, final String language) {
        final Iterator<ResourceProviderHandler> i = getQueriableProviders(ctx, resolver, language);
        return new Iterator<Map<String, Object>>() {

            private ValueMap nextObject = this.seek();

            private Iterator<ValueMap> nextResourceIter;

            private ValueMap seek() {
                ValueMap result = null;
                if ( nextResourceIter == null || !nextResourceIter.hasNext() ) {
                    nextResourceIter = null;
                    while ( i.hasNext() && nextResourceIter == null ) {
                        ResourceProviderHandler h = i.next();
                        ResolveContext resolveContext = ctx.getResolveContext(resolver, h);
                        nextResourceIter = h.getResourceProvider().getJCRQueryProvider().queryResources(resolveContext, query, language);
                    }
                }
                if ( nextResourceIter != null ) {
                    while ( nextResourceIter.hasNext() && result == null ) {
                        result = nextResourceIter.next();
                    }
                    if ( result == null ) {
                        result = seek();
                    }
                }
                return result;
            }

            /**
             * @see java.util.Iterator#hasNext()
             */
            public boolean hasNext() {
                return this.nextObject != null;
            }

            /**
             * @see java.util.Iterator#next()
             */
            public ValueMap next() {
                if ( this.nextObject == null ) {
                    throw new NoSuchElementException();
                }
                final ValueMap result = this.nextObject;
                this.nextObject = this.seek();
                return result;
            }

            /**
             * @see java.util.Iterator#remove()
             */
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    private static final String FORBIDDEN_ATTRIBUTE = ResourceResolverFactory.PASSWORD;

    /**
     * Invoke all attributes providers and combine the result
     * @see AttributableResourceProvider#getAttributeNames(ResourceResolver)
     */
    public Iterator<String> getAttributeNames(final ResourceResolverContext ctx, final ResourceResolver resolver) {
        final Set<String> names = new HashSet<String>();
        if ( ctx.getAuthenticationInfo() != null ) {
            names.addAll(ctx.getAuthenticationInfo().keySet());
        }
        for (ResourceProviderHandler h : ctx.getProviders()) {
            ResolveContext resolveContext = ctx.getResolveContext(resolver, h);
            final Collection<String> newNames = h.getResourceProvider().getAttributeNames(resolveContext);
            if ( newNames != null ) {
                names.addAll(newNames);
            }
        }
        names.remove(FORBIDDEN_ATTRIBUTE);
        return names.iterator();
    }

    /**
     * Return the result from the first matching attributes provider
     * @see AttributableResourceProvider#getAttribute(ResourceResolver, String)
     */
    public Object getAttribute(final ResourceResolverContext ctx, final ResourceResolver resolver, final String name) {
        Object result = null;
        if (!FORBIDDEN_ATTRIBUTE.equals(name) )  {
            if (ctx.getAuthenticationInfo() != null) {
                result = ctx.getAuthenticationInfo().get(name);
            }
            if ( result == null ) {
                for (ResourceProviderHandler h : ctx.getProviders()) {
                    ResolveContext resolveContext = ctx.getResolveContext(resolver, h);
                    result = h.getResourceProvider().getAttribute(resolveContext, name);
                    if (result != null) {
                        break;
                    }
                }
            }
        }
        return result;
    }
}