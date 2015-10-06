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
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.discovery.impl.Config;
import org.apache.sling.discovery.impl.common.View;
import org.apache.sling.discovery.impl.common.ViewHelper;
import org.apache.sling.discovery.impl.common.resource.ResourceHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DAO for an ongoing voting, providing a few helper methods
 */
public class VotingView extends View {

    /**
     * use static logger to avoid frequent initialization as is potentially the
     * case with ClusterViewResource.
     **/
    private final static Logger logger = LoggerFactory
            .getLogger(VotingView.class);

    /**
     * Create a new voting with the given list of instances, the given
     * voting/view id and the given slingid of the initiator.
     * @param newViewId the new voting/view id
     * @param initiatorId the slingid of the initiator
     * @param liveInstances the list of live instances to add to the voting
     * @return a DAO object representing the voting
     */
    public static VotingView newVoting(final ResourceResolver resourceResolver,
            final Config config,
            final String newViewId, String initiatorId, final Set<String> liveInstances) throws PersistenceException {
        final Resource votingResource = ResourceHelper.getOrCreateResource(
                resourceResolver, config.getOngoingVotingsPath() + "/"
                        + newViewId);
        final ModifiableValueMap votingMap = votingResource.adaptTo(ModifiableValueMap.class);
        votingMap.put("votingStart", Calendar.getInstance());

        String clusterId = null;
        Calendar clusterIdDefinedAt = null;
        String clusterIdDefinedBy = null;
        final View currentlyEstablishedView = ViewHelper.getEstablishedView(resourceResolver, config);
        if (currentlyEstablishedView != null) {
        	final ValueMap establishedViewValueMap = currentlyEstablishedView.getResource().adaptTo(ValueMap.class);
        	clusterId = establishedViewValueMap.get(VIEW_PROPERTY_CLUSTER_ID, String.class);
        	if (clusterId == null || clusterId.length() == 0) {
        		clusterId = currentlyEstablishedView.getResource().getName();
        	}
        	Date date = establishedViewValueMap.get(VIEW_PROPERTY_CLUSTER_ID_DEFINED_AT, Date.class);
        	if (date!=null) {
        		clusterIdDefinedAt = Calendar.getInstance();
        		clusterIdDefinedAt.setTime(date);
        	}
        	clusterIdDefinedBy = establishedViewValueMap.get(VIEW_PROPERTY_CLUSTER_ID_DEFINED_BY, String.class);
        }
        if (clusterId == null || clusterId.length() == 0) {
        	clusterId = newViewId;
        	clusterIdDefinedAt = Calendar.getInstance();
        }
        votingMap.put(VIEW_PROPERTY_CLUSTER_ID, clusterId);
        if (clusterIdDefinedAt != null) {
        	votingMap.put(VIEW_PROPERTY_CLUSTER_ID_DEFINED_AT, clusterIdDefinedAt);
        }
        if (clusterIdDefinedBy == null || clusterIdDefinedBy.length() == 0) {
        	clusterIdDefinedBy = initiatorId;
        }
        votingMap.put(VIEW_PROPERTY_CLUSTER_ID_DEFINED_BY, clusterIdDefinedBy);

        final Resource membersResource = resourceResolver.create(votingResource, "members", null);
        final Iterator<String> it = liveInstances.iterator();
        while (it.hasNext()) {
            String memberId = it.next();
            Map<String, Object> properties = new HashMap<String, Object>();
            if (memberId.equals(initiatorId)) {
                properties.put("initiator", true);
                properties.put("votedAt", Calendar.getInstance());
            }
            Resource instanceResource = ResourceHelper.getOrCreateResource(
                    resourceResolver, config.getClusterInstancesPath() + "/"
                            + memberId);
            String leaderElectionId = instanceResource.adaptTo(ValueMap.class)
                    .get("leaderElectionId", String.class);
            properties.put("leaderElectionId", leaderElectionId);

            resourceResolver.create(membersResource, memberId, properties);
        }
        resourceResolver.commit();
        logger.info("newVoting: new voting started: newViewId="+newViewId+", resource="+votingResource+", #members: "+liveInstances.size()+", members: "+liveInstances);
        return new VotingView(votingResource);
    }

    /**
     * Construct a voting view based on the given resource
     * @param viewResource the resource which is the place the voting is kept
     */
    public VotingView(final Resource viewResource) {
        super(viewResource);
    }

