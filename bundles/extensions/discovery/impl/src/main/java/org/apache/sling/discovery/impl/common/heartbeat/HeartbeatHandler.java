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
package org.apache.sling.discovery.impl.common.heartbeat;

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.jcr.Session;

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
import org.apache.sling.discovery.impl.Config;
import org.apache.sling.discovery.impl.DiscoveryServiceImpl;
import org.apache.sling.discovery.impl.cluster.voting.VotingHandler;
import org.apache.sling.discovery.impl.cluster.voting.VotingHelper;
import org.apache.sling.discovery.impl.cluster.voting.VotingView;
import org.apache.sling.discovery.impl.common.View;
import org.apache.sling.discovery.impl.common.ViewHelper;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.framework.BundleException;
import org.osgi.service.http.HttpService;

/**
 * The heartbeat handler is responsible and capable of issuing both local and
 * remote heartbeats and registers a periodic job with the scheduler for doing so.
 * <p>
 * Local heartbeats are stored in the repository. Remote heartbeats are POSTs to
 * remote TopologyConnectorServlets.
 */
@Component
@Service(value = HeartbeatHandler.class)
@Reference(referenceInterface=HttpService.class,
           cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE,
           policy=ReferencePolicy.DYNAMIC)
public class HeartbeatHandler extends BaseViewChecker {

    private static final String PROPERTY_ID_LAST_HEARTBEAT = "lastHeartbeat";

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

    @Reference
    private VotingHandler votingHandler;

    /** the id which is to be used for the next voting **/
    private String nextVotingId = UUID.randomUUID().toString();

    /** whether or not to reset the leaderElectionId at next heartbeat time **/
    private volatile boolean resetLeaderElectionId = false;

    /** SLING-5030 : upon resetLeaderElectionId() a newLeaderElectionId is calculated 
     * and passed on to the VotingHandler - but the actual storing under ./clusterInstances
     * is only done on heartbeats - this field is used to temporarily store the new
     * leaderElectionId that the next heartbeat then stores
     */
    private volatile String newLeaderElectionId;
    
    /** SLING-2892: remember first heartbeat written to repository by this instance **/
    private long firstHeartbeatWritten = -1;

    /** SLING-2892: remember the value of the heartbeat this instance has written the last time **/
    private volatile Calendar lastHeartbeatWritten = null;

    private DiscoveryServiceImpl discoveryServiceImpl;

    private String lastEstablishedViewId;

    protected String failedEstablishedViewId;

    protected PeriodicBackgroundJob periodicCheckJob;

    /** for testing only **/
    public static HeartbeatHandler testConstructor(
            SlingSettingsService slingSettingsService,
            ResourceResolverFactory factory, 
            AnnouncementRegistry announcementRegistry, 
            ConnectorRegistry connectorRegistry,
            Config config, 
            Scheduler scheduler,
            VotingHandler votingHandler) {
        HeartbeatHandler handler = new HeartbeatHandler();
        handler.slingSettingsService = slingSettingsService;
        handler.resourceResolverFactory = factory;
        handler.announcementRegistry = announcementRegistry;
        handler.connectorRegistry = connectorRegistry;
        handler.config = config;
        handler.scheduler = scheduler;
        handler.votingHandler = votingHandler;
        return handler;
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
        resetLeaderElectionId = true;
        runtimeId = UUID.randomUUID().toString();

        // SLING-2895: reset variables to avoid unnecessary log.error
        firstHeartbeatWritten = -1;
        lastHeartbeatWritten = null;

        logger.info("doActivate: activated with runtimeId: {}, slingId: {}", runtimeId, slingId);
    }
    
    @Override
    protected void deactivate() {
        super.deactivate();
        if (periodicCheckJob != null) {
            periodicCheckJob.stop();
            periodicCheckJob = null;
        }
    }
    
