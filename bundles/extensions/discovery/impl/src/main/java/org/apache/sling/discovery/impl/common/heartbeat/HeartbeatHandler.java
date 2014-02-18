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
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import javax.jcr.Session;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
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
import org.osgi.service.component.ComponentContext;
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
public class HeartbeatHandler implements Runnable, StartupListener {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** the name used for the period job with the scheduler **/
    private static final String NAME = "discovery.impl.heartbeat.runner";

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
    private boolean resetLeaderElectionId = false;

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
     * when switching form isolated to established view but with only the local
     * instance in the view.
     */
    public void initialize(final DiscoveryServiceImpl discoveryService,
            final String initialVotingId) {
        synchronized(lock) {
        	this.discoveryService = discoveryService;
        	this.nextVotingId = initialVotingId;
            issueHeartbeat();
        }

        try {
            scheduler.addPeriodicJob(NAME, this,
                    null, config.getHeartbeatInterval(), false);
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
            scheduler.fireJob(this, null);
        } catch (Exception e) {
            logger.info("triggerHeartbeat: Could not trigger heartbeat: " + e);
        }
    }

    /**
     * Issue a heartbeat.
     * <p>
     * This action consists of first updating the local properties,
     * then issuing a cluster-local heartbeat (within the repository)
     * and then a remote heartbeat (to all the topology connectors
     * which announce this part of the topology to others)
     */
    private void issueHeartbeat() {
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
        connectorRegistry.pingOutgoingConnectors(forcePing);
        forcePing = false;
    }

    /** Issue a cluster local heartbeat (into the repository) **/
    private void issueClusterLocalHeartbeat() {
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
            		final Calendar lastHeartbeat = resourceMap.get("lastHeartbeat", Calendar.class);
            		if (lastHeartbeat!=null) {
            			// if there is a heartbeat value, check if it is what I've written 
            			// the last time
            			if (!lastHeartbeatWritten.getTime().equals(lastHeartbeat.getTime())) {
            				// then we've likely hit the situation where there is another
            				// sling instance accessing the same repository (ie in the same cluster)
            				// using the same sling.id - hence writing to the same
            				// resource
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
            	final String readRuntimeId = resourceMap.get("runtimeId", String.class);
            	if (!runtimeId.equals(readRuntimeId)) {
            		logger.error("issueClusterLocalHeartbeat: SLING-2091: Detected more than 1 instance running in this cluster " +
            				" with the same sling.id. My sling.id is "+slingId+", " +
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
            resourceMap.put("lastHeartbeat", currentTime);
            if (firstHeartbeatWritten==-1) {
            	resourceMap.put("runtimeId", runtimeId);
            }
            if (resetLeaderElectionId || !resourceMap.containsKey("leaderElectionId")) {
                int maxLongLength = String.valueOf(Long.MAX_VALUE).length();
                String currentTimeMillisStr = String.format("%0"
                        + maxLongLength + "d", System.currentTimeMillis());

                String prefix = "0";

                String leaderElectionRepositoryDescriptor = config.getLeaderElectionRepositoryDescriptor();
                if (leaderElectionRepositoryDescriptor!=null && leaderElectionRepositoryDescriptor.length()!=0) {
                    // when this property is configured, check the value of the repository descriptor
                    // and if that value is set, include it in the leader election id

                    final Session session = resourceResolver.adaptTo(Session.class);
                    if ( session != null ) {
                        String value = session.getRepository()
                                .getDescriptor(leaderElectionRepositoryDescriptor);
                        if (value != null && value.equalsIgnoreCase("true")) {
                            prefix = "1";
                        }
                    }
                }
                resourceMap.put("leaderElectionId", prefix + "_"
                        + currentTimeMillisStr + "_" + slingId);
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

    /** Check whether the established view matches the reality, ie matches the
     * heartbeats
     */
    private void checkView() {
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
            logger.info("doCheckView: votingHandler is null!");
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
        String votingId = nextVotingId;
        nextVotingId = UUID.randomUUID().toString();

        VotingView.newVoting(resourceResolver, config, votingId, slingId, liveInstances);
    }

}
