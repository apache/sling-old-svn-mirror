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
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceDecorator;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceProviderFactory;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.resourceresolver.impl.console.ResourceResolverWebConsolePlugin;
import org.apache.sling.resourceresolver.impl.helper.ResourceDecoratorTracker;
import org.apache.sling.resourceresolver.impl.helper.ResourceResolverContext;
import org.apache.sling.resourceresolver.impl.mapping.MapEntries;
import org.apache.sling.resourceresolver.impl.mapping.Mapping;
import org.apache.sling.resourceresolver.impl.tree.RootResourceProviderEntry;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.EventAdmin;
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
 * TODO : Should we implement modifiable? It would be easy but what about long running resolvers?
 */
@Component(
     name = "org.apache.sling.jcr.resource.internal.JcrResourceResolverFactoryImpl",
     label = "%resource.resolver.name",
     description = "%resource.resolver.description",
     specVersion = "1.1",
     metatype = true)
@Service(value = ResourceResolverFactory.class)
@Properties({
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "Apache Sling ResourceResolverFactory Implementation"),
    @Property(name = Constants.SERVICE_VENDOR, value = "The Apache Software Foundation")
})
@References({
    @Reference(name = "ResourceProvider", referenceInterface = ResourceProvider.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC),
    @Reference(name = "ResourceProviderFactory", referenceInterface = ResourceProviderFactory.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC),
    @Reference(name = "ResourceDecorator", referenceInterface = ResourceDecorator.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC) })
public class ResourceResolverFactoryImpl implements ResourceResolverFactory {

    public final static class ResourcePattern {
        public final Pattern pattern;

        public final String replacement;

        public ResourcePattern(final Pattern p, final String r) {
            this.pattern = p;
            this.replacement = r;
        }
    }

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

    /** Default logger */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /** Tracker for the resource decorators. */
    private final ResourceDecoratorTracker resourceDecoratorTracker = new ResourceDecoratorTracker();

    // helper for the new ResourceResolver
    private MapEntries mapEntries = MapEntries.EMPTY;

    /** all mappings */
    private Mapping[] mappings;

    /** The fake urls */
    private BidiMap virtualURLMap;

    /** <code>true</code>, if direct mappings from URI to handle are allowed */
    private boolean allowDirect = false;

    // the search path for ResourceResolver.getResource(String)
    private String[] searchPath;

    // the root location of the /etc/map entries
    private String mapRoot;

    private final RootResourceProviderEntry rootProviderEntry = new RootResourceProviderEntry();

    // whether to mangle paths with namespaces or not
    private boolean mangleNamespacePrefixes;

    /** Event admin. */
    private EventAdmin eventAdmin;

    /** The web console plugin. */
    private ResourceResolverWebConsolePlugin plugin;

    // ---------- Resource Resolver Factory ------------------------------------

    /**
     * @see org.apache.sling.api.resource.ResourceResolverFactory#getAdministrativeResourceResolver(java.util.Map)
     */
    public ResourceResolver getAdministrativeResourceResolver(final Map<String, Object> authenticationInfo) throws LoginException {
        return getResourceResolverInternal(authenticationInfo, true);
    }

    /**
     * @see org.apache.sling.api.resource.ResourceResolverFactory#getResourceResolver(java.util.Map)
     */
    public ResourceResolver getResourceResolver(final Map<String, Object> authenticationInfo) throws LoginException {
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
        final ResourceResolverContext ctx = new ResourceResolverContext(isAdmin, authenticationInfo);

        // login
        this.rootProviderEntry.loginToRequiredFactories(ctx);

        return new ResourceResolverImpl(this, ctx);
    }

    /**
     * Get the resource decorator tracker.
     */
    public ResourceDecoratorTracker getResourceDecoratorTracker() {
        return this.resourceDecoratorTracker;
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

    public MapEntries getMapEntries() {
        return mapEntries;
    }

    /**
     * Getter for rootProviderEntry, making it easier to extend
     * JcrResourceResolverFactoryImpl. See <a
     * href="https://issues.apache.org/jira/browse/SLING-730">SLING-730</a>
     *
     * @return Our rootProviderEntry
     */
    protected RootResourceProviderEntry getRootProviderEntry() {
        return rootProviderEntry;
    }

    // ---------- SCR Integration ---------------------------------------------

    /** Activates this component, called by SCR before registering as a service */
    @Activate
    protected void activate(final ComponentContext componentContext) {
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

        // set up the map entries from configuration
        try {
            mapEntries = new MapEntries(this, componentContext.getBundleContext(), this.eventAdmin);
        } catch (final Exception e) {
            logger.error("activate: Cannot access repository, failed setting up Mapping Support", e);
        }

        try {
            plugin = new ResourceResolverWebConsolePlugin(componentContext.getBundleContext(), this);
        } catch (final Throwable ignore) {
            // an exception here propably means the web console plugin is not
            // available
            logger.debug("activate: unable to setup web console plugin.", ignore);
        }
    }

    /**
     * Deativates this component (called by SCR to take out of service)
     */
    @Deactivate
    protected void deactivate() {
        this.rootProviderEntry.setEventAdmin(null);
        if (plugin != null) {
            plugin.dispose();
            plugin = null;
        }

        if (mapEntries != null) {
            mapEntries.dispose();
            mapEntries = MapEntries.EMPTY;
        }
        this.resourceDecoratorTracker.close();
    }

    /**
     * Bind a resource provider.
     */
    protected void bindResourceProvider(final ResourceProvider provider, final Map<String, Object> props) {
        this.rootProviderEntry.bindResourceProvider(provider, props);
    }

    /**
     * Unbind a resource provider.
     */
    protected void unbindResourceProvider(final ResourceProvider provider, final Map<String, Object> props) {
        this.rootProviderEntry.unbindResourceProvider(provider, props);
    }

    /**
     * Bind a resource provider factory.
     */
    protected void bindResourceProviderFactory(final ResourceProviderFactory provider, final Map<String, Object> props) {
        this.rootProviderEntry.bindResourceProviderFactory(provider, props);
    }

    /**
     * Unbind a resource provider factory.
     */
    protected void unbindResourceProviderFactory(final ResourceProviderFactory provider, final Map<String, Object> props) {
        this.rootProviderEntry.unbindResourceProviderFactory(provider, props);
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