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
package org.apache.sling.discovery.commons.providers.spi.base;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.discovery.commons.providers.util.ResourceHelper;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

/**
 * The IdMapService is responsible for storing a slingId-clusterNodeId
 * pair to the repository and given all other instances in the cluster
 * do the same can map clusterNodeIds to slingIds (or vice-versa)
 */
@Component(immediate = false)
@Service(value = { IdMapService.class })
public class IdMapService extends AbstractServiceWithBackgroundCheck implements EventHandler {

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private SlingSettingsService settingsService;

    @Reference
    private DiscoveryLiteConfig commonsConfig;
    
    private boolean initialized = false;
    
    private String slingId;

    private long me;

    private final Map<Integer, String> oldIdMapCache = new HashMap<Integer, String>();
    private final Map<Integer, String> idMapCache = new HashMap<Integer, String>();

    private long lastCacheInvalidation = -1;

    private BundleContext bundleContext;

    private ServiceRegistration eventHandlerRegistration;
    
    /** test-only constructor **/
    public static IdMapService testConstructor(
            DiscoveryLiteConfig commonsConfig,
            SlingSettingsService settingsService, 
            ResourceResolverFactory resourceResolverFactory) {
        IdMapService service = new IdMapService();
        service.commonsConfig = commonsConfig;
        service.settingsService = settingsService;
        service.resourceResolverFactory = resourceResolverFactory;
        service.activate(null);
        return service;
    }

    @Activate
    protected void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        registerEventHandler();
        
