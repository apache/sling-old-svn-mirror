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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;

import org.apache.commons.collections.BidiMap;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.path.Path;
import org.apache.sling.resourceresolver.impl.console.ResourceResolverWebConsolePlugin;
import org.apache.sling.resourceresolver.impl.helper.ResourceDecoratorTracker;
import org.apache.sling.resourceresolver.impl.helper.ResourceResolverControl;
import org.apache.sling.resourceresolver.impl.mapping.MapConfigurationProvider;
import org.apache.sling.resourceresolver.impl.mapping.MapEntries;
import org.apache.sling.resourceresolver.impl.mapping.MapEntriesHandler;
import org.apache.sling.resourceresolver.impl.mapping.Mapping;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderTracker;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>CommonResourceResolverFactoryImpl</code> is a singleton
 * implementing the shared/common functionality of all resource
 * resolver factories.
 */
public class CommonResourceResolverFactoryImpl implements ResourceResolverFactory, MapConfigurationProvider {

    private static final Logger LOG = LoggerFactory.getLogger(CommonResourceResolverFactoryImpl.class);

    /** Helper for the resource resolver. */
    private MapEntriesHandler mapEntries = MapEntriesHandler.EMPTY;

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

    /** Map of the ResourceResolverControl's hash code to the references to open resource resolver instances. */
    private final Map<Integer, ResolverReference> refs = new ConcurrentHashMap<Integer, ResolverReference>();

    /** Background thread handling disposing of resource resolver instances. */
    private final Thread refQueueThread;

    private boolean logUnclosedResolvers;

    private final Object optionalNamespaceMangler;