    /**
     * The initialize method is called by the DiscoveryServiceImpl.activate
     * as we require the discoveryService (and the discoveryService has
     * a reference on us - but we cant have circular references in osgi).
     * <p>
     * The initialVotingId is used to avoid an unnecessary topologyChanged event
     * when starting up an instance in a 1-node cluster: the instance
     * will wait until the first voting has been finished to send
     * the TOPOLOGY_INIT event - BUT even before that the API method
     * getTopology() is open - so if anyone asks for the topology
     * BEFORE the first voting in a 1-node cluster is done, it gets
     * a particular clusterId - that one we aim to reuse for the first
     * voting.
     */
    public void initialize(final DiscoveryServiceImpl discoveryService,
            final String initialVotingId) {
        synchronized(lock) {
            this.discoveryServiceImpl = discoveryService;
        	this.nextVotingId = initialVotingId;
        	logger.info("initialize: nextVotingId="+nextVotingId);
            issueHeartbeat();
        }

        try {
            long interval = config.getHeartbeatInterval();
            logger.info("initialize: starting periodic heartbeat job for "+slingId+" with interval "+interval+" sec.");
            if (interval==0) {
                logger.warn("initialize: Repeat interval cannot be zero. Defaulting to 10sec");
                interval = 10;
            }
            periodicPingJob = new PeriodicBackgroundJob(interval, NAME, this);
        } catch (Exception e) {
            logger.error("activate: Could not start heartbeat runner: " + e, e);
        }

        // SLING-5195 - to account for repository delays, the writing of heartbeats and voting
        // should be done independently of getting the current clusterView and 
        // potentially sending a topology event.
        // so this second part is now done (additionally) in a 2nd runner here:
        try {
            long interval = config.getHeartbeatInterval();
            final long heartbeatTimeoutMillis = config.getHeartbeatTimeoutMillis();
            final long heartbeatIntervalMillis = config.getHeartbeatInterval() * 1000;
            final long maxMillisSinceHb = Math.max(Math.min(heartbeatTimeoutMillis, 2 * heartbeatIntervalMillis),
                    heartbeatTimeoutMillis - 2 * heartbeatIntervalMillis);
            logger.info("initialize: starting periodic checkForLocalClusterViewChange job for "+slingId+" with maxMillisSinceHb=" + maxMillisSinceHb + "ms, interval="+interval+" sec.");
            if (interval==0) {
                logger.warn("initialize: Repeat interval cannot be zero. Defaulting to 10sec.");
                interval = 10;
            }
            periodicCheckJob = new PeriodicBackgroundJob(interval, NAME+".checkForLocalClusterViewChange", new Runnable() {

                @Override
                public void run() {
                    Calendar lastHb = lastHeartbeatWritten;
                    if (lastHb!=null) {
                        // check to see when we last wrote a heartbeat
                        // if it is older than the configured timeout,
                        // then mark ourselves as in topologyChanging automatically
                        final long timeSinceHb = System.currentTimeMillis() - lastHb.getTimeInMillis();
                        // SLING-5285: add a safety-margin for SLING-5195
                        if (timeSinceHb > maxMillisSinceHb) {
                            logger.warn("checkForLocalClusterViewChange/.run: time since local instance last wrote a heartbeat is " + timeSinceHb + "ms"
                                    + " (heartbeatTimeoutMillis=" + heartbeatTimeoutMillis + ", heartbeatIntervalMillis=" + heartbeatIntervalMillis
                                    + " => maxMillisSinceHb=" + maxMillisSinceHb + "). Flagging us as (still) changing");
                            // mark the current establishedView as faulty
                            invalidateCurrentEstablishedView();
                            
                            // then tell the listeners immediately
                            // note that just calling handleTopologyChanging alone - without the above invalidate -
                            // won't be sufficient, because that would only affect the listeners, not the
                            // getTopology() call.
                            discoveryService.handleTopologyChanging();
                            return;
                        }
                    }
                    // SLING-5195: guarantee frequent calls to checkForLocalClusterViewChange,
                    // independently of blocked write/save operations
                    logger.debug("checkForLocalClusterViewChange/.run: going to check for topology change...");
                    discoveryService.checkForLocalClusterViewChange();
                    logger.debug("checkForLocalClusterViewChange/.run: check for topology change done.");
                }
                
            });
        } catch (Exception e) {
            logger.error("activate: Could not start heartbeat runner: " + e, e);
        }
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
        if (resetLeaderElectionId) {
            // then we already have a reset pending
            // resetting twice doesn't work
            return false;
        }
        resetLeaderElectionId = true;
        ResourceResolver resourceResolver = null;
        try{
            resourceResolver = getResourceResolver();
            if (resourceResolver!=null) {
                newLeaderElectionId = newLeaderElectionId(resourceResolver);
                if (votingHandler!=null) {
                    logger.info("resetLeaderElectionId: set new leaderElectionId with votingHandler to: "+newLeaderElectionId);
                    votingHandler.setLeaderElectionId(newLeaderElectionId);
                } else {
                    logger.info("resetLeaderElectionId: no votingHandler, new leaderElectionId would be: "+newLeaderElectionId);
                }
            } else {
                logger.warn("resetLeaderElectionId: could not login, new leaderElectionId will be calculated upon next heartbeat only!");
            }
        } catch (LoginException e) {
            logger.error("resetLeaderElectionid: could not login: "+e, e);
        } finally {
            if (resourceResolver!=null) {
                resourceResolver.close();
            }
        }
        return true;
    }

