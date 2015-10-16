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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.jcr.Session;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
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
import org.apache.sling.discovery.impl.Config;
import org.apache.sling.discovery.impl.DiscoveryServiceImpl;
import org.apache.sling.discovery.impl.cluster.voting.VotingHandler;
import org.apache.sling.discovery.impl.cluster.voting.VotingHelper;
import org.apache.sling.discovery.impl.cluster.voting.VotingView;
import org.apache.sling.discovery.impl.common.ViewHelper;
import org.apache.sling.discovery.impl.common.resource.ResourceHelper;
import org.apache.sling.discovery.impl.topology.announcement.AnnouncementRegistry;
import org.apache.sling.discovery.impl.topology.connector.ConnectorRegistry;
import org.apache.sling.launchpad.api.StartupListener;
import org.apache.sling.launchpad.api.StartupMode;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The heartbeat handler is responsible and capable of issuing both local and
 * remote heartbeats and registers a periodic job with the scheduler for doing so.
 * <p>
 * Local heartbeats are stored in the repository. Remote heartbeats are POSTs to
 * remote TopologyConnectorServlets.
 */
@Component
@Service(value = { HeartbeatHandler.class, StartupListener.class })
@Reference(referenceInterface=HttpService.class,
           cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE,
           policy=ReferencePolicy.DYNAMIC)
public class HeartbeatHandler implements Runnable, StartupListener {

    private static final String PROPERTY_ID_LAST_HEARTBEAT = "lastHeartbeat";

    private static final String PROPERTY_ID_ENDPOINTS = "endpoints";

    private static final String PROPERTY_ID_SLING_HOME_PATH = "slingHomePath";

    private static final String PROPERTY_ID_RUNTIME = "runtimeId";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** Endpoint service registration property from RFC 189 */
    private static final String REG_PROPERTY_ENDPOINTS = "osgi.http.service.endpoints";

    /** the name used for the period job with the scheduler **/
    private String NAME = "discovery.impl.heartbeat.runner.";

    @Reference
    private SlingSettingsService slingSettingsService;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private ConnectorRegistry connectorRegistry;

    @Reference
    private AnnouncementRegistry announcementRegistry;

    @Reference
    private Scheduler scheduler;

    @Reference
    private Config config;

    @Reference
    private VotingHandler votingHandler;

    /** the discovery service reference is used to get properties updated before heartbeats are sent **/
    private DiscoveryServiceImpl discoveryService;

    /** the sling id of the local instance **/
    private String slingId;

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
    
    /** lock object for synchronizing the run method **/
    private final Object lock = new Object();

    /** SLING-2892: remember first heartbeat written to repository by this instance **/
    private long firstHeartbeatWritten = -1;

    /** SLING-2892: remember the value of the heartbeat this instance has written the last time **/
    private Calendar lastHeartbeatWritten = null;

    /** SLING-2895: avoid heartbeats after deactivation **/
    private volatile boolean activated = false;

    /** SLING-2901: the runtimeId is a unique id, set on activation, used for robust duplicate sling.id detection **/
    private String runtimeId;

    /** keep a reference to the component context **/
    private ComponentContext context;

    /** SLING-2968 : start issuing remote heartbeats only after startup finished **/
    private boolean startupFinished = false;

    /** SLING-3382 : force ping instructs the servlet to start the backoff from scratch again **/
    private boolean forcePing;

    /** SLING-4765 : store endpoints to /clusterInstances for more verbose duplicate slingId/ghost detection **/
    private final Map<Long, String[]> endpoints = new HashMap<Long, String[]>();

    public void inform(StartupMode mode, boolean finished) {
    	if (finished) {
    		startupFinished(mode);
    	}
    }

    public void startupFinished(StartupMode mode) {
    	synchronized(lock) {
    		startupFinished = true;
    		issueHeartbeat();
    	}
    }

    public void startupProgress(float ratio) {
    	// we dont care
    }

    @Activate
    protected void activate(ComponentContext context) {
    	synchronized(lock) {
    		this.context = context;

	        slingId = slingSettingsService.getSlingId();
	        NAME = "discovery.impl.heartbeat.runner." + slingId;
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

	        activated = true;
	        logger.info("activate: activated with runtimeId: {}, slingId: {}", runtimeId, slingId);
    	}
    }