    @Override
    public String toString() {
        final Resource members = getResource().getChild("members");
        String initiatorId = null;
        final StringBuilder sb = new StringBuilder();
        if (members != null) {
            Iterator<Resource> it = members.getChildren().iterator();
            while (it.hasNext()) {
                Resource r = it.next();
                if (sb.length() != 0) {
                    sb.append(", ");
                }
                sb.append(r.getName());
                ValueMap properties = r.adaptTo(ValueMap.class);
                if (properties != null) {
                    Boolean initiator = properties.get("initiator",
                            Boolean.class);
                    if (initiator != null && initiator) {
                        initiatorId = r.getName();
                    }
                }
            }
        }
        return "a VotingView[viewId=" + getViewId() + ", initiator="
                + initiatorId + ", members=" + sb + "]";
    }

    /**
     * Checks whether this voting is still ongoing - that is, whether
     * a valid votingStart is set and whether that's within the heartbeat timeout configured
     * @param config
     * @return
     */
    public boolean isOngoingVoting(final Config config) {
        final long votingStart = getVotingStartTime();
        if (votingStart==-1) {
            return false;
        }
        final long now = System.currentTimeMillis();
        final long diff = now - votingStart;
        return diff < config.getHeartbeatTimeoutMillis();
    }

    /**
     * Checks whether this voting has timed out - that is, whether
     * there is a valid votingStart set and whether that has timed out
     */
    public boolean isTimedoutVoting(final Config config) {
        final long votingStart = getVotingStartTime();
        if (votingStart==-1) {
            return false;
        }
        final long now = System.currentTimeMillis();
        final long diff = now - votingStart;
        return diff > config.getHeartbeatTimeoutMillis();
    }

    /** Get the value of the votingStart property - or -1 if anything goes wrong reading that **/
    private long getVotingStartTime() {
        ValueMap properties = null;
        try{
            properties = getResource().adaptTo(ValueMap.class);
        } catch(RuntimeException e) {
            logger.info("getVotingStartTime: could not get properties of "+getResource()+". Likely in creation: "+e, e);
            return -1;
        }
        if (properties == null) {
            // no properties, odd. then it's not a valid voting.
            return -1;
        }
        final Date votingStartDate = properties.get("votingStart", Date.class);
        if (votingStartDate == null) {
        	if (logger.isDebugEnabled()) {
	            logger.debug("getVotingStartTime: got a voting without votingStart. Likely in creation: "
	                    + getResource());
        	}
            return -1;
        }
        final long votingStart = votingStartDate.getTime();
        return votingStart;
    }

    /**
     * Checks whether there are any no votes on this voting
     * @return true if there are any no votes on this voting
     */
    public boolean hasNoVotes() {
        Resource m = getResource().getChild("members");
        if (m==null) {
            // the vote is being created. wait.
            return false;
        }
        final Iterator<Resource> it = m.getChildren()
                .iterator();
        while (it.hasNext()) {
            Resource aMemberRes = it.next();
            ValueMap properties = aMemberRes.adaptTo(ValueMap.class);
            Boolean vote = properties.get("vote", Boolean.class);
            if (vote != null && !vote) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether the given slingId has voted yes or is the initiator of this voting
     * @param slingId the sling id to check for
     * @return true if the given slingId has voted yes or is the initiator of this voting
     */
    public boolean hasVotedOrIsInitiator(final String slingId) {
        Resource members = getResource().getChild("members");
        if (members==null) {
        	return false;
        }
		final Resource memberResource = members.getChild(
                slingId);
        if (memberResource == null) {
            return false;
        }
        final ValueMap properties = memberResource.adaptTo(ValueMap.class);
        if (properties == null) {
            return false;
        }
        final Boolean initiator = properties.get("initiator", Boolean.class);
        if (initiator != null && initiator) {
            return true;
        }
        final Boolean vote = properties.get("vote", Boolean.class);
        return vote != null && vote;
    }

    /**
     * Checks whether this voting was initiated by the given slingId
     * @return whether this voting was initiated by the given slingId
     */
    public boolean isInitiatedBy(final String slingId) {
        final Resource memberResource = getResource().getChild("members").getChild(
                slingId);
        if (memberResource == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("isInitiatedBy: slingId=" + slingId + ", memberResource null!");
            }
            return false;
        }
        final ValueMap properties = memberResource.adaptTo(ValueMap.class);
        if (properties == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("isInitiatedBy: slingId=" + slingId + ", properties null!");
            }
            return false;
        }
        final Boolean initiator = properties.get("initiator", Boolean.class);
        boolean result = initiator != null && initiator;
        if (logger.isDebugEnabled()) {
            logger.debug("isInitiatedBy: slingId=" + slingId + ", initiator=" + initiator + ", result=" + result);
        }
        return result;
    }

