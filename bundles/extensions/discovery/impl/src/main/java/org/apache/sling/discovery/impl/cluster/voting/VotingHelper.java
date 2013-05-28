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
import org.apache.sling.discovery.impl.Config;
import org.apache.sling.discovery.impl.common.resource.ResourceHelper;
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
     * @return the list of matching votings
     */
    public static List<VotingView> listOpenNonWinningVotings(
            final ResourceResolver resourceResolver, final Config config) {
        final String ongoingVotingsPath = config.getOngoingVotingsPath();
        final Resource ongoingVotingsResource = resourceResolver
                .getResource(ongoingVotingsPath);
        if (ongoingVotingsResource == null) {
            logger.info("listOpenNonWinningVotings: no ongoing votings parent resource found"); // TOOD - is this expected?
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
            if (c.matchesLiveView(config)
                    && c.isOngoingVoting(config) && !c.hasNoVotes()
                    && !c.isWinning()) {
            	if (logger.isDebugEnabled()) {
	                logger.debug("listOpenNonWinningVotings: found a valid voting: "
	                        + aChild
	                        + ", properties="
	                        + ResourceHelper.getPropertiesForLogging(aChild));
            	}
                result.add(c);
            } else {
            	if (logger.isDebugEnabled()) {
	                logger.debug("listOpenNonWinningVotings: found an invalid voting: "
	                        + aChild
	                        + ", matches live: " + c.matchesLiveView(config)
	                        + ", is ongoing: " + c.isOngoingVoting(config)
	                        + ", has no votes: " + c.hasNoVotes()
	                        + ", is winning: " + c.isWinning()
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
            logger.info("getWinningVoting: no ongoing votings parent resource found"); // TOOD - is this expected?
            return null;
        }
        Iterable<Resource> children = ongoingVotingsResource.getChildren();
        Iterator<Resource> it = children.iterator();
        List<VotingView> result = new LinkedList<VotingView>();
        while (it.hasNext()) {
            Resource aChild = it.next();
            VotingView c = new VotingView(aChild);
            if (c.isOngoingVoting(config) && c.isWinning()) {
            	if (logger.isDebugEnabled()) {
            		logger.debug("getWinningVoting: a winning voting: " + c);
            	}
                result.add(c);
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
    public static VotingView getYesVotingOf(final ResourceResolver resourceResolver,
            final Config config,
            final String slingId) {
        final String ongoingVotingsPath = config.getOngoingVotingsPath();
        final Resource ongoingVotingsResource = resourceResolver
                .getResource(ongoingVotingsPath);
        final Iterable<Resource> children = ongoingVotingsResource.getChildren();
        final Iterator<Resource> it = children.iterator();
        final List<VotingView> result = new LinkedList<VotingView>();
        while (it.hasNext()) {
            Resource aChild = it.next();
            VotingView c = new VotingView(aChild);
            if (c.hasVotedOrIsInitiator(slingId)) {
                result.add(c);
            }
        }
        if (result.size() >= 1) {
            // if result.size() is higher than 1, that means that there is more
            // than 1 yes vote
            // from myself - which is a bug!
            return result.get(0);
        } else {
            return null;
        }
    }

}