    @Deactivate
    protected void deactivate() {
        // SLING-3365 : dont synchronize on deactivate
        activated = false;
    	scheduler.removeJob(NAME);
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
        	this.discoveryService = discoveryService;
        	this.nextVotingId = initialVotingId;
        	logger.info("initialize: nextVotingId="+nextVotingId);
            issueHeartbeat();
        }

        try {
            final long interval = config.getHeartbeatInterval();
            logger.info("initialize: starting periodic heartbeat job for "+slingId+" with interval "+interval+" sec.");
            scheduler.addPeriodicJob(NAME, this,
                    null, interval, false);
        } catch (Exception e) {
            logger.error("activate: Could not start heartbeat runner: " + e, e);
        }
    }

    public void run() {
        synchronized(lock) {
        	if (!activated) {
        		// SLING:2895: avoid heartbeats if not activated
        		return;
        	}

            // issue a heartbeat
            issueHeartbeat();

            // check the view
            checkView();
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

    /** Trigger the issuance of the next heartbeat asap instead of at next heartbeat interval **/
    public void triggerHeartbeat() {
        forcePing = true;
        try {
            // then fire a job immediately
            // use 'fireJobAt' here, instead of 'fireJob' to make sure the job can always be triggered
            // 'fireJob' checks for a job from the same job-class to already exist
            // 'fireJobAt' though allows to pass a name for the job - which can be made unique, thus does not conflict/already-exist
            logger.info("triggerHeartbeat: firing job to trigger heartbeat");
            scheduler.fireJobAt(NAME+UUID.randomUUID(), this, null, new Date(System.currentTimeMillis()-1000 /* make sure it gets triggered immediately*/));
        } catch (Exception e) {
            logger.info("triggerHeartbeat: Could not trigger heartbeat: " + e);
        }
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
    void issueHeartbeat() {
        if (discoveryService == null) {
            logger.error("issueHeartbeat: discoveryService is null");
        } else {
            discoveryService.updateProperties();
        }
        issueClusterLocalHeartbeat();
        issueRemoteHeartbeats();
    }

    /** Issue a remote heartbeat using the topology connectors **/
    private void issueRemoteHeartbeats() {
        if (connectorRegistry == null) {
            logger.error("issueRemoteHeartbeats: connectorRegistry is null");
            return;
        }
        if (!startupFinished) {
        	logger.debug("issueRemoteHeartbeats: not issuing remote heartbeat yet, startup not yet finished");
        	return;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("issueRemoteHeartbeats: pinging outgoing topology connectors (if there is any) for "+slingId);
        }
        connectorRegistry.pingOutgoingConnectors(forcePing);
        forcePing = false;
    }

    /** Issue a cluster local heartbeat (into the repository) **/
    private void issueClusterLocalHeartbeat() {
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
            			    discoveryService.handleTopologyChanging();
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
            	    discoveryService.handleTopologyChanging();
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
            		discoveryService.forcedShutdown();
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
                logger.info("issueClusterLocalHeartbeat: set leaderElectionId to "+newLeaderElectionId);
                if (votingHandler!=null) {
                    votingHandler.setLeaderElectionId(newLeaderElectionId);
                }
                resetLeaderElectionId = false;
            }
            resourceResolver.commit();

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
    void checkView() {
        // check the remotes first
        if (announcementRegistry == null) {
            logger.error("announcementRegistry is null");
            return;
        }
        announcementRegistry.checkExpiredAnnouncements();

        ResourceResolver resourceResolver = null;
        try {
            resourceResolver = getResourceResolver();
            doCheckView(resourceResolver);
        } catch (LoginException e) {
            logger.error("checkView: could not log in administratively: " + e,
                    e);
        } catch (PersistenceException e) {
            logger.error(
                    "checkView: encountered a persistence exception during view check: "
                            + e, e);
        } finally {
            if (resourceResolver != null) {
                resourceResolver.close();
            }
        }
    }

    /** do the established-against-heartbeat view check using the given resourceResolver.
     */
    private void doCheckView(final ResourceResolver resourceResolver) throws PersistenceException {

        if (votingHandler==null) {
            logger.info("doCheckView: votingHandler is null! slingId="+slingId);
        } else {
            votingHandler.analyzeVotings(resourceResolver);
            try{
                votingHandler.cleanupTimedoutVotings(resourceResolver);
            } catch(Exception e) {
                logger.warn("doCheckView: Exception occurred while cleaning up votings: "+e, e);
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
            logger.info("doCheckView: there are pending votings, marking topology as changing...");
            discoveryService.handleTopologyChanging();
            
        	if (logger.isDebugEnabled()) {
	            logger.debug("doCheckView: "
	                    + numOpenNonWinningVotes
	                    + " ongoing votings, no one winning yet - I shall wait for them to settle.");
        	}
            return;
        }

        final Resource clusterNodesRes = ResourceHelper.getOrCreateResource(
                resourceResolver, config.getClusterInstancesPath());
        final Set<String> liveInstances = ViewHelper.determineLiveInstances(
                clusterNodesRes, config);

        if (ViewHelper.establishedViewMatches(resourceResolver, config, liveInstances)) {
            // that's the normal case. the established view matches what we're
            // seeing.
            // all happy and fine
            logger.debug("doCheckView: no pending nor winning votes. view is fine. we're all happy.");
            return;
        }
        
        // immediately send a TOPOLOGY_CHANGING - could already be sent, but just to be sure
        logger.info("doCheckView: no matching established view, marking topology as changing");
        discoveryService.handleTopologyChanging();
        
    	if (logger.isDebugEnabled()) {
	        logger.debug("doCheckView: no pending nor winning votes. But: view does not match established or no established yet. Initiating a new voting");
	        Iterator<String> it = liveInstances.iterator();
	        while (it.hasNext()) {
	            logger.debug("doCheckView: one of the live instances is: "
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

    /**
     * Bind a http service
     */
    protected void bindHttpService(final ServiceReference reference) {
        final String[] endpointUrls = toStringArray(reference.getProperty(REG_PROPERTY_ENDPOINTS));
        if ( endpointUrls != null ) {
            synchronized ( lock ) {
                this.endpoints.put((Long)reference.getProperty(Constants.SERVICE_ID), endpointUrls);
                
                // make sure this gets written on next heartbeat
                firstHeartbeatWritten = -1;
                lastHeartbeatWritten = null;
            }
        }
    }

    /**
     * Unbind a http service
     */
    protected void unbindHttpService(final ServiceReference reference) {
        synchronized ( lock ) {
            if ( this.endpoints.remove(reference.getProperty(Constants.SERVICE_ID)) != null ) {
                // make sure the change gets written on next heartbeat
                firstHeartbeatWritten = -1;
                lastHeartbeatWritten = null;
            }
        }
    }
    
    private String[] toStringArray(final Object propValue) {
        if (propValue == null) {
            // no value at all
            return null;

        } else if (propValue instanceof String) {
            // single string
            return new String[] { (String) propValue };

        } else if (propValue instanceof String[]) {
            // String[]
            return (String[]) propValue;

        } else if (propValue.getClass().isArray()) {
            // other array
            Object[] valueArray = (Object[]) propValue;
            List<String> values = new ArrayList<String>(valueArray.length);
            for (Object value : valueArray) {
                if (value != null) {
                    values.add(value.toString());
                }
            }
            return values.toArray(new String[values.size()]);

        } else if (propValue instanceof Collection<?>) {
            // collection
            Collection<?> valueCollection = (Collection<?>) propValue;
            List<String> valueList = new ArrayList<String>(valueCollection.size());
            for (Object value : valueCollection) {
                if (value != null) {
                    valueList.add(value.toString());
                }
            }
            return valueList.toArray(new String[valueList.size()]);
        }

        return null;
    }
    
    private String getEndpointsAsString() {
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        for(final String[] points : endpoints.values()) {
            for(final String point : points) {
                if ( first ) {
                    first = false;
                } else {
                    sb.append(",");
                }
                sb.append(point);
            }
        }
        return sb.toString();
        
    }
}
