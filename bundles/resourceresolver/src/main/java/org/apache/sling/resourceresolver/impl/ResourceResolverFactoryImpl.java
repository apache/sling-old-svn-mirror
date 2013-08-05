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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.BidiMap;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceProviderFactory;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.security.ResourceAccessSecurity;
import org.apache.sling.resourceresolver.impl.console.ResourceResolverWebConsolePlugin;
import org.apache.sling.resourceresolver.impl.helper.ResourceDecoratorTracker;
import org.apache.sling.resourceresolver.impl.helper.ResourceResolverContext;
import org.apache.sling.resourceresolver.impl.mapping.MapConfigurationProvider;
import org.apache.sling.resourceresolver.impl.mapping.MapEntries;
import org.apache.sling.resourceresolver.impl.mapping.Mapping;
import org.apache.sling.resourceresolver.impl.tree.RootResourceProviderEntry;
import org.apache.sling.serviceusermapping.ServiceUserMapper;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>ResourceResolverFactoryImpl</code> is the {@link ResourceResolverFactory} service
 * providing the following
 * functionality:
 * <ul>
 * <li><code>ResourceResolverFactory</code> service
 * <li>Fires OSGi EventAdmin events on behalf of internal helper objects
 * </ul>
 *
 */
public class ResourceResolverFactoryImpl implements ResourceResolverFactory, MapConfigurationProvider {

    /** Helper for the resource resolver. */
    private MapEntries mapEntries = MapEntries.EMPTY;

    /** The web console plugin. */
    private ResourceResolverWebConsolePlugin plugin;

    /** The activator */
    private final ResourceResolverFactoryActivator activator;

    private ServiceTracker resourceAccessSecurityTracker;

    private final ServiceUserMapper serviceUserMapper;

    private final Bundle usingBundle;

    public ResourceResolverFactoryImpl(final ResourceResolverFactoryActivator activator, final Bundle usingBundle,
            final ServiceUserMapper serviceUserMapper) {
        this.activator = activator;
        this.serviceUserMapper = serviceUserMapper;
        this.usingBundle = usingBundle;
    }

    // ---------- Resource Resolver Factory ------------------------------------

    public ResourceResolver getServiceResourceResolver(Map<String, Object> authenticationInfo) throws LoginException {

        // clean authenticaiton from password and get service info
        final String subServiceName;
        if (authenticationInfo != null) {
            authenticationInfo.remove(PASSWORD);
            final Object info = authenticationInfo.get(SUBSERVICE);
            subServiceName = (info instanceof String) ? (String) info : null;
        } else {
            authenticationInfo = new HashMap<String, Object>();
            subServiceName = null;
        }

        // Ensure a mapped user name: If no user is defined for a bundle
        // acting as a service, the user may be null. We can decide whether
        // this should yield guest access or no access at all. For now
        // no access is granted if there is no service user defined for
        // the bundle.
        final String userName = this.serviceUserMapper.getServiceUserID(this.usingBundle, subServiceName);
        if (userName == null) {
            throw new LoginException("Cannot derive user name for service "
                + this.serviceUserMapper.getServiceID(this.usingBundle, subServiceName));
        }

        // ensure proper user name and service bundle
        authenticationInfo.put(ResourceResolverFactory.USER, userName);
        authenticationInfo.put(ResourceProviderFactory.SERVICE_BUNDLE, this.usingBundle);

        return getResourceResolverInternal(authenticationInfo, false);
    }

    public ResourceResolver getAdministrativeResourceResolver(final Map<String, Object> authenticationInfo) throws LoginException {

        // make sure there is no leaking of service bundle and info props
        if (authenticationInfo != null) {
            authenticationInfo.remove(ResourceProviderFactory.SERVICE_BUNDLE);
            authenticationInfo.remove(SUBSERVICE);
        }

        return getResourceResolverInternal(authenticationInfo, true);
    }

    public ResourceResolver getResourceResolver(final Map<String, Object> authenticationInfo) throws LoginException {

        // make sure there is no leaking of service bundle and info props
        if (authenticationInfo != null) {
            authenticationInfo.remove(ResourceProviderFactory.SERVICE_BUNDLE);
            authenticationInfo.remove(SUBSERVICE);
        }

        return getResourceResolverInternal(authenticationInfo, false);
    }

    // ---------- Implementation helpers --------------------------------------

    /**
     * Create a new ResourceResolver
     * @param authenticationInfo The authentication map
     * @param isAdmin is an administrative resolver requested?
     * @return A resource resolver
     * @throws LoginException if login to any of the required resource providers fails.
     */
    private ResourceResolver getResourceResolverInternal(final Map<String, Object> authenticationInfo,
                    final boolean isAdmin)
    throws LoginException {
        // create context
        final ResourceResolverContext ctx = new ResourceResolverContext(isAdmin, authenticationInfo, resourceAccessSecurityTracker);

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
            // an exception here propably means the web console plugin is not
            // available
            logger.debug("activate: unable to setup web console plugin.", ignore);
        }
        // set up the map entries from configuration
        try {
            mapEntries = new MapEntries(this, bundleContext, this.activator.getEventAdmin());
        } catch (final Exception e) {
            logger.error("activate: Cannot access repository, failed setting up Mapping Support", e);
        }

        // create and open service tracker for ResourceAccessSecurity
        resourceAccessSecurityTracker = new ServiceTracker(bundleContext, ResourceAccessSecurity.class.getName(), null);
        resourceAccessSecurityTracker.open();
    }

    /**
     * Deativates this component
     */
    protected void deactivate() {
        if (plugin != null) {
            plugin.dispose();
            plugin = null;
        }

        if (mapEntries != null) {
            mapEntries.dispose();
            mapEntries = MapEntries.EMPTY;
        }

        resourceAccessSecurityTracker.close();
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

    public ServiceTracker getResourceAccessSecurityTracker () {
        return resourceAccessSecurityTracker;
    }
}