    /**
     * Create a new common resource resolver factory.
     */
    public CommonResourceResolverFactoryImpl(final ResourceResolverFactoryActivator activator) {
        this.activator = activator;
        this.logUnclosedResolvers = activator.isLogUnclosedResourceResolvers();
        this.refQueueThread = new Thread("Apache Sling Resource Resolver Finalizer Thread") {

            @Override
            public void run() {
                while (isLive()) {
                    try {
                        final ResolverReference ref = (ResolverReference) resolverReferenceQueue.remove();
                        ref.close();
                    } catch ( final InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        };
        this.refQueueThread.setDaemon(true);
        this.refQueueThread.start();

        // try create namespace mangler
        Object mangler = null;
        if ( this.isMangleNamespacePrefixes() ) {
            try {
                mangler = new JcrNamespaceMangler();
            } catch ( final Throwable t) {
                LOG.info("Unable to create JCR namespace mangler: {}", t.getMessage());
            }
        }
        this.optionalNamespaceMangler = mangler;
    }

    // ---------- Resource Resolver Factory ------------------------------------

    /**
     * @see org.apache.sling.api.resource.ResourceResolverFactory#getAdministrativeResourceResolver(java.util.Map)
     */
    @Nonnull
    @Override
    public ResourceResolver getAdministrativeResourceResolver(final Map<String, Object> passedAuthenticationInfo)
    throws LoginException {
        checkIsLive();

        // create a copy of the passed authentication info as we modify the map
        final Map<String, Object> authenticationInfo = new HashMap<String, Object>();
        authenticationInfo.put(ResourceProvider.AUTH_ADMIN, Boolean.TRUE);
        if ( passedAuthenticationInfo != null ) {
            authenticationInfo.putAll(passedAuthenticationInfo);
            // make sure there is no leaking of service info props
            // (but the bundle info is passed on as we need it downstream)
            authenticationInfo.remove(SUBSERVICE);
        }

        return getResourceResolverInternal(authenticationInfo, true);
    }

    /**
     * @see org.apache.sling.api.resource.ResourceResolverFactory#getResourceResolver(java.util.Map)
     */
    @Nonnull
    @Override
    public ResourceResolver getResourceResolver(final Map<String, Object> passedAuthenticationInfo)
    throws LoginException {
        checkIsLive();

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
        if (!isLive()) {
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
     * @param ctrl The resource resolver control
     */
    public void register(final ResourceResolver resolver,
            final ResourceResolverControl ctrl) {
        // create new weak reference
        refs.put(ctrl.hashCode(), new ResolverReference(resolver, this.resolverReferenceQueue, ctrl, this));
    }

    /**
     * Inform about a closed resource resolver.
     * Make sure to remove it from the current thread context.
     * @param resourceResolverImpl The resource resolver
     * @param ctrl The resource resolver control
     */
    public void unregister(final ResourceResolver resourceResolverImpl,
            final ResourceResolverControl ctrl) {
        unregisterControl(ctrl);

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
    ResourceResolver getResourceResolverInternal(final Map<String, Object> authenticationInfo,
                                                        final boolean isAdmin)
            throws LoginException {
        checkIsLive();

        return new ResourceResolverImpl(this, isAdmin, authenticationInfo);
    }

    /**
     * Close a resource resolver control and remove its corresponding
     * resolver reference from the map of weak references.
     *
     * @param ctrl The resource resolver control
     * @return true if the control was closed, false it had been closed before.
     */
    private boolean unregisterControl(final ResourceResolverControl ctrl) {
        // remove reference from the set of weak references and clear
        final ResolverReference reference = refs.remove(ctrl.hashCode());
        if (reference != null) {
            reference.clear();
        }
        final boolean doCloseControl = !ctrl.isClosed();
        if (doCloseControl) {
            ctrl.close();
        }
        return doCloseControl;
    }

    private void checkIsLive() throws LoginException {
        if ( !isLive() ) {
            throw new LoginException("ResourceResolverFactory is deactivated.");
        }
    }

    public MapEntriesHandler getMapEntries() {
        return mapEntries;
    }

    /** Activates this component */
    protected void activate(final BundleContext bundleContext) {
        final Logger logger = LoggerFactory.getLogger(getClass());
        try {
            plugin = new ResourceResolverWebConsolePlugin(bundleContext, this, this.activator.getRuntimeService());
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
        if (!isActive.compareAndSet(true, false)) {
            return;
        }
        this.refQueueThread.interrupt();

        if (plugin != null) {
            plugin.dispose();
            plugin = null;
        }

        if (mapEntries instanceof MapEntries ) {
            ((MapEntries)mapEntries).dispose();
            mapEntries = MapEntries.EMPTY;
        }
        resolverStackHolder = null;

        // copy and clear map before closing the remaining references
        final Collection<ResolverReference> references = new ArrayList<ResolverReference>(refs.values());
        refs.clear();
        for(final ResolverReference ref : references) {
            ref.close();
        }
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

    public Object getNamespaceMangler() {
        return this.optionalNamespaceMangler;
    }

    @Override
    public String getMapRoot() {
        return this.activator.getMapRoot();
    }

    @Override
    public boolean isMapConfiguration(String path) {
        return this.activator.isMapConfiguration(path);
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

    @Nonnull
    @Override
    public ResourceResolver getServiceResourceResolver(
            final Map<String, Object> authenticationInfo) throws LoginException {
        checkIsLive();

        return getResourceResolverInternal(authenticationInfo, false);
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
    public Path[] getObservationPaths() {
        return this.activator.getObservationPaths();
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
        return activator.shouldLogResourceResolverClosing();
    }

    public ResourceProviderTracker getResourceProviderTracker() {
        return activator.getResourceProviderTracker();
    }

    @Override
    public Map<String, Object> getServiceUserAuthenticationInfo(final String subServiceName)
    throws LoginException {
        // get an administrative resource resolver
        // Ensure a mapped user name: If no user is defined for a bundle
        // acting as a service, the user may be null. We can decide whether
        // this should yield guest access or no access at all. For now
        // no access is granted if there is no service user defined for
        // the bundle.
    	final Bundle bundle = this.activator.getBundleContext().getBundle();
        final String userName = this.activator.getServiceUserMapper().getServiceUserID(bundle, subServiceName);
        if (userName == null) {
            throw new LoginException("Cannot derive user name for bundle "
                + bundle + " and sub service " + subServiceName);
        }

        final Map<String, Object> authenticationInfo = new HashMap<>();
        // ensure proper user name and service bundle
        authenticationInfo.put(ResourceResolverFactory.SUBSERVICE, subServiceName);
        authenticationInfo.put(ResourceResolverFactory.USER, userName);
        authenticationInfo.put(ResourceProvider.AUTH_SERVICE_BUNDLE, bundle);

        return authenticationInfo;
    }

    /**
     * Extension of a weak reference to be able to get the control object
     * that is used for cleaning up.
     */
    private static final class ResolverReference extends WeakReference<ResourceResolver> {

        private final ResourceResolverControl control;

        private final Exception openingException;

        private final CommonResourceResolverFactoryImpl factory;

        ResolverReference(final ResourceResolver referent,
                          final ReferenceQueue<? super ResourceResolver> q,
                          final ResourceResolverControl ctrl,
                          final CommonResourceResolverFactoryImpl factory) {
            super(referent, q);
            this.control = ctrl;
            this.factory = factory;
            this.openingException = factory.logUnclosedResolvers && LOG.isInfoEnabled() ? new Exception("Opening Stacktrace") : null;
        }

        public void close() {
            try {
                if (factory.unregisterControl(this.control) && factory.logUnclosedResolvers) {
                    if (factory.isLive()) {
                        LOG.warn("Closed unclosed ResourceResolver. The creation stacktrace is available on info log level.");
                    } else {
                        LOG.warn("Forced close of ResourceResolver because the ResourceResolverFactory is shutting down.");
                    }
                    LOG.info("Unclosed ResourceResolver was created here: ", openingException);
                }
            } catch (Throwable t) {
                LOG.warn("Exception while closing ResolverReference", t);
            }
        }
    }
}