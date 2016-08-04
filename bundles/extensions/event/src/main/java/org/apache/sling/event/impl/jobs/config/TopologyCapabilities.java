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
package org.apache.sling.event.impl.jobs.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.TopologyView;
import org.apache.sling.event.impl.jobs.config.QueueConfigurationManager.QueueInfo;
import org.apache.sling.event.impl.support.Environment;
import org.apache.sling.event.jobs.QueueConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The capabilities of a topology.
 */
public class TopologyCapabilities {

    public static final String PROPERTY_TOPICS = "org.apache.sling.event.jobs.consumer.topics";

    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** Map: key: topic, value: sling IDs */
    private final Map<String, List<InstanceDescription>> instanceCapabilities;

    /** Round robin map. */
    private final Map<String, Integer> roundRobinMap = new HashMap<String, Integer>();

    /** Instance map. */
    private final Map<String, InstanceDescription> instanceMap = new HashMap<String, InstanceDescription>();

    /** Is this the leader of the cluster? */
    private final boolean isLeader;

    /** Is this still active? */
    private volatile boolean active = true;

    /** All instances. */
    private final Map<String, String> allInstances;

    /** Instance comparator. */
    private final InstanceDescriptionComparator instanceComparator;

    /** JobManagerConfiguration. */
    private final JobManagerConfiguration jobManagerConfiguration;

    /** Topology view. */
    private final TopologyView view;

    public static final class InstanceDescriptionComparator implements Comparator<InstanceDescription> {

        private final String localClusterId;


        public InstanceDescriptionComparator(final String clusterId) {
            this.localClusterId = clusterId;
        }

        @Override
        public int compare(final InstanceDescription o1, final InstanceDescription o2) {
            if ( o1.getSlingId().equals(o2.getSlingId()) ) {
                return 0;
            }
            final boolean o1IsLocalCluster = localClusterId.equals(o1.getClusterView().getId());
            final boolean o2IsLocalCluster = localClusterId.equals(o2.getClusterView().getId());
            if ( o1IsLocalCluster && !o2IsLocalCluster ) {
                return -1;
            }
            if ( !o1IsLocalCluster && o2IsLocalCluster ) {
                return 1;
            }
            if ( o1IsLocalCluster ) {
                if ( o1.isLeader() && !o2.isLeader() ) {
                    return -1;
                } else if ( o2.isLeader() && !o1.isLeader() ) {
                    return 1;
                }
            }
            return o1.getSlingId().compareTo(o2.getSlingId());
        }
    }

    public static Map<String, String> getAllInstancesMap(final TopologyView view) {
        final Map<String, String> allInstances = new TreeMap<String, String>();

        for(final InstanceDescription desc : view.getInstances() ) {
            final String topics = desc.getProperty(PROPERTY_TOPICS);
            if ( topics != null && topics.length() > 0 ) {
                allInstances.put(desc.getSlingId(), topics);
            } else {
                allInstances.put(desc.getSlingId(), "");
            }
        }
        return allInstances;
    }

    /**
     * Create a new instance
     * @param view The new view
     * @param config The current job manager configuration.
     */
    public TopologyCapabilities(final TopologyView view,
            final JobManagerConfiguration config) {
        this.jobManagerConfiguration = config;
        this.instanceComparator = new InstanceDescriptionComparator(view.getLocalInstance().getClusterView().getId());
        this.isLeader = view.getLocalInstance().isLeader();
        this.allInstances = getAllInstancesMap(view);
        final Map<String, List<InstanceDescription>> newCaps = new HashMap<String, List<InstanceDescription>>();
        for(final InstanceDescription desc : view.getInstances() ) {
            final String topics = desc.getProperty(PROPERTY_TOPICS);
            if ( topics != null && topics.length() > 0 ) {
                this.logger.debug("Detected capabilities of {} : {}", desc.getSlingId(), topics);
                for(final String topic : topics.split(",") ) {
                    List<InstanceDescription> list = newCaps.get(topic);
                    if ( list == null ) {
                        list = new ArrayList<InstanceDescription>();
                        newCaps.put(topic, list);
                    }
                    list.add(desc);
                    Collections.sort(list, this.instanceComparator);
                }
            }
            this.instanceMap.put(desc.getSlingId(), desc);
        }
        this.instanceCapabilities = newCaps;
        this.view = view;
    }

    /**
     * Is this capabilities the same as represented by the provided instance map?
     * @param newAllInstancesMap The instance map
     * @return {@code true} if they represent the same state.
     */
    public boolean isSame(final Map<String, String> newAllInstancesMap) {
        return this.allInstances.equals(newAllInstancesMap);
    }

    /**
     * Deactivate this object.
     */
    public void deactivate() {
        this.active = false;
    }

    /**
     * Is this object still active?
     * If it is not active anymore it should not be used!
     * @return {@code true} if still active.
     */
    public boolean isActive() {
        return this.active && this.jobManagerConfiguration.isActive() && this.view.isCurrent();
    }