    /**
     * Issue a heartbeat.
     * <p>
     * This action consists of first updating the local properties,
     * then issuing a cluster-local heartbeat (within the repository)
     * and then a remote heartbeat (to all the topology connectors
     * which announce this part of the topology to others)
     */
    protected void issueHeartbeat() {
        updateProperties();
        issueClusterLocalHeartbeat();
        issueConnectorPings();
    }

    protected void updateProperties() {
        if (discoveryServiceImpl == null) {
            logger.debug("updateProperties: discoveryService is null");
        } else {
            discoveryServiceImpl.updateProperties();
        }
    }
    
    /** Issue a cluster local heartbeat (into the repository) **/
    protected void issueClusterLocalHeartbeat() {
        if (logger.isDebugEnabled()) {
            logger.debug("issueClusterLocalHeartbeat: storing cluster-local heartbeat to repository for "+slingId);
        }
        ResourceResolver resourceResolver = null;
        final String myClusterNodePath = getLocalClusterNodePath();
        final Calendar currentTime = Calendar.getInstance();
        try {
            resourceResolver = getResourceResolver();
            if (resourceResolver == null) {
                logger.error("issueClusterLocalHeartbeat: no resourceresolver available!");
                return;
            }

            final Resource resource = ResourceHelper.getOrCreateResource(
                    resourceResolver, myClusterNodePath);
            final ModifiableValueMap resourceMap = resource.adaptTo(ModifiableValueMap.class);

            if (firstHeartbeatWritten!=-1 && lastHeartbeatWritten!=null) {
            	// SLING-2892: additional paranoia check
            	// after the first heartbeat, check if there's someone else using
            	// the same sling.id in this cluster
            	final long timeSinceFirstHeartbeat =
            			System.currentTimeMillis() - firstHeartbeatWritten;
            	if (timeSinceFirstHeartbeat > 2*config.getHeartbeatInterval()) {
            		// but wait at least 2 heartbeat intervals to handle the situation
            		// where a bundle is refreshed, and startup cases.
            		final Calendar lastHeartbeat = resourceMap.get(PROPERTY_ID_LAST_HEARTBEAT, Calendar.class);
            		if (lastHeartbeat!=null) {
            			// if there is a heartbeat value, check if it is what I've written
            			// the last time
            			if (!lastHeartbeatWritten.getTime().equals(lastHeartbeat.getTime())) {
            				// then we've likely hit the situation where there is another
            				// sling instance accessing the same repository (ie in the same cluster)
            				// using the same sling.id - hence writing to the same
            				// resource
            			    invalidateCurrentEstablishedView();
            			    discoveryServiceImpl.handleTopologyChanging();
            				logger.error("issueClusterLocalHeartbeat: SLING-2892: Detected unexpected, concurrent update of: "+
            						myClusterNodePath+" 'lastHeartbeat'. If not done manually, " +
            						"this likely indicates that there is more than 1 instance running in this cluster" +
            						" with the same sling.id. My sling.id is "+slingId+"." +
            						" Check for sling.id.file in your installation of all instances in this cluster " +
            						"to verify this! Duplicate sling.ids are not allowed within a cluster!");
            			}
            		}
            	}

            	// SLING-2901 : robust paranoia check: on first heartbeat write, the
            	//              'runtimeId' is set as a property (ignoring any former value).
            	//              If in subsequent calls the value of 'runtimeId' changes, then
            	//              there is someone else around with the same slingId.
            	final String readRuntimeId = resourceMap.get(PROPERTY_ID_RUNTIME, String.class);
            	if ( readRuntimeId == null ) { // SLING-3977
            	    // someone deleted the resource property
            	    firstHeartbeatWritten = -1;
            	} else if (!runtimeId.equals(readRuntimeId)) {
            	    invalidateCurrentEstablishedView();
            	    discoveryServiceImpl.handleTopologyChanging();
                    final String slingHomePath = slingSettingsService==null ? "n/a" : slingSettingsService.getSlingHomePath();
                    final String endpointsAsString = getEndpointsAsString();
                    final String readEndpoints = resourceMap.get(PROPERTY_ID_ENDPOINTS, String.class);
                    final String readSlingHomePath = resourceMap.get(PROPERTY_ID_SLING_HOME_PATH, String.class);
            		logger.error("issueClusterLocalHeartbeat: SLING-2901: Detected more than 1 instance running in this cluster " +
            				" with the same sling.id. " +
            				"My sling.id: "+slingId+", my runtimeId: " + runtimeId+", my endpoints: "+endpointsAsString+", my slingHomePath: "+slingHomePath+
            				", other runtimeId: "+readRuntimeId+", other endpoints: "+readEndpoints+", other slingHomePath:"+readSlingHomePath+
    						" Check for sling.id.file in your installation of all instances in this cluster " +
    						"to verify this! Duplicate sling.ids are not allowed within a cluster!");
            		logger.error("issueClusterLocalHeartbeat: sending TOPOLOGY_CHANGING before self-disabling.");
            		discoveryServiceImpl.forcedShutdown();
            		logger.error("issueClusterLocalHeartbeat: disabling discovery.impl");
            		activated = false;
            		if (context!=null) {
            			// disable all components
            			try {
							context.getBundleContext().getBundle().stop();
						} catch (BundleException e) {
							logger.warn("issueClusterLocalHeartbeat: could not stop bundle: "+e, e);
							// then disable all compnoents instead
							context.disableComponent(null);
						}
            		}
            		return;
            	}
            }
            resourceMap.put(PROPERTY_ID_LAST_HEARTBEAT, currentTime);
            if (firstHeartbeatWritten==-1) {
            	resourceMap.put(PROPERTY_ID_RUNTIME, runtimeId);
            	// SLING-4765 : store more infos to be able to be more verbose on duplicate slingId/ghost detection
            	final String slingHomePath = slingSettingsService==null ? "n/a" : slingSettingsService.getSlingHomePath();
                resourceMap.put(PROPERTY_ID_SLING_HOME_PATH, slingHomePath);
            	final String endpointsAsString = getEndpointsAsString();
                resourceMap.put(PROPERTY_ID_ENDPOINTS, endpointsAsString);
            	logger.info("issueClusterLocalHeartbeat: storing my runtimeId: {}, endpoints: {} and sling home path: {}", 
            	        new Object[]{runtimeId, endpointsAsString, slingHomePath});
            }
            if (resetLeaderElectionId || !resourceMap.containsKey("leaderElectionId")) {
                // the new leaderElectionId might have been 'pre set' in the field 'newLeaderElectionId'
                // if that's the case, use that one, otherwise calculate a new one now
                final String newLeaderElectionId = this.newLeaderElectionId!=null ? this.newLeaderElectionId : newLeaderElectionId(resourceResolver);
                this.newLeaderElectionId = null;
                resourceMap.put("leaderElectionId", newLeaderElectionId);
                resourceMap.put("leaderElectionIdCreatedAt", new Date());
                logger.info("issueClusterLocalHeartbeat: set leaderElectionId to "+newLeaderElectionId+" (resetLeaderElectionId: "+resetLeaderElectionId+")");
                if (votingHandler!=null) {
                    votingHandler.setLeaderElectionId(newLeaderElectionId);
                }
                resetLeaderElectionId = false;
            }
            logger.debug("issueClusterLocalHeartbeat: committing cluster-local heartbeat to repository for {}", slingId);
            resourceResolver.commit();
            logger.debug("issueClusterLocalHeartbeat: committed cluster-local heartbeat to repository for {}", slingId);

            // SLING-2892: only in success case: remember the last heartbeat value written
            lastHeartbeatWritten = currentTime;
            // and set the first heartbeat written value - if it is not already set
            if (firstHeartbeatWritten==-1) {
            	firstHeartbeatWritten = System.currentTimeMillis();
            }

        } catch (LoginException e) {
            logger.error("issueHeartbeat: could not log in administratively: "
                    + e, e);
        } catch (PersistenceException e) {
            logger.error("issueHeartbeat: Got a PersistenceException: "
                    + myClusterNodePath + " " + e, e);
        } finally {
            if (resourceResolver != null) {
                resourceResolver.close();
            }
        }
    }

