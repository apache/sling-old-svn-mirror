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
package org.apache.sling.event.impl.jobs.tasks;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.event.impl.jobs.JobTopicTraverser;
import org.apache.sling.event.impl.jobs.config.JobManagerConfiguration;
import org.apache.sling.event.impl.jobs.config.QueueConfigurationManager;
import org.apache.sling.event.impl.jobs.config.QueueConfigurationManager.QueueInfo;
import org.apache.sling.event.impl.jobs.config.TopologyCapabilities;
import org.apache.sling.event.impl.support.ResourceHelper;
import org.apache.sling.event.jobs.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The check topology task checks for changes in the topology and queue configuration
 * and reassigns jobs.
 * If the leader instance finds a dead instance it reassigns its jobs to live instances.
 * The leader instance also checks for unassigned jobs and tries to assign them.
 * If an instance detects jobs which it doesn't process anymore it reassigns them as
 * well.
 */
public class CheckTopologyTask {

    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** Job manager configuration. */
    private final JobManagerConfiguration configuration;

    /** The capabilities. */
    private final TopologyCapabilities caps;

    /**
     * Constructor
     * @param config The configuration
     */
    public CheckTopologyTask(final JobManagerConfiguration config) {
        this.configuration = config;
        this.caps = this.configuration.getTopologyCapabilities();
    }

