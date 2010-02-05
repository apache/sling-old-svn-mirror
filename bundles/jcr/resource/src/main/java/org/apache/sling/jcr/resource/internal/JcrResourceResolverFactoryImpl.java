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
package org.apache.sling.jcr.resource.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import javax.jcr.Session;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.TreeBidiMap;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.JcrResourceResolverFactory;
import org.apache.sling.jcr.resource.JcrResourceTypeProvider;
import org.apache.sling.jcr.resource.internal.helper.MapEntries;
import org.apache.sling.jcr.resource.internal.helper.Mapping;
import org.apache.sling.jcr.resource.internal.helper.ResourceProviderEntry;
import org.apache.sling.jcr.resource.internal.helper.jcr.JcrResourceProviderEntry;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>JcrResourceResolverFactoryImpl</code> is the
 * {@link JcrResourceResolverFactory} service providing the following
 * functionality:
 * <ul>
 * <li><code>JcrResourceResolverFactory</code> service
 * <li>Bundle listener to load initial content and manage OCM mapping
 * descriptors provided by bundles.
 * <li>Fires OSGi EventAdmin events on behalf of internal helper objects
 * </ul>
 *
 * @scr.component immediate="true" label="%resource.resolver.name"
 *                description="%resource.resolver.description"
 * @scr.property name="service.description"
 *                value="Sling JcrResourceResolverFactory Implementation"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.service interface="org.apache.sling.jcr.resource.JcrResourceResolverFactory"
 * @scr.reference name="ResourceProvider"
 *                interface="org.apache.sling.api.resource.ResourceProvider"
 *                cardinality="0..n" policy="dynamic"
 * @scr.reference name="JcrResourceTypeProvider"
 *                interface="org.apache.sling.jcr.resource.JcrResourceTypeProvider"
 *                cardinality="0..n" policy="dynamic"
 */
