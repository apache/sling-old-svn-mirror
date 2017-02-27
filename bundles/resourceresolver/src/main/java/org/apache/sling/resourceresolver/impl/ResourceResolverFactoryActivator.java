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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.TreeBidiMap;
import org.apache.sling.api.resource.ResourceDecorator;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.path.Path;
import org.apache.sling.api.resource.runtime.RuntimeService;
import org.apache.sling.resourceresolver.impl.helper.ResourceDecoratorTracker;
import org.apache.sling.resourceresolver.impl.mapping.Mapping;
import org.apache.sling.resourceresolver.impl.observation.ResourceChangeListenerWhiteboard;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderTracker;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderTracker.ChangeListener;
import org.apache.sling.resourceresolver.impl.providers.RuntimeServiceImpl;
import org.apache.sling.serviceusermapping.ServiceUserMapper;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>ResourceResolverFactoryActivator/code> keeps track of required services for the
 * resource resolver factory.
 * One all required providers and provider factories are available a resource resolver factory
 * is registered.
 *
 */
@Designate(ocd = ResourceResolverFactoryConfig.class)
@Component(name = "org.apache.sling.jcr.resource.internal.JcrResourceResolverFactoryImpl")
public class ResourceResolverFactoryActivator {


    private static final class FactoryRegistration {
        /** Registration .*/
        public volatile ServiceRegistration<ResourceResolverFactory> factoryRegistration;

        /** Runtime registration. */
        public volatile ServiceRegistration<RuntimeService> runtimeRegistration;

        public volatile CommonResourceResolverFactoryImpl commonFactory;
    }


    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** Tracker for the resource decorators. */
    private final ResourceDecoratorTracker resourceDecoratorTracker = new ResourceDecoratorTracker();

    /** all mappings */
    private volatile Mapping[] mappings;

    /** The fake URLs */
    private volatile BidiMap virtualURLMap;

    /** the search path for ResourceResolver.getResource(String) */
    private volatile String[] searchPath;

    /** the root location of the /etc/map entries */
    private volatile String mapRoot;

    private volatile String mapRootPrefix;

    /** Event admin. */
    @Reference
    EventAdmin eventAdmin;

    /** Service User Mapper */
    @Reference
    ServiceUserMapper serviceUserMapper;

    @Reference
    ResourceAccessSecurityTracker resourceAccessSecurityTracker;

    volatile ResourceProviderTracker resourceProviderTracker;

    volatile ResourceChangeListenerWhiteboard changeListenerWhiteboard;

    /** Bundle Context */
    private volatile BundleContext bundleContext;

    private volatile ResourceResolverFactoryConfig config = DEFAULT_CONFIG;

    /** Vanity path whitelist */
    private volatile String[] vanityPathWhiteList;

    /** Vanity path blacklist */
    private volatile String[] vanityPathBlackList;

    /** Observation paths */
    private volatile Path[] observationPaths;

    private final FactoryPreconditions preconds = new FactoryPreconditions();

    /** Factory registration. */
    private volatile FactoryRegistration factoryRegistration;

    /**
     * Get the resource decorator tracker.
     */
    public ResourceDecoratorTracker getResourceDecoratorTracker() {
        return this.resourceDecoratorTracker;
    }

    public ResourceAccessSecurityTracker getResourceAccessSecurityTracker() {
        return this.resourceAccessSecurityTracker;
    }

    public EventAdmin getEventAdmin() {
        return this.eventAdmin;
    }

    /**
     * This method is called from {@link MapEntries}
     */
    public BidiMap getVirtualURLMap() {
        return virtualURLMap;
    }

    /**
     * This method is called from {@link MapEntries}
     */
    public Mapping[] getMappings() {
        return mappings;
    }

    public String[] getSearchPath() {
        return searchPath;
    }

    public boolean isMangleNamespacePrefixes() {
        return config.resource_resolver_manglenamespaces();

    }

    public String getMapRoot() {
        return mapRoot;
    }

    public boolean isMapConfiguration(String path) {
        return path.equals(this.mapRoot)
               || path.startsWith(this.mapRootPrefix);
    }

