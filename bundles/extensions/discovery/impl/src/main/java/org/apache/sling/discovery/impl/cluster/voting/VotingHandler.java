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
package org.apache.sling.discovery.impl.cluster.voting;

import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import org.apache.sling.discovery.commons.providers.util.ResourceHelper;
import org.apache.sling.discovery.impl.Config;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The osgi event handler responsible for following any votings and vote
 * accordingly
 */
@Component(immediate = true)
@Service(value = {VotingHandler.class})
public class VotingHandler implements EventHandler {
    
    public static enum VotingDetail {
        PROMOTED,
        WINNING,
        VOTED_YES,
        VOTED_NO,
        UNCHANGED,
        TIMEDOUT
    }
    
    private final static Comparator<VotingView> VOTING_COMPARATOR = new Comparator<VotingView>() {

        public int compare(VotingView o1, VotingView o2) {
            if (o1 == o2) {
                return 0;
            }
            if (o1 == null && o2 != null) {
                return 1;
            }
            if (o2 == null && o1 != null) {
                return -1;
            }
            // now both are non-null
            return (o1.getVotingId().compareTo(o2.getVotingId()));
        }
    };
    
    /** the name used for the period job with the scheduler **/
    protected String NAME = "discovery.impl.analyzeVotings.runner.";

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Reference
    private SlingSettingsService slingSettingsService;

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference
    private Config config;

    /** the sling id of the local instance **/
    private String slingId;

    /** the HeartbeatHandler sets the leaderElectionid - this is subsequently used
     * to ensure the leaderElectionId is correctly set upon voting
     */
    private volatile String leaderElectionId;

    private volatile boolean activated;

    private ComponentContext context;
    
    private ServiceRegistration eventHandlerRegistration;

    /** for testing only **/
    public static VotingHandler testConstructor(SlingSettingsService settingsService,
            ResourceResolverFactory factory, Config config) {
        VotingHandler handler = new VotingHandler();
        handler.slingSettingsService = settingsService;
        handler.resolverFactory = factory;
        handler.config = config;
        return handler;
    }

    @Deactivate
    protected void deactivate() {
        if (eventHandlerRegistration != null) {
            eventHandlerRegistration.unregister();
            logger.info("deactivate: VotingHandler unregistered as EventHandler");
            eventHandlerRegistration = null;
        }
        activated = false;
        logger.info("deactivate: deactivated slingId: {}, this: {}", slingId, this);
    }
    
    @Activate
    protected void activate(final ComponentContext context) {
        slingId = slingSettingsService.getSlingId();
        logger = LoggerFactory.getLogger(this.getClass().getCanonicalName()
                + "." + slingId);
        this.context = context;
        activated = true;
        
        // once activated, register the eventHandler so that we can 
        // start receiving and processing votings...
        registerEventHandler();
        logger.info("activated: activated ("+slingId+")");
    }

    private void registerEventHandler() {
        BundleContext bundleContext = context == null ? null : context.getBundleContext();
        if (bundleContext == null) {
            logger.info("registerEventHandler: context or bundleContext is null - cannot register");
            return;
        }
        Dictionary<String,Object> properties = new Hashtable<String,Object>();
        properties.put(Constants.SERVICE_DESCRIPTION, "Voting Event Listener");
        String[] topics = new String[] {
                SlingConstants.TOPIC_RESOURCE_ADDED,
                SlingConstants.TOPIC_RESOURCE_CHANGED,
                SlingConstants.TOPIC_RESOURCE_REMOVED };
        properties.put(EventConstants.EVENT_TOPIC, topics);
        String path = config.getDiscoveryResourcePath();
        if (path.endsWith("/")) {
            path = path.substring(0, path.length()-1);
        }
        path = path + "/*";
        properties.put(EventConstants.EVENT_FILTER, "(&(path="+path+"))");
        eventHandlerRegistration = bundleContext.registerService(
                EventHandler.class.getName(), this, properties);
        logger.info("registerEventHandler: VotingHandler registered as EventHandler");
    }

