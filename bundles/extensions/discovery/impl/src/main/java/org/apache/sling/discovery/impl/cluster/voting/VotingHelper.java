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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.discovery.commons.providers.util.ResourceHelper;
import org.apache.sling.discovery.impl.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class for voting
 */
public class VotingHelper {

    private final static Logger logger = LoggerFactory
            .getLogger(VotingHelper.class);

    /**
     * List all the votings that are currently 'open' but 'not winning'.
     * <p>
     * 'Open' means that they have not expired yet, have zero no-votes,
     * and match the view that this instance has of the cluster.
     * <p>
     * 'Not winning' means that a voting still did not receive a vote
     * from everybody
     * @return the list of matching votings - never returns null
     */
    public static List<VotingView> listOpenNonWinningVotings(
            final ResourceResolver resourceResolver, final Config config) {
        if (config==null) {
            logger.info("listOpenNonWinningVotings: config is null, bundle likely deactivated.");
            return new ArrayList<VotingView>();
        }
        final String ongoingVotingsPath = config.getOngoingVotingsPath();
        final Resource ongoingVotingsResource = resourceResolver
                .getResource(ongoingVotingsPath);
        if (ongoingVotingsResource == null) {
            // it is legal that at this stage there is no ongoingvotings node yet 
            // for example when there was never a voting yet
            logger.debug("listOpenNonWinningVotings: no ongoing votings parent resource found");
            return new ArrayList<VotingView>();
        }
        final Iterable<Resource> children = ongoingVotingsResource.getChildren();
        final Iterator<Resource> it = children.iterator();
        final List<VotingView> result = new LinkedList<VotingView>();
        if (!it.hasNext()) {
            return result;
        }
        while (it.hasNext()) {
            Resource aChild = it.next();
            VotingView c = new VotingView(aChild);
            String matchesLiveView;
            try {
                matchesLiveView = c.matchesLiveView(config);
            } catch (Exception e) {
                logger.error("listOpenNonWinningVotings: could not compare voting with live view: "+e, e);
                continue;
            }
            boolean ongoingVoting = c.isOngoingVoting(config);
            boolean hasNoVotes = c.hasNoVotes();
            boolean isWinning = c.isWinning();
            if (matchesLiveView == null
                    && ongoingVoting && !hasNoVotes
                    && !isWinning) {
            	if (logger.isDebugEnabled()) {
	                logger.debug("listOpenNonWinningVotings: found an open voting: "
	                        + aChild
	                        + ", properties="
	                        + ResourceHelper.getPropertiesForLogging(aChild));
            	}
                result.add(c);
            } else {
            	if (logger.isDebugEnabled()) {
	                logger.debug("listOpenNonWinningVotings: a non-open voting: "
	                        + aChild
	                        + ", matches live: " + matchesLiveView
	                        + ", is ongoing: " + ongoingVoting
	                        + ", has no votes: " + hasNoVotes
	                        + ", is winning: " + isWinning
	                        + ", properties="
	                        + ResourceHelper.getPropertiesForLogging(aChild));
            	}
            }
        }
    	if (logger.isDebugEnabled()) {
	        logger.debug("listOpenNonWinningVotings: votings found: "
	                + result.size());
    	}
        return result;
    }

    /**
     * List all the votings that have timed out
     * @return the list of matching votings
     */
    public static List<VotingView> listTimedoutVotings(
            final ResourceResolver resourceResolver, final Config config) {
        final String ongoingVotingsPath = config.getOngoingVotingsPath();
        final Resource ongoingVotingsResource = resourceResolver
                .getResource(ongoingVotingsPath);
        if (ongoingVotingsResource == null) {
            logger.info("listTimedoutVotings: no ongoing votings parent resource found"); // TOOD - is this expected?
            return new ArrayList<VotingView>();
        }
        final Iterable<Resource> children = ongoingVotingsResource.getChildren();
        final Iterator<Resource> it = children.iterator();
        final List<VotingView> result = new LinkedList<VotingView>();
        if (!it.hasNext()) {
            return result;
        }
        while (it.hasNext()) {
            Resource aChild = it.next();
            VotingView c = new VotingView(aChild);
            if (c.isTimedoutVoting(config)) {
            	if (logger.isDebugEnabled()) {
	                logger.debug("listTimedoutVotings: found a timed-out voting: "
	                        + aChild
	                        + ", properties="
	                        + ResourceHelper.getPropertiesForLogging(aChild));
            	}
                result.add(c);
            }
        }
    	if (logger.isDebugEnabled()) {
	        logger.debug("listTimedoutVotings: votings found: "
	                + result.size());
    	}
        return result;
    }

