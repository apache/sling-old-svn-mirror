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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
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
import org.apache.sling.event.impl.support.Environment;
import org.apache.sling.event.impl.support.ResourceHelper;
import org.apache.sling.event.jobs.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Upgrade task
 *
 * Upgrade jobs from earlier versions to the new format.
 */
public class UpgradeTask {

    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** Job manager configuration. */
    private final JobManagerConfiguration configuration;

    /** The capabilities. */
    private final TopologyCapabilities caps;

    /**
     * Constructor
     * @param config the configuration
     */
    public UpgradeTask(final JobManagerConfiguration config) {
        this.configuration = config;
        this.caps = this.configuration.getTopologyCapabilities();
    }

    /**
     * Upgrade
     */
    public void run() {
        if ( caps.isLeader() ) {
            this.processJobsFromPreviousVersions();
        }
        this.upgradeBridgedJobs();
    }

    /**
     * Upgrade bridged jobs.
     * In previous versions, bridged jobs were stored under a special topic.
     * This has changed, the jobs are now stored with their real topic.
     */
    private void upgradeBridgedJobs() {
        final String path = configuration.getLocalJobsPath() + "/slingevent:eventadmin";
        final ResourceResolver resolver = configuration.createResourceResolver();
        if ( resolver != null ) {
            try {
                final Resource rootResource = resolver.getResource(path);
                if ( rootResource != null ) {
                    upgradeBridgedJobs(rootResource);
                }
                if ( caps.isLeader() ) {
                    final Resource unassignedRoot = resolver.getResource(configuration.getUnassignedJobsPath() + "/slingevent:eventadmin");
                    if ( unassignedRoot != null ) {
                        upgradeBridgedJobs(unassignedRoot);
                    }
                }
            } finally {
                resolver.close();
            }
        }
    }

    /**
     * Upgrade bridged jobs
     * @param rootResource  The root resource (topic resource)
     */
    private void upgradeBridgedJobs(final Resource topicResource) {
        final String topicName = topicResource.getName().replace('.', '/');
        final QueueConfigurationManager qcm = configuration.getQueueConfigurationManager();
        if ( qcm == null ) {
            return;
        }
        final QueueInfo info = qcm.getQueueInfo(topicName);
        JobTopicTraverser.traverse(logger, topicResource, new JobTopicTraverser.ResourceCallback() {

            @Override
            public boolean handle(final Resource rsrc) {
                try {
                    final ValueMap vm = ResourceHelper.getValueMap(rsrc);
                    final String targetId = caps.detectTarget(topicName, vm, info);

                    final Map<String, Object> props = new HashMap<String, Object>(vm);
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
                    props.remove(Job.PROPERTY_JOB_STARTED_TIME);
                    try {
                        ResourceHelper.getOrCreateResource(topicResource.getResourceResolver(), newPath, props);
                        topicResource.getResourceResolver().delete(rsrc);
                        topicResource.getResourceResolver().commit();
                    } catch ( final PersistenceException pe ) {
                        logger.warn("Unable to move job from previous version " + rsrc.getPath(), pe);
                        topicResource.getResourceResolver().refresh();
                        topicResource.getResourceResolver().revert();
                    }
                } catch (final InstantiationException ie) {
                    logger.warn("Unable to move job from previous version " + rsrc.getPath(), ie);
                    topicResource.getResourceResolver().refresh();
                    topicResource.getResourceResolver().revert();
                }
                return caps.isActive();
            }
        });
    }

    /**
     * Handle jobs from previous versions (<= 3.1.4) by moving them to the unassigned area
     */
    private void processJobsFromPreviousVersions() {
        final ResourceResolver resolver = configuration.createResourceResolver();
        if ( resolver != null ) {
            try {
                this.processJobsFromPreviousVersions(resolver.getResource(configuration.getPreviousVersionAnonPath()));
                this.processJobsFromPreviousVersions(resolver.getResource(configuration.getPreviousVersionIdentifiedPath()));
            } catch ( final PersistenceException pe ) {
                this.logger.warn("Problems moving jobs from previous version.", pe);
            } finally {
                resolver.close();
            }
        }
    }

