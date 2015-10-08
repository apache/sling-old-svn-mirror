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
package org.apache.sling.discovery.commons.providers.spi.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.Session;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.discovery.ClusterView;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.commons.providers.BaseTopologyView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Inherits the 'sync-token' part from the SyncTokenConsistencyService
 * and adds the 'wait while backlog' part to it, based on
 * the Oak discovery-lite descriptor.
 */
public class OakSyncTokenConsistencyService extends SyncTokenConsistencyService {

    private static final String IDMAP_PATH = "/var/discovery/commons/idmap";

    static enum BacklogStatus {
        UNDEFINED /* when there was an error retrieving the backlog status with oak */,
        HAS_BACKLOG /* when oak's discovery lite descriptor indicated that there is still some backlog */,
        NO_BACKLOG /* when oak's discovery lite descriptor declared we're backlog-free now */
    }
    
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /** TODO: avoid hardcoding the constant here but use an Oak constant class instead if possible */
    public static final String OAK_DISCOVERYLITE_CLUSTERVIEW = "oak.discoverylite.clusterview";

    private boolean initialized = false;

    private final long waitWhileBacklogTimeoutMillis;
    
    /**
     * 
     * @param resourceResolverFactory
     * @param slingId the local slingId
     * @param syncTokenTimeoutMillis timeout value in millis after which the
     * sync-token process is cancelled - or -1 if no timeout should be used there
     * @param waitWhileBacklogTimeoutMillis timeout value in millis after which
     * the waiting-while-backlog should be cancelled - or -1 if no timeout should be 
     * used there
     * @throws LoginException when the login for initialization failed
     * @throws JSONException when the descriptor wasn't proper json at init time
     */
    public OakSyncTokenConsistencyService(ResourceResolverFactory resourceResolverFactory,
            String slingId, long syncTokenTimeoutMillis, long waitWhileBacklogTimeoutMillis) {
        super(resourceResolverFactory, slingId, syncTokenTimeoutMillis);
        this.waitWhileBacklogTimeoutMillis = waitWhileBacklogTimeoutMillis;
        startBackgroundCheck("idmap-initializer", new BackgroundCheck() {
            
            @Override
            public boolean check() {
                return ensureInitialized();
            }
        }, null, -1);
    }
    
    private boolean ensureInitialized() {
        if (initialized) {
            return true;
        }
        logger.info("ensureInitialized: initializing.");
        try {
            initialized = init();
            return initialized;
        } catch (LoginException e) {
            logger.error("ensureInitialized: could not login: "+e, e);
            return false;
        } catch (JSONException e) {
            logger.error("ensureInitialized: got JSONException: "+e, e);
            return false;
        } catch (PersistenceException e) {
            logger.error("ensureInitialized: got PersistenceException: "+e, e);
            return false;
        }
    }
    
    private boolean init() throws LoginException, JSONException, PersistenceException {
        ResourceResolver resourceResolver = null;
        try{
            resourceResolver = getResourceResolver();
            JSONObject descriptor = getDescriptor(resourceResolver);
            if (descriptor == null) {
                logger.info("init: could not yet get descriptor '"+OAK_DISCOVERYLITE_CLUSTERVIEW+"'!");
                return false;
            }
            Object meObj = descriptor.get("me");
            if (meObj == null || !(meObj instanceof Number)) {
                logger.info("init: 'me' value of descriptor not a Number: "+meObj+" (descriptor: "+descriptor+")");
                return false;
            }
            Number me = (Number)meObj;
            final Resource resource = getOrCreateResource(resourceResolver, IDMAP_PATH);
            ModifiableValueMap idmap = resource.adaptTo(ModifiableValueMap.class);
            idmap.put(slingId, me.longValue());
            resourceResolver.commit();
            logger.info("init: mapped slingId="+slingId+" to discoveryLiteId="+me);
            return true;
        } finally {
            if (resourceResolver!=null) {
                resourceResolver.close();
            }
        }
        
    }
    
    @Override
    public void sync(final BaseTopologyView view, final Runnable callback) {
        // cancel the previous backgroundCheck if it's still running
        cancelPreviousBackgroundCheck();

        // first do the wait-for-backlog part
        logger.info("sync: doing wait-for-backlog part for view="+view);
        waitWhileBacklog(view, new Runnable() {

            @Override
            public void run() {
                // when done, then do the sync-token part
                logger.info("sync: doing sync-token part for view="+view);
                syncToken(view, callback);
            }
            
        });
    }

    private void waitWhileBacklog(final BaseTopologyView view, final Runnable runnable) {
        // start backgroundChecking until the backlogStatus 
        // is NO_BACKLOG
        startBackgroundCheck("OakSyncTokenConsistencyService-waitWhileBacklog", new BackgroundCheck() {
            
            @Override
            public boolean check() {
                if (!ensureInitialized()) {
                    logger.info("waitWhileBacklog: could not initialize...");
                    return false;
                }
                BacklogStatus backlogStatus = getBacklogStatus(view);
                if (backlogStatus == BacklogStatus.NO_BACKLOG) {
                    logger.info("waitWhileBacklog: no backlog (anymore), done.");
                    return true;
                } else {
                    logger.info("waitWhileBacklog: backlogStatus still "+backlogStatus);
                    return false;
                }
            }
        }, runnable, waitWhileBacklogTimeoutMillis);
    }
    
