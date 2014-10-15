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
package org.apache.sling.event.impl.jobs.topology;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.event.impl.jobs.JobImpl;
import org.apache.sling.event.impl.jobs.JobManagerConfiguration;
import org.apache.sling.event.impl.jobs.config.QueueConfigurationManager;
import org.apache.sling.event.impl.jobs.config.QueueConfigurationManager.QueueInfo;
import org.apache.sling.event.impl.support.ResourceHelper;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.QueueConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maintenance task...
 *
 * In the default configuration, this task runs every minute
 */
public class MaintenanceTask {

    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** Job manager configuration. */
    private final JobManagerConfiguration configuration;

    /**
     * Constructor
     */
    public MaintenanceTask(final JobManagerConfiguration config) {
        this.configuration = config;
    }

    private void reassignJobs(final TopologyCapabilities caps,
            final QueueConfigurationManager queueManager) {
        if ( caps != null && caps.isLeader() ) {
            this.logger.debug("Checking for stopped instances...");
            final ResourceResolver resolver = this.configuration.createResourceResolver();
            try {
                final Resource jobsRoot = resolver.getResource(this.configuration.getAssginedJobsPath());
                this.logger.debug("Got jobs root {}", jobsRoot);

                // this resource should exist, but we check anyway
                if ( jobsRoot != null ) {
                    final Iterator<Resource> instanceIter = jobsRoot.listChildren();
                    while ( caps.isActive() && instanceIter.hasNext() ) {
                        final Resource instanceResource = instanceIter.next();

                        final String instanceId = instanceResource.getName();
                        if ( !caps.isActive(instanceId) ) {
                            logger.debug("Found stopped instance {}", instanceId);
                            assignJobs(caps, queueManager, instanceResource, true);
                        }
                    }
                }
            } finally {
                resolver.close();
            }
        }
    }

    /**
     * Try to assign unassigned jobs as there might be changes in:
     * - queue configurations
     * - topology
     * - capabilities
     */
    private void assignUnassignedJobs(final TopologyCapabilities caps,
            final QueueConfigurationManager queueManager) {
        if ( caps != null && caps.isLeader() ) {
            logger.debug("Checking unassigned jobs...");
            final ResourceResolver resolver = this.configuration.createResourceResolver();
            try {
                final Resource unassignedRoot = resolver.getResource(this.configuration.getUnassignedJobsPath());
                logger.debug("Got unassigned root {}", unassignedRoot);

                // this resource should exist, but we check anyway
                if ( unassignedRoot != null ) {
                    assignJobs(caps, queueManager, unassignedRoot, false);
                }
            } finally {
                resolver.close();
            }
        }
    }

