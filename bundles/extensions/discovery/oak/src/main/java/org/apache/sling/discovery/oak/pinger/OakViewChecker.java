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
package org.apache.sling.discovery.oak.pinger;

import java.util.Calendar;
import java.util.UUID;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.discovery.base.commons.BaseViewChecker;
import org.apache.sling.discovery.base.commons.PeriodicBackgroundJob;
import org.apache.sling.discovery.base.connectors.BaseConfig;
import org.apache.sling.discovery.base.connectors.announcement.AnnouncementRegistry;
import org.apache.sling.discovery.base.connectors.ping.ConnectorRegistry;
import org.apache.sling.discovery.commons.providers.util.ResourceHelper;
import org.apache.sling.discovery.oak.Config;
import org.apache.sling.discovery.oak.OakDiscoveryService;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.service.http.HttpService;

/**
 * The OakViewChecker is taking care of checking the oak discovery-lite
 * descriptor when checking the local cluster view and passing that
 * on to the ViewStateManager which will then detect whether there was 
 * any change or not. Unlike discovery.impl's HeartbeatHandler this one
 * does not store any heartbeats in the repository anymore.
 * <p>
 * Remote heartbeats are POSTs to remote TopologyConnectorServlets using
 * discovery.base
 */
@Component
@Service(value = OakViewChecker.class)
@Reference(referenceInterface=HttpService.class,
           cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE,
           policy=ReferencePolicy.DYNAMIC)
public class OakViewChecker extends BaseViewChecker {

    @Reference
    protected SlingSettingsService slingSettingsService;

    @Reference
    protected ResourceResolverFactory resourceResolverFactory;

    @Reference
    protected ConnectorRegistry connectorRegistry;

    @Reference
    protected AnnouncementRegistry announcementRegistry;

    @Reference
    protected Scheduler scheduler;

    @Reference
    private Config config;

    private OakDiscoveryService discoveryService;

    protected PeriodicBackgroundJob periodicCheckViewJob;

    /** for testing only **/
    public static OakViewChecker testConstructor(
            SlingSettingsService slingSettingsService,
            ResourceResolverFactory resourceResolverFactory,
            ConnectorRegistry connectorRegistry,
            AnnouncementRegistry announcementRegistry,
            Scheduler scheduler,
            Config config) {
        OakViewChecker pinger = new OakViewChecker();
        pinger.slingSettingsService = slingSettingsService;
        pinger.resourceResolverFactory = resourceResolverFactory;
        pinger.connectorRegistry = connectorRegistry;
        pinger.announcementRegistry = announcementRegistry;
        pinger.scheduler = scheduler;
        pinger.config = config;
        return pinger;
    }

    @Override
    protected AnnouncementRegistry getAnnouncementRegistry() {
        return announcementRegistry;
    }
    
    @Override
    protected BaseConfig getConnectorConfig() {
        return config;
    }
    
    @Override
    protected ConnectorRegistry getConnectorRegistry() {
        return connectorRegistry;
    }
    
    @Override
    protected ResourceResolverFactory getResourceResolverFactory() {
        return resourceResolverFactory;
    }
    
    @Override
    protected Scheduler getScheduler() {
        return scheduler;
    }
    
    @Override
    protected SlingSettingsService getSlingSettingsService() {
        return slingSettingsService;
    }
    
    @Override
    protected void doActivate() {
        // on activate the resetLeaderElectionId is set to true to ensure that
        // the 'leaderElectionId' property is reset on next heartbeat issuance.
        // the idea being that a node which leaves the cluster should not
        // become leader on next join - and by resetting the leaderElectionId
        // to the current time, this is ensured.
        runtimeId = UUID.randomUUID().toString();

        logger.info("doActivate: activated with runtimeId: {}, slingId: {}", runtimeId, slingId);
        
        resetLeaderElectionId();
    }
    
    @Override
    protected void deactivate() {
        super.deactivate();
        if (periodicCheckViewJob != null) {
            periodicCheckViewJob.stop();
            periodicCheckViewJob = null;
        }
    }

    /**
     * The initialize method is called by the OakDiscoveryService.activate
     * as we require the discoveryService (and the discoveryService has
     * a reference on us - but we cant have circular references in osgi).
     */
    public void initialize(final OakDiscoveryService discoveryService) {
        logger.info("initialize: initializing.");
        synchronized(lock) {
        	this.discoveryService = discoveryService;
            issueHeartbeat();
        }

        // start the (less frequent) periodic job that does the
        // connector pings and checks the connector/topology view
        try {
            final long interval = config.getConnectorPingInterval();
            logger.info("initialize: starting periodic connectorPing job for "+slingId+" with interval "+interval+" sec.");
            periodicPingJob = new PeriodicBackgroundJob(interval, NAME+".connectorPinger", this);
        } catch (Exception e) {
            logger.error("activate: Could not start heartbeat runner: " + e, e);
        }
        
        // start the (more frequent) periodic job that checks
        // the discoveryLite descriptor - that can be more frequent
        // since it is only reading an oak repository descriptor
        // which is designed to be read very frequently (it caches
        // the value and only updates it on change, so reading is very cheap)
        // and because doing this more frequently means that the
        // reaction time is faster
        try{
            final long interval = config.getDiscoveryLiteCheckInterval();
            logger.info("initialize: starting periodic discoveryLiteCheck job for "+slingId+" with interval "+interval+" sec.");
            periodicCheckViewJob = new PeriodicBackgroundJob(interval, NAME+".discoveryLiteCheck", new Runnable() {

                @Override
                public void run() {
                    discoveryLiteCheck();
                }
                
            });
        } catch (Exception e) {
            logger.error("activate: Could not start heartbeat runner: " + e, e);
        }
    }
    