    /**
     * Reassign jobs from stopped instance.
     */
    private void reassignJobsFromStoppedInstances() {
        if ( caps.isLeader() && caps.isActive() ) {
            this.logger.debug("Checking for stopped instances...");
            final ResourceResolver resolver = this.configuration.createResourceResolver();
            if ( resolver != null ) {
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
                                assignJobs(instanceResource, true);
                            }
                        }
                    }
                } finally {
                    resolver.close();
                }
            }
        }
    }

    /**
     * Reassign stale jobs from this instance
     */
    private void reassignStaleJobs() {
        if ( caps.isActive() ) {
            this.logger.debug("Checking for stale jobs...");
            final ResourceResolver resolver = this.configuration.createResourceResolver();
            if ( resolver != null ) {
                try {
                    final Resource jobsRoot = resolver.getResource(this.configuration.getLocalJobsPath());

                    // this resource should exist, but we check anyway
                    if ( jobsRoot != null ) {
                        final Iterator<Resource> topicIter = jobsRoot.listChildren();
                        while ( caps.isActive() && topicIter.hasNext() ) {
                            final Resource topicResource = topicIter.next();

                            final String topicName = topicResource.getName().replace('.', '/');
                            this.logger.debug("Checking topic {}..." , topicName);
                            final List<InstanceDescription> potentialTargets = caps.getPotentialTargets(topicName);
                            boolean reassign = true;
                            for(final InstanceDescription desc : potentialTargets) {
                                if ( desc.isLocal() ) {
                                    reassign = false;
                                    break;
                                }
                            }
                            if ( reassign ) {
                                final QueueConfigurationManager qcm = this.configuration.getQueueConfigurationManager();
                                if ( qcm == null ) {
                                    break;
                                }
                                final QueueInfo info = qcm.getQueueInfo(topicName);
                                JobTopicTraverser.traverse(this.logger, topicResource, new JobTopicTraverser.ResourceCallback() {

                                    @Override
                                    public boolean handle(final Resource rsrc) {
                                        try {
                                            final ValueMap vm = ResourceHelper.getValueMap(rsrc);
                                            final String targetId = caps.detectTarget(topicName, vm, info);

                                            final Map<String, Object> props = new HashMap<String, Object>(vm);
                                            props.remove(Job.PROPERTY_JOB_STARTED_TIME);

                                            final String newPath;
                                            if ( targetId != null ) {
                                                newPath = configuration.getAssginedJobsPath() + '/' + targetId + '/' + topicResource.getName() + rsrc.getPath().substring(topicResource.getPath().length());
                                                props.put(Job.PROPERTY_JOB_QUEUE_NAME, info.queueName);
                                                props.put(Job.PROPERTY_JOB_TARGET_INSTANCE, targetId);
                                            } else {
                                                newPath = configuration.getUnassignedJobsPath() + '/' + topicResource.getName() + rsrc.getPath().substring(topicResource.getPath().length());
                                                props.remove(Job.PROPERTY_JOB_QUEUE_NAME);
                                                props.remove(Job.PROPERTY_JOB_TARGET_INSTANCE);
                                            }
                                            try {
                                                ResourceHelper.getOrCreateResource(resolver, newPath, props);
                                                resolver.delete(rsrc);
                                                resolver.commit();
                                                final String jobId = vm.get(ResourceHelper.PROPERTY_JOB_ID, String.class);
                                                if ( targetId != null ) {
                                                    configuration.getAuditLogger().debug("REASSIGN OK {} : {}", targetId, jobId);
                                                } else {
                                                    configuration.getAuditLogger().debug("REUNASSIGN OK : {}", jobId);
                                                }
                                            } catch ( final PersistenceException pe ) {
                                                logger.warn("Unable to move stale job from " + rsrc.getPath() + " to " + newPath, pe);
                                                resolver.refresh();
                                                resolver.revert();
                                            }
                                        } catch (final InstantiationException ie) {
                                            // something happened with the resource in the meantime
                                            logger.warn("Unable to move stale job from " + rsrc.getPath(), ie);
                                            resolver.refresh();
                                            resolver.revert();
                                        }
                                        return caps.isActive();
                                    }
                                });

                            }
                        }
                    }
                } finally {
                    resolver.close();
                }
            }
        }
    }

    /**
     * Try to assign unassigned jobs as there might be changes in:
     * - queue configurations
     * - topology
     * - capabilities
     */
    public void assignUnassignedJobs() {
        if ( caps != null && caps.isLeader() && caps.isActive() ) {
            logger.debug("Checking unassigned jobs...");
            final ResourceResolver resolver = this.configuration.createResourceResolver();
            if ( resolver != null ) {
                try {
                    final Resource unassignedRoot = resolver.getResource(this.configuration.getUnassignedJobsPath());
                    logger.debug("Got unassigned root {}", unassignedRoot);

                    // this resource should exist, but we check anyway
                    if ( unassignedRoot != null ) {
                        assignJobs(unassignedRoot, false);
                    }
                } finally {
                    resolver.close();
                }
            }
        }
    }

    /**
     * Try to assign all jobs from the jobs root.
     * The jobs are stored by topic
     * @param jobsRoot The root of the jobs
     * @param unassign Whether to unassign the job if no instance is found.
     */
    private void assignJobs(final Resource jobsRoot,
            final boolean unassign) {
        final ResourceResolver resolver = jobsRoot.getResourceResolver();

        final Iterator<Resource> topicIter = jobsRoot.listChildren();
        while ( caps.isActive() && topicIter.hasNext() ) {
            final Resource topicResource = topicIter.next();

            final String topicName = topicResource.getName().replace('.', '/');
            logger.debug("Found topic {}", topicName);

            // first check if there is an instance for these topics
            final List<InstanceDescription> potentialTargets = caps.getPotentialTargets(topicName);
            if ( potentialTargets != null && potentialTargets.size() > 0 ) {
                final QueueConfigurationManager qcm = this.configuration.getQueueConfigurationManager();
                if ( qcm == null ) {
                    break;
                }
                final QueueInfo info = qcm.getQueueInfo(topicName);
                logger.debug("Found queue {} for {}", info.queueConfiguration, topicName);

                JobTopicTraverser.traverse(this.logger, topicResource, new JobTopicTraverser.ResourceCallback() {

                    @Override
                    public boolean handle(final Resource rsrc) {
                        try {
                            final ValueMap vm = ResourceHelper.getValueMap(rsrc);
                            final String targetId = caps.detectTarget(topicName, vm, info);

                            if ( targetId != null ) {
                                final String newPath = configuration.getAssginedJobsPath() + '/' + targetId + '/' + topicResource.getName() + rsrc.getPath().substring(topicResource.getPath().length());
                                final Map<String, Object> props = new HashMap<String, Object>(vm);
                                props.put(Job.PROPERTY_JOB_QUEUE_NAME, info.queueName);
                                props.put(Job.PROPERTY_JOB_TARGET_INSTANCE, targetId);
                                props.remove(Job.PROPERTY_JOB_STARTED_TIME);
                                try {
                                    ResourceHelper.getOrCreateResource(resolver, newPath, props);
                                    resolver.delete(rsrc);
                                    resolver.commit();
                                    final String jobId = vm.get(ResourceHelper.PROPERTY_JOB_ID, String.class);
                                    configuration.getAuditLogger().debug("REASSIGN OK {} : {}", targetId, jobId);
                                } catch ( final PersistenceException pe ) {
                                    logger.warn("Unable to move unassigned job from " + rsrc.getPath() + " to " + newPath, pe);
                                    resolver.refresh();
                                    resolver.revert();
                                }
                            }
                        } catch (final InstantiationException ie) {
                            // something happened with the resource in the meantime
                            logger.warn("Unable to move unassigned job from " + rsrc.getPath(), ie);
                            resolver.refresh();
                            resolver.revert();
                        }
                        return caps.isActive();
                    }
                });
            }
            // now unassign if there are still jobs
            if ( caps.isActive() && unassign ) {
                // we have to move everything to the unassigned area
                JobTopicTraverser.traverse(this.logger, topicResource, new JobTopicTraverser.ResourceCallback() {

                    @Override
                    public boolean handle(final Resource rsrc) {
                        try {
                            final ValueMap vm = ResourceHelper.getValueMap(rsrc);
                            final String newPath = configuration.getUnassignedJobsPath() + '/' + topicResource.getName() + rsrc.getPath().substring(topicResource.getPath().length());
                            final Map<String, Object> props = new HashMap<String, Object>(vm);
                            props.remove(Job.PROPERTY_JOB_QUEUE_NAME);
                            props.remove(Job.PROPERTY_JOB_TARGET_INSTANCE);
                            props.remove(Job.PROPERTY_JOB_STARTED_TIME);

                            try {
                                ResourceHelper.getOrCreateResource(resolver, newPath, props);
                                resolver.delete(rsrc);
                                resolver.commit();
                                final String jobId = vm.get(ResourceHelper.PROPERTY_JOB_ID, String.class);
                                configuration.getAuditLogger().debug("REUNASSIGN OK : {}", jobId);
                            } catch ( final PersistenceException pe ) {
                                logger.warn("Unable to unassigned job from " + rsrc.getPath() + " to " + newPath, pe);
                                resolver.refresh();
                                resolver.revert();
                            }
                        } catch (final InstantiationException ie) {
                            // something happened with the resource in the meantime
                            logger.warn("Unable to unassigned job from " + rsrc.getPath(), ie);
                            resolver.refresh();
                            resolver.revert();
                        }
                        return caps.isActive();
                    }
                });
            }
        }
    }

    /**
     * One maintenance run
     */
    public void fullRun() {
        if ( this.caps != null ) {
            this.reassignJobsFromStoppedInstances();

            // check for all topics
            this.reassignStaleJobs();

            // try to assign unassigned jobs
            this.assignUnassignedJobs();
        }
    }
}