    /**
     * add a vote from the given slingId to this voting
     * @param slingId the slingId which is voting
     * @param vote true for a yes-vote, false for a no-vote
     */
    public void vote(final String slingId, final Boolean vote,
                     final String leaderElectionId) {
    	if (logger.isDebugEnabled()) {
    		logger.debug("vote: slingId=" + slingId + ", vote=" + vote);
    	}
        final Resource memberResource = getResource().getChild("members").getChild(
                slingId);
        if (memberResource == null) {
            logger.error("vote: no memberResource found for slingId=" + slingId
                    + ", vote=" + vote + ", resource=" + getResource());
            return;
        }
        final ModifiableValueMap memberMap = memberResource.adaptTo(ModifiableValueMap.class);

        if (vote == null) {
            logger.trace("vote: removing vote (vote==null)");
            memberMap.remove("vote");
        } else {
            boolean shouldVote = true;
            try {
                if (memberMap.containsKey("vote") && ((Property)memberMap.get("vote")).getBoolean()==vote) {
                    logger.debug("vote: already voted, with same vote ("+vote+"), not voting again");
                    shouldVote = false;
                }
            } catch (ValueFormatException e) {
                logger.warn("vote: got a ValueFormatException: "+e, e);
            } catch (RepositoryException e) {
                logger.warn("vote: got a RepositoryException: "+e, e);
            }
            if (shouldVote) {
                logger.info("vote: slingId=" + slingId + " is voting vote=" + vote+" on "+getResource());
                memberMap.put("vote", vote);
                memberMap.put("votedAt", Calendar.getInstance());
                String currentLeaderElectionId = memberMap.get("leaderElectionId", String.class);
                if (leaderElectionId!=null &&
                        (currentLeaderElectionId == null || !currentLeaderElectionId.equals(leaderElectionId))) {
                    // SLING-5030 : to ensure leader-step-down after being
                    // isolated from the cluster, the leaderElectionId must
                    // be explicitly set upon voting.
                    // for 99% of the cases not be necessary,
                    // for the rejoin-after-isolation case however it is
                    logger.info("vote: changing leaderElectionId on vote to "+leaderElectionId);
                    memberMap.put("leaderElectionId", leaderElectionId);
                    memberMap.put("leaderElectionIdCreatedAt", new Date());
                }
            }
        }
        try {
            getResource().getResourceResolver().commit();
        } catch (PersistenceException e) {
            logger.error("vote: PersistenceException while voting: "+e, e);
        }
    }

    /**
     * Checks whether this voting is winning - winning is when it has
     * votes from each of the members and all are yes votes
     * @return true if this voting is winning
     */
    public boolean isWinning() {
        final Resource members = getResource().getChild("members");
        if (members==null) {
            // the vote is being created. wait.
            return false;
        }
        try{
	        final Iterable<Resource> children = members.getChildren();
	        final Iterator<Resource> it = children.iterator();
	        boolean isWinning = false;
	        while (it.hasNext()) {
	            Resource aMemberRes = it.next();
	            try{
	                ValueMap properties = aMemberRes.adaptTo(ValueMap.class);
	                Boolean initiator = properties.get("initiator", Boolean.class);
	                Boolean vote = properties.get("vote", Boolean.class);
	                if (initiator != null && initiator) {
	                    isWinning = true;
	                    continue;
	                }
	                if (vote != null && vote) {
	                    isWinning = true;
	                    continue;
	                }
	                return false;
	            } catch(RuntimeException re) {
	                logger.info("isWinning: Could not check vote due to "+re);
	                return false;
	            }
	        }
	        return isWinning;
        } catch(RuntimeException re) {
        	// SLING-2945: gracefully handle case where members node is
        	//             deleted by another instance
        	logger.info("isWinning: could not check vote due to "+re);
        	return false;
        }
    }

    /**
     * Checks if this voting matches the current live view
     */
    public boolean matchesLiveView(final Config config) {
        Resource clusterNodesRes = getResource().getResourceResolver()
                .getResource(config.getClusterInstancesPath());
        if (clusterNodesRes == null) {
            return false;
        }
        return matchesLiveView(clusterNodesRes, config);
    }

}