    private void discoveryLiteCheck() {
        logger.debug("discoveryLiteCheck: start. [for slingId="+slingId+"]");
        synchronized(lock) {
            if (!activated) {
                // SLING:2895: avoid checks if not activated
                logger.debug("discoveryLiteCheck: not activated yet");
                return;
            }

            // check the view
            // discovery.oak relies on oak's discovery-lite descriptor
            // to be updated independently in case of cluster view change.
            // all that we can therefore do here is assume something
            // might have changed and let discoveryService/viewStateManager
            // filter out the 99.99% of unchanged cases.
            discoveryService.checkForTopologyChange();
        }
        logger.debug("discoveryLiteCheck: end. [for slingId="+slingId+"]");
    }

    /** Get or create a ResourceResolver **/
    private ResourceResolver getResourceResolver() throws LoginException {
        if (resourceResolverFactory == null) {
            logger.error("getResourceResolver: resourceResolverFactory is null!");
            return null;
        }
        return resourceResolverFactory.getAdministrativeResourceResolver(null);
    }

    /** Calcualte the local cluster instance path **/
    private String getLocalClusterNodePath() {
        return config.getClusterInstancesPath() + "/" + slingId;
    }

    /**
     * Hook that will cause a reset of the leaderElectionId 
     * on next invocation of issueClusterLocalHeartbeat.
     * @return true if the leaderElectionId was reset - false if that was not
     * necessary as that happened earlier already and it has not propagated
     * yet to the ./clusterInstances in the meantime
     */
    public boolean resetLeaderElectionId() {
        ResourceResolver resourceResolver = null;
        try{
            final String myClusterNodePath = getLocalClusterNodePath();
            resourceResolver = getResourceResolver();
            if (resourceResolver==null) {
                logger.warn("resetLeaderElectionId: could not login, new leaderElectionId will be calculated upon next heartbeat only!");
                return false;
            }
            String newLeaderElectionId = newLeaderElectionId();

            final Resource resource = ResourceHelper.getOrCreateResource(
                    resourceResolver, myClusterNodePath);
            final ModifiableValueMap resourceMap = resource.adaptTo(ModifiableValueMap.class);

            resourceMap.put(PROPERTY_ID_RUNTIME, runtimeId);
            // SLING-4765 : store more infos to be able to be more verbose on duplicate slingId/ghost detection
            String slingHomePath = "n/a";
            if (slingSettingsService != null && slingSettingsService.getSlingHomePath() != null) {
                slingHomePath = slingSettingsService.getSlingHomePath();
            }
            resourceMap.put(PROPERTY_ID_SLING_HOME_PATH, slingHomePath);
            final String endpointsAsString = getEndpointsAsString();
            resourceMap.put(PROPERTY_ID_ENDPOINTS, endpointsAsString);

            Calendar leaderElectionCreatedAt = Calendar.getInstance();
            resourceMap.put("leaderElectionId", newLeaderElectionId);
            resourceMap.put("leaderElectionIdCreatedAt", leaderElectionCreatedAt);

            logger.info("resetLeaderElectionId: storing my runtimeId: {}, endpoints: {}, sling home path: {}, new leaderElectionId: {}, created at: {}", 
                    new Object[]{runtimeId, endpointsAsString, slingHomePath, newLeaderElectionId, leaderElectionCreatedAt});
            resourceResolver.commit();
        } catch (LoginException e) {
            logger.error("resetLeaderElectionid: could not login: "+e, e);
        } catch (PersistenceException e) {
            logger.error("resetLeaderElectionid: got PersistenceException: "+e, e);
        } finally {
            if (resourceResolver!=null) {
                resourceResolver.close();
            }
        }
        return true;
    }

    /**
     * Calculate a new leaderElectionId based on the current config and system time
     */
    private String newLeaderElectionId() {
        int maxLongLength = String.valueOf(Long.MAX_VALUE).length();
        String currentTimeMillisStr = String.format("%0"
                + maxLongLength + "d", System.currentTimeMillis());

        String prefix = "1";

        final String newLeaderElectionId = prefix + "_"
                + currentTimeMillisStr + "_" + slingId;
        return newLeaderElectionId;
    }

    @Override
    protected void doCheckView() {
        super.doCheckView();

        // discovery.oak relies on oak's discovery-lite descriptor
        // to be updated independently in case of cluster view change.
        // all that we can therefore do here is assume something
        // might have changed and let discoveryService/viewStateManager
        // filter out the 99.99% of unchanged cases.
        discoveryService.checkForTopologyChange();
    }

    protected void updateProperties() {
        if (discoveryService == null) {
            logger.error("issueHeartbeat: discoveryService is null");
        } else {
            discoveryService.updateProperties();
        }
    }}
