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
import java.util.Iterator;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
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
import org.apache.sling.discovery.impl.Config;
import org.apache.sling.discovery.impl.common.resource.ResourceHelper;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.framework.Constants;
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
@Service(value = {EventHandler.class, VotingHandler.class})
@Properties({
        @Property(name = Constants.SERVICE_DESCRIPTION, value = "New Voting Event Listener."),
        @Property(name = EventConstants.EVENT_TOPIC, value = {
                SlingConstants.TOPIC_RESOURCE_ADDED,
                SlingConstants.TOPIC_RESOURCE_CHANGED,
                SlingConstants.TOPIC_RESOURCE_REMOVED }) })
// ,
// @Property(name = EventConstants.EVENT_FILTER, value = "(path="
// + org.apache.sling.discovery.viewmgr.Constants.ROOT_PATH + ")") })
public class VotingHandler implements EventHandler {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Reference
    private SlingSettingsService slingSettingsService;

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference
    private Config config;

    /** the sling id of the local instance **/
    private String slingId;

    protected void activate(final ComponentContext context) {
        slingId = slingSettingsService.getSlingId();
        logger = LoggerFactory.getLogger(this.getClass().getCanonicalName()
                + "." + slingId);
    }

    /**
     * handle repository changes and react to ongoing votings
     */
    public void handleEvent(final Event event) {
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
    public synchronized void analyzeVotings(final ResourceResolver resourceResolver) throws PersistenceException {
        // SLING-3406: refreshing resourceResolver/session here to get the latest state from the repository
        resourceResolver.refresh();
        VotingView winningVote = VotingHelper.getWinningVoting(
                resourceResolver, config);
        if (winningVote != null) {
            if (winningVote.isInitiatedBy(slingId)) {
            	if (logger.isDebugEnabled()) {
	                logger.debug("analyzeVotings: my voting was winning. I'll mark it as established then! "
	                        + winningVote);
            	}
                promote(resourceResolver, winningVote.getResource());
                // SLING-3406: committing resourceResolver/session here, while we're in the synchronized
                resourceResolver.commit();
            } else {
            	if (logger.isDebugEnabled()) {
            		logger.debug("analyzeVotings: there is a winning vote. No need to vote any further. Expecting it to get promoted to established: "
            				+ winningVote);
            	}
            }
        }

        List<VotingView> ongoingVotings = VotingHelper
                .listOpenNonWinningVotings(resourceResolver,
                        config);

        Resource clusterNodesRes = resourceResolver
                .getResource(config.getClusterInstancesPath());

        Iterator<VotingView> it = ongoingVotings.iterator();
        while (it.hasNext()) {
            VotingView ongoingVotingRes = it.next();
            if (winningVote != null && !winningVote.equals(ongoingVotingRes)
                    && !ongoingVotingRes.hasVotedOrIsInitiator(slingId)) {
                // there is a winning vote, it doesn't equal
                // ongoingVotingRes, and I have not voted on
                // ongoingVotingRes yet.
                // so I vote no there now
                ongoingVotingRes.vote(slingId, false);
                it.remove();
            } else if (!ongoingVotingRes.matchesLiveView(clusterNodesRes,
                    config)) {
                logger.warn("analyzeVotings: encountered a voting which does not match mine. Voting no: "
                        + ongoingVotingRes);
                ongoingVotingRes.vote(slingId, false);
                it.remove();
            } else if (ongoingVotingRes.isInitiatedBy(slingId)
                    && ongoingVotingRes.hasNoVotes()) {
            	if (logger.isDebugEnabled()) {
	                logger.debug("analyzeVotings: there were no votes for my voting, so I have to remove it: "
	                        + ongoingVotingRes);
            	}
                ongoingVotingRes.remove();
                it.remove();
            }
        }

        if (winningVote != null) {
        	if (logger.isDebugEnabled()) {
	            logger.debug("analyzeVotings: done with vote-handling. there was a winner.");
        	}
            return;
        }

        if (ongoingVotings.size() == 0) {
            return;
        }

        if (ongoingVotings.size() == 1) {
            VotingView votingResource = ongoingVotings.get(0);
            if (votingResource.isInitiatedBy(slingId)) {
            	if (logger.isDebugEnabled()) {
	                logger.debug("analyzeVotings: only one voting found, and it is mine. I dont have to vote therefore: "
	                        + votingResource);
            	}
                return;
            } // else:
        	if (logger.isDebugEnabled()) {
	            logger.debug("analyzeVotings: only one voting found for which I did not yet vote - and it is not mine. I'll vote yes then: "
	                    + votingResource);
        	}
            votingResource.vote(slingId, true);
        }

        // otherwise there is more than one voting going on, all matching my
        // view of the cluster
        Collections.sort(ongoingVotings, new Comparator<VotingView>() {

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
                return (o1.getViewId().compareTo(o2.getViewId()));
            }
        });

        // having sorted, the lowest view should now win!
        // first lets check if I have voted yes already
        VotingView myYesVoteResource = VotingHelper.getYesVotingOf(resourceResolver,
                config, slingId);
        VotingView lowestVoting = ongoingVotings.get(0);

        if (myYesVoteResource != null && lowestVoting.equals(myYesVoteResource)) {
            // all fine. then I've voted for the lowest viewId - which is
            // the whole idea
        	if (logger.isDebugEnabled()) {
	            logger.debug("analyzeVotings: my voted for view is currently already the lowest id. which is good. I dont have to change any voting. "
	                    + myYesVoteResource);
        	}
        } else if (myYesVoteResource == null) {
            // I've not voted yet - so I should vote for the lowestVoting
        	if (logger.isDebugEnabled()) {
	            logger.debug("analyzeVotings: I apparently have not yet voted. So I shall vote now for the lowest id which is: "
	                    + lowestVoting);
        	}
            lowestVoting.vote(slingId, true);
        } else {
            // otherwise I've already voted, but not for the lowest. which
            // is a shame.
            // I shall change my mind!
            logger.warn("analyzeVotings: I've already voted - but it so happened that there was a lower voting created after I voted... so I shall change my vote from "
                    + myYesVoteResource + " to " + lowestVoting);
            myYesVoteResource.vote(slingId, null);
            lowestVoting.vote(slingId, true);
        }
    	if (logger.isDebugEnabled()) {
	        logger.debug("analyzeVotings: all done now. I've voted yes for "
	                + lowestVoting);
    	}
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
                timedoutVotingRes.remove();
            }
        }
    }

    /**
     * Promote a particular voting to be the new established view
     */
    private void promote(final ResourceResolver resourceResolver,
            final Resource winningVoteResource) throws PersistenceException {
        final Resource previousViewsResource = ResourceHelper
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
        while (it1.hasNext()) {
            Resource previousView = it1.next();
            resourceResolver.delete(previousView);
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
        while (it2.hasNext()) {
            Resource aMember = it2.next();
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
    	if (logger.isDebugEnabled()) {
	        logger.debug("promote: promote to new established node "
	                + newEstablishedViewPath);
    	}
        ResourceHelper.moveResource(winningVoteResource, newEstablishedViewPath);

        // step 4: delete all ongoing votings...
        final Iterable<Resource> ongoingVotingsChildren = ongoingVotingsResource
                .getChildren();
        if (ongoingVotingsChildren != null) {
            Iterator<Resource> it4 = ongoingVotingsChildren.iterator();
            while (it4.hasNext()) {
                Resource anOngoingVoting = it4.next();
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
    }
}