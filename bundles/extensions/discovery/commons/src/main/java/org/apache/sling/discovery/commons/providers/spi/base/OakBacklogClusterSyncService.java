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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.discovery.ClusterView;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.commons.providers.BaseTopologyView;
import org.apache.sling.discovery.commons.providers.spi.ClusterSyncService;
import org.apache.sling.settings.SlingSettingsService;

/**
 * The OakBacklogClusterSyncService will wait until all instances
 * in the local cluster are no longer in any backlog state.
 */
@Component(immediate = false)
@Service(value = { ClusterSyncService.class, OakBacklogClusterSyncService.class })
public class OakBacklogClusterSyncService extends AbstractServiceWithBackgroundCheck implements ClusterSyncService {

    static enum BacklogStatus {
        UNDEFINED /* when there was an error retrieving the backlog status with oak */,
        HAS_BACKLOG /* when oak's discovery lite descriptor indicated that there is still some backlog */,
        NO_BACKLOG /* when oak's discovery lite descriptor declared we're backlog-free now */
    }
    
    @Reference
    private IdMapService idMapService;
    
    @Reference
    protected DiscoveryLiteConfig commonsConfig;

    @Reference
    protected ResourceResolverFactory resourceResolverFactory;

    @Reference
    protected SlingSettingsService settingsService;

    private ClusterSyncHistory consistencyHistory = new ClusterSyncHistory();
    
    public static OakBacklogClusterSyncService testConstructorAndActivate(
            final DiscoveryLiteConfig commonsConfig,
            final IdMapService idMapService,
            final SlingSettingsService settingsService,
            ResourceResolverFactory resourceResolverFactory) {
        OakBacklogClusterSyncService service = testConstructor(commonsConfig, idMapService, settingsService, resourceResolverFactory);
        service.activate();
        return service;
    }
    
    /**
     * for testing only!
     * @param resourceResolverFactory
     * @param slingId the local slingId
     * @param syncTokenTimeoutMillis timeout value in millis after which the
     * sync-token process is cancelled - or -1 if no timeout should be used there
     * @param backlogWaitTimeoutMillis timeout value in millis after which
     * the waiting-while-backlog should be cancelled - or -1 if no timeout should be 
     * used there
     * @throws LoginException when the login for initialization failed
     * @throws JSONException when the descriptor wasn't proper json at init time
     */
    public static OakBacklogClusterSyncService testConstructor(
            final DiscoveryLiteConfig commonsConfig,
            final IdMapService idMapService,
            final SlingSettingsService settingsService,
            ResourceResolverFactory resourceResolverFactory) {
        OakBacklogClusterSyncService service = new OakBacklogClusterSyncService();
        if (commonsConfig == null) {
            throw new IllegalArgumentException("commonsConfig must not be null");
        }
        if (resourceResolverFactory == null) {
            throw new IllegalArgumentException("resourceResolverFactory must not be null");
        }
        if (settingsService == null) {
            throw new IllegalArgumentException("settingsService must not be null");
        }
        service.commonsConfig = commonsConfig;
        service.resourceResolverFactory = resourceResolverFactory;
        service.idMapService = idMapService;
        service.settingsService = settingsService;
        return service;
    }
    
    @Activate
    protected void activate() {
        this.slingId = getSettingsService().getSlingId();
        logger.info("activate: activated with slingId="+slingId);
    }
    
    public void setConsistencyHistory(ClusterSyncHistory consistencyHistory) {
        this.consistencyHistory = consistencyHistory;
    }
    
    public ClusterSyncHistory getConsistencyHistory() {
        return consistencyHistory;
    }
    
    /** Get or create a ResourceResolver **/
    protected ResourceResolver getResourceResolver() throws LoginException {
        return resourceResolverFactory.getAdministrativeResourceResolver(null);
    }
    
    @Override
    public void cancelSync() {
        cancelPreviousBackgroundCheck();
    }