    /**
     * Calculate a new leaderElectionId based on the current config and system time
     */
    private String newLeaderElectionId(ResourceResolver resourceResolver) {
        int maxLongLength = String.valueOf(Long.MAX_VALUE).length();
        String currentTimeMillisStr = String.format("%0"
                + maxLongLength + "d", System.currentTimeMillis());

        final boolean shouldInvertRepositoryDescriptor = config.shouldInvertRepositoryDescriptor();
        String prefix = (shouldInvertRepositoryDescriptor ? "1" : "0");

        String leaderElectionRepositoryDescriptor = config.getLeaderElectionRepositoryDescriptor();
        if (leaderElectionRepositoryDescriptor!=null && leaderElectionRepositoryDescriptor.length()!=0) {
            // when this property is configured, check the value of the repository descriptor
            // and if that value is set, include it in the leader election id

            final Session session = resourceResolver.adaptTo(Session.class);
            if ( session != null ) {
                String value = session.getRepository()
                        .getDescriptor(leaderElectionRepositoryDescriptor);
                if (value != null) {
                    if (value.equalsIgnoreCase("true")) {
                        if (!shouldInvertRepositoryDescriptor) {
                            prefix = "1";
                        } else {
                            prefix = "0";
                        }
                    }
                }
            }
        }
        final String newLeaderElectionId = prefix + "_"
                + currentTimeMillisStr + "_" + slingId;
        return newLeaderElectionId;
    }