    /**
     * handle repository changes and react to ongoing votings
     */
    public void handleEvent(final Event event) {
        if (!activated) {
            return;
        }
        String resourcePath = (String) event.getProperty("path");
        String ongoingVotingsPath = config.getOngoingVotingsPath();

        if (resourcePath == null) {
            // not of my business
            return;
        }
        if (!resourcePath.startsWith(ongoingVotingsPath)) {
            // not of my business
            return;
        }

        ResourceResolver resourceResolver = null;
        try {
            resourceResolver = resolverFactory
                    .getAdministrativeResourceResolver(null);
        } catch (LoginException e) {
            logger.error(
                    "handleEvent: could not log in administratively: " + e, e);
            return;
        }
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("handleEvent: path = "+resourcePath+", event = "+event);
            }
            analyzeVotings(resourceResolver);
        } catch (PersistenceException e) {
            logger.error(
                    "handleEvent: got a PersistenceException during votings analysis: "
                            + e, e);
        } finally {
            if (resourceResolver != null) {
                resourceResolver.close();
            }
        }
    }

    /**
     * Analyze any ongoing voting in the repository.
     * <p>
     * SLING-2885: this method must be synchronized as it can be called concurrently
     * by the HearbeatHandler.doCheckView and the VotingHandler.handleEvent.
     */
    public synchronized Map<VotingView,VotingDetail> analyzeVotings(final ResourceResolver resourceResolver) throws PersistenceException {
        if (!activated) {
            logger.info("analyzeVotings: VotingHandler not yet initialized, can't vote.");
            return null;
        }
        Map<VotingView,VotingDetail> result = new HashMap<VotingView,VotingDetail>();
        // SLING-3406: refreshing resourceResolver/session here to get the latest state from the repository
        logger.debug("analyzeVotings: start. slingId: {}", slingId);
        resourceResolver.refresh();
        VotingView winningVote = VotingHelper.getWinningVoting(
                resourceResolver, config);
        if (winningVote != null) {
            if (winningVote.isInitiatedBy(slingId)) {
                logger.info("analyzeVotings: my voting was winning. I'll mark it as established then! "
	                        + winningVote);
                try{
                    promote(resourceResolver, winningVote.getResource());
                } catch (RuntimeException re) {
                    logger.error("analyzeVotings: RuntimeException during promotion: "+re, re);
                    throw re;
                } catch (Error er) {
                    logger.error("analyzeVotings: Error during promotion: "+er, er);
                    throw er;
                }
                // SLING-3406: committing resourceResolver/session here, while we're in the synchronized
                resourceResolver.commit();
                
                // for test verification
                result.put(winningVote, VotingDetail.PROMOTED);
                return result;
            } else {
        		logger.info("analyzeVotings: there is a winning vote. No need to vote any further. Expecting it to get promoted to established: "
            				+ winningVote);
        		
        		result.put(winningVote, VotingDetail.WINNING);
            	return result;
            }
        }

        List<VotingView> ongoingVotings = VotingHelper.listVotings(resourceResolver, config);
        if (ongoingVotings == null || ongoingVotings.size() == 0) {
            logger.debug("analyzeVotings: no ongoing votings at the moment. done.");
            return result;
        }
        Collections.sort(ongoingVotings, VOTING_COMPARATOR);
        VotingView yesVote = null;
        for (VotingView voting : ongoingVotings) {
            Boolean myVote = voting.getVote(slingId);
            boolean votedNo = myVote != null && !myVote;
            boolean votedYes = myVote != null && myVote;
            if (voting.isTimedoutVoting(config)) {
                // if a voting has timed out, delete it
                logger.info("analyzeVotings: deleting a timed out voting: "+voting);
                voting.remove(false);
                result.put(voting, VotingDetail.TIMEDOUT);
                continue;
            }
            if (voting.hasNoVotes()) {
                if (!votedNo) {
                    logger.info("analyzeVotings: vote already has no votes, so I shall also vote no: "+voting);
                    voting.vote(slingId, false, null);
                    result.put(voting, VotingDetail.VOTED_NO);
                } else {
                    // else ignore silently
                    result.put(voting, VotingDetail.UNCHANGED);
                }
                continue;
            }
            if (!voting.isOngoingVoting(config)) {
                logger.debug("analyzeVotings: vote is not ongoing (ignoring): "+voting);
                continue;
            }
            String liveComparison;
            try {
                liveComparison = voting.matchesLiveView(config);
            } catch (Exception e) {
                logger.error("analyzeVotings: could not compare voting with live view: "+e, e);
                continue;
            }
            if (liveComparison != null) {
                if (!votedNo) {
                    logger.info("analyzeVotings: vote doesnt match my live view, voting no. "
                            + "comparison result: "+liveComparison+", vote: "+voting);
                    voting.vote(slingId, false, null);
                    result.put(voting, VotingDetail.VOTED_NO);
                } else {
                    result.put(voting, VotingDetail.UNCHANGED);
                }
                continue;
            }
            if (yesVote != null) {
                // as soon as I found the one I should vote yes for, 
                // vote no for the rest
                if (!votedNo) {
                    logger.info("analyzeVotings: already voted yes, so voting no for: "+voting);
                    voting.vote(slingId, false, null);
                    result.put(voting, VotingDetail.VOTED_NO);
                } else {
                    // else ignore silently
                    result.put(voting, VotingDetail.UNCHANGED);
                }
                continue;
            }
            if (!votedYes) {
                logger.info("analyzeVotings: not timed out, no no-votes, matches live, still ongoing, "
                        + "I have not yet voted yes, so noting candidate for yes as: "+voting);
            }
            yesVote = voting;
        }
        if (yesVote != null) {
            Boolean myVote = yesVote.getVote(slingId);
            boolean votedYes = myVote != null && myVote;
            if (!votedYes) {
                logger.info("analyzeVotings: declaring my personal winner: "+yesVote+" (myVote==null: "+(myVote==null)+")");
                yesVote.vote(slingId, true, leaderElectionId);
                result.put(yesVote, VotingDetail.VOTED_YES);
            } else {
                // else don't double vote / log
                result.put(yesVote, VotingDetail.UNCHANGED);
            }
        }
        resourceResolver.commit();
        logger.debug("analyzeVotings: result: my yes vote was for: " + yesVote);
        return result;
    }
    
    public void cleanupTimedoutVotings(final ResourceResolver resourceResolver) {
        List<VotingView> timedoutVotings = VotingHelper
                .listTimedoutVotings(resourceResolver,
                        config);
        Iterator<VotingView> it = timedoutVotings.iterator();
        while (it.hasNext()) {
            VotingView timedoutVotingRes = it.next();
            if (timedoutVotingRes!=null) {
                logger.info("cleanupTimedoutVotings: removing a timed out voting: "+timedoutVotingRes);
                timedoutVotingRes.remove(false);
            }
        }
    }

    /**
     * Promote a particular voting to be the new established view
     */
    private void promote(final ResourceResolver resourceResolver,
            final Resource winningVoteResource) throws PersistenceException {
        Resource previousViewsResource = ResourceHelper
                .getOrCreateResource(
                        resourceResolver,
                        config.getPreviousViewPath());
        final Resource establishedViewsResource = ResourceHelper
                .getOrCreateResource(
                        resourceResolver,
                        config.getEstablishedViewPath());
        final Resource ongoingVotingsResource = ResourceHelper
                .getOrCreateResource(
                        resourceResolver,
                        config.getOngoingVotingsPath());

    	if (logger.isDebugEnabled()) {
	        logger.debug("promote: previousViewsResource="
	                + previousViewsResource.getPath());
	        logger.debug("promote: establishedViewsResource="
	                + establishedViewsResource.getPath());
	        logger.debug("promote: ongoingVotingsResource="
	                + ongoingVotingsResource.getPath());
	        logger.debug("promote: winningVoteResource="
	                + winningVoteResource.getPath());
    	}

        // step 1: remove any nodes under previousViews
        final Iterator<Resource> it1 = previousViewsResource.getChildren().iterator();
        try{
            while (it1.hasNext()) {
                Resource previousView = it1.next();
                resourceResolver.delete(previousView);
            }
        } catch(PersistenceException e) {
            // if we cannot delete, apply workaround suggested in SLING-3785
            logger.error("promote: Could not delete a previous view - trying move next: "+e, e);
            ResourceHelper.moveResource(previousViewsResource, config.getPreviousViewPath()+"_trash_"+UUID.randomUUID().toString());
            logger.info("promote: recreating the previousviews node");
            previousViewsResource = ResourceHelper
                    .getOrCreateResource(
                            resourceResolver,
                            config.getPreviousViewPath());            
        }

        // step 2: retire the existing established view.
        // Note that there must always only be one. But if there's more, retire
        // them all now.
        final Iterator<Resource> it = establishedViewsResource.getChildren()
                .iterator();
        boolean first = true;
        while (it.hasNext()) {
            Resource retiredView = it.next();
            if (first) {
                first = !first;
            	if (logger.isDebugEnabled()) {
	                logger.debug("promote: moving the old established view to previous views: "
	                        + retiredView.getPath());
            	}
                ResourceHelper.moveResource(retiredView, 
                        previousViewsResource.getPath()
                                + "/" + retiredView.getName());
            } else {
            	if (logger.isDebugEnabled()) {
	                logger.debug("promote: retiring an erroneously additionally established node "
	                        + retiredView.getPath());
            	}
                resourceResolver.delete(retiredView);
            }
        }

        // step 3: move the winning vote resource under the
        // establishedViewsResource

        // 3a: set the leaderid
        final Iterator<Resource> it2 = winningVoteResource.getChild("members")
                .getChildren().iterator();
        String leaderElectionId = null;
        String leaderid = null;
        int membersCount = 0;
        while (it2.hasNext()) {
            Resource aMember = it2.next();
            membersCount++;
            String leid = aMember.adaptTo(ValueMap.class).get(
                    "leaderElectionId", String.class);
            if (leaderElectionId == null
                    || (leid != null && leid.compareTo(leaderElectionId) < 0)) {
                leaderElectionId = leid;
                leaderid = aMember.getName();
            }
        }
    	if (logger.isDebugEnabled()) {
	        logger.debug("promote: leader is " + leaderid
	                + " - with leaderElectionId=" + leaderElectionId);
    	}
        ModifiableValueMap winningVoteMap = winningVoteResource.adaptTo(ModifiableValueMap.class);
        winningVoteMap.put("leaderId", leaderid);
        winningVoteMap.put("leaderElectionId", leaderElectionId);
        winningVoteMap.put("promotedAt", Calendar.getInstance());
        winningVoteMap.put("promotedBy", slingId);

        // 3b: move the result under /established
        final String newEstablishedViewPath = establishedViewsResource.getPath()
                + "/" + winningVoteResource.getName();
        logger.info("promote: promoting to new established node (#members: " + membersCount + ", path: "
                + newEstablishedViewPath + ")");
        ResourceHelper.moveResource(winningVoteResource, newEstablishedViewPath);

        // step 4: delete all ongoing votings...
        final Iterable<Resource> ongoingVotingsChildren = ongoingVotingsResource
                .getChildren();
        if (ongoingVotingsChildren != null) {
            Iterator<Resource> it4 = ongoingVotingsChildren.iterator();
            while (it4.hasNext()) {
                Resource anOngoingVoting = it4.next();
                logger.info("promote: deleting ongoing voting: "+anOngoingVoting.getName());
                resourceResolver.delete(anOngoingVoting);
            }
        }

        // step 5: make sure there are no duplicate ongoingVotings nodes
        // created. if so, cleanup
        final Iterator<Resource> it5 = ongoingVotingsResource.getParent()
                .getChildren().iterator();
        while (it5.hasNext()) {
            Resource resource = it5.next();
            if (!resource
                    .getPath()
                    .startsWith(
                            config.getOngoingVotingsPath())) {
                continue;
            }
            if (resource
                    .getPath()
                    .equals(config.getOngoingVotingsPath())) {
                // then it's [0] so to speak .. which we're not cleaning up
                continue;
            }
            logger.warn("promote: cleaning up a duplicate ongoingVotingPath: "
                    + resource.getPath());
            resourceResolver.delete(resource);
        }

        logger.debug("promote: done with promotiong. saving.");
        resourceResolver.commit();
        logger.info("promote: promotion done (#members: " + membersCount + ", path: "
                + newEstablishedViewPath + ")");
    }

    public void setLeaderElectionId(String leaderElectionId) {
        logger.info("setLeaderElectionId: leaderElectionId="+leaderElectionId);
        this.leaderElectionId = leaderElectionId;
    }
}