    public int getDefaultVanityPathRedirectStatus() {
        return config.resource_resolver_default_vanity_redirect_status();
    }

    public boolean isVanityPathEnabled() {
        return this.config.resource_resolver_enable_vanitypath();
    }

    public boolean isOptimizeAliasResolutionEnabled() {
        return this.config.resource_resolver_optimize_alias_resolution();
    }

    public boolean isLogUnclosedResourceResolvers() {
        return this.config.resource_resolver_log_unclosed();
    }

    public String[] getVanityPathWhiteList() {
        return this.vanityPathWhiteList;
    }

    public String[] getVanityPathBlackList() {
        return this.vanityPathBlackList;
    }

    public boolean hasVanityPathPrecedence() {
        return this.config.resource_resolver_vanity_precedence();
    }

    public long getMaxCachedVanityPathEntries() {
        return this.config.resource_resolver_vanitypath_maxEntries();
    }

    public boolean isMaxCachedVanityPathEntriesStartup() {
        return this.config.resource_resolver_vanitypath_maxEntries_startup();
    }

    public int getVanityBloomFilterMaxBytes() {
        return this.config.resource_resolver_vanitypath_bloomfilter_maxBytes();
    }

    public boolean shouldLogResourceResolverClosing() {
        return this.config.resource_resolver_log_closing();
    }

    public Path[] getObservationPaths() {
        return this.observationPaths;
    }

    // ---------- SCR Integration ---------------------------------------------