        startBackgroundCheck("IdMapService-initializer", new BackgroundCheck() {
            
            @Override
            public boolean check() {
                try {
                    return init();
                } catch (Exception e) {
                    logger.error("initializer: could not init due to "+e, e);
                    return false;
                }
            }
        }, null, -1, 1000 /* = 1sec interval */);
    }
    
    @Deactivate
    protected void deactivate() {
        if (eventHandlerRegistration != null) {
            eventHandlerRegistration.unregister();
            eventHandlerRegistration = null;
        }
        // SLING-5592: cancel the potentially running background thread
        cancelPreviousBackgroundCheck();
    }
    
    private void registerEventHandler() {
        if (bundleContext == null) {
            logger.info("registerEventHandler: bundleContext is null - cannot register");
            return;
        }
        Dictionary<String,Object> properties = new Hashtable<String,Object>();
        properties.put(Constants.SERVICE_DESCRIPTION, "IdMap Change Listener.");
        String[] topics = new String[] {
                SlingConstants.TOPIC_RESOURCE_ADDED,
                SlingConstants.TOPIC_RESOURCE_CHANGED,
                SlingConstants.TOPIC_RESOURCE_REMOVED };
        properties.put(EventConstants.EVENT_TOPIC, topics);
        String path = getIdMapPath();
        if (path.endsWith("/")) {
            path = path.substring(0, path.length()-1);
        }
        properties.put(EventConstants.EVENT_FILTER, "(&(path="+path+"))");
        eventHandlerRegistration = bundleContext.registerService(
                EventHandler.class.getName(), this, properties);
    }

    /** Get or create a ResourceResolver **/
    private ResourceResolver getResourceResolver() throws LoginException {
        return resourceResolverFactory.getAdministrativeResourceResolver(null);
    }
    
    public synchronized long getMyId() {
        if (!initialized) {
            return -1;
        }
        return me;
    }
    
    /** for testing only **/
    public synchronized boolean waitForInit(long timeout) {
        long start = System.currentTimeMillis();
        while(!initialized && timeout != 0) {
            try {
                if (timeout>0) {
                    long diff = (start+timeout) - System.currentTimeMillis();
                    if (diff<=0) {
                        return false;
                    }
                    wait(diff);
                } else {
                    wait();
                }
            } catch (InterruptedException e) {
                // ignore
            }
        }
        return initialized;
    }
    
    public synchronized boolean isInitialized() {
        return initialized;
    }

    private synchronized boolean init() throws LoginException, JSONException, PersistenceException {
        if (initialized) {
            return true;
        }
        slingId = settingsService.getSlingId();
        ResourceResolver resourceResolver = null;
        try{
            resourceResolver = getResourceResolver();
            DiscoveryLiteDescriptor descriptor = 
                    DiscoveryLiteDescriptor.getDescriptorFrom(resourceResolver);
            long me = descriptor.getMyId();
            final Resource resource = ResourceHelper.getOrCreateResource(resourceResolver, getIdMapPath());
            ModifiableValueMap idmap = resource.adaptTo(ModifiableValueMap.class);
            // check to see if either my slingId is already mapped to another clusterNodeId
            // or when my clusterNodeId is already mapped to another slingId
            // in both cases: clean that up
            boolean foundMe = false;
            for (String aKey : new HashSet<String>(idmap.keySet())) {
                Object value = idmap.get(aKey);
                if (value instanceof Number) {
                    Number n = (Number)value;
                    if (n.longValue()==me) {
                        // my clusterNodeId is already mapped to
                        // let's check if the key is my slingId
                        if (aKey.equals(slingId)) {
                            // perfect
                            foundMe = true;
                        } else {
                            // cleanup necessary
                            logger.info("init: my clusterNodeId is already mapped to by another slingId, deleting entry: key="+aKey+" mapped to "+value);
                            idmap.remove(aKey);
                        }
                    } else if (aKey.equals(slingId)) {
                        // cleanup necessary
                        logger.info("init: my slingId is already mapped to by another clusterNodeId, deleting entry: key="+aKey+" mapped to "+value);
                        idmap.remove(aKey);
                    } else {
                        // that's just some other slingId-clusterNodeId mapping
                        // leave it unchanged
                    }
                }
            }
            if (!foundMe) {
                logger.info("init: added the following mapping: slingId="+slingId+" to discovery-lite id="+me);
                idmap.put(slingId, me);
            } else {
                logger.info("init: mapping already existed, left unchanged: slingId="+slingId+" to discovery-lite id="+me);
            }
            resourceResolver.commit();
            this.me = me;
            initialized = true;
            notifyAll();
            return true;
        } catch(Exception e) {
            logger.info("init: init failed: "+e);
            return false;
        } finally {
            if (resourceResolver!=null) {
                resourceResolver.close();
            }
        }
        
    }
    
    public synchronized void clearCache() {
        if (!idMapCache.isEmpty()) {
            logger.debug("clearCache: clearing idmap cache");
            oldIdMapCache.clear();
            oldIdMapCache.putAll(idMapCache);
            idMapCache.clear();
        } else {
            logger.debug("clearCache: cache was already emtpy");
        }
        lastCacheInvalidation = System.currentTimeMillis();
    }

    public synchronized String toSlingId(int clusterNodeId, ResourceResolver resourceResolver) throws PersistenceException {
        if (System.currentTimeMillis() - lastCacheInvalidation > 30000) {
            // since upon a restart of an instance it could opt to have changed
            // the slingId, we might not be able to catch that change if we
            // noticed the view change before that (the view change would
            // force a cache invalidation).
            // we can either rely on observation - or combine that with
            // an invalidation of once per minute
            // (note that this means we'll be reading 
            // /var/discovery/oak/idMap once per minute - but that sounds
            // perfectly fine)
            clearCache();
        }
        String slingId = idMapCache.get(clusterNodeId);
        if (slingId!=null) {
            // cache-hit
            return slingId;
        }
        // cache-miss
        logger.debug("toSlingId: cache miss, refreshing idmap cache");
        Map<Integer, String> readMap = readIdMap(resourceResolver);
        Set<Entry<Integer, String>> newEntries = readMap.entrySet();
        for (Entry<Integer, String> newEntry : newEntries) {
            String oldValue = oldIdMapCache.get(newEntry.getKey());
            if (oldValue == null || !oldValue.equals(newEntry.getValue())) {
                logger.info("toSlingId: mapping for "+newEntry.getKey()+" to "+newEntry.getValue() + " was newly added.");
            } else if (!oldValue.equals(newEntry.getValue())) {
                logger.info("toSlingId: mapping for "+newEntry.getKey()+" changed from "+oldValue+" to "+newEntry.getValue());
            }
            idMapCache.put(newEntry.getKey(), newEntry.getValue());
        }
        Set<Entry<Integer, String>> oldEntries = oldIdMapCache.entrySet();
        for (Entry<Integer, String> oldEntry : oldEntries) {
            if (!idMapCache.containsKey(oldEntry.getKey())) {
                logger.info("toSlingId: mapping for "+oldEntry.getKey()+" to "+oldEntry.getValue()+" disappeared.");
            }
        }
        
        return idMapCache.get(clusterNodeId);
    }
    
    private Map<Integer, String> readIdMap(ResourceResolver resourceResolver) throws PersistenceException {
        Resource resource = ResourceHelper.getOrCreateResource(resourceResolver, getIdMapPath());
        ValueMap idmapValueMap = resource.adaptTo(ValueMap.class);
        Map<Integer, String> idmap = new HashMap<Integer, String>();
        for (String slingId : idmapValueMap.keySet()) {
            Object value = idmapValueMap.get(slingId);
            if (value instanceof Number) {
                Number number = (Number)value;
                idmap.put(number.intValue(), slingId);
            }
        }
        return idmap;
    }

    private String getIdMapPath() {
        return commonsConfig.getIdMapPath();
    }

    @Override
    public void handleEvent(Event event) {
        final String resourcePath = (String) event.getProperty("path");
        if (resourcePath == null) {
            // not of my business
            return;
        }
        
        if (!resourcePath.startsWith(getIdMapPath())) {
            // not of my business
            return;
        }
        logger.debug("handleEvent: got event for path: {}, event: {}", resourcePath, event);
        clearCache();
    }

}