    @Override
    public void sync(final BaseTopologyView view, final Runnable callback) {
        // cancel the previous backgroundCheck if it's still running
        cancelPreviousBackgroundCheck();

        // first do the wait-for-backlog part
        logger.info("sync: doing wait-for-backlog part for view="+view.toShortString());
        waitWhileBacklog(view, callback);
    }

    private void waitWhileBacklog(final BaseTopologyView view, final Runnable runnable) {
        // start backgroundChecking until the backlogStatus 
        // is NO_BACKLOG
        startBackgroundCheck("OakBacklogClusterSyncService-backlog-waiting", new BackgroundCheck() {
            
            @Override
            public boolean check() {
                try {
                    if (!idMapService.isInitialized()) {
                        logger.info("waitWhileBacklog: could not initialize...");
                        consistencyHistory.addHistoryEntry(view, "could not initialize idMapService");
                        return false;
                    }
                } catch (Exception e) {
                    logger.error("waitWhileBacklog: could not initialized due to "+e, e);
                    consistencyHistory.addHistoryEntry(view, "got Exception while initializing idMapService ("+e+")");
                    return false;
                }
                BacklogStatus backlogStatus = getBacklogStatus(view);
                if (backlogStatus == BacklogStatus.NO_BACKLOG) {
                    logger.info("waitWhileBacklog: no backlog (anymore), done.");
                    consistencyHistory.addHistoryEntry(view, "no backlog (anymore)");
                    return true;
                } else {
                    logger.info("waitWhileBacklog: backlogStatus still "+backlogStatus);
                    // clear the cache to make sure to get the latest version in case something changed
                    idMapService.clearCache();
                    consistencyHistory.addHistoryEntry(view, "backlog status "+backlogStatus);
                    return false;
                }
            }
        }, runnable, getCommonsConfig().getClusterSyncServiceTimeoutMillis(), getCommonsConfig().getClusterSyncServiceIntervalMillis());
    }
    
    private BacklogStatus getBacklogStatus(BaseTopologyView view) {
        logger.trace("getBacklogStatus: start");
        ResourceResolver resourceResolver = null;
        try{
            resourceResolver = getResourceResolver();
            DiscoveryLiteDescriptor descriptor = 
                    DiscoveryLiteDescriptor.getDescriptorFrom(resourceResolver);

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
            
            int[] activeIds = descriptor.getActiveIds();
            int[] deactivatingIds = descriptor.getDeactivatingIds();
            // we're not worried about 'inactive' ones - as that could
            // be a larger list filled with legacy entries too
            // plus once the instance is inactive there's no need to 
            // check anything further - that one is then backlog-free
            
            // 1) 'deactivating' must be empty 
            if (deactivatingIds.length!=0) {
                logger.info("getBacklogStatus: there are deactivating instances: "+Arrays.toString(deactivatingIds));
                return BacklogStatus.HAS_BACKLOG;
            }

            ClusterView cluster = view.getLocalInstance().getClusterView();
            Set<String> slingIds = new HashSet<String>();
            for (InstanceDescription instance : cluster.getInstances()) {
                slingIds.add(instance.getSlingId());
            }
            
            for(int i=0; i<activeIds.length; i++) {
                int activeId = activeIds[i];
                String slingId = idMapService.toSlingId(activeId, resourceResolver);
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
        } catch(Exception e) {
            logger.info("getBacklogStatus: failed to determine backlog status: "+e);
            return BacklogStatus.UNDEFINED;
        } finally {
            logger.trace("getBacklogStatus: end");
            if (resourceResolver!=null) {
                resourceResolver.close();
            }
        }
    }

    protected DiscoveryLiteConfig getCommonsConfig() {
        return commonsConfig;
    }

    protected SlingSettingsService getSettingsService() {
        return settingsService;
    }

    public List<String> getSyncHistory() {
        return consistencyHistory.getSyncHistory();
    }

}