    /**
     * Activates this component (called by SCR before)
     */
    @Activate
    protected void activate(final BundleContext bundleContext, final ResourceResolverFactoryConfig config) {
        this.bundleContext = bundleContext;
        this.config = config;

        final BidiMap virtuals = new TreeBidiMap();
        for (int i = 0; config.resource_resolver_virtual() != null && i < config.resource_resolver_virtual().length; i++) {
            final String[] parts = Mapping.split(config.resource_resolver_virtual()[i]);
            virtuals.put(parts[0], parts[2]);
        }
        virtualURLMap = virtuals;

        final List<Mapping> maps = new ArrayList<Mapping>();
        for (int i = 0; config.resource_resolver_mapping() != null && i < config.resource_resolver_mapping().length; i++) {
            maps.add(new Mapping(config.resource_resolver_mapping()[i]));
        }
        final Mapping[] tmp = maps.toArray(new Mapping[maps.size()]);

        // check whether direct mappings are allowed
        if (config.resource_resolver_allowDirect()) {
            final Mapping[] tmp2 = new Mapping[tmp.length + 1];
            tmp2[0] = Mapping.DIRECT;
            System.arraycopy(tmp, 0, tmp2, 1, tmp.length);
            mappings = tmp2;
        } else {
            mappings = tmp;
        }

        // from configuration if available
        searchPath = config.resource_resolver_searchpath();
        if (searchPath != null && searchPath.length > 0) {
            for (int i = 0; i < searchPath.length; i++) {
                // ensure leading slash
                if (!searchPath[i].startsWith("/")) {
                    searchPath[i] = "/" + searchPath[i];
                }
                // ensure trailing slash
                if (!searchPath[i].endsWith("/")) {
                    searchPath[i] += "/";
                }
            }
        }
        if (searchPath == null) {
            searchPath = new String[] { "/" };
        }
        // the root of the resolver mappings
        mapRoot = config.resource_resolver_map_location();
        mapRootPrefix = mapRoot + '/';

        final String[] paths = config.resource_resolver_map_observation();
        this.observationPaths = new Path[paths.length];
        for(int i=0;i<paths.length;i++) {
            this.observationPaths[i] = new Path(paths[i]);
        }

        // vanity path white list
        this.vanityPathWhiteList = null;
        String[] vanityPathPrefixes = config.resource_resolver_vanitypath_whitelist();
        if ( vanityPathPrefixes != null ) {
            final List<String> prefixList = new ArrayList<String>();
            for(final String value : vanityPathPrefixes) {
                if ( value.trim().length() > 0 ) {
                    if ( value.trim().endsWith("/") ) {
                        prefixList.add(value.trim());
                    } else {
                        prefixList.add(value.trim() + "/");
                    }
                }
            }
            if ( prefixList.size() > 0 ) {
                this.vanityPathWhiteList = prefixList.toArray(new String[prefixList.size()]);
            }
        }
        // vanity path black list
        this.vanityPathBlackList = null;
        vanityPathPrefixes = config.resource_resolver_vanitypath_blacklist();
        if ( vanityPathPrefixes != null ) {
            final List<String> prefixList = new ArrayList<String>();
            for(final String value : vanityPathPrefixes) {
                if ( value.trim().length() > 0 ) {
                    if ( value.trim().endsWith("/") ) {
                        prefixList.add(value.trim());
                    } else {
                        prefixList.add(value.trim() + "/");
                    }
                }
            }
            if ( prefixList.size() > 0 ) {
                this.vanityPathBlackList = prefixList.toArray(new String[prefixList.size()]);
            }
        }

        // check for required property
        Set<String> requiredResourceProvidersLegacy = getStringSet(config.resource_resolver_required_providers());
        Set<String> requiredResourceProviderNames = getStringSet(config.resource_resolver_required_providernames());

        boolean hasLegacyRequiredProvider = false;
        if ( requiredResourceProvidersLegacy != null ) {
            for(final String name : requiredResourceProvidersLegacy) {
            	if ( name.equals(ResourceResolverFactoryConfig.LEGACY_REQUIRED_PROVIDER_PID)) {
                	hasLegacyRequiredProvider = true;
                	break;
            	}
            }
            if ( hasLegacyRequiredProvider ) {
            	requiredResourceProvidersLegacy.remove(ResourceResolverFactoryConfig.LEGACY_REQUIRED_PROVIDER_PID);
            }
            if ( !requiredResourceProvidersLegacy.isEmpty() ) {
                logger.error("ResourceResolverFactory is using deprecated required providers configuration (resource.resolver.required.providers" +
                        "). Please change to use the property resource.resolver.required.providernames for values: " + requiredResourceProvidersLegacy);
            } else {
            	requiredResourceProvidersLegacy = null;
            }
        }
        if ( hasLegacyRequiredProvider ) {
            final boolean hasRequiredProvider;
        	if ( requiredResourceProviderNames != null ) {
        		hasRequiredProvider = !requiredResourceProviderNames.add(ResourceResolverFactoryConfig.REQUIRED_PROVIDER_NAME);
        	} else {
        		hasRequiredProvider = false;
        		requiredResourceProviderNames = Collections.singleton(ResourceResolverFactoryConfig.REQUIRED_PROVIDER_NAME);
        	}
        	if ( hasRequiredProvider ) {
                logger.warn("ResourceResolverFactory is using deprecated required providers configuration (resource.resolver.required.providers" +
                        ") with value '" + ResourceResolverFactoryConfig.LEGACY_REQUIRED_PROVIDER_PID + ". Please remove this configuration property. " +
                        ResourceResolverFactoryConfig.REQUIRED_PROVIDER_NAME + " is already contained in the property resource.resolver.required.providernames.");        		        		
        	} else {
                logger.warn("ResourceResolverFactory is using deprecated required providers configuration (resource.resolver.required.providers" +
                        ") with value '" + ResourceResolverFactoryConfig.LEGACY_REQUIRED_PROVIDER_PID + ". Please remove this configuration property and add " +
                        ResourceResolverFactoryConfig.REQUIRED_PROVIDER_NAME + " to the property resource.resolver.required.providernames.");        		
        	}
        }

        // for testing: if we run unit test, both trackers are set from the outside
        if ( this.resourceProviderTracker == null ) {
            this.resourceProviderTracker = new ResourceProviderTracker();
            this.changeListenerWhiteboard = new ResourceChangeListenerWhiteboard();
            this.preconds.activate(this.bundleContext, 
            		requiredResourceProvidersLegacy, 
            		requiredResourceProviderNames, 
            		resourceProviderTracker);
            this.changeListenerWhiteboard.activate(this.bundleContext,
                this.resourceProviderTracker, searchPath);
            this.resourceProviderTracker.activate(this.bundleContext,
                    this.eventAdmin,
                    new ChangeListener() {

                        @Override
                        public void providerAdded() {
                            if ( factoryRegistration == null ) {
                                checkFactoryPreconditions(null, null);
                            }

                        }

                        @Override
                        public void providerRemoved(final String name, final String pid, final boolean stateful, final boolean isUsed) {
                            if ( factoryRegistration != null ) {
                                if ( isUsed && (stateful || config.resource_resolver_providerhandling_paranoid()) ) {
                                    unregisterFactory();
                                }
                                checkFactoryPreconditions(name, pid);
                            }
                        }
                    });
        } else {
            this.preconds.activate(this.bundleContext, 
            		requiredResourceProvidersLegacy, 
            		requiredResourceProviderNames, 
            		resourceProviderTracker);
            this.checkFactoryPreconditions(null, null);
         }
    }