    /**
     * Try to assign all jobs from the jobs root.
     * The jobs are stored by topic
     */
    private void assignJobs(final TopologyCapabilities caps,
            final QueueConfigurationManager queueManager,
            final Resource jobsRoot,
            final boolean unassign) {
        final ResourceResolver resolver = jobsRoot.getResourceResolver();

        final Iterator<Resource> topicIter = jobsRoot.listChildren();
        while ( caps.isActive() && topicIter.hasNext() ) {
            final Resource topicResource = topicIter.next();

            final String topicName = topicResource.getName().replace('.', '/');
            logger.debug("Found topic {}", topicName);

            final String checkTopic;
            if ( topicName.equals(JobImpl.PROPERTY_BRIDGED_EVENT) ) {
                checkTopic = "/";
            } else {
                checkTopic = topicName;
            }

            // first check if there is an instance for these topics
            final List<InstanceDescription> potentialTargets = caps.getPotentialTargets(checkTopic, null);
            if ( potentialTargets != null && potentialTargets.size() > 0 ) {
                final QueueInfo info = queueManager.getQueueInfo(topicName);
                logger.debug("Found queue {} for {}", info.queueConfiguration, topicName);

                // if queue is configured to drop, we drop
                if ( info.queueConfiguration.getType() ==  QueueConfiguration.Type.DROP) {
                    final Iterator<Resource> i = topicResource.listChildren();
                    while ( caps.isActive() && i.hasNext() ) {
                        final Resource rsrc = i.next();
                        try {
                            resolver.delete(rsrc);
                            resolver.commit();
                        } catch ( final PersistenceException pe ) {
                            this.ignoreException(pe);
                            resolver.refresh();
                        }
                    }
                } else if ( info.queueConfiguration.getType() != QueueConfiguration.Type.IGNORE ) {
                    // if the queue is not configured to ignore, we can reschedule
                    for(final Resource yearResource : topicResource.getChildren() ) {
                        for(final Resource monthResource : yearResource.getChildren() ) {
                            for(final Resource dayResource : monthResource.getChildren() ) {
                                for(final Resource hourResource : dayResource.getChildren() ) {
                                    for(final Resource minuteResource : hourResource.getChildren() ) {
                                        for(final Resource rsrc : minuteResource.getChildren() ) {

                                            if ( !caps.isActive() ) {
                                                return;
                                            }

                                            try {
                                                final ValueMap vm = ResourceHelper.getValueMap(rsrc);
                                                final String targetId = caps.detectTarget(topicName, vm, info);

                                                if ( targetId != null ) {
                                                    final String newPath = this.configuration.getAssginedJobsPath() + '/' + targetId + '/' + topicResource.getName() + rsrc.getPath().substring(topicResource.getPath().length());
                                                    final Map<String, Object> props = new HashMap<String, Object>(vm);
                                                    props.put(Job.PROPERTY_JOB_QUEUE_NAME, info.queueName);
                                                    props.put(Job.PROPERTY_JOB_TARGET_INSTANCE, targetId);
                                                    props.remove(Job.PROPERTY_JOB_STARTED_TIME);
                                                    try {
                                                        ResourceHelper.getOrCreateResource(resolver, newPath, props);
                                                        resolver.delete(rsrc);
                                                        resolver.commit();
                                                    } catch ( final PersistenceException pe ) {
                                                        this.ignoreException(pe);
                                                        resolver.refresh();
                                                    }
                                                }
                                            } catch (final InstantiationException ie) {
                                                // something happened with the resource in the meantime
                                                this.ignoreException(ie);
                                                resolver.refresh();
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if ( caps.isActive() && unassign ) {
                // we have to move everything to the unassigned area
                for(final Resource yearResource : topicResource.getChildren() ) {
                    for(final Resource monthResource : yearResource.getChildren() ) {
                        for(final Resource dayResource : monthResource.getChildren() ) {
                            for(final Resource hourResource : dayResource.getChildren() ) {
                                for(final Resource minuteResource : hourResource.getChildren() ) {
                                    for(final Resource rsrc : minuteResource.getChildren() ) {

                                        if ( !caps.isActive() ) {
                                            return;
                                        }

                                        try {
                                            final ValueMap vm = ResourceHelper.getValueMap(rsrc);
                                            final String newPath = this.configuration.getUnassignedJobsPath() + '/' + topicResource.getName() + rsrc.getPath().substring(topicResource.getPath().length());
                                            final Map<String, Object> props = new HashMap<String, Object>(vm);
                                            props.remove(Job.PROPERTY_JOB_QUEUE_NAME);
                                            props.remove(Job.PROPERTY_JOB_TARGET_INSTANCE);
                                            props.remove(Job.PROPERTY_JOB_STARTED_TIME);

                                            try {
                                                ResourceHelper.getOrCreateResource(resolver, newPath, props);
                                                resolver.delete(rsrc);
                                                resolver.commit();
                                            } catch ( final PersistenceException pe ) {
                                                this.ignoreException(pe);
                                                resolver.refresh();
                                            }
                                        } catch (final InstantiationException ie) {
                                            // something happened with the resource in the meantime
                                            this.ignoreException(ie);
                                            resolver.refresh();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * One maintenance run
     */
    public void run(final TopologyCapabilities topologyCapabilities,
            final QueueConfigurationManager queueManager,
            final boolean topologyChanged,
            final boolean configChanged) {
        // if topology changed, reschedule assigned jobs for stopped instances
        if ( topologyChanged ) {
            this.reassignJobs(topologyCapabilities, queueManager);
        }
        // try to assign unassigned jobs
        if ( topologyChanged || configChanged ) {
            this.assignUnassignedJobs(topologyCapabilities, queueManager);
        }
    }

    /**
     * Helper method which just logs the exception in debug mode.
     * @param e
     */
    private void ignoreException(final Exception e) {
        if ( this.logger.isDebugEnabled() ) {
            this.logger.debug("Ignored exception " + e.getMessage(), e);
        }
    }
}
