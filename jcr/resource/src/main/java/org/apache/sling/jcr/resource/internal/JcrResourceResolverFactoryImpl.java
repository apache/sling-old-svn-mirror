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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import javax.jcr.Session;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.TreeBidiMap;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.JcrResourceResolverFactory;
import org.apache.sling.jcr.resource.JcrResourceTypeProvider;
import org.apache.sling.jcr.resource.internal.helper.Mapping;
import org.apache.sling.jcr.resource.internal.helper.ResourceProviderEntry;
import org.apache.sling.jcr.resource.internal.helper.jcr.JcrResourceProviderEntry;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
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
public class JcrResourceResolverFactoryImpl
    implements JcrResourceResolverFactory {

    public final static class ResourcePattern {
        public final Pattern pattern;
        public final String  replacement;

        public ResourcePattern(final Pattern p, final String r) {
            this.pattern = p;
            this.replacement = r;
        }
    }

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
     * @scr.property values.1="/apps" values.2="/libs"
     */
    public static final String PROP_PATH = "resource.resolver.searchpath";

    /**
     * These regexps are executing during the resource resolving phase
     * before the mappings are applied.
     * @scr.property values.1="/_(.+?)_|/$1:"
     */
    private static final String PROP_REGEXPS = "resource.resolver.regexps";

    /**
     * These regexps are executed during a map operation as the back conversion
     * of the {@link #PROP_REGEXPS}.
     * @scr.property values.1="/_(.+?)_|/$1:"
     */
    private static final String PROP_MAPREGEXPS = "resource.resolver.mapregexps";


    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * The JCR Repository we access to resolve resources
     *
     * @scr.reference
     */
    private SlingRepository repository;

    /** The (optional) resource type providers.
     */
    protected final List<JcrResourceTypeProviderEntry> jcrResourceTypeProviders = new ArrayList<JcrResourceTypeProviderEntry>();

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

    /** all mappings */
    private Mapping[] mappings;

    /** The fake urls */
    private BidiMap virtualURLMap;

    /** The regexp patterns */
    private ResourcePattern[] patterns;

    /** The back regexp patterns */
    private ResourcePattern[] backPatterns;

    /** <code>true</code>, if direct mappings from URI to handle are allowed */
    private boolean allowDirect = false;

    // the search path for ResourceResolver.getResource(String)
    private String[] searchPath;

    private ResourceProviderEntry rootProviderEntry;

    public JcrResourceResolverFactoryImpl() {
        this.rootProviderEntry = new ResourceProviderEntry("/", null, null);
    }

    // ---------- JcrResourceResolverFactory -----------------------------------

    /**
     * Returns a new <code>ResourceResolve</code> for the given session. Note
     * that each call to this method returns a new resource manager instance.
     */
    public ResourceResolver getResourceResolver(Session session) {
        JcrResourceProviderEntry sessionRoot = new JcrResourceProviderEntry(
            session, rootProviderEntry.getEntries(), getJcrResourceTypeProvider());
        return new JcrResourceResolver(sessionRoot, this);
    }

    protected JcrResourceTypeProvider[] getJcrResourceTypeProvider() {
        JcrResourceTypeProvider[] providers = null;
        synchronized ( this.jcrResourceTypeProviders ) {
            if ( this.jcrResourceTypeProviders.size() > 0 ) {
                providers = new JcrResourceTypeProvider[this.jcrResourceTypeProviders.size()];
                int index = 0;
                final Iterator<JcrResourceTypeProviderEntry> i = this.jcrResourceTypeProviders.iterator();
                while ( i.hasNext() ) {
                    providers[index] = i.next().provider;
                }
            }
        }
        return providers;
    }

    // ---------- Implementation helpers --------------------------------------

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

    Mapping[] getMappings() {
        return mappings;
    }

    String[] getSearchPath() {
        return searchPath;
    }

    ResourcePattern[] getPatterns() {
        return patterns;
    }

    ResourcePattern[] getBackPatterns() {
        return backPatterns;
    }

    // ---------- SCR Integration ---------------------------------------------

    /** Activates this component, called by SCR before registering as a service */
    protected void activate(ComponentContext componentContext) {
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

        // regexps
        this.patterns = this.getResourcePatterns((String[]) properties.get(PROP_REGEXPS));
        this.backPatterns = this.getResourcePatterns((String[]) properties.get(PROP_MAPREGEXPS));

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

        // bind resource providers not bound yet
        for (ServiceReference reference : delayedResourceProviders) {
            bindResourceProvider(reference);
        }
        delayedResourceProviders.clear();
        this.processDelayedJcrResourceTypeProviders();
    }

    private ResourcePattern[] getResourcePatterns(String[] patternList) {
        // regexps
        List<ResourcePattern> patterns = new ArrayList<ResourcePattern>();
        if ( patternList != null ) {
            for(final String p : patternList) {
                int pos = p.lastIndexOf('|');
                if ( pos == -1 ) {
                    log.error("Invalid regexp: {}", p);
                } else {
                    final String replString = p.substring(pos + 1);
                    final Pattern pat = Pattern.compile(p.substring(0, pos));
                    patterns.add(new ResourcePattern(pat, replString));
                }
            }
        }
        return patterns.toArray(new ResourcePattern[patterns.size()]);
    }

    protected void processDelayedJcrResourceTypeProviders() {
        synchronized ( this.jcrResourceTypeProviders ) {
            for(ServiceReference reference : delayedJcrResourceTypeProviders ) {
                this.addJcrResourceTypeProvider(reference);
            }
            delayedJcrResourceTypeProviders.clear();
        }
    }

    protected void addJcrResourceTypeProvider(final ServiceReference reference) {
        final Long id = (Long)reference.getProperty(Constants.SERVICE_ID);
        long ranking = -1;
        if ( reference.getProperty(Constants.SERVICE_RANKING) != null ) {
            ranking = (Long)reference.getProperty(Constants.SERVICE_RANKING);
        }
        this.jcrResourceTypeProviders.add(new JcrResourceTypeProviderEntry(id,
                 ranking,
                 (JcrResourceTypeProvider)this.componentContext.locateService("JcrResourceTypeProvider", reference)));
        Collections.sort(this.jcrResourceTypeProviders, new Comparator<JcrResourceTypeProviderEntry>() {

            public int compare(JcrResourceTypeProviderEntry o1,
                               JcrResourceTypeProviderEntry o2) {
                if ( o1.ranking < o2.ranking ) {
                    return 1;
                } else if ( o1.ranking > o2.ranking ) {
                    return -1;
                } else {
                    if ( o1.serviceId < o2.serviceId ) {
                        return -1;
                    } else if ( o1.serviceId > o2.serviceId ) {
                        return 1;
                    }
                }
                return 0;
            }
        });

    }

    /** Deativates this component, called by SCR to take out of service */
    protected void deactivate(ComponentContext componentContext) {
        this.componentContext = null;
    }

    protected void bindResourceProvider(ServiceReference reference) {
        if (componentContext == null) {

            // delay binding resource providers if called before activation
            delayedResourceProviders.add(reference);

        } else {
            String[] roots = OsgiUtil.toStringArray(reference.getProperty(ResourceProvider.ROOTS));
            if (roots != null && roots.length > 0) {

                ResourceProvider provider = (ResourceProvider) componentContext.locateService(
                    "ResourceProvider", reference);

                for (String root : roots) {
                    try {
                        rootProviderEntry.addResourceProvider(root, provider);
                    } catch (IllegalStateException ise) {
                        log.error(
                            "bindResourceProvider: A ResourceProvider for {} is already registered",
                            root);
                    }
                }
            }
        }
    }

    protected void unbindResourceProvider(ServiceReference reference) {
        String[] roots = OsgiUtil.toStringArray(reference.getProperty(ResourceProvider.ROOTS));
        if (roots != null && roots.length > 0) {
            for (String root : roots) {
                // TODO: Do not remove this path, if another resource
                // owns it. This may be the case if adding the provider
                // yielded an IllegalStateException
                rootProviderEntry.removeResourceProvider(root);
            }
        }
    }

    protected void bindJcrResourceTypeProvider(ServiceReference reference) {
        synchronized ( this.jcrResourceTypeProviders ) {
            if (componentContext == null) {
                delayedJcrResourceTypeProviders.add(reference);
            } else {
                this.addJcrResourceTypeProvider(reference);
            }
        }
    }

    protected void unbindJcrResourceTypeProvider(ServiceReference reference) {
        synchronized ( this.jcrResourceTypeProviders ) {
            delayedJcrResourceTypeProviders.remove(reference);
            final long id = (Long)reference.getProperty(Constants.SERVICE_ID);
            final Iterator<JcrResourceTypeProviderEntry> i = this.jcrResourceTypeProviders.iterator();
            while ( i.hasNext() ) {
                final JcrResourceTypeProviderEntry current = i.next();
                if ( current.serviceId == id ) {
                    i.remove();
                }
            }
        }
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

        public JcrResourceTypeProviderEntry(final long id,
                                            final long ranking,
                                            final JcrResourceTypeProvider p) {
            this.serviceId = id;
            this.ranking = ranking;
            this.provider = p;
        }
    }
}
