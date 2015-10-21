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
package org.apache.sling.resourceresolver.impl;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.collections.BidiMap;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.resourceresolver.impl.console.ResourceResolverWebConsolePlugin;
import org.apache.sling.resourceresolver.impl.helper.ResourceDecoratorTracker;
import org.apache.sling.resourceresolver.impl.helper.ResourceResolverContext;
import org.apache.sling.resourceresolver.impl.mapping.MapConfigurationProvider;
import org.apache.sling.resourceresolver.impl.mapping.MapEntries;
import org.apache.sling.resourceresolver.impl.mapping.Mapping;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderTracker;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>CommonResourceResolverFactoryImpl</code> is a singleton
 * implementing the shared/common functionality of all resource
 * resolver factories.
 */
public class CommonResourceResolverFactoryImpl implements ResourceResolverFactory, MapConfigurationProvider {

    /** Helper for the resource resolver. */
    private MapEntries mapEntries = MapEntries.EMPTY;

    /** The web console plugin. */
    private ResourceResolverWebConsolePlugin plugin;

    /** The activator */
    private final ResourceResolverFactoryActivator activator;

    /**
     * Thread local holding the resource resolver stack
     */
    private ThreadLocal<Stack<WeakReference<ResourceResolver>>> resolverStackHolder = new ThreadLocal<Stack<WeakReference<ResourceResolver>>>();

    /** Flag indicating whether this factory is still active. */
    private final AtomicBoolean isActive = new AtomicBoolean(true);

    /** The reference queue to handle disposing of resource resolver instances. */
    private final ReferenceQueue<ResourceResolver> resolverReferenceQueue = new ReferenceQueue<ResourceResolver>();

    /** All weak references for the resource resolver instances. */
    private final Map<Integer, ResolverWeakReference> refs = new ConcurrentHashMap<Integer, CommonResourceResolverFactoryImpl.ResolverWeakReference>();

    /** Background thread handling disposing of resource resolver instances. */
    private final Thread refQueueThread;

    private boolean logResourceResolverClosing = false;

