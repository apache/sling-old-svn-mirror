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
package org.apache.sling.jcr.resource.internal.helper;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.jcr.resource.internal.JcrResourceResolver;
import org.apache.sling.jcr.resource.internal.JcrResourceResolverFactoryImpl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapEntries implements EventHandler {

    public static MapEntries EMPTY = new MapEntries();

    public static final String DEFAULT_MAP_ROOT = "/etc/map";

    static final String ANY_SCHEME_HOST = "[^/]+/[^/]+";

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    private JcrResourceResolverFactoryImpl factory;

    private volatile ResourceResolver resolver;

    private final String mapRoot;

    private List<MapEntry> resolveMaps;

    private Collection<MapEntry> mapMaps;

    private Collection<String> vanityTargets;

    private ServiceRegistration registration;

    private ServiceTracker eventAdminTracker;

    private final Semaphore initTrigger = new Semaphore(0);

    private final ReentrantLock initializing = new ReentrantLock();

    private MapEntries() {
        this.factory = null;
        this.resolver = null;
        this.mapRoot = DEFAULT_MAP_ROOT;

        this.resolveMaps = Collections.<MapEntry> emptyList();
        this.mapMaps = Collections.<MapEntry> emptyList();
        this.vanityTargets = Collections.<String> emptySet();
        this.registration = null;
        this.eventAdminTracker = null;
    }

    public MapEntries(final JcrResourceResolverFactoryImpl factory,
                      final BundleContext bundleContext,
                      final ServiceTracker eventAdminTracker)
    throws LoginException {
        this.resolver = factory.getAdministrativeResourceResolver(null);
        this.factory = factory;
        this.mapRoot = factory.getMapRoot();
        this.eventAdminTracker = eventAdminTracker;

        this.resolveMaps = Collections.<MapEntry> emptyList();
        this.mapMaps = Collections.<MapEntry> emptyList();
        this.vanityTargets = Collections.<String> emptySet();

        doInit();

        // build a filter which matches if any of the nodeProps (JCR
        // properties modified) is listed in any of the eventProps (event
        // properties listing modified JCR properties)
        // this allows to only get events interesting for updating the
        // internal structure
        final String[] nodeProps = {
            "sling:vanityPath", "sling:vanityOrder", JcrResourceResolver.PROP_REDIRECT_EXTERNAL_REDIRECT_STATUS,
            JcrResourceResolver.PROP_REDIRECT_EXTERNAL, JcrResourceResolver.PROP_REDIRECT_INTERNAL,
            JcrResourceResolver.PROP_REDIRECT_EXTERNAL_STATUS
        };
        final String[] eventProps = {
            "resourceAddedAttributes", "resourceChangedAttributes", "resourceRemovedAttributes"
        };
        StringBuilder filter = new StringBuilder();
        filter.append("(|");
        for (String eventProp : eventProps) {
            filter.append("(|");
            for (String nodeProp : nodeProps) {
                filter.append('(').append(eventProp).append('=').append(nodeProp).append(')');
            }
            filter.append(")");
        }
        filter.append("(" + EventConstants.EVENT_TOPIC + "=" + SlingConstants.TOPIC_RESOURCE_REMOVED + ")");
        filter.append(")");

        final Dictionary<String, String> props = new Hashtable<String, String>();
        props.put(EventConstants.EVENT_TOPIC, "org/apache/sling/api/resource/*");
        props.put(EventConstants.EVENT_FILTER, filter.toString());
        props.put(Constants.SERVICE_DESCRIPTION, "Map Entries Observation");
        props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        this.registration = bundleContext.registerService(EventHandler.class.getName(), this, props);

        Thread updateThread = new Thread(new Runnable() {
            public void run() {
                MapEntries.this.init();
            }
        }, "MapEntries Update");
        updateThread.start();
    }

    /**
     * Signals the init method that a the doInit method should be
     * called.
     */
    private void triggerInit() {
        // only release if there is not one in the queue already
        if (initTrigger.availablePermits() < 1) {
            initTrigger.release();
        }
    }

    /**
     * Runs as the method of the update thread. Waits for the triggerInit
     * method to trigger a call to doInit. Terminates when the resolver
     * has been null-ed after having been triggered.
     */
    void init() {
        while (MapEntries.this.resolver != null) {
            try {
                MapEntries.this.initTrigger.acquire();
                MapEntries.this.doInit();
            } catch (InterruptedException ie) {
                // just continue acquisition
            }
        }

    }

    /**
     * Actual initializer. Guards itself agains concurrent use by
     * using a ReentrantLock. Does nothing if the resource resolver
     * has already been null-ed.
     */
    private void doInit() {

        this.initializing.lock();
        try {
            final ResourceResolver resolver = this.resolver;
            if (resolver == null) {
                return;
            }

            List<MapEntry> newResolveMaps = new ArrayList<MapEntry>();
            SortedMap<String, MapEntry> newMapMaps = new TreeMap<String, MapEntry>();

            // load the /etc/map entries into the maps
            loadResolverMap(resolver, newResolveMaps, newMapMaps);

            // load the configuration into the resolver map
            Collection<String> vanityTargets = loadVanityPaths(resolver, newResolveMaps);
            loadConfiguration(factory, newResolveMaps);

            // load the configuration into the mapper map
            loadMapConfiguration(factory, newMapMaps);

            // sort List
            Collections.sort(newResolveMaps);

            this.vanityTargets = Collections.unmodifiableCollection(vanityTargets);
            this.resolveMaps = Collections.unmodifiableList(newResolveMaps);
            this.mapMaps = Collections.unmodifiableSet(new TreeSet<MapEntry>(newMapMaps.values()));

            sendChangeEvent();

        } catch (Exception e) {

            log.warn("doInit: Unexpected problem during initialization", e);

        } finally {

            this.initializing.unlock();

        }
    }

    /**
     * Cleans up this class.
     */
    public void dispose() {
        if ( this.registration != null ) {
            this.registration.unregister();
            this.registration = null;
        }

        /*
         * Cooperation with doInit: The same lock as used by doInit
         * is acquired thus preventing doInit from running and waiting
         * for a concurrent doInit to terminate.
         * Once the lock has been acquired, the resource resolver is
         * null-ed (thus causing the init to terminate when triggered
         * the right after and prevent the doInit method from doing any
         * thing).
         */

        // wait at most 10 seconds for a notifcation during initialization
        boolean initLocked;
        try {
            initLocked = this.initializing.tryLock(10, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            initLocked = false;
        }

        try {
            if (!initLocked) {
                log.warn("dispose: Could not acquire initialization lock within 10 seconds; ongoing intialization may fail");
            }

            // immediately set the resolver field to null to indicate
            // that we have been disposed (this also signals to the
            // event handler to stop working
            final ResourceResolver oldResolver = this.resolver;
            this.resolver = null;

            // trigger initialization to terminate init thread
            triggerInit();

            if (oldResolver != null) {
                oldResolver.close();
            } else {
                log.warn("dispose: ResourceResolver has already been cleared before; duplicate call to dispose ?");
            }
        } finally {
            if (initLocked) {
                this.initializing.unlock();
            }
        }

        // clear the rest of the fields
        this.factory = null;
        this.eventAdminTracker = null;
    }

    public List<MapEntry> getResolveMaps() {
        return resolveMaps;
    }

    public Collection<MapEntry> getMapMaps() {
        return mapMaps;
    }

    // ---------- EventListener interface

    /**
     * Handles the change to any of the node properties relevant for vanity URL
     * mappings. The
     * {@link #MapEntries(JcrResourceResolverFactoryImpl, BundleContext, ServiceTracker)}
     * constructor makes sure the event listener is registered to only get
     * appropriate events.
     */
    public void handleEvent(final Event event) {
        // check whether a remove event has an influence on vanity paths
        boolean doInit = true;
        if (SlingConstants.TOPIC_RESOURCE_REMOVED.equals(event.getTopic())) {
            doInit = false;
            final Object p = event.getProperty(SlingConstants.PROPERTY_PATH);
            if (p instanceof String) {
                final String path = (String) p;
                doInit = path.startsWith(this.mapRoot);
                if (!doInit) {
                    for (String target : this.vanityTargets) {
                        if (target.startsWith(path)) {
                            doInit = true;
                            break;
                        }
                    }
                }
            }
        }

        // trigger an update
        if (doInit) {
            triggerInit();
        }
    }

    // ---------- internal

    /**
     * Send an OSGi event
     */
    private void sendChangeEvent() {
        final EventAdmin ea = (EventAdmin) this.eventAdminTracker.getService();
        if (ea != null) {
            // we hard code the topic here and don't use
            // SlingConstants.TOPIC_RESOURCE_RESOLVER_MAPPING_CHANGED
            // to avoid requiring the latest API version for this bundle to work
            final Event event = new Event("org/apache/sling/api/resource/ResourceResolverMapping/CHANGED",
                (Dictionary<?, ?>) null);
            ea.postEvent(event);
        }
    }

    private void loadResolverMap(final ResourceResolver resolver,
            Collection<MapEntry> resolveEntries,
            Map<String, MapEntry> mapEntries) {
        // the standard map configuration
        Resource res = resolver.getResource(mapRoot);
        if (res != null) {
            gather(resolver, resolveEntries, mapEntries, res, "");
        }
    }

    private void gather(final ResourceResolver resolver,
            Collection<MapEntry> resolveEntries,
            Map<String, MapEntry> mapEntries, Resource parent, String parentPath) {
        // scheme list
        Iterator<Resource> children = ResourceUtil.listChildren(parent);
        while (children.hasNext()) {
            final Resource child = children.next();
            final ValueMap vm = ResourceUtil.getValueMap(child);

            String name = vm.get(JcrResourceResolver.PROP_REG_EXP, String.class);
            boolean trailingSlash = false;
            if (name == null) {
                name = ResourceUtil.getName(child).concat("/");
                trailingSlash = true;
            }

            String childPath = parentPath.concat(name);

            // gather the children of this entry (only if child is not end hooked)
            if (!childPath.endsWith("$")) {

                // add trailing slash to child path to append the child
                String childParent = childPath;
                if (!trailingSlash) {
                    childParent = childParent.concat("/");
                }

                gather(resolver, resolveEntries, mapEntries, child, childParent);
            }

            // add resolution entries for this node
            MapEntry childResolveEntry = MapEntry.createResolveEntry(childPath,
                child, trailingSlash);
            if (childResolveEntry != null) {
                resolveEntries.add(childResolveEntry);
            }

            // add map entries for this node
            List<MapEntry> childMapEntries = MapEntry.createMapEntry(childPath,
                child, trailingSlash);
            if (childMapEntries != null) {
                for (MapEntry mapEntry : childMapEntries) {
                    addMapEntry(mapEntries, mapEntry.getPattern(),
                        mapEntry.getRedirect()[0], mapEntry.getStatus());
                }
            }

        }
    }

    private Collection<String> loadVanityPaths(final ResourceResolver resolver,
            List<MapEntry> entries) {
        // sling:VanityPath (uppercase V) is the mixin name
        // sling:vanityPath (lowercase) is the property name
        final HashSet<String> targetPaths = new HashSet<String>();
        final String queryString = "SELECT sling:vanityPath, sling:redirect, sling:redirectStatus FROM sling:VanityPath WHERE sling:vanityPath IS NOT NULL ORDER BY sling:vanityOrder DESC";
        final Iterator<Resource> i = resolver.findResources(
            queryString, "sql");
        while (i.hasNext()) {
            Resource resource = i.next();
            ValueMap row = resource.adaptTo(ValueMap.class);
            if (row == null) {
                continue;
            }

            // url is ignoring scheme and host.port and the path is
            // what is stored in the sling:vanityPath property
            String[] pVanityPaths = row.get("sling:vanityPath", new String[0]);
            for (String pVanityPath : pVanityPaths) {
                final String url = getVanityPath(pVanityPath);
                if ( url != null ) {
                    // redirect target is the node providing the sling:vanityPath
                    // property (or its parent if the node is called jcr:content)
                    String redirect = resource.getPath();
                    if (ResourceUtil.getName(redirect).equals("jcr:content")) {
                        redirect = ResourceUtil.getParent(redirect);
                    }

                    // whether the target is attained by a 302/FOUND or by an
                    // internal redirect is defined by the sling:redirect property
                    int status = row.get("sling:redirect", false)
                            ? row.get(JcrResourceResolver.PROP_REDIRECT_EXTERNAL_REDIRECT_STATUS, HttpServletResponse.SC_FOUND)
                            : -1;

                    // 1. entry with exact match
                    entries.add(new MapEntry(url + "$", status, false, redirect
                        + ".html"));

                    // 2. entry with match supporting selectors and extension
                    entries.add(new MapEntry(url + "(\\..*)", status, false,
                        redirect + "$1"));

                    // 3. keep the path to return
                    targetPaths.add(redirect);
                }
            }
        }
        return targetPaths;
    }

    private String getVanityPath(final String pVanityPath) {
        String result = null;
        if ( pVanityPath != null ) {
            String path = pVanityPath.trim();
            if ( path.length() > 0 ) {
                // check for url
                if ( path.indexOf(":/") > - 1 ) {
                    try {
                        final URL u = new URL(path);
                        path = u.getProtocol() + '/' + u.getHost() + '.' + u.getPort() + u.getPath();
                    } catch (MalformedURLException e) {
                        log.warn("Ignoring malformed vanity path {}", pVanityPath);
                        path = null;
                    }
                } else {
                    if ( !path.startsWith("/") ) {
                        path = "/" + path;
                    }
                    path = "^" + ANY_SCHEME_HOST + path;
                }

                // remove extension
                if ( path != null ) {
                    final int lastSlash = path.lastIndexOf('/');
                    final int firstDot = path.indexOf('.', lastSlash + 1);
                    if ( firstDot != -1 ) {
                        path = path.substring(0, firstDot);
                        log.warn("Removing extension from vanity path {}", pVanityPath);
                    }
                    result = path;
                }
            }
        }
        return result;
    }

    private void loadConfiguration(JcrResourceResolverFactoryImpl factory,
            List<MapEntry> entries) {
        // virtual uris
        Map<?, ?> virtuals = factory.getVirtualURLMap();
        if (virtuals != null) {
            for (Entry<?, ?> virtualEntry : virtuals.entrySet()) {
                String extPath = (String) virtualEntry.getKey();
                String intPath = (String) virtualEntry.getValue();
                if (!extPath.equals(intPath)) {
                    // this regular expression must match the whole URL !!
                    String url = "^" + ANY_SCHEME_HOST + extPath + "$";
                    String redirect = intPath;
                    entries.add(new MapEntry(url, -1, false, redirect));
                }
            }
        }

        // URL Mappings
        Mapping[] mappings = factory.getMappings();
        if (mappings != null) {
            Map<String, List<String>> map = new HashMap<String, List<String>>();
            for (Mapping mapping : mappings) {
                if (mapping.mapsInbound()) {
                    String url = mapping.getTo();
                    String alias = mapping.getFrom();
                    if (url.length() > 0) {
                        List<String> aliasList = map.get(url);
                        if (aliasList == null) {
                            aliasList = new ArrayList<String>();
                            map.put(url, aliasList);
                        }
                        aliasList.add(alias);
                    }
                }
            }
            for (Entry<String, List<String>> entry : map.entrySet()) {
                entries.add(new MapEntry(ANY_SCHEME_HOST + entry.getKey(),
                    -1, false, entry.getValue().toArray(new String[0])));
            }
        }
    }

    private void loadMapConfiguration(JcrResourceResolverFactoryImpl factory,
            Map<String, MapEntry> entries) {
        // URL Mappings
        Mapping[] mappings = factory.getMappings();
        if (mappings != null) {
            for (int i = mappings.length - 1; i >= 0; i--) {
                Mapping mapping = mappings[i];
                if (mapping.mapsOutbound()) {
                    String url = mapping.getTo();
                    String alias = mapping.getFrom();
                    if (!url.equals(alias)) {
                        addMapEntry(entries, alias, url, -1);
                    }
                }
            }
        }

        // virtual uris
        Map<?, ?> virtuals = factory.getVirtualURLMap();
        if (virtuals != null) {
            for (Entry<?, ?> virtualEntry : virtuals.entrySet()) {
                String extPath = (String) virtualEntry.getKey();
                String intPath = (String) virtualEntry.getValue();
                if (!extPath.equals(intPath)) {
                    // this regular expression must match the whole URL !!
                    String path = "^" + intPath + "$";
                    String url = extPath;
                    addMapEntry(entries, path, url, -1);
                }
            }
        }
    }

    private void addMapEntry(Map<String, MapEntry> entries, String path,
            String url, int status) {
        MapEntry entry = entries.get(path);
        if (entry == null) {
            entry = new MapEntry(path, status, false, url);
        } else {
            String[] redir = entry.getRedirect();
            String[] newRedir = new String[redir.length + 1];
            System.arraycopy(redir, 0, newRedir, 0, redir.length);
            newRedir[redir.length] = url;
            entry = new MapEntry(entry.getPattern(), entry.getStatus(),
                false, newRedir);
        }
        entries.put(path, entry);
    }
}