    /**
     * Modifies this component (called by SCR to update this component)
     */
    @Modified
    protected void modified(final BundleContext bundleContext, final ResourceResolverFactoryConfig config) {
        this.deactivate();
        this.activate(bundleContext, config);
    }

    /**
     * Deactivates this component (called by SCR to take out of service)
     */
    @Deactivate
    protected void deactivate() {
        this.unregisterFactory();

        this.bundleContext = null;
        this.config = DEFAULT_CONFIG;
        this.changeListenerWhiteboard.deactivate();
        this.changeListenerWhiteboard = null;
        this.resourceProviderTracker.deactivate();
        this.resourceProviderTracker = null;

        this.preconds.deactivate();
        this.resourceDecoratorTracker.close();

        // this is just a sanity call to make sure that unregister
        // in the case that a registration happened again
        // while deactivation
        this.unregisterFactory();
    }

    /**
     * Unregister the factory (if registered)
     * This method might be called concurrently from deactivate and the
     * background thread, therefore we need to synchronize.
     */
    private void unregisterFactory() {
        FactoryRegistration local = null;
        synchronized ( this ) {
            if ( this.factoryRegistration != null ) {
                local = this.factoryRegistration;
                this.factoryRegistration = null;
            }
        }
        this.unregisterFactory(local);
    }

    /**
     * Unregister the provided factory
     */
    private void unregisterFactory(final FactoryRegistration local) {
        if ( local != null ) {
            if ( local.factoryRegistration != null ) {
                local.factoryRegistration.unregister();
            }
            if ( local.runtimeRegistration != null ) {
                local.runtimeRegistration.unregister();
            }
            if ( local.commonFactory != null ) {
                local.commonFactory.deactivate();
            }
        }
    }

    /**
     * Try to register the factory.
     */
    private void registerFactory(final BundleContext localContext) {
        final FactoryRegistration local = new FactoryRegistration();

        if ( localContext != null ) {
            // activate and register factory
            final Dictionary<String, Object> serviceProps = new Hashtable<String, Object>();
            serviceProps.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
            serviceProps.put(Constants.SERVICE_DESCRIPTION, "Apache Sling Resource Resolver Factory");

            local.commonFactory = new CommonResourceResolverFactoryImpl(this);
            local.commonFactory.activate(localContext);
            local.factoryRegistration = localContext.registerService(
                ResourceResolverFactory.class, new ServiceFactory<ResourceResolverFactory>() {

                    @Override
                    public ResourceResolverFactory getService(final Bundle bundle, final ServiceRegistration<ResourceResolverFactory> registration) {
                        if ( ResourceResolverFactoryActivator.this.bundleContext == null ) {
                            return null;
                        }
                        final ResourceResolverFactoryImpl r = new ResourceResolverFactoryImpl(
                                local.commonFactory, bundle,
                            ResourceResolverFactoryActivator.this.serviceUserMapper);
                        return r;
                    }

                    @Override
                    public void ungetService(final Bundle bundle, final ServiceRegistration<ResourceResolverFactory> registration, final ResourceResolverFactory service) {
                        // nothing to do
                    }
                }, serviceProps);

            local.runtimeRegistration = localContext.registerService(RuntimeService.class,
                    this.getRuntimeService(), null);

            this.factoryRegistration = local;
        }
    }