public class JcrResourceResolverFactoryImpl implements
        JcrResourceResolverFactory {

    public final static class ResourcePattern {
        public final Pattern pattern;

        public final String replacement;

        public ResourcePattern(final Pattern p, final String r) {
            this.pattern = p;
            this.replacement = r;
        }
    }

    /**
     * @scr.property values.1="/apps" values.2="/libs"
     */
    public static final String PROP_PATH = "resource.resolver.searchpath";

    /**
     * Defines whether namespace prefixes of resource names inside the path
     * (e.g. <code>jcr:</code> in <code>/home/path/jcr:content</code>) are
     * mangled or not.
     * <p>
     * Mangling means that any namespace prefix contained in the path is replaced
     * as per the generic substitution pattern <code>/([^:]+):/_$1_/</code>
     * when calling the <code>map</code> method of the resource resolver.
     * Likewise the <code>resolve</code> methods will unmangle such namespace
     * prefixes according to the substituation pattern
     * <code>/_([^_]+)_/$1:/</code>.
     * <p>
     * This feature is provided since there may be systems out there in the wild
     * which cannot cope with URLs containing colons, even though they are
     * perfectly valid characters in the path part of URI references with a
     * scheme.
     * <p>
     * The default value of this property if no configuration is provided is
     * <code>true</code>.
     *
     * @scr.property value="true" type="Boolean"
     */
    private static final String PROP_MANGLE_NAMESPACES = "resource.resolver.manglenamespaces";

    /**
     * @scr.property value="true" type="Boolean"
     */
    private static final String PROP_ALLOW_DIRECT = "resource.resolver.allowDirect";

    /**
     * The resolver.virtual property has no default configuration. But the sling
     * maven plugin and the sling management console cannot handle empty
     * multivalue properties at the moment. So we just add a dummy direct
     * mapping.
     *
     * @scr.property values.1="/-/"
     */
    private static final String PROP_VIRTUAL = "resource.resolver.virtual";

    /**
     * @scr.property values.1="/-/" values.2="/content/-/"
     *               Cvalues.3="/apps/&times;/docroot/-/"
     *               Cvalues.4="/libs/&times;/docroot/-/"
     *               values.5="/system/docroot/-/"
     */
    private static final String PROP_MAPPING = "resource.resolver.mapping";

    /**
     * @scr.property valueRef="MapEntries.DEFAULT_MAP_ROOT"
     */
    private static final String PROP_MAP_LOCATION = "resource.resolver.map.location";

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * The JCR Repository we access to resolve resources
     *
     * @scr.reference
     */
    private SlingRepository repository;

    /**
     * The (optional) resource type providers, working copy.
     */
    protected final List<JcrResourceTypeProviderEntry> jcrResourceTypeProviders = new ArrayList<JcrResourceTypeProviderEntry>();

    /**
     * An array of the above, updates when changes are created.
     */
    private JcrResourceTypeProvider[] jcrResourceTypeProvidersArray;

    /**
     * List of ResourceProvider services bound before activation of the
     * component.
     */
    private final List<ServiceReference> delayedResourceProviders = new LinkedList<ServiceReference>();

    /**
     * List of JcrResourceTypeProvider services bound before activation of the
     * component.
     */
    protected List<ServiceReference> delayedJcrResourceTypeProviders = new LinkedList<ServiceReference>();


    protected ComponentContext componentContext;

    // helper for the new JcrResourceResolver2
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

    private ResourceProviderEntry rootProviderEntry;

    // whether to mangle paths with namespaces or not
    private boolean mangleNamespacePrefixes;

    /** The resource listenr for the observation events. */
    private JcrResourceListener resourceListener;

    /** The service tracker for the event admin
     */
    private ServiceTracker eventAdminTracker;

    /** The dynamic class loader
     * @scr.reference */
    private DynamicClassLoaderManager dynamicClassLoaderManager;

    public JcrResourceResolverFactoryImpl() {
        this.rootProviderEntry = new ResourceProviderEntry("/", null);

    }

    // ---------- JcrResourceResolverFactory -----------------------------------

    /**
     * Returns a new <code>ResourceResolve</code> for the given session. Note
     * that each call to this method returns a new resource manager instance.
     */
    public ResourceResolver getResourceResolver(Session session) {
        JcrResourceProviderEntry sessionRoot = new JcrResourceProviderEntry(
            session, rootProviderEntry, getJcrResourceTypeProviders(),
            this.getDynamicClassLoader());

        return new JcrResourceResolver(sessionRoot, this, mapEntries);
    }

    protected JcrResourceTypeProvider[] getJcrResourceTypeProviders() {
        return jcrResourceTypeProvidersArray;
    }

    // ---------- Implementation helpers --------------------------------------

    /** Get the dynamic class loader if available */
    ClassLoader getDynamicClassLoader() {
        final DynamicClassLoaderManager dclm = this.dynamicClassLoaderManager;
        if ( dclm != null ) {
            return dclm.getDynamicClassLoader();
        }
        return null;
    }

    /** If uri is a virtual URI returns the real URI, otherwise returns null */
    String virtualToRealUri(String virtualUri) {
        return (virtualURLMap != null)
                ? (String) virtualURLMap.get(virtualUri)
                : null;
    }

    /**
     * If uri is a real URI for any virtual URI, the virtual URI is returned,
     * otherwise returns null
     */
    String realToVirtualUri(String realUri) {
        return (virtualURLMap != null)
                ? (String) virtualURLMap.getKey(realUri)
                : null;
    }

    public BidiMap getVirtualURLMap() {
        return virtualURLMap;
    }

    public Mapping[] getMappings() {
        return mappings;
    }

    String[] getSearchPath() {
        return searchPath;
    }

    boolean isMangleNamespacePrefixes() {
        return mangleNamespacePrefixes;

    }

    public String getMapRoot() {
        return mapRoot;
    }

    MapEntries getMapEntries() {
        return mapEntries;
    }

    /**
     * Getter for rootProviderEntry, making it easier to extend
     * JcrResourceResolverFactoryImpl. See <a
     * href="https://issues.apache.org/jira/browse/SLING-730">SLING-730</a>
     *
     * @return Our rootProviderEntry
     */
    protected ResourceProviderEntry getRootProviderEntry() {
        return rootProviderEntry;
    }

    // ---------- SCR Integration ---------------------------------------------

    /** Activates this component, called by SCR before registering as a service */
    protected void activate(ComponentContext componentContext) {
        // setup tracker first as this is used in the bind/unbind methods
        this.eventAdminTracker = new ServiceTracker(componentContext.getBundleContext(),
                EventAdmin.class.getName(), null);
        this.eventAdminTracker.open();

        this.componentContext = componentContext;

        Dictionary<?, ?> properties = componentContext.getProperties();

        BidiMap virtuals = new TreeBidiMap();
        String[] virtualList = (String[]) properties.get(PROP_VIRTUAL);
        for (int i = 0; virtualList != null && i < virtualList.length; i++) {
            String[] parts = Mapping.split(virtualList[i]);
            virtuals.put(parts[0], parts[2]);
        }
        virtualURLMap = virtuals;

        List<Mapping> maps = new ArrayList<Mapping>();
        String[] mappingList = (String[]) properties.get(PROP_MAPPING);
        for (int i = 0; mappingList != null && i < mappingList.length; i++) {
            maps.add(new Mapping(mappingList[i]));
        }
        Mapping[] tmp = maps.toArray(new Mapping[maps.size()]);

        // check whether direct mappings are allowed
        Boolean directProp = (Boolean) properties.get(PROP_ALLOW_DIRECT);
        allowDirect = (directProp != null) ? directProp.booleanValue() : true;
        if (allowDirect) {
            Mapping[] tmp2 = new Mapping[tmp.length + 1];
            tmp2[0] = Mapping.DIRECT;
            System.arraycopy(tmp, 0, tmp2, 1, tmp.length);
            mappings = tmp2;
        } else {
            mappings = tmp;
        }

        // from configuration if available
        searchPath = OsgiUtil.toStringArray(properties.get(PROP_PATH));
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
        mangleNamespacePrefixes = OsgiUtil.toBoolean(
            properties.get(PROP_MANGLE_NAMESPACES), false);

        // the root of the resolver mappings
        mapRoot = OsgiUtil.toString(properties.get(PROP_MAP_LOCATION),
            MapEntries.DEFAULT_MAP_ROOT);

        // bind resource providers not bound yet
        for (ServiceReference reference : delayedResourceProviders) {
            bindResourceProvider(reference);
        }
        delayedResourceProviders.clear();
        this.processDelayedJcrResourceTypeProviders();

        // set up the map entries from configuration
        try {
            mapEntries = new MapEntries(this, getRepository());
        } catch (Exception e) {
            log.error(
                "activate: Cannot access repository, failed setting up Mapping Support",
                e);
        }

        // start observation listener
        try {
            this.resourceListener = new JcrResourceListener(this.repository, this, "/", "/", this.eventAdminTracker);
        } catch (Exception e) {
            log.error(
                "activate: Cannot create resource listener; resource events for JCR resources will be disabled.",
                e);
        }

        try {
            plugin = new JcrResourceResolverWebConsolePlugin(componentContext.getBundleContext(), this);
        } catch (Throwable ignore) {
            // an exception here propably means the web console plugin is not available
            log.debug(
                    "activate: unable to setup web console plugin.", ignore);
        }
    }

    private JcrResourceResolverWebConsolePlugin plugin;

    /** Deativates this component, called by SCR to take out of service */
    protected void deactivate(ComponentContext componentContext) {
        if (plugin != null) {
            plugin.dispose();
            plugin = null;
        }

        if (mapEntries != null) {
            mapEntries.dispose();
            mapEntries = MapEntries.EMPTY;
        }
        if ( this.eventAdminTracker != null ) {
            this.eventAdminTracker.close();
            this.eventAdminTracker = null;
        }
        if ( this.resourceListener != null ) {
            this.resourceListener.dispose();
            this.resourceListener = null;
        }
        this.componentContext = null;
    }

    protected void processDelayedJcrResourceTypeProviders() {
        synchronized (this.jcrResourceTypeProviders) {
            for (ServiceReference reference : delayedJcrResourceTypeProviders) {
                this.addJcrResourceTypeProvider(reference);
            }
            delayedJcrResourceTypeProviders.clear();
            updateJcrResourceTypeProvidersArray();

        }
    }

    protected void addJcrResourceTypeProvider(final ServiceReference reference) {
        final Long id = (Long) reference.getProperty(Constants.SERVICE_ID);
        long ranking = -1;
        if (reference.getProperty(Constants.SERVICE_RANKING) != null) {
            ranking = (Long) reference.getProperty(Constants.SERVICE_RANKING);
        }
        this.jcrResourceTypeProviders.add(new JcrResourceTypeProviderEntry(id,
            ranking,
            (JcrResourceTypeProvider) this.componentContext.locateService(
                "JcrResourceTypeProvider", reference)));
        Collections.sort(this.jcrResourceTypeProviders,
            new Comparator<JcrResourceTypeProviderEntry>() {

                public int compare(JcrResourceTypeProviderEntry o1,
                        JcrResourceTypeProviderEntry o2) {
                    if (o1.ranking < o2.ranking) {
                        return 1;
                    } else if (o1.ranking > o2.ranking) {
                        return -1;
                    } else {
                        if (o1.serviceId < o2.serviceId) {
                            return -1;
                        } else if (o1.serviceId > o2.serviceId) {
                            return 1;
                        }
                    }
                    return 0;
                }
            });

    }

    protected void bindResourceProvider(ServiceReference reference) {

        String serviceName = getServiceName(reference);

        if (componentContext == null) {

            log.debug("bindResourceProvider: Delaying {}", serviceName);

            // delay binding resource providers if called before activation
            delayedResourceProviders.add(reference);

        } else {

            log.debug("bindResourceProvider: Binding {}", serviceName);

            String[] roots = OsgiUtil.toStringArray(reference.getProperty(ResourceProvider.ROOTS));
            if (roots != null && roots.length > 0) {
                final EventAdmin localEA = (EventAdmin) this.eventAdminTracker.getService();

                ResourceProvider provider = (ResourceProvider) componentContext.locateService(
                    "ResourceProvider", reference);

                // synchronized insertion of new resource providers into
                // the tree to not inadvertently loose an entry
                synchronized (this) {

                    for (String root : roots) {
                        // cut off trailing slash
                        if (root.endsWith("/") && root.length() > 1) {
                            root = root.substring(0, root.length() - 1);
                        }

                        rootProviderEntry.addResourceProvider(root,
                            provider, reference);

                        log.debug("bindResourceProvider: {}={} ({})",
                            new Object[] { root, provider, serviceName });
                        if ( localEA != null ) {
                            final Dictionary<String, Object> props = new Hashtable<String, Object>();
                            props.put(SlingConstants.PROPERTY_PATH, root);
                            localEA.postEvent(new Event(SlingConstants.TOPIC_RESOURCE_PROVIDER_ADDED,
                                    props));
                        }
                    }
                }
            }

            log.debug("bindResourceProvider: Bound {}", serviceName);
        }
    }

    protected void unbindResourceProvider(ServiceReference reference) {

        String serviceName = getServiceName(reference);

        log.debug("unbindResourceProvider: Unbinding {}", serviceName);

        String[] roots = OsgiUtil.toStringArray(reference.getProperty(ResourceProvider.ROOTS));
        if (roots != null && roots.length > 0) {

            final EventAdmin localEA = (EventAdmin) ( this.eventAdminTracker != null ? this.eventAdminTracker.getService() : null);

            // synchronized insertion of new resource providers into
            // the tree to not inadvertently loose an entry
            synchronized (this) {
               if ( componentContext != null ) {
                ResourceProvider provider = (ResourceProvider) componentContext.locateService(
                   "ResourceProvider", reference);


                for (String root : roots) {
                    // cut off trailing slash
                    if (root.endsWith("/") && root.length() > 1) {
                        root = root.substring(0, root.length() - 1);
                    }

                    // TODO: Do not remove this path, if another resource
                    // owns it. This may be the case if adding the provider
                    // yielded an ResourceProviderEntryException
                    rootProviderEntry.removeResourceProvider(root, provider, reference);

                    log.debug("unbindResourceProvider: root={} ({})", root,
                        serviceName);
                    if ( localEA != null ) {
                        final Dictionary<String, Object> props = new Hashtable<String, Object>();
                        props.put(SlingConstants.PROPERTY_PATH, root);
                        localEA.postEvent(new Event(SlingConstants.TOPIC_RESOURCE_PROVIDER_REMOVED,
                                props));
                    }
                }
               }
            }
        }

        log.debug("unbindResourceProvider: Unbound {}", serviceName);
    }

    protected void bindJcrResourceTypeProvider(ServiceReference reference) {
        synchronized (this.jcrResourceTypeProviders) {
            if (componentContext == null) {
                delayedJcrResourceTypeProviders.add(reference);
            } else {
                this.addJcrResourceTypeProvider(reference);
                updateJcrResourceTypeProvidersArray();
            }
        }
    }

    protected void unbindJcrResourceTypeProvider(ServiceReference reference) {
        synchronized (this.jcrResourceTypeProviders) {
            delayedJcrResourceTypeProviders.remove(reference);
            final long id = (Long) reference.getProperty(Constants.SERVICE_ID);
            final Iterator<JcrResourceTypeProviderEntry> i = this.jcrResourceTypeProviders.iterator();
            while (i.hasNext()) {
                final JcrResourceTypeProviderEntry current = i.next();
                if (current.serviceId == id) {
                    i.remove();
                }
            }
            updateJcrResourceTypeProvidersArray();
        }
    }

    /**
     * Updates the JcrResourceTypeProviders array, this method is not thread safe and should only be
     * called from a synchronized block.
     */
    protected void updateJcrResourceTypeProvidersArray() {
        JcrResourceTypeProvider[] providers = null;
        if (this.jcrResourceTypeProviders.size() > 0) {
            providers = new JcrResourceTypeProvider[this.jcrResourceTypeProviders.size()];
            int index = 0;
            final Iterator<JcrResourceTypeProviderEntry> i = this.jcrResourceTypeProviders.iterator();
            while (i.hasNext()) {
                providers[index] = i.next().provider;
                index++;
            }
        }
        jcrResourceTypeProvidersArray = providers;
    }

    // ---------- internal helper ----------------------------------------------

    /** Returns the JCR repository used by this factory */
    protected SlingRepository getRepository() {
        return repository;
    }

    protected static final class JcrResourceTypeProviderEntry {
        final long serviceId;

        final long ranking;

        final JcrResourceTypeProvider provider;

        public JcrResourceTypeProviderEntry(final long id, final long ranking,
                final JcrResourceTypeProvider p) {
            this.serviceId = id;
            this.ranking = ranking;
            this.provider = p;
        }
    }

    private String getServiceName(ServiceReference reference) {
        if (log.isDebugEnabled()) {
            StringBuilder snBuilder = new StringBuilder(64);
            snBuilder.append('{');
            snBuilder.append(reference.toString());
            snBuilder.append('/');
            snBuilder.append(reference.getProperty(Constants.SERVICE_ID));
            snBuilder.append('}');
            return snBuilder.toString();
        }

        return null;
    }


    public void run() {
        String stat = rootProviderEntry.getResolutionStats();
        if ( stat != null ) {
            log.info(stat);
        }
    }
}
