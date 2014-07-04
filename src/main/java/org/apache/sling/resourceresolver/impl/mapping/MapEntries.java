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
package org.apache.sling.resourceresolver.impl.mapping;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.resourceresolver.impl.ResourceResolverFactoryImpl;
import org.apache.sling.resourceresolver.impl.ResourceResolverImpl;
import org.apache.sling.resourceresolver.impl.mapping.MapConfigurationProvider.VanityPathConfig;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapEntries implements EventHandler {

    public static final MapEntries EMPTY = new MapEntries();

    private static final String PROP_REG_EXP = "sling:match";

    public static final String PROP_REDIRECT_EXTERNAL = "sling:redirect";

    public static final String PROP_REDIRECT_EXTERNAL_STATUS = "sling:status";

    public static final String PROP_REDIRECT_EXTERNAL_REDIRECT_STATUS = "sling:redirectStatus";

    /** Key for the global list. */
    private static final String GLOBAL_LIST_KEY = "*";

    public static final String DEFAULT_MAP_ROOT = "/etc/map";

    public static final int DEFAULT_DEFAULT_VANITY_PATH_REDIRECT_STATUS = HttpServletResponse.SC_FOUND;

    private static final String JCR_SYSTEM_PREFIX = "/jcr:system/";

    static final String ANY_SCHEME_HOST = "[^/]+/[^/]+";

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    private MapConfigurationProvider factory;

    private volatile ResourceResolver resolver;

    private final String mapRoot;

    private Map<String, List<MapEntry>> resolveMapsMap;

    private Collection<MapEntry> mapMaps;

    private Map <String,List <String>> vanityTargets;

    private Map<String, Map<String, String>> aliasMap;

    private ServiceRegistration registration;

    private EventAdmin eventAdmin;

    private final ReentrantLock initializing = new ReentrantLock();

    private final boolean enabledVanityPaths;

    private final boolean enableOptimizeAliasResolution;
    
    private final boolean vanityPathPrecedence;

    private final List<VanityPathConfig> vanityPathConfig;

    @SuppressWarnings("unchecked")
    private MapEntries() {
        this.factory = null;
        this.resolver = null;
        this.mapRoot = DEFAULT_MAP_ROOT;

        this.resolveMapsMap = Collections.singletonMap(GLOBAL_LIST_KEY, (List<MapEntry>)Collections.EMPTY_LIST);
        this.mapMaps = Collections.<MapEntry> emptyList();
        this.vanityTargets = Collections.<String,List <String>>emptyMap();
        this.aliasMap = Collections.<String, Map<String, String>>emptyMap();
        this.registration = null;
        this.eventAdmin = null;
        this.enabledVanityPaths = true;
        this.enableOptimizeAliasResolution = true;
        this.vanityPathConfig = null;
        this.vanityPathPrecedence = false;
    }

    @SuppressWarnings("unchecked")
    public MapEntries(final MapConfigurationProvider factory, final BundleContext bundleContext, final EventAdmin eventAdmin)
                    throws LoginException {
        this.resolver = factory.getAdministrativeResourceResolver(null);
        this.factory = factory;
        this.mapRoot = factory.getMapRoot();
        this.enabledVanityPaths = factory.isVanityPathEnabled();
        this.vanityPathConfig = factory.getVanityPathConfig();
        this.enableOptimizeAliasResolution = factory.isOptimizeAliasResolutionEnabled();
        this.vanityPathPrecedence = factory.hasVanityPathPrecedence();
        this.eventAdmin = eventAdmin;

        this.resolveMapsMap = Collections.singletonMap(GLOBAL_LIST_KEY, (List<MapEntry>)Collections.EMPTY_LIST);
        this.mapMaps = Collections.<MapEntry> emptyList();
        this.vanityTargets = Collections.<String,List <String>>emptyMap();
        this.aliasMap = Collections.<String, Map<String, String>>emptyMap();

        doInit();

        final Dictionary<String, String> props = new Hashtable<String, String>();
        props.put(EventConstants.EVENT_TOPIC, "org/apache/sling/api/resource/*");
        props.put(EventConstants.EVENT_FILTER, createFilter(this.enabledVanityPaths));
        props.put(Constants.SERVICE_DESCRIPTION, "Map Entries Observation");
        props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        this.registration = bundleContext.registerService(EventHandler.class.getName(), this, props);
    }

    /**
     * Actual initializer. Guards itself against concurrent use by using a
     * ReentrantLock. Does nothing if the resource resolver has already been
     * null-ed.
     */
    protected void doInit() {

        this.initializing.lock();
        try {
            final ResourceResolver resolver = this.resolver;
            final MapConfigurationProvider factory = this.factory;
            if (resolver == null || factory == null) {
                return;
            }

            final Map<String, List<MapEntry>> newResolveMapsMap = new ConcurrentHashMap<String, List<MapEntry>>();  
 
            final Map<String,List<String>> vanityTargets = (this.enabledVanityPaths ? this.loadVanityPaths(resolver, newResolveMapsMap) : Collections.<String,List <String>>emptyMap());
            
            //optimization made in SLING-2521
            if (enableOptimizeAliasResolution){
                final Map<String, Map<String, String>> aliasMap = this.loadAliases(resolver);
                this.aliasMap = aliasMap;
            }

            this.vanityTargets = vanityTargets;
            this.resolveMapsMap = newResolveMapsMap; 

            doUpdateConfiguration();

            sendChangeEvent();
        } catch (final Exception e) {

            log.warn("doInit: Unexpected problem during initialization", e);

        } finally {

            this.initializing.unlock();

        }
    }

    private boolean doAddAttributes(String path, String[] addedAttributes, boolean refreshed) {
        this.initializing.lock();
        boolean newRefreshed = refreshed;
        if (!newRefreshed) {
            resolver.refresh();
            newRefreshed = true;
        }
        try {
            boolean configurationUpdate = false;
            for (String changedAttribute:addedAttributes){
                if ("sling:vanityPath".equals(changedAttribute)) {
                    doAddVanity(path); 
                } else if ("sling:vanityOrder".equals(changedAttribute)) {
                    doUpdateVanityOrder(path, false);
                } else if ("sling:alias".equals(changedAttribute)) {
                    if (enableOptimizeAliasResolution) {
                       doAddAlias(path);
                    }
                } else {
                    configurationUpdate = true;
                }
            }
            if (configurationUpdate) {
                doUpdateConfiguration();
            }
            sendChangeEvent();
        } finally {
            this.initializing.unlock();
        }
        return newRefreshed;
    }

    private boolean doUpdateAttributes(String path, String[] changedAttributes, boolean refreshed) {
        this.initializing.lock();
        boolean newRefreshed = refreshed;
        if (!newRefreshed) {
            resolver.refresh();
            newRefreshed = true;
        }
        try {
            boolean configurationUpdate = false;
            for (String changedAttribute:changedAttributes){
                if ("sling:vanityPath".equals(changedAttribute)) {
                    doUpdateVanity(path);
                } else if ("sling:vanityOrder".equals(changedAttribute)) {
                    doUpdateVanityOrder(path, false);
                } else if ("sling:alias".equals(changedAttribute)) {
                    if (enableOptimizeAliasResolution) {
                        doRemoveAlias(path, false);
                        doAddAlias(path);
                        doUpdateAlias(path, false);
                     }                    
                } else {
                    configurationUpdate = true;
                }
            }
            if (configurationUpdate) {
                doUpdateConfiguration();
            }
            sendChangeEvent();
        } finally {
            this.initializing.unlock();
        }
        return newRefreshed;
    }

    private boolean doRemoveAttributes(String path, String[] removedAttributes, boolean nodeDeletion, boolean refreshed) {
        this.initializing.lock();
        boolean newRefreshed = refreshed;
        if (!newRefreshed) {
            resolver.refresh();
            newRefreshed = true;
        }
        try {
            boolean configurationUpdate = false;
            for (String changedAttribute:removedAttributes){
                if ("sling:vanityPath".equals(changedAttribute)){
                    doRemoveVanity(path);
                } else if ("sling:vanityOrder".equals(changedAttribute)) {
                    doUpdateVanityOrder(path, true);
                } else if ("sling:alias".equals(changedAttribute)) {
                    if (enableOptimizeAliasResolution) {
                        doRemoveAlias(path, nodeDeletion); 
                        doUpdateAlias(path, nodeDeletion);                        
                    }
                } else {
                    configurationUpdate = true;
                }
            }
            if (configurationUpdate) {
                doUpdateConfiguration();
            }
            sendChangeEvent();
        } finally {
            this.initializing.unlock();
        }
        return newRefreshed;
    }

    private void doUpdateConfiguration(){
        final List<MapEntry> globalResolveMap = new ArrayList<MapEntry>();
        final SortedMap<String, MapEntry> newMapMaps = new TreeMap<String, MapEntry>();
        // load the /etc/map entries into the maps
        loadResolverMap(resolver, globalResolveMap, newMapMaps);
        // load the configuration into the resolver map
        loadConfiguration(factory, globalResolveMap);
        // load the configuration into the mapper map
        loadMapConfiguration(factory, newMapMaps);
        // sort global list and add to map
        Collections.sort(globalResolveMap);
        resolveMapsMap.put(GLOBAL_LIST_KEY, globalResolveMap);
        this.mapMaps = Collections.unmodifiableSet(new TreeSet<MapEntry>(newMapMaps.values()));
    }

    private void doAddVanity(String path) {
        Resource resource = resolver.getResource(path);
        loadVanityPath(resource, resolveMapsMap, vanityTargets);
    }

    private void doUpdateVanity(String path) {
         doRemoveVanity(path);
         doAddVanity(path);
    }

    private void doRemoveVanity(String path) {
        String actualContentPath = getActualContentPath(path);
        List <String> l = vanityTargets.get(actualContentPath);
        if (l != null){
            for (String s : l){
                List<MapEntry> entries = this.resolveMapsMap.get(s);
                if (entries!= null) {
                    for (Iterator<MapEntry> iterator =entries.iterator(); iterator.hasNext(); ) {
                        MapEntry entry = iterator.next();
                        String redirect = getMapEntryRedirect(entry);
                        if (redirect != null && redirect.equals(actualContentPath)) {
                            iterator.remove();
                        }
                    }
                }
                if (entries!= null && entries.isEmpty()) {
                    this.resolveMapsMap.remove(s);
                }     
            }
        }
        vanityTargets.remove(actualContentPath);
    }

    private void doUpdateVanityOrder(String path, boolean deletion) {
        Resource resource = resolver.getResource(path);
        final ValueMap props = resource.adaptTo(ValueMap.class);

        long vanityOrder;
        if (deletion) {
            vanityOrder = 0;
        } else {
            vanityOrder = props.get("sling:vanityOrder", Long.class);
        }

        String actualContentPath = getActualContentPath(path);
        List<String> vanityPaths = vanityTargets.get(actualContentPath);
        if (vanityPaths != null) {
            boolean updatedOrder = false;
            for (String vanityTarget : vanityPaths) {
                List<MapEntry> entries = this.resolveMapsMap.get(vanityTarget);
                for (MapEntry entry : entries) {
                    String redirect = getMapEntryRedirect(entry);
                    if (redirect != null && redirect.equals(actualContentPath)) {
                        entry.setOrder(vanityOrder);
                        updatedOrder = true;
                    }
                }
                if (updatedOrder) {
                    Collections.sort(entries);
                }
            }
        }
    }

    private void doAddAlias(String path) {
        Resource resource = resolver.getResource(path);
        loadAlias(resource, this.aliasMap);
    }

    private void doUpdateAlias(String path, boolean nodeDeletion) {
        if (nodeDeletion){
            if (path.endsWith("/jcr:content")) {
                path =  path.substring(0, path.length() - "/jcr:content".length());
                final Resource resource = resolver.getResource(path);  
                if (resource != null) {
                    path =  resource.getPath();            
                    final ValueMap props = resource.adaptTo(ValueMap.class);
                    if (props.get(ResourceResolverImpl.PROP_ALIAS, String[].class) != null) {
                        doAddAlias(path);
                    }
                }
            }  
        } else {
            final Resource resource = resolver.getResource(path);  
            if (resource != null) {
                if (resource.getName().equals("jcr:content")) {  
                    final Resource parent = resource.getParent();
                    path =  parent.getPath();            
                    final ValueMap props = parent.adaptTo(ValueMap.class);
                    if (props.get(ResourceResolverImpl.PROP_ALIAS, String[].class) != null) {
                        doAddAlias(path);
                    }
                } else if (resource.getChild("jcr:content") != null) {
                    Resource jcrContent = resource.getChild("jcr:content");
                    path =  jcrContent.getPath();         
                    final ValueMap props = jcrContent.adaptTo(ValueMap.class);
                    if (props.get(ResourceResolverImpl.PROP_ALIAS, String[].class) != null) {
                        doAddAlias(path);
                    }
                } 
            }
        }
    }

    private void doRemoveAlias(String path, boolean nodeDeletion) {
        String resourceName = null;
        if (nodeDeletion) { 
            if (!"/".equals(path)){
                if (path.endsWith("/jcr:content")) {
                    path =  path.substring(0, path.length() - "/jcr:content".length());
                }  
                resourceName = path.substring(path.lastIndexOf("/")+1);
                path = path.substring(0, path.lastIndexOf("/"));
            } else {
                resourceName = "";
            }
        } else {
            final Resource resource = resolver.getResource(path); 
            if (resource.getName().equals("jcr:content")) {
                final Resource containingResource = resource.getParent();
                path = containingResource.getParent().getPath();   
                resourceName = containingResource.getName();
            } else {
                path =  resource.getParent().getPath();
                resourceName = resource.getName();
            }            
        }
        Map<String, String> aliasMapEntry = aliasMap.get(path);
        if (aliasMapEntry != null) {
            for (Iterator<String> iterator =aliasMapEntry.keySet().iterator(); iterator.hasNext(); ) {
                String key = iterator.next();
                if (resourceName.equals(aliasMapEntry.get(key))){
                    iterator.remove();
                }
            }
        }
        if (aliasMapEntry != null && aliasMapEntry.isEmpty()) {
            this.aliasMap.remove(path);
        }
    }

    public boolean isOptimizeAliasResolutionEnabled() {
        return this.enableOptimizeAliasResolution;
    }

    /**
     * Cleans up this class.
     */
    public void dispose() {
        if (this.registration != null) {
            this.registration.unregister();
            this.registration = null;
        }

        /*
         * Cooperation with doInit: The same lock as used by doInit is acquired
         * thus preventing doInit from running and waiting for a concurrent
         * doInit to terminate. Once the lock has been acquired, the resource
         * resolver is null-ed (thus causing the init to terminate when
         * triggered the right after and prevent the doInit method from doing
         * any thing).
         */

        // wait at most 10 seconds for a notifcation during initialization
        boolean initLocked;
        try {
            initLocked = this.initializing.tryLock(10, TimeUnit.SECONDS);
        } catch (final InterruptedException ie) {
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
        this.eventAdmin = null;
    }

    /**
     * This is for the web console plugin
     */
    public List<MapEntry> getResolveMaps() {
        final List<MapEntry> entries = new ArrayList<MapEntry>();
        for (final List<MapEntry> list : this.resolveMapsMap.values()) {
            entries.addAll(list);
        }
        Collections.sort(entries);
        return Collections.unmodifiableList(entries);
    }

    /**
     * Calculate the resolve maps. As the entries have to be sorted by pattern
     * length, we have to create a new list containing all relevant entries.
     */
    public Iterator<MapEntry> getResolveMapsIterator(final String requestPath) {
        String key = null;
        final int firstIndex = requestPath.indexOf('/');
        final int secondIndex = requestPath.indexOf('/', firstIndex + 1);
        if (secondIndex != -1) {
            key = requestPath.substring(secondIndex);
        }

        return new MapEntryIterator(key, resolveMapsMap, vanityPathPrecedence);
    }

    public Collection<MapEntry> getMapMaps() {
        return mapMaps;
    }

    public Map<String, String> getAliasMap(final String parentPath) {
        return aliasMap.get(parentPath);
    }

    // ---------- EventListener interface

    /**
     * Handles the change to any of the node properties relevant for vanity URL
     * mappings. The {@link #MapEntries(ResourceResolverFactoryImpl, BundleContext, EventAdmin)}
     * constructor makes sure the event listener is registered to only get
     * appropriate events.
     */
    public void handleEvent(final Event event) {

        // check for path (used for some tests below
        final Object p = event.getProperty(SlingConstants.PROPERTY_PATH);
        final String path;
        if (p instanceof String) {
            path = (String) p;
        } else {
            // not a string path or null, ignore this event
            return;
        }

        // don't care for system area
        if (path.startsWith(JCR_SYSTEM_PREFIX)) {
            return;
        }

        boolean wasResolverRefreshed = false;

        //removal of a node is handled differently
        if (SlingConstants.TOPIC_RESOURCE_REMOVED.equals(event.getTopic())) {
            final String actualContentPath = getActualContentPath(path);
            for (final String target : this.vanityTargets.keySet()) {
                if (target.startsWith(actualContentPath)) {
                    wasResolverRefreshed = doRemoveAttributes(actualContentPath, new String [] {"sling:vanityPath"}, true, wasResolverRefreshed);
                }
            }
            for (final String target : this.aliasMap.keySet()) {
                if (actualContentPath.startsWith(target)) {
                    wasResolverRefreshed = doRemoveAttributes(actualContentPath, new String [] {"sling:alias"}, true, wasResolverRefreshed);
                }
            }
        } else {
            String [] addedAttributes = (String []) event.getProperty(SlingConstants.PROPERTY_ADDED_ATTRIBUTES);
            if (addedAttributes != null) {
                if (log.isDebugEnabled()) {
                    log.debug("found added attributes {}", addedAttributes);
                }
                wasResolverRefreshed = doAddAttributes(path, addedAttributes, wasResolverRefreshed);
            }

            String [] changedAttributes = (String []) event.getProperty(SlingConstants.PROPERTY_CHANGED_ATTRIBUTES);
            if (changedAttributes != null) {
                if (log.isDebugEnabled()) {
                    log.debug("found changed attributes {}", changedAttributes);
                }
                wasResolverRefreshed = doUpdateAttributes(path, changedAttributes, wasResolverRefreshed);
            }

            String [] removedAttributes = (String []) event.getProperty(SlingConstants.PROPERTY_REMOVED_ATTRIBUTES);
            if (removedAttributes != null) {
                if (log.isDebugEnabled()) {
                    log.debug("found removed attributes {}", removedAttributes);
                }
                final String checkPath = getActualContentPath(path);
                wasResolverRefreshed = doRemoveAttributes(checkPath, removedAttributes, false, wasResolverRefreshed);
            } 
        }
    }

    // ---------- internal

    private String getActualContentPath(String path){
        final String checkPath;
        if ( path.endsWith("/jcr:content") ) {
            checkPath = path.substring(0, path.length() - "/jcr:content".length());
        } else {
            checkPath = path;
        }
        return checkPath;
    }

    private String getMapEntryRedirect(MapEntry mapEntry) {
        String[] redirect = mapEntry.getRedirect();
        if (redirect.length > 1) {
            log.warn("something went wrong, please restart the bundle");
            return null;
        }

        String path = redirect[0];
        if (path.endsWith("$1")) {
            path = path.substring(0, path.length() - "$1".length());
        } else if (path.endsWith(".html")) {
            path = path.substring(0, path.length() - ".html".length());
        }

        return path;
    }

    /**
     * Send an OSGi event
     */
    private void sendChangeEvent() {
        if (this.eventAdmin != null) {
            final Event event = new Event(SlingConstants.TOPIC_RESOURCE_RESOLVER_MAPPING_CHANGED,
                            (Dictionary<?, ?>) null);
            this.eventAdmin.postEvent(event);
        }
    }

    private void loadResolverMap(final ResourceResolver resolver, final List<MapEntry> entries, final Map<String, MapEntry> mapEntries) {
        // the standard map configuration
        final Resource res = resolver.getResource(mapRoot);
        if (res != null) {
            gather(resolver, entries, mapEntries, res, "");
        }
    }

    private void gather(final ResourceResolver resolver, final List<MapEntry> entries, final Map<String, MapEntry> mapEntries,
                    final Resource parent, final String parentPath) {
        // scheme list
        final Iterator<Resource> children = parent.listChildren();
        while (children.hasNext()) {
            final Resource child = children.next();
            final ValueMap vm = ResourceUtil.getValueMap(child);

            String name = vm.get(PROP_REG_EXP, String.class);
            boolean trailingSlash = false;
            if (name == null) {
                name = child.getName().concat("/");
                trailingSlash = true;
            }

            final String childPath = parentPath.concat(name);

            // gather the children of this entry (only if child is not end
            // hooked)
            if (!childPath.endsWith("$")) {

                // add trailing slash to child path to append the child
                String childParent = childPath;
                if (!trailingSlash) {
                    childParent = childParent.concat("/");
                }

                gather(resolver, entries, mapEntries, child, childParent);
            }

            // add resolution entries for this node
            MapEntry childResolveEntry = null;
            try{
                childResolveEntry=MapEntry.createResolveEntry(childPath, child, trailingSlash);
            }catch (IllegalArgumentException iae){
                //ignore this entry
                log.debug("ignored entry due exception ",iae);
            }
            if (childResolveEntry != null) {
                entries.add(childResolveEntry);
            }

            // add map entries for this node
            final List<MapEntry> childMapEntries = MapEntry.createMapEntry(childPath, child, trailingSlash);
            if (childMapEntries != null) {
                for (final MapEntry mapEntry : childMapEntries) {
                    addMapEntry(mapEntries, mapEntry.getPattern(), mapEntry.getRedirect()[0], mapEntry.getStatus());
                }
            }

        }
    }

    /**
     * Add an entry to the resolve map.
     */
    private boolean addEntry(final Map<String, List<MapEntry>> entryMap, final String key, final MapEntry entry) {
        if (entry==null){
            return false;
        }
        List<MapEntry> entries = entryMap.get(key);
        if (entries == null) {
            entries = new ArrayList<MapEntry>();
            entryMap.put(key, entries);
        }
        entries.add(entry);
        // and finally sort list
        Collections.sort(entries);
        return true;
    }

    /**
     * Load aliases Search for all nodes inheriting the sling:alias
     * property
     */
    private Map<String, Map<String, String>> loadAliases(final ResourceResolver resolver) {
        final Map<String, Map<String, String>> map = new ConcurrentHashMap<String, Map<String, String>>();
        final String queryString = "SELECT sling:alias FROM nt:base WHERE sling:alias IS NOT NULL";
        final Iterator<Resource> i = resolver.findResources(queryString, "sql");
        while (i.hasNext()) {
            final Resource resource = i.next();         
            loadAlias(resource, map);
        }
        return map;
    }
    
    /**
     * Load alias given a resource
     */
    private void loadAlias(final Resource resource, Map<String, Map<String, String>> map) {
        // ignore system tree
        if (resource.getPath().startsWith(JCR_SYSTEM_PREFIX)) {
            log.debug("loadAliases: Ignoring {}", resource);
            return;
        }

        // require properties
        final ValueMap props = resource.adaptTo(ValueMap.class);
        if (props == null) {
            log.debug("loadAliases: Ignoring {} without properties", resource);
            return;
        }

        final String resourceName;
        final String parentPath;
        if (resource.getName().equals("jcr:content")) {
            final Resource containingResource = resource.getParent();
            parentPath = containingResource.getParent().getPath();
            resourceName = containingResource.getName();
        } else {
            parentPath = resource.getParent().getPath();
            resourceName = resource.getName();
        }
        Map<String, String> parentMap = map.get(parentPath);
        for (final String alias : props.get(ResourceResolverImpl.PROP_ALIAS, String[].class)) {
            if (parentMap != null && parentMap.containsKey(alias)) {
                log.warn("Encountered duplicate alias {} under parent path {}. Refusing to replace current target {} with {}.", new Object[] {
                        alias,
                        parentPath,
                        parentMap.get(alias),
                        resourceName
                });
            } else {
                // check alias
                boolean invalid = alias.equals("..") || alias.equals(".");
                if ( !invalid ) {
                    for(final char c : alias.toCharArray()) {
                        // invalid if / or # or a ?
                        if ( c == '/' || c == '#' || c == '?' ) {
                            invalid = true;
                            break;
                        }
                    }
                }
                if ( invalid ) {
                    log.warn("Encountered invalid alias {} under parent path {}. Refusing to use it.",
                            alias, parentPath);
                } else {
                    if (parentMap == null) {
                        parentMap = new HashMap<String, String>();
                        map.put(parentPath, parentMap);
                    }
                    parentMap.put(alias, resourceName);
                }
            }
        }
    }

    /**
     * Load vanity paths Search for all nodes inheriting the sling:VanityPath
     * mixin
     */
    private Map <String, List<String>> loadVanityPaths(final ResourceResolver resolver, final Map<String, List<MapEntry>> entryMap) {
        // sling:VanityPath (uppercase V) is the mixin name
        // sling:vanityPath (lowercase) is the property name
        final Map <String, List<String>> targetPaths = new ConcurrentHashMap <String, List<String>>();
        final String queryString = "SELECT sling:vanityPath, sling:redirect, sling:redirectStatus FROM sling:VanityPath WHERE sling:vanityPath IS NOT NULL ORDER BY sling:vanityOrder DESC";
        final Iterator<Resource> i = resolver.findResources(queryString, "sql");

        while (i.hasNext()) {
            final Resource resource = i.next();
            loadVanityPath(resource, entryMap, targetPaths);
        }

        return targetPaths;
    }

    /**
     * Load vanity path given a resource
     */
    private void loadVanityPath(final Resource resource, final Map<String, List<MapEntry>> entryMap, final Map <String, List<String>> targetPaths) {
        // ignore system tree
        if (resource.getPath().startsWith(JCR_SYSTEM_PREFIX)) {
            log.debug("loadVanityPaths: Ignoring {}", resource);
            return;
        }

        // check white list
        if ( this.vanityPathConfig != null ) {
            boolean allowed = false;
            for(final VanityPathConfig config : this.vanityPathConfig) {
                if ( resource.getPath().startsWith(config.prefix) ) {
                    allowed = !config.isExclude;
                    break;
                }
            }
            if ( !allowed ) {
                log.debug("loadVanityPaths: Ignoring as not in white list {}", resource);
                return;
            }
        }
        // require properties
        final ValueMap props = resource.adaptTo(ValueMap.class);
        if (props == null) {
            log.debug("loadVanityPaths: Ignoring {} without properties", resource);
            return;
        }

        long vanityOrder = 0;
        if (props.containsKey("sling:vanityOrder")) {
            vanityOrder = props.get("sling:vanityOrder", Long.class);
        }   

        // url is ignoring scheme and host.port and the path is
        // what is stored in the sling:vanityPath property
        final String[] pVanityPaths = props.get("sling:vanityPath", new String[0]);
        for (final String pVanityPath : pVanityPaths) {
            final String[] result = this.getVanityPathDefinition(pVanityPath);
            if (result != null) {
                final String url = result[0] + result[1];
                // redirect target is the node providing the
                // sling:vanityPath
                // property (or its parent if the node is called
                // jcr:content)
                final Resource redirectTarget;
                if (resource.getName().equals("jcr:content")) {
                    redirectTarget = resource.getParent();
                } else {
                    redirectTarget = resource;
                }
                final String redirect = redirectTarget.getPath();
                final String redirectName = redirectTarget.getName();

                // whether the target is attained by a external redirect or
                // by an internal redirect is defined by the sling:redirect
                // property
                final int status = props.get("sling:redirect", false) ? props.get(
                        PROP_REDIRECT_EXTERNAL_REDIRECT_STATUS, factory.getDefaultVanityPathRedirectStatus())
                        : -1;

                final String checkPath = result[1];

                boolean addedEntry;
                if (redirectName.indexOf('.') > -1) {
                    // 1. entry with exact match
                    this.addEntry(entryMap, checkPath, getMapEntry(url + "$", status, false, vanityOrder, redirect));

                    final int idx = redirectName.lastIndexOf('.');
                    final String extension = redirectName.substring(idx + 1);

                    // 2. entry with extension
                    addedEntry = this.addEntry(entryMap, checkPath, getMapEntry(url + "\\." + extension, status, false, vanityOrder, redirect));
                } else {
                    // 1. entry with exact match
                    this.addEntry(entryMap, checkPath, getMapEntry(url + "$", status, false, vanityOrder, redirect + ".html"));

                    // 2. entry with match supporting selectors and extension
                    addedEntry = this.addEntry(entryMap, checkPath, getMapEntry(url + "(\\..*)", status, false, vanityOrder, redirect + "$1"));
                }
                if (addedEntry) {
                    // 3. keep the path to return
                    this.updateTargetPaths(targetPaths, redirect, checkPath);
                }
            }
        }
    }
    
    private void updateTargetPaths(final Map<String, List<String>> targetPaths, final String key, final String entry) {
        if (entry == null) {
           return;
        }
        List<String> entries = targetPaths.get(key);
        if (entries == null) {
            entries = new ArrayList<String>();
            targetPaths.put(key, entries);
        }
        entries.add(entry);
    }
    
    /**
     * Create the vanity path definition. String array containing:
     * {protocol}/{host}[.port] {absolute path}
     */
    private String[] getVanityPathDefinition(final String pVanityPath) {
        String[] result = null;
        if (pVanityPath != null) {
            final String info = pVanityPath.trim();
            if (info.length() > 0) {
                String prefix = null;
                String path = null;
                // check for url
                if (info.indexOf(":/") > -1) {
                    try {
                        final URL u = new URL(info);
                        prefix = u.getProtocol() + '/' + u.getHost() + '.' + u.getPort();
                        path = u.getPath();
                    } catch (final MalformedURLException e) {
                        log.warn("Ignoring malformed vanity path {}", pVanityPath);
                    }
                } else {
                    prefix = "^" + ANY_SCHEME_HOST;
                    if (!info.startsWith("/")) {
                        path = "/" + info;
                    } else {
                        path = info;
                    }
                }

                // remove extension
                if (prefix != null) {
                    final int lastSlash = path.lastIndexOf('/');
                    final int firstDot = path.indexOf('.', lastSlash + 1);
                    if (firstDot != -1) {
                        path = path.substring(0, firstDot);
                        log.warn("Removing extension from vanity path {}", pVanityPath);
                    }
                    result = new String[] { prefix, path };
                }
            }
        }
        return result;
    }

    private void loadConfiguration(final MapConfigurationProvider factory, final List<MapEntry> entries) {
        // virtual uris
        final Map<?, ?> virtuals = factory.getVirtualURLMap();
        if (virtuals != null) {
            for (final Entry<?, ?> virtualEntry : virtuals.entrySet()) {
                final String extPath = (String) virtualEntry.getKey();
                final String intPath = (String) virtualEntry.getValue();
                if (!extPath.equals(intPath)) {
                    // this regular expression must match the whole URL !!
                    final String url = "^" + ANY_SCHEME_HOST + extPath + "$";
                    final String redirect = intPath;
                    MapEntry mapEntry = getMapEntry(url, -1, false, redirect);
                    if (mapEntry!=null){
                        entries.add(mapEntry);
                    }
                }
            }
        }

        // URL Mappings
        final Mapping[] mappings = factory.getMappings();
        if (mappings != null) {
            final Map<String, List<String>> map = new HashMap<String, List<String>>();
            for (final Mapping mapping : mappings) {
                if (mapping.mapsInbound()) {
                    final String url = mapping.getTo();
                    final String alias = mapping.getFrom();
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

            for (final Entry<String, List<String>> entry : map.entrySet()) {
                MapEntry mapEntry = getMapEntry(ANY_SCHEME_HOST + entry.getKey(), -1, false, entry.getValue().toArray(new String[0]));
                if (mapEntry!=null){
                    entries.add(mapEntry);
                }
            }
        }
    }

    private void loadMapConfiguration(final MapConfigurationProvider factory, final Map<String, MapEntry> entries) {
        // URL Mappings
        final Mapping[] mappings = factory.getMappings();
        if (mappings != null) {
            for (int i = mappings.length - 1; i >= 0; i--) {
                final Mapping mapping = mappings[i];
                if (mapping.mapsOutbound()) {
                    final String url = mapping.getTo();
                    final String alias = mapping.getFrom();
                    if (!url.equals(alias)) {
                        addMapEntry(entries, alias, url, -1);
                    }
                }
            }
        }

        // virtual uris
        final Map<?, ?> virtuals = factory.getVirtualURLMap();
        if (virtuals != null) {
            for (final Entry<?, ?> virtualEntry : virtuals.entrySet()) {
                final String extPath = (String) virtualEntry.getKey();
                final String intPath = (String) virtualEntry.getValue();
                if (!extPath.equals(intPath)) {
                    // this regular expression must match the whole URL !!
                    final String path = "^" + intPath + "$";
                    final String url = extPath;
                    addMapEntry(entries, path, url, -1);
                }
            }
        }
    }

    private void addMapEntry(final Map<String, MapEntry> entries, final String path, final String url, final int status) {
        MapEntry entry = entries.get(path);
        if (entry == null) {
            entry = getMapEntry(path, status, false, url);
        } else {
            final String[] redir = entry.getRedirect();
            final String[] newRedir = new String[redir.length + 1];
            System.arraycopy(redir, 0, newRedir, 0, redir.length);
            newRedir[redir.length] = url;
            entry = getMapEntry(entry.getPattern(), entry.getStatus(), false, newRedir);
        }
        if (entry!=null){
            entries.put(path, entry);
        }
    }

    /**
     * Returns a filter which matches if any of the nodeProps (JCR properties
     * modified) is listed in any of the eventProps (event properties listing
     * modified JCR properties) this allows to only get events interesting for
     * updating the internal structure
     */
    private static String createFilter(final boolean vanityPathEnabled) {
        final String[] nodeProps = {
                        PROP_REDIRECT_EXTERNAL_REDIRECT_STATUS, PROP_REDIRECT_EXTERNAL,
                        ResourceResolverImpl.PROP_REDIRECT_INTERNAL, PROP_REDIRECT_EXTERNAL_STATUS,
                        PROP_REG_EXP, ResourceResolverImpl.PROP_ALIAS };
        final String[] eventProps = { SlingConstants.PROPERTY_ADDED_ATTRIBUTES, SlingConstants.PROPERTY_CHANGED_ATTRIBUTES, SlingConstants.PROPERTY_REMOVED_ATTRIBUTES };
        final StringBuilder filter = new StringBuilder();
        filter.append("(|");
        for (final String eventProp : eventProps) {
            filter.append("(|");
            if (  vanityPathEnabled ) {
                filter.append('(').append(eventProp).append('=').append("sling:vanityPath").append(')');
                filter.append('(').append(eventProp).append('=').append("sling:vanityOrder").append(')');
            }
            for (final String nodeProp : nodeProps) {
                filter.append('(').append(eventProp).append('=').append(nodeProp).append(')');
            }
            filter.append(")");
        }
        filter.append("(").append(EventConstants.EVENT_TOPIC).append("=").append(SlingConstants.TOPIC_RESOURCE_REMOVED).append(")");
        filter.append(")");

        return filter.toString();
    }

    private static final class MapEntryIterator implements Iterator<MapEntry> {

        private final Map<String, List<MapEntry>> resolveMapsMap;

        private String key;

        private MapEntry next;

        private final Iterator<MapEntry> globalListIterator;
        private MapEntry nextGlobal;

        private Iterator<MapEntry> specialIterator;
        private MapEntry nextSpecial;
        
        private boolean vanityPathPrecedence;

        public MapEntryIterator(final String startKey, final Map<String, List<MapEntry>> resolveMapsMap, final boolean vanityPathPrecedence) {
            this.key = startKey;
            this.resolveMapsMap = resolveMapsMap;
            this.globalListIterator = this.resolveMapsMap.get(GLOBAL_LIST_KEY).iterator();
            this.vanityPathPrecedence = vanityPathPrecedence;
            this.seek();
        }

        /**
         * @see java.util.Iterator#hasNext()
         */
        public boolean hasNext() {
            return this.next != null;
        }

        /**
         * @see java.util.Iterator#next()
         */
        public MapEntry next() {
            if (this.next == null) {
                throw new NoSuchElementException();
            }
            final MapEntry result = this.next;
            this.seek();
            return result;
        }

        /**
         * @see java.util.Iterator#remove()
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }

        private void seek() {
            if (this.nextGlobal == null && this.globalListIterator.hasNext()) {
                this.nextGlobal = this.globalListIterator.next();
            }
            if (this.nextSpecial == null) {
                if (specialIterator != null && !specialIterator.hasNext()) {
                    specialIterator = null;
                }
                while (specialIterator == null && key != null) {
                    // remove selectors and extension
                    final int lastSlashPos = key.lastIndexOf('/');
                    final int lastDotPos = key.indexOf('.', lastSlashPos);
                    if (lastDotPos != -1) {
                        key = key.substring(0, lastDotPos);
                    }
                    final List<MapEntry> special = this.resolveMapsMap.get(key);
                    if (special != null) {
                        specialIterator = special.iterator();
                    }
                    // recurse to the parent
                    if (key.length() > 1) {
                        final int lastSlash = key.lastIndexOf("/");
                        if (lastSlash == 0) {
                            key = null;
                        } else {
                            key = key.substring(0, lastSlash);
                        }
                    } else {
                        key = null;
                    }
                }
                if (this.specialIterator != null && this.specialIterator.hasNext()) {
                    this.nextSpecial = this.specialIterator.next();
                }
            }
            if (this.nextSpecial == null) {
                this.next = this.nextGlobal;
                this.nextGlobal = null;
            } else if (!this.vanityPathPrecedence){
                if (this.nextGlobal == null) {
                    this.next = this.nextSpecial;
                    this.nextSpecial = null;
                } else if (this.nextGlobal.getPattern().length() >= this.nextSpecial.getPattern().length()) {
                    this.next = this.nextGlobal;
                    this.nextGlobal = null;

                }else {
                    this.next = this.nextSpecial;
                    this.nextSpecial = null;
                }                
            } else {
                this.next = this.nextSpecial;
                this.nextSpecial = null;
            }
        }
    };

    private MapEntry getMapEntry(String url, final int status, final boolean trailingSlash,
            final String... redirect){

        MapEntry mapEntry = null;
        try{
            mapEntry = new MapEntry(url, status, trailingSlash, 0, redirect);
        }catch (IllegalArgumentException iae){
            //ignore this entry
            log.debug("ignored entry due exception ",iae);
        }
        return mapEntry;
    }
    
    private MapEntry getMapEntry(String url, final int status, final boolean trailingSlash, long order,
            final String... redirect){

        MapEntry mapEntry = null;
        try{
            mapEntry = new MapEntry(url, status, trailingSlash, order, redirect);
        }catch (IllegalArgumentException iae){
            //ignore this entry
            log.debug("ignored entry due exception ",iae);
        }
        return mapEntry;
    }

}
