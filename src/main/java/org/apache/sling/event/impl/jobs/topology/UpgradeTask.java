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
import org.apache.sling.event.impl.jobs.JobImpl;
import org.apache.sling.event.impl.jobs.JobManagerConfiguration;
import org.apache.sling.event.impl.jobs.config.QueueConfigurationManager;
import org.apache.sling.event.impl.jobs.config.QueueConfigurationManager.QueueInfo;
import org.apache.sling.event.impl.support.Environment;
import org.apache.sling.event.impl.support.ResourceHelper;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.QueueConfiguration;
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

    /**
     * Upgrade
     */
    public void run(final JobManagerConfiguration configuration,
            final TopologyCapabilities topologyCapabilities,
            final QueueConfigurationManager queueManager) {
        if ( topologyCapabilities.isLeader() ) {
            this.processJobsFromPreviousVersions(configuration, topologyCapabilities, queueManager);
        }
    }

    /**
     * Handle jobs from previous versions (<= 3.1.4) by moving them to the unassigned area
     */
    private void processJobsFromPreviousVersions(final JobManagerConfiguration configuration,
            final TopologyCapabilities caps,
            final QueueConfigurationManager queueManager) {
        final ResourceResolver resolver = configuration.createResourceResolver();
        try {
            this.processJobsFromPreviousVersions(configuration, caps, queueManager, resolver.getResource(configuration.getPreviousVersionAnonPath()));
            this.processJobsFromPreviousVersions(configuration, caps, queueManager, resolver.getResource(configuration.getPreviousVersionIdentifiedPath()));
        } catch ( final PersistenceException pe ) {
            this.logger.warn("Problems moving jobs from previous version.", pe);
        } finally {
            resolver.close();
        }
    }

    /**
     * Recursively find jobs and move them
     */
    private void processJobsFromPreviousVersions(final JobManagerConfiguration configuration,
            final TopologyCapabilities caps,
            final QueueConfigurationManager queueManager,
            final Resource rsrc) throws PersistenceException {
        if ( rsrc != null && caps.isActive() ) {
            if ( rsrc.isResourceType(ResourceHelper.RESOURCE_TYPE_JOB) ) {
                this.moveJobFromPreviousVersion(configuration, caps, queueManager, rsrc);
            } else {
                for(final Resource child : rsrc.getChildren()) {
                    this.processJobsFromPreviousVersions(configuration, caps, queueManager, child);
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
    private void moveJobFromPreviousVersion(final JobManagerConfiguration configuration,
            final TopologyCapabilities caps,
            final QueueConfigurationManager queueManager,
            final Resource jobResource)
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

            properties.put(JobImpl.PROPERTY_BRIDGED_EVENT, true);
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

            final List<InstanceDescription> potentialTargets = caps.getPotentialTargets("/", null);
            String targetId = null;
            if ( potentialTargets != null && potentialTargets.size() > 0 ) {
                final QueueInfo info = queueManager.getQueueInfo(topic);
                logger.debug("Found queue {} for {}", info.queueConfiguration, topic);
                // if queue is configured to drop, we drop
                if ( info.queueConfiguration.getType() ==  QueueConfiguration.Type.DROP) {
                    resolver.delete(jobResource);
                    resolver.commit();
                    return;
                }
                if ( info.queueConfiguration.getType() != QueueConfiguration.Type.IGNORE ) {
                    targetId = caps.detectTarget(topic, vm, info);
                    if ( targetId != null ) {
                        properties.put(Job.PROPERTY_JOB_QUEUE_NAME, info.queueName);
                        properties.put(Job.PROPERTY_JOB_TARGET_INSTANCE, targetId);
                        properties.put(Job.PROPERTY_JOB_RETRIES, info.queueConfiguration.getMaxRetries());
                    }
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
