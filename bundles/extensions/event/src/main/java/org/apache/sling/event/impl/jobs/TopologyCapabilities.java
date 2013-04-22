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
package org.apache.sling.event.impl.jobs;

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
import org.apache.sling.event.jobs.JobConsumer;
import org.apache.sling.event.jobs.QueueConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The capabilities of a topology.
 */
public class TopologyCapabilities {

    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** Map: key: topic, value: sling IDs */
    private final Map<String, List<InstanceDescription>> instanceCapabilities;

    /** Round robin map. */
    private final Map<String, Integer> roundRobinMap = new HashMap<String, Integer>();

    /** Is this the leader of the cluster? */
    private final boolean isLeader;

    /** Is this still active? */
    private volatile boolean active = true;

    /** Change count. */
    private final long changeCount;

    /** All instances. */
    private final Map<String, String> allInstances;

    private static final class InstanceDescriptionComparator implements Comparator<InstanceDescription> {

        @Override
        public int compare(final InstanceDescription o1, final InstanceDescription o2) {
            if ( o1.getSlingId().equals(o2.getSlingId()) ) {
                return 0;
            }
            if ( o1.isLeader() ) {
                return -1;
            } else if ( o2.isLeader() ) {
                return 1;
            }
            return o1.getSlingId().compareTo(o2.getSlingId());
        }
    }
    private static final InstanceDescriptionComparator COMPARATOR = new InstanceDescriptionComparator();

    public static Map<String, String> getAllInstancesMap(final TopologyView view) {
        final Map<String, String> allInstances = new TreeMap<String, String>();

        for(final InstanceDescription desc : view.getInstances() ) {
            final String topics = desc.getProperty(JobConsumer.PROPERTY_TOPICS);
            if ( topics != null && topics.length() > 0 ) {
                allInstances.put(desc.getSlingId(), topics);
            } else {
                allInstances.put(desc.getSlingId(), "");
            }
        }
        return allInstances;
    }

    public TopologyCapabilities(final TopologyView view, final long changeCount) {
        this.changeCount = changeCount;
        this.isLeader = view.getLocalInstance().isLeader();
        this.allInstances = getAllInstancesMap(view);
        final Map<String, List<InstanceDescription>> newCaps = new HashMap<String, List<InstanceDescription>>();
        for(final InstanceDescription desc : view.getInstances() ) {
            final String topics = desc.getProperty(JobConsumer.PROPERTY_TOPICS);
            if ( topics != null && topics.length() > 0 ) {
                this.logger.info("Capabilities of {} : {}", desc.getSlingId(), topics); // TODO debug
                for(final String topic : topics.split(",") ) {
                    List<InstanceDescription> list = newCaps.get(topic);
                    if ( list == null ) {
                        list = new ArrayList<InstanceDescription>();
                        newCaps.put(topic, list);
                    }
                    list.add(desc);
                    Collections.sort(list, COMPARATOR);
                }
            }
        }
        this.instanceCapabilities = newCaps;
    }

    public boolean isSame(final Map<String, String> newAllInstancesMap) {
        return this.allInstances.equals(newAllInstancesMap);
    }

    public void deactivate() {
        this.active = false;
    }

    public boolean isActive() {
        return this.active;
    }

    public long getChangeCount() {
        return this.changeCount;
    }

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
     * Return the potential targets (Sling IDs) sorted by ID
     */
    public List<InstanceDescription> getPotentialTargets(final String jobTopic, final Map<String, Object> jobProperties) {
        // calculate potential targets
        final List<InstanceDescription> potentialTargets = new ArrayList<InstanceDescription>();

        // first: topic targets - directly handling the topic
        final List<InstanceDescription> topicTargets = this.instanceCapabilities.get(jobTopic);
        if ( topicTargets != null ) {
            potentialTargets.addAll(topicTargets);
        }
        // second: category targets - handling the topic category
        final int pos = jobTopic.lastIndexOf('/');
        if ( pos > 0 ) {
            final String category = jobTopic.substring(0, pos + 1).concat("*");
            final List<InstanceDescription> categoryTargets = this.instanceCapabilities.get(category);
            if ( categoryTargets != null ) {
                potentialTargets.addAll(categoryTargets);
            }
        }
        // third: bridged consumers
        final List<InstanceDescription> bridgedTargets = (jobProperties != null && jobProperties.containsKey(JobImpl.PROPERTY_BRIDGED_EVENT) ? this.instanceCapabilities.get("/") : null);
        if ( bridgedTargets != null ) {
            potentialTargets.addAll(bridgedTargets);
        }
        Collections.sort(potentialTargets, COMPARATOR);

        return potentialTargets;
    }

    /**
     * Detect the target instance.
     */
    public String detectTarget(final String jobTopic, final Map<String, Object> jobProperties,
            final QueueInfo queueInfo) {
        final List<InstanceDescription> potentialTargets = this.getPotentialTargets(jobTopic, jobProperties);

        if ( potentialTargets != null && potentialTargets.size() > 0 ) {
            if ( queueInfo.queueConfiguration.getType() == QueueConfiguration.Type.ORDERED ) {
                // for ordered queues we always pick the first as we have to pick the same target
                // on all instances (TODO - we could try to do some round robin of the whole queue)
                return potentialTargets.get(0).getSlingId();
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
            return potentialTargets.get(index).getSlingId();
        }

        return null;
    }
}
