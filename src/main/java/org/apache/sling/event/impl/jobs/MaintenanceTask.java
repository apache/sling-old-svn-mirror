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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.jackrabbit.util.ISO8601;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.QuerySyntaxException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.event.impl.jobs.config.QueueConfigurationManager;
import org.apache.sling.event.impl.jobs.config.QueueConfigurationManager.QueueInfo;
import org.apache.sling.event.impl.support.Environment;
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

    /** Resource resolver factory. */
    private final ResourceResolverFactory resourceResolverFactory;

    /** Job manager configuration. */
    private final JobManagerConfiguration configuration;

    /** Change count for queue configurations .*/
    private volatile long queueConfigChangeCount = -1;

    /** Change count for topology changes .*/
    private volatile long topologyChangeCount = -1;

    private boolean checkedForPreviousVersion = false;

    /**
     * Constructor
     */
    public MaintenanceTask(final JobManagerConfiguration config, final ResourceResolverFactory factory) {
        this.resourceResolverFactory = factory;
        this.configuration = config;
    }

    private void reassignJobs(final TopologyCapabilities caps,
            final QueueConfigurationManager queueManager) {
        if ( caps != null && caps.isLeader() ) {
            this.logger.debug("Checking for stopped instances...");
            ResourceResolver resolver = null;
            try {
                resolver = this.resourceResolverFactory.getAdministrativeResourceResolver(null);
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
            } catch ( final LoginException le ) {
                this.ignoreException(le);
            } finally {
                if ( resolver != null ) {
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
    private void assignUnassignedJobs(final TopologyCapabilities caps,
            final QueueConfigurationManager queueManager) {
        if ( caps != null && caps.isLeader() ) {
            logger.debug("Checking unassigned jobs...");
            ResourceResolver resolver = null;
            try {
                resolver = this.resourceResolverFactory.getAdministrativeResourceResolver(null);
                final Resource unassignedRoot = resolver.getResource(this.configuration.getUnassignedJobsPath());
                logger.debug("Got unassigned root {}", unassignedRoot);

                // this resource should exist, but we check anyway
                if ( unassignedRoot != null ) {
                    assignJobs(caps, queueManager, unassignedRoot, false);
                }
            } catch ( final LoginException le ) {
                this.ignoreException(le);
            } finally {
                if ( resolver != null ) {
                    resolver.close();
                }
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
     * Check if the topology has changed.
     */
    private boolean topologyHasChanged(final TopologyCapabilities topologyCapabilities) {
        boolean topologyChanged = false;
        if ( topologyCapabilities != null ) {
            if ( this.topologyChangeCount != topologyCapabilities.getChangeCount() ) {
                this.topologyChangeCount = topologyCapabilities.getChangeCount();
                topologyChanged = true;
            }
        }
        return topologyChanged;
    }

    private boolean queueConfigurationHasChanged(final TopologyCapabilities topologyCapabilities,
            final QueueConfigurationManager queueManager) {
        boolean configChanged = false;
        if ( topologyCapabilities != null ) {
            final int queueChangeCount = queueManager.getChangeCount();
            if ( this.queueConfigChangeCount < queueChangeCount ) {
                configChanged = true;
                this.queueConfigChangeCount = queueChangeCount;
            }
        }
        return configChanged;
    }

    /**
     * One maintenance run
     */
    public void run(final TopologyCapabilities topologyCapabilities,
            final QueueConfigurationManager queueManager,
            final long cleanUpCounter) {
        // check topology and config change during each invocation
        final boolean topologyChanged = this.topologyHasChanged(topologyCapabilities);
        final boolean configChanged = this.queueConfigurationHasChanged(topologyCapabilities, queueManager);

        // if topology changed, reschedule assigned jobs for stopped instances
        if ( topologyChanged ) {
            this.reassignJobs(topologyCapabilities, queueManager);
        }
        // try to assign unassigned jobs
        if ( topologyChanged || configChanged ) {
            this.assignUnassignedJobs(topologyCapabilities, queueManager);
        }

        if ( topologyChanged && !this.checkedForPreviousVersion && topologyCapabilities != null && topologyCapabilities.isLeader() ) {
            this.processJobsFromPreviousVersions(topologyCapabilities, queueManager);
        }

        if ( topologyCapabilities != null ) {
            // Clean up
            final String cleanUpAssignedPath;;
            if ( topologyCapabilities.isLeader() ) {
                cleanUpAssignedPath = this.configuration.getUnassignedJobsPath();
            } else {
                cleanUpAssignedPath = null;
            }

            if ( cleanUpCounter % 60 == 0 ) { // full clean up is done every hour
                this.fullEmptyFolderCleanup(topologyCapabilities, this.configuration.getLocalJobsPath());
                if ( cleanUpAssignedPath != null ) {
                    this.fullEmptyFolderCleanup(topologyCapabilities, cleanUpAssignedPath);
                }
            } else if ( cleanUpCounter % 5 == 0 ) { // simple clean up every 5 minutes
                this.simpleEmptyFolderCleanup(topologyCapabilities, this.configuration.getLocalJobsPath());
                if ( cleanUpAssignedPath != null ) {
                    this.simpleEmptyFolderCleanup(topologyCapabilities, cleanUpAssignedPath);
                }
            }
        }

        // lock cleanup is done every 3 minutes
        if ( cleanUpCounter % 3 == 0 ) {
            this.lockCleanup(topologyCapabilities);
        }
    }

    /**
     * Clean up the locks
     * All locks older than three minutes are removed
     */
    private void lockCleanup(final TopologyCapabilities caps) {
        if ( caps != null && caps.isLeader() ) {
            this.logger.debug("Cleaning up job resource tree: removing obsolete locks");
            ResourceResolver resolver = null;
            try {
                resolver = this.resourceResolverFactory.getAdministrativeResourceResolver(null);
                final Calendar startDate = Calendar.getInstance();
                startDate.add(Calendar.MINUTE, -3);

                final StringBuilder buf = new StringBuilder(64);

                buf.append("//element(*)[@");
                buf.append(ISO9075.encode(ResourceResolver.PROPERTY_RESOURCE_TYPE));
                buf.append(" = '");
                buf.append(Utility.RESOURCE_TYPE_LOCK);
                buf.append("' and @");
                buf.append(ISO9075.encode(Utility.PROPERTY_LOCK_CREATED));
                buf.append(" < xs:dateTime('");
                buf.append(ISO8601.format(startDate));
                buf.append("')]");
                final Iterator<Resource> result = resolver.findResources(buf.toString(), "xpath");

                while ( caps.isActive() && result.hasNext() ) {
                    final Resource lockResource = result.next();
                    // sanity check for the path
                    if ( this.configuration.isLock(lockResource.getPath()) ) {
                        try {
                            resolver.delete(lockResource);
                            resolver.commit();
                        } catch ( final PersistenceException pe) {
                            this.ignoreException(pe);
                            resolver.refresh();
                        }
                    }
                }
            } catch (final QuerySyntaxException qse) {
                this.ignoreException(qse);
            } catch (final LoginException le) {
                this.ignoreException(le);
            } finally {
                if ( resolver != null ) {
                    resolver.close();
                }
            }
        }
    }

    /**
     * Simple empty folder removes empty folders for the last five minutes
     * from an hour ago!
     * If folder for minute 59 is removed, we check the hour folder as well.
     */
    private void simpleEmptyFolderCleanup(final TopologyCapabilities caps, final String basePath) {
        this.logger.debug("Cleaning up job resource tree: looking for empty folders");
        ResourceResolver resolver = null;
        try {
            resolver = this.resourceResolverFactory.getAdministrativeResourceResolver(null);
            final Calendar cleanUpDate = Calendar.getInstance();
            // go back ten minutes
            cleanUpDate.add(Calendar.HOUR, -1);

            final Resource baseResource = resolver.getResource(basePath);
            // sanity check - should never be null
            if ( baseResource != null ) {
                final Iterator<Resource> topicIter = baseResource.listChildren();
                while ( caps.isActive() && topicIter.hasNext() ) {
                    final Resource topicResource = topicIter.next();

                    for(int i = 0; i < 5; i++) {
                        if ( caps.isActive() ) {
                            final StringBuilder sb = new StringBuilder(topicResource.getPath());
                            sb.append('/');
                            sb.append(cleanUpDate.get(Calendar.YEAR));
                            sb.append('/');
                            sb.append(cleanUpDate.get(Calendar.MONTH) + 1);
                            sb.append('/');
                            sb.append(cleanUpDate.get(Calendar.DAY_OF_MONTH));
                            sb.append('/');
                            sb.append(cleanUpDate.get(Calendar.HOUR_OF_DAY));
                            final String path = sb.toString();

                            final Resource dateResource = resolver.getResource(path);
                            if ( dateResource != null && !dateResource.listChildren().hasNext() ) {
                                resolver.delete(dateResource);
                                resolver.commit();
                            }
                            // check hour folder
                            if ( path.endsWith("59") ) {
                                final String hourPath = path.substring(0, path.length() - 3);
                                final Resource hourResource = resolver.getResource(hourPath);
                                if ( hourResource != null && !hourResource.listChildren().hasNext() ) {
                                    resolver.delete(hourResource);
                                    resolver.commit();
                                }
                            }
                            cleanUpDate.add(Calendar.MINUTE, -1);
                        }
                    }
                }
            }

        } catch (final PersistenceException pe) {
            // in the case of an error, we just log this as a warning
            this.logger.warn("Exception during job resource tree cleanup.", pe);
        } catch (final LoginException ignore) {
            this.ignoreException(ignore);
        } finally {
            if ( resolver != null ) {
                resolver.close();
            }
        }
    }

    /**
     * Full cleanup - this scans all directories!
     */
    private void fullEmptyFolderCleanup(final TopologyCapabilities caps, final String basePath) {
        this.logger.debug("Cleaning up job resource tree: removing ALL empty folders");
        ResourceResolver resolver = null;
        try {
            resolver = this.resourceResolverFactory.getAdministrativeResourceResolver(null);

            final Resource baseResource = resolver.getResource(basePath);
            // sanity check - should never be null
            if ( baseResource != null ) {
                final Calendar now = Calendar.getInstance();

                final Iterator<Resource> topicIter = baseResource.listChildren();
                while ( caps.isActive() && topicIter.hasNext() ) {
                    final Resource topicResource = topicIter.next();

                    // now years
                    final Iterator<Resource> yearIter = topicResource.listChildren();
                    while ( caps.isActive() && yearIter.hasNext() ) {
                        final Resource yearResource = yearIter.next();
                        final int year = Integer.valueOf(yearResource.getName());
                        final boolean oldYear = year < now.get(Calendar.YEAR);

                        // months
                        final Iterator<Resource> monthIter = yearResource.listChildren();
                        while ( caps.isActive() && monthIter.hasNext() ) {
                            final Resource monthResource = monthIter.next();
                            final int month = Integer.valueOf(monthResource.getName());
                            final boolean oldMonth = oldYear || month < (now.get(Calendar.MONTH) + 1);

                            // days
                            final Iterator<Resource> dayIter = monthResource.listChildren();
                            while ( caps.isActive() && dayIter.hasNext() ) {
                                final Resource dayResource = dayIter.next();
                                final int day = Integer.valueOf(dayResource.getName());
                                final boolean oldDay = oldMonth || day < now.get(Calendar.DAY_OF_MONTH);

                                // hours
                                final Iterator<Resource> hourIter = dayResource.listChildren();
                                while ( caps.isActive() && hourIter.hasNext() ) {
                                    final Resource hourResource = hourIter.next();
                                    final int hour = Integer.valueOf(hourResource.getName());
                                    final boolean oldHour = (oldDay && (oldMonth || now.get(Calendar.HOUR_OF_DAY) > 0)) || hour < (now.get(Calendar.HOUR_OF_DAY) -1);

                                    // we only remove minutes if the hour is old
                                    if ( oldHour ) {
                                        final Iterator<Resource> minuteIter = hourResource.listChildren();
                                        while ( caps.isActive() && minuteIter.hasNext() ) {
                                            final Resource minuteResource = minuteIter.next();

                                            // check if we can delete the minute
                                            if ( !minuteResource.listChildren().hasNext() ) {
                                                resolver.delete(minuteResource);
                                                resolver.commit();
                                            }
                                        }
                                    }

                                    // check if we can delete the hour
                                    if ( caps.isActive() && oldHour && !hourResource.listChildren().hasNext()) {
                                        resolver.delete(hourResource);
                                        resolver.commit();
                                    }
                                }
                                // check if we can delete the day
                                if ( caps.isActive() && oldDay && !dayResource.listChildren().hasNext()) {
                                    resolver.delete(dayResource);
                                    resolver.commit();
                                }
                            }

                            // check if we can delete the month
                            if ( caps.isActive() && oldMonth && !monthResource.listChildren().hasNext() ) {
                                resolver.delete(monthResource);
                                resolver.commit();
                            }
                        }

                        // check if we can delete the year
                        if ( caps.isActive() && oldYear && !yearResource.listChildren().hasNext() ) {
                            resolver.delete(yearResource);
                            resolver.commit();
                        }
                    }
                }
            }

        } catch (final PersistenceException pe) {
            // in the case of an error, we just log this as a warning
            this.logger.warn("Exception during job resource tree cleanup.", pe);
        } catch (final LoginException ignore) {
            this.ignoreException(ignore);
        } finally {
            if ( resolver != null ) {
                resolver.close();
            }
        }
    }

    /**
     * Reassign a job to a different target
     * @param job The job
     * @param targetId New target or <code>null</code> if unknown
     */
    public void reassignJob(final JobImpl job, final String targetId) {
        ResourceResolver resolver = null;
        try {
            resolver = this.resourceResolverFactory.getAdministrativeResourceResolver(null);
            final Resource jobResource = resolver.getResource(job.getResourcePath());
            if ( jobResource != null ) {
                try {
                    final ValueMap vm = ResourceHelper.getValueMap(jobResource);
                    final String newPath = this.configuration.getUniquePath(targetId, job.getTopic(), job.getId(), job.getProperties());

                    final Map<String, Object> props = new HashMap<String, Object>(vm);
                    props.remove(Job.PROPERTY_JOB_QUEUE_NAME);
                    if ( targetId == null ) {
                        props.remove(Job.PROPERTY_JOB_TARGET_INSTANCE);
                    } else {
                        props.put(Job.PROPERTY_JOB_TARGET_INSTANCE, targetId);
                    }
                    props.remove(Job.PROPERTY_JOB_STARTED_TIME);

                    try {
                        ResourceHelper.getOrCreateResource(resolver, newPath, props);
                        resolver.delete(jobResource);
                        resolver.commit();
                    } catch ( final PersistenceException pe ) {
                        this.ignoreException(pe);
                    }
                } catch (final InstantiationException ie) {
                    // something happened with the resource in the meantime
                    this.ignoreException(ie);
                }
            }
        } catch (final LoginException ignore) {
            this.ignoreException(ignore);
        } finally {
            if ( resolver != null ) {
                resolver.close();
            }
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

    /**
     * Handle jobs from previous versions (<= 3.1.4) by moving them to the unassigned area
     */
    private void processJobsFromPreviousVersions(final TopologyCapabilities caps,
            final QueueConfigurationManager queueManager) {
        ResourceResolver resolver = null;
        try {
            resolver = this.resourceResolverFactory.getAdministrativeResourceResolver(null);

            this.processJobsFromPreviousVersions(caps, queueManager, resolver.getResource(this.configuration.getPreviousVersionAnonPath()));
            this.processJobsFromPreviousVersions(caps, queueManager, resolver.getResource(this.configuration.getPreviousVersionIdentifiedPath()));
            this.checkedForPreviousVersion = true;
        } catch ( final PersistenceException pe ) {
            this.logger.warn("Problems moving jobs from previous version.", pe);
        } catch ( final LoginException le ) {
            this.ignoreException(le);
        } finally {
            if ( resolver != null ) {
                resolver.close();
            }
        }
    }

    /**
     * Recursively find jobs and move them
     */
    private void processJobsFromPreviousVersions(final TopologyCapabilities caps,
            final QueueConfigurationManager queueManager,
            final Resource rsrc) throws PersistenceException {
        if ( rsrc != null && caps.isActive() ) {
            if ( rsrc.isResourceType(ResourceHelper.RESOURCE_TYPE_JOB) ) {
                this.moveJobFromPreviousVersion(caps, queueManager, rsrc);
            } else {
                for(final Resource child : rsrc.getChildren()) {
                    this.processJobsFromPreviousVersions(caps, queueManager, child);
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
    private void moveJobFromPreviousVersion(final TopologyCapabilities caps,
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
                        this.ignoreException(ioe);
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

            final String jobId = this.configuration.getUniqueId(topic);
            properties.put(ResourceHelper.PROPERTY_JOB_ID, jobId);
            properties.remove(Job.PROPERTY_JOB_STARTED_TIME);

            final String newPath = this.configuration.getUniquePath(targetId, topic, jobId, vm);
            this.logger.debug("Moving 'old' job from {} to {}", jobResource.getPath(), newPath);

            ResourceHelper.getOrCreateResource(resolver, newPath, properties);
            resolver.delete(jobResource);
            resolver.commit();
        } catch (final InstantiationException ie) {
            throw new PersistenceException("Exception while reading reasource: " + ie.getMessage(), ie.getCause());
        }
    }
}