    /** Check whether the established view matches the reality, ie matches the
     * heartbeats
     */
    protected void doCheckView() {
        super.doCheckView();

        ResourceResolver resourceResolver = null;
        try {
            resourceResolver = getResourceResolver();
            doCheckViewWith(resourceResolver);
        } catch (LoginException e) {
            logger.error("checkView: could not log in administratively: " + e,
                    e);
        } catch (PersistenceException e) {
            logger.error(
                    "checkView: encountered a persistence exception during view check: "
                            + e, e);
        } catch (RuntimeException e) {
            logger.error(
                    "checkView: encountered a runtime exception during view check: "
                            + e, e);
        } finally {
            if (resourceResolver != null) {
                resourceResolver.close();
            }
        }
    }

    /** do the established-against-heartbeat view check using the given resourceResolver.
     */
    private void doCheckViewWith(final ResourceResolver resourceResolver) throws PersistenceException {

        if (votingHandler==null) {
            logger.info("doCheckViewWith: votingHandler is null! slingId="+slingId);
        } else {
            votingHandler.analyzeVotings(resourceResolver);
            try{
                votingHandler.cleanupTimedoutVotings(resourceResolver);
            } catch(Exception e) {
                logger.warn("doCheckViewWith: Exception occurred while cleaning up votings: "+e, e);
            }
        }

        final VotingView winningVoting = VotingHelper.getWinningVoting(
                resourceResolver, config);
        int numOpenNonWinningVotes = VotingHelper.listOpenNonWinningVotings(
                resourceResolver, config).size();
        if (winningVoting != null || (numOpenNonWinningVotes > 0)) {
            // then there are votings pending and I shall wait for them to
            // settle
            
            // but first: make sure we sent the TOPOLOGY_CHANGING
            logger.info("doCheckViewWith: there are pending votings, marking topology as changing...");
            invalidateCurrentEstablishedView();
            discoveryServiceImpl.handleTopologyChanging();
            
        	if (logger.isDebugEnabled()) {
	            logger.debug("doCheckViewWith: "
	                    + numOpenNonWinningVotes
	                    + " ongoing votings, no one winning yet - I shall wait for them to settle.");
        	}
            return;
        }

        final Resource clusterNodesRes = ResourceHelper.getOrCreateResource(
                resourceResolver, config.getClusterInstancesPath());
        final Set<String> liveInstances = ViewHelper.determineLiveInstances(
                clusterNodesRes, config);

        final View establishedView = ViewHelper.getEstablishedView(resourceResolver, config);
        lastEstablishedViewId = establishedView == null ? null : establishedView.getResource().getName();
        boolean establishedViewMatches;
        if (lastEstablishedViewId != null && failedEstablishedViewId != null
                && lastEstablishedViewId.equals(failedEstablishedViewId)) {
            // SLING-5195 : heartbeat-self-check caused this establishedViewId
            // to be declared as failed - so we must now cause a new voting
            logger.info("doCheckView: current establishedViewId ({}) was declared as failed earlier already.", lastEstablishedViewId);
            establishedViewMatches = false;
        } else {
            if (establishedView == null) {
                establishedViewMatches = false;
            } else {
                String mismatchDetails;
                try{
                    mismatchDetails = establishedView.matches(liveInstances);
                } catch(Exception e) {
                    logger.error("doCheckViewWith: could not compare established view with live ones: "+e, e);
                    invalidateCurrentEstablishedView();
                    discoveryServiceImpl.handleTopologyChanging();
                    return;
                }
                if (mismatchDetails != null) {
                    logger.info("doCheckView: established view does not match. (details: " + mismatchDetails + ")");
                } else {
                    logger.debug("doCheckView: established view matches with expected.");
                }
                establishedViewMatches = mismatchDetails == null;
            }
        }
        
        if (establishedViewMatches) {
            // that's the normal case. the established view matches what we're
            // seeing.
            // all happy and fine
            logger.debug("doCheckViewWith: no pending nor winning votes. view is fine. we're all happy.");
            return;
        }
        
        // immediately send a TOPOLOGY_CHANGING - could already be sent, but just to be sure
        logger.info("doCheckViewWith: no matching established view, marking topology as changing");
        invalidateCurrentEstablishedView();
        discoveryServiceImpl.handleTopologyChanging();
        
        List<VotingView> myYesVotes = VotingHelper.getYesVotingsOf(resourceResolver, config, slingId);
        if (myYesVotes != null && myYesVotes.size() > 0) {
            logger.info("doCheckViewWith: I have voted yes (" + myYesVotes.size() + "x)- the vote was not yet promoted but expecting it to be soon. Not voting again in the meantime. My yes vote was for: "+myYesVotes);
            return;
        }
        
    	if (logger.isDebugEnabled()) {
	        logger.debug("doCheckViewWith: no pending nor winning votes. But: view does not match established or no established yet. Initiating a new voting");
	        Iterator<String> it = liveInstances.iterator();
	        while (it.hasNext()) {
	            logger.debug("doCheckViewWith: one of the live instances is: "
	                    + it.next());
	        }
    	}

        // we seem to be the first to realize that the currently established
        // view doesnt match
        // the currently live instances.

        // initiate a new voting
        doStartNewVoting(resourceResolver, liveInstances);
    }

