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

import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.adapter.Adaptable;
import org.apache.sling.api.resource.AttributableResourceProvider;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.QueriableResourceProvider;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceProviderFactory;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.resourceresolver.impl.helper.ResourceResolverContext;
import org.apache.sling.resourceresolver.impl.helper.SortedProviderList;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the root resource provider entry which keeps track of the resource
 * providers.
 */
public class RootResourceProviderEntry extends ResourceProviderEntry {

    /** Default logger */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /** Event admin. */
    private EventAdmin eventAdmin;

    /** Array of required factories. */
    private ResourceProviderFactoryHandler[] requiredFactories = new ResourceProviderFactoryHandler[0];

    /** All adaptable resource providers. */
    private final SortedProviderList<Adaptable> adaptableProviders = new SortedProviderList<Adaptable>(Adaptable.class);

    /** All queriable resource providers. */
    private final SortedProviderList<QueriableResourceProvider> queriableProviders = new SortedProviderList<QueriableResourceProvider>(QueriableResourceProvider.class);

    /** All attributable resource providers. */
    private final SortedProviderList<AttributableResourceProvider> attributableProviders = new SortedProviderList<AttributableResourceProvider>(AttributableResourceProvider.class);

    public RootResourceProviderEntry() {
        super("/", null);
    }

    /**
     * Set or unset the event admin.
     */
    public void setEventAdmin(final EventAdmin ea) {
        this.eventAdmin = ea;
    }