    /**
     * Get the runtime service
     * @return The runtime service
     */
    public RuntimeService getRuntimeService() {
        return new RuntimeServiceImpl(this.resourceProviderTracker);
    }

    public ServiceUserMapper getServiceUserMapper() {
    	return this.serviceUserMapper;
    }
    
    public BundleContext getBundleContext() {
    	return this.bundleContext;
    }
    
    /**
     * Check the preconditions and if it changed, either register factory or unregister
     */
    private void checkFactoryPreconditions(final String unavailableName, final String unavailableServicePid) {
        final BundleContext localContext = this.bundleContext;
        if ( localContext != null ) {
            final boolean result = this.preconds.checkPreconditions(unavailableName, unavailableServicePid);
            if ( result && this.factoryRegistration == null ) {
                // check system bundle state - if stopping, don't register new factory
                final Bundle systemBundle = localContext.getBundle(Constants.SYSTEM_BUNDLE_LOCATION);
                if ( systemBundle != null && systemBundle.getState() != Bundle.STOPPING ) {
                    boolean create = true;
                    synchronized ( this ) {
                        if ( this.factoryRegistration == null ) {
                            this.factoryRegistration = new FactoryRegistration();
                        } else {
                            create = false;
                        }
                    }
                    if ( create ) {
                        this.registerFactory(localContext);
                    }
                }
            } else if ( !result && this.factoryRegistration != null ) {
                this.unregisterFactory();
            }
        }
    }

    /**
     * Bind a resource decorator.
     */
    @Reference(service = ResourceDecorator.class, cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void bindResourceDecorator(final ResourceDecorator decorator, final Map<String, Object> props) {
        this.resourceDecoratorTracker.bindResourceDecorator(decorator, props);
    }

    /**
     * Unbind a resource decorator.
     */
    protected void unbindResourceDecorator(final ResourceDecorator decorator, final Map<String, Object> props) {
        this.resourceDecoratorTracker.unbindResourceDecorator(decorator, props);
    }

    /**
     * Get the resource provider tracker
     * @return The tracker
     */
    public ResourceProviderTracker getResourceProviderTracker() {
        return resourceProviderTracker;
    }

    /**
     * Utility method to create a set out of a string array
     */
    private Set<String> getStringSet(final String[] values) {
    	if ( values == null || values.length == 0 ) {
    		return null;
    	}
    	final Set<String> set = new HashSet<>();
    	for(final String val : values) {
    		if ( val != null && !val.trim().isEmpty() ) {
    			set.add(val.trim());
    		}
    	}
    	return set.isEmpty() ? null : set;
    }
    
    public static ResourceResolverFactoryConfig DEFAULT_CONFIG;

    static {
       final InvocationHandler handler = new InvocationHandler() {

            @Override
            public Object invoke(final Object obj, final Method calledMethod, final Object[] args)
            throws Throwable {
                if ( calledMethod.getDeclaringClass().isAssignableFrom(ResourceResolverFactoryConfig.class) ) {
                   return calledMethod.getDefaultValue();
                }
                if ( calledMethod.getDeclaringClass() == Object.class ) {
                    if ( calledMethod.getName().equals("toString") && (args == null || args.length == 0) ) {
                        return "Generated @" + ResourceResolverFactoryConfig.class.getName() + " instance";
                    }
                    if ( calledMethod.getName().equals("hashCode") && (args == null || args.length == 0) ) {
                        return this.hashCode();
                    }
                    if ( calledMethod.getName().equals("equals") && args != null && args.length == 1 ) {
                        return Boolean.FALSE;
                    }
                }
                throw new InternalError("unexpected method dispatched: " + calledMethod);
            }
        };
        DEFAULT_CONFIG = (ResourceResolverFactoryConfig) Proxy.newProxyInstance(ResourceResolverFactoryConfig.class.getClassLoader(),
                new Class[] { ResourceResolverFactoryConfig.class },
                handler);
    }
}