    /**
     * Is this instance still active?
     * @param instanceId The instance id
     * @return {@code true} if the instance is active.
     */
    public boolean isActive(final String instanceId) {
        return this.allInstances.containsKey(instanceId);
    }
    /**
     * Is the current instance the leader?
     */
    public boolean isLeader() {
        return this.isLeader;
    }

    /**
     * Add instances to the list if not already included
     */
    private void addAll(final List<InstanceDescription> potentialTargets, final List<InstanceDescription> newTargets) {
        if ( newTargets != null ) {
            for(final InstanceDescription desc : newTargets) {
                boolean found = false;
                for(final InstanceDescription existingDesc : potentialTargets) {
                    if ( desc.getSlingId().equals(existingDesc.getSlingId()) ) {
                        found = true;
                        break;
                    }
                }
                if ( !found ) {
                    potentialTargets.add(desc);
                }
            }
        }
    }

    /**
     * Return the potential targets (Sling IDs) sorted by ID
     * @return A list of instance descriptions. The list might be empty.
     */
    public List<InstanceDescription> getPotentialTargets(final String jobTopic) {
        // calculate potential targets
        final List<InstanceDescription> potentialTargets = new ArrayList<InstanceDescription>();

        // first: topic targets - directly handling the topic
        addAll(potentialTargets, this.instanceCapabilities.get(jobTopic));

        // second: category targets - handling the topic category
        int pos = jobTopic.lastIndexOf('/');
        if ( pos > 0 ) {
            final String category = jobTopic.substring(0, pos + 1).concat("*");
            addAll(potentialTargets, this.instanceCapabilities.get(category));

            // search deep consumers (since 1.2 of the consumer package)
            do {
                final String subCategory = jobTopic.substring(0, pos + 1).concat("**");
                addAll(potentialTargets, this.instanceCapabilities.get(subCategory));

                pos = jobTopic.lastIndexOf('/', pos - 1);
            } while ( pos > 0 );
        }
        Collections.sort(potentialTargets, this.instanceComparator);

        return potentialTargets;
    }

    /**
     * Detect the target instance.
     */
    public String detectTarget(final String jobTopic, final Map<String, Object> jobProperties,
            final QueueInfo queueInfo) {
        final List<InstanceDescription> potentialTargets = this.getPotentialTargets(jobTopic);
        logger.debug("Potential targets for {} : {}", jobTopic, potentialTargets);
        String createdOn = null;
        if ( jobProperties != null ) {
            createdOn = (String) jobProperties.get(org.apache.sling.event.jobs.Job.PROPERTY_JOB_CREATED_INSTANCE);
        }
        if ( createdOn == null ) {
            createdOn = Environment.APPLICATION_ID;
        }
        final InstanceDescription createdOnInstance = this.instanceMap.get(createdOn);

        if ( potentialTargets != null && potentialTargets.size() > 0 ) {
            if ( createdOnInstance != null ) {
                // create a list with local targets first.
                final List<InstanceDescription> localTargets = new ArrayList<InstanceDescription>();
                for(final InstanceDescription desc : potentialTargets) {
                    if ( desc.getClusterView().getId().equals(createdOnInstance.getClusterView().getId()) ) {
                        if ( !this.jobManagerConfiguration.disableDistribution() || desc.isLeader() ) {
                            localTargets.add(desc);
                        }
                    }
                }
                if ( localTargets.size() > 0 ) {
                    potentialTargets.clear();
                    potentialTargets.addAll(localTargets);
                    logger.debug("Potential targets filtered for {} : {}", jobTopic, potentialTargets);
                }
            }
            // check prefer run on creation instance
            if ( queueInfo.queueConfiguration.isPreferRunOnCreationInstance() ) {
                InstanceDescription creationDesc = null;
                for(final InstanceDescription desc : potentialTargets) {
                    if ( desc.getSlingId().equals(createdOn) ) {
                        creationDesc = desc;
                        break;
                    }
                }
                if ( creationDesc != null ) {
                    potentialTargets.clear();
                    potentialTargets.add(creationDesc);
                    logger.debug("Potential targets reduced to creation instance for {} : {}", jobTopic, potentialTargets);
                }
            }
            if ( queueInfo.queueConfiguration.getType() == QueueConfiguration.Type.ORDERED ) {
                // for ordered queues we always pick the first as we have to pick the same target on each cluster view
                // on all instances (TODO - we could try to do some round robin of the whole queue)
                final String result = potentialTargets.get(0).getSlingId();
                logger.debug("Target for {} : {}", jobTopic, result);

                return result;
            }
            // TODO - this is a simple round robin which is not based on the actual load
            //        of the instances
            Integer index = this.roundRobinMap.get(jobTopic);
            if ( index == null ) {
                index = 0;
            }
            if ( index >= potentialTargets.size() ) {
                index = 0;
            }
            this.roundRobinMap.put(jobTopic, index + 1);
            final String result = potentialTargets.get(index).getSlingId();
            logger.debug("Target for {} : {}", jobTopic, result);
            return result;
        }

        return null;
    }

    /**
     * Get the instance capabilities.
     * @return The map of instance capabilities.
     */
    public Map<String, List<InstanceDescription>> getInstanceCapabilities() {
        return this.instanceCapabilities;
    }
}
