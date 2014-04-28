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
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.TreeBidiMap;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.References;
import org.apache.sling.api.resource.ResourceDecorator;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceProviderFactory;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.resourceresolver.impl.helper.ResourceDecoratorTracker;
import org.apache.sling.resourceresolver.impl.mapping.MapEntries;
import org.apache.sling.resourceresolver.impl.mapping.Mapping;
import org.apache.sling.resourceresolver.impl.tree.RootResourceProviderEntry;
import org.apache.sling.serviceusermapping.ServiceUserMapper;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.EventAdmin;

/**
 * The <code>ResourceResolverFactoryActivator/code> keeps track of required services for the
 * resource resolver factory.
 * One all required providers and provider factories are available a resource resolver factory
 * is registered.
 *
 * TODO : Should we implement modifiable? It would be easy but what about long running resolvers?
 */
@Component(
     name = "org.apache.sling.jcr.resource.internal.JcrResourceResolverFactoryImpl",
     label = "Apache Sling Resource Resolver Factory",
     description = "Configures the Resource Resolver for request URL and resource path rewriting.",
     specVersion = "1.1",
     metatype = true)
@Properties({
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "Apache Sling Resource Resolver Factory"),
    @Property(name = Constants.SERVICE_VENDOR, value = "The Apache Software Foundation")
})
@References({
    @Reference(name = "ResourceProvider", referenceInterface = ResourceProvider.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC),
    @Reference(name = "ResourceProviderFactory", referenceInterface = ResourceProviderFactory.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC),
    @Reference(name = "ResourceDecorator", referenceInterface = ResourceDecorator.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
})
public class ResourceResolverFactoryActivator {

    private static final class FactoryRegistration {
        /** Registration .*/
        public volatile ServiceRegistration factoryRegistration;

        public volatile CommonResourceResolverFactoryImpl commonFactory;
    }

    @Property(value = { "/apps", "/libs" },
              label = "Resource Search Path",
              description = "The list of absolute path prefixes " +
                            "applied to find resources whose path is just specified with a relative path. " +
                            "The default value is [ \"/apps\", \"/libs\" ]. If an empty path is specified a " +
                            "single entry path of [ \"/\" ] is assumed.")
    public static final String PROP_PATH = "resource.resolver.searchpath";

    /**
     * Defines whether namespace prefixes of resource names inside the path
     * (e.g. <code>jcr:</code> in <code>/home/path/jcr:content</code>) are
     * mangled or not.
     * <p>
     * Mangling means that any namespace prefix contained in the path is replaced as per the generic
     * substitution pattern <code>/([^:]+):/_$1_/</code> when calling the <code>map</code> method of
     * the resource resolver. Likewise the <code>resolve</code> methods will unmangle such namespace
     * prefixes according to the substitution pattern <code>/_([^_]+)_/$1:/</code>.
     * <p>
     * This feature is provided since there may be systems out there in the wild which cannot cope
     * with URLs containing colons, even though they are perfectly valid characters in the path part
     * of URI references with a scheme.
     * <p>
     * The default value of this property if no configuration is provided is <code>true</code>.
     *
     */
    @Property(boolValue = true,
              label = "Namespace Mangling",
              description = "Defines whether namespace " +
                            "prefixes of resource names inside the path (e.g. \"jcr:\" in \"/home/path/jcr:content\") " +
                            "are mangled or not. Mangling means that any namespace prefix contained in the " +
                            "path is replaced as per the generic substitution pattern \"/([^:]+):/_$1_/\" " +
                            "when calling the \"map\" method of the resource resolver. Likewise the " +
                            "\"resolve\" methods will unmangle such namespace prefixes according to the " +
                            "substituation pattern \"/_([^_]+)_/$1:/\". This feature is provided since " +
                            "there may be systems out there in the wild which cannot cope with URLs " +
                            "containing colons, even though they are perfectly valid characters in the " +
                            "path part of URI references with a scheme. The default value of this property " +
                            "if no configuration is provided is \"true\".")
    private static final String PROP_MANGLE_NAMESPACES = "resource.resolver.manglenamespaces";

    @Property(boolValue = true,
              label = "Allow Direct Mapping",
              description = "Whether to add a direct URL mapping to the front of the mapping list.")
    private static final String PROP_ALLOW_DIRECT = "resource.resolver.allowDirect";

    @Property(unbounded=PropertyUnbounded.ARRAY,
              value = "org.apache.sling.jcr.resource.internal.helper.jcr.JcrResourceProviderFactory",
              label = "Required Providers",
              description = "A resource resolver factory is only " +
                             "available (registered) if all resource providers mentioned in this configuration " +
                             "are available. Each entry is either a service PID or a filter expression.  " +
                             "Invalid filters are ignored.")
    private static final String PROP_REQUIRED_PROVIDERS = "resource.resolver.required.providers";

    /**
     * The resolver.virtual property has no default configuration. But the Sling
     * maven plugin and the sling management console cannot handle empty
     * multivalue properties at the moment. So we just add a dummy direct
     * mapping.
     */
    @Property(value = "/:/", unbounded = PropertyUnbounded.ARRAY,
              label = "Virtual URLs",
              description = "List of virtual URLs and there mappings to real URLs. " +
                            "Format is <externalURL>:<internalURL>. Mappings are " +
                            "applied on the complete request URL only.")
    private static final String PROP_VIRTUAL = "resource.resolver.virtual";

    @Property(value = { "/:/", "/content/:/", "/system/docroot/:/" },
              label = "URL Mappings",
              description = "List of mappings to apply to paths. Incoming mappings are " +
                            "applied to request paths to map to resource paths, " +
                            "outgoing mappings are applied to map resource paths to paths used on subsequent " +
                            "requests. Form is <internalPathPrefix><op><externalPathPrefix> where <op> is " +
                            "\">\" for incoming mappings, \"<\" for outgoing mappings and \":\" for mappings " +
                            "applied in both directions. Mappings are applied in configuration order by " +
                            "comparing and replacing URL prefixes. Note: The use of \"-\" as the <op> value " +
                            "indicating a mapping in both directions is deprecated.")
    private static final String PROP_MAPPING = "resource.resolver.mapping";

    @Property(value = MapEntries.DEFAULT_MAP_ROOT,
              label = "Mapping Location",
              description = "The path to the root of the configuration to setup and configure " +
                            "the ResourceResolver mapping. The default value is /etc/map.")
    private static final String PROP_MAP_LOCATION = "resource.resolver.map.location";

    @Property(intValue = MapEntries.DEFAULT_DEFAULT_VANITY_PATH_REDIRECT_STATUS,
              label = "Default Vanity Path Redirect Status",
              description = "The default status code used when a sling:vanityPath is configured to redirect " +
                            "and does not have a specific status code associated with it " +
                            "(via a sling:redirectStatus property)")
    private static final String PROP_DEFAULT_VANITY_PATH_REDIRECT_STATUS = "resource.resolver.default.vanity.redirect.status";

    private static final boolean DEFAULT_ENABLE_VANITY_PATH = true;
    @Property(boolValue = DEFAULT_ENABLE_VANITY_PATH,
              label = "Enable Vanity Paths",
              description = "This flag controls whether all resources with a sling:vanityPath property " +
                            "are processed and added to the mappoing table.")
    private static final String PROP_ENABLE_VANITY_PATH = "resource.resolver.enable.vanitypath";

    private static final boolean DEFAULT_ENABLE_OPTIMIZE_ALIAS_RESOLUTION = true;
    @Property(boolValue = DEFAULT_ENABLE_OPTIMIZE_ALIAS_RESOLUTION ,
              label = "Optimize alias resolution",
              description ="This flag controls whether to optimize" +
                      " the alias resolution by creating an internal cache of aliases. This might have an impact on the startup time"+
                      " and on the alias update time if the number of aliases is huge (over 10000).")
    private static final String PROP_ENABLE_OPTIMIZE_ALIAS_RESOLUTION = "resource.resolver.optimize.alias.resolution";

    @Property(unbounded=PropertyUnbounded.ARRAY,
            label = "Allowed Vanity Path Location",
            description ="This setting can contain a list of path prefixes, e.g. /libs/, /content/. If " +
                    "such a list is configured, only vanity paths from resources starting with this prefix " +
                    " are considered. If the list is empty, all vanity paths are used.")
    private static final String PROP_ALLOWED_VANITY_PATH_PREFIX = "resource.resolver.vanitypath.whitelist";

    @Property(unbounded=PropertyUnbounded.ARRAY,
            label = "Denied Vanity Path Location",
            description ="This setting can contain a list of path prefixes, e.g. /misc/. If " +
                    "such a list is configured,vanity paths from resources starting with this prefix " +
                    " are not considered. If the list is empty, all vanity paths are used.")
    private static final String PROP_DENIED_VANITY_PATH_PREFIX = "resource.resolver.vanitypath.blacklist";
    
    private static final boolean DEFAULT_VANITY_PATH_PRECEDENCE = false;
    @Property(boolValue = DEFAULT_VANITY_PATH_PRECEDENCE ,
              label = "Vanity Path Precedence",
              description ="This flag controls whether vanity paths" +
                      " will have precedence over existing /etc/map mapping")
    private static final String PROP_VANITY_PATH_PRECEDENCE = "resource.resolver.vanity.precedence";
 
    /** Tracker for the resource decorators. */
    private final ResourceDecoratorTracker resourceDecoratorTracker = new ResourceDecoratorTracker();

    /** all mappings */
    private Mapping[] mappings;

    /** The fake URLs */
    private BidiMap virtualURLMap;

    /** <code>true</code>, if direct mappings from URI to handle are allowed */
    private boolean allowDirect = false;

    /** the search path for ResourceResolver.getResource(String) */
    private String[] searchPath;

    /** the root location of the /etc/map entries */
    private String mapRoot;

    /** whether to mangle paths with namespaces or not */
    private boolean mangleNamespacePrefixes;

    /** The root provider entry. */
    private final RootResourceProviderEntry rootProviderEntry = new RootResourceProviderEntry();

    /** Event admin. */
    @Reference
    EventAdmin eventAdmin;

    /** Service User Mapper */
    @Reference
    private ServiceUserMapper serviceUserMapper;

    @Reference
    ResourceAccessSecurityTracker resourceAccessSecurityTracker;

    /** ComponentContext */
    private volatile ComponentContext componentContext;

    private int defaultVanityPathRedirectStatus;

    /** vanityPath enabled? */
    private boolean enableVanityPath = DEFAULT_ENABLE_VANITY_PATH;

    /** alias resource resolution optimization enabled? */
    private boolean enableOptimizeAliasResolution = DEFAULT_ENABLE_OPTIMIZE_ALIAS_RESOLUTION;
    
    /** vanity paths will have precedence over existing /etc/map mapping? */
    private boolean vanityPathPrecedence = DEFAULT_VANITY_PATH_PRECEDENCE;


    /** Vanity path whitelist */
    private String[] vanityPathWhiteList;

    /** Vanity path blacklist */
    private String[] vanityPathBlackList;

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
     * Getter for rootProviderEntry.
     */
    public RootResourceProviderEntry getRootProviderEntry() {
        return rootProviderEntry;
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
        return mangleNamespacePrefixes;

    }

    public String getMapRoot() {
        return mapRoot;
    }

    public int getDefaultVanityPathRedirectStatus() {
        return defaultVanityPathRedirectStatus;
    }

    public boolean isVanityPathEnabled() {
        return this.enableVanityPath;
    }

    public boolean isOptimizeAliasResolutionEnabled() {
        return this.enableOptimizeAliasResolution;
    }

    public String[] getVanityPathWhiteList() {
        return this.vanityPathWhiteList;
    }

    public String[] getVanityPathBlackList() {
        return this.vanityPathBlackList;
    }
    
    public boolean hasVanityPathPrecedence() {
        return this.vanityPathPrecedence;
    }

    // ---------- SCR Integration ---------------------------------------------

    /** Activates this component, called by SCR before registering as a service */
    @Activate
    protected void activate(final ComponentContext componentContext) {
        this.componentContext = componentContext;
        this.rootProviderEntry.setEventAdmin(this.eventAdmin);
        final Dictionary<?, ?> properties = componentContext.getProperties();

        final BidiMap virtuals = new TreeBidiMap();
        final String[] virtualList = PropertiesUtil.toStringArray(properties.get(PROP_VIRTUAL));
        for (int i = 0; virtualList != null && i < virtualList.length; i++) {
            final String[] parts = Mapping.split(virtualList[i]);
            virtuals.put(parts[0], parts[2]);
        }
        virtualURLMap = virtuals;

        final List<Mapping> maps = new ArrayList<Mapping>();
        final String[] mappingList = (String[]) properties.get(PROP_MAPPING);
        for (int i = 0; mappingList != null && i < mappingList.length; i++) {
            maps.add(new Mapping(mappingList[i]));
        }
        final Mapping[] tmp = maps.toArray(new Mapping[maps.size()]);

        // check whether direct mappings are allowed
        final Boolean directProp = (Boolean) properties.get(PROP_ALLOW_DIRECT);
        allowDirect = (directProp != null) ? directProp.booleanValue() : true;
        if (allowDirect) {
            final Mapping[] tmp2 = new Mapping[tmp.length + 1];
            tmp2[0] = Mapping.DIRECT;
            System.arraycopy(tmp, 0, tmp2, 1, tmp.length);
            mappings = tmp2;
        } else {
            mappings = tmp;
        }

        // from configuration if available
        searchPath = PropertiesUtil.toStringArray(properties.get(PROP_PATH));
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

        // namespace mangling
        mangleNamespacePrefixes = PropertiesUtil.toBoolean(properties.get(PROP_MANGLE_NAMESPACES), false);

        // the root of the resolver mappings
        mapRoot = PropertiesUtil.toString(properties.get(PROP_MAP_LOCATION), MapEntries.DEFAULT_MAP_ROOT);

        defaultVanityPathRedirectStatus = PropertiesUtil.toInteger(properties.get(PROP_DEFAULT_VANITY_PATH_REDIRECT_STATUS),
                                                                   MapEntries.DEFAULT_DEFAULT_VANITY_PATH_REDIRECT_STATUS);
        this.enableVanityPath = PropertiesUtil.toBoolean(properties.get(PROP_ENABLE_VANITY_PATH), DEFAULT_ENABLE_VANITY_PATH);
        // vanity path white list
        this.vanityPathWhiteList = null;
        String[] vanityPathPrefixes = PropertiesUtil.toStringArray(properties.get(PROP_ALLOWED_VANITY_PATH_PREFIX));
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
        vanityPathPrefixes = PropertiesUtil.toStringArray(properties.get(PROP_DENIED_VANITY_PATH_PREFIX));
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

        this.enableOptimizeAliasResolution = PropertiesUtil.toBoolean(properties.get(PROP_ENABLE_OPTIMIZE_ALIAS_RESOLUTION), DEFAULT_ENABLE_OPTIMIZE_ALIAS_RESOLUTION);

        this.vanityPathPrecedence = PropertiesUtil.toBoolean(properties.get(PROP_VANITY_PATH_PRECEDENCE), DEFAULT_VANITY_PATH_PRECEDENCE);
        
        final BundleContext bc = componentContext.getBundleContext();

        // check for required property
        final String[] required = PropertiesUtil.toStringArray(properties.get(PROP_REQUIRED_PROVIDERS));
        this.preconds.activate(bc, required);
        this.checkFactoryPreconditions();
    }

    /**
     * Deactivates this component (called by SCR to take out of service)
     */
    @Deactivate
    protected void deactivate() {
        this.componentContext = null;
        this.preconds.deactivate();
        this.rootProviderEntry.setEventAdmin(null);
        this.resourceDecoratorTracker.close();

        this.unregisterFactory();
    }

    /**
     * Unregister the factory (if registered)
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
            if ( local.commonFactory != null ) {
                local.commonFactory.deactivate();;
            }
        }
    }

    /**
     * Try to register the factory.
     */
    private void registerFactory(final ComponentContext localContext) {
        final FactoryRegistration local;
        synchronized (this) {
            if (this.factoryRegistration == null) {
                this.factoryRegistration = new FactoryRegistration();
                local = this.factoryRegistration;
            } else {
                local = null;
            }
        }

        if ( local != null ) {
            // activate and register factory
            final Dictionary<String, Object> serviceProps = new Hashtable<String, Object>();
            serviceProps.put(Constants.SERVICE_VENDOR, localContext.getProperties().get(Constants.SERVICE_VENDOR));
            serviceProps.put(Constants.SERVICE_DESCRIPTION, localContext.getProperties().get(Constants.SERVICE_DESCRIPTION));

            local.commonFactory = new CommonResourceResolverFactoryImpl(this);
            local.commonFactory.activate(localContext.getBundleContext());
            local.factoryRegistration = localContext.getBundleContext().registerService(
                ResourceResolverFactory.class.getName(), new ServiceFactory() {

                    public Object getService(final Bundle bundle, final ServiceRegistration registration) {
                        final ResourceResolverFactoryImpl r = new ResourceResolverFactoryImpl(
                                local.commonFactory, bundle,
                            ResourceResolverFactoryActivator.this.serviceUserMapper);
                        return r;
                    }

                    public void ungetService(final Bundle bundle, final ServiceRegistration registration, final Object service) {
                        // nothing to do
                    }
                }, serviceProps);

            // check if an unregister happened in between
            boolean doUnregister = false;
            synchronized ( this ) {
                if ( this.factoryRegistration != local ) {
                    doUnregister = true;
                }
            }
            if ( doUnregister ) {
                this.unregisterFactory(local);
            }
        }
    }

    /**
     * Check the preconditions and if it changed, either register factory or unregister
     */
    private void checkFactoryPreconditions() {
        final ComponentContext localContext = this.componentContext;
        if ( localContext != null ) {
            final boolean result = this.preconds.checkPreconditions();
            if ( result ) {
                this.registerFactory(localContext);
            } else {
                this.unregisterFactory();
            }
        }
    }

    /**
     * Bind a resource provider.
     */
    protected void bindResourceProvider(final ResourceProvider provider, final Map<String, Object> props) {
        this.rootProviderEntry.bindResourceProvider(provider, props);
        this.preconds.bindProvider(props);
        this.checkFactoryPreconditions();
    }

    /**
     * Unbind a resource provider.
     */
    protected void unbindResourceProvider(final ResourceProvider provider, final Map<String, Object> props) {
        this.rootProviderEntry.unbindResourceProvider(provider, props);
        this.preconds.unbindProvider(props);
        this.checkFactoryPreconditions();
    }

    /**
     * Bind a resource provider factory.
     */
    protected void bindResourceProviderFactory(final ResourceProviderFactory provider, final Map<String, Object> props) {
        this.rootProviderEntry.bindResourceProviderFactory(provider, props);
        this.preconds.bindProvider(props);
        this.checkFactoryPreconditions();
    }

    /**
     * Unbind a resource provider factory.
     */
    protected void unbindResourceProviderFactory(final ResourceProviderFactory provider, final Map<String, Object> props) {
        this.rootProviderEntry.unbindResourceProviderFactory(provider, props);
        this.preconds.unbindProvider(props);
        this.checkFactoryPreconditions();
        boolean unregister = false;
        synchronized ( this ) {
            unregister = this.factoryRegistration != null;
        }
        if (unregister ) {
            this.unregisterFactory();
            this.checkFactoryPreconditions();
        }
    }

    /**
     * Bind a resource decorator.
     */
    protected void bindResourceDecorator(final ResourceDecorator decorator, final Map<String, Object> props) {
        this.resourceDecoratorTracker.bindResourceDecorator(decorator, props);
    }

    /**
     * Unbind a resource decorator.
     */
    protected void unbindResourceDecorator(final ResourceDecorator decorator, final Map<String, Object> props) {
        this.resourceDecoratorTracker.unbindResourceDecorator(decorator, props);
    }
}