    private void doStartNewVoting(final ResourceResolver resourceResolver,
            final Set<String> liveInstances) throws PersistenceException {
        String votingId = nextVotingId;
        nextVotingId = UUID.randomUUID().toString();

        VotingView.newVoting(resourceResolver, config, votingId, slingId, liveInstances);
    }
    
    /**
     * Mark the current establishedView as invalid - requiring it to be
     * replaced with a new one, be it by another instance or this one,
     * via a new vote
     */
    public void invalidateCurrentEstablishedView() {
        if (lastEstablishedViewId == null) {
            logger.info("invalidateCurrentEstablishedView: cannot invalidate, lastEstablishedViewId==null");
            return;
        }
        logger.info("invalidateCurrentEstablishedView: invalidating slingId=" + slingId
                + ", lastEstablishedViewId=" + lastEstablishedViewId);
        failedEstablishedViewId = lastEstablishedViewId;
        discoveryServiceImpl.getClusterViewServiceImpl().invalidateEstablishedViewId(lastEstablishedViewId);
    }
    
    /**
     * Management function to trigger the otherwise algorithm-dependent
     * start of a new voting.
     * This can make sense when explicitly trying to force a leader
     * change (which is otherwise not allowed by the discovery API)
     */
    public void startNewVoting() {
        logger.info("startNewVoting: explicitly starting new voting...");
        ResourceResolver resourceResolver = null;
        try {
            resourceResolver = getResourceResolver();
            final Resource clusterNodesRes = ResourceHelper.getOrCreateResource(
                    resourceResolver, config.getClusterInstancesPath());
            final Set<String> liveInstances = ViewHelper.determineLiveInstances(
                    clusterNodesRes, config);
            doStartNewVoting(resourceResolver, liveInstances);
            logger.info("startNewVoting: explicit new voting was started.");
        } catch (LoginException e) {
            logger.error("startNewVoting: could not log in administratively: " + e,
                    e);
        } catch (PersistenceException e) {
            logger.error(
                    "startNewVoting: encountered a persistence exception during view check: "
                            + e, e);
        } finally {
            if (resourceResolver != null) {
                resourceResolver.close();
            }
        }
    }
    
}