    /**
     * Create a new common resource resolver factory.
     */
    public CommonResourceResolverFactoryImpl(final ResourceResolverFactoryActivator activator) {
        this.activator = activator;
        this.logResourceResolverClosing = activator.shouldLogResourceResolverClosing();
        this.refQueueThread = new Thread("Apache Sling Resource Resolver Finalizer Thread") {

            @Override
            public void run() {
                while ( isActive.get() ) {
                    try {
                        final ResolverWeakReference ref = (ResolverWeakReference) resolverReferenceQueue.remove();
                        try {
                            ref.close();
                        } catch ( final Throwable t ) {
                            // we ignore everything from there to not stop this thread
                        }
                        refs.remove(ref.context.hashCode());
                    } catch ( final InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
                for(final ResolverWeakReference ref : refs.values()) {
                    ref.close();
                }
                refs.clear();
            }

        };
        this.refQueueThread.setDaemon(true);
        this.refQueueThread.start();
    }

    // ---------- Resource Resolver Factory ------------------------------------

    /**
     * @see org.apache.sling.api.resource.ResourceResolverFactory#getAdministrativeResourceResolver(java.util.Map)
     */
    @Override
    public ResourceResolver getAdministrativeResourceResolver(final Map<String, Object> passedAuthenticationInfo)
    throws LoginException {
        if ( !isActive.get() ) {
            throw new LoginException("ResourceResolverFactory is deactivated.");
        }

        // create a copy of the passed authentication info as we modify the map
        final Map<String, Object> authenticationInfo = new HashMap<String, Object>();
        authenticationInfo.put(ResourceProvider.AUTH_ADMIN, Boolean.TRUE);
        if ( passedAuthenticationInfo != null ) {
            authenticationInfo.putAll(passedAuthenticationInfo);
            // make sure there is no leaking of service bundle and info props
            authenticationInfo.remove(ResourceProvider.AUTH_SERVICE_BUNDLE);
            authenticationInfo.remove(SUBSERVICE);
        }

        return getResourceResolverInternal(authenticationInfo, true);
    }

    /**
     * @see org.apache.sling.api.resource.ResourceResolverFactory#getResourceResolver(java.util.Map)
     */
    @Override
    public ResourceResolver getResourceResolver(final Map<String, Object> passedAuthenticationInfo)
    throws LoginException {
        if ( !isActive.get() ) {
            throw new LoginException("ResourceResolverFactory is deactivated.");
        }

        // create a copy of the passed authentication info as we modify the map
        final Map<String, Object> authenticationInfo = new HashMap<String, Object>();
        if ( passedAuthenticationInfo != null ) {
            authenticationInfo.putAll(passedAuthenticationInfo);
            // make sure there is no leaking of service bundle and info props
            authenticationInfo.remove(ResourceProvider.AUTH_SERVICE_BUNDLE);
            authenticationInfo.remove(SUBSERVICE);
        }

        final ResourceResolver result = getResourceResolverInternal(authenticationInfo, false);
        Stack<WeakReference<ResourceResolver>> resolverStack = resolverStackHolder.get();
        if ( resolverStack == null ) {
            resolverStack = new Stack<WeakReference<ResourceResolver>>();
            resolverStackHolder.set(resolverStack);
        }
        resolverStack.push(new WeakReference<ResourceResolver>(result));
        return result;
    }


    /**
     * @see org.apache.sling.api.resource.ResourceResolverFactory#getThreadResourceResolver()
     */
    @Override
    public ResourceResolver getThreadResourceResolver() {
        if ( !isActive.get() ) {
            return null;
        }

        ResourceResolver result = null;
        final Stack<WeakReference<ResourceResolver>> resolverStack = resolverStackHolder.get();
        if ( resolverStack != null) {
            while ( result == null && !resolverStack.isEmpty() ) {
                result = resolverStack.peek().get();
                if ( result == null ) {
                    resolverStack.pop();
                }
            }
        }
        return result;
    }

    // ---------- Implementation helpers --------------------------------------

    /**
     * Inform about a new resource resolver instance.
     * We create a weak reference to be able to close the resolver if close on the
     * resource resolver is never called.
     * @param resolver The resource resolver
     * @param ctx The resource resolver context
     */
    public void register(final ResourceResolver resolver,
            final ResourceResolverContext ctx) {
        // create new weak reference
        refs.put(ctx.hashCode(), new ResolverWeakReference(resolver, this.resolverReferenceQueue, ctx));
    }

    /**
     * Inform about a closed resource resolver.
     * Make sure to remove it from the current thread context.
     * @param resourceResolverImpl The resource resolver
     * @param ctx The resource resolver context
     */
    public void unregister(final ResourceResolver resourceResolverImpl,
            final ResourceResolverContext ctx) {
        // close the context
        ctx.close();
        // remove it from the set of weak references.
        refs.remove(ctx.hashCode());

        // on shutdown, the factory might already be closed before the resolvers close
        // therefore we have to check for null
        final ThreadLocal<Stack<WeakReference<ResourceResolver>>> tl = resolverStackHolder;
        if ( tl != null ) {
            final Stack<WeakReference<ResourceResolver>> resolverStack = tl.get();
            if ( resolverStack != null ) {
                final Iterator<WeakReference<ResourceResolver>> i = resolverStack.iterator();
                while ( i.hasNext() ) {
                    final WeakReference<ResourceResolver> ref = i.next();
                    if ( ref.get() == null || ref.get() == resourceResolverImpl ) {
                        i.remove();
                    }
                }
                if ( resolverStack.isEmpty() ) {
                    tl.remove();
                }
            }
        }
    }

    /**
     * Create a new ResourceResolver
     * @param authenticationInfo The authentication map
     * @param isAdmin is an administrative resolver requested?
     * @return A resource resolver
     * @throws LoginException if login to any of the required resource providers fails.
     */
    public ResourceResolver getResourceResolverInternal(final Map<String, Object> authenticationInfo,
                    final boolean isAdmin)
    throws LoginException {
        if ( !isActive.get() ) {
            throw new LoginException("ResourceResolverFactory is deactivated.");
        }

        return new ResourceResolverImpl(this, isAdmin, authenticationInfo);
    }

    public MapEntries getMapEntries() {
        return mapEntries;
    }

    /** Activates this component */
    protected void activate(final BundleContext bundleContext) {
        final Logger logger = LoggerFactory.getLogger(getClass());
        try {
            plugin = new ResourceResolverWebConsolePlugin(bundleContext, this);
        } catch (final Throwable ignore) {
            // an exception here probably means the web console plugin is not
            // available
            logger.debug("activate: unable to setup web console plugin.", ignore);
        }
        // set up the map entries from configuration
        try {
            mapEntries = new MapEntries(this, bundleContext, this.activator.getEventAdmin());
        } catch (final Exception e) {
            logger.error("activate: Cannot access repository, failed setting up Mapping Support", e);
        }
    }

    /**
     * Deactivates this component
     */
    protected void deactivate() {
        isActive.set(false);
        this.refQueueThread.interrupt();
        if (plugin != null) {
            plugin.dispose();
            plugin = null;
        }

        if (mapEntries != null) {
            mapEntries.dispose();
            mapEntries = MapEntries.EMPTY;
        }
        resolverStackHolder = null;
    }

    public ResourceDecoratorTracker getResourceDecoratorTracker() {
        return this.activator.getResourceDecoratorTracker();
    }

    public String[] getSearchPath() {
        return this.activator.getSearchPath();
    }

    public boolean isMangleNamespacePrefixes() {
        return this.activator.isMangleNamespacePrefixes();
    }

    @Override
    public String getMapRoot() {
        return this.activator.getMapRoot();
    }

    @Override
    public Mapping[] getMappings() {
        return this.activator.getMappings();
    }

    @Override
    public BidiMap getVirtualURLMap() {
        return this.activator.getVirtualURLMap();
    }

    @Override
    public int getDefaultVanityPathRedirectStatus() {
        return this.activator.getDefaultVanityPathRedirectStatus();
    }

    /**
     * get's the ServiceTracker of the ResourceAccessSecurity service
     */
    public ResourceAccessSecurityTracker getResourceAccessSecurityTracker () {
        return this.activator.getResourceAccessSecurityTracker();
    }

    @Override
    public ResourceResolver getServiceResourceResolver(
            final Map<String, Object> authenticationInfo) throws LoginException {
        throw new IllegalStateException("This method is not implemented.");
    }

    @Override
    public boolean isVanityPathEnabled() {
        return this.activator.isVanityPathEnabled();
    }

    @Override
    public long getMaxCachedVanityPathEntries() {
        return this.activator.getMaxCachedVanityPathEntries();
    }
    
    @Override
    public boolean isMaxCachedVanityPathEntriesStartup() {
        return this.activator.isMaxCachedVanityPathEntriesStartup();
    }

    @Override
    public int getVanityBloomFilterMaxBytes() {
        return this.activator.getVanityBloomFilterMaxBytes();
    }

    @Override
    public boolean isOptimizeAliasResolutionEnabled() {
        return this.activator.isOptimizeAliasResolutionEnabled();
    }

    @Override
    public boolean hasVanityPathPrecedence() {
        return this.activator.hasVanityPathPrecedence();
    }

    @Override
    public List<VanityPathConfig> getVanityPathConfig() {
        final String[] includes = this.activator.getVanityPathWhiteList();
        final String[] excludes = this.activator.getVanityPathBlackList();
        if ( includes == null && excludes == null ) {
            return null;
        }
        final List<VanityPathConfig> configs = new ArrayList<VanityPathConfig>();
        if ( includes != null ) {
            for(final String val : includes) {
                configs.add(new VanityPathConfig(val, false));
            }
        }
        if ( excludes != null ) {
            for(final String val : excludes) {
                configs.add(new VanityPathConfig(val, true));
            }
        }
        Collections.sort(configs);
        return configs;
    }

    /**
     * Is this factory still alive?
     */
    public boolean isLive() {
        return this.isActive.get();
    }

    public boolean shouldLogResourceResolverClosing() {
        return logResourceResolverClosing;
    }

    /**
     * Extension of a weak reference to be able to get the context object
     * that is used for cleaning up.
     */
    private static final class ResolverWeakReference extends WeakReference<ResourceResolver> {

        private final ResourceResolverContext context;

        public ResolverWeakReference(final ResourceResolver referent,
                final ReferenceQueue<? super ResourceResolver> q,
                final ResourceResolverContext ctx) {
            super(referent, q);
            this.context = ctx;
        }

        public void close() {
            this.context.close();
        }
    }

    public ResourceProviderTracker getResourceProviderTracker() {
        return activator.getResourceProviderTracker();
    }
}