    private BacklogStatus getBacklogStatus(BaseTopologyView view) {
        logger.trace("getBacklogStatus: start");
        ResourceResolver resourceResolver = null;
        try{
            resourceResolver = getResourceResolver();
            JSONObject descriptor = getDescriptor(resourceResolver);
            if (descriptor == null) {
                logger.warn("getBacklogStatus: could not get descriptor '"+OAK_DISCOVERYLITE_CLUSTERVIEW+"'!");
                return BacklogStatus.UNDEFINED;
            }
            // backlog-free means:
            // 1) 'deactivating' must be empty 
            //     (otherwise we indeed have a backlog)
            // 2) all active ids of the descriptor must have a mapping to slingIds
            //     (otherwise the init failed or is pending for some instance(s))
            // 3) all 'active' instances must be in the view 
            //     (otherwise discovery lite might not yet consider 
            //     an instance dead but discovery-service does)
            // instead what is fine from a backlog point of view
            // * instances in the view but listed as 'inactive'
            //     (this might be the case for just-started instances)
            // * instances in the view but not contained in the descriptor at all
            //     (this might be the case for just-started instances)
            
            Object activeObj = descriptor.get("active");
            JSONArray active = (JSONArray) activeObj;
            Object deactivatingObj = descriptor.get("deactivating");
            JSONArray deactivating = (JSONArray) deactivatingObj;
            // we're not worried about 'inactive' ones - as that could
            // be a larger list filled with legacy entries too
            // plus once the instance is inactive there's no need to 
            // check anything further - that one is then backlog-free
            
            // 1) 'deactivating' must be empty 
            if (deactivating.length()!=0) {
                logger.info("getBacklogStatus: there are deactivating instances: "+deactivating);
                return BacklogStatus.HAS_BACKLOG;
            }

            Resource resource = getOrCreateResource(resourceResolver, IDMAP_PATH);
            ValueMap idmapValueMap = resource.adaptTo(ValueMap.class);
            ClusterView cluster = view.getLocalInstance().getClusterView();
            Set<String> slingIds = new HashSet<String>();
            for (InstanceDescription instance : cluster.getInstances()) {
                slingIds.add(instance.getSlingId());
            }
            Map<Long, String> idmap = new HashMap<Long, String>();
            for (String slingId : idmapValueMap.keySet()) {
                Object value = idmapValueMap.get(slingId);
                if (value instanceof Number) {
                    Number number = (Number)value;
                    idmap.put(number.longValue(), slingId);
                }
            }
            
            for(int i=0; i<active.length(); i++) {
                Number activeId = (Number) active.get(i);
                String slingId = idmap.get(activeId.longValue());
                // 2) all ids of the descriptor must have a mapping to slingIds
                if (slingId == null) {
                    logger.info("getBacklogStatus: no slingId found for active id: "+activeId);
                    return BacklogStatus.UNDEFINED;
                }
                // 3) all 'active' instances must be in the view
                if (!slingIds.contains(slingId)) {
                    logger.info("getBacklogStatus: active instance's ("+activeId+") slingId ("+slingId+") not found in cluster ("+cluster+")");
                    return BacklogStatus.HAS_BACKLOG;
                }
            }

            logger.info("getBacklogStatus: no backlog (anymore)");
            return BacklogStatus.NO_BACKLOG;
        } catch (LoginException e) {
            logger.error("getBacklogStatus: could not login: "+e, e);
            return BacklogStatus.UNDEFINED;
        } catch (JSONException e) {
            logger.error("getBacklogStatus: got JSONException: "+e, e);
            return BacklogStatus.UNDEFINED;
        } catch (PersistenceException e) {
            logger.error("getBacklogStatus: got PersistenceException: "+e, e);
            return BacklogStatus.UNDEFINED;
        } finally {
            logger.trace("getBacklogStatus: end");
            if (resourceResolver!=null) {
                resourceResolver.close();
            }
        }
    }
    
    /**
     * {"seq":8,"final":true,"id":"aae34e9a-b08d-409e-be10-9ff4106e5387","me":4,"active":[4],"deactivating":[],"inactive":[1,2,3]}
     */
    private JSONObject getDescriptor(ResourceResolver resourceResolver) throws JSONException {
        Session session = resourceResolver.adaptTo(Session.class);
        if (session == null) {
            return null;
        }
        String descriptorStr = session.getRepository().getDescriptor(OAK_DISCOVERYLITE_CLUSTERVIEW);
        if (descriptorStr == null) {
            return null;
        }
        JSONObject descriptor = new JSONObject(descriptorStr);
        return descriptor;
    }

}
