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
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
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
     label = "%resource.resolver.name",
     description = "%resource.resolver.description",
     specVersion = "1.1",
     metatype = true)
@Properties({
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "Apache Sling Resource Resolver Factory"),
    @Property(name = Constants.SERVICE_VENDOR, value = "The Apache Software Foundation")
})
@References({
    @Reference(name = "ResourceProvider", referenceInterface = ResourceProvider.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC),
    @Reference(name = "ResourceProviderFactory", referenceInterface = ResourceProviderFactory.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC),
    @Reference(name = "ResourceDecorator", referenceInterface = ResourceDecorator.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC) })
public class ResourceResolverFactoryActivator {

    @Property(value = { "/apps", "/libs" })
    public static final String PROP_PATH = "resource.resolver.searchpath";

    /**
     * Defines whether namespace prefixes of resource names inside the path
     * (e.g. <code>jcr:</code> in <code>/home/path/jcr:content</code>) are
     * mangled or not.
     * <p>
     * Mangling means that any namespace prefix contained in the path is replaced as per the generic
     * substitution pattern <code>/([^:]+):/_$1_/</code> when calling the <code>map</code> method of
     * the resource resolver. Likewise the <code>resolve</code> methods will unmangle such namespace
     * prefixes according to the substituation pattern <code>/_([^_]+)_/$1:/</code>.
     * <p>
     * This feature is provided since there may be systems out there in the wild which cannot cope
     * with URLs containing colons, even though they are perfectly valid characters in the path part
     * of URI references with a scheme.
     * <p>
     * The default value of this property if no configuration is provided is <code>true</code>.
     *
     */
    @Property(boolValue = true)
    private static final String PROP_MANGLE_NAMESPACES = "resource.resolver.manglenamespaces";

    @Property(boolValue = true)
    private static final String PROP_ALLOW_DIRECT = "resource.resolver.allowDirect";

    @Property(unbounded=PropertyUnbounded.ARRAY, value = "org.apache.sling.jcr.resource.internal.helper.jcr.JcrResourceProviderFactory")
    private static final String PROP_REQUIRED_PROVIDERS = "resource.resolver.required.providers";

    /**
     * The resolver.virtual property has no default configuration. But the sling
     * maven plugin and the sling management console cannot handle empty
     * multivalue properties at the moment. So we just add a dummy direct
     * mapping.
     */
    @Property(value = "/:/", unbounded = PropertyUnbounded.ARRAY)
    private static final String PROP_VIRTUAL = "resource.resolver.virtual";

    @Property(value = { "/:/", "/content/:/", "/system/docroot/:/" })
    private static final String PROP_MAPPING = "resource.resolver.mapping";

    @Property(value = MapEntries.DEFAULT_MAP_ROOT)
    private static final String PROP_MAP_LOCATION = "resource.resolver.map.location";

    @Property(intValue = MapEntries.DEFAULT_DEFAULT_VANITY_PATH_REDIRECT_STATUS)
    private static final String PROP_DEFAULT_VANITY_PATH_REDIRECT_STATUS = "resource.resolver.default.vanity.redirect.status";

    /** Tracker for the resource decorators. */
    private final ResourceDecoratorTracker resourceDecoratorTracker = new ResourceDecoratorTracker();

    /** all mappings */
    private Mapping[] mappings;

    /** The fake urls */
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
    private EventAdmin eventAdmin;

    /** Registered resource resolver factory. */
    private ResourceResolverFactoryImpl factory;

    /** Registration .*/
    private ServiceRegistration factoryRegistration;

    /** ComponentContext */
    private ComponentContext componentContext;

    private int defaultVanityPathRedirectStatus;

    private final FactoryPreconditions preconds = new FactoryPreconditions();

    /**
     * Get the resource decorator tracker.
     */
    public ResourceDecoratorTracker getResourceDecoratorTracker() {
        return this.resourceDecoratorTracker;
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

        defaultVanityPathRedirectStatus = PropertiesUtil.toInteger(properties.get(PROP_DEFAULT_VANITY_PATH_REDIRECT_STATUS), MapEntries.DEFAULT_DEFAULT_VANITY_PATH_REDIRECT_STATUS);

        final BundleContext bc = componentContext.getBundleContext();

        // check for required property
        final String[] required = PropertiesUtil.toStringArray(properties.get(PROP_REQUIRED_PROVIDERS));
        this.preconds.activate(bc, required);
        this.checkFactoryPreconditions();
    }

    /**
     * Deativates this component (called by SCR to take out of service)
     */
    @Deactivate
    protected void deactivate() {
        this.componentContext = null;
        this.preconds.deactivate();
        this.rootProviderEntry.setEventAdmin(null);
        this.resourceDecoratorTracker.close();

        synchronized ( this ) {
            this.unregisterFactory();
        }
    }

    private void unregisterFactory() {
        if ( this.factoryRegistration != null ) {
            final ServiceRegistration local = this.factoryRegistration;
            this.factoryRegistration = null;
            local.unregister();
        }
        if ( this.factory != null ) {
            final ResourceResolverFactoryImpl local = this.factory;
            this.factory = null;
            local.deactivate();
        }
    }

    private void checkFactoryPreconditions() {
        if ( this.componentContext != null ) {
            final boolean result = this.preconds.checkPreconditions();
            if ( result ) {
                synchronized ( this ) {
                    if ( factory == null ) {
                        // register factory
                        this.factory = new ResourceResolverFactoryImpl(this);
                        this.factory.activate(this.componentContext.getBundleContext());
                        final Dictionary<String, Object> serviceProps = new Hashtable<String, Object>();
                        serviceProps.put(Constants.SERVICE_VENDOR, this.componentContext.getProperties().get(Constants.SERVICE_VENDOR));
                        serviceProps.put(Constants.SERVICE_DESCRIPTION, this.componentContext.getProperties().get(Constants.SERVICE_DESCRIPTION));

                        this.factoryRegistration = this.componentContext.getBundleContext().registerService(ResourceResolverFactory.class.getName(),
                                        factory, serviceProps);
                    }
                }
            } else {
                synchronized ( this ) {
                    if ( factory != null ) {
                        this.unregisterFactory();
                    }
                }
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