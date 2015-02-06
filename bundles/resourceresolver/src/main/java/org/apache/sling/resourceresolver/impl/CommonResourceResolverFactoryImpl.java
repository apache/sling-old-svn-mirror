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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.collections.BidiMap;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceProviderFactory;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.resourceresolver.impl.console.ResourceResolverWebConsolePlugin;
import org.apache.sling.resourceresolver.impl.helper.ResourceDecoratorTracker;
import org.apache.sling.resourceresolver.impl.helper.ResourceResolverContext;
import org.apache.sling.resourceresolver.impl.mapping.MapConfigurationProvider;
import org.apache.sling.resourceresolver.impl.mapping.MapEntries;
import org.apache.sling.resourceresolver.impl.mapping.Mapping;
import org.apache.sling.resourceresolver.impl.tree.RootResourceProviderEntry;
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
    private ThreadLocal<Stack<ResourceResolver>> resolverStackHolder = new ThreadLocal<Stack<ResourceResolver>>();

    private final AtomicBoolean isActive = new AtomicBoolean(true);

    public CommonResourceResolverFactoryImpl(final ResourceResolverFactoryActivator activator) {
        this.activator = activator;
    }

    // ---------- Resource Resolver Factory ------------------------------------

    /**
     * @see org.apache.sling.api.resource.ResourceResolverFactory#getAdministrativeResourceResolver(java.util.Map)
     */
    public ResourceResolver getAdministrativeResourceResolver(final Map<String, Object> passedAuthenticationInfo)
    throws LoginException {
        if ( !isActive.get() ) {
            throw new LoginException("ResourceResolverFactory is deactivated.");
        }

        // create a copy of the passed authentication info as we modify the map
        final Map<String, Object> authenticationInfo = new HashMap<String, Object>();
        if ( passedAuthenticationInfo != null ) {
            authenticationInfo.putAll(passedAuthenticationInfo);
            // make sure there is no leaking of service bundle and info props
            authenticationInfo.remove(ResourceProviderFactory.SERVICE_BUNDLE);
            authenticationInfo.remove(SUBSERVICE);
        }

        return getResourceResolverInternal(authenticationInfo, true);
    }

    /**
     * @see org.apache.sling.api.resource.ResourceResolverFactory#getResourceResolver(java.util.Map)
     */
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
            authenticationInfo.remove(ResourceProviderFactory.SERVICE_BUNDLE);
            authenticationInfo.remove(SUBSERVICE);
        }

        final ResourceResolver result = getResourceResolverInternal(authenticationInfo, false);
        Stack<ResourceResolver> resolverStack = resolverStackHolder.get();
        if ( resolverStack == null ) {
            resolverStack = new Stack<ResourceResolver>();
            resolverStackHolder.set(resolverStack);
        }
        resolverStack.push(result);
        return result;
    }


    /**
     * @see org.apache.sling.api.resource.ResourceResolverFactory#getThreadResourceResolver()
     */
    public ResourceResolver getThreadResourceResolver() {
        if ( !isActive.get() ) {
            return null;
        }

        ResourceResolver result = null;
        final Stack<ResourceResolver> resolverStack = resolverStackHolder.get();
        if ( resolverStack != null && !resolverStack.isEmpty() ) {
            result = resolverStack.peek();
        }
        return result;
    }

    // ---------- Implementation helpers --------------------------------------

    /**
     * Inform about a closed resource resolver.
     * Make sure to remove it from the current thread context.
     */
    public void closed(final ResourceResolverImpl resourceResolverImpl) {
        // on shutdown, the factory might already be closed before the resolvers close
        // therefore we have to check for null
        final ThreadLocal<Stack<ResourceResolver>> tl = resolverStackHolder;
        if ( tl != null ) {
            final Stack<ResourceResolver> resolverStack = tl.get();
            if ( resolverStack != null ) {
                resolverStack.remove(resourceResolverImpl);
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

        // create context
        final ResourceResolverContext ctx = new ResourceResolverContext(isAdmin, authenticationInfo, this.activator.getResourceAccessSecurityTracker());

        // login
        this.activator.getRootProviderEntry().loginToRequiredFactories(ctx);

        return new ResourceResolverImpl(this, ctx);
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

    public String getMapRoot() {
        return this.activator.getMapRoot();
    }

    public Mapping[] getMappings() {
        return this.activator.getMappings();
    }

    public BidiMap getVirtualURLMap() {
        return this.activator.getVirtualURLMap();
    }

    public RootResourceProviderEntry getRootProviderEntry() {
        return this.activator.getRootProviderEntry();
    }

    public int getDefaultVanityPathRedirectStatus() {
        return this.activator.getDefaultVanityPathRedirectStatus();
    }

    /**
     * get's the ServiceTracker of the ResourceAccessSecurity service
     */
    public ResourceAccessSecurityTracker getResourceAccessSecurityTracker () {
        return this.activator.getResourceAccessSecurityTracker();
    }

    public ResourceResolver getServiceResourceResolver(
            final Map<String, Object> authenticationInfo) throws LoginException {
        throw new IllegalStateException("This method is not implemented.");
    }

    public boolean isVanityPathEnabled() {
        return this.activator.isVanityPathEnabled();
    }

    public long getMaxCachedVanityPathEntries() {
        return this.activator.getMaxCachedVanityPathEntries();
    }

    public int getVanityBloomFilterMaxBytes() {
        return this.activator.getVanityBloomFilterMaxBytes();
    }

    public boolean isOptimizeAliasResolutionEnabled() {
        return this.activator.isOptimizeAliasResolutionEnabled();
    }

    public boolean hasVanityPathPrecedence() {
        return this.activator.hasVanityPathPrecedence();
    }

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

    public boolean isLive() {
        return this.isActive.get();
    }
}