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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.query.Query;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.internal.JcrResourceResolver2;
import org.apache.sling.jcr.resource.internal.JcrResourceResolverFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapEntries implements EventListener {

    public static MapEntries EMPTY = new MapEntries();

    private static final String ANY_SCHEME_HOST = "[^/]+/[^/]+";

    private static final String MAP_ROOT = "/etc/map";

    private static final String MAP_ROOT_PREFIX = MAP_ROOT + "/";

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    private JcrResourceResolverFactoryImpl factory;

    private JcrResourceResolver2 resolver;

    private Session session;

    private List<MapEntry> resolveMaps;

    private List<MapEntry> mapMaps;

    private Set<String> namespaces;

    private boolean initializing = false;

    private MapEntries() {
        session = null; // not needed
        factory = null;
        resolver = null;

        resolveMaps = Collections.<MapEntry> emptyList();
        mapMaps = Collections.<MapEntry> emptyList();
        namespaces = Collections.<String> emptySet();
    }

    public MapEntries(JcrResourceResolverFactoryImpl factory,
            SlingRepository repository) throws RepositoryException {
        this.factory = factory;
        this.session = repository.loginAdministrative(null);
        this.resolver = (JcrResourceResolver2) factory.getResourceResolver(session);

        init();

        try {
            session.getWorkspace().getObservationManager().addEventListener(
                this, 255, "/", true, null, null, false);
        } catch (RepositoryException re) {
            log.error(
                "MapEntries<init>: Failed registering as observation listener",
                re);
        }
    }

    private void init() {
        synchronized (this) {
            // no initialization if the session has already been reset
            if (session == null) {
                return;
            }

            // set the flag
            initializing = true;
        }

        try {

            List<MapEntry> newResolveMaps = new ArrayList<MapEntry>();
            List<MapEntry> newMapMaps = new ArrayList<MapEntry>();

            // load the /etc/map entries into the maps
            loadResolverMap(resolver, newResolveMaps, newMapMaps);

            // load the configuration into the resolver map
            loadVanityPaths(resolver, newResolveMaps);
            loadConfiguration(factory, newResolveMaps);

            // load the configuration into the mapper map 
            loadMapConfiguration(factory, newMapMaps);

            this.resolveMaps = newResolveMaps;
            this.mapMaps = newMapMaps;

        } finally {

            // reset the flag and notify listeners
            synchronized (this) {
                initializing = false;
                notifyAll();
            }
        }
    }

    public void dispose() {

        Session oldSession;

        // wait at most 10 seconds for a notifcation during initialization
        synchronized (this) {
            if (initializing) {
                try {
                    wait(10L * 1000L);
                } catch (InterruptedException ie) {
                    // ignore
                }
            }

            // immediately set the session field to null to indicate
            // that we have been disposed (this also signals to the
            // event handler to stop working
            oldSession = session;
            session = null;
        }

        if (oldSession != null) {
            try {
                oldSession.getWorkspace().getObservationManager().removeEventListener(
                    this);
            } catch (RepositoryException re) {
                log.error(
                    "dispose: Failed unregistering as observation listener", re);
            }

            try {
                oldSession.logout();
            } catch (Exception e) {
                log.error("dispose: Unexpected problem logging out", e);
            }
        }

        // clear the rest of the fields
        resolver = null;
        factory = null;
    }

    public List<MapEntry> getResolveMaps() {
        return resolveMaps;
    }

    public List<MapEntry> getMapMaps() {
        return mapMaps;
    }

    public Set<String> getNamespacePrefixes() {
        if (namespaces == null) {

            // get the current set of namespaces, we cache throughout our
            // life time..
            String[] namespaceList;
            try {
                namespaceList = getSession().getNamespacePrefixes();
            } catch (RepositoryException re) {
                namespaceList = new String[0];
            }

            namespaces = new HashSet<String>(Arrays.asList(namespaceList));
        }
        return namespaces;
    }

    public Session getSession() {
        return session;
    }

    // ---------- EventListener interface

    public void onEvent(EventIterator events) {
        boolean handleEvent = false;
        while (!handleEvent && session != null && events.hasNext()) {
            Event event = events.nextEvent();
            try {
                String path = event.getPath();
                handleEvent = MAP_ROOT.equals(path)
                    || path.startsWith(MAP_ROOT_PREFIX)
                    || path.endsWith("/sling:vanityPath")
                    || path.endsWith("/sling:vanityOrder")
                    || path.endsWith("/sling:redirect");
            } catch (Throwable t) {
                log.warn("onEvent: Cannot complete event handling", t);
            }
        }

        if (handleEvent) {
            if (session != null) {
                try {
                    init();
                } catch (Throwable t) {
                    log.warn("onEvent: Failed initializing after changes", t);
                }
            } else {
                log.info("onEvent: Already disposed, not reinitializing");
            }
        } else if (log.isDebugEnabled()) {
            log.debug("onEvent: Ignoring irrelevant events");
        }
    }

    // ---------- internal

    private void loadResolverMap(JcrResourceResolver2 resolver,
            List<MapEntry> resolveEntries, List<MapEntry> mapEntries) {
        // the standard map configuration
        Resource res = resolver.getResource(MAP_ROOT);
        if (res != null) {
            gather(resolver, resolveEntries, mapEntries, res, "");
        }
    }

    private void gather(JcrResourceResolver2 resolver,
            List<MapEntry> resolveEntries, List<MapEntry> mapEntries,
            Resource parent, String parentPath) {
        // scheme list
        Iterator<Resource> children = ResourceUtil.listChildren(parent);
        while (children.hasNext()) {
            Resource child = children.next();
            String name = resolver.getProperty(child,
                JcrResourceResolver2.PROP_REG_EXP);
            if (name == null) {
                name = ResourceUtil.getName(child);
            }
            String childPath = parentPath + name;

            MapEntry childResolveEntry = MapEntry.createResolveEntry(childPath,
                child);
            if (childResolveEntry != null) {
                resolveEntries.add(childResolveEntry);
            }

            List<MapEntry> childMapEntries = MapEntry.createMapEntry(childPath,
                child);
            if (childMapEntries != null) {
                mapEntries.addAll(childMapEntries);
            }

            // add trailing slash to child path to append the child
            childPath += "/";

            // gather the children of this entry
            gather(resolver, resolveEntries, mapEntries, child, childPath);
        }
    }

    private void loadVanityPaths(JcrResourceResolver2 resolver,
            List<MapEntry> entries) {
        // sling:VanityPath (uppercase V) is the mixin name
        // sling:vanityPath (lowercase) is the property name
        final String queryString = "SELECT sling:vanityPath, sling:redirect FROM sling:VanityPath WHERE sling:vanityPath IS NOT NULL ORDER BY sling:vanityOrder DESC";
        final Iterator<Map<String, Object>> i = resolver.queryResources(
            queryString, Query.SQL);
        while (i.hasNext()) {
            Map<String, Object> row = i.next();

            // url is ignoring scheme and host.port and the path is
            // what is stored in the sling:vanityPath property
            Object pVanityPath = row.get("sling:vanityPath");
            if (pVanityPath != null) {
                String url = "^" + ANY_SCHEME_HOST
                    + String.valueOf(pVanityPath);

                // redirect target is the node providing the sling:vanityPath
                // property (or its parent if the node is called jcr:content)
                String redirect = String.valueOf(row.get("jcr:path"));
                if (ResourceUtil.getName(redirect).equals("jcr:content")) {
                    redirect = ResourceUtil.getParent(redirect);
                }

                // whether the target is attained by a 302/FOUND or by an
                // internal redirect is defined by the sling:redirect property
                int status = -1;
                if (row.containsKey("sling:redirect")
                    && Boolean.valueOf(String.valueOf(row.get("sling:redirect")))) {
                    status = HttpServletResponse.SC_FOUND;
                }

                // 1. entry with exact match
                entries.add(new MapEntry(url + "$", redirect + ".html", status));

                // 2. entry with match supporting selectors and extension
                entries.add(new MapEntry(url + "(\\..*)", redirect + "$1",
                    status));
            }
        }
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
                    entries.add(new MapEntry(url, redirect, -1));
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
                    entry.getValue().toArray(new String[0]), -1));
            }
        }
    }

    private void loadMapConfiguration(JcrResourceResolverFactoryImpl factory,
            List<MapEntry> entries) {
        // URL Mappings
        Mapping[] mappings = factory.getMappings();
        if (mappings != null) {
            for (int i = mappings.length - 1; i >= 0; i--) {
                Mapping mapping = mappings[i];
                if (mapping.mapsOutbound()) {
                    String url = mapping.getTo();
                    String alias = mapping.getFrom();
                    if (!url.equals(alias)) {
                        entries.add(new MapEntry(alias, url, -1));
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
                    entries.add(new MapEntry(path, url, -1));
                }
            }
        }
    }
}