    /**
     * Recursively find jobs and move them
     */
    private void processJobsFromPreviousVersions(final Resource rsrc) throws PersistenceException {
        if ( rsrc != null && caps.isActive() ) {
            if ( rsrc.isResourceType(ResourceHelper.RESOURCE_TYPE_JOB) ) {
                this.moveJobFromPreviousVersion(rsrc);
            } else {
                for(final Resource child : rsrc.getChildren()) {
                    this.processJobsFromPreviousVersions(child);
                }
                if ( caps.isActive() ) {
                    rsrc.getResourceResolver().delete(rsrc);
                    rsrc.getResourceResolver().commit();
                    rsrc.getResourceResolver().refresh();
                }
            }
        }
    }

    /**
     * Move a single job
     */
    private void moveJobFromPreviousVersion(final Resource jobResource)
    throws PersistenceException {
        final ResourceResolver resolver = jobResource.getResourceResolver();

        try {
            final ValueMap vm = ResourceHelper.getValueMap(jobResource);
            // check for binary properties
            Map<String, Object> binaryProperties = new HashMap<String, Object>();
            final ObjectInputStream ois = vm.get("slingevent:properties", ObjectInputStream.class);
            if ( ois != null ) {
                try {
                    int length = ois.readInt();
                    for(int i=0;i<length;i++) {
                        final String key = (String)ois.readObject();
                        final Object value = ois.readObject();
                        binaryProperties.put(key, value);
                    }
                } catch (final ClassNotFoundException cnfe) {
                    throw new PersistenceException("Class not found.", cnfe);
                } catch (final java.io.InvalidClassException ice) {
                    throw new PersistenceException("Invalid class.", ice);
                } catch (final IOException ioe) {
                    throw new PersistenceException("Unable to deserialize job properties.", ioe);
                } finally {
                    try {
                        ois.close();
                    } catch (final IOException ioe) {
                        throw new PersistenceException("Unable to deserialize job properties.", ioe);
                    }
                }
            }

            final Map<String, Object> properties = ResourceHelper.cloneValueMap(vm);

            final String topic = (String)properties.remove("slingevent:topic");
            properties.put(ResourceHelper.PROPERTY_JOB_TOPIC, topic);

            properties.remove(Job.PROPERTY_JOB_QUEUE_NAME);
            properties.remove(Job.PROPERTY_JOB_TARGET_INSTANCE);
            // and binary properties
            properties.putAll(binaryProperties);
            properties.remove("slingevent:properties");

            if ( !properties.containsKey(Job.PROPERTY_JOB_RETRIES) ) {
                properties.put(Job.PROPERTY_JOB_RETRIES, 10); // we put a dummy value here; this gets updated by the queue
            }
            if ( !properties.containsKey(Job.PROPERTY_JOB_RETRY_COUNT) ) {
                properties.put(Job.PROPERTY_JOB_RETRY_COUNT, 0);
            }

            final List<InstanceDescription> potentialTargets = caps.getPotentialTargets(topic);
            String targetId = null;
            if ( potentialTargets != null && potentialTargets.size() > 0 ) {
                final QueueConfigurationManager qcm = configuration.getQueueConfigurationManager();
                if ( qcm == null ) {
                    resolver.revert();
                    return;
                }
                final QueueInfo info = qcm.getQueueInfo(topic);
                logger.debug("Found queue {} for {}", info.queueConfiguration, topic);
                targetId = caps.detectTarget(topic, vm, info);
                if ( targetId != null ) {
                    properties.put(Job.PROPERTY_JOB_QUEUE_NAME, info.queueName);
                    properties.put(Job.PROPERTY_JOB_TARGET_INSTANCE, targetId);
                    properties.put(Job.PROPERTY_JOB_RETRIES, info.queueConfiguration.getMaxRetries());
                }
            }

            properties.put(Job.PROPERTY_JOB_CREATED_INSTANCE, "old:" + Environment.APPLICATION_ID);
            properties.put(ResourceResolver.PROPERTY_RESOURCE_TYPE, ResourceHelper.RESOURCE_TYPE_JOB);

            final String jobId = configuration.getUniqueId(topic);
            properties.put(ResourceHelper.PROPERTY_JOB_ID, jobId);
            properties.remove(Job.PROPERTY_JOB_STARTED_TIME);

            final String newPath = configuration.getUniquePath(targetId, topic, jobId, vm);
            this.logger.debug("Moving 'old' job from {} to {}", jobResource.getPath(), newPath);

            ResourceHelper.getOrCreateResource(resolver, newPath, properties);
            resolver.delete(jobResource);
            resolver.commit();
        } catch (final InstantiationException ie) {
            throw new PersistenceException("Exception while reading reasource: " + ie.getMessage(), ie.getCause());
        }
    }
}