    /**
     * Return the still valid (ongoing) and winning (received a yes vote
     * from everybody) voting
     * @return the valid and winning voting
     */
    public static VotingView getWinningVoting(
            final ResourceResolver resourceResolver, final Config config) {
        String ongoingVotingsPath = config.getOngoingVotingsPath();
        Resource ongoingVotingsResource = resourceResolver
                .getResource(ongoingVotingsPath);
        if (ongoingVotingsResource == null) {
            // it is legal that at this stage there is no ongoingvotings node yet 
            // for example when there was never a voting yet
            logger.debug("getWinningVoting: no ongoing votings parent resource found");
            return null;
        }
        Iterable<Resource> children = ongoingVotingsResource.getChildren();
        Iterator<Resource> it = children.iterator();
        List<VotingView> result = new LinkedList<VotingView>();
        while (it.hasNext()) {
            Resource aChild = it.next();
            VotingView c = new VotingView(aChild);
            boolean ongoing = c.isOngoingVoting(config);
            boolean winning = c.isWinning();
            if (ongoing && winning) {
            	if (logger.isDebugEnabled()) {
            		logger.debug("getWinningVoting: a winning voting: " + aChild);
            	}
                result.add(c);
            } else {
                logger.debug("getWinningVote: not winning: vote="+aChild+" is ongoing="+ongoing+", winning="+winning);
            }
        }
        if (result.size() == 1) {
            return result.get(0);
        } else {
            return null;
        }
    }

    /**
     * Returns the voting for which the given slingId has vote yes or was the
     * initiator (which is equal to yes).
     * @param slingId the instance for which its yes vote should be looked up
     * @return the voting for which the given slingId has votes yes or was the
     * initiator
     */
    public static List<VotingView> getYesVotingsOf(final ResourceResolver resourceResolver,
            final Config config,
            final String slingId) {
        if (resourceResolver == null) {
            throw new IllegalArgumentException("resourceResolver must not be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        if (slingId == null || slingId.length() == 0) {
            throw new IllegalArgumentException("slingId must not be null or empty");
        }
        final String ongoingVotingsPath = config.getOngoingVotingsPath();
        final Resource ongoingVotingsResource = resourceResolver
                .getResource(ongoingVotingsPath);
        if (ongoingVotingsResource == null) {
            return null;
        }
        final Iterable<Resource> children = ongoingVotingsResource.getChildren();
        if (children == null) {
            return null;
        }
        final Iterator<Resource> it = children.iterator();
        final List<VotingView> result = new LinkedList<VotingView>();
        while (it.hasNext()) {
            Resource aChild = it.next();
            VotingView c = new VotingView(aChild);
            if (c.hasVotedYes(slingId)) {
                result.add(c);
            }
        }
        if (result.size() >= 1) {
            // if result.size() is higher than 1, that means that there is more
            // than 1 yes vote
            // which can happen if the local instance is initiator of one
            // and has voted on another voting
            return result;
        } else {
            return null;
        }
    }

    public static List<VotingView> listVotings(ResourceResolver resourceResolver, Config config) {
        if (config==null) {
            logger.info("listVotings: config is null, bundle likely deactivated.");
            return new ArrayList<VotingView>();
        }
        final String ongoingVotingsPath = config.getOngoingVotingsPath();
        final Resource ongoingVotingsResource = resourceResolver
                .getResource(ongoingVotingsPath);
        if (ongoingVotingsResource == null) {
            // it is legal that at this stage there is no ongoingvotings node yet 
            // for example when there was never a voting yet
            logger.debug("listVotings: no ongoing votings parent resource found");
            return new ArrayList<VotingView>();
        }
        final Iterable<Resource> children = ongoingVotingsResource.getChildren();
        final Iterator<Resource> it = children.iterator();
        final List<VotingView> result = new LinkedList<VotingView>();
        while (it.hasNext()) {
            Resource aChild = it.next();
            VotingView c = new VotingView(aChild);
            result.add(c);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("listVotings: votings found: "
                    + result.size());
        }
        return result;
    }

}