    /**
     * Login into all required factories
     * @throws LoginException If login fails.
     */
    public void loginToRequiredFactories(final ResourceResolverContext ctx) throws LoginException {
        try {
            final ResourceProviderFactoryHandler[] factories = this.requiredFactories;
            for (final ResourceProviderFactoryHandler wrapper : factories) {
                wrapper.login(ctx);
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
    public <AdapterType> AdapterType adaptTo(final ResourceResolverContext ctx, final Class<AdapterType> type) {
        final Iterator<Adaptable> i = this.adaptableProviders.getProviders(ctx, null);
        AdapterType result = null;
        while ( result == null && i.hasNext() ) {
            final Adaptable adap = i.next();
            result = adap.adaptTo(type);
        }
        return result;
    }

    /**
     * Invoke all queriable resource providers.
     * @see QueriableResourceProvider#findResources(ResourceResolver, String, String)
     */
    public Iterator<Resource> findResources(final ResourceResolverContext ctx,
                    final ResourceResolver resolver, final String query, final String language) {
        final Iterator<QueriableResourceProvider> i = this.queriableProviders.getProviders(ctx,
                        new SortedProviderList.Filter<QueriableResourceProvider>() {

                            public boolean select(final ProviderHandler handler, final QueriableResourceProvider provider) {
                                return handler.supportsQueryLanguages(language);
                            }

                        });
        return new Iterator<Resource>() {

            private Resource nextObject = this.seek();

            private Iterator<Resource> nextResourceIter;

            private Resource seek() {
                Resource result = null;
                if ( nextResourceIter == null || !nextResourceIter.hasNext() ) {
                    nextResourceIter = null;
                    while ( i.hasNext() && nextResourceIter == null ) {
                        final QueriableResourceProvider adap = i.next();
                        nextResourceIter = adap.findResources(resolver, query, language);
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

    /**
     * Invoke all queriable resource providers.
     * @see QueriableResourceProvider#queryResources(ResourceResolver, String, String)
     */
    public Iterator<Map<String, Object>> queryResources(final ResourceResolverContext ctx,
                    final ResourceResolver resolver, final String query, final String language) {
        final Iterator<QueriableResourceProvider> i = this.queriableProviders.getProviders(ctx,
                        new SortedProviderList.Filter<QueriableResourceProvider>() {

            public boolean select(final ProviderHandler handler, final QueriableResourceProvider provider) {
                return handler.supportsQueryLanguages(language);
            }

        });
        return new Iterator<Map<String, Object>>() {

            private Map<String, Object> nextObject = this.seek();

            private Iterator<Map<String, Object>> nextResourceIter;

            private Map<String, Object> seek() {
                Map<String, Object> result = null;
                if ( nextResourceIter == null || !nextResourceIter.hasNext() ) {
                    nextResourceIter = null;
                    while ( i.hasNext() && nextResourceIter == null ) {
                        final QueriableResourceProvider adap = i.next();
                        nextResourceIter = adap.queryResources(resolver, query, language);
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
            public Map<String, Object> next() {
                if ( this.nextObject == null ) {
                    throw new NoSuchElementException();
                }
                final Map<String, Object> result = this.nextObject;
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
        final Iterator<AttributableResourceProvider> i = this.attributableProviders.getProviders(ctx, null);
        while ( i.hasNext() ) {
            final AttributableResourceProvider adap = i.next();
            final Collection<String> newNames = adap.getAttributeNames(resolver);
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
                final Iterator<AttributableResourceProvider> i = this.attributableProviders.getProviders(ctx, null);
                while ( result == null && i.hasNext() ) {
                    final AttributableResourceProvider adap = i.next();
                    result = adap.getAttribute(resolver, name);
                }
            }
        }
        return result;
    }

    /**
     * Bind a resource provider.
     */
    public void bindResourceProvider(final ResourceProvider provider, final Map<String, Object> props) {
        final ResourceProviderHandler handler = new ResourceProviderHandler(provider, props);

        this.bindHandler(handler);
        this.adaptableProviders.add(handler);
        this.queriableProviders.add(handler);
        this.attributableProviders.add(handler);
    }

    /**
     * Unbind a resource provider.
     */
    public void unbindResourceProvider(final ResourceProvider provider, final Map<String, Object> props) {
        final ResourceProviderHandler handler = new ResourceProviderHandler(provider, props);

        this.unbindHandler(handler);
        this.adaptableProviders.remove(handler);
        this.queriableProviders.remove(handler);
        this.attributableProviders.remove(handler);
    }

    /**
     * Bind a resource provider factory.
     */
    public void bindResourceProviderFactory(final ResourceProviderFactory factory, final Map<String, Object> props) {
        final ResourceProviderFactoryHandler handler = new ResourceProviderFactoryHandler(factory, props);

        this.bindHandler(handler);
        this.adaptableProviders.add(handler);
        this.queriableProviders.add(handler);
        this.attributableProviders.add(handler);

        final boolean required = PropertiesUtil.toBoolean(props.get(ResourceProviderFactory.PROPERTY_REQUIRED), false);
        if (required) {
            synchronized (this) {
                final List<ResourceProviderFactoryHandler> factories = new LinkedList<ResourceProviderFactoryHandler>();
                factories.addAll(Arrays.asList(this.requiredFactories));
                factories.add(handler);
                this.requiredFactories = factories.toArray(new ResourceProviderFactoryHandler[factories.size()]);
            }
        }
    }

    /**
     * Unbind a resource provider factory
     */
    public void unbindResourceProviderFactory(final ResourceProviderFactory factory, final Map<String, Object> props) {
        final ResourceProviderFactoryHandler handler = new ResourceProviderFactoryHandler(factory, props);

        this.unbindHandler(handler);
        this.adaptableProviders.remove(handler);
        this.queriableProviders.remove(handler);
        this.attributableProviders.remove(handler);

        final boolean required = PropertiesUtil.toBoolean(props.get(ResourceProviderFactory.PROPERTY_REQUIRED), false);
        if (required) {
            synchronized (this) {
                final List<ResourceProviderFactoryHandler> factories = new LinkedList<ResourceProviderFactoryHandler>();
                factories.addAll(Arrays.asList(this.requiredFactories));
                factories.remove(handler);
                this.requiredFactories = factories.toArray(new ResourceProviderFactoryHandler[factories.size()]);
            }
        }
    }

    /**
     * Bind a resource provider wrapper
     */
    private void bindHandler(final ProviderHandler provider) {
        // this is just used for debug logging
        final String debugServiceName = getDebugServiceName(provider);

        logger.debug("bindResourceProvider: Binding {}", debugServiceName);

        final String[] roots = provider.getRoots();
        boolean foundRoot = false;
        if (roots != null) {
            final EventAdmin localEA = this.eventAdmin;
            for (final String root : roots) {
                foundRoot = true;

                this.addResourceProvider(root, provider);

                logger.debug("bindResourceProvider: {}={} ({})", new Object[] { root, provider, debugServiceName });
                if (localEA != null) {
                    final Dictionary<String, Object> eventProps = new Hashtable<String, Object>();
                    eventProps.put(SlingConstants.PROPERTY_PATH, root);
                    localEA.postEvent(new Event(SlingConstants.TOPIC_RESOURCE_PROVIDER_ADDED, eventProps));
                }
            }
        }
        if ( !foundRoot ) {
            logger.info("Ignoring ResourceProvider(Factory) {} : no configured roots.", provider.getName());
        }
        logger.debug("bindResourceProvider: Bound {}", debugServiceName);
    }

    /**
     * Unbind a resource provider wrapper
     */
    private void unbindHandler(final ProviderHandler provider) {
        // this is just used for debug logging
        final String debugServiceName = getDebugServiceName(provider);

        logger.debug("unbindResourceProvider: Unbinding {}", debugServiceName);

        final String[] roots = provider.getRoots();
        if (roots != null) {

            final EventAdmin localEA = this.eventAdmin;

            for (final String root : roots) {

                this.removeResourceProvider(root, provider);

                logger.debug("unbindResourceProvider: root={} ({})", root, debugServiceName);
                if (localEA != null) {
                    final Dictionary<String, Object> eventProps = new Hashtable<String, Object>();
                    eventProps.put(SlingConstants.PROPERTY_PATH, root);
                    localEA.postEvent(new Event(SlingConstants.TOPIC_RESOURCE_PROVIDER_REMOVED, eventProps));
                }
            }
        }

        logger.debug("unbindResourceProvider: Unbound {}", debugServiceName);
    }

    private String getDebugServiceName(final ProviderHandler provider) {
        if (logger.isDebugEnabled()) {
            return provider.getName();
        }

        return null;
    }
}